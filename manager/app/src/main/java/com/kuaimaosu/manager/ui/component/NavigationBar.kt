package com.kuaimaosu.manager.ui.activity.component

import android.annotation.SuppressLint
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Offset
import com.kuaimaosu.manager.ui.theme.CapsuleEffect
import com.kuaimaosu.manager.ui.theme.CardConfig
import kotlin.math.absoluteValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.ui.component.FloatingBottomBar
import com.kuaimaosu.manager.ui.component.FloatingBottomBarItem
import com.kuaimaosu.manager.ui.screen.BottomBarDestination
import com.kuaimaosu.manager.ui.theme.BackgroundEffect
import com.kuaimaosu.manager.ui.theme.BottomBarStyle
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoDimens
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.theme.barSurfaceColor
import com.kuaimaosu.manager.ui.theme.blurEffect
import com.kuaimaosu.manager.ui.theme.renderBackgroundBlur
import com.kuaimaosu.manager.ui.theme.isInDarkTheme
import com.kuaimaosu.manager.ui.util.LocalBlurState
import com.kuaimaosu.manager.ui.util.LocalHandlePageChange
import com.kuaimaosu.manager.ui.util.LocalPagerState
import com.kuaimaosu.manager.ui.util.LocalSelectedPage
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
                    // 胶囊的左右留白不用 dp 定死：改成按屏幕宽度取比例（见下方 0.72f），
                    // 否则 16dp 在 400dp 级宽屏上只占 8%，整条会拉成一长条。
                    if (style == BottomBarStyle.CAPSULE) {
                        Modifier.padding(
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
                BottomBarStyle.CAPSULE -> Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    KuaiMaoLiquidBottomBar(
                        // 胶囊宽度取屏宽的 72%（对齐原版 BakaSU 的比例），
                        // 大屏/平板再用 360dp 封顶，避免四格被拉得过散。
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .widthIn(max = 360.dp),
                        destinations = destinations,
                        selectedIndex = pagerState.currentPage,
                        onSelected = handlePageChange,
                        superuserCount = superuserCount,
                        moduleCount = moduleCount,
                        showBadge = showNavigationBarBadge,
                        // 只有「开关打开」且「真的拿到 backdrop」时才走玻璃。
                        // 设备不支持 RenderEffect 或 backdrop 为空时退回实心胶囊，
                        // 否则采样到的是空图层，玻璃会画成一块黑。
                        isBlurEnabled = themeConfig.isEnableBlur && LocalBlurState.current != null,
                    )
                }

                // 侧边栏在 MainScreen 里走 rail 布局，这里只作为兜底。
                BottomBarStyle.TRADITIONAL,
                BottomBarStyle.RAIL,
                -> KuaiMaoTraditionalBottomBar(
                    destinations = destinations,
                    selectedIndex = pagerState.currentPage,
                    onSelected = handlePageChange,
                    superuserCount = superuserCount,
                    moduleCount = moduleCount,
                    showBadge = showNavigationBarBadge,
                )
            }
        }
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

/** 侧边栏：悬浮胶囊 rail，圆形按钮竖排（对齐 FoxSu 的侧边样式）。 */
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
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val wallpaperMode = cardConfig.isCustomBackgroundEnabled
    val blurEnabled = themeConfig.isEnableBlur
    val effect = themeConfig.capsuleEffect
    val hasBackdrop = wallpaperMode || themeConfig.backgroundEffect != BackgroundEffect.NONE
    // 膜底色跟顶栏 / 底栏共用 barSurfaceColor()：暗壁纸或夜间配深膜、亮底配浅膜。
    // 之前这里写死 canvasCardSolid，开着暗色壁纸时侧边栏就是一条惨白的实心柱，
    // 亮色壁纸上又因为不透明而像贴了块瓷砖。
    val surface = barSurfaceColor()
    val darkSurface = surface.luminance() < 0.5f
    val railShape = RoundedCornerShape(percent = 50)
    Column(
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
            )
            .padding(horizontal = 10.dp, vertical = 16.dp)
            // 胶囊宽度写死：选中圆 48dp、未选中 44dp，若让外框跟着最宽的那颗走，
            // 切页时整条 rail 会忽宽忽窄，右侧内容跟着左右抖 4dp。
            .width(56.dp)
            // 阴影放在 clip 之前：先画投影，再裁形状，否则投影会被自己裁掉。
            .shadow(elevation = 14.dp, shape = railShape, clip = false)
            .clip(railShape)
            .then(
                when {
                    // 有壁纸且选了「半透明」档：按定义不做模糊，只留一层薄膜。
                    wallpaperMode && blurEnabled && effect == CapsuleEffect.TRANSLUCENT ->
                        Modifier.background(
                            surface.copy(
                                alpha = (cardConfig.cardAlpha * 0.62f).coerceAtLeast(0.30f),
                            ),
                        )

                    // 壁纸 / 动态背景：跟底栏同一套磨砂。侧边栏上没有文字，
                    // 膜可以比底栏薄一档，让壁纸透出一点，边界仍然清楚。
                    hasBackdrop -> Modifier
                        .background(surface.copy(alpha = 0.58f))
                        .renderBackgroundBlur(
                            tintColor = surface,
                            tintAlpha = if (effect == CapsuleEffect.FROSTED) 0.90f else 0.80f,
                        )

                    else -> Modifier.background(surface)
                },
            )
            // 内边距必须留够 48dp：选中圆是 48dp，若内容区只有 44dp，
            // Box 会被父约束压成 44×48 —— 画出来就是一颗竖椭圆。
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        destinations.forEachIndexed { index, destination ->
            NavigationRailItem(
                isSelected = index == selectedIndex,
                destination = destination,
                onClick = { onSelected(index) },
                superuserCount = superuserCount,
                moduleCount = moduleCount,
                showNavigationBarBadge = showBadge,
                darkSurface = darkSurface,
            )
        }
    }
}

