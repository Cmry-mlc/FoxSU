package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.screen.glass.GlassStylePickerDialog
import me.weishu.kernelsu.ui.screen.settings.SettingsScreenActions
import me.weishu.kernelsu.ui.screen.settings.SettingsUiState
import me.weishu.kernelsu.ui.uiModeLabelRes

/** Nocturne 设置页：Group r20 分组卡 + 图标盒行 + 开关/徽章/箭头。 */
@Composable
fun NocturneSettingsScreen(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
    onOpenCustomization: () -> Unit,
    onOpenSulog: () -> Unit,
) {
    val t = ncTokens()
    var showStylePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = NcDimens.pageX,
            end = NcDimens.pageX,
            top = ncStatusTop() + 20.dp,
            bottom = bottomInnerPadding + 100.dp,
        ),
    ) {
        item {
            NcHeader(kicker = "Preferences", title = stringResource(R.string.settings))
        }
        item {
            NcPanel {
                NcGroupRow(
                    iconRes = R.drawable.ic_d_download,
                    accent = t.violet,
                    title = stringResource(R.string.settings_check_update),
                    subtitle = BuildConfig.VERSION_NAME,
                ) {
                    NcSwitch(
                        checked = uiState.checkUpdate,
                        onClick = { actions.onSetCheckUpdate(!uiState.checkUpdate) },
                    )
                }
                NcGroupRow(
                    iconRes = R.drawable.ic_d_refresh,
                    accent = t.mint,
                    title = stringResource(R.string.settings_module_check_update),
                    subtitle = stringResource(R.string.settings_check_update_summary),
                    showDivider = true,
                ) {
                    NcSwitch(
                        checked = uiState.checkModuleUpdate,
                        onClick = { actions.onSetCheckModuleUpdate(!uiState.checkModuleUpdate) },
                    )
                }
                NcGroupRow(
                    iconRes = R.drawable.ic_d_power2,
                    accent = t.amber,
                    title = stringResource(R.string.settings_soft_reboot),
                    subtitle = stringResource(R.string.settings_soft_reboot_summary),
                    showDivider = true,
                ) {
                    NcSwitch(
                        checked = uiState.useSoftReboot,
                        onClick = { actions.onSetUseSoftReboot(!uiState.useSoftReboot) },
                    )
                }
                NcGroupRow(
                    iconRes = R.drawable.ic_d_hammer,
                    accent = t.lime,
                    title = stringResource(R.string.settings_auto_jailbreak),
                    subtitle = stringResource(R.string.settings_auto_jailbreak_summary),
                    showDivider = true,
                ) {
                    NcSwitch(
                        checked = uiState.autoJailbreak,
                        onClick = { actions.onSetAutoJailbreak(!uiState.autoJailbreak) },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            NcPanel {
                NcGroupRow(
                    iconRes = R.drawable.ic_d_palette,
                    accent = t.violet,
                    title = stringResource(R.string.settings_ui_mode),
                    subtitle = stringResource(uiModeLabelRes(LocalUiMode.current)),
                    onClick = { showStylePicker = true },
                ) { NcChevron() }
                NcGroupRow(
                    iconRes = R.drawable.ic_d_image,
                    accent = t.mint,
                    title = stringResource(R.string.customization_title),
                    subtitle = stringResource(R.string.customization_entry_summary),
                    showDivider = true,
                    onClick = onOpenCustomization,
                ) { NcChevron() }
                NcGroupRow(
                    iconRes = R.drawable.ic_d_tool,
                    accent = t.amber,
                    title = stringResource(R.string.settings_theme),
                    subtitle = stringResource(R.string.settings_theme_summary),
                    showDivider = true,
                    onClick = actions.onOpenTheme,
                ) { NcChevron() }
                NcGroupRow(
                    iconRes = R.drawable.ic_d_grid,
                    accent = t.violet,
                    title = stringResource(R.string.settings_profile_template),
                    subtitle = stringResource(R.string.settings_profile_template_summary),
                    showDivider = true,
                    onClick = actions.onOpenProfileTemplate,
                ) { NcChevron() }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            NcPanel {
                NcGroupRow(
                    iconRes = R.drawable.ic_d_terminal,
                    accent = t.mint,
                    title = stringResource(R.string.settings_sucompat),
                    subtitle = uiState.suCompatStatus.ifBlank {
                        stringResource(R.string.settings_sucompat_summary)
                    },
                ) {
                    NcBadge(
                        text = if (uiState.isSuEnabled) "开启" else "关闭",
                        color = if (uiState.isSuEnabled) t.mint else t.text3,
                    )
                }
                NcGroupRow(
                    iconRes = R.drawable.ic_d_shield2,
                    accent = t.violet,
                    title = stringResource(R.string.settings_kernel_umount),
                    subtitle = uiState.kernelUmountStatus.ifBlank {
                        stringResource(R.string.settings_kernel_umount_summary)
                    },
                    showDivider = true,
                ) {
                    NcSwitch(
                        checked = uiState.isKernelUmountEnabled,
                        onClick = { actions.onSetKernelUmountEnabled(!uiState.isKernelUmountEnabled) },
                    )
                }
                NcGroupRow(
                    iconRes = R.drawable.ic_d_lock,
                    accent = t.amber,
                    title = stringResource(R.string.settings_selinux_hide),
                    subtitle = uiState.selinuxHideStatus.ifBlank {
                        stringResource(R.string.settings_selinux_hide_summary)
                    },
                    showDivider = true,
                ) {
                    NcSwitch(
                        checked = uiState.isSelinuxHideEnabled,
                        onClick = { actions.onSetSelinuxHideEnabled(!uiState.isSelinuxHideEnabled) },
                    )
                }
                NcGroupRow(
                    iconRes = R.drawable.ic_d_log,
                    accent = t.violet,
                    title = stringResource(R.string.settings_sulog),
                    subtitle = stringResource(R.string.settings_sulog_summary),
                    showDivider = true,
                    onClick = onOpenSulog,
                ) { NcChevron() }
                NcGroupRow(
                    iconRes = R.drawable.ic_d_android,
                    accent = t.mint,
                    title = stringResource(R.string.settings_adb_root),
                    subtitle = uiState.adbRootStatus.ifBlank {
                        stringResource(R.string.settings_adb_root_summary)
                    },
                    showDivider = true,
                ) {
                    NcSwitch(
                        checked = uiState.isAdbRootEnabled,
                        onClick = { actions.onSetAdbRootEnabled(!uiState.isAdbRootEnabled) },
                    )
                }
                NcGroupRow(
                    iconRes = R.drawable.ic_d_terminal,
                    accent = t.amber,
                    title = stringResource(R.string.settings_shell_su_adb_only),
                    subtitle = uiState.shellSuAdbOnlyStatus.ifBlank {
                        stringResource(R.string.settings_shell_su_adb_only_summary)
                    },
                    showDivider = true,
                ) {
                    NcSwitch(
                        checked = uiState.isShellSuAdbOnlyEnabled,
                        onClick = { actions.onSetShellSuAdbOnlyEnabled(!uiState.isShellSuAdbOnlyEnabled) },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            NcPanel {
                NcGroupRow(
                    iconRes = R.drawable.ic_d_grid,
                    accent = t.red,
                    title = stringResource(R.string.settings_umount_modules_default),
                    subtitle = stringResource(R.string.settings_umount_modules_default_summary),
                ) {
                    NcSwitch(
                        checked = uiState.isDefaultUmountModules,
                        onClick = { actions.onSetDefaultUmountModules(!uiState.isDefaultUmountModules) },
                    )
                }
                NcGroupRow(
                    iconRes = R.drawable.ic_d_tool,
                    accent = t.violet,
                    title = stringResource(R.string.enable_web_debugging),
                    subtitle = stringResource(R.string.enable_web_debugging_summary),
                    showDivider = true,
                ) {
                    NcSwitch(
                        checked = uiState.enableWebDebugging,
                        onClick = { actions.onSetEnableWebDebugging(!uiState.enableWebDebugging) },
                    )
                }
                NcGroupRow(
                    iconRes = R.drawable.ic_d_book,
                    accent = t.mint,
                    title = stringResource(R.string.about),
                    subtitle = "FoxSu Nocturne",
                    showDivider = true,
                    onClick = actions.onOpenAbout,
                ) { NcChevron() }
            }
        }
    }

    if (showStylePicker) {
        GlassStylePickerDialog(current = LocalUiMode.current, onDismiss = { showStylePicker = false })
    }
}
