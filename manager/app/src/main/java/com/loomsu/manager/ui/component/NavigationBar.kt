package com.loomsu.manager.ui.activity.component

import android.annotation.SuppressLint
import android.widget.Toast
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.DeveloperBoard
import androidx.compose.material.icons.twotone.PowerSettingsNew
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.RestartAlt
import androidx.compose.material.icons.twotone.SettingsBackupRestore
import androidx.compose.material.icons.twotone.Usb
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.WideNavigationRailColors
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loomsu.manager.ui.component.FloatingBottomBar
import com.loomsu.manager.ui.component.FloatingBottomBarItem
import com.loomsu.manager.ui.screen.BottomBarDestination
import com.loomsu.manager.ui.theme.BottomBarStyle
import com.loomsu.manager.ui.theme.LoomColors
import com.loomsu.manager.ui.theme.LoomDimens
import com.loomsu.manager.ui.theme.ThemeConfig
import com.loomsu.manager.ui.theme.blurEffect
import com.loomsu.manager.ui.util.LocalBlurState
import com.loomsu.manager.ui.util.LocalHandlePageChange
import com.loomsu.manager.ui.util.LocalPagerState
import com.loomsu.manager.ui.util.LocalSelectedPage
import com.loomsu.manager.ui.viewmodel.HomeUiAction
import com.loomsu.manager.ui.viewmodel.HomeViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    destinations: List<BottomBarDestination>,
    isBottomBar: Boolean,
) {
    val themeConfig: ThemeConfig = koinInject()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val superuserCount = uiState.systemInfo.superuserCount
    val moduleCount = uiState.systemInfo.moduleCount
    val showNavigationBarBadge = uiState.showNavigationBarBadge
    val page = LocalSelectedPage.current
    val handlePageChange = LocalHandlePageChange.current
    val pagerState = LocalPagerState.current

    var showRebootSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (isBottomBar) {
        val style = themeConfig.bottomBarStyle
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
                )
                .then(
                    // 胶囊栏需要四周留白浮起来；传统栏贴底贴边。
                    if (style == BottomBarStyle.CAPSULE) {
                        Modifier.padding(
                            start = LoomDimens.navInset,
                            end = LoomDimens.navInset,
                            bottom = LoomDimens.navInset + bottomInset,
                        )
                    } else {
                        Modifier
                    },
                )
                // 整框左右拖动即可切换页面
                .pointerInput(pagerState.currentPage, destinations.size) {
                    var accumulated = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { accumulated = 0f },
                        onDragCancel = { accumulated = 0f },
                        onDragEnd = {
                            val threshold = 40.dp.toPx()
                            when {
                                accumulated <= -threshold -> handlePageChange(
                                    (pagerState.currentPage + 1).coerceAtMost(destinations.size - 1),
                                )

                                accumulated >= threshold -> handlePageChange(
                                    (pagerState.currentPage - 1).coerceAtLeast(0),
                                )
                            }
                            accumulated = 0f
                        },
                    ) { change, dragAmount ->
                        accumulated += dragAmount
                        change.consume()
                    }
                },
        ) {
            when (style) {
                BottomBarStyle.CAPSULE -> LoomBottomBar(
                    destinations = destinations,
                    selectedIndex = pagerState.currentPage,
                    onSelected = handlePageChange,
                    onCenterClick = { showRebootSheet = true },
                    superuserCount = superuserCount,
                    moduleCount = moduleCount,
                    showBadge = showNavigationBarBadge,
                )

                // 侧边栏在 MainScreen 里走 rail 布局，这里只作为兜底。
                BottomBarStyle.TRADITIONAL,
                BottomBarStyle.RAIL,
                -> LoomTraditionalBottomBar(
                    destinations = destinations,
                    selectedIndex = pagerState.currentPage,
                    onSelected = handlePageChange,
                    onCenterClick = { showRebootSheet = true },
                    superuserCount = superuserCount,
                    moduleCount = moduleCount,
                    showBadge = showNavigationBarBadge,
                )
            }
        }

        RebootSheet(
            visible = showRebootSheet,
            onDismiss = { showRebootSheet = false },
            onReboot = { reason ->
                showRebootSheet = false
                if (uiState.systemStatus.isRootAvailable) {
                    homeViewModel.dispatch(HomeUiAction.Reboot(reason))
                } else {
                    Toast.makeText(context, "尚未进入工作状态，无法重启", Toast.LENGTH_SHORT).show()
                }
            },
        )
    } else {
        LoomRailNavigation(
            modifier = modifier,
            destinations = destinations,
            selectedIndex = page,
            onSelected = handlePageChange,
            superuserCount = superuserCount,
            moduleCount = moduleCount,
            showBadge = showNavigationBarBadge,
        )
    }
}

/**
 * 底栏模糊层的着色不透明度。
 * 取 0.92：底栏看起来仍是实心卡片，只透出极淡的一层背景色雾，
 * 图标与文字对比度不受影响（不会出现「看不清」）。
 */