/**
 * 侧边栏里的一格：正圆。
 * 选中态用品牌色填充 + 反色图标，尺寸略放大；未选中是浅色圆底 + 次级墨色图标。
 * 圆形底本身充当「方块背景」——不再是圆角方片，和截图里的圆钮一致。
 * 不做文字标签——侧边栏宽度只有 60dp 左右，塞字会把圆撑变形。
 */
@Composable
private fun NavigationRailItem(
    isSelected: Boolean,
    destination: BottomBarDestination,
    onClick: () -> Unit,
    superuserCount: Int,
    moduleCount: Int,
    showNavigationBarBadge: Boolean,
    darkSurface: Boolean,
) {
    val accent = KuaiMaoColors.brand
    val shape = CircleShape
    // 夜间品牌色本身接近白色（#F2F2F7），再配白图标就是「白底白图」，整整一格看不见。
    // 选中圆的前景统一按底色亮度反推：白底给近黑，深底给白。
    val selectedBg = if (darkSurface && accent.luminance() < 0.5f) {
        // 暗膜上深灰圆几乎和膜同色，提亮一档才看得出是个圆。
        lerp(accent, Color.White, 0.30f)
    } else {
        accent
    }
    // 未选中的圆：夜间 canvasField 与卡面同为 #1C1C1C，圆直接消失；
    // 暗膜上改用 9% 白的玻璃圆，浅膜上仍用 field 底色。
    val idleBg = if (darkSurface) {
        Color.White.copy(alpha = 0.09f)
    } else {
        KuaiMaoColors.canvasFieldSolid
    }
    val idleInk = if (darkSurface) Color(0xFFE8E8EC) else KuaiMaoColors.textSec
    val background = if (isSelected) selectedBg else idleBg
    val foreground = if (isSelected) onAccentInk(selectedBg) else idleInk
    Box(
        modifier = Modifier
            .size(if (isSelected) 48.dp else 44.dp)
            .clip(shape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
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
            Icon(
                imageVector = if (isSelected) {
                    destination.iconSelected
                } else {
                    destination.iconNotSelected
                },
                contentDescription = stringResource(destination.label),
                tint = foreground,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * 强调色底上的前景色：深底配白字，亮底配近黑字。
 * 只看亮度，避免"白底白字"。
 */
@Composable
@ReadOnlyComposable
private fun onBadgeInk(): Color = onAccentInk(KuaiMaoColors.brand)

/** 同上，但允许调用方传入实际用到的底色（侧边栏选中圆可能被提亮过）。 */
@Composable
@ReadOnlyComposable
private fun onAccentInk(accent: Color): Color =
    if (accent.luminance() < 0.55f) Color.White else Color(0xFF101318)

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


/**
 * 传统底栏：贴底全宽、顶部一条发丝线，选中项带胶囊底色。
 * 与胶囊栏的区别是“不浮起来”：没有外边距与圆角，背景一直铺到导航条底部。
 */
@Composable
private fun KuaiMaoTraditionalBottomBar(
    destinations: List<BottomBarDestination>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
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

/**
 * 胶囊底栏：直接用仓库自带的 iOS 液态玻璃实现（miuix blur + lens 折射 + vibrancy + 随重力偏移的高光）。
 *
 * 不要再改回 Canvas 手绘：折射率、色散、边缘高光和拖拽时的液体拉伸都在 [FloatingBottomBar] 里，
 * 手绘版本再怎么调参数都不会是同一个东西。
 */
@Composable
private fun KuaiMaoLiquidBottomBar(
    modifier: Modifier = Modifier,
    destinations: List<BottomBarDestination>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    superuserCount: Int,
    moduleCount: Int,
    showBadge: Boolean,
    isBlurEnabled: Boolean,
) {
    FloatingBottomBar(
        modifier = modifier,
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        tabsCount = destinations.size,
        isBlurEnabled = isBlurEnabled,
    ) { activate ->
        destinations.forEachIndexed { index, destination ->
            FloatingBottomBarItem(
                selected = index == selectedIndex,
                onClick = { activate(index) },
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
