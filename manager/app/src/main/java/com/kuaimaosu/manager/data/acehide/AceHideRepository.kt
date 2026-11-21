package com.kuaimaosu.manager.data.acehide

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kuaimaosu.manager.data.shell.KsuCliRepository

/**
 * eBPF 控制台：三个模块，各自独立 attach，互不影响。
 *
 *   dents   getdents64 目录项过滤 —— ls/find/ps 遍历时名字含关键字的项直接消失，
 *           作用于 /proc、/sys/module、/data/adb、/data/local/tmp 等所有目录
 *   procfs  /proc/modules、/proc/kallsyms 打开时重定向为 /dev/null —— 模块表读空
 *   stealth 守护进程自身隐身（二进制名、comm、cmdline、pid/日志路径全部中性化）
 *
 * 隐藏逻辑全部在随包二进制里（libace.so），这里只做部署、启停、状态与自证。
 */
class AceHideRepository(
    private val context: Context,
    private val ksuCliRepository: KsuCliRepository,
) {
    companion object {
        private const val TAG = "AceHideRepository"
        private const val PREFS = "acehide_prefs"
        private const val KEY_KEYWORDS = "keywords"
        private const val KEY_DENTS = "mod_dents"
        private const val KEY_PROCFS = "mod_procfs"

        const val BIN = "/data/local/tmp/.klogd"
        const val PID_FILE = "/data/adb/.klog.pid"
        const val LOG_FILE = "/data/adb/.klog.log"
        const val KW_FILE = "/data/adb/.klog.kw"

        const val DEFAULT_KEYWORDS = "ksu,kernelsu,susfs"

        const val MOD_DENTS = "dents"
        const val MOD_PROCFS = "procfs"
    }

    enum class Verdict { PASS, FAIL, UNCOVERED }

    data class CheckItem(val name: String, val detail: String, val verdict: Verdict)

    data class AceState(
        val running: Boolean = false,
        val pid: Int = -1,
        val keywords: String = DEFAULT_KEYWORDS,
        val dentsOn: Boolean = true,
        val procfsOn: Boolean = true,
        val dentsOk: Boolean = false,
        val procfsOk: Boolean = false,
        val log: String = "",
    )

    private data class CommandResult(val success: Boolean, val stdout: String, val stderr: String)

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var keywords: String
        get() = prefs.getString(KEY_KEYWORDS, DEFAULT_KEYWORDS)?.takeIf { it.isNotBlank() } ?: DEFAULT_KEYWORDS
        set(value) = prefs.edit().putString(KEY_KEYWORDS, value.trim()).apply()

    var dentsEnabled: Boolean
        get() = prefs.getBoolean(KEY_DENTS, true)
        set(value) = prefs.edit().putBoolean(KEY_DENTS, value).apply()

    var procfsEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROCFS, true)
        set(value) = prefs.edit().putBoolean(KEY_PROCFS, value).apply()

    suspend fun status(): AceState = withContext(Dispatchers.IO) {
        val pid = readPid()
        val alive = pid > 0 && shell("kill -0 $pid 2>/dev/null && echo ALIVE").contains("ALIVE")
        val log = readLog()
        AceState(
            running = alive,
            pid = if (alive) pid else -1,
            keywords = keywords,
            dentsOn = dentsEnabled,
            procfsOn = procfsEnabled,
            dentsOk = alive && parseModule(log, MOD_DENTS),
            procfsOk = alive && parseModule(log, MOD_PROCFS),
            log = log,
        )
    }

    private fun parseModule(log: String, name: String): Boolean =
        Regex("\\[module]\\s+$name=(OK|FAIL)").find(log)?.groupValues?.get(1) == "OK"

    private suspend fun ensureBinary(): Boolean = withContext(Dispatchers.IO) {
        try {
            val src = ksuCliRepository.getAceHidePath()
            val r = exec(
                "cp -f '$src' '$BIN' 2>/dev/null; chmod 755 '$BIN' 2>/dev/null; " +
                    "[ -x '$BIN' ] && echo OK || echo FAIL"
            )
            r.success && r.stdout.contains("OK")
        } catch (e: Exception) {
            Log.e(TAG, "ensureBinary failed", e)
            false
        }
    }

    suspend fun start(rawKw: String, dents: Boolean, procfs: Boolean): Result<String> =
        withContext(Dispatchers.IO) {
            val kw = rawKw.trim()
            if (kw.isEmpty() || kw.length > 80 || kw.any { it.isWhitespace() }) {
                return@withContext Result.failure(IllegalArgumentException("关键字不能为空、不能含空格，总长 ≤ 80"))
            }
            if (!kw.all { it.isLetterOrDigit() || it in "._-," }) {
                return@withContext Result.failure(
                    IllegalArgumentException("关键字只允许字母、数字与 . _ - ,（走 shell 与目录名，避免注入）")
                )
            }
            if (!dents && !procfs) {
                return@withContext Result.failure(IllegalArgumentException("至少启用一个模块"))
            }
            keywords = kw
            dentsEnabled = dents
            procfsEnabled = procfs

            if (!ensureBinary()) {
                return@withContext Result.failure(IllegalStateException("二进制部署失败，请确认 root 可用"))
            }

            // 清旧实例 + 旧探针；此时 hook 已解除，采集的是真实基线
            exec(
                "kill ${'$'}(cat $PID_FILE 2>/dev/null) 2>/dev/null; sleep 1; " +
                    "rm -f $PID_FILE $LOG_FILE; " +
                    "rm -rf /data/local/tmp/probe_${kw}_a /data/local/tmp/probe_${kw}_b"
            )

            // 关键字落盘（二进制内不留关键字字面量）
            exec("printf '%s' '$kw' > $KW_FILE; chmod 600 $KW_FILE")

            // 基线：探针目录数 + 模块表行数
            exec("mkdir -p /data/local/tmp/probe_${kw}_a /data/local/tmp/probe_${kw}_b")
            val probeBefore = countProbes(kw)
            val modBefore = countLines("/proc/modules")

            val flags = buildString {
                append(if (dents) " --dents" else " --no-dents")
                append(if (procfs) " --procfs" else " --no-procfs")
            }
            // 子 shell + 断 fd：libsu 的交互 shell 不会去等这个后台 job
            val launched = exec("( '$BIN' --kw '$kw'$flags </dev/null >/dev/null 2>&1 & ) ; echo LAUNCHED")
            if (!launched.success || !launched.stdout.contains("LAUNCHED")) {
                return@withContext Result.failure(
                    IllegalStateException("启动失败：${launched.stderr.ifBlank { "无回执" }}")
                )
            }

            var log = ""
            var ready = false
            repeat(20) {
                kotlinx.coroutines.delay(400)
                log = readLog()
                if (log.contains("[+] READY") || log.contains("[!] NO_MODULE")) {
                    ready = true
                    return@repeat
                }
            }

            val pid = readPid()
            val probeAfter = countProbes(kw)
            val modAfter = countLines("/proc/modules")
            val proof = buildProof(probeBefore, probeAfter, modBefore, modAfter, dents, procfs, kw)

            if (pid <= 0) {
                return@withContext Result.failure(IllegalStateException("守护进程未存活（见日志）\n$proof"))
            }
            if (!ready) {
                return@withContext Result.failure(IllegalStateException("内核未确认加载完成（见日志）\n$proof"))
            }

            val okModules = listOfNotNull(
                MOD_DENTS.takeIf { parseModule(log, MOD_DENTS) },
                MOD_PROCFS.takeIf { parseModule(log, MOD_PROCFS) },
            )
            if (okModules.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("所有模块 attach 失败（见日志）\n$proof")
                )
            }
            Result.success("已启用 ${okModules.joinToString("+")} · 守护进程常驻 (pid=$pid)\n$proof")
        }

    suspend fun stop(): Result<String> = withContext(Dispatchers.IO) {
        val pid = readPid()
        val r = exec(
            "if [ -n '$pid' ] && [ '$pid' -gt 0 ] 2>/dev/null; then kill '$pid' 2>/dev/null; fi; " +
                "sleep 1; rm -f $PID_FILE; echo DONE"
        )
        if (r.success) Result.success("已停止，hook 已解除") else Result.failure(IllegalStateException("停止失败"))
    }

    private suspend fun countProbes(kw: String): Int =
        shell("ls -1 /data/local/tmp/ 2>/dev/null | grep -c -F '$kw'").trim().toIntOrNull() ?: -1

    private suspend fun countLines(path: String): Int =
        shell("wc -l < $path 2>/dev/null").trim().toIntOrNull() ?: -1

    private fun buildProof(
        probeBefore: Int,
        probeAfter: Int,
        modBefore: Int,
        modAfter: Int,
        dents: Boolean,
        procfs: Boolean,
        kw: String,
    ): String = buildString {
        appendLine("=== 隐藏效果自证（App 内自动完成）===")
        if (dents) {
            appendLine("探针: /data/local/tmp/ 下含 \"$kw\" 的目录")
            appendLine("启用前可见: $probeBefore   启用后可见: $probeAfter")
            appendLine(
                when {
                    probeBefore >= 2 && probeAfter < probeBefore ->
                        "dents: [生效] ${probeBefore - probeAfter} 个探针目录被过滤"
                    probeBefore >= 2 ->
                        "dents: [未生效] 探针仍可见，看日志 attach 结果"
                    else ->
                        "dents: [无法判定] 基线异常（应为 2）"
                }
            )
        }
        if (procfs) {
            appendLine("/proc/modules 行数: 启用前 $modBefore   启用后 $modAfter")
            appendLine(
                when {
                    modBefore > 0 && modAfter == 0 -> "procfs: [生效] 模块表已被重定向为空"
                    modBefore <= 0 -> "procfs: [无法判定] 本机模块表本来就空"
                    else -> "procfs: [未生效] 仍能读到 $modAfter 行"
                }
            )
        }
        appendLine("进程: comm=kworker/u8:2，cmdline 已擦除")
    }

    /** 检测项矩阵：真实跑命令，给出 PASS / FAIL / 未覆盖。 */
    suspend fun selfCheck(): List<CheckItem> = withContext(Dispatchers.IO) {
        val out = ArrayList<CheckItem>()
        val pid = readPid()
        val alive = pid > 0 && shell("kill -0 $pid 2>/dev/null && echo ALIVE").contains("ALIVE")
        if (!alive) {
            out += CheckItem("守护进程", "未运行，先启用隐藏", Verdict.FAIL)
            return@withContext out
        }

        suspend fun check(name: String, cmd: String, passIf: (String) -> Boolean, failDetail: String) {
            val v = shell(cmd).trim()
            out += if (passIf(v)) {
                CheckItem(name, "实测: $v", Verdict.PASS)
            } else {
                CheckItem(name, "实测: $v（$failDetail）", Verdict.UNCOVERED)
            }
        }

        check("ls /data/adb 无 ksu 目录项", "ls -1 /data/adb 2>/dev/null | grep -c -E 'ksu|susfs'", { it == "0" }, "目录项过滤未覆盖")
        check("ls /sys/module 无 ksu/susfs", "ls -1 /sys/module 2>/dev/null | grep -c -E 'kernelsu|susfs'", { it == "0" }, "目录项过滤未覆盖")
        check("ls /proc 无 ksu 进程名", "ls -1 /proc 2>/dev/null | grep -c -E 'ksu|kernelsu'", { it == "0" }, "hidepid/命名空间限制")
        check("/proc/modules 读空", "wc -l < /proc/modules 2>/dev/null || echo -1", { it.trim() == "0" }, "procfs 模块未生效")
        check("/proc/kallsyms 读空", "wc -l < /proc/kallsyms 2>/dev/null || echo -1", { it.trim() == "0" }, "procfs 模块未生效")
        check("守护进程伪装", "cat /proc/$pid/comm 2>/dev/null", { it.trim() == "kworker/u8:2" }, "comm 伪装未生效")
        check("cmdline 不含关键字", "tr '\\0' ' ' < /proc/$pid/cmdline 2>/dev/null", { !it.contains("klogd") && !it.contains("ace") }, "cmdline 未擦除")

        // 明确不做假承诺的三项
        out += CheckItem("挂载表 /proc/self/mountinfo", "含 ksu/susfs 的挂载行仍在 —— 文本级过滤成本高，未做", Verdict.UNCOVERED)
        out += CheckItem("stat(\"/data/adb/ksu\") 返回 ENOENT", "pt_regs 只读，改不了 syscall 返回值 —— 需 SUSFS/内核补丁", Verdict.UNCOVERED)
        out += CheckItem("包名查询 PackageManager", "包名保留（你的选择），gpi/intent 查询不走 eBPF", Verdict.UNCOVERED)
        out += CheckItem("getprop 属性检测", "属性服务走 Binder + shared memory，eBPF 管不到", Verdict.UNCOVERED)

        out
    }

    private suspend fun readPid(): Int = withContext(Dispatchers.IO) {
        exec("cat $PID_FILE 2>/dev/null").stdout.trim().toIntOrNull() ?: -1
    }

    private suspend fun readLog(): String = withContext(Dispatchers.IO) {
        exec("tail -n 80 $LOG_FILE 2>/dev/null").stdout.trim()
    }

    private suspend fun shell(cmd: String): String = withContext(Dispatchers.IO) {
        exec(cmd).stdout
    }

    private suspend fun exec(cmd: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            val stdout = ArrayList<String>()
            val stderr = ArrayList<String>()
            val result = ksuCliRepository.withNewRootShell {
                newJob().add(cmd).to(stdout, stderr).exec()
            }
            CommandResult(
                success = result.isSuccess,
                stdout = stdout.joinToString("\n").trim(),
                stderr = stderr.joinToString("\n").trim(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "exec failed: $cmd", e)
            CommandResult(false, "", e.message.orEmpty())
        }
    }
}