private const val NAV_BLUR_TINT_ALPHA = 0.92f

/** 侧边栏：竖排导航，整条 rail 也支持左右拖动切页。 */
@Composable
private fun LoomRailNavigation(
    modifier: Modifier = Modifier,
    destinations: List<BottomBarDestination>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    superuserCount: Int,
    moduleCount: Int,
    showBadge: Boolean,
) {
    WideNavigationRail(
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
            ),
        colors = WideNavigationRailColors(
            containerColor = LoomColors.canvasCard,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modalContainerColor = WideNavigationRailDefaults.colors().modalContainerColor,
            modalScrimColor = WideNavigationRailDefaults.colors().modalScrimColor,
            modalContentColor = WideNavigationRailDefaults.colors().modalContentColor,
        ),
    ) {
        destinations.forEachIndexed { index, destination ->
            NavigationRailItem(
                isSelected = index == selectedIndex,
                destination = destination,
                onClick = { onSelected(index) },
                superuserCount = superuserCount,
                moduleCount = moduleCount,
                showNavigationBarBadge = showBadge,
            )
        }
    }
}

@Composable
private fun NavigationRailItem(
    isSelected: Boolean,
    destination: BottomBarDestination,
    onClick: () -> Unit,
    superuserCount: Int,
    moduleCount: Int,
    showNavigationBarBadge: Boolean,
) {
    WideNavigationRailItem(
        railExpanded = false,
        selected = isSelected,
        onClick = onClick,
        icon = {
            BadgedBox(
                badge = {
                    DestinationBadge(
                        dest = destination,
                        superUser = superuserCount,
                        module = moduleCount,
                        show = showNavigationBarBadge,
                    )
                },
            ) {
                if (isSelected) {
                    Icon(destination.iconSelected, stringResource(destination.label))
                } else {
                    Icon(destination.iconNotSelected, stringResource(destination.label))
                }
            }
        },
        label = {
            Text(
                stringResource(destination.label),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
            )
        },
    )
}

@Composable
private fun DestinationBadge(
    dest: BottomBarDestination,
    superUser: Int,
    module: Int,
    show: Boolean,
) {
    val count = when (dest) {
        BottomBarDestination.SuperUser -> superUser
        BottomBarDestination.Module -> module
        else -> 0
    }

    AnimatedVisibility(
        visible = count > 0 && show,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Badge(
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Text(count.toString())
        }
    }
}


private data class RebootAction(
    val label: String,
    val desc: String,
    val reason: String,
    val icon: ImageVector,
    val danger: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RebootSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onReboot: (String) -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val actions = listOf(
        RebootAction("重启", "完整重启设备", "reboot", Icons.TwoTone.RestartAlt),
        RebootAction("软重启", "仅重启系统界面", "soft_reboot", Icons.TwoTone.Refresh),
        RebootAction("Recovery", "进入恢复模式", "recovery", Icons.TwoTone.SettingsBackupRestore),
        RebootAction("Bootloader", "进入引导加载器", "bootloader", Icons.TwoTone.DeveloperBoard),
        RebootAction("EDL", "进入紧急下载模式", "edl", Icons.TwoTone.Usb, danger = true),
        RebootAction("关机", "完全关闭设备电源", "poweroff", Icons.TwoTone.PowerSettingsNew, danger = true),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LoomColors.canvasCard,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 30.dp),
        ) {
            // grab 条
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 16.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(LoomColors.lineField),
            )

            Text(
                text = "电源与重启",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.2).sp,
                color = LoomColors.text,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "选择一项操作，设备会立即执行",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = LoomColors.textMuted,
            )

            Spacer(modifier = Modifier.height(16.dp))

            actions.chunked(2).forEachIndexed { index, row ->
                if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { action ->
                        RebootTile(
                            action = action,
                            modifier = Modifier.weight(1f),
                            onClick = { onReboot(action.reason) },
                        )
                    }
                }
            }
        }
    }
}

