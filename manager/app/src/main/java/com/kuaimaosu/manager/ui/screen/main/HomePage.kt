package com.kuaimaosu.manager.ui.screen.main

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.system.Os
import android.widget.Toast
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import com.kuaimaosu.manager.ui.theme.renderBackgroundBlur
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.MenuBook
import androidx.compose.material.icons.twotone.Android
import androidx.compose.material.icons.twotone.AutoAwesome
import androidx.compose.material.icons.twotone.Bolt
import androidx.compose.material.icons.twotone.Coffee
import androidx.compose.material.icons.twotone.DeveloperBoard
import androidx.compose.material.icons.twotone.Error
import androidx.compose.material.icons.twotone.Extension
import androidx.compose.material.icons.twotone.FilterList
import androidx.compose.material.icons.twotone.Group
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Layers
import androidx.compose.material.icons.twotone.Memory
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Smartphone
import androidx.compose.material.icons.twotone.Tag
import androidx.compose.material.icons.twotone.Tune
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kuaimaosu.manager.BuildConfig
import com.kuaimaosu.manager.Natives.KernelPatchImplementation
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.domain.model.HomeSystemInfo
import com.kuaimaosu.manager.domain.model.KernelStatus
import com.kuaimaosu.manager.domain.model.ManagerUpdateChannel
import com.kuaimaosu.manager.domain.model.ManagerUpdateInfo
import com.kuaimaosu.manager.domain.usecase.EnqueueManagerUpdateUseCase
import com.kuaimaosu.manager.ui.component.SwipeableSnackbarHost
import com.kuaimaosu.manager.ui.component.RevealItem
import com.kuaimaosu.manager.ui.component.SkeletonBone
import com.kuaimaosu.manager.ui.component.SkeletonLine
import com.kuaimaosu.manager.ui.component.SkeletonSwap
import com.kuaimaosu.manager.ui.component.rememberSkeletonPhase
import com.kuaimaosu.manager.ui.component.rememberSkeletonVisible
import com.kuaimaosu.manager.ui.component.WarningCard
import com.kuaimaosu.manager.ui.component.rememberConfirmDialog
import com.kuaimaosu.manager.ui.component.rememberLoadingDialog
import com.kuaimaosu.manager.ui.component.settings.AppBackButton
import com.kuaimaosu.manager.ui.component.settings.SegmentedColumn
import com.kuaimaosu.manager.ui.component.settings.SettingsBaseWidget
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.data.notify.KuaiMaoNotificationStore
import com.kuaimaosu.manager.ui.navigation.Route
import com.kuaimaosu.manager.ui.screen.BottomBarDestination
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.backdropInk
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.theme.barSurfaceColor
import com.kuaimaosu.manager.ui.theme.blurEffect
import com.kuaimaosu.manager.ui.theme.blurSource
import com.kuaimaosu.manager.ui.util.LocalHandlePageChange
import com.kuaimaosu.manager.ui.util.LocalPermissionRequestInterface
import com.kuaimaosu.manager.ui.util.LocalSnackbarHost
import com.kuaimaosu.manager.ui.util.adaptiveScaffoldWindowInsets
import com.kuaimaosu.manager.ui.util.downloader.downloadManagerUpdate
import com.kuaimaosu.manager.ui.viewmodel.HomeUiAction
import com.kuaimaosu.manager.ui.viewmodel.HomeUiEvent
import com.kuaimaosu.manager.ui.viewmodel.HomeUiState
import com.kuaimaosu.manager.ui.viewmodel.HomeViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * KuaiMaoSU 主页。
 *
 * 结构对照设计稿 pages/home：
 * 问候行 → 内核状态大卡 → 四宫格 → 系统动态。
 * 所有数值来自 HomeViewModel 的真实状态，不使用占位数据。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomePage(
    bottomPadding: Dp,
) {
    val context = LocalContext.current
    val viewModel = koinViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.dispatch(HomeUiAction.AwaitInitialData)
    }

    LaunchedEffect(Unit) {
        KuaiMaoNotificationStore.load(context)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeUiEvent.Error -> if (event.message.isNotBlank()) {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ReportDrawnWhen { uiState.isInitialDataLoaded }

    val navigator = LocalNavigator.current
    val handlePageChange = LocalHandlePageChange.current
    val loadingDialog = rememberLoadingDialog()
    val scope = rememberCoroutineScope()
    val pages = BottomBarDestination.getPages(uiState.systemStatus.isFullFeatured)
    val onInstall: () -> Unit = {
        navigator.push(Route.Install(preselectedKernelUri = null))
    }

    // 内核状态读得很快，骨架一闪而过反而廉价：给它一个最短驻留时间。
    val skeletonVisible = rememberSkeletonVisible(uiState.isCoreDataLoaded)

    var showAssistant by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val appear by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "homeAppear",
    )

    // 顶部问候行（头像 / 标题 / 铃铛角标）放到 Scaffold 的 topBar 里，
    // 跟随页面固定，不再随下面的内容滚动而移动。
    val notices = kuaimaoNotices(
        uiState = uiState,
        onOpenUpdate = { navigator.push(Route.SettingsUpdate) },
        onOpenInstall = onInstall,
    )
    val unreadCount = notices.count { it.id !in KuaiMaoNotificationStore.readIds }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 顶栏不再铺任何膜：自定义壁纸下那一整条色块会把壁纸割成上下两层，
                    // 用户要的是「跟下面一样、不要有层次」。所有模式统一透明，
                    // 文字颜色由 LocalDarkBackdrop 自己适配明暗壁纸。
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        alpha = appear
                        translationY = (1f - appear) * 26.dp.toPx()
                    },
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                KuaiMaoGreetingRow(
                    notificationCount = unreadCount,
                    onAvatarClick = { navigator.push(Route.About) },
                    onBellClick = { navigator.push(Route.Notifications) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = adaptiveScaffoldWindowInsets(includeBottom = false),
        snackbarHost = {
            SwipeableSnackbarHost(
                modifier = Modifier.padding(bottom = bottomPadding),
                hostState = LocalSnackbarHost.current,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blurSource()
                // 先让出 topBar 的高度再裁剪：否则列表往上一滚就会画到问候行上
                .padding(top = innerPadding.calculateTopPadding())
                .clipToBounds(),
            // 主页整页定死，不做上下滚动：内容一屏放得下，
            // 之前挂 verticalScroll 会让底栏的投影随内容上下浮出/消失。
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 内容比一屏高了：改成整页滚动。底栏是浮岛、不参与滚动，
                    // 滚动内容会从它下方穿过去，所以底部留 96dp 的让位空间。
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        alpha = appear
                        translationY = (1f - appear) * 26.dp.toPx()
                    },
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 骨架盖在真实内容上：内容始终参与测量，页面高度不变，
                // 切换时不会出现「先塌一下再撑开」的跳动。
                SkeletonSwap(
                    loading = skeletonVisible,
                    skeleton = { HomeSkeleton() },
                ) {
                    Column {
                        if (uiState.isCoreDataLoaded) {
                    val systemStatus = uiState.systemStatus
                    KuaiMaoHeroCard(
                        label = heroLabel(systemStatus),
                        stateText = heroStateText(uiState),
                        stateHint = heroHintText(uiState),
                        badgeText = heroBadgeText(uiState),
                        badgeColor = heroBadgeColor(uiState),
                        superuserCount = uiState.systemInfo.superuserCount,
                        moduleCount = uiState.systemInfo.moduleCount,
                        modeTags = heroModeTags(systemStatus, uiState.systemInfo.susfsLkmLoaded),
                        isActivated = systemStatus.ksuVersion != null,
                        // 四个快捷入口并进卡片底部，不再单独占一行卡片。
                        quickActions = listOf(
                            KuaiMaoQuickAction(
                                key = "su",
                                label = stringResource(R.string.superuser),
                                icon = Icons.TwoTone.Person,
                                tone = KuaiMaoTones.violet,
                                onClick = {
                                    val index = pages.indexOf(BottomBarDestination.SuperUser)
                                    if (index >= 0) handlePageChange(index)
                                },
                            ),
                            KuaiMaoQuickAction(
                                key = "mods",
                                label = stringResource(R.string.module),
                                icon = Icons.TwoTone.Extension,
                                tone = KuaiMaoTones.rose,
                                onClick = {
                                    val index = pages.indexOf(BottomBarDestination.Module)
                                    if (index >= 0) handlePageChange(index)
                                },
                            ),
                            KuaiMaoQuickAction(
                                key = "ai",
                                label = "AI 助手",
                                icon = Icons.TwoTone.AutoAwesome,
                                tone = KuaiMaoTones.blue,
                                onClick = { showAssistant = true },
                            ),
                            KuaiMaoQuickAction(
                                key = "set",
                                label = stringResource(R.string.settings),
                                icon = Icons.TwoTone.Settings,
                                tone = KuaiMaoTones.slate,
                                onClick = {
                                    val index = pages.indexOf(BottomBarDestination.Settings)
                                    if (index >= 0) handlePageChange(index)
                                },
                            ),
                        ),
                        onClick = {
                            // 整卡只在「未工作 + 内核可装」时进入安装镜像页，其余不跳转。
                            if (systemStatus.ksuVersion == null && systemStatus.kernelVersion.isGKI()) {
                                onInstall()
                            }
                        },
                    )
                }

                        KuaiMaoSectionHeader(title = "状态信息")
                        RevealItem(index = 1) {
                            KuaiMaoFeedCard(items = statusFeedItems(uiState))
                        }
                        KuaiMaoSectionHeader(title = "版本信息")
                        RevealItem(index = 2) {
                            KuaiMaoFeedCard(items = versionFeedItems(uiState))
                        }
                        KuaiMaoSectionHeader(title = "支持开发及了解更多")
                        RevealItem(index = 3) {
                            SupportAndLearnCard()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(bottomPadding + 96.dp))
            }
        }
    }

    if (showAssistant) {
        KuaiMaoAssistantSheet(
            uiState = uiState,
            onDismiss = { showAssistant = false },
            onOpenSuperUser = {
                showAssistant = false
                val index = pages.indexOf(BottomBarDestination.SuperUser)
                if (index >= 0) handlePageChange(index)
            },
            onOpenModule = {
                showAssistant = false
                val index = pages.indexOf(BottomBarDestination.Module)
                if (index >= 0) handlePageChange(index)
            },
            onInstallModule = {
                showAssistant = false
                navigator.push(Route.Install(preselectedKernelUri = null))
            },
            onOpenSystemInfo = {
                showAssistant = false
                navigator.push(Route.SystemInfo)
            },
        )
    }
}

/** 「越狱模式」是真实 late-load 运行态才有的说法，其余情况只描述工作状态。 */
private fun heroLabel(status: KernelStatus): String =
    if (status.isLateLoadMode) "JAILBREAK MODE · 越狱模式" else "KERNEL STATUS · 运行状态"

private fun heroStateText(uiState: HomeUiState): String {
    val status = uiState.systemStatus
    return when {
        status.ksuVersion != null && status.isSafeMode -> "安全模式"
        status.ksuVersion != null -> "工作中"
        status.kernelVersion.isGKI() -> "未工作"
        else -> "不支持"
    }
}

private fun heroHintText(uiState: HomeUiState): String {
    val status = uiState.systemStatus
    return when {
        status.ksuVersion == null -> if (status.kernelVersion.isGKI()) "内核可安装" else "内核版本过低"
        status.isLateLoadMode -> "临时加载"
        status.lkmMode == true -> "LKM 模式"
        else -> "内置模式"
    }
}

/** 运行模式标签：LKM / 内置，加载了 SUSFS 守卫模块时追加「SUSFS LKM 模式」，late-load 时再追加「越狱模式」。 */
private fun heroModeTags(status: KernelStatus, susfsLkmLoaded: Boolean): List<String> = buildList {
    if (status.ksuVersion != null) {
        add(if (status.lkmMode == true) "LKM" else "内置")
    }
    if (susfsLkmLoaded) {
        add("SUSFS LKM 模式")
    }
    if (status.isLateLoadMode) {
        add("越狱模式")
    }
    val machine = runCatching { Os.uname().machine }.getOrNull()
    if (status.ksuVersion != null && !machine.isNullOrBlank() && machine != "aarch64") {
        add(machine)
    }
}

private fun heroBadgeText(uiState: HomeUiState): String {
    val status = uiState.systemStatus
    return when {
        status.ksuVersion == null -> "待激活"
        status.isSELinuxPermissive -> "风险"
        else -> "安全"
    }
}

@Composable
private fun heroBadgeColor(uiState: HomeUiState): Color {
    val status = uiState.systemStatus
    return when {
        status.ksuVersion == null -> KuaiMaoColors.warning
        status.isSELinuxPermissive -> KuaiMaoColors.danger
        else -> KuaiMaoColors.success
    }
}

private fun seccompText(status: Int, notSupported: String, disabled: String, strict: String, filter: String): String = when (status) {
    -1 -> notSupported
    0 -> disabled
    1 -> strict
    2 -> filter
    else -> notSupported
}

/**
 * 主页骨架：圆角、行高、图标块尺寸都照真实卡片摆。
 *
 * 骨架是盖在真实内容上的一层（见 SkeletonSwap），所以这里只需要「长得像」，
 * 不需要负责撑高度——切换时页面高度由真实内容决定，绝不会跳。
 */
@Composable
private fun HomeSkeleton() {
    val phase = rememberSkeletonPhase()
    Column(modifier = Modifier.fillMaxWidth()) {
        // ── 大卡：标题 / 状态词 / 标签 / 指标 / 四宫格 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(324.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(KuaiMaoColors.canvasCard)
                .renderBackgroundBlur(tintColor = KuaiMaoColors.canvasCardSolid)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            SkeletonLine(widthFraction = 0.30f, height = 9.dp, phase = phase)
            Spacer(Modifier.height(16.dp))
            SkeletonLine(widthFraction = 0.46f, height = 26.dp, phase = phase)
            Spacer(Modifier.height(13.dp))
            Row {
                SkeletonBone(
                    modifier = Modifier
                        .width(58.dp)
                        .height(11.dp),
                    shape = RoundedCornerShape(6.dp),
                    phase = phase,
                )
                Spacer(Modifier.width(8.dp))
                SkeletonBone(
                    modifier = Modifier
                        .width(86.dp)
                        .height(11.dp),
                    shape = RoundedCornerShape(6.dp),
                    phase = phase,
                )
            }
            Spacer(Modifier.height(20.dp))
            SkeletonLine(widthFraction = 1f, height = 1.dp, phase = phase)
            Spacer(Modifier.height(18.dp))
            Row {
                SkeletonBone(
                    modifier = Modifier
                        .width(76.dp)
                        .height(19.dp),
                    shape = RoundedCornerShape(9.dp),
                    phase = phase,
                )
                Spacer(Modifier.width(28.dp))
                SkeletonBone(
                    modifier = Modifier
                        .width(62.dp)
                        .height(19.dp),
                    shape = RoundedCornerShape(9.dp),
                    phase = phase,
                )
            }
            Spacer(Modifier.height(22.dp))
            SkeletonLine(widthFraction = 1f, height = 1.dp, phase = phase)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(4) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SkeletonBone(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(15.dp),
                            phase = phase,
                        )
                        Spacer(Modifier.height(8.dp))
                        SkeletonLine(widthFraction = 0.58f, height = 9.dp, phase = phase)
                    }
                }
            }
        }

        // ── 状态信息 / 版本信息 / 支持开发及了解更多 三段列表 ──
        repeat(3) { section ->
            Spacer(Modifier.height(20.dp))
            Spacer(Modifier.height(4.dp))
            SkeletonLine(widthFraction = 0.18f, height = 11.dp, phase = phase)
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(KuaiMaoColors.canvasCard)
                    .renderBackgroundBlur(tintColor = KuaiMaoColors.canvasCardSolid)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                repeat(if (section == 2) 2 else 5) { index ->
                    if (index > 0) {
                        SkeletonLine(widthFraction = 1f, height = 1.dp, phase = phase)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkeletonBone(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(14.dp),
                            phase = phase,
                        )
                        Spacer(Modifier.width(13.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SkeletonLine(widthFraction = 0.34f, height = 12.dp, phase = phase)
                            Spacer(Modifier.height(6.dp))
                            SkeletonLine(widthFraction = 0.52f, height = 9.dp, phase = phase)
                        }
                        Spacer(Modifier.width(12.dp))
                        SkeletonLine(widthFraction = 0.16f, height = 11.dp, phase = phase)
                    }
                }
            }
        }
    }
}

