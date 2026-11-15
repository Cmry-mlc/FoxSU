package com.kuaimaosu.manager.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material.icons.twotone.Tune
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoMenuLabel
import com.kuaimaosu.manager.ui.component.KuaiMaoMenuItem
import com.kuaimaosu.manager.ui.component.KuaiMaoMenuPanel
import com.kuaimaosu.manager.ui.component.KuaiMaoTextField
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.ui.theme.KuaiMaoType
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Article
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material.icons.twotone.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.domain.model.AllowlistOperationResult
import com.kuaimaosu.manager.domain.model.InstalledAppGroup
import com.kuaimaosu.manager.domain.model.SuperUserLoadFailure
import com.kuaimaosu.manager.ui.component.ConfirmResult
import com.kuaimaosu.manager.ui.component.PackageIcon
import com.kuaimaosu.manager.ui.component.SearchAppBar
import com.kuaimaosu.manager.ui.component.SwipeableSnackbarHost
import com.kuaimaosu.manager.ui.component.WarningCard
import com.kuaimaosu.manager.ui.component.rememberConfirmDialog
import com.kuaimaosu.manager.ui.component.rememberSearchAppBarScrollBehavior
import com.kuaimaosu.manager.ui.component.settings.SettingsBaseWidget
import com.kuaimaosu.manager.ui.component.settings.lazySegmentColumn
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.navigation.Route
import com.kuaimaosu.manager.ui.screen.LabelText
import com.kuaimaosu.manager.ui.theme.blurSource
import com.kuaimaosu.manager.ui.util.LocalPagerPage
import com.kuaimaosu.manager.ui.util.LocalPagerState
import com.kuaimaosu.manager.ui.util.LocalSnackbarHost
import com.kuaimaosu.manager.ui.util.adaptiveScaffoldWindowInsets
import com.kuaimaosu.manager.ui.util.showReplacingSnackbar
import com.kuaimaosu.manager.ui.viewmodel.SortType
import com.kuaimaosu.manager.ui.viewmodel.SuperUserUiAction
import com.kuaimaosu.manager.ui.viewmodel.SuperUserUiEvent
import com.kuaimaosu.manager.ui.viewmodel.SuperUserUiState
import com.kuaimaosu.manager.ui.viewmodel.SuperUserViewModel
import org.koin.compose.viewmodel.koinViewModel

