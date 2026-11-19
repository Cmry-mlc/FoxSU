package com.kuaimaosu.manager.data.kmroot

import android.content.Context

/**
 * KMRoot 提权引擎 —— 编排核心。
 * 移植自 com.km.kmroot.engine.Engine：部署载荷 → spawn trrun.sh → 逐行解析日志 → 判定结果。
 * late-load 用 KuaiMaoSU 自带、已内嵌全套 ko 的 ksud（由 KmCfg.deployKsud 从本 APK 落地）。
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
    @Volatile
    private var sawDeath = false

    private var listener: Listener? = null
    private var live: KmShell.Live? = null
    private var worker: Thread? = null

    // 日志标志位
    private var sawKsuLoaded = false
    private var sawRootdReady = false
    private var sawGuardOff = false
    private var sawFallback = false
    private var sawHeld = false
    private var sawScriptError = false
    private var sawKernelMismatch = false
    private var sawBaseCalFail = false
    private var sawAdaptUnsupported = false
    private var sawOldNextArmed = false
    private var sawProfileRelaxed = false
    private var sawTraceBlocked = false
    private var sawFast61Gate = false
    private var sawFast66Gate = false
    private var saw61269B = false
    private var sawRetryHolder = false

    private var result = ""
    private var resultLine = ""
    private var nosuWhy = ""
    private var gate612 = ""
    private var lastLine = ""
    private var lastFail = ""

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
        sawKsuLoaded = false
        sawRootdReady = false
        sawGuardOff = false
        sawFallback = false
        sawHeld = false
        sawDeath = false
        sawScriptError = false
        sawKernelMismatch = false
        sawBaseCalFail = false
        sawAdaptUnsupported = false
        sawOldNextArmed = false
        sawProfileRelaxed = false
        sawTraceBlocked = false
        sawFast61Gate = false
        sawFast66Gate = false
        saw61269B = false
        sawRetryHolder = false
        result = ""
        resultLine = ""
        nosuWhy = ""
        gate612 = ""
        lastLine = ""
        lastFail = ""
        lastPct = 0
        lastLabel = "准备"
        val t = Thread({ runInternal(app) }, "kmroot-fast").apply { isDaemon = true }
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

    private fun buildCmd(dir: String, mgr: String, mode: String, nd: String?, assets: String): String {
        var cmd = "KMROOT_DIR='$dir' KMROOT_MGR='$mgr' KMROOT_MODE='$mode' KMROOT_ASSETS='$assets'"
        if (nd != null) cmd += " KMROOT_ND='$nd'"
        return "$cmd sh '$assets/trrun.sh'"
    }

    private fun runInternal(context: Context) {
        try {
            stage(2, "检查 Shizuku")
            if (!KmShizuku.waitReady(context, 12000) { log(it) }) {
                finish(false, "shell 服务未就绪：${KmShizuku.describe(context, KmShizuku.current(context))}")
                return
            }

            stage(6, "部署载荷")
            val probe = safe(
                KmShell.run(
                    "printf '%s\\n' \"\$(getprop ro.product.model)\" \"\$(uname -r)\" \"\$(getprop ro.build.display.id)\" \"\$(getenforce)\" " +
                        "\"\$(grep -i '^kernelsu ' /proc/modules | head -n1)\" " +
                        "\"\$( { test -w /sys/kernel/tracing/events/kmem/mm_page_alloc/enable && test -w /sys/kernel/tracing/events/kmem/mm_page_free/enable; } && echo KMEM_OK || echo KMEM_DENIED)\" " +
                        "\"\$(cut -d. -f1 /proc/uptime)\" \"\$( [ -f /sdcard/kmroot-force ] && echo FORCE || echo NOFORCE)\"",
                    20
                )
            )
            val parts = probe.split("\n")
            val model = parts.getOrNull(0)?.trim() ?: ""
            val kernel = parts.getOrNull(1)?.trim() ?: ""
            log("机型: $model · 内核: $kernel")
            if (parts.size > 2) log("${parts[2].trim()} · ${parts.getOrNull(3)?.trim() ?: ""}")
            if (parts.size > 4 && parts[4].trim().isNotEmpty()) log("已加载: ${parts[4].trim()}")
            if (parts.size > 5) log("tracefs: ${parts[5].trim()}")

            // 开机稳定门：uptime < 100s 时等待（抢页命中的关键），支持 /sdcard/kmroot-force 跳过
            try {
                var uptime = parts.getOrNull(6)?.trim()?.toInt() ?: 0
                val force = parts.getOrNull(7)?.trim() == "FORCE"
                when {
                    force -> log("环境门：检测到 /sdcard/kmroot-force → 直接开跑")
                    uptime >= 100 -> log("环境门：uptime=${uptime}s（≥100s）→ 系统已稳定，直接开跑")
                    else -> {
                        val t0 = System.currentTimeMillis()
                        while (uptime < 100 && System.currentTimeMillis() - t0 < 110000 && !done) {
                            log("刚开机（uptime=${uptime}s < 100s），等系统稳定再跑…")
                            Thread.sleep(3000)
                            try {
                                uptime = safe(KmShell.run("cut -d. -f1 /proc/uptime", 8)).trim().split("\n")[0].trim().toInt()
                            } catch (_: Throwable) {}
                        }
                        log("环境门：开跑（uptime=${uptime}s）")
                    }
                }
            } catch (_: Throwable) {}

            val mgr = KmMgr.pkg(context)
            val mode = KmCfg.mode(context)
            log("提权模式: ${if (mode == "slow") "慢速 98260（兼容模式）" else "快速（自动）"}")
            val installed = KmMgr.installed(context, mgr)
            log("ROOT 管理器: ${KmMgr.displayName(context, mgr)}（$mgr）${if (installed) " 已安装" else " 未安装"}")

            val dir = KmCfg.resolveDev(context)
            if (KmCfg.lastResolveError.isNotEmpty()) {
                log((if (KmCfg.ndMode) "工作目录: " else "工作目录探测失败: ") + KmCfg.lastResolveError)
            }

            // 从 KuaiMaoSU 自身 APK 提取带全套 ko 的 ksud
            val ksudInfo = KmCfg.deployKsud(mgr)
            if (ksudInfo.isNotEmpty()) log("ksud: $ksudInfo")

            val copyResult = safe(KmShell.run(KmCfg.copyScript(context, kernel, installed), 240))
            if (copyResult.trim().isNotEmpty()) log(copyResult.trim())
            if (copyResult.contains("COPY_FAIL")) {
                val diag = safe(KmShell.run("id; getenforce; ls -ld /data /data/local /data/local/tmp 2>&1 | head -n 6; df -h /data 2>/dev/null | tail -n 2", 20)).trim()
                log("落地诊断："); log(diag)
                finish(false, "载荷落地失败：目标目录建不出来/写不进（看上面「落地诊断」的目录属主与 SELinux）")
                return
            }
            val deploySkip = copyResult.contains("DEPLOY_SKIP")
            if (!KmCfg.ndMode && !deploySkip) {
                val verify = safe(KmShell.run(KmCfg.verifyScript(kernel, installed), 60))
                log("载荷校验："); log(verify.trim())
                if (verify.contains("size:0 ")) {
                    finish(false, "载荷落地失败（nativeLibraryDir 里缺文件）")
                    return
                }
            }

            stage(18, "启动引擎")
            val assetsDir = KmCfg.ensureAssets(context, kernel)
            if (KmCfg.assetsError.isNotEmpty()) {
                finish(false, "设备端脚本落地失败：${KmCfg.assetsError}")
                return
            }
            log("脚本目录: $assetsDir")

            val argv = arrayOf("sh", "-c", buildCmd(dir, mgr, mode, if (KmCfg.ndMode) KmCfg.nativeDir(context) else null, assetsDir))
            val process = KmShell.spawn(argv, dir)
            live = process
            lastLineAt = System.currentTimeMillis()
            val onLineCb: (String) -> Unit = { onLine(it) }
            process.startReaders(onLineCb, onLineCb)

            val budgetMs = (if (mode == "slow") 45 else 16) * 60 * 1000L
            if (mode == "slow") log("慢速链预算 ${budgetMs / 60000} 分钟")
            val deadline = System.currentTimeMillis() + budgetMs
            while (System.currentTimeMillis() < deadline && !done) {
                Thread.sleep(400)
                if (!process.isAlive() && System.currentTimeMillis() - lastLineAt > 8000) {
                    sawDeath = true
                    break
                }
            }
            if (!earlyOk) try { live?.kill() } catch (_: Throwable) {}
            if (settled) return

            if (!done) {
                val tail = safe(KmShell.run("tail -n 20 '${KmCfg.dev}/kmfast.log' 2>/dev/null; tail -n 20 '${KmCfg.dev}/plant.log' 2>/dev/null", 20))
                if (tail.trim().isNotEmpty()) log(tail.trim())
            }

            stage(94, "验证结果")
            var module = safe(KmShell.run("grep -E '^kernelsu ' /proc/modules | head -n1", 15)).trim()
            if (module.isEmpty() && sawDeath) {
                var i = 0
                while (i < 20) {
                    Thread.sleep(1500); i++
                    module = safe(KmShell.run("grep -E '^kernelsu ' /proc/modules | head -n1", 5)).trim()
                    if (module.isNotEmpty()) break
                }
            }
            var su = ""
            val suOut = safe(KmShell.run("su -c id 2>&1", 20)).trim()
            if (!suOut.contains("inaccessible") && !suOut.contains("not found")) su = suOut
            log("模块: ${if (module.isEmpty()) "未加载" else module}")
            log("su: ${if (su.isEmpty()) "不可用" else su}")

            judgeResult(module, su)
        } catch (t: Throwable) {
            try {
                if (live != null && !earlyOk) live?.kill()
            } catch (_: Throwable) {}
            finish(false, "异常: $t")
        }
    }

    private fun judgeResult(module: String, su: String) {
        when (result) {
            "ROOT", "ALREADY" -> {
                finish(true, if (result == "ALREADY") "KernelSU 已就绪（本机本来就加载着，无需重复越狱）"
                            else "越狱成功：临时 root 已就绪，KernelSU 已接管")
                return
            }
        }
        when (result) {
            "NOSU" -> {
                if (module.isEmpty()) {
                    finish(false, if (nosuWhy.isEmpty()) "模块已加载但 su 不可用：重新点一次；仍不行就冷重启手机后重试"
                                else "模块已加载但 su 不可用 —— $nosuWhy")
                } else {
                    finish(true, "越狱成功：KernelSU 模块已加载（su 暂不可用，重启后生效）")
                }
                return
            }
            "NOMGR" -> { finish(false, "所选管理器没装：先装好它，否则无法 late-load。"); return }
            "ENVFAIL" -> { finish(false, "系统负载太高，这一轮没有开跑（保护设备）。关掉后台应用 / 等几分钟再试；确实要跑可在 /sdcard 建 kmroot-force 强制"); return }
            "DEPLOYFAIL" -> { finish(false, "引擎文件不完整（工作目录载荷没拷全）。重装 APK / 删掉工作目录后重试"); return }
            "UNSUPPORTED" -> {
                val msg = when {
                    gate612.isNotEmpty() -> "本机是 $gate612，与 6.12.69 不是同一内核布局，引擎偏移按 6.12.69 编译，硬套会野写，故未开跑（安全）"
                    saw61269B -> "本机是 6.12.69 的第二个 OTA 构建，内置引擎按第一份构建编译，base 校准不过属预期"
                    sawProfileRelaxed -> "这台机内核没有精确档案，本轮借用同系列档案试跑，失败属预期"
                    sawFast66Gate -> "6.6 内核（$lastLine）没有匹配档案，引擎没启动（安全）"
                    sawFast61Gate -> "6.1 内核没有匹配档案，引擎没启动（安全）"
                    sawTraceBlocked -> "这台机的 ROM 不给 shell 开 tracefs 的 kmem 事件（权限门槛），引擎拿不到 page 事件"
                    sawAdaptUnsupported -> "内核 build 需要引擎侧适配：两条 6.12 引擎都在 base 校准阶段失败，重试无意义"
                    sawKernelMismatch -> "内核串不在慢速 98260 兼容链档案里，重试无意义。切回「快速」再试"
                    else -> "内核不在快速引擎已验证名单里，也没有匹配档案。可切「慢速 98260」再试"
                }
                finish(false, msg); return
            }
            "TRACEBLOCK" -> { finish(false, "ROM 没给 shell 开 tracefs 的 kmem 事件，快速引擎无法工作。可切「慢速 98260」兼容模式再试"); return }
            "FASTFAIL" -> {
                val msg = when {
                    sawRetryHolder -> "快速引擎这次没中，但已把持页交给 holder 进程 → 同一 boot 直接再点一次即可，不必重启"
                    sawBaseCalFail && sawOldNextArmed -> "内核 build 让新引擎 base 校准失败 —— 冷重启手机后再点一次就会自动改试老引擎"
                    sawBaseCalFail -> "内核 build 让引擎在 base 校准阶段就失败（[base] best=0 count=0），引擎还没适配这个 build"
                    else -> "快速引擎没有成功（当前「快速」模式不会自动回退）。重启后再点；多次失败可切「慢速 98260」"
                }
                finish(false, msg); return
            }
            "PARTIAL" -> { finish(false, "已拿到临时 root（uid=0），但 KernelSU 没起来：检查管理器是否装好，或重启后重试"); return }
            "HELD", "PARKED", "REBOOT" -> { finish(false, "失败，请手动重启手机后再尝试"); return }
        }
        if (su.contains("uid=0") || module.isNotEmpty()) {
            finish(true, "越狱成功：KernelSU 模块已加载（su ${if (su.isEmpty()) "稍后才能用" else "uid=0"}）")
        } else {
            finish(false, failReason())
        }
    }

    private fun failReason(): String = when {
        sawKernelMismatch -> "内核串不在慢链适配档案里（kernel mismatch），重试无意义；切「快速」或把机型+内核串反馈适配"
        sawKsuLoaded && sawHeld -> "KernelSU 已 late-load，但引擎持页（parking without reap）：冷重启手机后再点一次"
        sawHeld -> "引擎已持页：这次开机不能再试，必须冷重启手机后再点"
        sawScriptError -> "设备端脚本损坏：重新打开 App 会自动重写脚本；若仍出现，卸载重装"
        sawDeath && resultLine.isEmpty() -> "运行中断：脚本进程提前结束（多半是 shell 服务被系统回收）。重新点一次即可"
        result == "FAIL" -> "未拿到 root${if (lastFail.isEmpty()) "" else "：$lastFail"}"
        resultLine.isNotEmpty() -> "未成功：$resultLine"
        lastFail.isNotEmpty() -> "未成功：$lastFail"
        else -> "未拿到临时 root：${if (lastLine.isEmpty()) "运行中断" else lastLine}"
    }

    /** 逐行日志 → 进度状态机 + 标志位。移植自 Engine.onLine。 */
    fun onLine(raw: String?) {
        lastLineAt = System.currentTimeMillis()
        log(raw ?: "")
        val s = raw?.trim() ?: ""
        if (s.isEmpty()) return
        lastLine = s

        when {
            s.startsWith("KERNEL=") -> stage(20, "内核分流")
            s.startsWith("MGR_READY") || s.startsWith("MGR_INSTALL") -> stage(22, "管理器就绪")
            s.startsWith("KSUD=") -> stage(24, "ksud 就绪")
            s.contains("engine=kmfast61") -> stage(30, "快速引擎 · 6.1")
            s.contains("engine=km612s") -> stage(30, "快速引擎 · 6.12")
            s.contains("engine=km612") -> stage(30, "快速引擎 · 6.12")
            s.contains("engine=kmfast") -> stage(30, "快速引擎 · 6.6")
            s.contains("FAST_PROFILE") -> stage(26, "加载内核档案")
            s.startsWith("[1/5]") -> stage(26, "probe")
            s.contains("PROBE_OK") -> stage(30, "probe 通过")
            s.startsWith("[2/5]") -> stage(34, "解算 slide")
            s.startsWith("SAMPLE_ROUND") -> stage(38, "采样 KASLR")
            s.startsWith("SLIDE_READY") -> stage(46, "slide 就绪")
            s.startsWith("[3/5]") || s.startsWith("PLANT_PID") -> stage(50, "触发 UAF")
            s.contains("ROOTD_LISTEN") -> stage(58, "rootd 已监听")
            s.contains("ROOTD_READY") -> stage(66, "临时 root 就绪")
            s.contains("GUARD_ANTIROOT_OFF") -> stage(70, "压制反 root 守卫")
            s.startsWith("LATE_LOAD") -> stage(76, "管理器 late-load")
            s.startsWith("KSU_LOADED") -> stage(88, "KernelSU 已加载")
            s.startsWith("MODULE_LOADED") -> stage(90, "模块已加载")
            s.startsWith("SU_OK") -> stage(94, "su 可用")
            s.startsWith("GUARD_RESTORED") -> stage(96, "收尾")
            s.startsWith("MGR_START") || s.startsWith("FALLBACK") -> stage(92, "收尾")
        }

        if (s == "KSU_LOADED") sawKsuLoaded = true
        if (s.startsWith("ROOTD_READY")) sawRootdReady = true
        if (s.contains("GUARD_ANTIROOT_OFF")) sawGuardOff = true
        if (s.startsWith("FALLBACK")) sawFallback = true
        if (s.startsWith("PLANT_PARKED") || s.startsWith("FAST_HELD") || s.startsWith("ENGINE_HOLD") ||
            s.startsWith("PARKED=1") || s.contains("parking") || s.contains("page held")) sawHeld = true
        if (s.contains("syntax error") || s.contains("inaccessible or not found") ||
            Regex(".*trrun\\.sh\\[\\d+\\]:.*not found.*").matches(s)) sawScriptError = true
        if (s.contains("REASON=kernel mismatch") || s.startsWith("KERNEL_MISMATCH")) sawKernelMismatch = true
        if (s.contains("ENGINE_BASECAL_FAIL") || s.contains("base calibration failed")) sawBaseCalFail = true
        if (s.contains("ADAPT_ENGINE_UNSUPPORTED")) sawAdaptUnsupported = true
        if (s.contains("TRACE_BLOCKED") || s.contains("TRACE_BLOCK_KIND")) sawTraceBlocked = true
        if (s.contains("FAST61_GATE")) sawFast61Gate = true
        if (s.contains("FAST_GATE")) sawFast66Gate = true
        if (s.contains("PROFILE_RELAXED")) sawProfileRelaxed = true
        if (s.contains("ENGINE_WARN_61269B")) saw61269B = true
        if (s.contains("ENGINE_GATE_61238")) gate612 = "6.12.38"
        else if (s.contains("ENGINE_GATE_61258")) gate612 = "6.12.58"
        else if (s.contains("ENGINE_GATE_61223")) gate612 = "6.12.23"
        if (s.contains("ENGINE_OLDNEXT_ARMED")) sawOldNextArmed = true
        if (s.contains("ENGINE_WARN_RETRYHOLDER") || s.contains("RETRY_HOLDER")) sawRetryHolder = true
        if (s.startsWith("NOSU_WHY:")) nosuWhy = s.substring(9).trim()
        if (s.startsWith("FAIL=") || s.startsWith("ENGINE_FAIL") || s.startsWith("DEPLOY_FAIL")) lastFail = s

        if (s.startsWith("RESULT=")) {
            result = s.substring(7).trim()
            if (result == "ROOT" || result == "ALREADY") {
                earlyOk = true
                finish(true, if (result == "ALREADY") "KernelSU 已就绪（本机本来就加载着，无需重复越狱）"
                            else "越狱成功：临时 root 已就绪，KernelSU 已接管")
            }
            resultLine = s
        }
        if (s == "DONE" || s.startsWith("DEPLOY_FAIL")) done = true
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
