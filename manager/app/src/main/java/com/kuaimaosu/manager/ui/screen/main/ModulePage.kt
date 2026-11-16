package com.kuaimaosu.manager.ui.screen.main

import com.kuaimaosu.manager.ui.component.KuaiMaoTextField
import com.kuaimaosu.manager.ui.theme.KuaiMaoType
import android.annotation.SuppressLint
import android.app.Activity.CLIPBOARD_SERVICE
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Undo
import androidx.compose.material.icons.automirrored.twotone.Wysiwyg
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.Cloud
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Download
import androidx.compose.material.icons.twotone.Extension
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material.icons.twotone.PlayArrow
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.RevealItem
import com.kuaimaosu.manager.ui.component.SkeletonBone
import com.kuaimaosu.manager.ui.component.SkeletonLine
import com.kuaimaosu.manager.ui.component.rememberSkeletonPhase
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.component.KuaiMaoSwitch
import com.kuaimaosu.manager.ui.component.KuaiMaoGhostButton
import com.kuaimaosu.manager.ui.component.KuaiMaoPrimaryButton
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.data.settings.SettingsPlatformRepository
import com.kuaimaosu.manager.domain.model.DocumentFormat
import com.kuaimaosu.manager.domain.model.DocumentPolicy
import com.kuaimaosu.manager.domain.model.InstalledModule
import com.kuaimaosu.manager.domain.model.MetaModuleStatus
import com.kuaimaosu.manager.domain.model.ModuleReadme
import com.kuaimaosu.manager.domain.model.ModuleUpdateVerification
import com.kuaimaosu.manager.domain.usecase.EnqueueDownloadUseCase
import com.kuaimaosu.manager.domain.usecase.ExtractModuleNameUseCase
import com.kuaimaosu.manager.domain.usecase.IsModuleUriAccessibleUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveDownloadUseCase
import com.kuaimaosu.manager.domain.usecase.PeekOneshotScriptUseCase
import com.kuaimaosu.manager.domain.usecase.TakeModuleUriPermissionUseCase
import com.kuaimaosu.manager.ui.component.ConfirmResult
import com.kuaimaosu.manager.ui.component.InstallConfirmationDialog
import com.kuaimaosu.manager.ui.component.SearchAppBar
import com.kuaimaosu.manager.ui.component.SwipeableSnackbarHost
import com.kuaimaosu.manager.ui.component.WarningCard
import com.kuaimaosu.manager.ui.component.ZipFileDetector
import com.kuaimaosu.manager.ui.component.ZipFileInfo
import com.kuaimaosu.manager.ui.component.ZipType
import com.kuaimaosu.manager.ui.component.rememberConfirmDialog
import com.kuaimaosu.manager.ui.component.rememberLoadingDialog
import com.kuaimaosu.manager.ui.component.onAccentContent
import com.kuaimaosu.manager.ui.component.rememberSearchAppBarScrollBehavior
import com.kuaimaosu.manager.ui.component.settings.SegmentedColumn
import com.kuaimaosu.manager.ui.component.settings.SettingsBaseWidget
import com.kuaimaosu.manager.ui.component.settings.SettingsJumpPageWidget
import com.kuaimaosu.manager.ui.component.settings.SettingsTextFieldWidget
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.navigation.Route
import com.kuaimaosu.manager.ui.screen.LabelText
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.theme.blurSource
import com.kuaimaosu.manager.ui.theme.renderBackgroundBlur
import com.kuaimaosu.manager.ui.util.LocalPermissionRequestInterface
import com.kuaimaosu.manager.ui.util.LocalSnackbarHost
import com.kuaimaosu.manager.ui.util.Shortcut
import com.kuaimaosu.manager.ui.util.adaptiveScaffoldWindowInsets
import com.kuaimaosu.manager.ui.util.downloader.download
import com.kuaimaosu.manager.ui.util.downloader.downloadFailureMessage
import com.kuaimaosu.manager.ui.util.showReplacingSnackbar
import com.kuaimaosu.manager.ui.viewmodel.HomeViewModel
import com.kuaimaosu.manager.ui.viewmodel.ModuleUiAction
import com.kuaimaosu.manager.ui.viewmodel.ModuleUiEvent
import com.kuaimaosu.manager.ui.viewmodel.ModuleUiState
import com.kuaimaosu.manager.ui.viewmodel.ModuleViewModel
import com.kuaimaosu.manager.ui.webui.WebUIActivity
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

enum class ShortcutType {
    Action,
    WebUI,
}

/**
 * @author ShirkNeko
 * @date 2025/9/29.
 */
