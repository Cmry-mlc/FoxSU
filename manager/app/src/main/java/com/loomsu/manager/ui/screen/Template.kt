package com.loomsu.manager.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.KeyboardArrowLeft
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.ImportExport
import androidx.compose.material.icons.twotone.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import com.loomsu.manager.R
import com.loomsu.manager.domain.model.ProfileTemplate
import com.loomsu.manager.ui.component.LoomCard
import com.loomsu.manager.ui.component.LoomMenuItem
import com.loomsu.manager.ui.component.LoomMenuPanel
import com.loomsu.manager.ui.component.LoomPageTitle
import com.loomsu.manager.ui.component.LoomSquareIconButton
import com.loomsu.manager.ui.component.NetworkRefreshContent
import com.loomsu.manager.ui.navigation.LocalNavigator
import com.loomsu.manager.ui.navigation.Navigator
import com.loomsu.manager.ui.navigation.Route
import com.loomsu.manager.ui.theme.LoomColors
import com.loomsu.manager.ui.theme.LoomType
import com.loomsu.manager.ui.theme.blurSource
import com.loomsu.manager.ui.util.ActivityResumeEffect
import com.loomsu.manager.ui.util.adaptiveScaffoldWindowInsets
import com.loomsu.manager.ui.viewmodel.TemplateUiAction
import com.loomsu.manager.ui.viewmodel.TemplateUiEvent
import com.loomsu.manager.ui.viewmodel.TemplateViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * App Profile 模版列表（设计稿 pages/profile 的「模版」二级页）。
 *
 * 结构：圆形返回 + 大标题 + 右上同步/导入导出 → 模版白卡列表 → 右下「创建模版」胶囊。
 * 同步、导入、导出、创建、编辑等动作全部保留原有实现。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppProfileTemplateScreen() {
    val pullRefreshState = rememberPullToRefreshState()
    val viewModel = koinViewModel<TemplateViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var isUserRefreshing by remember { mutableStateOf(false) }
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val clipboardManager = context.getSystemService<ClipboardManager>()
    val appProfileTemplateImportEmpty = stringResource(R.string.app_profile_template_import_empty)
    val appProfileTemplateImportSuccess = stringResource(R.string.app_profile_template_import_success)
    val appProfileTemplateExportEmpty = stringResource(R.string.app_profile_template_export_empty)

    LaunchedEffect(viewModel, clipboardManager) {
        viewModel.events.collect { event ->
            when (event) {
                TemplateUiEvent.ImportCompleted -> {
                    Toast.makeText(context, appProfileTemplateImportSuccess, Toast.LENGTH_SHORT).show()
                    viewModel.dispatch(TemplateUiAction.Refresh())
                }

                is TemplateUiEvent.Exported -> {
                    clipboardManager?.setPrimaryClip(ClipData.newPlainText("", event.json))
                }

                TemplateUiEvent.ExportEmpty -> {
                    Toast.makeText(context, appProfileTemplateExportEmpty, Toast.LENGTH_SHORT).show()
                }

                is TemplateUiEvent.Error -> if (event.message.isNotBlank()) {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        navigator.observeResult<Boolean>("template_edit").collect { success ->
            if (success) {
                navigator.clearResult("template_edit")
                scope.launch { viewModel.dispatch(TemplateUiAction.Refresh()) }
            }
        }
    }

    ActivityResumeEffect {
        viewModel.dispatch(TemplateUiAction.Refresh())
    }

    var showImportMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blurSource(),
        ) {
            PullToRefreshBox(
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize(),
                isRefreshing = isUserRefreshing,
                onRefresh = {
                    scope.launch {
                        isUserRefreshing = true
                        try {
                            viewModel.fetchTemplates()
                        } finally {
                            isUserRefreshing = false
                        }
                    }
                },
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullRefreshState,
                        isRefreshing = isUserRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = innerPadding.calculateTopPadding()),
                    )
                },
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = innerPadding.calculateBottomPadding() + 108.dp,
                    ),
                ) {
                    item {
                        Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding() + 6.dp))
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
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
                                    .clickable(onClick = dropUnlessResumed { navigator.pop() }),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.TwoTone.KeyboardArrowLeft,
                                    contentDescription = "返回",
                                    tint = LoomColors.textBody,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            LoomSquareIconButton(
                                icon = Icons.TwoTone.Sync,
                                contentDescription = stringResource(R.string.app_profile_template_sync),
                                onClick = { viewModel.dispatch(TemplateUiAction.Refresh(synchronize = true)) },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box {
                                LoomSquareIconButton(
                                    icon = Icons.TwoTone.ImportExport,
                                    contentDescription = stringResource(R.string.app_profile_import_export),
                                    onClick = { showImportMenu = true },
                                )
                                DropdownMenu(
                                    expanded = showImportMenu,
                                    onDismissRequest = { showImportMenu = false },
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    offset = DpOffset(x = 0.dp, y = 6.dp),
                                ) {
                                    LoomMenuPanel {
                                        LoomMenuItem(
                                            text = stringResource(R.string.app_profile_import_from_clipboard),
                                            onClick = {
                                                showImportMenu = false
                                                val clipboardText =
                                                    clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString()
                                                if (clipboardText.isNullOrEmpty()) {
                                                    Toast.makeText(
                                                        context,
                                                        appProfileTemplateImportEmpty,
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                } else {
                                                    viewModel.dispatch(TemplateUiAction.Import(clipboardText))
                                                }
                                            },
                                        )
                                        LoomMenuItem(
                                            text = stringResource(R.string.app_profile_export_to_clipboard),
                                            onClick = {
                                                showImportMenu = false
                                                viewModel.dispatch(TemplateUiAction.Export)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        LoomPageTitle(
                            title = stringResource(R.string.settings_profile_template),
                            subtitle = if (uiState.templateList.isEmpty()) {
                                "按应用定制 UID / GID / 安全上下文"
                            } else {
                                "${uiState.templateList.size} 个模版 · 支持本地与远程"
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.templateList.isEmpty()) {
                        item {
                            NetworkRefreshContent(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp),
                                offline = uiState.isOffline,
                                onRetry = {
                                    scope.launch {
                                        viewModel.dispatch(TemplateUiAction.Refresh(synchronize = true))
                                    }
                                },
                            )
                        }
                    } else {
                        items(
                            items = uiState.templateList,
                            key = { it.id },
                        ) { template ->
                            TemplateItem(template)
                        }
                    }
                }
            }

            // 创建模版：右下角的浅紫胶囊
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 20.dp,
                        bottom = innerPadding.calculateBottomPadding() + 26.dp,
                    )
                    .shadow(
                        14.dp,
                        RoundedCornerShape(100.dp),
                        clip = false,
                        ambientColor = Color(0x2E3B3F8C),
                        spotColor = Color(0x2E3B3F8C),
                    )
                    .clip(RoundedCornerShape(100.dp))
                    .background(LoomColors.brandSoft)
                    .clickable {
                        navigator.navigateForResult(
                            Route.TemplateEditor(
                                templateId = "",
                                readOnly = false,
                                isCreation = true,
                            ),
                            "template_edit",
                        )
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Add,
                    contentDescription = null,
                    tint = LoomColors.brandDeep,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = stringResource(R.string.app_profile_template_create),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    color = LoomColors.brandDeep,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateItem(
    template: ProfileTemplate,
) {
    val navigator = LocalNavigator.current

    LoomCard(
        modifier = Modifier.padding(bottom = 10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navigator.navigateForResult(
                        Route.TemplateEditor(template.id, !template.local),
                        "template_edit",
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.1).sp,
                    color = LoomColors.textBody,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${template.id}${if (template.author.isEmpty()) "" else "@${template.author}"}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = LoomColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (template.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = template.description,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp,
                        color = LoomColors.textSec,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LabelText("UID: ${template.uid}", containerColor = LoomColors.ink, contentColor = Color.White)
                    LabelText("GID: ${template.gid}", containerColor = LoomColors.canvasSubtle, contentColor = LoomColors.textLabel)
                    LabelText(template.context, containerColor = LoomColors.brandSoft, contentColor = LoomColors.brandDeep)
                    LabelText(
                        label = stringResource(if (template.local) R.string.local else R.string.remote),
                        containerColor = LoomColors.canvasSubtle,
                        contentColor = LoomColors.textSec,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.TwoTone.ChevronRight,
                contentDescription = null,
                tint = LoomColors.chevron,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Preview
@Composable
private fun TemplateItemPreview() {
    CompositionLocalProvider(
        LocalNavigator provides Navigator(Route.AppProfileTemplate),
    ) {
        TemplateItem(ProfileTemplate())
    }
}

@Composable
fun LabelText(
    label: String,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = contentColorFor(containerColor),
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
