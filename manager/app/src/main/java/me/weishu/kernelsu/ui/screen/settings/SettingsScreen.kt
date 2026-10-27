package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel

@Composable
fun SettingPager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestIsCurrentPage by rememberUpdatedState(isCurrentPage)
    val initialResumeHandled = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            viewModel.refresh()
        }
    }

    LifecycleResumeEffect(Unit) {
        if (initialResumeHandled.value && latestIsCurrentPage) {
            viewModel.refresh()
        }
        initialResumeHandled.value = true
        onPauseOrDispose { }
    }

    val actions = SettingsScreenActions(
        onSetCheckUpdate = viewModel::setCheckUpdate,
        onSetCheckModuleUpdate = viewModel::setCheckModuleUpdate,
        onOpenTheme = { navigator.push(Route.ColorPalette) },
        onSetUiModeIndex = { index ->
            val mode = UiMode.entries.getOrNull(index) ?: UiMode.Miuix
            viewModel.setUiMode(mode.value)
        },
        onOpenProfileTemplate = { navigator.push(Route.AppProfileTemplate) },
        onSetSuCompatMode = viewModel::setSuCompatMode,
        onSetKernelUmountEnabled = viewModel::setKernelUmountEnabled,
        onSetSelinuxHideEnabled = viewModel::setSelinuxHideEnabled,
        onSetSulogEnabled = viewModel::setSulogEnabled,
        onSetAdbRootEnabled = viewModel::setAdbRootEnabled,
        onSetShellSuAdbOnlyEnabled = viewModel::setShellSuAdbOnlyEnabled,
        onSetDefaultUmountModules = viewModel::setDefaultUmountModules,
        onSetEnableWebDebugging = viewModel::setEnableWebDebugging,
        onSetAutoJailbreak = viewModel::setAutoJailbreak,
        onSetUseSoftReboot = viewModel::setUseSoftReboot,
        onOpenAbout = { navigator.push(Route.About) },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> SettingPagerMiuix(uiState, actions, bottomInnerPadding)
        UiMode.Material, UiMode.Fresh, UiMode.Glass -> SettingPagerMaterial(uiState, actions, bottomInnerPadding)
        UiMode.Glass -> SettingPagerGlass(navigator, uiState, actions, bottomInnerPadding)
    }
}

@Composable
private fun SettingPagerGlass(
    navigator: Navigator,
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
) {
    var showStylePicker by remember { mutableStateOf(false) }
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 13.dp, end = 13.dp, top = 18.dp, bottom = bottomInnerPadding + 90.dp
        ),
    ) {
        item {
            me.weishu.kernelsu.ui.screen.glass.GlassTopBar(
                title = stringResource(R.string.settings),
                subtitle = "FoxSu Glass",
            )
        }
        item {
            me.weishu.kernelsu.ui.screen.glass.GlassSettingsGroup(
                title = stringResource(R.string.customization_title),
                items = listOf(
                    me.weishu.kernelsu.ui.screen.glass.GlassSettingItem(
                        iconRes = R.drawable.ic_d_palette,
                        title = stringResource(R.string.settings_ui_mode),
                        subtitle = stringResource(me.weishu.kernelsu.ui.uiModeLabelRes(me.weishu.kernelsu.ui.UiMode.fromValue(uiState.uiMode))),
                        accentColor = 0xFFC77DFF,
                        onClick = { showStylePicker = true },
                    ),
                    me.weishu.kernelsu.ui.screen.glass.GlassSettingItem(
                        iconRes = R.drawable.ic_d_image,
                        title = stringResource(R.string.customization_title),
                        subtitle = stringResource(R.string.customization_entry_summary),
                        accentColor = 0xFF5CE1E6,
                        onClick = { navigator.push(Route.Customization) },
                    ),
                    me.weishu.kernelsu.ui.screen.glass.GlassSettingItem(
                        iconRes = R.drawable.ic_d_power2,
                        title = stringResource(R.string.settings_theme),
                        subtitle = stringResource(R.string.settings_theme_summary),
                        accentColor = 0xFFFF7AC8,
                        onClick = actions.onOpenTheme,
                    ),
                ),
            )
        }
        item {
            me.weishu.kernelsu.ui.screen.glass.GlassSettingsGroup(
                title = stringResource(R.string.superuser),
                items = listOf(
                    me.weishu.kernelsu.ui.screen.glass.GlassSettingItem(
                        iconRes = R.drawable.ic_d_user,
                        title = stringResource(R.string.settings_profile_template),
                        subtitle = stringResource(R.string.settings_profile_template_summary),
                        accentColor = 0xFF6EF08A,
                        onClick = actions.onOpenProfileTemplate,
                    ),
                ),
            )
        }
        item {
            me.weishu.kernelsu.ui.screen.glass.GlassSettingsGroup(
                title = stringResource(R.string.about),
                items = listOf(
                    me.weishu.kernelsu.ui.screen.glass.GlassSettingItem(
                        iconRes = R.drawable.ic_d_book,
                        title = stringResource(R.string.settings_grid_about),
                        subtitle = "FoxSu · Glass",
                        accentColor = 0xFF7AA2FF,
                        onClick = actions.onOpenAbout,
                    ),
                ),
            )
        }
    }
    if (showStylePicker) {
        me.weishu.kernelsu.ui.screen.glass.GlassStylePickerDialog(
            current = me.weishu.kernelsu.ui.UiMode.fromValue(uiState.uiMode),
            onDismiss = { showStylePicker = false },
        )
    }
}
