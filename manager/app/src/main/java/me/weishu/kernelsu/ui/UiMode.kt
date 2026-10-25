package me.weishu.kernelsu.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.staticCompositionLocalOf
import me.weishu.kernelsu.R

enum class UiMode(val value: String) {
    Miuix("miuix"),
    Material("material"),
    Fresh("fresh");

    companion object {
        fun fromValue(value: String): UiMode = when (value) {
            Material.value -> Material
            Fresh.value -> Fresh
            else -> Miuix
        }

        val DEFAULT_VALUE = Miuix.value
    }
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Miuix }

@StringRes
fun uiModeLabelRes(mode: UiMode): Int = when (mode) {
    UiMode.Miuix -> R.string.settings_ui_mode_miuix
    UiMode.Material -> R.string.settings_ui_mode_material
    UiMode.Fresh -> R.string.settings_ui_mode_fresh
}
