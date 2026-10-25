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
    const val STYLE_FOX2 = 2
    const val STYLE_FOX3 = 3
    const val STYLE_FOX4 = 4

    private val ALIASES = listOf(
        "me.weishu.kernelsu.ui.LauncherDefault",
        "me.weishu.kernelsu.ui.LauncherKernelSU",
        "me.weishu.kernelsu.ui.LauncherFox2",
        "me.weishu.kernelsu.ui.LauncherFox3",
        "me.weishu.kernelsu.ui.LauncherFox4",
    )

    fun apply(style: Int) {
        val pm = ksuApp.packageManager
        val pkg = ksuApp.packageName
        // Enable the wanted alias first so the launcher never ends up with no entry point.
        val target = style.coerceIn(0, ALIASES.lastIndex)
        setEnabled(pm, pkg, ALIASES[target], true)
        ALIASES.forEachIndexed { index, alias ->
            if (index != target) setEnabled(pm, pkg, alias, false)
        }
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
