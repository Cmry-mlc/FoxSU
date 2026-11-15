package com.loomsu.manager.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.KeyboardArrowLeft
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.loomsu.manager.R
import com.loomsu.manager.domain.model.DynamicManagerApp
import com.loomsu.manager.domain.model.DynamicManagerConfig
import com.loomsu.manager.ui.component.ConfirmResult
import com.loomsu.manager.ui.component.DialogHandle
import com.loomsu.manager.ui.component.LoomCard
import com.loomsu.manager.ui.component.LoomCheckMark
import com.loomsu.manager.ui.component.LoomRow
import com.loomsu.manager.ui.component.LoomRowDivider
import com.loomsu.manager.ui.component.PackageIcon
import com.loomsu.manager.ui.component.SwipeableSnackbarHost
import com.loomsu.manager.ui.component.rememberConfirmDialog
import com.loomsu.manager.ui.component.rememberCustomDialog
import com.loomsu.manager.ui.component.settings.SegmentedColumn
import com.loomsu.manager.ui.component.settings.SettingsTextFieldWidget
import com.loomsu.manager.ui.navigation.LocalNavigator
import com.loomsu.manager.ui.theme.LoomColors
import com.loomsu.manager.ui.theme.LoomTones
import com.loomsu.manager.ui.theme.LoomType
import com.loomsu.manager.ui.theme.backdropInk
import com.loomsu.manager.ui.theme.blurSource
import com.loomsu.manager.ui.theme.renderBackgroundBlur
import com.loomsu.manager.ui.util.ActivityResumeEffect
import com.loomsu.manager.ui.util.LocalSnackbarHost
import com.loomsu.manager.ui.util.adaptiveScaffoldWindowInsets
import com.loomsu.manager.ui.util.showReplacingSnackbar
import com.loomsu.manager.ui.viewmodel.DynamicManagerAppItem
import com.loomsu.manager.ui.viewmodel.DynamicManagerOperation
import com.loomsu.manager.ui.viewmodel.DynamicManagerUiAction
import com.loomsu.manager.ui.viewmodel.DynamicManagerUiEvent
import com.loomsu.manager.ui.viewmodel.DynamicManagerViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * 动态管理器配置。
 *
 * 版式与 AppProfile 等二级页保持一致：圆角返回钮 + 24sp 大标题 + 白色圆角卡片，
 * 不再使用 Material 的 SearchAppBar / SegmentedColumn —— 那种「一整块列表 + 分组灰字」
 * 的观感和现在的卡片语言完全是两套东西。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DynamicManagerScreen() {
    val navigator = LocalNavigator.current
    val viewModel = koinViewModel<DynamicManagerViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    var isUserRefreshing by remember { mutableStateOf(false) }
    val confirmDialog = rememberConfirmDialog()

    val grantConfirmTitle = stringResource(R.string.dynamic_manager_grant_confirm_title)
    val grantConfirmMessage = stringResource(R.string.dynamic_manager_grant_confirm_message)
    val clearConfirmTitle = stringResource(R.string.dynamic_manager_clear_confirm_title)
    val clearConfirmMessage = stringResource(R.string.dynamic_manager_clear_confirm_message)
    val setSuccess = stringResource(R.string.dynamic_manager_set_success)
    val setFailed = stringResource(R.string.dynamic_manager_set_failed)
    val clearSuccess = stringResource(R.string.dynamic_manager_disabled_success)
    val clearFailed = stringResource(R.string.dynamic_manager_clear_failed)
    val confirmText = stringResource(R.string.confirm)

    suspend fun confirmPrivilegeGrant(): Boolean {
        val first = confirmDialog.awaitConfirm(
            title = grantConfirmTitle,
            content = grantConfirmMessage,
            confirm = confirmText,
        )
        return first == ConfirmResult.Confirmed
    }

    fun runGrantOperation(action: DynamicManagerUiAction) {
        scope.launch {
            if (!confirmPrivilegeGrant()) return@launch
            viewModel.dispatch(action)
        }
    }

    fun runClearOperation() {
        scope.launch {
            val confirmed = confirmDialog.awaitConfirm(
                title = clearConfirmTitle,
                content = clearConfirmMessage,
                confirm = confirmText,
            )
            if (confirmed != ConfirmResult.Confirmed) return@launch
            viewModel.dispatch(DynamicManagerUiAction.Clear)
        }
    }

    val manualDialog = rememberDynamicManagerManualDialog { size, hash ->
        runGrantOperation(DynamicManagerUiAction.SetManual(size, hash))
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is DynamicManagerUiEvent.OperationCompleted -> {
                    val message = when (event.operation) {
                        DynamicManagerOperation.Set -> if (event.success) setSuccess else setFailed
                        DynamicManagerOperation.Clear -> if (event.success) clearSuccess else clearFailed
                    }
                    snackbarHost.showReplacingSnackbar(message)
                }
            }
        }
    }

    ActivityResumeEffect {
        viewModel.dispatch(DynamicManagerUiAction.Refresh)
    }

    Scaffold(
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        snackbarHost = { SwipeableSnackbarHost(hostState = snackbarHost) },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }
        } else {
            PullToRefreshBox(
                state = pullToRefreshState,
                isRefreshing = isUserRefreshing,
                onRefresh = {
                    scope.launch {
                        isUserRefreshing = true
                        try {
                            viewModel.refresh()
                        } finally {
                            isUserRefreshing = false
                        }
                    }
                },
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullToRefreshState,
                        isRefreshing = isUserRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = paddingValues.calculateTopPadding()),
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .blurSource(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding() + 10.dp,
                        bottom = paddingValues.calculateBottomPadding() + 32.dp,
                    ),
                ) {
                    item(key = "header") {
                        DynamicManagerHeader(onBack = { navigator.pop() })
                    }

                    item(key = "search") {
                        Spacer(modifier = Modifier.height(16.dp))
                        DynamicManagerSearchField(
                            query = uiState.search,
                            onQueryChange = { viewModel.dispatch(DynamicManagerUiAction.Search(it)) },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }

                    item(key = "status") {
                        Spacer(modifier = Modifier.height(14.dp))
                        DynamicManagerStatusCard(
                            config = uiState.config,
                            enabled = !uiState.isSubmitting,
                            onManualConfig = { manualDialog.show() },
                            onClearConfig = { runClearOperation() },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }

                    item(key = "managers-title") {
                        Text(
                            text = stringResource(R.string.manage_managers),
                            style = LoomType.sectionTitle,
                            modifier = Modifier.padding(
                                start = 22.dp,
                                end = 20.dp,
                                top = 26.dp,
                                bottom = 10.dp,
                            ),
                        )
                    }

                    item(key = "managers") {
                        DynamicManagerAppList(
                            apps = uiState.apps,
                            searching = uiState.search.isNotBlank(),
                            searchEmpty = uiState.apps.isEmpty(),
                            onSelect = { app ->
                                if (app.isSelected) {
                                    runClearOperation()
                                } else {
                                    runGrantOperation(DynamicManagerUiAction.SelectApp(app))
                                }
                            },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 顶部：圆形返回钮 + 页面大标题（二级页统一版式）。 */
@Composable
private fun DynamicManagerHeader(onBack: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(
                    6.dp,
                    CircleShape,
                    clip = false,
                    ambientColor = Color(0x1418263C),
                    spotColor = Color(0x1418263C),
                )
                .clip(CircleShape)
                .background(LoomColors.canvasCard)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.TwoTone.KeyboardArrowLeft,
                contentDescription = "返回",
                tint = LoomColors.textBody,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.dynamic_manager_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
            color = backdropInk(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 搜索框：14dp 圆角、浅底、无描边，和卡片同一套磨砂规则。
 *
 * 不用 Material 的 SearchBar，是因为它自带一条强调色下划线和展开动画，
 * 放在这张卡片语言的白底页面里会显得突兀。
 */
@Composable
private fun DynamicManagerSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LoomColors.canvasField)
            .renderBackgroundBlur(tintColor = LoomColors.canvasFieldSolid)
            .padding(start = 13.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.TwoTone.Search,
            contentDescription = null,
            tint = LoomColors.textMuted,
            modifier = Modifier.size(17.dp),
        )

        Spacer(modifier = Modifier.width(9.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_apps),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = LoomColors.textMuted,
                )
            }

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = LoomColors.textBody,
                ),
                cursorBrush = SolidColor(LoomColors.brand),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Close,
                    contentDescription = null,
                    tint = LoomColors.textMuted,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

/** 状态 + 操作卡：当前配置、签名信息、手动配置与清除。 */
@Composable
private fun DynamicManagerStatusCard(
    config: DynamicManagerConfig?,
    enabled: Boolean,
    onManualConfig: () -> Unit,
    onClearConfig: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = config?.isValid == true
    val tone = LoomTones.unified

    LoomCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tone.container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Security,
                    contentDescription = null,
                    tint = tone.content,
                    modifier = Modifier.size(21.dp),
                )
            }

            Spacer(modifier = Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dynamic_manager_current_status),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LoomColors.textBody,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (active && config != null) {
                        stringResource(R.string.dynamic_manager_signature_size_value, config.size)
                    } else {
                        stringResource(R.string.dynamic_manager_disabled)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = LoomColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (active) {
                Spacer(modifier = Modifier.width(10.dp))
                LoomStatePill(
                    text = stringResource(R.string.dynamic_manager_active),
                    color = LoomColors.success,
                    container = LoomColors.successSoft,
                )
            }
        }

        if (active && config != null) {
            LoomRowDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 10.dp),
            ) {
                Text(
                    text = stringResource(R.string.signature_hash),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    color = LoomColors.textMuted,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = config.hash.chunked(32).joinToString("\n"),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = LoomColors.textSec,
                )
            }
        }

        LoomRowDivider()

        LoomRow(
            icon = Icons.TwoTone.Edit,
            title = stringResource(R.string.dynamic_manager_manual_config),
            description = stringResource(R.string.dynamic_manager_manual_config_summary),
            enabled = enabled,
            onClick = onManualConfig,
        )

        LoomRowDivider()

        LoomRow(
            icon = Icons.TwoTone.Delete,
            title = stringResource(R.string.dynamic_manager_clear_config),
            description = stringResource(R.string.dynamic_manager_clear_config_summary),
            iconTint = LoomColors.danger,
            iconContainer = LoomColors.dangerSoft,
            enabled = enabled && active,
            onClick = onClearConfig,
        )
    }
}

