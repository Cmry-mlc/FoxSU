package com.kuaimaosu.manager.data.susfs

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.kuaimaosu.manager.data.shell.KsuCliRepository

/**
 * SUSFS 守卫内核模块（susfs_guard_lkm）的加载与自动加载。
 *
 * 只做一件事：把 ksud 里内嵌的 `{kmi}_susfs_guard_lkm.ko` 加载进内核。
 * 内核已经内建 SUSFS 时不需要它，[LkmState.available] 会告诉界面这一情况。
 */
class SusfsLkmRepository(
    private val ksuCliRepository: KsuCliRepository,
) {
    companion object {
        private const val TAG = "SusfsLkmRepository"
    }

    data class LkmState(
        val loaded: Boolean = false,
        /** 内核里已经有 SUSFS（内建或别的 LKM），此时不需要也无法再加载。 */
        val available: Boolean = false,
        val version: String? = null,
        val autoload: Boolean = false,
        val autoloadKmi: String? = null,
        val arch: String? = null,
    )

    private data class CommandResult(
        val success: Boolean,
        val stdout: String,
        val stderr: String,
    )

    // 主页大卡的「SUSFS LKM 模式」标签直接跟着这个流走：
    // 加载 / 卸载成功、或者任何一次 status() 刷新都会让它更新，
    // 不需要主页自己去轮询。
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    suspend fun status(): LkmState = withContext(Dispatchers.IO) {
        val result = execute("lkm status")
        if (!result.success) {
            Log.w(TAG, "lkm status failed: ${result.stderr}")
            return@withContext LkmState()
        }
        val values = result.stdout.lineSequence()
            .mapNotNull { line ->
                val index = line.indexOf('=')
                if (index <= 0) null else line.substring(0, index) to line.substring(index + 1)
            }
            .toMap()
        LkmState(
            loaded = values["loaded"] == "1",
            available = values["available"] == "1",
            version = values["version"]?.takeIf { it.isNotBlank() },
            autoload = values["autoload"] == "1",
            autoloadKmi = values["autoload_kmi"]?.takeIf { it.isNotBlank() },
            arch = values["arch"]?.takeIf { it.isNotBlank() },
        ).also { _loaded.value = it.loaded }
    }

    suspend fun load(kmi: String? = null): Result<String> = executeChecked(
        buildString {
            append("lkm load")
            if (!kmi.isNullOrBlank()) append(" --kmi ").append(shellQuote(kmi))
        },
    ).also { if (it.isSuccess) _loaded.value = true }

    suspend fun unload(): Result<String> = executeChecked("lkm unload")
        .also { if (it.isSuccess) _loaded.value = false }

    suspend fun setAutoload(enabled: Boolean, kmi: String?): Result<String> = executeChecked(
        if (enabled) {
            buildString {
                append("lkm autoload enable")
                if (!kmi.isNullOrBlank()) append(" --kmi ").append(shellQuote(kmi))
            }
        } else {
            "lkm autoload disable"
        },
    )

    /** ksud 里真正打包了模块的 KMI，用来给用户手选。 */
    suspend fun supportedKmi(): List<String> = withContext(Dispatchers.IO) {
        val result = execute("lkm list")
        if (!result.success) {
            Log.w(TAG, "lkm list failed: ${result.stderr}")
            return@withContext emptyList()
        }
        result.stdout.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    private suspend fun executeChecked(args: String): Result<String> = withContext(Dispatchers.IO) {
        val result = execute(args)
        if (result.success) {
            Result.success(result.stdout.ifBlank { result.stderr })
        } else {
            val message = result.stderr.ifBlank { result.stdout }.ifBlank { "ksud susfs $args failed" }
            Log.e(TAG, "ksud susfs $args failed: $message")
            Result.failure(IllegalStateException(message))
        }
    }

    private suspend fun execute(args: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            val stdout = ArrayList<String>()
            val stderr = ArrayList<String>()
            val result = ksuCliRepository.withNewRootShell {
                newJob()
                    .add("${shellQuote(ksuCliRepository.getKsuDaemonPath())} susfs $args")
                    .to(stdout, stderr)
                    .exec()
            }
            CommandResult(
                success = result.isSuccess,
                stdout = stdout.joinToString("\n").trim(),
                stderr = stderr.joinToString("\n").trim(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute ksud susfs $args", e)
            CommandResult(false, "", e.message.orEmpty())
        }
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\"'\"'")}'"
}