/** 状态信息：安全上下文、系统调用过滤、管理器、钩子、注入框架。 */
@Composable
private fun statusFeedItems(uiState: HomeUiState): List<KuaiMaoFeedItem> {
    val info = uiState.systemInfo
    val status = uiState.systemStatus
    val selinuxValue = info.selinuxStatus.ifBlank { "—" }
    val permissive = info.selinuxStatus.contains("Permissive", ignoreCase = true) ||
        status.isSELinuxPermissive
    val seccomp = seccompText(
        info.seccompStatus,
        stringResource(R.string.seccomp_status_not_supported),
        stringResource(R.string.seccomp_status_disabled),
        stringResource(R.string.seccomp_status_strict),
        stringResource(R.string.seccomp_status_filter),
    )
    val zygisk = info.zygiskImplement.ifBlank { "None" }
    val zygiskActive = zygisk.isNotBlank() && zygisk != "None"
    // 活跃管理器：按签名分组拼成「UID 10462 · UID 10503」，和系统状态页同一套口径。
    val managerText = info.managersList?.managers.orEmpty()
        .groupBy { it.signatureIndex }
        .toSortedMap()
        .entries
        .joinToString(" · ") { (_, managers) ->
            managers.joinToString(", ") { "UID ${it.uid}" }
        }
        .ifBlank { "无" }

    return listOf(
        KuaiMaoFeedItem(
            key = "selinux",
            title = "SELinux 状态",
            desc = "安全上下文 · ${if (permissive) "宽容" else "完整"}",
            value = selinuxValue,
            icon = Icons.TwoTone.Security,
            tone = if (permissive) KuaiMaoTones.danger else KuaiMaoTones.green,
            valueColor = if (permissive) KuaiMaoColors.danger else backdropInk(),
        ),
        KuaiMaoFeedItem(
            key = "seccomp",
            title = "Seccomp 状态",
            desc = "系统调用过滤",
            value = seccomp,
            icon = Icons.TwoTone.FilterList,
            tone = KuaiMaoTones.violet,
            valueColor = if (info.seccompStatus > 0) backdropInk() else KuaiMaoColors.textSec,
        ),
        KuaiMaoFeedItem(
            key = "managers",
            title = "活跃管理器",
            desc = "当前签名与进程",
            value = managerText,
            icon = Icons.TwoTone.Group,
            tone = KuaiMaoTones.amber,
            valueColor = KuaiMaoColors.textSec,
        ),
        KuaiMaoFeedItem(
            key = "hook",
            title = "钩子类型",
            desc = "内核接管方式",
            value = status.hookType.ifBlank { "—" },
            icon = Icons.TwoTone.Tune,
            tone = KuaiMaoTones.blue,
            valueColor = backdropInk(),
        ),
        KuaiMaoFeedItem(
            key = "zygisk",
            title = "Zygisk 实现",
            desc = "注入框架",
            value = zygisk,
            icon = Icons.TwoTone.Bolt,
            tone = if (zygiskActive) KuaiMaoTones.blue else KuaiMaoTones.slate,
            valueColor = if (zygiskActive) backdropInk() else KuaiMaoColors.danger,
        ),
    )
}

