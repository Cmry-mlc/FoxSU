package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.freshCardBorder
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.component.GlassSheen
import me.weishu.kernelsu.ui.component.cardContentColor
import me.weishu.kernelsu.ui.component.freshCardColor
import me.weishu.kernelsu.ui.theme.LaunchTint
import me.weishu.kernelsu.ui.theme.UiPalette
import me.weishu.kernelsu.ui.component.isNightScheme
import me.weishu.kernelsu.ui.component.hasWallpaperBackdrop
import me.weishu.kernelsu.ui.uiModeLabelRes

private enum class TileState { On, Off, Link, Disabled }

private class SettingsTile(
    val iconRes: Int,
    val title: String,
    val caption: String?,
    val state: TileState,
    val onClick: () -> Unit,
)

/**
 * ColorUI settings: every option is a tile in a 3-column grid instead of a list row.
 * Switch tiles show 已开启 / 已关闭 and toggle on tap; entry tiles open their page.
 */
@Composable
fun FreshSettingsPage(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    onOpenUiModePicker: () -> Unit,
    onOpenCustomization: () -> Unit,
    onShowUninstall: () -> Unit,
    onShowLog: () -> Unit,
) {
    val onLabel = stringResource(R.string.settings_grid_on)
    val offLabel = stringResource(R.string.settings_grid_off)
    val unsupported = stringResource(R.string.feature_status_unsupported_summary)

    fun switchTile(
        icon: Int,
        title: String,
        checked: Boolean,
        enabled: Boolean = true,
        onChange: (Boolean) -> Unit,
    ) = SettingsTile(
        iconRes = icon,
        title = title,
        caption = when {
            !enabled -> unsupported
            checked -> onLabel
            else -> offLabel
        },
        state = when {
            !enabled -> TileState.Disabled
            checked -> TileState.On
            else -> TileState.Off
        },
        onClick = { if (enabled) onChange(!checked) },
    )

    fun linkTile(icon: Int, title: String, caption: String?, onClick: () -> Unit) =
        SettingsTile(icon, title, caption, TileState.Link, onClick)

    var sucompatDialog by remember { mutableStateOf(false) }

    Column {
        FreshSettingsSection(stringResource(R.string.settings_grid_section_general)) {
            FreshTileGrid(
                listOf(
                    switchTile(
                        icon = R.drawable.ic_d_download,
                        title = stringResource(R.string.settings_check_update),
                        checked = uiState.checkUpdate,
                        onChange = actions.onSetCheckUpdate,
                    ),
                    switchTile(
                        icon = R.drawable.ic_d_refresh,
                        title = stringResource(R.string.settings_module_check_update),
                        checked = uiState.checkModuleUpdate,
                        onChange = actions.onSetCheckModuleUpdate,
                    ),
                )
            )
        }

        FreshSettingsSection(stringResource(R.string.settings_grid_section_look)) {
            FreshTileGrid(
                listOf(
                    linkTile(
                        icon = R.drawable.ic_d_palette,
                        title = stringResource(R.string.settings_ui_mode),
                        caption = stringResource(uiModeLabelRes(UiMode.fromValue(uiState.uiMode))),
                        onClick = onOpenUiModePicker,
                    ),
                    linkTile(
                        icon = R.drawable.ic_d_image,
                        title = stringResource(R.string.customization_title),
                        caption = stringResource(R.string.customization_entry_summary),
                        onClick = onOpenCustomization,
                    ),
                    linkTile(
                        icon = R.drawable.ic_d_palette,
                        title = stringResource(R.string.settings_theme),
                        caption = stringResource(R.string.settings_theme_summary),
                        onClick = actions.onOpenTheme,
                    ),
                )
            )
        }

        FreshSettingsSection(stringResource(R.string.settings_grid_section_root)) {
            FreshTileGrid(
                listOf(
                    linkTile(
                        icon = R.drawable.ic_d_terminal,
                        title = stringResource(R.string.settings_sucompat),
                        caption = when (uiState.suCompatStatus) {
                            "unsupported" -> unsupported
                            "managed" -> stringResource(R.string.feature_status_managed_summary)
                            "forced" -> stringResource(R.string.feature_status_forced_summary)
                            else -> stringResource(R.string.settings_mode_enable_by_default)
                        },
                        onClick = { if (uiState.suCompatStatus == "supported") sucompatDialog = true },
                    ),
                    switchTile(
                        icon = R.drawable.ic_d_hammer,
                        title = stringResource(R.string.settings_kernel_umount),
                        checked = uiState.isKernelUmountEnabled,
                        enabled = uiState.kernelUmountStatus == "supported",
                        onChange = actions.onSetKernelUmountEnabled,
                    ),
                    switchTile(
                        icon = R.drawable.ic_d_lock,
                        title = stringResource(R.string.settings_selinux_hide),
                        checked = uiState.isSelinuxHideEnabled,
                        enabled = uiState.selinuxHideStatus == "supported",
                        onChange = actions.onSetSelinuxHideEnabled,
                    ),
                    switchTile(
                        icon = R.drawable.ic_d_book,
                        title = stringResource(R.string.settings_sulog),
                        checked = uiState.isSulogEnabled,
                        enabled = uiState.sulogStatus == "supported",
                        onChange = actions.onSetSulogEnabled,
                    ),
                    switchTile(
                        icon = R.drawable.ic_d_android,
                        title = stringResource(R.string.settings_adb_root),
                        checked = uiState.isAdbRootEnabled,
                        enabled = uiState.adbRootStatus == "supported",
                        onChange = actions.onSetAdbRootEnabled,
                    ),
                    switchTile(
                        icon = R.drawable.ic_d_shield2,
                        title = stringResource(R.string.settings_shell_su_adb_only),
                        checked = uiState.isShellSuAdbOnlyEnabled,
                        enabled = uiState.shellSuAdbOnlyStatus == "supported",
                        onChange = actions.onSetShellSuAdbOnlyEnabled,
                    ),
                    switchTile(
                        icon = R.drawable.ic_d_refresh,
                        title = stringResource(R.string.settings_soft_reboot),
                        checked = uiState.isLateLoadMode || uiState.useSoftReboot,
                        enabled = !uiState.isLateLoadMode,
                        onChange = actions.onSetUseSoftReboot,
                    ),
                    switchTile(
                        icon = R.drawable.ic_d_power2,
                        title = stringResource(R.string.settings_auto_jailbreak),
                        checked = uiState.autoJailbreak,
                        enabled = uiState.isLateLoadMode,
                        onChange = actions.onSetAutoJailbreak,
                    ),
                )
            )
        }

        FreshSettingsSection(stringResource(R.string.settings_grid_section_module)) {
            FreshTileGrid(
                buildList {
                    add(
                        linkTile(
                            icon = R.drawable.ic_d_log,
                            title = stringResource(R.string.settings_profile_template),
                            caption = stringResource(R.string.settings_profile_template_summary),
                            onClick = actions.onOpenProfileTemplate,
                        )
                    )
                    add(
                        switchTile(
                            icon = R.drawable.ic_d_tune,
                            title = stringResource(R.string.settings_umount_modules_default),
                            checked = uiState.isDefaultUmountModules,
                            onChange = actions.onSetDefaultUmountModules,
                        )
                    )
                    add(
                        switchTile(
                            icon = R.drawable.ic_d_terminal,
                            title = stringResource(R.string.enable_web_debugging),
                            checked = uiState.enableWebDebugging,
                            onChange = actions.onSetEnableWebDebugging,
                        )
                    )
                    if (uiState.isLkmMode) {
                        add(
                            linkTile(
                                icon = R.drawable.ic_d_hammer,
                                title = stringResource(R.string.settings_uninstall),
                                caption = null,
                                onClick = { if (!uiState.isLateLoadMode) onShowUninstall() },
                            )
                        )
                    }
                }
            )
        }

        FreshSettingsSection(stringResource(R.string.settings_grid_section_other)) {
            FreshTileGrid(
                listOf(
                    linkTile(
                        icon = R.drawable.ic_d_log,
                        title = stringResource(R.string.send_log),
                        caption = null,
                        onClick = onShowLog,
                    ),
                    linkTile(
                        icon = R.drawable.ic_d_book,
                        title = stringResource(R.string.about),
                        caption = null,
                        onClick = actions.onOpenAbout,
                    ),
                )
            )
        }

        Spacer(Modifier.height(12.dp))
    }

    if (sucompatDialog) {
        FreshOptionDialog(
            title = stringResource(R.string.settings_sucompat),
            options = listOf(
                stringResource(R.string.settings_mode_enable_by_default),
                stringResource(R.string.settings_mode_disable_until_reboot),
                stringResource(R.string.settings_mode_disable_always),
            ),
            selectedIndex = uiState.suCompatMode,
            onSelect = {
                actions.onSetSuCompatMode(it)
                sucompatDialog = false
            },
            onDismiss = { sucompatDialog = false },
        )
    }
}

