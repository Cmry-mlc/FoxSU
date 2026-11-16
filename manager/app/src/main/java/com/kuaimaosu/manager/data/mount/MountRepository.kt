package com.kuaimaosu.manager.data.mount

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import com.kuaimaosu.manager.data.shell.KsuCliRepository

/**
 * 内置挂载控制器（ksud 里的 OverlayFS + Magic Mount 后端）。
 *
 * 挂载本身由 ksud 在 post-fs-data 阶段完成，这里只负责读写配置、展示状态：
 * 改后端 / 改逐模块模式都会落到 `mount_config.json`，重启后生效。
 */
class MountRepository(
    private val ksuCliRepository: KsuCliRepository,
) {
    companion object {
        private const val TAG = "MountRepository"
        const val MODE_AUTO = "auto"
        const val MODE_OVERLAY = "overlay"
        const val MODE_MAGIC = "magic"
        const val MODE_NONE = "none"
    }

    data class MountStatus(
        val enabled: Boolean = true,
        val mounted: Boolean = false,
        /** 配置里的全局后端 */
        val backend: String = MODE_AUTO,
        /** 本次开机实际生效的后端，如 hybrid(overlay=2,magic=1) */
        val applied: String = "",
        val overlaySupported: Boolean = false,
        val magicSupported: Boolean = true,
        val moduleCount: Int = 0,
        val mountCount: Int = 0,
        val metamodule: Boolean = false,
        val disabled: Boolean = false,
        val detail: String = "",
    )

    data class MountModule(
        val id: String,
        val name: String,
        val mode: String,
        val backend: String,
        val mounted: Boolean,
    )

    private data class CommandResult(
        val success: Boolean,
        val stdout: String,
        val stderr: String,
    )

    private val _status = MutableStateFlow(MountStatus())
    val status: StateFlow<MountStatus> = _status.asStateFlow()

    suspend fun refreshStatus(): MountStatus = withContext(Dispatchers.IO) {
        val result = execute("status")
        if (!result.success) {
            Log.w(TAG, "mount status failed: ${result.stderr}")
            return@withContext _status.value
        }
        val values = result.stdout.lineSequence()
            .mapNotNull { line ->
                val index = line.indexOf('=')
                if (index <= 0) null else line.substring(0, index) to line.substring(index + 1)
            }
            .toMap()
        val status = MountStatus(
            enabled = values["enabled"] == "1",
            mounted = values["mounted"] == "1",
            backend = values["backend"].orEmpty().ifBlank { MODE_AUTO },
            applied = values["applied"].orEmpty(),
            overlaySupported = values["overlay_supported"] == "1",
            magicSupported = values["magic_supported"] != "0",
            moduleCount = values["modules"]?.toIntOrNull() ?: 0,
            mountCount = values["mounts"]?.toIntOrNull() ?: 0,
            metamodule = values["metamodule"] == "1",
            disabled = values["disabled"] == "1",
            detail = values["detail"].orEmpty(),
        )
        _status.value = status
        status
    }

    suspend fun modules(): List<MountModule> = withContext(Dispatchers.IO) {
        val result = execute("module list --json")
        if (!result.success) {
            Log.w(TAG, "mount module list failed: ${result.stderr}")
            return@withContext emptyList()
        }
        runCatching {
            val array = JSONArray(result.stdout)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        MountModule(
                            id = item.optString("id"),
                            name = item.optString("name"),
                            mode = item.optString("mode", MODE_AUTO),
                            backend = item.optString("backend", MODE_AUTO),
                            mounted = item.optBoolean("mounted", false),
                        ),
                    )
                }
            }
        }.getOrElse {
            Log.w(TAG, "parse mount module list failed", it)
            emptyList()
        }
    }

    suspend fun setEnabled(enabled: Boolean): Result<Unit> =
        executeChecked("enable ${if (enabled) "true" else "false"}").map { }

    suspend fun setBackend(backend: String): Result<Unit> = executeChecked("backend $backend").map { }

    suspend fun setModuleMode(id: String, mode: String): Result<Unit> =
        executeChecked("module set ${shellQuote(id)} $mode").map { }

    /** 手动挂载：仅开机阶段有效，开机后会由 ksud 拒绝。 */
    suspend fun applyNow(): Result<String> = executeChecked("apply").map { it }

    suspend fun unmount(): Result<Unit> = executeChecked("unmount").map { }

    suspend fun resetProtection(): Result<Unit> = executeChecked("reset").map { }

    private suspend fun executeChecked(args: String): Result<String> = withContext(Dispatchers.IO) {
        val result = execute(args)
        if (result.success) {
            Result.success(result.stdout.ifBlank { "ok" })
        } else {
            val message = result.stderr.ifBlank { result.stdout }.ifBlank { "ksud mount $args failed" }
            Log.e(TAG, "ksud mount $args failed: $message")
            Result.failure(IllegalStateException(humanize(message)))
        }
    }

    /** ksud 报错是英文的，这里换成能直接给用户看的中文。 */
    private fun humanize(message: String): String = when {
        message.contains("must happen during boot") -> "挂载只能在开机阶段执行，改动重启后生效"
        message.contains("bootloop protection tripped") -> "连续多次开机未完成，挂载已被停用，请先重置防砖保护"
        message.contains("disabled by bootloop protection") -> "防砖保护已触发，挂载已停用，请先重置"
        message.contains("already ran this boot") -> "本次开机已经执行过挂载"
        message.contains("metamodule installed") -> "检测到 metamodule，内置控制器不会接管挂载"
        message.contains("unknown backend") || message.contains("unknown mode") -> "后端取值不合法"
        message.contains("module not found") -> "找不到这个模块"
        else -> message
    }

    private suspend fun execute(args: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            val stdout = ArrayList<String>()
            val stderr = ArrayList<String>()
            val result = ksuCliRepository.withNewRootShell {
                newJob()
                    .add("${shellQuote(ksuCliRepository.getKsuDaemonPath())} mount $args")
                    .to(stdout, stderr)
                    .exec()
            }
            CommandResult(
                success = result.isSuccess,
                stdout = stdout.joinToString("\n").trim(),
                stderr = stderr.joinToString("\n").trim(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute ksud mount $args", e)
            CommandResult(false, "", e.message.orEmpty())
        }
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\"'\"'")}'"
}