/** 版本信息：设备、系统、内核与两个版本号。 */
@Composable
private fun versionFeedItems(uiState: HomeUiState): List<KuaiMaoFeedItem> {
    val info = uiState.systemInfo
    val status = uiState.systemStatus
    val managerVersion = info.managerVersion
    val managerText = if (managerVersion.first.isBlank()) {
        "—"
    } else {
        "${managerVersion.first} (${managerVersion.second}/${managerVersion.third})"
    }

    return listOf(
        KuaiMaoFeedItem(
            key = "device",
            title = "设备",
            desc = "机型",
            value = info.deviceModel.ifBlank { "—" },
            icon = Icons.TwoTone.Smartphone,
            tone = KuaiMaoTones.blue,
            valueColor = backdropInk(),
        ),
        KuaiMaoFeedItem(
            key = "android",
            title = "Android 版本",
            desc = "系统版本",
            value = info.androidVersion.ifBlank { "—" },
            icon = Icons.TwoTone.Android,
            tone = KuaiMaoTones.green,
            valueColor = backdropInk(),
        ),
        KuaiMaoFeedItem(
            key = "kernel",
            title = "内核版本",
            desc = "Linux kernel",
            value = info.kernelRelease.ifBlank { "—" },
            icon = Icons.TwoTone.Memory,
            tone = KuaiMaoTones.amber,
            valueColor = KuaiMaoColors.textSec,
        ),
        KuaiMaoFeedItem(
            key = "driver",
            title = "内核驱动版本",
            desc = if (status.ksuVersion != null) "已加载" else "未加载",
            value = status.ksuFullVersion.orEmpty().ifBlank { "—" },
            icon = Icons.TwoTone.DeveloperBoard,
            tone = KuaiMaoTones.violet,
            valueColor = if (status.ksuVersion != null) backdropInk() else KuaiMaoColors.danger,
        ),
        KuaiMaoFeedItem(
            key = "manager",
            title = "管理器版本",
            desc = stringResource(R.string.app_name),
            value = managerText,
            icon = Icons.TwoTone.Tag,
            tone = KuaiMaoTones.slate,
            valueColor = KuaiMaoColors.textSec,
        ),
    )
}

