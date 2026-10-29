package me.weishu.kernelsu.ui.util

import android.os.Process
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 动态管理器（Dynamic Manager）。
 *
 * KernelSU 默认靠「包名 + APK 签名」认管理器，换了包名/签名的管理器会被判定为「未安装」。
 * 内核若开放了管理器的 appid 参数（或带 CONFIG_KSU_DEBUG 的 `ksud debug set-manager`），
 * 就可以把当前 App 直接登记成管理器，绕过包名/签名校验——也就是别的管理器里的「动态管理器」。
 *
 * 这里只做「探测 + 写入 + 回读确认」，不做任何静默修改；内核没开放接口时如实返回失败。
 */
object DynamicManager {

    /** ksu 内核模块参数：管理器 appid（uid % PER_USER_RANGE）。 */
    private const val PARAM_PATH = "/sys/module/kernelsu/parameters/ksu_debug_manager_appid"

    /** 当前 App 的 appid（内核里 manager 身份用的是 appid，不是完整 uid）。 */
    fun selfAppId(): Int = Process.myUid() % 100_000

    /** 内核是否开放了动态管理器接口。 */
    suspend fun isSupported(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val shell = getRootShell()
            val raw = ShellUtils.fastCmd(shell, "cat $PARAM_PATH 2>/dev/null").trim()
            raw.toIntOrNull() != null
        }.getOrDefault(false)
    }

    /** 读取内核当前登记的管理器 appid，读不到返回 null。 */
    suspend fun currentAppId(): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val shell = getRootShell()
            ShellUtils.fastCmd(shell, "cat $PARAM_PATH 2>/dev/null").trim().toIntOrNull()
        }.getOrNull()
    }

    /**
     * 把当前 App 登记为动态管理器。
     * 先写内核参数；参数不可写时退回 `ksud debug set-manager`（要求内核开启 CONFIG_KSU_DEBUG）。
     */
    suspend fun registerSelf(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val appid = selfAppId()
            val shell = getRootShell()

            val wrote = runCatching {
                ShellUtils.fastCmdResult(shell, "echo $appid > $PARAM_PATH")
            }.getOrDefault(false)

            if (wrote) {
                val now = ShellUtils.fastCmd(shell, "cat $PARAM_PATH 2>/dev/null").trim()
                if (now == appid.toString()) {
                    return@runCatching Result.success("已登记为动态管理器（appid $appid），重启应用后生效")
                }
            }

            // 回退：debug 命令（内核 CONFIG_KSU_DEBUG 时可用）
            val debugOk = runCatching { execKsud("debug set-manager $packageName") }.getOrDefault(false)
            if (debugOk) {
                Result.success("已通过 ksud 登记为动态管理器，重启应用后生效")
            } else {
                Result.failure(
                    IllegalStateException(
                        "当前内核未开放动态管理器接口。\n" +
                            "需要内核支持 ksu_debug_manager_appid 参数或 CONFIG_KSU_DEBUG。"
                    )
                )
            }
        }.getOrElse { Result.failure(it) }
    }
}
