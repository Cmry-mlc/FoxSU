package com.loomsu.manager.ui.screen.main

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Article
import androidx.compose.material.icons.twotone.Adb
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.ElectricalServices
import androidx.compose.material.icons.twotone.Policy
import androidx.compose.material.icons.twotone.RemoveCircle
import androidx.compose.material.icons.twotone.RemoveModerator
import androidx.compose.material.icons.twotone.RestartAlt
import androidx.compose.material.icons.twotone.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loomsu.manager.R
import com.loomsu.manager.ui.component.LoomCard
import com.loomsu.manager.ui.component.LoomRow
import com.loomsu.manager.ui.component.LoomRowDivider
import com.loomsu.manager.ui.component.settings.SettingsChooseDialog
import com.loomsu.manager.ui.theme.LoomColors
import com.loomsu.manager.ui.theme.LoomTones
import com.loomsu.manager.ui.theme.LoomType
import com.loomsu.manager.ui.viewmodel.HomeViewModel
import com.loomsu.manager.ui.viewmodel.SettingsUiAction
import com.loomsu.manager.ui.viewmodel.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * 通用与 Root（设计稿 pages/root）。
 *
 * 8 项：su 兼容模式、内核卸载模块、自动越狱（仅 late-load 系统出现）、ADB Root、
 * 仅 ADB 可拿 Root、软重启、访问日志、隐藏 SELinux 修改。全部绑定真实 ksu 能力状态。
 */
