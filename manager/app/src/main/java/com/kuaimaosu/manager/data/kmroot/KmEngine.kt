package com.kuaimaosu.manager.data.kmroot

import android.content.Context

/**
 * 98260 提权引擎 —— 编排核心（新引擎版）。
 *
 * 流程：等 Shizuku shell → 探测工作目录 → 部署 kmengine/kmplant/ksud/profile →
 * spawn `kmengine run`（环境变量下发 KE_*）→ 逐行解析日志 → 判定结果。
 *
 * runner（libkmengine.so）会 fork plant（libkmplant.so）并把 stdout/stderr 实时转发，
 * 出现 CRED_OK / LATE_LOAD_RC=0 即判定成功；plant 结束输出 DONE，runner 退出码 0。
 */
class KmEngine {
    interface Listener {
        fun onLog(line: String)
        fun onStage(pct: Int, label: String)
        fun onFinished(ok: Boolean, message: String)
    }

    @Volatile
    var running = false
        private set
    @Volatile
    private var done = false
    @Volatile
    private var settled = false
    @Volatile
    private var earlyOk = false
    @Volatile
    private var lastPct = 0
    @Volatile
    private var lastLabel = "准备"
    @Volatile
    private var lastLineAt = 0L

    private var listener: Listener? = null
    private var live: KmShell.Live? = null
    private var worker: Thread? = null

    private var sawUnsupported = false
    private var sawProfileOk = false
    private var sawCredOk = false
    private var sawLateLoad = false
    private var sawScriptError = false
    private var lastFail = ""
    private var lastLine = ""

    fun setListener(l: Listener?) {
        listener = l
    }

    fun log(s: String) {
        try { listener?.onLog(s) } catch (_: Throwable) {}
    }

    fun stage(pct: Int, label: String) {
        if (settled) return
        lastPct = pct
        lastLabel = label
        try { listener?.onStage(pct, label) } catch (_: Throwable) {}
    }

    @Synchronized
    fun start(context: Context): Boolean {
        if (running) {
            log("已有任务在运行，继续观察日志即可。")
            return false
        }
        if (worker?.isAlive == true) {
            log("上一轮的收尾还没结束，等 1~2 秒再点一次。")
            return false
        }
        val app = context.applicationContext
        running = true
        done = false
        settled = false
        earlyOk = false
        sawUnsupported = false
        sawProfileOk = false
        sawCredOk = false
        sawLateLoad = false
        sawScriptError = false
        lastFail = ""
        lastLine = ""
        lastPct = 0
        lastLabel = "准备"
        val t = Thread({ runInternal(app) }, "kmengine").apply { isDaemon = true }
        worker = t
        t.start()
        return true
    }

    @Synchronized
    fun stop() {
        if (running) {
            log("用户停止。")
            try {
                if (live != null && !earlyOk) live?.kill()
            } catch (_: Throwable) {}
            finish(false, "已停止；若已进入泄漏阶段，建议重启手机后再试")
        }
    }

    private fun safe(s: String?) = s ?: ""

    /**
     * 由内核 release 串推导 ksud late-load 用的 KMI。
     *
     * 两个原则：
     * 1) 用正则取「主版本.次版本」，不再用 contains("6.1")——否则 6.10/6.12/6.18
     *    都会被它误命中，只能靠判断顺序躲坑，极易在加新版本时踩雷。
     * 2) 只返回 bin/aarch64 里真实存在的 ko 前缀。旧代码会给 6.10 内核返回
     *    android15-6.10，但根本没有这个 ko，late-load 取文件时必然失败。
     *    6.10 没有专属 ko，按最接近的 6.12 兜底（ksuinit 手动重定位会绕过 vermagic）。
     */
    private fun kmiOf(kernel: String): String {
        val m = Regex("""(\d+)\.(\d+)""").find(kernel.lowercase())
        val ver = if (m != null) "${m.groupValues[1]}.${m.groupValues[2]}" else ""
        return when (ver) {
            "6.18" -> "android17-6.18"
            "6.12" -> "android16-6.12"
            "6.6"  -> "android15-6.6"   // 66118/66127（如用户 abogki537459655）
            "6.1"  -> "android14-6.1"
            "5.15" -> "android14-5.15"
            "5.10" -> "android13-5.10"
            else   -> "android16-6.12"  // 6.10 及未知内核：用真实存在且最接近的 ko 兜底
        }
    }