/** 主页顶部的一排安全提示，保持原有判定逻辑不变。 */
@Composable
private fun HomeWarnings(
    uiState: HomeUiState,
    onInstall: () -> Unit,
) {
    val status = uiState.systemStatus

    if (status.isManager && !status.isFullFeatured) {
        val message = if ((status.kernelUAPIVersion ?: 1) > status.managerUAPIVersion) {
            stringResource(R.string.require_manager_version)
        } else if (status.lkmMode == true) {
            stringResource(R.string.require_kernel_version)
        } else {
            stringResource(R.string.require_kernel_version_gki)
        }
        WarningCard(
            message = message,
            icon = {
                Icon(
                    imageVector = Icons.TwoTone.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp),
                )
            },
            onClick = onInstall,
        )
        Spacer(modifier = Modifier.height(10.dp))
    }

    if (!status.isOfficialSignature) {
        WarningCard(
            message = stringResource(R.string.unofficial_version_notice, stringResource(R.string.app_name)),
            icon = {
                Icon(
                    imageVector = Icons.TwoTone.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        Spacer(modifier = Modifier.height(10.dp))
    }

    if (BuildConfig.IS_PR_BUILD || status.isPrBuild) {
        WarningCard(
            message = stringResource(id = R.string.home_pr_build_warning),
            icon = {
                Icon(
                    imageVector = Icons.TwoTone.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        Spacer(modifier = Modifier.height(10.dp))
    }

    if (status.kernelPatchImplementation == KernelPatchImplementation.OFFICIAL) {
        WarningCard(
            message = stringResource(R.string.conflict_with_apatch),
            icon = {
                Icon(
                    imageVector = Icons.TwoTone.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        Spacer(modifier = Modifier.height(10.dp))
    }

    if (status.ksuVersion != null && !status.isRootAvailable) {
        WarningCard(
            message = stringResource(id = R.string.grant_root_failed),
            icon = {
                Icon(
                    imageVector = Icons.TwoTone.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

/**
 * 系统信息页：承接原主页的完整版本 / 状态明细，以及更新与链接卡片。
 * 「主页 → 查看全部」跳转至此，保证信息不丢失。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SystemInfoPage(bottomPadding: Dp) {
    val viewModel = koinViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.blurEffect(),
                title = { Text(text = "系统状态") },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
                scrollBehavior = scrollBehavior,
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = adaptiveScaffoldWindowInsets(includeBottom = false),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blurSource()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(scrollState)
                .padding(
                    top = innerPadding.calculateTopPadding() + 2.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
        ) {
            if (uiState.isCoreDataLoaded) {
                HomeWarnings(
                    uiState = uiState,
                    onInstall = {
                        if (uiState.systemStatus.isLateLoadMode) return@HomeWarnings
                        navigator.push(Route.Install(preselectedKernelUri = null))
                    },
                )
            }

            ManagerUpdateCard(uiState.stableManagerUpdate)
            ManagerUpdateCard(uiState.betaManagerUpdate)
            if (uiState.isBetaManagerUpdateCheckFailed) {
                WarningCard(
                    message = stringResource(R.string.beta_update_check_failed),
                    icon = {
                        Icon(
                            imageVector = Icons.TwoTone.Error,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (uiState.isExtendedDataLoaded) {
                InfoCard(
                    systemStatus = uiState.systemStatus,
                    systemInfo = uiState.systemInfo,
                    isSimpleMode = uiState.isSimpleMode,
                    showHomeCardIcons = uiState.showHomeCardIcons,
                )
            }

            if (!uiState.isSimpleMode) {
                KuaiMaoSectionHeader(title = "支持开发及了解更多")
                SupportAndLearnCard()
            }

            Spacer(Modifier.height(bottomPadding + 24.dp))
        }
    }
}

@Composable
private fun ManagerUpdateCard(update: ManagerUpdateInfo?) {
    val visibilityState = remember { MutableTransitionState(false) }
    var displayedUpdate by remember { mutableStateOf<ManagerUpdateInfo?>(null) }

    LaunchedEffect(update) {
        if (update != null) displayedUpdate = update
        visibilityState.targetState = update != null
    }

    AnimatedVisibility(
        visibleState = visibilityState,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        displayedUpdate?.let { updateInfo ->
            ManagerUpdateCardContent(updateInfo)
        }
    }
}

@Composable
private fun ManagerUpdateCardContent(updateInfo: ManagerUpdateInfo) {
    val context = LocalContext.current
    val permissionRequestInterface = LocalPermissionRequestInterface.current
    val enqueueManagerUpdate = koinInject<EnqueueManagerUpdateUseCase>()
    val channelTitle = stringResource(
        if (updateInfo.channel == ManagerUpdateChannel.STABLE) {
            R.string.manager_update_stable
        } else {
            R.string.manager_update_beta
        },
    )
    val message = if (updateInfo.channel == ManagerUpdateChannel.STABLE) {
        stringResource(R.string.new_version_available, updateInfo.versionCode)
    } else {
        stringResource(R.string.beta_version_available, updateInfo.versionCode)
    }
    val updateText = stringResource(R.string.module_update)
    val details = stringResource(
        R.string.manager_update_details,
        updateInfo.versionName,
        updateInfo.versionCode,
        updateInfo.abi,
    )
    val dialogContent = if (updateInfo.changelog.isBlank()) {
        details
    } else {
        "$details\n\n${updateInfo.changelog}"
    }
    val updateDialog = rememberConfirmDialog(
        onConfirm = {
            downloadManagerUpdate(
                context,
                permissionRequestInterface,
                updateInfo,
                enqueueManagerUpdate,
            )
        },
    )

    WarningCard(
        message = message,
        color = MaterialTheme.colorScheme.outlineVariant,
        icon = {
            Icon(
                imageVector = Icons.TwoTone.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        onClick = {
            updateDialog.showConfirm(
                title = channelTitle,
                content = dialogContent,
                markdown = updateInfo.changelog.isNotBlank(),
                confirm = updateText,
            )
        },
    )

    Spacer(modifier = Modifier.height(10.dp))
}


/**
 * 「支持开发」+「了解更多」。
 *
 * 以前是两张卡、各自带着一句分组标题，读起来像两个互不相干的模块，
 * 而且标题和行标题重复（上面写「支持开发」下面又写「支持开发」）。
 * 现在并成一张卡、两行，共用一句分组标题，行高 / 字号 / 配色
 * 与上面「状态信息」「版本信息」完全一致（同一个 [KuaiMaoFeedCard]）。
 */
@Composable
fun SupportAndLearnCard() {
    val uriHandler = LocalUriHandler.current
    val learnUrl = stringResource(R.string.home_learn_kernelsu_url)

    KuaiMaoFeedCard(
        items = listOf(
            KuaiMaoFeedItem(
                key = "donate",
                title = stringResource(R.string.home_support_title),
                desc = stringResource(R.string.home_support_content),
                value = "",
                icon = Icons.TwoTone.Coffee,
                tone = KuaiMaoTones.rose,
                valueColor = KuaiMaoColors.textSec,
                descMaxLines = 2,
                trailingChevron = true,
                onClick = {
                    uriHandler.openUri("https://patreon.com/weishu")
                },
            ),
            KuaiMaoFeedItem(
                key = "learn-more",
                title = stringResource(R.string.home_learn_kernelsu),
                desc = stringResource(R.string.home_click_to_learn_kernelsu),
                value = "",
                icon = Icons.AutoMirrored.TwoTone.MenuBook,
                tone = KuaiMaoTones.blue,
                valueColor = KuaiMaoColors.textSec,
                descMaxLines = 2,
                trailingChevron = true,
                onClick = {
                    uriHandler.openUri(learnUrl)
                },
            ),
        ),
    )
}


@Composable
private fun InfoCard(
    systemStatus: KernelStatus,
    systemInfo: HomeSystemInfo,
    isSimpleMode: Boolean,
    showHomeCardIcons: Boolean,
) {
    val managersList = systemInfo.managersList

    SegmentedColumn(
        title = stringResource(R.string.home_version_info),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
    ) {
        item {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Smartphone.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_device_model),
                description = systemInfo.deviceModel,
            )
        }

        item {
            SettingsBaseWidget(
                icon = Icons.TwoTone.DeveloperBoard.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_kernel),
                description = systemInfo.kernelRelease,
            )
        }

        item(
            visible = !isSimpleMode,
        ) {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Android.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_android_version),
                description = systemInfo.androidVersion,
            )
        }

        item(
            visible = systemStatus.isManager,
        ) {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Memory.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_kernel_version),
                description = systemStatus.ksuFullVersion.orEmpty(),
            )
        }

        item {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Tag.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_manager_version),
                description = "${systemInfo.managerVersion.first} (${systemInfo.managerVersion.second}/${systemInfo.managerVersion.third})",
            )
        }

        item(
            visible = !isSimpleMode && systemInfo.susfsEnabled && systemInfo.susfsVersion.isNotEmpty(),
        ) {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Settings.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_susfs_version),
                description = systemInfo.susfsVersion,
            )
        }
    }

    SegmentedColumn(
        title = stringResource(R.string.home_status_info),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
    ) {
        item {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Security.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_selinux_status),
                description = systemInfo.selinuxStatus,
            )
        }

        item {
            val seccompDisplay = when (systemInfo.seccompStatus) {
                -1 -> stringResource(R.string.seccomp_status_not_supported)
                0 -> stringResource(R.string.seccomp_status_disabled)
                1 -> stringResource(R.string.seccomp_status_strict)
                2 -> stringResource(R.string.seccomp_status_filter)
                else -> stringResource(R.string.seccomp_status_unknown)
            }

            SettingsBaseWidget(
                icon = Icons.TwoTone.FilterList.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_seccomp_status),
                description = seccompDisplay,
            )
        }

        item(
            visible = !isSimpleMode && managersList != null,
        ) {
            val signatureMap =
                managersList?.managers.orEmpty().groupBy { it.signatureIndex }
            val managersText = buildString {
                signatureMap.toSortedMap().forEach { (signatureIndex, managers) ->
                    append(managers.joinToString(", ") { "UID: ${it.uid}" })
                    append(" ")
                    append(
                        when (signatureIndex) {
                            0 -> "(${stringResource(R.string.app_name)})"

                            255 -> "(${stringResource(R.string.dynamic_managerature)})"

                            else -> if (signatureIndex >= 1) {
                                "(${
                                    stringResource(
                                        R.string.signature_index,
                                        signatureIndex,
                                    )
                                })"
                            } else {
                                "(${stringResource(R.string.unknown_signature)})"
                            }
                        },
                    )
                    append(" | ")
                }
            }.trimEnd(' ', '|')

            SettingsBaseWidget(
                icon = Icons.TwoTone.Group.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.multi_manager_list),
                description = managersText.ifEmpty { stringResource(R.string.no_active_manager) },
            )
        }

        item(
            visible = !isSimpleMode && systemStatus.isFullFeatured,
        ) {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Tune.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_hook_type),
                description = systemStatus.hookType,
            )
        }

        item(
            visible = !isSimpleMode && systemInfo.zygiskImplement.isNotEmpty() && systemInfo.zygiskImplement != "None",
        ) {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Extension.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_zygisk_implement),
                description = systemInfo.zygiskImplement,
            )
        }

        item(
            visible = !isSimpleMode && systemInfo.metaModuleImplement.isNotEmpty() && systemInfo.metaModuleImplement != "None",
        ) {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Extension.takeIf { showHomeCardIcons },
                iconPlaceholder = false,
                title = stringResource(R.string.home_meta_module_implement),
                description = systemInfo.metaModuleImplement,
            )
        }
    }
}