@Composable
fun SettingsRootPage(bottomPadding: Dp = 0.dp) {
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val status = homeState.systemStatus

    LaunchedEffect(Unit) {
        settingsViewModel.dispatch(SettingsUiAction.LoadFeatureSettings)
    }

    val modeItems = listOf(
        stringResource(R.string.settings_mode_default),
        stringResource(R.string.settings_mode_disable_until_reboot),
        stringResource(R.string.settings_mode_disable_always),
    )
    var showSuCompatDialog by remember { mutableStateOf(false) }

    if (showSuCompatDialog) {
        SettingsChooseDialog(
            show = true,
            title = stringResource(R.string.settings_sucompat),
            items = modeItems,
            selectedIndex = uiState.suCompatMode,
            onDismiss = { showSuCompatDialog = false },
            onSelectedIndexChange = { index ->
                settingsViewModel.dispatch(SettingsUiAction.SetSuCompatMode(index))
                showSuCompatDialog = false
            },
        )
    }

    val itemCount = 8

    LoomSettingsScaffold(
        title = "通用与 Root",
        subtitle = "$itemCount 项 · 影响全局 Root 行为",
        bottomPadding = bottomPadding,
    ) {
        item {
            Text(
                text = "Root 行为",
                style = LoomType.sectionTitle,
                modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
            )
        }

        item {
            LoomCard {
                LoomRow(
                    icon = Icons.TwoTone.RemoveModerator,
                    title = stringResource(id = R.string.settings_sucompat),
                    description = suCompatSummary(uiState.suStatus),
                    iconTint = LoomTones.rose.content,
                    iconContainer = LoomTones.rose.container,
                    enabled = uiState.suStatus == "supported",
                    onClick = {
                        if (uiState.suStatus == "supported") showSuCompatDialog = true
                    },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = modeItems.getOrElse(uiState.suCompatMode) { modeItems.first() },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = LoomColors.textTer,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.TwoTone.ChevronRight,
                                contentDescription = null,
                                tint = LoomColors.chevron,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    },
                )

                LoomRowDivider()
                LoomSwitchRow(
                    icon = Icons.TwoTone.RemoveCircle,
                    title = stringResource(id = R.string.settings_kernel_umount),
                    description = featureSummary(uiState.kernelUmountStatus, R.string.settings_kernel_umount_summary),
                    checked = uiState.isKernelUmountEnabled,
                    enabled = uiState.kernelUmountStatus == "supported",
                    tone = LoomTones.green,
                    onCheckedChange = {
                        settingsViewModel.dispatch(SettingsUiAction.SetKernelUmount(it))
                    },
                )

                LoomRowDivider()
                LoomSwitchRow(
                    icon = Icons.TwoTone.ElectricalServices,
                    title = stringResource(id = R.string.settings_auto_jailbreak),
                    description = if (status.isLateLoadMode) {
                        stringResource(id = R.string.settings_auto_jailbreak_summary)
                    } else {
                        "仅在 late-load（越狱模式）下可用，需授予本应用自启动权限"
                    },
                    checked = uiState.autoJailbreakEnabled,
                    enabled = status.isLateLoadMode,
                    tone = LoomTones.amber,
                    onCheckedChange = {
                        settingsViewModel.dispatch(SettingsUiAction.SetAutoJailbreak(it))
                    },
                )

                LoomRowDivider()
                LoomSwitchRow(
                    icon = Icons.TwoTone.Adb,
                    title = stringResource(id = R.string.settings_adb_root),
                    description = featureSummary(uiState.adbRootStatus, R.string.settings_adb_root_summary),
                    checked = uiState.isAdbRootEnabled,
                    enabled = uiState.adbRootStatus == "supported",
                    tone = LoomTones.blue,
                    onCheckedChange = {
                        settingsViewModel.dispatch(SettingsUiAction.SetAdbRoot(it))
                    },
                )

                LoomRowDivider()
                LoomSwitchRow(
                    icon = Icons.TwoTone.VerifiedUser,
                    title = stringResource(id = R.string.settings_shell_su_adb_only),
                    description = featureSummary(
                        uiState.shellSuAdbOnlyStatus,
                        R.string.settings_shell_su_adb_only_summary,
                    ),
                    checked = uiState.isShellSuAdbOnlyEnabled,
                    enabled = uiState.shellSuAdbOnlyStatus == "supported",
                    tone = LoomTones.violet,
                    onCheckedChange = {
                        settingsViewModel.dispatch(SettingsUiAction.SetShellSuAdbOnly(it))
                    },
                )

                LoomRowDivider()
                LoomSwitchRow(
                    icon = Icons.TwoTone.RestartAlt,
                    title = stringResource(id = R.string.settings_soft_reboot),
                    description = stringResource(id = R.string.settings_soft_reboot_summary),
                    checked = status.isLateLoadMode || uiState.useSoftReboot,
                    enabled = status.isFullFeatured && !status.isLateLoadMode,
                    tone = LoomTones.slate,
                    onCheckedChange = {
                        settingsViewModel.dispatch(SettingsUiAction.SetUseSoftReboot(it))
                    },
                )
            }
        }

        item {
            Text(
                text = "审计与隐藏",
                style = LoomType.sectionTitle,
                modifier = Modifier.padding(start = 2.dp, top = 20.dp, bottom = 10.dp),
            )
        }

        item {
            LoomCard {
                LoomSwitchRow(
                    icon = Icons.AutoMirrored.TwoTone.Article,
                    title = stringResource(id = R.string.settings_sulog),
                    description = featureSummary(uiState.sulogStatus, R.string.settings_sulog_summary),
                    checked = uiState.isSuLogEnabled,
                    enabled = uiState.sulogStatus == "supported",
                    tone = LoomTones.rose,
                    onCheckedChange = {
                        settingsViewModel.dispatch(SettingsUiAction.SetSuLog(it))
                    },
                )

                LoomRowDivider()
                LoomSwitchRow(
                    icon = Icons.TwoTone.Policy,
                    title = stringResource(id = R.string.settings_selinux_hide),
                    description = featureSummary(uiState.selinuxHideStatus, R.string.settings_selinux_hide_summary),
                    checked = uiState.isSelinuxHideEnabled,
                    enabled = uiState.selinuxHideStatus == "supported",
                    tone = LoomTones.green,
                    onCheckedChange = {
                        settingsViewModel.dispatch(SettingsUiAction.SetSelinuxHide(it))
                    },
                )
            }
        }

        item {
            Text(
                text = "修改部分选项后需重启生效",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = LoomColors.textMuted,
                modifier = Modifier.padding(start = 2.dp, top = 14.dp),
            )
        }
    }
}

@Composable
private fun featureSummary(status: String, @androidx.annotation.StringRes fallback: Int): String = when (status) {
    "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
    "managed" -> stringResource(id = R.string.feature_status_managed_summary)
    "forced" -> stringResource(id = R.string.feature_status_forced_summary)
    else -> stringResource(id = fallback)
}

@Composable
private fun suCompatSummary(status: String): String = when (status) {
    "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
    "managed" -> stringResource(id = R.string.feature_status_managed_summary)
    "forced" -> stringResource(id = R.string.feature_status_forced_summary)
    else -> stringResource(id = R.string.settings_sucompat_summary)
}