/** 状态胶囊：小圆点 + 短词，只用来标「已启用」这类瞬时状态。 */
@Composable
private fun LoomStatePill(
    text: String,
    color: Color,
    container: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(container)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            color = color,
            maxLines = 1,
        )
    }
}

/** 管理器列表：一张卡里排全部候选，选中态用勾选块而不是系统 Checkbox。 */
@Composable
private fun DynamicManagerAppList(
    apps: List<DynamicManagerAppItem>,
    searching: Boolean,
    searchEmpty: Boolean,
    onSelect: (DynamicManagerApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    LoomCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
        if (searchEmpty) {
            Text(
                text = stringResource(R.string.no_apps_found),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = LoomColors.textMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (searching) 26.dp else 18.dp),
            )
        } else {
            apps.forEachIndexed { index, app ->
                if (index > 0) LoomRowDivider()
                DynamicManagerAppRow(
                    app = app,
                    onClick = { onSelect(app) },
                )
            }
        }
    }
}

/** 单个管理器行：应用图标 + 名称 / 包名 + 勾选块。 */
@Composable
private fun DynamicManagerAppRow(
    app: DynamicManagerAppItem,
    onClick: () -> Unit,
) {
    val changeable = app.isChangeable
    val titleColor = if (changeable) LoomColors.textBody else LoomColors.textDisabled
    val descColor = if (changeable) LoomColors.textMuted else LoomColors.textDisabled

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = changeable, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PackageIcon(
            packageName = app.packageName,
            contentDescription = app.label,
            modifier = Modifier.size(42.dp),
        )

        Spacer(modifier = Modifier.width(13.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (changeable) {
                    app.packageName
                } else {
                    stringResource(
                        R.string.dynamic_manager_fixed_manager_summary,
                        app.packageName,
                        when (app.managerSignatureIndex ?: 0) {
                            254 -> "Debug"
                            253 -> "KernelSU Toolkit"
                            else -> "Kernel"
                        },
                    )
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 15.sp,
                color = descColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        LoomCheckMark(
            checked = app.isSelected || !changeable,
            enabled = changeable,
        )
    }
}

@Composable
private fun rememberDynamicManagerManualDialog(
    onConfirm: (Int, String) -> Unit,
): DialogHandle = rememberCustomDialog { dismiss ->
    DynamicManagerManualDialog(
        onDismiss = dismiss,
        onConfirm = { size, hash ->
            dismiss()
            onConfirm(size, hash)
        },
    )
}

@Composable
private fun DynamicManagerManualDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit,
) {
    val sizeState = rememberTextFieldState()
    val hashState = rememberTextFieldState()
    val size = sizeState.text.toString()
    val hash = hashState.text.toString()
    val sizeValue = size.toIntOrNull()
    val hashValid = hash.length == 64
    val isValid = sizeValue != null && sizeValue > 0 && hashValid

    LaunchedEffect(sizeState.text) {
        val filtered = sizeState.text.toString().filter(Char::isDigit)
        if (filtered != sizeState.text.toString()) {
            sizeState.edit {
                replace(0, length, filtered)
            }
        }
    }

    LaunchedEffect(hashState.text) {
        val trimmed = hashState.text.toString().trim()
        if (trimmed != hashState.text.toString()) {
            hashState.edit {
                replace(0, length, trimmed)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dynamic_manager_manual_config)) },
        text = {
            SegmentedColumn {
                item {
                    SettingsTextFieldWidget(
                        modifier = Modifier.fillMaxWidth(),
                        renderBackgroundBlur = false,
                        state = sizeState,
                        title = stringResource(R.string.signature_size),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        lineLimits = TextFieldLineLimits.SingleLine,
                    )
                }

                item {
                    SettingsTextFieldWidget(
                        modifier = Modifier.fillMaxWidth(),
                        renderBackgroundBlur = false,
                        state = hashState,
                        title = stringResource(R.string.signature_hash),
                        error = if (hash.isNotEmpty() && !hashValid) {
                            stringResource(R.string.hash_must_be_64_chars)
                        } else {
                            ""
                        },
                        lineLimits = TextFieldLineLimits.SingleLine,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onConfirm(sizeValue ?: return@TextButton, hash)
                },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
