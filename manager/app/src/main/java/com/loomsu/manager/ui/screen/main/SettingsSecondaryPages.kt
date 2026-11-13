package com.loomsu.manager.ui.screen.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Description
import androidx.compose.material.icons.twotone.GridView
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Inventory2
import androidx.compose.material.icons.twotone.Science
import androidx.compose.material.icons.twotone.Terminal
import androidx.compose.material.icons.twotone.Palette
import androidx.compose.material.icons.twotone.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loomsu.manager.BuildConfig
import com.loomsu.manager.R
import com.loomsu.manager.domain.model.ManagerUpdateChannel
import com.loomsu.manager.domain.usecase.CheckManagerUpdateUseCase
import com.loomsu.manager.ui.component.LoomCard
import com.loomsu.manager.ui.component.LoomPrimaryButton
import com.loomsu.manager.ui.component.LoomRow
import com.loomsu.manager.ui.component.LoomRowDivider
import com.loomsu.manager.ui.navigation.LocalNavigator
import com.loomsu.manager.ui.navigation.Route
import com.loomsu.manager.ui.theme.LoomColors
import com.loomsu.manager.ui.theme.LoomTones
import com.loomsu.manager.ui.theme.LoomType
import com.loomsu.manager.ui.util.LocalSnackbarHost
import com.loomsu.manager.ui.util.showReplacingSnackbar
import com.loomsu.manager.ui.viewmodel.SettingsUiAction
import com.loomsu.manager.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * App Profile（设计稿 pages/profile）：2 项 · 按应用定制 Root 与模块行为。
 */
