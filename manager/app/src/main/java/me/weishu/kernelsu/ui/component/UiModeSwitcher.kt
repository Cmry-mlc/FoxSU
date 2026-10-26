package me.weishu.kernelsu.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.theme.ColorMode
import me.weishu.kernelsu.ui.uiModeLabelRes

/**
 * 一键切换界面风格：Miuix → Material → ColorUI → Miuix。
 * 顶栏放一个调色板图标，点一下换一套风格，不用再进设置页。
 */
@Composable
fun UiModeSwitchButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val current = UiMode.fromValue(SettingsRepositoryImpl().uiMode)
    val label = stringResource(uiModeLabelRes(current))

    IconButton(
        modifier = modifier,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            val next = nextUiMode(current)
            applyUiMode(next)
            Toast.makeText(
                context,
                context.getString(R.string.settings_ui_mode) + " · " + context.getString(uiModeLabelRes(next)),
                Toast.LENGTH_SHORT,
            ).show()
        },
    ) {
        Icon(
            imageVector = Icons.Filled.Palette,
            contentDescription = stringResource(R.string.settings_ui_mode) + " (" + label + ")",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun nextUiMode(mode: UiMode): UiMode = when (mode) {
    UiMode.Miuix -> UiMode.Material
    UiMode.Material -> UiMode.Fresh
    UiMode.Fresh -> UiMode.Miuix
}

/**
 * 与设置页的切换保持一致：Miuix/Material 之间的 Monet 状态需要同步，否则会出现配色错乱。
 */
private fun applyUiMode(next: UiMode) {
    val repo = SettingsRepositoryImpl()
    val old = UiMode.fromValue(repo.uiMode)
    val currentThemeMode = repo.themeMode

    val newThemeMode = when {
        old == UiMode.Material && next == UiMode.Miuix -> {
            val colorMode = ColorMode.fromValue(currentThemeMode)
            val baseMode = if (colorMode == ColorMode.DARK_AMOLED) 2 else currentThemeMode
            if (repo.miuixMonet && !colorMode.isMonet) {
                ColorMode.fromValue(baseMode).toMonetMode()
            } else if (!repo.miuixMonet && colorMode.isMonet) {
                ColorMode.fromValue(baseMode).toNonMonetMode()
            } else baseMode
        }

        old == UiMode.Miuix && next == UiMode.Material -> {
            val colorMode = ColorMode.fromValue(currentThemeMode)
            if (colorMode.isMonet) colorMode.toNonMonetMode() else currentThemeMode
        }

        else -> currentThemeMode
    }

    repo.uiMode = next.value
    if (newThemeMode != currentThemeMode) repo.themeMode = newThemeMode
}