/** 设计稿的六宫格 tile：图标在上，标题 13.5/900，描述 9.5，危险项整体转红。 */
@Composable
private fun RebootTile(
    action: RebootAction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val accent = if (action.danger) LoomColors.danger else MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .clip(shape)
            .background(LoomColors.canvasField)
            .border(1.5.dp, LoomColors.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = action.label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Black,
            color = if (action.danger) LoomColors.danger else LoomColors.textBody,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = action.desc,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            color = LoomColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 传统底栏：贴底全宽、顶部一条发丝线，选中项带胶囊底色。
 * 与胶囊栏的区别是“不浮起来”：没有外边距与圆角，背景一直铺到导航条底部。
 */
@Composable
private fun LoomTraditionalBottomBar(
    destinations: List<BottomBarDestination>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    onCenterClick: () -> Unit,
    superuserCount: Int,
    moduleCount: Int,
    showBadge: Boolean,
) {
    val themeConfig: ThemeConfig = koinInject()
    // 模糊只做在底栏自己的背景层上：底色用卡片色 + 高不透明度，文字始终清晰
    val blurEnabled = themeConfig.isEnableBlur
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (blurEnabled) {
                    Modifier.blurEffect(
                        compensateHorizontalOverscroll = true,
                        compensateVerticalOverscroll = true,
                        useFixedSurfaceBoundsForOverscroll = true,
                        tintAlphaOverride = NAV_BLUR_TINT_ALPHA,
                        tintColorOverride = LoomColors.canvasCard,
                    )
                } else {
                    Modifier
                },
            )
            .background(if (blurEnabled) Color.Transparent else LoomColors.canvasCard),
    ) {
        HorizontalDivider(
            color = LoomColors.line,
            thickness = 1.dp,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .height(58.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEachIndexed { index, destination ->
                if (index == 2) {
                    LoomClassicCenterButton(onClick = onCenterClick)
                }
                LoomClassicNavItem(
                    destination = destination,
                    selected = index == selectedIndex,
                    superuserCount = superuserCount,
                    moduleCount = moduleCount,
                    showBadge = showBadge,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun LoomClassicNavItem(
    destination: BottomBarDestination,
    selected: Boolean,
    superuserCount: Int,
    moduleCount: Int,
    showBadge: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val color = if (selected) accent else LoomColors.textDisabled
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) accent.copy(alpha = 0.12f) else Color.Transparent)
                .padding(horizontal = 13.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            BadgedBox(
                badge = {
                    DestinationBadge(
                        dest = destination,
                        superUser = superuserCount,
                        module = moduleCount,
                        show = showBadge,
                    )
                },
            ) {
                Icon(
                    imageVector = if (selected) destination.iconSelected else destination.iconNotSelected,
                    contentDescription = stringResource(destination.label),
                    tint = color,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = stringResource(destination.label),
            color = color,
            fontSize = 8.5.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}

/** 传统栏中央电源键：圆形实心按钮。 */
@Composable
private fun LoomClassicCenterButton(onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(42.dp)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = accent.copy(alpha = 0.30f),
                spotColor = accent.copy(alpha = 0.40f),
            )
            .clip(CircleShape)
            .background(accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.TwoTone.PowerSettingsNew,
            contentDescription = "电源与重启",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun LoomBottomBar(
    destinations: List<BottomBarDestination>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    onCenterClick: () -> Unit,
    superuserCount: Int,
    moduleCount: Int,
    showBadge: Boolean,
) {
    // 设计稿：66 高、22 圆角、左右 14 内边距的浮岛底栏
    val themeConfig: ThemeConfig = koinInject()
    val blurEnabled = themeConfig.isEnableBlur
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                clip = false,
                ambientColor = Color(0x2918263C),
                spotColor = Color(0x2918263C),
            )
            .clip(shape)
            .then(
                if (blurEnabled) {
                    Modifier.blurEffect(
                        compensateHorizontalOverscroll = true,
                        compensateVerticalOverscroll = true,
                        useFixedSurfaceBoundsForOverscroll = true,
                        tintAlphaOverride = NAV_BLUR_TINT_ALPHA,
                        tintColorOverride = LoomColors.canvasCard,
                    )
                } else {
                    Modifier
                },
            )
            .background(if (blurEnabled) Color.Transparent else LoomColors.canvasCard)
            .height(66.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        destinations.forEachIndexed { index, destination ->
            if (index == 2) {
                LoomCenterButton(onClick = onCenterClick)
            }
            LoomNavItem(
                destination = destination,
                selected = index == selectedIndex,
                superuserCount = superuserCount,
                moduleCount = moduleCount,
                showBadge = showBadge,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(index) },
            )
        }
    }
}

@Composable
private fun LoomNavItem(
    destination: BottomBarDestination,
    selected: Boolean,
    superuserCount: Int,
    moduleCount: Int,
    showBadge: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // 与角标（Badge）同源：选中取主题 primary，未选中为设计稿的 textDisabled
    val accent = MaterialTheme.colorScheme.primary
    val color = if (selected) accent else LoomColors.textDisabled
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        BadgedBox(
            badge = {
                DestinationBadge(
                    dest = destination,
                    superUser = superuserCount,
                    module = moduleCount,
                    show = showBadge,
                )
            },
        ) {
            Icon(
                imageVector = if (selected) destination.iconSelected else destination.iconNotSelected,
                contentDescription = stringResource(destination.label),
                tint = color,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = stringResource(destination.label),
            color = color,
            fontSize = 8.5.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}

/** 中央电源键：与底栏角标同色（主题 primary）的竖向圆角块。 */
@Composable
private fun LoomCenterButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .size(width = 52.dp, height = 44.dp)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.copy(alpha = 0.34f),
                spotColor = accent.copy(alpha = 0.44f),
            )
            .clip(shape)
            .background(accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.TwoTone.PowerSettingsNew,
            contentDescription = "电源与重启",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(21.dp),
        )
    }
}
