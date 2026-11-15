package com.kuaimaosu.manager.ui.screen.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.twotone.AutoAwesome
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.GridView
import androidx.compose.material.icons.twotone.Handyman
import androidx.compose.material.icons.twotone.Palette
import androidx.compose.material.icons.twotone.Tune
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoPageTitle
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTone
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Article
import androidx.compose.material.icons.automirrored.twotone.Undo
import androidx.compose.material.icons.twotone.Adb
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.DeleteForever
import androidx.compose.material.icons.twotone.ElectricalServices
import androidx.compose.material.icons.twotone.Extension
import androidx.compose.material.icons.twotone.Fence
import androidx.compose.material.icons.twotone.FolderDelete
import androidx.compose.material.icons.twotone.FolderOff
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.MusicNote
import androidx.compose.material.icons.twotone.Policy
import androidx.compose.material.icons.twotone.RemoveCircle
import androidx.compose.material.icons.twotone.RemoveModerator
import androidx.compose.material.icons.twotone.RestartAlt
import androidx.compose.material.icons.twotone.Save
import androidx.compose.material.icons.twotone.Science
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Share
import androidx.compose.material.icons.twotone.Update
import androidx.compose.material.icons.twotone.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kuaimaosu.manager.BuildConfig
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.domain.usecase.GenerateBugreportUseCase
import com.kuaimaosu.manager.ui.component.ConfirmResult
import com.kuaimaosu.manager.ui.component.SwipeableSnackbarHost
import com.kuaimaosu.manager.ui.component.rememberConfirmDialog
import com.kuaimaosu.manager.ui.component.rememberLoadingDialog
import com.kuaimaosu.manager.ui.component.settings.SegmentedColumn
import com.kuaimaosu.manager.ui.component.settings.SettingsBaseWidget
import com.kuaimaosu.manager.ui.component.settings.SettingsChooseWidget
import com.kuaimaosu.manager.ui.component.settings.SettingsJumpPageWidget
import com.kuaimaosu.manager.ui.component.settings.SettingsSwitchWidget
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.navigation.Route
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.theme.blurEffect
import com.kuaimaosu.manager.ui.theme.blurSource
import com.kuaimaosu.manager.ui.util.LocalSnackbarHost
import com.kuaimaosu.manager.ui.util.adaptiveScaffoldWindowInsets
import com.kuaimaosu.manager.ui.util.showReplacingSnackbar
import com.kuaimaosu.manager.ui.viewmodel.HomeViewModel
import com.kuaimaosu.manager.ui.viewmodel.SettingsUiAction
import com.kuaimaosu.manager.ui.viewmodel.SettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * @author ShirkNeko
 * @date 2025/9/29.
 */

private val SPACING_MEDIUM = 8.dp
private val SPACING_LARGE = 16.dp

/** 设置首页的分类入口（对应设计稿 .lx-category）。 */
private data class KuaiMaoCategory(
    val title: String,
    val summary: String,
    /** 右侧数量胶囊；null 表示这类入口没有「几项」的概念，不画胶囊。 */
    val count: Int?,
    val icon: ImageVector,
    val tone: KuaiMaoTone,
    val route: Route,
)

/**
 * 设置首页：六个分类入口。
 *
 * 对照设计稿 pages/set：大标题 + 一条白色卡片内的分类行（图标块 / 标题 / 副标题 / 数量胶囊 / 箭头），
 * 底部一行版本注脚。数量按当前内核真实能力计算，不写死。
 */
@Composable
fun SettingsPage(bottomPadding: Dp) {
    val navigator = LocalNavigator.current
    val homeViewModel = koinViewModel<HomeViewModel>()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val status = homeState.systemStatus

    val categories = listOf(
        KuaiMaoCategory(
            title = "通用与 Root",
            summary = "su 命令 · ADB Root · 卸载与日志",
            count = 7 + if (status.isLateLoadMode) 1 else 0,
            icon = Icons.TwoTone.Tune,
            tone = KuaiMaoTones.violet,
            route = Route.SettingsRoot,
        ),
        KuaiMaoCategory(
            title = "App Profile",
            summary = "模版管理 · 默认卸载策略",
            count = 2,
            icon = Icons.TwoTone.GridView,
            tone = KuaiMaoTones.amber,
            route = Route.SettingsProfile,
        ),
        KuaiMaoCategory(
            title = "主题设置",
            summary = "配色 · 卡片 · 模糊与底栏样式",
            count = 5,
            icon = Icons.TwoTone.Palette,
            tone = KuaiMaoTones.green,
            route = Route.ThemeSettings,
        ),
        KuaiMaoCategory(
            title = "更新",
            summary = "管理器更新 · 测试版 · 模块更新",
            count = 3,
            icon = Icons.TwoTone.Update,
            tone = KuaiMaoTones.blue,
            route = Route.SettingsUpdate,
        ),
        KuaiMaoCategory(
            title = "音乐",
            summary = "在线搜索 · 后台播放 · 助手可代放",
            count = null,
            icon = Icons.TwoTone.MusicNote,
            tone = KuaiMaoTones.violet,
            route = Route.Music,
        ),
        KuaiMaoCategory(
            title = "AI 设置",
            summary = "模型服务 · 密钥 · 生成参数",
            count = 2,
            icon = Icons.TwoTone.AutoAwesome,
            tone = KuaiMaoTones.rose,
            route = Route.SettingsAi,
        ),
        KuaiMaoCategory(
            title = "工具",
            summary = "日志 · 动态管理器 · Umount 路径",
            count = 3 + if (status.lkmMode == true && !status.isLateLoadMode) 1 else 0,
            icon = Icons.TwoTone.Handyman,
            tone = KuaiMaoTones.slate,
            route = Route.SettingsTools,
        ),
        KuaiMaoCategory(
            title = "关于",
            summary = "版本 · 法律信息 · 开源许可",
            count = 4,
            icon = Icons.TwoTone.Info,
            tone = KuaiMaoTones.neutral,
            route = Route.SettingsAbout,
        ),
    )

    val footnote = buildString {
        append("KuaiMaoSU ")
        append(BuildConfig.VERSION_NAME)
        append(" · 内核 ")
        append(runCatching { status.kernelVersion.toString() }.getOrDefault("未知"))
        append(" · ")
        append(if (status.ksuVersion != null) "已激活" else "未激活")
    }

    Scaffold(
        contentWindowInsets = adaptiveScaffoldWindowInsets(includeBottom = false),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            // 标题固定在顶部，下面只有卡片列表滚动
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                KuaiMaoPageTitle(
                    title = "设置",
                    subtitle = "KuaiMaoSU · V${BuildConfig.VERSION_NAME}",
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .blurSource()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
            KuaiMaoCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
                categories.forEachIndexed { index, category ->
                    if (index > 0) KuaiMaoRowDivider()
                    KuaiMaoCategoryRow(category = category) {
                        navigator.push(category.route)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = footnote,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = KuaiMaoColors.textMuted,
                modifier = Modifier.padding(start = 2.dp),
            )
            Spacer(modifier = Modifier.height(bottomPadding + 28.dp))
            }
        }
    }
}