    private fun runInternal(context: Context) {
        try {
            stage(2, "检查 Shizuku")
            if (!KmShizuku.waitReady(context, 12000) { log(it) }) {
                finish(false, "shell 服务未就绪：${KmShizuku.describe(context, KmShizuku.current(context))}")
                return
            }

            stage(6, "探测环境")
            val kernel = safe(KmShell.run("uname -r", 12)).trim().split("\n")[0].trim()
            log("内核: $kernel")
            if (kernel.isEmpty()) {
                finish(false, "读不到内核版本（uname -r 为空）")
                return
            }

            stage(10, "部署载荷")
            val dir = KmCfg.resolveDev(context)
            if (KmCfg.lastResolveError.isNotEmpty()) {
                log((if (KmCfg.ndMode) "工作目录: " else "工作目录探测失败: ") + KmCfg.lastResolveError)
            }
            val mgr = KmMgr.pkg(context)
            val mode = KmCfg.mode(context) // fake | zero
            log("提权模式: ${if (mode == "zero") "zero（只清零）" else "fake（完整 root + late-load）"}")
            log("管理器: ${KmMgr.displayName(context, mgr)}（$mgr）")

            val ksudInfo = KmCfg.deployKsud(mgr)
            if (ksudInfo.isNotEmpty()) log("ksud: $ksudInfo")

            val copyResult = safe(KmShell.run(KmCfg.copyScript(context), 240))
            if (copyResult.trim().isNotEmpty()) log(copyResult.trim())
            if (copyResult.contains("COPY_FAIL")) {
                val diag = safe(KmShell.run("id; getenforce; ls -ld /data /data/local /data/local/tmp 2>&1 | head -n 6", 20)).trim()
                log("落地诊断："); log(diag)
                finish(false, "载荷落地失败：目标目录建不出来/写不进（看上面的落地诊断）")
                return
            }
            val deploySkip = copyResult.contains("DEPLOY_SKIP")
            if (!KmCfg.ndMode && !deploySkip) {
                val verify = safe(KmShell.run(KmCfg.verifyScript(), 60))
                log("载荷校验：$verify")
                if (verify.contains("engine:0 ") || verify.contains("plant:0 ")) {
                    finish(false, "载荷落地失败（nativeLibraryDir 里缺新引擎 so）")
                    return
                }
            }

            val assetsDir = KmCfg.ensureAssets(context)
            if (KmCfg.assetsError.isNotEmpty()) {
                finish(false, "内核档案落地失败：${KmCfg.assetsError}")
                return
            }
            log("档案目录: $assetsDir")

            // 拼 runner 命令：环境变量前置，exec 工作目录里的 kmengine
            val plantPath = if (KmCfg.ndMode) "${KmCfg.nativeDir(context)}/libkmplant.so" else "$dir/kmplant"
            val runner = if (KmCfg.ndMode) "${KmCfg.nativeDir(context)}/libkmengine.so" else "$dir/kmengine"
            val keMode = if (mode == "zero") "zero" else "fake"
            val kmi = kmiOf(kernel)
            val ksudPath = "$dir/ksud"
            val cmd = buildString {
                append("KE_DIR='$dir' ")
                append("KE_PLANT='$plantPath' ")
                append("KE_PROFILE='$dir/kernel_profiles.txt' ")
                append("KE_MODE='$keMode' ")
                // 新 core（plant_core.inc）的 late-load 读 PYB110_* 环境变量
                append("PYB110_KSUD='$ksudPath' ")
                append("PYB110_MANAGER_PKG='$mgr' ")
                append("PYB110_KMI='$kmi' ")
                append("PYB110_FILES_DIR='$dir' ")
                append("'$runner' run 2>&1")
            }
            log("启动引擎: KE_MODE=$keMode PYB110_KMI=$kmi")

            stage(18, "启动引擎")
            val argv = arrayOf("sh", "-c", cmd)
            val process = KmShell.spawn(argv, dir)
            live = process
            lastLineAt = System.currentTimeMillis()
            val onLineCb: (String) -> Unit = { onLine(it) }
            process.startReaders(onLineCb, onLineCb)

            // 新引擎看门狗 5 分钟（runner 内部 300s 会自己收尾）
            val deadline = System.currentTimeMillis() + 5 * 60 * 1000L
            while (System.currentTimeMillis() < deadline && !done) {
                Thread.sleep(400)
                if (!process.isAlive() && System.currentTimeMillis() - lastLineAt > 8000) break
            }
            if (!earlyOk) try { live?.kill() } catch (_: Throwable) {}
            if (settled) return

            stage(94, "验证结果")
            val module = safe(KmShell.run("grep -E '^kernelsu ' /proc/modules | head -n1", 15)).trim()
            val suOut = safe(KmShell.run("su -c id 2>&1", 20)).trim()
            val su = if (!suOut.contains("inaccessible") && !suOut.contains("not found")) suOut else ""
            log("模块: ${if (module.isEmpty()) "未加载" else module}")
            log("su: ${if (su.isEmpty()) "不可用" else su}")

            // 提权成功后自动开启 SUSFS 挂载隐藏。
            // Duck Detector 这类检测工具会 fork zygote_next 隔离进程去读 /proc/self/mountinfo；
            // 不开这个开关，非 root 进程就能看到 Root 管理的 200+ 条挂载，直接暴露 root。
            // 临时 root 没有 post-fs-data 阶段，持久化配置不会自动应用，所以这里下发的是
            // 「当前内核即时命令」，生效于本次开机周期；每次提权后重下，幂等无副作用。
            if (su.contains("uid=0")) {
                log("自动开启 SUSFS 挂载隐藏（对非 root 进程隐藏 sus 挂载）…")
                val susfsOut = safe(
                    KmShell.run(
                        "su -c \"$ksudPath susfs hide_sus_mnts_for_non_su_procs 1\" 2>&1",
                        25,
                    )
                ).trim()
                log("SUSFS 挂载隐藏: ${if (susfsOut.isEmpty()) "已下发（无回显即成功）" else susfsOut}")
            }

            judgeResult(module, su)
        } catch (t: Throwable) {
            try {
                if (live != null && !earlyOk) live?.kill()
            } catch (_: Throwable) {}
            finish(false, "异常: $t")
        }
    }

