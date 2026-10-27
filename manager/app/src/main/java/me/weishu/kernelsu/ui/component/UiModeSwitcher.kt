package me.weishu.kernelsu.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.theme.ColorMode
import me.weishu.kernelsu.ui.uiModeLabelRes

/**
 * 顶栏的界面风格入口：点开是一张列表，直接选 Miuix / Material / ColorUI，不需要来回循环。
 */
@Composable
fun UiModeSwitchButton(modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    val repo = remember { SettingsRepositoryImpl() }
    var current by remember { mutableStateOf(UiMode.fromValue(repo.uiMode)) }

    IconButton(
        modifier = modifier,
        onClick = { expanded = true },
    ) {
        Icon(
            imageVector = Icons.Filled.Palette,
            contentDescription = stringResource(R.string.settings_ui_mode),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            UiMode.entries.forEach { mode ->
                val selected = mode == current
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(uiModeLabelRes(mode)),
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    trailingIcon = {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        applyUiMode(mode)
                        current = mode
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * 与设置页的切换保持一致：Miuix/Material 之间的 Monet 状态需要同步，否则会出现配色错乱。
 */
internal fun applyUiMode(next: UiMode) {
    val repo = SettingsRepositoryImpl()
    val old = UiMode.fromValue(repo.uiMode)
    if (old == next) return
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