@Composable
private fun KuaiMaoCategoryRow(
    category: KuaiMaoCategory,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(category.tone.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = category.tone.content,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                color = KuaiMaoColors.textBody,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = category.summary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = KuaiMaoColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        if (category.count != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(KuaiMaoColors.canvasSubtle)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = category.count.toString(),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    color = KuaiMaoColors.textTer,
                )
            }
        }
        Spacer(modifier = Modifier.width(7.dp))
        Icon(
            imageVector = Icons.TwoTone.ChevronRight,
            contentDescription = null,
            tint = KuaiMaoColors.chevron,
            modifier = Modifier.size(16.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogBottomSheet(
    onDismiss: () -> Unit,
    onSaveLog: () -> Unit,
    onShareLog: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_LARGE),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LogActionButton(
                icon = Icons.TwoTone.Save,
                text = stringResource(R.string.save_log),
                onClick = onSaveLog,
            )

            LogActionButton(
                icon = Icons.TwoTone.Share,
                text = stringResource(R.string.send_log),
                onClick = onShareLog,
            )
        }
        Spacer(modifier = Modifier.height(SPACING_LARGE))
    }
}

@Composable
fun LogActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(SPACING_MEDIUM),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(SPACING_MEDIUM))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun UninstallItem(
    withLoading: suspend (suspend () -> Unit) -> Unit,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uninstallConfirmDialog = rememberConfirmDialog()
    val showTodo = {
        Toast.makeText(context, "TODO", Toast.LENGTH_SHORT).show()
    }
    val options = remember {
        listOf(
            UninstallType.PERMANENT,
            UninstallType.RESTORE_STOCK_IMAGE,
        )
    }

    SettingsChooseWidget(
        icon = Icons.TwoTone.Delete,
        title = stringResource(id = R.string.settings_uninstall),
        items = options.map { stringResource(it.title) },
        itemDescriptions = options.map {
            if (it.message != 0) stringResource(it.message) else null
        },
        selectedIndex = -1,
        onSelectedIndexChange = { index ->
            options.getOrNull(index)?.let { uninstallType ->
                scope.launch {
                    val result = uninstallConfirmDialog.awaitConfirm(
                        title = context.getString(uninstallType.title),
                        content = context.getString(uninstallType.message),
                    )
                    if (result == ConfirmResult.Confirmed) {
                        withLoading {
                            when (uninstallType) {
                                UninstallType.TEMPORARY -> showTodo()
                                UninstallType.PERMANENT -> navigator.push(Route.Flash.uninstall())
                                UninstallType.RESTORE_STOCK_IMAGE -> navigator.push(Route.Flash.restore())
                                UninstallType.NONE -> Unit
                            }
                        }
                    }
                }
            }
        },
    )
}

enum class UninstallType(@param:StringRes val title: Int, @param:StringRes val message: Int, val icon: ImageVector) {
    TEMPORARY(
        R.string.settings_uninstall_temporary,
        R.string.settings_uninstall_temporary_message,
        Icons.TwoTone.Delete,
    ),
    PERMANENT(
        R.string.settings_uninstall_permanent,
        R.string.settings_uninstall_permanent_message,
        Icons.TwoTone.DeleteForever,
    ),
    RESTORE_STOCK_IMAGE(
        R.string.settings_restore_stock_image,
        R.string.settings_restore_stock_image_message,
        Icons.AutoMirrored.TwoTone.Undo,
    ),
    NONE(0, 0, Icons.TwoTone.Delete),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    LargeFlexibleTopAppBar(
        modifier = Modifier.blurEffect(),
        title = {
            Text(text = stringResource(R.string.settings))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor =
                if (themeConfig.isEnableBlur) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.surfaceContainer.copy(cardConfig.cardAlpha)
                },
            scrolledContainerColor =
                if (themeConfig.isEnableBlur) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.surfaceContainer.copy(cardConfig.cardAlpha)
                },
        ),
        windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
        scrollBehavior = scrollBehavior,
    )
}