private data class SuperUserMenuItem(
    val checked: Boolean = false,
    val titleRes: Int,
    val onClick: () -> Unit,
    val closeOnClick: Boolean = true,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SuperUserPage(bottomPadding: Dp) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val viewModel = koinViewModel<SuperUserViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerPage = LocalPagerPage.current
    val isPageVisible = pagerPage == null || LocalPagerState.current.currentPage == pagerPage
    val scope = rememberCoroutineScope()
    val snackBarHostState = LocalSnackbarHost.current
    val restoreConfirmDialog = rememberConfirmDialog()
    val restoreConfirmTitle = stringResource(R.string.allowlist_restore_confirm_title)
    val restoreConfirmMessage = stringResource(R.string.allowlist_restore_confirm_message)
    val confirmText = stringResource(R.string.confirm)
    val cancelText = stringResource(R.string.cancel)

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SuperUserUiEvent.AllowlistOperationFinished -> {
                    val successMessage = if (event.restore) {
                        R.string.allowlist_restore_success
                    } else {
                        R.string.allowlist_backup_success
                    }
                    val failureMessage = if (event.restore) {
                        R.string.allowlist_restore_failed
                    } else {
                        R.string.allowlist_backup_failed
                    }
                    snackBarHostState.showReplacingSnackbar(
                        message = context.allowlistOperationMessage(
                            result = event.result,
                            successMessage = successMessage,
                            failureMessage = failureMessage,
                        ),
                        duration = SnackbarDuration.Long,
                    )
                }
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            viewModel.dispatch(SuperUserUiAction.BackupAllowlist(uri.toString()))
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val confirmed = restoreConfirmDialog.awaitConfirm(
                    title = restoreConfirmTitle,
                    content = restoreConfirmMessage,
                    confirm = confirmText,
                    dismiss = cancelText,
                )
                if (confirmed != ConfirmResult.Confirmed) return@launch

                viewModel.dispatch(SuperUserUiAction.RestoreAllowlist(uri.toString()))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.dispatch(SuperUserUiAction.Search(""))
    }

    LaunchedEffect(viewModel, isPageVisible) {
        if (isPageVisible) viewModel.dispatch(SuperUserUiAction.LoadInitialData)
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        // 标题与搜索固定在顶部，不跟随列表滚动
        topBar = {
            var showDropdown by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "超级用户", style = KuaiMaoType.pageTitle)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.appGroupList.isEmpty()) {
                                "授权与 App Profile"
                            } else {
                                "${uiState.appGroupList.size} 个应用 · 点击配置 Profile"
                            },
                            style = KuaiMaoType.pageSub,
                        )
                    }
                    Box {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(KuaiMaoColors.canvasCard)
                                .clickable { showDropdown = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.Tune,
                                contentDescription = stringResource(R.string.settings),
                                tint = KuaiMaoColors.textSec,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        SuperUserDropdown(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false },
                            viewModel = viewModel,
                            uiState = uiState,
                            onBackupAllowlist = {
                                backupLauncher.launch(createAllowlistBackupFileName())
                            },
                            onRestoreAllowlist = {
                                restoreLauncher.launch(arrayOf("application/octet-stream"))
                            },
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(KuaiMaoColors.canvasCard)
                            .clickable { navigator.push(Route.Sulog) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.TwoTone.Article,
                            contentDescription = stringResource(R.string.sulog),
                            tint = KuaiMaoColors.textSec,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                KuaiMaoTextField(
                    value = uiState.search,
                    onValueChange = { viewModel.dispatch(SuperUserUiAction.Search(it)) },
                    placeholder = stringResource(R.string.search_apps),
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        },
        snackbarHost = {
            SwipeableSnackbarHost(
                modifier = Modifier.padding(bottom = bottomPadding),
                hostState = snackBarHostState,
            )
        },
        contentWindowInsets = adaptiveScaffoldWindowInsets(includeBottom = false),
    ) { innerPadding ->
        SuperUserContent(
            innerPadding = innerPadding,
            viewModel = viewModel,
            uiState = uiState,
            bottomPadding = bottomPadding,
            onBackupAllowlist = { backupLauncher.launch(createAllowlistBackupFileName()) },
            onRestoreAllowlist = { restoreLauncher.launch(arrayOf("application/octet-stream")) },
            onSulogClick = { navigator.push(Route.Sulog) },
            onAppClick = { appGroup ->
                navigator.push(Route.AppProfile(appGroup.uid, appGroup.profileKey))
            },
        )
    }
}

private fun Context.allowlistOperationMessage(
    result: AllowlistOperationResult,
    successMessage: Int,
    failureMessage: Int,
): String = when (result) {
    AllowlistOperationResult.Success ->
        getString(successMessage)

    AllowlistOperationResult.InvalidFile ->
        getString(failureMessage, getString(R.string.unknown_file))

    AllowlistOperationResult.UnsupportedVersion ->
        getString(failureMessage, getString(R.string.home_unsupported))

    is AllowlistOperationResult.ProfileUpdateFailed ->
        getString(
            failureMessage,
            getString(R.string.failed_to_update_app_profile, result.uid.toString()),
        )

    is AllowlistOperationResult.Failed ->
        getString(
            failureMessage,
            result.cause?.localizedMessage ?: getString(R.string.unknown),
        )
}

@Composable
private fun SuperUserLoadError(failure: SuperUserLoadFailure, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val message = stringResource(failure.resId)
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WarningCard(message = message)
        FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.network_retry)) }
    }
}

