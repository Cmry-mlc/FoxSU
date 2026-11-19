package com.kuaimaosu.manager.ui.navigation

import android.os.Parcelable
import java.util.UUID
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * Type-safe navigation keys for Navigation.
 * Each destination is a NavKey (data object/data class) and can be saved/restored in the back stack.
 */
@Serializable
sealed interface Route :
    NavKey,
    Parcelable {
    @Parcelize
    @Serializable
    data object About : Route

    /** 图标与视觉：应用图标作者、图标来源声明。 */
    @Parcelize
    @Serializable
    data object IconCredits : Route

    @Parcelize
    @Serializable
    data object OpenSourceLicense : Route

    @Parcelize
    @Serializable
    data object Credits : Route

    /** 相对上游 BakaSU 的改动清单。 */
    @Parcelize
    @Serializable
    data object Modifications : Route

    @Parcelize
    @Serializable
    data object Sulog : Route

    @Parcelize
    @Serializable
    data object Main : Route

    @Parcelize
    @Serializable
    data object Home : Route

    @Parcelize
    @Serializable
    data object SuperUser : Route

    @Parcelize
    @Serializable
    data object Module : Route

    @Parcelize
    @Serializable
    data object Settings : Route

    @Parcelize
    @Serializable
    data object SettingsRoot : Route

    @Parcelize
    @Serializable
    data object SettingsProfile : Route

    @Parcelize
    @Serializable
    data object SettingsUpdate : Route

    @Parcelize
    @Serializable
    data object SettingsAi : Route

    @Parcelize
    @Serializable
    data object SettingsTools : Route

    /** 应用语言：应用内自建的语言选择页，不再跳系统设置。 */
    @Parcelize
    @Serializable
    data object Language : Route

    @Parcelize
    @Serializable
    data object SystemInfo : Route

    @Parcelize
    @Serializable
    data object Notifications : Route

    @Parcelize
    @Serializable
    data object AppProfileTemplate : Route

    @Parcelize
    @Serializable
    data class TemplateEditor(
        val templateId: String,
        val readOnly: Boolean,
        val isCreation: Boolean = false,
    ) : Route

    @Parcelize
    @Serializable
    data class AppProfile(val uid: Int, val packageName: String) : Route

    @Parcelize
    @Serializable
    data class Install(val preselectedKernelUri: String?) : Route

    @Parcelize
    @Serializable
    data class ModuleRepoDetail(val moduleId: String, val repositoryUrl: String) : Route

    @Parcelize
    @Serializable
    data class RepositoryModules(val repositoryUrl: String) : Route

    @Parcelize
    @Serializable
    data object AddRepository : Route

    @Parcelize
    @Serializable
    data object ModuleRepo : Route

    @Parcelize
    @Serializable
    data class Flash(
        val flashType: String,
        val uris: List<String> = emptyList(),
        val currentIndex: Int = 0,
        val bootUri: String? = null,
        val lkmUri: String? = null,
        val kmi: String? = null,
        val ota: Boolean = false,
        val partition: String? = null,
        val allowShell: Boolean = false,
        val enableAdb: Boolean = false,
        val forceBackup: Boolean = false,
    ) : Route {
        companion object {
            const val TYPE_BOOT = "boot"
            const val TYPE_MODULE = "module"
            const val TYPE_MODULES = "modules"
            const val TYPE_MODULE_UPDATE = "module_update"
            const val TYPE_RESTORE = "restore"
            const val TYPE_UNINSTALL = "uninstall"

            fun boot(
                bootUri: String?,
                lkmUri: String?,
                kmi: String?,
                ota: Boolean,
                partition: String?,
                allowShell: Boolean = false,
                enableAdb: Boolean = false,
                forceBackup: Boolean = false,
            ) = Flash(
                flashType = TYPE_BOOT,
                bootUri = bootUri,
                lkmUri = lkmUri,
                kmi = kmi,
                ota = ota,
                partition = partition,
                allowShell = allowShell,
                enableAdb = enableAdb,
                forceBackup = forceBackup,
            )

            fun module(uri: String) = Flash(TYPE_MODULE, uris = listOf(uri))
            fun modules(uris: List<String>, currentIndex: Int = 0) = Flash(TYPE_MODULES, uris = uris, currentIndex = currentIndex)

            fun moduleUpdate(uri: String) = Flash(TYPE_MODULE_UPDATE, uris = listOf(uri))
            fun restore() = Flash(TYPE_RESTORE)
            fun uninstall() = Flash(TYPE_UNINSTALL)
        }
    }

    @Parcelize
    @Serializable
    data class ExecuteModuleAction(val moduleId: String, val fromShortcut: Boolean) : Route

    @Parcelize
    @Serializable
    data class ScriptRun(
        val uri: String,
        val registryId: String? = null,
        val expectedSize: Long? = null,
        val expectedSha256: String? = null,
        val offerInstall: Boolean = false,
        val displayName: String? = null,
        val visitId: String = UUID.randomUUID().toString(),
    ) : Route

    @Parcelize
    @Serializable
    data object SuSFSConfig : Route

    /** SUSFS 守卫内核模块：加载 / 卸载 / 开机自动加载。 */
    @Parcelize
    @Serializable
    data object SuSFSLkm : Route

    /** 内置挂载控制器：OverlayFS / Magic Mount 与逐模块规则。 */
    @Parcelize
    @Serializable
    data object MountController : Route

    /** 内置 Zygisk 引擎（FolkZygisk）：状态与开关。 */
    @Parcelize
    @Serializable
    data object ZygiskEngine : Route

    /** 一键越狱：KMRoot 引擎的临时提权。 */
    @Parcelize
    @Serializable
    data object KmRoot : Route

    @Parcelize
    @Serializable
    data object ThemeSettings : Route

    @Parcelize
    @Serializable
    data object UmountManager : Route

    @Parcelize
    @Serializable
    data object DynamicManager : Route

    @Parcelize
    @Serializable
    data class KernelFlash(
        val kernelUri: String,
        val selectedSlot: String?,
        val skipKsud: Boolean = false,
    ) : Route
}