@Composable
fun SettingsProfilePage(bottomPadding: Dp = 0.dp) {
    val navigator = LocalNavigator.current
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    LoomSettingsScaffold(
        title = "App Profile",
        subtitle = "2 项 · 按应用定制 Root 与模块行为",
        bottomPadding = bottomPadding,
    ) {
        item {
            LoomCard {
                LoomRow(
                    icon = Icons.TwoTone.GridView,
                    title = stringResource(R.string.settings_profile_template),
                    description = stringResource(R.string.settings_profile_template_summary),
                    iconTint = LoomTones.amber.content,
                    iconContainer = LoomTones.amber.container,
                    onClick = { navigator.push(Route.AppProfileTemplate) },
                    trailing = {
                        Icon(
                            imageVector = Icons.TwoTone.ChevronRight,
                            contentDescription = null,
                            tint = LoomColors.chevron,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )

                LoomRowDivider()
                LoomSwitchRow(
                    icon = Icons.TwoTone.Inventory2,
                    title = stringResource(R.string.settings_umount_modules_default),
                    description = stringResource(R.string.settings_umount_modules_default_summary),
                    checked = uiState.defaultUmountModules,
                    tone = LoomTones.green,
                    onCheckedChange = {
                        settingsViewModel.dispatch(SettingsUiAction.SetDefaultUmountModules(it))
                    },
                )
            }
        }

        item {
            Text(
                text = "为单个应用配置后，将优先使用该应用的 Profile",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = LoomColors.textMuted,
                modifier = Modifier.padding(start = 2.dp, top = 14.dp),
            )
        }
    }
}

/**
 * 更新（设计稿 pages/update）：3 项 + 检查更新按钮。主题设置已上移到设置首页。
 */
@Composable
fun SettingsUpdatePage(bottomPadding: Dp = 0.dp) {
    val navigator = LocalNavigator.current
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val checkManagerUpdate = koinInject<CheckManagerUpdateUseCase>()
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsViewModel.dispatch(SettingsUiAction.LoadFeatureSettings)
    }

    LoomSettingsScaffold(
        title = "更新",
        subtitle = "3 项 · 当前版本 V${BuildConfig.VERSION_NAME}",
        bottomPadding = bottomPadding,
    ) {
        item {
            LoomCard {
                LoomSwitchRow(
                    icon = Icons.TwoTone.Update,
                    title = stringResource(R.string.settings_check_manager_update),
                    description = stringResource(R.string.settings_check_manager_update_summary),
                    checked = uiState.checkManagerUpdate,
                    tone = LoomTones.blue,
                    onCheckedChange = {
                        settingsViewModel.dispatch(SettingsUiAction.SetManagerUpdateCheck(it))
                    },
                )

                LoomRowDivider()
                LoomSwitchRow(
                    icon = Icons.TwoTone.Science,
                    title = stringResource(R.string.settings_check_beta_update),
                    description = stringResource(R.string.settings_check_beta_update_summary),
                    checked = uiState.checkBetaUpdate,
                    tone = LoomTones.rose,
                    onCheckedChange = {
                        settingsViewModel.dispatch(SettingsUiAction.SetBetaUpdateCheck(it))
                    },
                )

                LoomRowDivider()
                LoomSwitchRow(
                    icon = Icons.TwoTone.Inventory2,
                    title = stringResource(R.string.settings_check_module_update),
                    description = stringResource(R.string.settings_check_module_update_summary),
                    checked = uiState.checkModuleUpdate,
                    tone = LoomTones.violet,
                    onCheckedChange = {
                        settingsViewModel.dispatch(SettingsUiAction.SetModuleUpdateCheck(it))
                    },
                )


            }
        }

        item {
            LoomPrimaryButton(
                text = if (checking) "检查中…" else "检查更新",
                enabled = !checking,
                onClick = {
                    if (checking) return@LoomPrimaryButton
                    checking = true
                    scope.launch {
                        val info = runCatching {
                            checkManagerUpdate(
                                if (uiState.checkBetaUpdate) ManagerUpdateChannel.BETA else ManagerUpdateChannel.STABLE,
                            )
                        }.getOrNull()
                        checking = false
                        snackBarHost.showReplacingSnackbar(
                            if (info == null) {
                                "已是最新版本 V${BuildConfig.VERSION_NAME}"
                            } else {
                                "发现新版本 ${info.versionName}"
                            },
                        )
                    }
                },
                modifier = Modifier.padding(top = 18.dp),
            )
        }
    }
}

/**
 * 关于（设计稿 pages/about）：版本 / 法律信息 / 开源许可 / 检查更新。
 */
@Composable
fun SettingsAboutPage(bottomPadding: Dp = 0.dp) {
    val navigator = LocalNavigator.current
    val checkManagerUpdate = koinInject<CheckManagerUpdateUseCase>()
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }

    LoomSettingsScaffold(
        title = "关于",
        subtitle = "Loom SU ${BuildConfig.VERSION_NAME}",
        bottomPadding = bottomPadding,
    ) {
        item {
            LoomCard {
                LoomRow(
                    icon = Icons.TwoTone.Info,
                    title = "版本信息",
                    description = "V${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_CODE}",
                    iconTint = LoomTones.violet.content,
                    iconContainer = LoomTones.violet.container,
                    onClick = { navigator.push(Route.About) },
                    trailing = {
                        Icon(
                            imageVector = Icons.TwoTone.ChevronRight,
                            contentDescription = null,
                            tint = LoomColors.chevron,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )

                LoomRowDivider()
                LoomRow(
                    icon = Icons.TwoTone.Description,
                    title = "法律信息",
                    description = "开源协议与免责声明",
                    iconTint = LoomTones.slate.content,
                    iconContainer = LoomTones.slate.container,
                    onClick = { navigator.push(Route.About) },
                    trailing = {
                        Icon(
                            imageVector = Icons.TwoTone.ChevronRight,
                            contentDescription = null,
                            tint = LoomColors.chevron,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )

                LoomRowDivider()
                LoomRow(
                    icon = Icons.TwoTone.Terminal,
                    title = "开源许可",
                    description = "GPL-3.0 · 依赖组件清单",
                    iconTint = LoomTones.green.content,
                    iconContainer = LoomTones.green.container,
                    onClick = { navigator.push(Route.OpenSourceLicense) },
                    trailing = {
                        Icon(
                            imageVector = Icons.TwoTone.ChevronRight,
                            contentDescription = null,
                            tint = LoomColors.chevron,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )

                LoomRowDivider()
                LoomRow(
                    icon = Icons.TwoTone.Update,
                    title = "检查更新",
                    description = if (checking) "正在检查…" else "当前版本 V${BuildConfig.VERSION_NAME}",
                    iconTint = LoomTones.amber.content,
                    iconContainer = LoomTones.amber.container,
                    enabled = !checking,
                    onClick = {
                        checking = true
                        scope.launch {
                            val info = runCatching {
                                checkManagerUpdate(ManagerUpdateChannel.STABLE)
                            }.getOrNull()
                            checking = false
                            snackBarHost.showReplacingSnackbar(
                                if (info == null) {
                                    "已是最新版本 V${BuildConfig.VERSION_NAME}"
                                } else {
                                    "发现新版本 ${info.versionName}"
                                },
                            )
                        }
                    },
                )
            }
        }

        item {
            Text(
                text = "织光 · Loom SU · 基于 KernelSU",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = LoomColors.textMuted,
                modifier = Modifier.padding(start = 2.dp, top = 14.dp),
            )
        }
    }
}