    private fun judgeResult(module: String, su: String) {
        // 只看「结果证据」：su 真的返回 uid=0，或 /proc/modules 里真的有 kernelsu。
        // 过程标志（sawCredOk / sawLateLoad）只代表 UAF 拿到临时 cred、发起过 late-load，
        // 不能单独判定成功——否则会出现「界面说成功，但 su 根本用不了」的假成功。
        val suOk = su.contains("uid=0")
        val moduleOk = module.isNotEmpty()
        when {
            suOk && moduleOk -> finish(true, "越狱成功：临时 root 已就绪，KernelSU 已接管")
            suOk -> finish(true, "越狱成功：su 已就绪（模块未在 /proc/modules 显示，可能延迟加载）")
            moduleOk -> finish(true, "越狱成功：KernelSU 模块已加载（su 尚未响应，若用不了请重启 KuaiMaoSU）")
            // 到这里 su 和模块都没有真实证据，即使过程标志为真也判失败并给出可诊断的原因。
            sawCredOk -> finish(false, "已拿到临时 root，但 KernelSU 未接管、su 不可用：多半是 ko 加载失败（KMI 不匹配或缺少对应 ko），请把日志回传")
            sawLateLoad -> finish(false, "发起过 late-load，但模块和 su 都不可用：ko 加载失败，请把日志回传")
            else -> {
                val msg = when {
                    sawUnsupported -> "内核不在 19 机型精确档案里，引擎未开跑（安全）。可反馈机型+内核串适配"
                    sawProfileOk && lastFail.isNotEmpty() -> "已匹配内核档案，但利用未成功：$lastFail"
                    lastFail.isNotEmpty() -> "引擎未成功：$lastFail"
                    else -> "未拿到临时 root：${if (lastLine.isEmpty()) "引擎运行中断" else lastLine}"
                }
                finish(false, msg)
            }
        }
    }