private fun createAllowlistBackupFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return "ksu_allowlist_backup_$timestamp.dat"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SuperUserContent(
    innerPadding: PaddingValues,
    viewModel: SuperUserViewModel,
    uiState: SuperUserUiState,
    bottomPadding: Dp,
    onBackupAllowlist: () -> Unit,
    onRestoreAllowlist: () -> Unit,
    onSulogClick: () -> Unit,
    onAppClick: (InstalledAppGroup) -> Unit,
) {
    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    PullToRefreshBox(
        state = pullRefreshState,
        onRefresh = { viewModel.dispatch(SuperUserUiAction.Refresh) },
        isRefreshing = uiState.isRefreshing,
        modifier = Modifier
            .fillMaxSize()
            .blurSource(),
        // 列表为空时由下方空态卡片显示唯一的加载指示，避免出现两个 spinner
        indicator = {
            if (uiState.isRefreshing && uiState.appGroupList.isNotEmpty()) {
                PullToRefreshDefaults.LoadingIndicator(
                    modifier = Modifier
                        .padding(top = innerPadding.calculateTopPadding())
                        .align(Alignment.TopCenter),
                    state = pullRefreshState,
                    isRefreshing = uiState.isRefreshing,
                )
            }
        },
    ) {
        LazyColumn(
            state = listState,
            // 用 padding 把列表真正挪到顶栏下方，而不是 contentPadding：
            // contentPadding 只偏移内容，列表本身仍占满全屏，滚动时 item 会画到
            // 透明顶栏区域里（就是「内容透过去」）。padding 缩小了列表自身边界，
            // 滚动内容会被裁在顶栏下方。
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = innerPadding.calculateBottomPadding() + bottomPadding + 28.dp,
            ),
        ) {
            uiState.loadFailure?.let { failure ->
                item(key = "load-error") {
                    SuperUserLoadError(
                        failure = failure,
                        onRetry = { viewModel.dispatch(SuperUserUiAction.Refresh) },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }

            if (uiState.appGroupList.isEmpty()) {
                item {
                    when {
                        uiState.isRefreshing -> KuaiMaoCard(
                            contentPadding = PaddingValues(vertical = 40.dp, horizontal = 22.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                LoadingIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp),
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "正在读取授权列表…",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KuaiMaoColors.textMuted,
                                )
                            }
                        }

                        else -> KuaiMaoCard(
                            contentPadding = PaddingValues(vertical = 34.dp, horizontal = 22.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(KuaiMaoTones.unified.container),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (uiState.search.isNotEmpty()) {
                                            Icons.TwoTone.SearchOff
                                        } else {
                                            Icons.TwoTone.Person
                                        },
                                        contentDescription = null,
                                        tint = KuaiMaoTones.unified.content,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (uiState.search.isNotEmpty()) {
                                        stringResource(R.string.no_apps_found)
                                    } else {
                                        "暂无应用请求 Root"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = KuaiMaoColors.textBody,
                                    textAlign = TextAlign.Center,
                                )
                                if (uiState.search.isEmpty()) {
                                    Spacer(modifier = Modifier.height(7.dp))
                                    Text(
                                        text = "当应用执行 su 请求时会出现在这里，\n也可以点击应用配置独立的 Profile",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 17.sp,
                                        color = KuaiMaoColors.textMuted,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = uiState.appGroupList,
                    key = { "${it.uid}-${it.profileKey}" },
                ) { appGroup ->
                    AppGroupItem(
                        appGroup = appGroup,
                        isManager = appGroup.uid in uiState.managerUids,
                    ) {
                        onAppClick(appGroup)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SuperUserDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    viewModel: SuperUserViewModel,
    uiState: SuperUserUiState,
    onBackupAllowlist: () -> Unit,
    onRestoreAllowlist: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(20.dp),
        offset = DpOffset(x = 0.dp, y = 6.dp),
    ) {
        KuaiMaoMenuPanel {
            KuaiMaoMenuLabel("排序")

            SortType.entries.forEach { sortType ->
                val selected = uiState.currentSortType == sortType
                KuaiMaoMenuItem(
                    text = stringResource(sortType.displayNameRes),
                    selected = selected,
                    showCheck = true,
                    onClick = {
                        viewModel.dispatch(SuperUserUiAction.SetSort(sortType))
                    },
                )
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(KuaiMaoColors.line),
            )

            KuaiMaoMenuItem(
                text = stringResource(R.string.reverse_order),
                selected = uiState.reverseOrder,
                showCheck = true,
                onClick = {
                    viewModel.dispatch(SuperUserUiAction.SetReverseOrder(!uiState.reverseOrder))
                },
            )
            KuaiMaoMenuItem(
                text = stringResource(R.string.show_system_apps),
                selected = uiState.showSystemApps,
                showCheck = true,
                onClick = {
                    viewModel.dispatch(SuperUserUiAction.SetShowSystemApps(!uiState.showSystemApps))
                },
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(KuaiMaoColors.line),
            )

            KuaiMaoMenuItem(
                text = stringResource(R.string.backup_allowlist),
                onClick = {
                    onDismissRequest()
                    onBackupAllowlist()
                },
            )
            KuaiMaoMenuItem(
                text = stringResource(R.string.restore_allowlist),
                onClick = {
                    onDismissRequest()
                    onRestoreAllowlist()
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AppGroupItem(
    appGroup: InstalledAppGroup,
    isManager: Boolean,
    onClick: () -> Unit,
) {
    val mainApp = appGroup.mainApp
    KuaiMaoCard(
        modifier = Modifier.padding(bottom = 8.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PackageIcon(
                packageName = if (appGroup.isWebViewZygote) "android" else mainApp.packageName,
                contentDescription = mainApp.label,
                modifier = Modifier.size(44.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mainApp.label,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = KuaiMaoColors.textBody,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (appGroup.apps.size > 1) {
                        stringResource(R.string.group_contains_apps, appGroup.apps.size)
                    } else {
                        mainApp.displayIdentifier
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = KuaiMaoColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (appGroup.allowSu) {
                        LabelText(label = "ROOT")
                    } else if (appGroup.shouldUmount) {
                        LabelText(
                            label = "UMOUNT",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        )
                    }
                    if (appGroup.hasCustomProfile) {
                        LabelText(
                            label = "CUSTOM",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        )
                    } else if (!appGroup.allowSu) {
                        LabelText(
                            label = "DEFAULT",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    }
                    if (isManager) {
                        LabelText(
                            label = "MANAGER",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        )
                    }
                    if (appGroup.isRecentlyInstalled) {
                        LabelText(
                            label = stringResource(R.string.recently_installed),
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.TwoTone.ChevronRight,
                contentDescription = null,
                tint = KuaiMaoColors.chevron,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
