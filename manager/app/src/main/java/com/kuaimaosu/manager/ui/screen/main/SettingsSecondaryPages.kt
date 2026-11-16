package com.kuaimaosu.manager.ui.screen.main

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
import com.kuaimaosu.manager.BuildConfig
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.domain.model.ManagerUpdateChannel
import com.kuaimaosu.manager.domain.usecase.CheckManagerUpdateUseCase
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoPrimaryButton
import com.kuaimaosu.manager.ui.component.KuaiMaoRow
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.navigation.Route
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.ui.theme.KuaiMaoType
import com.kuaimaosu.manager.ui.util.LocalSnackbarHost
import com.kuaimaosu.manager.ui.util.showReplacingSnackbar
import com.kuaimaosu.manager.ui.viewmodel.SettingsUiAction
import com.kuaimaosu.manager.ui.viewmodel.SettingsViewModel
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

    KuaiMaoSettingsScaffold(
        title = "App Profile",
        subtitle = "2 项 · 按应用定制 Root 与模块行为",
        bottomPadding = bottomPadding,
    ) {
        item {
            KuaiMaoCard {
                KuaiMaoRow(
                    icon = Icons.TwoTone.GridView,
                    title = stringResource(R.string.settings_profile_template),
                    description = stringResource(R.string.settings_profile_template_summary),
                    iconTint = KuaiMaoTones.amber.content,
                    iconContainer = KuaiMaoTones.amber.container,
                    onClick = { navigator.push(Route.AppProfileTemplate) },
                    trailing = {
                        Icon(
                            imageVector = Icons.TwoTone.ChevronRight,
                            contentDescription = null,
                            tint = KuaiMaoColors.chevron,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )

                KuaiMaoRowDivider()
                KuaiMaoSwitchRow(
                    icon = Icons.TwoTone.Inventory2,
                    title = stringResource(R.string.settings_umount_modules_default),
                    description = stringResource(R.string.settings_umount_modules_default_summary),
                    checked = uiState.defaultUmountModules,
                    tone = KuaiMaoTones.green,
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
                color = KuaiMaoColors.textMuted,
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

    KuaiMaoSettingsScaffold(
        title = "更新",
        subtitle = "3 项 · 当前版本 V${BuildConfig.VERSION_NAME}",
        bottomPadding = bottomPadding,
    ) {
        item {
            KuaiMaoCard {
                KuaiMaoSwitchRow(
                    icon = Icons.TwoTone.Update,
                    title = stringResource(R.string.settings_check_manager_update),
                    description = stringResource(R.string.settings_check_manager_update_summary),
                    checked = uiState.checkManagerUpdate,
                    tone = KuaiMaoTones.blue,
                    onCheckedChange = {
                        settingsViewModel.dispatch(SettingsUiAction.SetManagerUpdateCheck(it))
                    },
                )

                KuaiMaoRowDivider()
                KuaiMaoSwitchRow(
                    icon = Icons.TwoTone.Science,
                    title = stringResource(R.string.settings_check_beta_update),
                    description = stringResource(R.string.settings_check_beta_update_summary),
                    checked = uiState.checkBetaUpdate,
                    tone = KuaiMaoTones.rose,
                    onCheckedChange = {
                        settingsViewModel.dispatch(SettingsUiAction.SetBetaUpdateCheck(it))
                    },
                )

                KuaiMaoRowDivider()
                KuaiMaoSwitchRow(
                    icon = Icons.TwoTone.Inventory2,
                    title = stringResource(R.string.settings_check_module_update),
                    description = stringResource(R.string.settings_check_module_update_summary),
                    checked = uiState.checkModuleUpdate,
                    tone = KuaiMaoTones.violet,
                    onCheckedChange = {
                        settingsViewModel.dispatch(SettingsUiAction.SetModuleUpdateCheck(it))
                    },
                )


            }
        }

        item {
            KuaiMaoPrimaryButton(
                text = if (checking) "检查中…" else "检查更新",
                enabled = !checking,
                onClick = {
                    if (checking) return@KuaiMaoPrimaryButton
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
