package com.kuaimaosu.manager.data.zygisk

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.kuaimaosu.manager.data.shell.KsuCliRepository

/**
 * 内置 Zygisk 引擎（FolkZygisk）的状态读取与开关。
 *
 * ksud 侧命令：
 * - `zygisk status --json` → 一行 JSON（字段见 [Status]）
 * - `zygisk enable` / `zygisk disable` → 下一次启动生效
 *
 * 这套命令在 ksud 尚未编入引擎时会返回失败，界面据此显示「需更新内核组件」，
 * 不会崩，也不会误报已启用。
 */
class ZygiskRepository(
    private val ksuCliRepository: KsuCliRepository,
) {
    companion object {
        private const val TAG = "ZygiskRepository"
    }

    data class Status(
        val supported: Boolean = false,
        val enabled: Boolean = false,
        val engineAvailable: Boolean = false,
        val externalProviders: List<String> = emptyList(),
        /** unverified / starting / injected / partial / failed */
        val injectionState: String = "unverified",
        val rebootRequired: Boolean = false,
        val startupFailures: Int = 0,
        val startupBlocked: Boolean = false,
    ) {
        /** 引擎是否已在本机真正注入（本次启动）。 */
        val injected: Boolean get() = injectionState == "injected"
        /** 有外部 provider 时内置引擎会让位。 */
        val yieldedToExternal: Boolean get() = externalProviders.isNotEmpty()
    }

    private data class CommandResult(
        val success: Boolean,
        val stdout: String,
        val stderr: String,
    )

    suspend fun status(): Status = withContext(Dispatchers.IO) {
        val result = execute("status --json")
        if (!result.success || result.stdout.isBlank()) {
            Log.w(TAG, "zygisk status failed: ${result.stderr}")
            return@withContext Status()
        }
        runCatching {
            val json = JSONObject(result.stdout.trim().lines().last())
            Status(
                supported = true,
                enabled = json.optBoolean("enabled", false),
                engineAvailable = json.optBoolean("engine_available", false),
                externalProviders = json.optJSONArray("external_providers")?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it) }
                }.orEmpty(),
                injectionState = json.optString("injection_state", "unverified"),
                rebootRequired = json.optBoolean("reboot_required", false),
                startupFailures = json.optInt("startup_failures", 0),
                startupBlocked = json.optBoolean("startup_blocked", false),
            )
        }.getOrElse { error ->
            Log.w(TAG, "parse zygisk status failed: ${error.message}")
            Status()
        }
    }

    suspend fun enable(): Result<String> = executeChecked("enable")

    suspend fun disable(): Result<String> = executeChecked("disable")

    private suspend fun executeChecked(args: String): Result<String> = withContext(Dispatchers.IO) {
        val result = execute(args)
        if (result.success) {
            Result.success(result.stdout.ifBlank { result.stderr })
        } else {
            val message = result.stderr.ifBlank { result.stdout }.ifBlank { "ksud zygisk $args failed" }
            Log.e(TAG, "ksud zygisk $args failed: $message")
            Result.failure(IllegalStateException(message))
        }
    }

    private suspend fun execute(args: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            val stdout = ArrayList<String>()
            val stderr = ArrayList<String>()
            val result = ksuCliRepository.withNewRootShell {
                newJob()
                    .add("${shellQuote(ksuCliRepository.getKsuDaemonPath())} zygisk $args")
                    .to(stdout, stderr)
                    .exec()
            }
            CommandResult(
                success = result.isSuccess,
                stdout = stdout.joinToString("\n").trim(),
                stderr = stderr.joinToString("\n").trim(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute ksud zygisk $args", e)
            CommandResult(false, "", e.message.orEmpty())
        }
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\"'\"'")}'"
}
