package com.kuaimaosu.manager.ui.activity.component

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import com.kuaimaosu.manager.ui.theme.CapsuleEffect
import com.kuaimaosu.manager.ui.theme.CardConfig
import kotlin.math.absoluteValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuaimaosu.manager.ui.component.FloatingBottomBar
import com.kuaimaosu.manager.ui.component.FloatingBottomBarItem
import com.kuaimaosu.manager.ui.screen.BottomBarDestination
import com.kuaimaosu.manager.ui.theme.BottomBarStyle
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoDimens
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.theme.blurEffect
import com.kuaimaosu.manager.ui.theme.renderBackgroundBlur
import com.kuaimaosu.manager.ui.theme.isInDarkTheme
import com.kuaimaosu.manager.ui.util.LocalBlurState
import com.kuaimaosu.manager.ui.util.LocalHandlePageChange
import com.kuaimaosu.manager.ui.util.LocalPagerState
import com.kuaimaosu.manager.ui.util.LocalSelectedPage
import com.kuaimaosu.manager.ui.viewmodel.HomeUiAction
import com.kuaimaosu.manager.ui.viewmodel.HomeViewModel
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
                            start = KuaiMaoDimens.navInset,
                            end = KuaiMaoDimens.navInset,
                            bottom = KuaiMaoDimens.navInset + bottomInset,
                        )
                    } else {
                        Modifier
                    },
                )
                // 传统栏自己不带手势：整框左右拖动切页。
                // 胶囊栏的拖动由 FloatingBottomBar 内部的 DampedDragAnimation 接管，这里不能再抢事件。
                .then(
                    if (style == BottomBarStyle.CAPSULE) {
                        Modifier
                    } else {
                        Modifier.pointerInput(pagerState.currentPage, destinations.size) {
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
                        }
                    },
                ),
        ) {
            when (style) {
                BottomBarStyle.CAPSULE -> KuaiMaoLiquidBottomBar(
                    destinations = destinations,
                    selectedIndex = pagerState.currentPage,
                    onSelected = handlePageChange,
                    onCenterClick = { showRebootSheet = true },
                    superuserCount = superuserCount,
                    moduleCount = moduleCount,
                    showBadge = showNavigationBarBadge,
                    // 质感档位由 FloatingBottomBar 自己读 ThemeConfig 判定，
                    // 这里只把「全局模糊开关」传下去，别在传参时就把半透明当成关模糊。
                    isBlurEnabled = themeConfig.isEnableBlur,
                )

                // 侧边栏在 MainScreen 里走 rail 布局，这里只作为兜底。
                BottomBarStyle.TRADITIONAL,
                BottomBarStyle.RAIL,
                -> KuaiMaoTraditionalBottomBar(
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
        KuaiMaoRailNavigation(
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
private const val NAV_BLUR_TINT_ALPHA = 0.80f

/** 侧边栏：竖排导航，整条 rail 也支持左右拖动切页。 */
@Composable
private fun KuaiMaoRailNavigation(
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
            containerColor = KuaiMaoColors.canvasCardSolid,
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

/**
 * 强调色底上的前景色：石墨灰底配白字，亮色主题色底配近黑字。
 * 只看亮度，避免"白底白字"。
 */
@Composable
@ReadOnlyComposable
private fun onBadgeInk(): Color =
    if (KuaiMaoColors.brand.luminance() < 0.55f) Color.White else Color(0xFF101318)

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
        // Material3 的 Badge 默认吃 LocalTextStyle（bodyLarge 16sp），一个「7」
        // 就被撑成 22dp 的大圆，正好糊在 20dp 的图标脸上。这里字号、行高、
        // 最小尺寸全部写死，做成 13dp 的小角标，再往右上让开图标。
        Badge(
            containerColor = KuaiMaoColors.brand,
            contentColor = onBadgeInk(),
            modifier = Modifier
                .offset(x = 4.dp, y = (-4).dp)
                .heightIn(min = 13.dp)
                .widthIn(min = 13.dp),
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                fontSize = 8.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
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
        // 面板自己不再填实色：交给内容层按卡片的规则铺（半透明 + 壁纸磨砂），
        // 否则自定义壁纸下这一整块就是压在壁纸上的实心黑板 / 白板。
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        // 系统栏留白改由内容自己吃（在磨砂层之后），否则面板底部会露出一条
        // 透明的缝，壁纸直接从缝里穿上来。
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(KuaiMaoColors.canvasCard)
                .renderBackgroundBlur(tintColor = KuaiMaoColors.canvasCardSolid)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 30.dp),
        ) {
            // grab 条
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 16.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(KuaiMaoColors.lineField),
            )

            Text(
                text = "电源与重启",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.2).sp,
                color = KuaiMaoColors.text,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "选择一项操作，设备会立即执行",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = KuaiMaoColors.textMuted,
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
    val accent = if (action.danger) KuaiMaoColors.danger else KuaiMaoColors.brand
    Column(
        modifier = modifier
            .clip(shape)
            .background(KuaiMaoColors.canvasField)
            .border(1.5.dp, KuaiMaoColors.line, shape)
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
            color = if (action.danger) KuaiMaoColors.danger else KuaiMaoColors.textBody,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = action.desc,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            color = KuaiMaoColors.textMuted,
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
private fun KuaiMaoTraditionalBottomBar(
    destinations: List<BottomBarDestination>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    onCenterClick: () -> Unit,
    superuserCount: Int,
    moduleCount: Int,
    showBadge: Boolean,
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val wallpaperMode = cardConfig.isCustomBackgroundEnabled
    val blurEnabled = themeConfig.isEnableBlur
    val effect = themeConfig.capsuleEffect
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    // 有壁纸：磨砂档把膜加厚、液态玻璃档维持原观感；
                    // 半透明档按定义不做模糊，只留一层半透薄膜。
                    wallpaperMode && blurEnabled && effect == CapsuleEffect.TRANSLUCENT ->
                        Modifier.background(
                            KuaiMaoColors.canvasCard.copy(
                                alpha = (cardConfig.cardAlpha * 0.52f).coerceAtLeast(0.22f),
                            ),
                        )

                    wallpaperMode -> Modifier
                        .background(KuaiMaoColors.canvasCard)
                        .renderBackgroundBlur(
                            tintColor = KuaiMaoColors.canvasCardSolid,
                            tintAlpha = if (effect == CapsuleEffect.FROSTED) {
                                0.95f
                            } else {
                                cardConfig.cardAlpha
                            },
                        )

                    // 没壁纸：底栏自己一层磨砂底；三档的模糊半径与膜厚由 blurEffect 统一处理。
                    blurEnabled -> Modifier.blurEffect(
                        compensateHorizontalOverscroll = true,
                        compensateVerticalOverscroll = true,
                        useFixedSurfaceBoundsForOverscroll = true,
                        tintAlphaOverride = NAV_BLUR_TINT_ALPHA,
                        tintColorOverride = KuaiMaoColors.canvasCardSolid,
                    )

                    else -> Modifier.background(KuaiMaoColors.canvasCardSolid)
                },
            ),
    ) {
        // 自定义壁纸下不要顶边那条线：墙纸是一张图，压一道灰边很突兀
        if (!cardConfig.isCustomBackgroundEnabled) {
            HorizontalDivider(
                color = KuaiMaoColors.line,
                thickness = 1.dp,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
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
                    KuaiMaoClassicCenterButton(onClick = onCenterClick)
                }
                KuaiMaoClassicNavItem(
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
private fun KuaiMaoClassicNavItem(
    destination: BottomBarDestination,
    selected: Boolean,
    superuserCount: Int,
    moduleCount: Int,
    showBadge: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // 强调色取 KuaiMaoColors.brand（和全站按钮、选中态同一个来源）。
    // 之前取 MaterialTheme.primary，开了动态取色时这里会拿到种子色（偏蓝），
    // 于是别的控件已经是石墨灰，只有底栏还是蓝的。
    val accent = KuaiMaoColors.brand
    // 只改这里的取色：底栏本身仍是半透明磨砂，但未选中项直接用正文墨色
    // （之前是 #A6ACBC/浅灰，压在花壁纸的磨砂条上根本读不出来）。
    // 深色主题下品牌色如果本身很暗（比如石板蓝），选中项要提亮才看得见。
    val darkTheme = KuaiMaoColors.text.luminance() > 0.5f
    val selectedInk = if (darkTheme && accent.luminance() < 0.42f) {
        lerp(accent, Color.White, 0.62f)
    } else {
        accent
    }
    val color = if (selected) selectedInk else KuaiMaoColors.text
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
                .background(if (selected) selectedInk.copy(alpha = 0.12f) else Color.Transparent)
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
            fontSize = 9.5.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}

/** 传统栏中央电源键：与胶囊栏完全一致的横向圆角块（52×44、16dp 圆角）。 */
@Composable
private fun KuaiMaoClassicCenterButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val accent = KuaiMaoColors.brand
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
            tint = onBadgeInk(),
            modifier = Modifier.size(21.dp),
        )
    }
}

/** 中间电源键占的格子：0 首页、1 超级用户、2 电源、3 模块、4 设置。 */
private const val LIQUID_POWER_SLOT = 2

/**
 * 胶囊底栏：直接用仓库自带的 iOS 液态玻璃实现（miuix blur + lens 折射 + vibrancy + 随重力偏移的高光）。
 *
 * 不要再改回 Canvas 手绘：折射率、色散、边缘高光和拖拽时的液体拉伸都在 [FloatingBottomBar] 里，
 * 手绘版本再怎么调参数都不会是同一个东西。
 */
@Composable
private fun KuaiMaoLiquidBottomBar(
    destinations: List<BottomBarDestination>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    onCenterClick: () -> Unit,
    superuserCount: Int,
    moduleCount: Int,
    showBadge: Boolean,
    isBlurEnabled: Boolean,
) {
    // page(0..3) → slot：电源插在第 3 格，后面的页面顺延一格
    fun slotOf(page: Int) = if (page >= LIQUID_POWER_SLOT) page + 1 else page

    FloatingBottomBar(
        modifier = Modifier.fillMaxWidth(),
        selectedIndex = slotOf(selectedIndex),
        onSelected = { slot ->
            if (slot != LIQUID_POWER_SLOT) {
                onSelected(if (slot > LIQUID_POWER_SLOT) slot - 1 else slot)
            }
        },
        tabsCount = destinations.size + 1,
        isBlurEnabled = isBlurEnabled,
        isTabSelectable = { it != LIQUID_POWER_SLOT },
        onTabTap = { slot ->
            if (slot == LIQUID_POWER_SLOT) {
                onCenterClick()
            } else {
                onSelected(if (slot > LIQUID_POWER_SLOT) slot - 1 else slot)
            }
        },
    ) { activate ->
        destinations.forEachIndexed { index, destination ->
            if (index == LIQUID_POWER_SLOT) {
                // 电源键只是个入口，不参与选中
                FloatingBottomBarItem(
                    selected = false,
                    onClick = onCenterClick,
                ) {
                    KuaiMaoPowerGlyph()
                }
            }
            FloatingBottomBarItem(
                selected = index == selectedIndex,
                onClick = { activate(slotOf(index)) },
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
                        imageVector = if (index == selectedIndex) {
                            destination.iconSelected
                        } else {
                            destination.iconNotSelected
                        },
                        contentDescription = stringResource(destination.label),
                        tint = LocalContentColor.current,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = stringResource(destination.label),
                    color = LocalContentColor.current,
                    fontSize = 9.5.sp,
                    fontWeight = if (index == selectedIndex) FontWeight.ExtraBold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                )
            }
        }
    }
}

/** 胶囊栏中间的电源键：纯主题色的独立小胶囊，比导航项高一档，手指好按。 */
@Composable
private fun KuaiMaoPowerGlyph() {
    val accent = KuaiMaoColors.brand
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(width = 58.dp, height = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(accent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.TwoTone.PowerSettingsNew,
            contentDescription = "电源与重启",
            tint = onBadgeInk(),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun KuaiMaoNavItem(
    destination: BottomBarDestination,
    selected: Boolean,
    superuserCount: Int,
    moduleCount: Int,
    showBadge: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // 与角标同源：选中取品牌色，未选中用中性深灰（textDisabled 太淡，壁纸上看不见）
    val accent = KuaiMaoColors.brand
    val color = if (selected) accent else KuaiMaoColors.textMuted
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
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
            fontSize = 9.5.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}

/** 中央电源键：与底栏角标同色（主题 primary）的竖向圆角块。 */
@Composable
private fun KuaiMaoCenterButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val accent = KuaiMaoColors.brand
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
            tint = onBadgeInk(),
            modifier = Modifier.size(21.dp),
        )
    }
}