@Composable
private fun FreshSettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, top = 16.dp, bottom = 9.dp),
        )
        content()
    }
}

@Composable
private fun FreshTileGrid(tiles: List<SettingsTile>) {
    // Rows of three; a leftover pair fills the whole row; exactly four tiles become a 2x2 grid.
    val rows = buildList {
        var i = 0
        while (i < tiles.size) {
            val remaining = tiles.size - i
            val take = when {
                tiles.size == 4 -> 2
                remaining == 2 || remaining == 1 -> remaining
                else -> 3
            }
            add(tiles.subList(i, i + take))
            i += take
        }
    }
    val custom = CustomizationStore.current
    val night = isNightScheme()
    val rainbow = custom.rainbowCards && !custom.cardGlass && !night
    // 设计稿配色：磁贴循环 rose / mint / cream / lav / sky / gray；开七彩走七彩表；壁纸/玻璃走磨砂
    val tones = if (night) {
        listOf(
            UiPalette.KpiRoseDark, UiPalette.PillMintDark,
            UiPalette.KpiCreamDark, UiPalette.KpiLilacDark,
            UiPalette.PillSkyDark, UiPalette.PillGrayDark,
        )
    } else {
        listOf(
            UiPalette.KpiRose, UiPalette.PillMint,
            UiPalette.KpiCream, UiPalette.KpiLilac,
            UiPalette.PillSky, UiPalette.PillGray,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        var index = 0
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { tile ->
                    val pair = when {
                        rainbow -> LaunchTint.rainbowPair(index).let { Color(it.first) to Color(it.second) }
                        custom.cardGlass || hasWallpaperBackdrop() -> null
                        else -> tones[index % tones.size]
                    }
                    index++
                    SettingsTileView(
                        tile = tile,
                        containerColor = pair?.first,
                        contentColor = pair?.second,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) { Spacer(Modifier.weight(1f)); Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SettingsTileView(
    tile: SettingsTile,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    val onCard = containerColor != null && contentColor != null
    // 设计稿：磁贴标题/说明都用卡片自己的深彩色；未支持的状态降透明度
    val accent = when {
        onCard -> contentColor!!
        tile.state == TileState.On || tile.state == TileState.Link -> MaterialTheme.colorScheme.primary
        tile.state == TileState.Off -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.outline
    }
    val titleColor = (contentColor ?: cardContentColor()).let {
        if (tile.state == TileState.Disabled) it.copy(alpha = 0.6f) else it
    }
    val captionColor = when {
        onCard -> contentColor!!.copy(alpha = if (tile.state == TileState.Disabled) 0.5f else 0.72f)
        tile.state == TileState.On -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .height(118.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = tile.state != TileState.Disabled, onClick = tile.onClick),
        shape = RoundedCornerShape(22.dp),
        color = containerColor ?: freshCardColor(),
        contentColor = contentColor ?: cardContentColor(),
        border = if (onCard) null else freshCardBorder(),
        shadowElevation = 0.dp,
    ) {
        Box {
            GlassSheen(Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 设计稿 chip：40dp 白底、13dp 圆角、彩色描边图标
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = if (onCard) {
                    Color.White.copy(alpha = if (isNightScheme()) 0.10f else 1f)
                } else {
                    accent.copy(alpha = if (tile.state == TileState.On) 0.18f else 0.10f)
                },
                contentColor = accent,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(tile.iconRes),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = tile.title,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = titleColor,
            )
            if (tile.caption != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = tile.caption,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = captionColor,
                )
            }
        }
        }
    }
}

@Composable
private fun FreshOptionDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = freshCardColor()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelect(index) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (index == selectedIndex) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isNightScheme()) Color(0xFF24242E) else Color(0xFFECECF2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onDismiss),
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.fresh_home_cancel),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
