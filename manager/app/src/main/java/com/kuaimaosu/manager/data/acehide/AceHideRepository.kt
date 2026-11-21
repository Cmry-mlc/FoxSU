package com.kuaimaosu.manager.data.acehide

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kuaimaosu.manager.data.shell.KsuCliRepository

/**
 * eBPF 控制台：raw_syscalls hook getdents64，隐藏目录里名字含关键字的条目。
 *
 * ace_hide 是一份随包的 arm64 静态 ELF（libace.so）。本仓库只做三件事：
 *  1. 把二进制部署到 /data/local/tmp/ace_hide 并 chmod 755；
 *  2. 以 root 启动/停止它（二进制内部 double-fork 成常驻守护进程，写 pid 到
 *     /data/adb/ace_hide.pid、日志到 /data/adb/ace_hide.log）；
 *  3. 读 pid / 日志，并在 App 内用探针目录自证隐藏是否真的生效。
 *
 * 关键字持久化在 SharedPreferences；隐藏本身完全可回滚（停止即 detach）。
 */
class AceHideRepository(
    private val context: Context,
    private val ksuCliRepository: KsuCliRepository,
) {
    companion object {
        private const val TAG = "AceHideRepository"
        private const val PREFS = "acehide_prefs"
        private const val KEY_KEYWORD = "keyword"
        private const val BIN = "/data/local/tmp/ace_hide"
        const val PID_FILE = "/data/adb/ace_hide.pid"
        const val LOG_FILE = "/data/adb/ace_hide.log"
        const val DEFAULT_KEYWORD = "ksu"
    }

    data class AceState(
        val running: Boolean = false,
        val pid: Int = -1,
        val keyword: String = DEFAULT_KEYWORD,
        val log: String = "",
        val proof: String = "",
    )

    private data class CommandResult(val success: Boolean, val stdout: String, val stderr: String)

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var keyword: String
        get() = prefs.getString(KEY_KEYWORD, DEFAULT_KEYWORD)?.takeIf { it.isNotBlank() } ?: DEFAULT_KEYWORD
        set(value) = prefs.edit().putString(KEY_KEYWORD, value).apply()

    suspend fun status(): AceState = withContext(Dispatchers.IO) {
        val pid = readPid()
        val alive = pid > 0 && shell("kill -0 $pid 2>/dev/null && echo ALIVE").contains("ALIVE")
        AceState(
            running = alive,
            pid = if (alive) pid else -1,
            keyword = keyword,
            log = readLog(),
        )
    }

    /** 确保二进制已部署且可执行。返回是否成功。 */
    private suspend fun ensureBinary(): Boolean = withContext(Dispatchers.IO) {
        try {
            val src = ksuCliRepository.getAceHidePath()
            val result = exec(
                "cp -f '$src' '$BIN' 2>/dev/null; chmod 755 '$BIN' 2>/dev/null; " +
                    "[ -x '$BIN' ] && echo OK || echo FAIL"
            )
            result.success && result.stdout.contains("OK")
        } catch (e: Exception) {
            Log.e(TAG, "ensureBinary failed", e)
            false
        }
    }

    suspend fun start(rawKw: String): Result<String> = withContext(Dispatchers.IO) {
        val kw = rawKw.trim()
        if (kw.isEmpty() || kw.length > 28 || kw.any { it.isWhitespace() }) {
            return@withContext Result.failure(IllegalArgumentException("关键字需为 1-28 字符且不含空格"))
        }
        keyword = kw

        // 1. 部署二进制
        if (!ensureBinary()) {
            return@withContext Result.failure(IllegalStateException("ace_hide 二进制部署失败，请确认 root 可用"))
        }

        // 2. 先清理旧守护进程，保证自证基线在无 hook 状态采集；清掉旧探针
        //    注意 Kotlin 字符串里 shell 的 $ 必须写 ${'$'} 避免被当模板变量
        exec(
            "kill ${'$'}(cat $PID_FILE 2>/dev/null) 2>/dev/null; " +
                "sleep 1; rm -f $PID_FILE $LOG_FILE; " +
                "rm -rf /data/local/tmp/acehide_probe_${kw}_a /data/local/tmp/acehide_probe_${kw}_b"
        )

        // 3. 采集基线：建 2 个含关键字的探针目录，数可见数
        exec("mkdir -p /data/local/tmp/acehide_probe_${kw}_a /data/local/tmp/acehide_probe_${kw}_b")
        val baseline = exec("ls -1 /data/local/tmp/ 2>/dev/null | grep -c -F '$kw'").stdout.trim()
        val base = baseline.toIntOrNull() ?: 2

        // 4. 启动守护进程。二进制内部 double-fork+setsid，父进程立即返回；
        //    但 libsu 的交互 shell 会等待后台 job 的 stdin/stdout，直接写 "&" 可能挂住，
        //    所以用子 shell 包一层并显式断开 fd，最后回一个 LAUNCHED 作为启动回执。
        val launched = exec("( '$BIN' '$kw' </dev/null >/dev/null 2>&1 & ) ; echo LAUNCHED")
        if (!launched.success || !launched.stdout.contains("LAUNCHED")) {
            return@withContext Result.failure(IllegalStateException("启动 ace_hide 失败：${launched.stderr.ifBlank { "无回执" }}"))
        }

        // 5. 轮询日志直到 attach 完成（最多 ~8s）
        var attached = false
        var log = ""
        repeat(20) {
            kotlinx.coroutines.delay(400)
            log = readLog()
            if (log.contains("隐藏已生效") || log.contains("已 attach raw tracepoint exit")) {
                attached = true
                return@repeat
            }
            if (log.contains("BPF_PROG_LOAD") && log.contains("失败") ||
                log.contains("BPF_RAW_TRACEPOINT_OPEN") && log.contains("失败")) {
                return@repeat
            }
        }

        val pid = readPid()
        val after = exec("ls -1 /data/local/tmp/ 2>/dev/null | grep -c -F '$kw'").stdout.trim()
            .toIntOrNull() ?: -1
        val proof = buildProof(base, after, kw)

        if (pid <= 0) {
            return@withContext Result.failure(IllegalStateException("守护进程未存活（见日志）\n$proof"))
        }
        if (!attached) {
            return@withContext Result.failure(IllegalStateException("hook 未确认 attach 成功（见日志）\n$proof"))
        }
        Result.success("隐藏已生效 · 守护进程常驻 (pid=$pid)\n$proof")
    }

    suspend fun stop(): Result<String> = withContext(Dispatchers.IO) {
        val pid = readPid()
        val r = exec(
            "if [ -n '$pid' ] && [ '$pid' -gt 0 ] 2>/dev/null; then kill '$pid' 2>/dev/null; fi; " +
                "sleep 1; rm -f $PID_FILE; echo DONE"
        )
        if (r.success) Result.success("已停止，hook 已解除") else Result.failure(IllegalStateException(r.stderr.ifBlank { "停止失败" }))
    }

    /** App 内自证：对比启用前后含关键字的目录项数量。 */
    private fun buildProof(before: Int, after: Int, kw: String): String = buildString {
        appendLine("=== 隐藏效果自证（App 内自动完成）===")
        appendLine("探针: /data/local/tmp/ 下含 \"$kw\" 的目录")
        appendLine("启用前可见数: $before   启用后可见数: $after")
        when {
            before >= 2 && after < before ->
                appendLine("结论: [生效] 启用后 ${before - after} 个探针目录从 ls 中消失，getdents64 已被 hook 过滤")
            before >= 2 && after == before ->
                appendLine("结论: [未生效] 探针目录仍全部可见，检查日志是否 attach 成功")
            else ->
                appendLine("结论: [无法判定] 基线异常（启用前应为 2），换简单关键字重试")
        }
    }

    private suspend fun readPid(): Int = withContext(Dispatchers.IO) {
        exec("cat $PID_FILE 2>/dev/null").stdout.trim().toIntOrNull() ?: -1
    }

    private suspend fun readLog(): String = withContext(Dispatchers.IO) {
        exec("cat $LOG_FILE 2>/dev/null").stdout.trim()
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
