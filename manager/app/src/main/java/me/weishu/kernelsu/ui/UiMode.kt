package me.weishu.kernelsu.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.staticCompositionLocalOf
import me.weishu.kernelsu.R

enum class UiMode(val value: String) {
    Miuix("miuix"),
    Material("material"),
    Fresh("fresh"),
    Glass("glass"),
    Nocturne("nocturne");

    companion object {
        fun fromValue(value: String): UiMode = when (value) {
            Miuix.value -> Miuix
            Material.value -> Material
            Glass.value -> Glass
            Nocturne.value -> Nocturne
            else -> Fresh
        }

        val DEFAULT_VALUE = Fresh.value
    }
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Fresh }

@StringRes
fun uiModeLabelRes(mode: UiMode): Int = when (mode) {
    UiMode.Miuix -> R.string.settings_ui_mode_miuix
    UiMode.Material -> R.string.settings_ui_mode_material
    UiMode.Glass -> R.string.settings_ui_mode_glass
    UiMode.Nocturne -> R.string.settings_ui_mode_nocturne
    UiMode.Fresh -> R.string.settings_ui_mode_fresh
}
