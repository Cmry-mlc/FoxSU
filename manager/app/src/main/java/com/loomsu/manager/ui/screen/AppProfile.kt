package com.loomsu.manager.ui.screen

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AccountCircle
import androidx.compose.material.icons.twotone.Android
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.loomsu.manager.R
import com.loomsu.manager.domain.model.AppControlAction
import com.loomsu.manager.domain.model.AppProfile
import com.loomsu.manager.domain.model.InstalledApp
import com.loomsu.manager.domain.model.InstalledAppGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.twotone.KeyboardArrowLeft
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import com.loomsu.manager.ui.component.LoomCard
import com.loomsu.manager.ui.component.LoomChips
import com.loomsu.manager.ui.component.LoomMenuItem
import com.loomsu.manager.ui.component.LoomMenuPanel
import com.loomsu.manager.ui.component.LoomRow
import com.loomsu.manager.ui.component.LoomRowDivider
import com.loomsu.manager.ui.component.LoomSquareIconButton
import com.loomsu.manager.ui.component.LoomSwitch
import com.loomsu.manager.ui.theme.LoomColors
import com.loomsu.manager.ui.theme.LoomType
import com.loomsu.manager.ui.component.PackageIcon
import com.loomsu.manager.ui.component.SwipeableSnackbarHost
import com.loomsu.manager.ui.component.profile.AppProfileConfig
import com.loomsu.manager.ui.component.profile.RootProfileConfig
import com.loomsu.manager.ui.component.profile.TemplateConfig
import com.loomsu.manager.ui.component.settings.AppBackButton
import com.loomsu.manager.ui.component.settings.SegmentedColumn
import com.loomsu.manager.ui.component.settings.SettingsBaseWidget
import com.loomsu.manager.ui.component.settings.SettingsDropdownWidget
import com.loomsu.manager.ui.component.settings.SettingsJumpPageWidget
import com.loomsu.manager.ui.component.settings.SettingsSwitchWidget
import com.loomsu.manager.ui.component.settings.lazySegmentColumn
import com.loomsu.manager.ui.navigation.LocalNavigator
import com.loomsu.manager.ui.navigation.Route
import com.loomsu.manager.ui.theme.CardConfig
import com.loomsu.manager.ui.theme.ThemeConfig
import com.loomsu.manager.ui.theme.blurEffect
import com.loomsu.manager.ui.theme.blurSource
import com.loomsu.manager.ui.theme.renderBackgroundBlur
import com.loomsu.manager.ui.util.ActivityResumeEffect
import com.loomsu.manager.ui.util.LocalSnackbarHost
import com.loomsu.manager.ui.util.adaptiveScaffoldWindowInsets
import com.loomsu.manager.ui.util.showReplacingSnackbar
import com.loomsu.manager.ui.viewmodel.AppProfileUiAction
import com.loomsu.manager.ui.viewmodel.AppProfileUiEvent
import com.loomsu.manager.ui.viewmodel.AppProfileViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * @author weishu
 * @date 2023/5/16.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProfileScreen(
    uid: Int,
    packageName: String,
) {
    val navigator = LocalNavigator.current
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val viewModel =
        koinViewModel<AppProfileViewModel>(parameters = { parametersOf(uid, packageName) })
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val appGroup = uiState.appGroup
    val appLabel = appGroup?.mainApp?.label ?: packageName
    val isSpecial = appGroup?.isWebViewZygote == true
    val failToUpdateAppProfile = stringResource(R.string.failed_to_update_app_profile).format(appLabel)
    val failToUpdateSepolicy = stringResource(R.string.failed_to_update_sepolicy).format(appLabel)
    val suNotAllowed = stringResource(R.string.su_not_allowed).format(appLabel)

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AppProfileUiEvent.Error -> snackBarHost.showReplacingSnackbar(failToUpdateAppProfile)
                AppProfileUiEvent.SepolicyUpdateFailed -> snackBarHost.showReplacingSnackbar(failToUpdateSepolicy)
                AppProfileUiEvent.Saved -> Unit
            }
        }
    }

    ActivityResumeEffect(packageName, uid) {
        viewModel.dispatch(AppProfileUiAction.Load)
    }

    val profile = uiState.profile
    if (appGroup == null || profile == null) {
        LoomLoadingScreen()
        return
    }

    Scaffold(
        snackbarHost = { SwipeableSnackbarHost(hostState = snackBarHost) },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
    ) { paddingValues ->
        AppProfileInner(
            modifier = Modifier
                .fillMaxSize()
                .blurSource(),
            topPadding = paddingValues.calculateTopPadding(),
            bottomPadding = paddingValues.calculateBottomPadding(),
            appGroup = appGroup,
            isSpecial = isSpecial,
            appIcon = {
                PackageIcon(
                    packageName = if (isSpecial) "android" else appGroup.mainApp.packageName,
                    contentDescription = appGroup.mainApp.label,
                    modifier = Modifier.size(46.dp),
                )
            },
            profile = profile,
            defaultUmountModules = uiState.defaultUmountModules,
            sepolicyValid = uiState.sepolicyValid,
            onValidateSepolicy = { viewModel.dispatch(AppProfileUiAction.ValidateSepolicy(it)) },
            onViewTemplate = { navigator.push(Route.TemplateEditor(it, true)) },
            onManageTemplate = { navigator.push(Route.AppProfileTemplate) },
            onProfileChange = {
                scope.launch {
                    if (it.allowSu) {
                        // sync with allowlist.c - forbid_system_uid
                        if (uid < 2000 && uid != 1000) {
                            snackBarHost.showReplacingSnackbar(suNotAllowed)
                            return@launch
                        }
                    }
                    viewModel.dispatch(AppProfileUiAction.Save(it))
                }
            },
            onControlApp = { viewModel.dispatch(AppProfileUiAction.ControlApp(it)) },
        )
    }
}