@SuppressLint("ResourceType", "AutoboxingStateCreation")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModulePage(bottomPadding: Dp) {
    val isModuleUriAccessible = koinInject<IsModuleUriAccessibleUseCase>()
    val takeModuleUriPermission = koinInject<TakeModuleUriPermissionUseCase>()
    val extractModuleName = koinInject<ExtractModuleNameUseCase>()
    val zipFileDetector = koinInject<ZipFileDetector>()
    val peekScript = koinInject<PeekOneshotScriptUseCase>()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val viewModel = koinViewModel<ModuleViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val homeState by koinViewModel<HomeViewModel>().uiState.collectAsStateWithLifecycle()
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    var lastClickTime by remember { mutableStateOf(0L) }

    var showDropdown by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var pendingZipFiles by remember { mutableStateOf<List<ZipFileInfo>>(emptyList()) }
    InstallConfirmationDialog(
        show = showConfirmationDialog,
        zipFiles = pendingZipFiles,
        onConfirm = { info ->
            showConfirmationDialog = false
            navigator.push(
                Route.Flash.modules(
                    info.filter { it.type == ZipType.MODULE }
                        .map { it.uri.toString() },
                ),
            )
            viewModel.dispatch(ModuleUiAction.MarkNeedRefresh)
        },
        onDismiss = {
            showConfirmationDialog = false
            pendingZipFiles = emptyList()
        },
    )

    val selectZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (it.resultCode != RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        val data = it.data ?: return@rememberLauncherForActivityResult

        scope.launch {
            val zipFiles = mutableListOf<ZipFileInfo>()
            val clipData = data.clipData
            if (clipData != null) {
                val selectedModules = mutableListOf<Uri>()
                val selectedModuleNames = mutableMapOf<Uri, String>()

                suspend fun processUri(uri: Uri) {
                    try {
                        val uriString = uri.toString()
                        if (!isModuleUriAccessible(uriString)) {
                            return
                        }
                        takeModuleUriPermission(uriString)
                        val moduleName =
                            withContext(Dispatchers.IO) { extractModuleName(uriString) }
                        selectedModules.add(uri)
                        selectedModuleNames[uri] = moduleName
                    } catch (e: Exception) {
                        Log.e("ModuleScreen", "Error while processing URI: $uri, Error: ${e.message}")
                    }
                }

                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    processUri(uri)
                }

                if (selectedModules.isEmpty()) {
                    snackBarHost.showReplacingSnackbar("Unable to access selected module files")
                    return@launch
                }
                selectedModules.singleOrNull()?.let { uri ->
                    peekScript(uri.toString())?.let { manifest ->
                        navigator.push(Route.ScriptRun(uri.toString(), offerInstall = true, displayName = manifest.name))
                        return@launch
                    }
                }
                selectedModules.forEach { it ->
                    val info =
                        withContext(Dispatchers.IO) { zipFileDetector.parseModuleInfo(context, it) }
                    zipFiles.add(info)
                }
                pendingZipFiles = zipFiles

                showConfirmationDialog = true
            } else {
                val uri = data.data ?: return@launch
                // 单个安装模块
                try {
                    val uriString = uri.toString()
                    if (!isModuleUriAccessible(uriString)) {
                        snackBarHost.showReplacingSnackbar("Unable to access selected module files")
                        return@launch
                    }

                    takeModuleUriPermission(uriString)

                    peekScript(uriString)?.let { manifest ->
                        navigator.push(Route.ScriptRun(uriString, offerInstall = true, displayName = manifest.name))
                        return@launch
                    }

                    val info = withContext(Dispatchers.IO) {
                        zipFileDetector.parseModuleInfo(
                            context,
                            uri,
                        )
                    }
                    zipFiles.add(info)
                    pendingZipFiles = zipFiles

                    showConfirmationDialog = true
                } catch (e: Exception) {
                    Log.e("ModuleScreen", "Error processing a single URI: $uri, Error: ${e.message}")
                    snackBarHost.showReplacingSnackbar("Error processing module file: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.dispatch(ModuleUiAction.Search(""))
        if (uiState.moduleList.isEmpty() || uiState.isNeedRefresh) {
            viewModel.dispatch(ModuleUiAction.Refresh())
        }
    }

    val isSafeMode = homeState.systemStatus.isSafeMode
    val hideInstallButton = isSafeMode || uiState.hasMagisk

    Scaffold(
        // 标题 / 搜索 / 动作按钮固定在顶部，不跟随列表滚动
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                ModuleHeader(
                    viewModel = viewModel,
                    uiState = uiState,
                    showActions = uiState.moduleList.isNotEmpty(),
                    onInstallClick = {
                        selectZipLauncher.launch(
                            Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            },
                        )
                    },
                    onRepoClick = { navigator.push(Route.ModuleRepo) },
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = adaptiveScaffoldWindowInsets(includeBottom = false),
        snackbarHost = {
            SwipeableSnackbarHost(
                hostState = snackBarHost,
            )
        },
    ) { innerPadding ->
        when {
            uiState.hasMagisk -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.Warning,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .padding(bottom = 16.dp),
                        )
                        Text(
                            stringResource(R.string.module_magisk_conflict),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            uiState.moduleList.isEmpty() -> {
                val searching = uiState.search.isNotEmpty()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    KuaiMaoCard(
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
                                    imageVector = Icons.TwoTone.Extension,
                                    contentDescription = null,
                                    tint = KuaiMaoTones.unified.content,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searching) {
                                    stringResource(R.string.search_no_any_match)
                                } else {
                                    stringResource(R.string.module_empty)
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = KuaiMaoColors.textBody,
                                textAlign = TextAlign.Center,
                            )
                            if (!searching) {
                                Spacer(modifier = Modifier.height(7.dp))
                                Text(
                                    text = "从本地存储安装 zip 模块，\n或前往模块仓库在线浏览",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 17.sp,
                                    color = KuaiMaoColors.textMuted,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    if (!searching) {
                        Spacer(modifier = Modifier.height(16.dp))
                        KuaiMaoPrimaryButton(
                            text = "安装模块",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectZipLauncher.launch(
                                    Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                    },
                                )
                            },
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        KuaiMaoGhostButton(
                            text = "模块仓库",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { navigator.push(Route.ModuleRepo) },
                        )
                    }
                }
            }

            else -> {
                ModuleList(
                    viewModel = viewModel,
                    uiState = uiState,
                    listState = listState,
                    modifier = Modifier,
                    onInstallClick = {
                        selectZipLauncher.launch(
                            Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            },
                        )
                    },
                    onRepoClick = { navigator.push(Route.ModuleRepo) },
                    onUpdateModule = {
                        navigator.push(Route.Flash.moduleUpdate(it.toString()))
                    },
                    onClickModule = { id, hasWebUi ->
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < 600) {
                            Log.d("ModuleScreen", "Click too fast, ignoring")
                            return@ModuleList
                        }
                        lastClickTime = currentTime

                        if (hasWebUi) {
                            try {
                                context.startActivity(
                                    Intent(context, WebUIActivity::class.java)
                                        .setData(
                                            Uri.Builder()
                                                .scheme("kernelsu")
                                                .authority("webui")
                                                .appendQueryParameter("id", id)
                                                .build(),
                                        ),
                                )
                            } catch (e: Exception) {
                                Log.e("ModuleScreen", "Error launching WebUI: ${e.message}", e)
                                scope.launch {
                                    snackBarHost.showReplacingSnackbar("Error launching WebUI: ${e.message}")
                                }
                            }
                            return@ModuleList
                        }
                    },
                    context = context,
                    snackBarHost = snackBarHost,
                    bottomPadding = bottomPadding + innerPadding.calculateBottomPadding(),
                    topPadding = innerPadding.calculateTopPadding(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModuleDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    viewModel: ModuleViewModel,
    uiState: ModuleUiState,
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShapes(),
        ) {
            CheckableDropdownMenuItem(
                checked = uiState.sortActionFirst,
                onCheckedChange = {
                    viewModel.dispatch(
                        ModuleUiAction.Sort(uiState.sortEnabledFirst, it),
                    )
                },
                text = { Text(stringResource(R.string.module_sort_action_first)) },
                shapes = MenuDefaults.itemShape(
                    index = 0,
                    count = 2,
                ),
            )
            CheckableDropdownMenuItem(
                checked = uiState.sortEnabledFirst,
                onCheckedChange = {
                    viewModel.dispatch(
                        ModuleUiAction.Sort(it, uiState.sortActionFirst),
                    )
                },
                text = { Text(stringResource(R.string.module_sort_enabled_first)) },
                shapes = MenuDefaults.itemShape(
                    index = 1,
                    count = 2,
                ),
            )
        }
    }
}

private fun getMetaModuleWarningText(
    hasModuleRequireMount: Boolean,
    showWarning: Boolean,
    context: Context,
    status: MetaModuleStatus,
): String? {
    if (!showWarning) return null
    if (!hasModuleRequireMount) return null

    return status.resId?.let { context.getString(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetaModuleWarningCard(
    text: String,
    visible: Boolean,
    onClose: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        WarningCard(
            shape = CardDefaults.elevatedShape,
            message = text,
            onClose = onClose,
        )

        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

/**
 * 模块页头部：大标题 + 计数 + 筛选入口 + 搜索框 + 安装/仓库按钮。
 */
@Composable
private fun ModuleHeader(
    viewModel: ModuleViewModel,
    uiState: ModuleUiState,
    showActions: Boolean,
    onInstallClick: () -> Unit,
    onRepoClick: () -> Unit,
) {
    var showDropdown by remember { mutableStateOf(false) }
    val enabledCount = uiState.moduleList.count { it.enabled }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "模块", style = KuaiMaoType.pageTitle)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (uiState.moduleList.isEmpty()) {
                        "已安装的内核模块"
                    } else {
                        "${uiState.moduleList.size} 个模块 · 已启用 $enabledCount"
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
                        imageVector = Icons.TwoTone.MoreVert,
                        contentDescription = stringResource(R.string.settings),
                        tint = KuaiMaoColors.textSec,
                        modifier = Modifier.size(19.dp),
                    )
                }
                ModuleDropdown(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    viewModel = viewModel,
                    uiState = uiState,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        KuaiMaoTextField(
            value = uiState.search,
            onValueChange = { viewModel.dispatch(ModuleUiAction.Search(it)) },
            placeholder = stringResource(R.string.search_modules),
        )

        if (showActions) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KuaiMaoPrimaryButton(
                    text = "安装模块",
                    modifier = Modifier.weight(1f),
                    onClick = onInstallClick,
                )
                KuaiMaoGhostButton(
                    text = "模块仓库",
                    modifier = Modifier.weight(1f),
                    onClick = onRepoClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModuleList(
    viewModel: ModuleViewModel,
    uiState: ModuleUiState,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    boxModifier: Modifier = Modifier,
    onUpdateModule: (Uri) -> Unit,
    onClickModule: (id: String, hasWebUi: Boolean) -> Unit,
    context: Context,
    snackBarHost: SnackbarHostState,
    bottomPadding: Dp,
    topPadding: Dp,
    onInstallClick: () -> Unit,
    onRepoClick: () -> Unit,
) {
    val shortcut = koinInject<Shortcut>()
    val settings = koinInject<SettingsPlatformRepository>()
    var showMetaModuleWarning by rememberSaveable { mutableStateOf(true) }
    val enqueueDownload = koinInject<EnqueueDownloadUseCase>()
    val observeDownload = koinInject<ObserveDownloadUseCase>()
    val permissionRequestInterface = LocalPermissionRequestInterface.current
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullToRefreshState()
    val failedEnable = stringResource(R.string.module_failed_to_enable)
    val failedDisable = stringResource(R.string.module_failed_to_disable)
    val failedUninstall = stringResource(R.string.module_uninstall_failed)
    val successUninstall = stringResource(R.string.module_uninstall_success)
    val reboot = stringResource(R.string.reboot)
    val rebootToApply = stringResource(R.string.reboot_to_apply)
    val moduleStr = stringResource(R.string.module)
    val uninstall = stringResource(R.string.uninstall)
    val cancel = stringResource(android.R.string.cancel)
    val moduleUninstallConfirm = stringResource(R.string.module_uninstall_confirm)
    val metaModuleUninstallConfirm = stringResource(R.string.metamodule_uninstall_confirm)
    val updateText = stringResource(R.string.module_update)
    val changelogText = stringResource(R.string.module_changelog)
    val downloadingText = stringResource(R.string.module_downloading)
    val startDownloadingText = stringResource(R.string.module_start_downloading)

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ModuleUiEvent.EnabledChanged -> {
                    val moduleName = uiState.moduleList
                        .find { it.dirId == event.moduleId }
                        ?.name ?: event.moduleId
                    if (event.successful) {
                        val result = snackBarHost.showReplacingSnackbar(
                            message = rebootToApply,
                            actionLabel = reboot,
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.dispatch(ModuleUiAction.Reboot)
                        }
                    } else {
                        val message = if (event.enabled) failedEnable else failedDisable
                        snackBarHost.showReplacingSnackbar(message.format(moduleName))
                    }
                }

                is ModuleUiEvent.RemovedChanged -> {
                    val moduleName = uiState.moduleList
                        .find { it.dirId == event.moduleId }
                        ?.name ?: event.moduleId
                    if (event.successful) {
                        viewModel.dispatch(ModuleUiAction.MarkNeedRefresh)
                        viewModel.dispatch(ModuleUiAction.Refresh())
                    }
                    if (event.removed) {
                        val message = if (event.successful) successUninstall else failedUninstall
                        val result = snackBarHost.showReplacingSnackbar(
                            message = message.format(moduleName),
                            actionLabel = reboot.takeIf { event.successful },
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.dispatch(ModuleUiAction.Reboot)
                        }
                    }
                }

                is ModuleUiEvent.Error -> if (event.message.isNotBlank()) {
                    snackBarHost.showReplacingSnackbar(event.message)
                }

                ModuleUiEvent.RefreshCompleted -> Unit
            }
        }
    }

    val loadingDialog = rememberLoadingDialog()
    val confirmDialog = rememberConfirmDialog()

    var shortcutModuleId by rememberSaveable { mutableStateOf<String?>(null) }
    val textFieldState = rememberTextFieldState()
    var shortcutIconUri by rememberSaveable { mutableStateOf<String?>(null) }
    var defaultShortcutIconUri by rememberSaveable { mutableStateOf<String?>(null) }
    var defaultActionShortcutIconUri by rememberSaveable { mutableStateOf<String?>(null) }
    var defaultWebUiShortcutIconUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedShortcutType by rememberSaveable { mutableStateOf<ShortcutType?>(null) }
    val showShortcutDialog = remember { mutableStateOf(false) }

    fun openShortcutDialogForType(type: ShortcutType) {
        selectedShortcutType = type
        val defaultIcon = when (type) {
            ShortcutType.Action -> defaultActionShortcutIconUri ?: defaultWebUiShortcutIconUri
            ShortcutType.WebUI -> defaultWebUiShortcutIconUri ?: defaultActionShortcutIconUri
        }
        defaultShortcutIconUri = defaultIcon
        shortcutIconUri = defaultIcon
        showShortcutDialog.value = true
    }

    fun hasModuleShortcut(context: Context, moduleId: String, type: ShortcutType): Boolean = when (type) {
        ShortcutType.Action -> shortcut.hasModuleActionShortcut(context, moduleId)
        ShortcutType.WebUI -> shortcut.hasModuleWebUiShortcut(context, moduleId)
    }

    fun deleteModuleShortcut(context: Context, moduleId: String, type: ShortcutType) {
        when (type) {
            ShortcutType.Action -> shortcut.deleteModuleActionShortcut(context, moduleId)
            ShortcutType.WebUI -> shortcut.deleteModuleWebUiShortcut(context, moduleId)
        }
    }

    fun createModuleShortcut(
        context: Context,
        moduleId: String,
        name: String,
        iconUri: String?,
        type: ShortcutType,
        intentToken: String,
    ) {
        when (type) {
            ShortcutType.Action -> {
                shortcut.createModuleActionShortcut(
                    context = context,
                    moduleId = moduleId,
                    name = name,
                    iconUri = iconUri,
                    intentToken = intentToken,
                )
            }

            ShortcutType.WebUI -> {
                shortcut.createModuleWebUiShortcut(
                    context = context,
                    moduleId = moduleId,
                    name = name,
                    iconUri = iconUri,
                    intentToken = intentToken,
                )
            }
        }
    }

    val pickShortcutIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        shortcutIconUri = uri?.toString()
    }

    val shortcutPreviewIcon = remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(shortcutIconUri) {
        val uriStr = shortcutIconUri
        if (uriStr.isNullOrBlank()) {
            shortcutPreviewIcon.value = null
            return@LaunchedEffect
        }
        val bitmap = withContext(Dispatchers.IO) {
            shortcut.loadShortcutBitmap(context, uriStr)
        }
        shortcutPreviewIcon.value = bitmap?.asImageBitmap()
    }

    var hasExistingShortcut by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(shortcutModuleId, selectedShortcutType, showShortcutDialog.value) {
        val moduleId = shortcutModuleId
        val type = selectedShortcutType
        if (!showShortcutDialog.value || moduleId.isNullOrBlank() || type == null) {
            hasExistingShortcut = false
            return@LaunchedEffect
        }
        val exists = withContext(Dispatchers.IO) {
            hasModuleShortcut(context, moduleId, type)
        }
        hasExistingShortcut = exists
    }

    suspend fun onModuleUpdate(
        module: InstalledModule,
        changelogUrl: String,
        downloadUrl: String,
        fileName: String,
    ) {
        val showToast: suspend (String) -> Unit = { msg ->
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    msg,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        val verification = loadingDialog.withLoading { viewModel.resolveUpdate(module) }
        if (verification == ModuleUpdateVerification.Rejected) {
            showToast(context.getString(R.string.repo_update_unverifiable))
            return
        }
        val registry = verification as? ModuleUpdateVerification.Registry
        val canInstall = { registry == null || viewModel.isUpdateCurrent(module, registry) }
        val changelogResult = if (registry != null) {
            Result.success(
                ModuleReadme(
                    registry.release.descriptionHTML.ifBlank { module.description },
                    registry.repositoryUrl,
                    if (registry.release.descriptionHTML.isNotBlank()) DocumentFormat.HTML else DocumentFormat.MARKDOWN,
                    DocumentPolicy.SameOrigin(registry.repositoryUrl),
                ),
            )
        } else {
            loadingDialog.withLoading { viewModel.loadUpdateChangelog(changelogUrl, module.updateJson) }
        }
        val changelog = changelogResult.getOrElse {
            showToast(context.getString(R.string.repo_document_error))
            return
        }

        val confirmResult = confirmDialog.awaitConfirm(
            changelogText,
            content = changelog.content,
            document = changelog,
            confirm = updateText,
        )

        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }
        if (!canInstall()) {
            showToast(context.getString(R.string.repo_update_unverifiable))
            return
        }

        showToast(startDownloadingText.format(module.name))

        val downloading = downloadingText.format(module.name)
        withContext(Dispatchers.IO) {
            download(
                context,
                permissionRequestInterface,
                registry?.zipUrl ?: downloadUrl,
                fileName,
                enqueueDownload,
                observeDownload,
                expected = registry?.expected,
                canEnqueue = canInstall,
                onFailed = { failed -> Toast.makeText(context, downloadFailureMessage(context, failed), Toast.LENGTH_LONG).show() },
                onDownloaded = { uri ->
                    if (canInstall()) onUpdateModule(uri)
                },
                onDownloading = {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, downloading, Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }
    }

    suspend fun onModuleUninstallClicked(module: InstalledModule) {
        val isUninstall = !module.remove
        if (isUninstall) {
            val formatter = if (module.metamodule) metaModuleUninstallConfirm else moduleUninstallConfirm
            val confirmResult = confirmDialog.awaitConfirm(
                moduleStr,
                content = formatter.format(module.name),
                confirm = uninstall,
                dismiss = cancel,
            )
            if (confirmResult != ConfirmResult.Confirmed) {
                return
            }
        }

        if (isUninstall) {
            withContext(Dispatchers.IO) {
                shortcut.deleteModuleActionShortcut(context, module.id)
                shortcut.deleteModuleWebUiShortcut(context, module.id)
            }
        }
        viewModel.dispatch(ModuleUiAction.SetRemoved(module.dirId, isUninstall))
    }

    fun onModuleAddShortcut(module: InstalledModule, shortcutType: ShortcutType) {
        shortcutModuleId = module.id
        textFieldState.edit {
            replace(0, length, module.name)
        }
        shortcutIconUri = null
        defaultShortcutIconUri = null
        defaultActionShortcutIconUri = module.actionIconPath
            ?.takeIf { it.isNotBlank() }
            ?.let { "su:$it" }
        defaultWebUiShortcutIconUri = module.webUiIconPath
            ?.takeIf { it.isNotBlank() }
            ?.let { "su:$it" }
        openShortcutDialogForType(shortcutType)
    }

    PullToRefreshBox(
        state = pullRefreshState,
        onRefresh = {
            viewModel.dispatch(ModuleUiAction.Refresh(manual = true))
        },
        modifier = boxModifier
            .fillMaxSize()
            .blurSource(),
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                modifier = Modifier
                    .padding(top = topPadding)
                    .align(Alignment.TopCenter),
                state = pullRefreshState,
                isRefreshing = uiState.isRefreshing,
            )
        },
        isRefreshing = uiState.isRefreshing,
    ) {
        val metaModuleWarningText by produceState<String?>(
            initialValue = null,
            uiState.hasModuleRequireMount,
            showMetaModuleWarning,
            uiState.metaModuleStatus,
        ) {
            value = withContext(Dispatchers.IO) {
                getMetaModuleWarningText(
                    uiState.hasModuleRequireMount,
                    showMetaModuleWarning,
                    context,
                    uiState.metaModuleStatus,
                )
            }
        }

        LazyColumn(
            state = listState,
            // 列表自身从顶栏下方开始，滚动内容才会被裁住，不会透到透明顶栏上。
            modifier = modifier.padding(top = topPadding),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 24.dp,
            ),
        ) {

            if (metaModuleWarningText != null) {
                item(
                    key = "warning",
                ) {
                    MetaModuleWarningCard(
                        text = metaModuleWarningText!!,
                        visible = showMetaModuleWarning,
                        onClose = { showMetaModuleWarning = false },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 首次读取模块列表时先铺骨架：标题条 + 版本条 + 说明条 + 右侧开关位，
            // 和真实卡片的圆角、内边距、行距完全一致。
            if (uiState.moduleList.isEmpty() && uiState.isRefreshing) {
                item(key = "module-skeleton") {
                    ModuleSkeletonList()
                }
            }

            itemsIndexed(
                items = uiState.moduleList,
                key = { _, item -> "module-${item.id}" },
            ) { index, module ->
                ModuleItem(
                    viewModel = viewModel,
                    module = module,
                    moduleSizes = uiState.moduleSizes,
                    updateUrl = module.moduleUpdate?.zipUrl.orEmpty(),
                    onUninstallClicked = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                onModuleUninstallClicked(module)
                            }
                        }
                    },
                    onCheckChanged = { enabled ->
                        viewModel.dispatch(ModuleUiAction.SetEnabled(module.dirId, enabled))
                        true
                    },
                    onUpdate = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                onModuleUpdate(
                                    module,
                                    module.moduleUpdate!!.changelog,
                                    module.moduleUpdate.zipUrl,
                                    "${module.name}-${module.moduleUpdate.version}.zip",
                                )
                            }
                        }
                    },
                    onClick = {
                        onClickModule(it.dirId, it.hasWebUi)
                    },
                    onModuleAddShortcut = { module, type ->
                        onModuleAddShortcut(module, type)
                    },
                    showMoreModuleInfo = uiState.showMoreModuleInfo,
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Spacer(modifier = Modifier.height(bottomPadding))
            }
        }
    }

    if (showShortcutDialog.value) {
        ModalBottomSheet(
            sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
            onDismissRequest = {
                showShortcutDialog.value = false
            },
        ) {
            var error by remember { mutableStateOf("") }
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.module_shortcut_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(horizontal = 24.dp),
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .size(100.dp)
                        .clip(ContinuousRoundedRectangle(25.dp)),
                ) {
                    val preview = shortcutPreviewIcon.value
                    if (preview != null) {
                        Image(
                            bitmap = preview,
                            modifier = Modifier.size(100.dp),
                            contentDescription = null,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color.White),
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground_alt),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                SegmentedColumn {
                    if (shortcutIconUri == defaultShortcutIconUri) {
                        item {
                            SettingsBaseWidget(
                                iconPlaceholder = false,
                                isOnBackground = false,
                                title = stringResource(id = R.string.module_shortcut_icon_pick),
                                onClick = {
                                    pickShortcutIconLauncher.launch("image/*")
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.TwoTone.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    } else {
                        item {
                            SettingsBaseWidget(
                                iconPlaceholder = false,
                                isOnBackground = false,
                                title = stringResource(id = R.string.restore),
                                onClick = {
                                    shortcutIconUri = defaultShortcutIconUri
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.TwoTone.Undo,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }

                    item {
                        val shouldNotEmpty =
                            stringResource(R.string.module_shortcut_should_not_empty)
                        SettingsTextFieldWidget(
                            state = textFieldState,
                            title = stringResource(id = R.string.module_shortcut_name_label),
                            error = error,
                            renderBackgroundBlur = false,
                        )

                        LaunchedEffect(textFieldState.text) {
                            error = if (textFieldState.text.isBlank()) {
                                shouldNotEmpty
                            } else {
                                ""
                            }
                        }
                    }

                    if (hasExistingShortcut) {
                        item {
                            SettingsJumpPageWidget(
                                iconPlaceholder = false,
                                renderBackgroundBlur = false,
                                title = stringResource(id = R.string.module_shortcut_delete),
                                onClick = {
                                    val moduleId = shortcutModuleId
                                    val type = selectedShortcutType
                                    if (!moduleId.isNullOrBlank() && type != null) {
                                        deleteModuleShortcut(context, moduleId, type)
                                    }
                                    showShortcutDialog.value = false
                                },
                            )
                        }
                    }

                    item {
                        val settings = koinInject<SettingsPlatformRepository>()
                        val moduleShortcutSchemeCopied = stringResource(R.string.module_shortcut_scheme_copied)

                        SettingsJumpPageWidget(
                            iconPlaceholder = false,
                            renderBackgroundBlur = false,
                            title = stringResource(R.string.module_shortcut_copy_scheme),
                            onClick = {
                                val currentModuleId = shortcutModuleId
                                val type = selectedShortcutType
                                if (currentModuleId.isNullOrBlank() || type == null) {
                                    return@SettingsJumpPageWidget
                                }
                                val url = shortcut.buildShortcutUri(currentModuleId, type, settings.intentToken).toString()
                                val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("KernelSU deep link", url))
                                Toast.makeText(context, moduleShortcutSchemeCopied, Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { showShortcutDialog.value = false },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                    ) {
                        Text(
                            text = stringResource(id = android.R.string.cancel),
                        )
                    }

                    Button(
                        onClick = {
                            val moduleId = shortcutModuleId
                            val type = selectedShortcutType
                            if (!moduleId.isNullOrBlank() && textFieldState.text.isNotBlank() && type != null) {
                                createModuleShortcut(
                                    context = context,
                                    moduleId = moduleId,
                                    name = textFieldState.text.toString(),
                                    iconUri = shortcutIconUri,
                                    type = type,
                                    intentToken = settings.intentToken,
                                )
                            }
                            showShortcutDialog.value = false
                        },
                        enabled = error.isBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp),
                    ) {
                        Text(
                            text = if (hasExistingShortcut) {
                                stringResource(id = R.string.module_update)
                            } else {
                                stringResource(id = android.R.string.ok)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 模块列表骨架：与真实模块卡同一套内边距，右侧留出开关的位置。 */
@Composable
private fun ModuleSkeletonList(count: Int = 4) {
    val phase = rememberSkeletonPhase()
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(count) {
            KuaiMaoCard(
                modifier = Modifier.padding(bottom = 10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonLine(widthFraction = 0.48f, height = 15.dp, phase = phase)
                        Spacer(modifier = Modifier.height(7.dp))
                        SkeletonLine(widthFraction = 0.26f, height = 10.dp, phase = phase)
                        Spacer(modifier = Modifier.height(5.dp))
                        SkeletonLine(widthFraction = 0.22f, height = 10.dp, phase = phase)
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonLine(widthFraction = 0.92f, height = 10.dp, phase = phase)
                        Spacer(modifier = Modifier.height(5.dp))
                        SkeletonLine(widthFraction = 0.64f, height = 10.dp, phase = phase)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    // 开关位：52×26 的胶囊
                    SkeletonBone(
                        modifier = Modifier
                            .width(52.dp)
                            .height(26.dp),
                        shape = RoundedCornerShape(100.dp),
                        phase = phase,
                    )
                }
            }
        }
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun ModuleItem(
    viewModel: ModuleViewModel,
    module: InstalledModule,
    moduleSizes: Map<String, String>,
    updateUrl: String,
    onUninstallClicked: (InstalledModule) -> Unit,
    onCheckChanged: suspend (Boolean) -> Boolean,
    onUpdate: (InstalledModule) -> Unit,
    onClick: (InstalledModule) -> Unit,
    onModuleAddShortcut: (InstalledModule, ShortcutType) -> Unit,
    showMoreModuleInfo: Boolean,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isEnabled by remember(module.dirId) { mutableStateOf(module.enabled) }
    var isChangingEnabled by remember(module.dirId) { mutableStateOf(false) }
    val removed = module.remove

    LaunchedEffect(module.enabled) {
        isEnabled = module.enabled
    }

    LaunchedEffect(module.dirId) {
        viewModel.dispatch(ModuleUiAction.LoadSize(module.dirId))
    }

    val sizeStr = moduleSizes[module.dirId]
    val textDecoration = if (removed) TextDecoration.LineThrough else null
    val titleColor = if (removed) KuaiMaoColors.textMuted else KuaiMaoColors.textBody
    val secondaryColor = KuaiMaoColors.textMuted

    KuaiMaoCard(
        modifier = Modifier.padding(bottom = 10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (module.hasWebUi) {
                        Modifier.clickable { onClick(module) }
                    } else {
                        Modifier
                    },
                ),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = module.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 19.sp,
                    letterSpacing = (-0.2).sp,
                    color = titleColor,
                    textDecoration = textDecoration,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "版本: ${module.version}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondaryColor,
                    textDecoration = textDecoration,
                    maxLines = 1,
                )
                Text(
                    text = "作者: ${module.author}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondaryColor,
                    textDecoration = textDecoration,
                    maxLines = 1,
                )
                if (showMoreModuleInfo && module.updateJson.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "更新 JSON: ${module.updateJson}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = KuaiMaoColors.brand,
                        textDecoration = textDecoration,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { },
                                onLongClick = {
                                    val clipData = ClipData.newPlainText("Update JSON URL", module.updateJson)
                                    clipboardManager.setPrimaryClip(clipData)
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.module_update_json_copied),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            KuaiMaoSwitch(
                checked = isEnabled,
                enabled = !module.update && !isChangingEnabled && !removed,
                onCheckedChange = { enabled ->
                    scope.launch {
                        isChangingEnabled = true
                        try {
                            if (onCheckChanged(enabled)) {
                                isEnabled = enabled
                            }
                        } finally {
                            isChangingEnabled = false
                        }
                    }
                },
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        if (module.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = module.description,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp,
                color = if (removed) KuaiMaoColors.textMuted else KuaiMaoColors.textSec,
                textDecoration = textDecoration,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ModuleTag(text = module.dirId, container = KuaiMaoColors.brand, content = onAccentContent())
            if (module.metamodule) {
                ModuleTag(text = "META", container = KuaiMaoColors.amberSoft, content = KuaiMaoColors.amber)
            }
            ModuleTag(text = sizeStr ?: "0 KB", container = KuaiMaoColors.canvasSubtle, content = KuaiMaoColors.textSec)
        }

        Spacer(modifier = Modifier.height(12.dp))
        KuaiMaoRowDivider()
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (module.hasActionScript) {
                ModuleIconAction(
                    icon = Icons.TwoTone.PlayArrow,
                    enabled = !removed && isEnabled,
                    onClick = {
                        navigator.push(Route.ExecuteModuleAction(module.dirId, false))
                        viewModel.dispatch(ModuleUiAction.MarkNeedRefresh)
                    },
                    onLongClick = { onModuleAddShortcut(module, ShortcutType.Action) },
                )
            }
            if (module.hasWebUi) {
                ModuleIconAction(
                    icon = Icons.AutoMirrored.TwoTone.Wysiwyg,
                    enabled = !removed && isEnabled,
                    onClick = { onClick(module) },
                    onLongClick = { onModuleAddShortcut(module, ShortcutType.WebUI) },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (updateUrl.isNotEmpty()) {
                ModuleIconAction(
                    icon = Icons.TwoTone.Download,
                    enabled = !removed,
                    onClick = { onUpdate(module) },
                )
            }

            ModuleIconAction(
                icon = if (removed) Icons.TwoTone.Refresh else Icons.TwoTone.Delete,
                enabled = true,
                rotate = removed,
                onClick = { onUninstallClicked(module) },
            )
        }
    }
}

/** 模块卡里的标签小胶囊。 */
@Composable
private fun ModuleTag(
    text: String,
    container: Color,
    content: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 模块卡底部的小圆角操作按钮。 */
@Composable
private fun ModuleIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    rotate: Boolean = false,
) {
    val alpha = if (enabled) 1f else 0.38f
    val tone = KuaiMaoTones.unified
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(tone.container.copy(alpha = alpha * tone.container.alpha))
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tone.content.copy(alpha = alpha),
            modifier = Modifier
                .size(17.dp)
                .then(if (rotate) Modifier.rotate(180f) else Modifier),
        )
    }
}

@Preview
@Composable
private fun ModuleItemPreview() {
    val module = InstalledModule(
        id = "id",
        name = "name",
        version = "version",
        versionCode = 1,
        author = "author",
        description = "I am a test module and i do nothing but show a very long description",
        enabled = true,
        update = true,
        remove = false,
        updateJson = "",
        hasWebUi = true,
        hasActionScript = true,
        metamodule = true,
        actionIconPath = null,
        webUiIconPath = null,
        dirId = "dirId",
        moduleUpdate = null,
    )
    ModuleItem(
        koinViewModel<ModuleViewModel>(),
        module,
        emptyMap(),
        "",
        {},
        { true },
        {},
        {},
        { _, _ -> },
        false,
    )
}