    /** 逐行日志 → 进度状态机。解析新引擎 runner/plant 的标志串。 */
    fun onLine(raw: String?) {
        lastLineAt = System.currentTimeMillis()
        log(raw ?: "")
        val s = raw?.trim() ?: ""
        if (s.isEmpty()) return
        lastLine = s

        when {
            s.startsWith("PROBE_KERNEL=") || s.startsWith("RELEASE=") -> stage(20, "识别内核")
            s.startsWith("PROFILE_OK=") || s.startsWith("PROFILE_66118=") || s.startsWith("PROFILE_FUZZY=") -> {
                sawProfileOk = true
                stage(26, "加载内核档案")
            }
            s.startsWith("PROFILE_UNSUPPORTED=") || s.contains("REASON=kernel mismatch") || s.startsWith("UNSUPPORTED_KERNEL=") -> {
                sawUnsupported = true
                lastFail = "内核无匹配档案（$s）"
            }
            s.contains("[+] pid=") || s.startsWith("ENGINE_START") -> stage(30, "触发 UAF")
            s.contains("base=") || s.startsWith("PLANT_SLIDE=") -> stage(42, "解算 KASLR")
            s.contains("victim page") || s.startsWith("FAST_CHURN") -> stage(52, "抢占 victim 页")
            s.contains("burst hit") || s.startsWith("FAST_RESULT") -> stage(66, "写原语收割")
            s.contains("[cred]") || s.startsWith("CRED_SCAN") -> stage(70, "定位 cred")
            s.contains("[+] stage ") || s.startsWith("STAGE ") -> stage(74, "写入内核")
            s.startsWith("PLANT_CRED_OK") || s.startsWith("CRED_OK") -> {
                sawCredOk = true
                stage(86, "root 就绪")
            }
            s.startsWith("LATE_LOAD_CMD=") || s.contains("late-load") -> stage(90, "管理器 late-load")
            s.contains("LATE_LOAD_RC=0") || s.contains("MODULE_LOADED") -> sawLateLoad = true
            s.startsWith("PARKED=1") || s == "DONE" -> stage(96, "收尾")
        }

        if (s.startsWith("PLANT_NOT_FOUND=") || s.startsWith("PROFILE_NOT_FOUND=") ||
            s.startsWith("DIR_MISSING=") || s.startsWith("PLANT_SPAWN_FAIL=") ||
            s.startsWith("PROFILE_FILE_MISSING=")) {
            lastFail = "引擎文件不完整：$s"
            sawScriptError = true
        }
        // 新 core 失败标志：FAIL=<tag> REASON=<...> 或 [-] ... miss/failed
        if (s.startsWith("FAIL=")) {
            lastFail = s.removePrefix("FAIL=")
        }
        if (s == "PLANT_FAIL") lastFail = if (lastFail.isEmpty()) "利用链未成功（竞态未命中，可重试）" else lastFail
        if (s.startsWith("ZERO_NO_TARGET")) lastFail = "zero 模式需要目标地址，请改用 fake 模式"

        if (s == "DONE" || s == "PLANT_FAIL") done = true
        if (s.startsWith("PLANT_CRED_OK") || s.startsWith("CRED_OK")) {
            earlyOk = true   // 已成 root，收尾阶段不要再 kill plant（它持有 fake cred 页）
        }
    }

    @Synchronized
    private fun finish(ok: Boolean, message: String) {
        if (settled) return
        running = false
        stage(if (ok) 100 else lastPct, if (ok) "成功" else "失败")
        settled = true
        done = true
        try { listener?.onFinished(ok, message) } catch (_: Throwable) {}
    }
}