/** 加载态：单个指示器 + 说明，避免空白闪烁。 */
@Composable
private fun LoomLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "正在读取应用配置…",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = LoomColors.textMuted,
            )
        }
    }
}

@Composable
private fun LoomCircleBack(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .shadow(6.dp, CircleShape, clip = false, ambientColor = Color(0x1418263C), spotColor = Color(0x1418263C))
            .clip(CircleShape)
            .background(LoomColors.canvasCard)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.TwoTone.KeyboardArrowLeft,
            contentDescription = "返回",
            tint = LoomColors.textBody,
            modifier = Modifier.size(20.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppProfileInner(
    modifier: Modifier = Modifier,
    topPadding: Dp,
    bottomPadding: Dp = 0.dp,
    appGroup: InstalledAppGroup,
    isSpecial: Boolean = false,
    appIcon: @Composable () -> Unit,
    profile: AppProfile,
    defaultUmountModules: Boolean = profile.umountModules,
    sepolicyValid: Boolean = true,
    onValidateSepolicy: (String) -> Unit = {},
    onViewTemplate: (id: String) -> Unit = {},
    onManageTemplate: () -> Unit = {},
    onControlApp: (AppControlAction) -> Unit,
    onProfileChange: (AppProfile) -> Unit,
) {
    val navigator = LocalNavigator.current
    val isRootGranted = !isSpecial && profile.allowSu
    val affectedApplicationsTitle = stringResource(R.string.affected_applications)
    val currentAppIcon by rememberUpdatedState(appIcon)
    val iconContent = remember { movableContentOf { currentAppIcon() } }

    val initialMode = when {
        isRootGranted -> if (profile.rootUseDefault) Mode.Default else if (profile.rootTemplate != null) Mode.Template else Mode.Custom
        else -> if (profile.nonRootUseDefault) Mode.Default else Mode.Custom
    }
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    var showAppMenu by remember { mutableStateOf(false) }
    val modes = if (isRootGranted) listOf(Mode.Default, Mode.Template, Mode.Custom) else listOf(Mode.Default, Mode.Custom)

    fun selectMode(next: Mode) {
        mode = next
        if (isRootGranted) {
            if (next == Mode.Default || next == Mode.Custom) {
                onProfileChange(profile.copy(rootUseDefault = next == Mode.Default, rootTemplate = null))
            }
        } else {
            onProfileChange(profile.copy(nonRootUseDefault = next == Mode.Default))
        }
    }

    LazyColumn(modifier = modifier) {
        item {
            Spacer(modifier = Modifier.height(topPadding + 6.dp))
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                LoomCircleBack(onClick = { navigator.pop() })
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = appGroup.mainApp.label,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.6).sp,
                    color = LoomColors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = appGroup.mainApp.displayIdentifier,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LoomColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(16.dp))

                LoomCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSpecial) { showAppMenu = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        iconContent()
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appGroup.mainApp.label,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = LoomColors.textBody,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = appGroup.mainApp.displayIdentifier,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = LoomColors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!isSpecial) {
                            Box {
                                LoomSquareIconButton(
                                    icon = Icons.TwoTone.MoreVert,
                                    contentDescription = null,
                                    onClick = { showAppMenu = true },
                                )
                                DropdownMenu(
                                    expanded = showAppMenu,
                                    onDismissRequest = { showAppMenu = false },
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    offset = DpOffset(x = 0.dp, y = 6.dp),
                                ) {
                                    LoomMenuPanel {
                                        LoomMenuItem(stringResource(R.string.launch_app)) {
                                            showAppMenu = false
                                            onControlApp(AppControlAction.LAUNCH)
                                        }
                                        LoomMenuItem(stringResource(R.string.force_stop_app)) {
                                            showAppMenu = false
                                            onControlApp(AppControlAction.FORCE_STOP)
                                        }
                                        LoomMenuItem(stringResource(R.string.restart_app)) {
                                            showAppMenu = false
                                            onControlApp(AppControlAction.RESTART)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isSpecial) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LoomCard {
                        LoomRow(
                            icon = Icons.TwoTone.Security,
                            title = stringResource(R.string.superuser),
                            iconTint = if (isRootGranted) LoomColors.success else LoomColors.textSec,
                            iconContainer = if (isRootGranted) LoomColors.successSoft else LoomColors.canvasSubtle,
                            onClick = { onProfileChange(profile.copy(allowSu = !isRootGranted)) },
                            trailing = {
                                LoomSwitch(
                                    checked = isRootGranted,
                                    onCheckedChange = { onProfileChange(profile.copy(allowSu = it)) },
                                )
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                LoomCard {
                    LoomRow(
                        icon = Icons.TwoTone.AccountCircle,
                        title = stringResource(R.string.profile),
                        description = mode.text,
                        iconTint = LoomColors.brandDeep,
                        iconContainer = LoomColors.brandSoft,
                    )
                    LoomChips(
                        options = modes.map { it.text },
                        selectedIndex = modes.indexOf(mode).coerceAtLeast(0),
                        onSelect = { selectMode(modes[it]) },
                        modifier = Modifier.padding(bottom = 14.dp),
                    )
                }
            }
        }

        // 模版 / 自定义的详细配置
        if (mode != Mode.Default) {
            item {
                Crossfade(targetState = mode, label = "ProfileMode") { currentMode ->
                    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp)) {
                        when (currentMode) {
                            Mode.Template -> {
                                LoomCard(contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                                    TemplateConfig(
                                        profile = profile,
                                        onViewTemplate = onViewTemplate,
                                        onProfileChange = onProfileChange,
                                    )
                                    LoomRow(
                                        icon = Icons.TwoTone.Edit,
                                        title = stringResource(R.string.manage_app_profile),
                                        description = stringResource(R.string.settings_profile_template_summary),
                                        iconTint = LoomColors.amber,
                                        iconContainer = LoomColors.amberSoft,
                                        onClick = onManageTemplate,
                                        trailing = {
                                            Icon(
                                                imageVector = Icons.TwoTone.ChevronRight,
                                                contentDescription = null,
                                                tint = LoomColors.chevron,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        },
                                    )
                                }
                            }

                            Mode.Custom -> {
                                LoomCard(contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                                    if (isRootGranted) {
                                        RootProfileConfig(
                                            profile = profile,
                                            sepolicyValid = sepolicyValid,
                                            onValidateSepolicy = onValidateSepolicy,
                                            onProfileChange = onProfileChange,
                                        )
                                    } else {
                                        AppProfileConfig(
                                            enabled = true,
                                            profile = profile,
                                            defaultUmountModules = defaultUmountModules,
                                            onProfileChange = onProfileChange,
                                        )
                                    }
                                }
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }

        if (appGroup.apps.size > 1) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        text = affectedApplicationsTitle,
                        style = LoomType.sectionTitle,
                        modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 8.dp),
                    )
                    LoomCard {
                        appGroup.apps.forEachIndexed { index, app ->
                            if (index > 0) LoomRowDivider()
                            LoomRow(
                                icon = Icons.TwoTone.Android,
                                title = app.label,
                                description = app.packageName,
                                enabled = false,
                                iconTint = LoomColors.textSec,
                                iconContainer = LoomColors.canvasSubtle,
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(bottomPadding + 40.dp))
        }
    }
}

private enum class Mode(@param:StringRes private val res: Int) {
    Default(R.string.profile_default),
    Template(R.string.profile_template),
    Custom(R.string.profile_custom),
    ;

    val text: String
        @Composable get() = stringResource(res)
}

@Preview
@Composable
private fun AppProfilePreview() {
    val cardConfig: CardConfig = koinInject()
    var profile by remember { mutableStateOf(AppProfile("")) }

    Surface(
        color = if (cardConfig.isCustomBackgroundEnabled) Color.Transparent else MaterialTheme.colorScheme.surfaceBright,
    ) {
        AppProfileInner(
            appGroup = InstalledAppGroup(
                uid = 0,
                primaryPackageName = "preview",
                apps = listOf(InstalledApp("preview", "Preview", 0)),
            ),
            appIcon = {
                Icon(
                    imageVector = Icons.TwoTone.Android,
                    contentDescription = null,
                )
            },
            profile = profile,
            topPadding = 0.dp,
            onControlApp = {},
            onProfileChange = {
                profile = it
            },
        )
    }
}
