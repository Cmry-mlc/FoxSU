package me.weishu.kernelsu.ui.util

import android.content.ComponentName
import android.content.pm.PackageManager
import me.weishu.kernelsu.ksuApp

/**
 * Switches the launcher icon by toggling the activity-alias components declared in the
 * manifest. The aliases target [me.weishu.kernelsu.ui.MainActivity], so the activity itself
 * stays enabled and deep links keep working.
 */
object LauncherIconManager {
    const val STYLE_FOXSU = 0
    const val STYLE_KERNELSU = 1

    private const val ALIAS_FOXSU = "me.weishu.kernelsu.ui.LauncherDefault"
    private const val ALIAS_KERNELSU = "me.weishu.kernelsu.ui.LauncherKernelSU"

    fun apply(style: Int) {
        val pm = ksuApp.packageManager
        val pkg = ksuApp.packageName
        setEnabled(pm, pkg, ALIAS_FOXSU, style == STYLE_FOXSU)
        setEnabled(pm, pkg, ALIAS_KERNELSU, style == STYLE_KERNELSU)
    }

    private fun setEnabled(pm: PackageManager, pkg: String, alias: String, enabled: Boolean) {
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(pkg, alias),
                newState,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
