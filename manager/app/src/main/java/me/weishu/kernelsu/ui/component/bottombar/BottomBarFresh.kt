package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.component.LocalAppBackdrop
import me.weishu.kernelsu.ui.component.freshCardBorder
import me.weishu.kernelsu.ui.component.freshCardColor
import me.weishu.kernelsu.ui.component.hasWallpaperBackdrop
import me.weishu.kernelsu.ui.component.isNightScheme
import me.weishu.kernelsu.ui.component.liquid.lens
import me.weishu.kernelsu.ui.component.liquid.vibrancy
import me.weishu.kernelsu.ui.theme.LaunchTint
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop

private data class FreshNavItem(
    val label: Int,
    val selectedIcon: Int,
    val unselectedIcon: Int,
)

/** 底栏胶囊宽度上限：宽屏上不再拉满，两侧留出空气。 */
private val BarMaxWidth = 340.dp

/** 液态高光手柄：比一个 tab 窄一圈，像一颗浮在玻璃上的水滴。 */
private val HandleWidth = 34.dp
private val HandleHeight = 40.dp

/** 点击时的玻璃透镜：直径、扫过的距离。 */
private val LensSize = 46.dp

/** 胶囊外侧留白，决定底栏的「长度」。 */
private val CapsuleSidePadding = 24.dp

@Composable
fun BottomBarFresh(navigationBadge: NavigationBadgeState) {
    if (!Natives.isFullFeatured()) return

    val mainPagerState = LocalMainPagerState.current
    val custom = CustomizationStore.current

    val items = listOf(
        FreshNavItem(R.string.home, R.drawable.ic_nav_home_fill, R.drawable.ic_nav_home),
        FreshNavItem(R.string.superuser, R.drawable.ic_nav_shield_fill, R.drawable.ic_nav_shield),
        FreshNavItem(R.string.module, R.drawable.ic_nav_module_fill, R.drawable.ic_nav_module),
        FreshNavItem(R.string.settings, R.drawable.ic_nav_settings_fill, R.drawable.ic_nav_settings),
    )

    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val drawerStyle = custom.bottomBarStyle == 1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = CapsuleSidePadding,
                end = CapsuleSidePadding,
                bottom = navigationBarPadding + 10.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // 胶囊形态一律用全圆角（高度的一半），无论设置里的圆角是多少，出来的就是参考图那种药丸
        val capsule = !drawerStyle
        val barShape = if (capsule) RoundedCornerShape(percent = 50) else RoundedCornerShape(custom.bottomBarRadius.dp)
        // 视频样式：半透明悬浮条（自定义卡片色也保持通透）；沉浸光感更透并叠柔光
        val immersive = custom.bottomBarImmersive
        val nightBar = isNightScheme()
        // 用户设置的底栏不透明度（默认 1f = 完全不透明）
        val userAlpha = custom.bottomBarAlpha
        // 是否铺了壁纸/玻璃背景：决定底栏是白胶囊还是磨砂玻璃条
        val onBackdrop = hasWallpaperBackdrop()
        // 底栏液态玻璃（设置项，默认关闭）：开启后整条底栏换成实时折射玻璃 + 可拖动高光
        val glassBackdrop = LocalAppBackdrop.current?.takeIf { custom.bottomBarLiquidGlass }
        val glass = glassBackdrop != null

        val barColor = when {
            // 玻璃模式：统一深色玻璃条，和卡片同一套材质
            custom.cardGlass -> Color(0xFF101014).copy(alpha = 0.46f * userAlpha)
            nightBar -> Color(if (immersive) 0xFF17181A else 0xFF1B1C1E)
                .copy(alpha = (if (immersive) 0.86f else 1f) * userAlpha)
            // 悬浮胶囊：纯色画布上用白胶囊；铺了壁纸才转成半透明磨砂
            capsule -> if (onBackdrop) {
                Color(0xFF20242A).copy(alpha = (if (immersive) 0.34f else 0.46f) * userAlpha)
            } else {
                Color.White.copy(alpha = (if (immersive) 0.82f else 1f) * userAlpha)
            }
            immersive -> freshCardColor().copy(alpha = 0.66f * userAlpha)
            else -> freshCardColor().copy(alpha = userAlpha)
        }

        // ---- 液态高光手柄的坐标系统 ----
        val density = LocalDensity.current
        val horizontalPaddingDp = if (capsule) 10.dp else 8.dp
        val horizontalPaddingPx = with(density) { horizontalPaddingDp.toPx() }
        val handleWidthPx = with(density) { HandleWidth.toPx() }
        var barWidthPx by remember { mutableIntStateOf(0) }
        val tabWidthPx = if (barWidthPx <= 0) {
            0f
        } else {
            ((barWidthPx - horizontalPaddingPx * 2f) / items.size).coerceAtLeast(1f)
        }

        // handlePos：手柄当前落在第几个 tab 上（0..size-1，拖动时是小数）
        val handlePos = remember { Animatable(mainPagerState.selectedPage.toFloat()) }
        var dragging by remember { mutableFloatStateOf(0f) }
        // 点击切换 / 左右滑页切换：手柄弹到新 tab，并先“拉长”再回弹（点击反馈的液态感）
        val stretch = remember { Animatable(0f) }
        LaunchedEffect(mainPagerState.selectedPage) {
            launch {
                if (dragging == 0f) {
                    handlePos.animateTo(
                        targetValue = mainPagerState.selectedPage.toFloat(),
                        animationSpec = spring(dampingRatio = 0.58f, stiffness = 300f),
                    )
                }
            }
            stretch.snapTo(1f)
            stretch.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = 420f))
        }

        // ---- 点击透镜：点在哪个 tab 上，玻璃球就在那个 tab 里扫一下再收掉 ----
        var lensIndex by remember { mutableIntStateOf(-1) }
        var lensToken by remember { mutableIntStateOf(0) }
        val lensTravel = remember { Animatable(0f) }
        val lensFade = remember { Animatable(0f) }
        val lensVisible = lensIndex >= 0
        LaunchedEffect(lensToken) {
            if (lensIndex >= 0) {
                launch {
                    lensTravel.snapTo(0f)
                    lensTravel.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(260, easing = LinearOutSlowInEasing),
                    )
                }
                lensFade.snapTo(1f)
                lensFade.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(120, easing = FastOutLinearInEasing),
                )
            }
        }
        val lensSizePx = with(density) { LensSize.toPx() }

        val scope = rememberCoroutineScope()
        val barModifier = (if (capsule) Modifier.widthIn(max = BarMaxWidth) else Modifier.fillMaxWidth())
            .then(
                // 液态玻璃的填充来自折射层，Surface 的投影要自己补一层，否则整条会“飘”起来
                if (glass) {
                    Modifier.dropShadow(
                        shape = barShape,
                        shadow = Shadow(
                            radius = 14.dp,
                            color = Color.Black,
                            alpha = if (nightBar) 0.30f else 0.12f,
                        ),
                    )
                } else {
                    Modifier
                }
            )

        Surface(
            shape = barShape,
            color = if (glass) Color.Transparent else barColor,
            border = freshCardBorder(),
            shadowElevation = when {
                glass -> 0.dp
                custom.cardGlass -> 0.dp
                capsule -> 10.dp
                else -> 12.dp
            },
            modifier = barModifier,
        ) {
            Box(
                modifier = Modifier
                    .onSizeChanged { barWidthPx = it.width }
                    .then(
                        // 液态玻璃专属手势：在底栏上左右拖，手柄跟手；松手吸附到最近的 tab。
                        // 轻点仍由各 tab 自己处理，不影响原有点击。
                        if (glass && tabWidthPx > 0f) {
                            Modifier.pointerInput(tabWidthPx, items.size) {
                                fun indexAt(x: Float): Float =
                                    ((x - horizontalPaddingPx) / tabWidthPx - 0.5f)
                                        .coerceIn(0f, (items.size - 1).toFloat())

                                detectHorizontalDragGestures(
                                    onDragStart = { down: Offset ->
                                        dragging = 1f
                                        scope.launch { handlePos.snapTo(indexAt(down.x)) }
                                    },
                                    onDragCancel = {
                                        dragging = 0f
                                        scope.launch {
                                            handlePos.animateTo(
                                                mainPagerState.selectedPage.toFloat(),
                                                spring(dampingRatio = 0.6f, stiffness = 320f),
                                            )
                                        }
                                    },
                                    onDragEnd = {
                                        dragging = 0f
                                        val target = handlePos.value
                                            .roundToInt()
                                            .coerceIn(0, items.size - 1)
                                        scope.launch {
                                            handlePos.animateTo(
                                                target.toFloat(),
                                                spring(dampingRatio = 0.6f, stiffness = 320f),
                                            )
                                        }
                                        if (target != mainPagerState.selectedPage) {
                                            mainPagerState.animateToPage(target)
                                        }
                                    },
                                    onHorizontalDrag = { change: PointerInputChange, _: Float ->
                                        val target = indexAt(change.position.x)
                                        scope.launch { handlePos.snapTo(target) }
                                    },
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                // 液态玻璃层：采样页面内容做模糊 + 色彩提亮 + 边缘折射，再压一层极薄色调
                if (glassBackdrop != null) {
                    val glassTint = if (custom.cardGlass || nightBar) {
                        Color.Black.copy(alpha = 0.06f)
                    } else if (onBackdrop) {
                        Color.Black.copy(alpha = 0.05f)
                    } else {
                        Color.White.copy(alpha = 0.04f)
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBackdrop(
                                backdrop = glassBackdrop,
                                shape = { barShape },
                                effects = {
                                    vibrancy()
                                    blur(6.dp.toPx(), 6.dp.toPx())
                                    lens(
                                        refractionHeight = 20.dp.toPx(),
                                        refractionAmount = 26.dp.toPx(),
                                    )
                                },
                                onDrawSurface = { drawRect(glassTint) },
                            )
                    )
                }

                // 磨砂高光：玻璃条 / 壁纸上的胶囊才加薄白反光，白胶囊保持干净
                if (!glass &&
                    (immersive || custom.cardGlass || (capsule && onBackdrop)) && !nightBar
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.White.copy(alpha = if (custom.cardGlass) 0.10f else 0.12f),
                                    0.55f to Color.White.copy(alpha = 0.03f),
                                    1f to Color.Transparent,
                                )
                            ),
                    )
                }

                // 可左右拖动的液态高光：跟手滑、拖动时变亮胀大、切 tab 时拉长回弹
                if (glass && tabWidthPx > 0f) {
                    val lift = dragging * 0.5f + stretch.value * 0.5f
                    Box(
                        modifier = Modifier
                            .offset(
                                x = ((horizontalPaddingPx +
                                    tabWidthPx * (handlePos.value + 0.5f) -
                                    handleWidthPx / 2f) / density.density).dp,
                                y = 6.dp,
                            )
                            .width(HandleWidth)
                            .height(HandleHeight)
                            .graphicsLayer {
                                scaleX = 1f + dragging * 0.12f + stretch.value * 0.22f
                                scaleY = 1f + dragging * 0.10f + stretch.value * 0.10f
                            }
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.20f + 0.14f * lift),
                                        Color.White.copy(alpha = 0.06f + 0.10f * lift),
                                    )
                                ),
                                RoundedCornerShape(percent = 50),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        // 中间那颗小胶囊：暗示“可以左右拖”
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(4.dp)
                                .background(
                                    Color.White.copy(alpha = 0.42f + 0.34f * dragging),
                                    RoundedCornerShape(50),
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPaddingDp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEachIndexed { index, item ->
                        val selected = mainPagerState.selectedPage == index
                        val onClick = {
                            if (!selected) {
                                lensIndex = index
                                lensToken++
                                mainPagerState.animateToPage(index)
                            }
                        }
                        if (drawerStyle) {
                            DrawerNavItem(
                                item = item,
                                selected = selected,
                                badge = badgeFor(index, navigationBadge),
                                accent = LaunchTint.navColor(index),
                                onClick = onClick,
                            )
                        } else {
                            FreshNavItemView(
                                item = item,
                                selected = selected,
                                badge = badgeFor(index, navigationBadge),
                                accent = LaunchTint.navColor(index),
                                onClick = onClick,
                            )
                        }
                    }
                }

                // 玻璃透镜：等 tab 测完宽度后再画，保证能落在正确的 tab 中心
                if (lensVisible && tabWidthPx > 0f && lensFade.value > 0.01f) {
                    val center = horizontalPaddingPx + tabWidthPx * (lensIndex + 0.5f)
                    val sweep = tabWidthPx * 0.22f
                    val lensBrush = if (nightBar || custom.cardGlass) {
                        Brush.radialGradient(
                            0f to Color.White.copy(alpha = 0.20f),
                            0.62f to Color.White.copy(alpha = 0.06f),
                            1f to Color.White.copy(alpha = 0f),
                        )
                    } else {
                        Brush.radialGradient(
                            0f to Color.White.copy(alpha = 0.95f),
                            0.55f to Color(0xFFF3F6FD).copy(alpha = 0.55f),
                            0.88f to Color(0xFFDCE4F5).copy(alpha = 0.30f),
                            1f to Color.Transparent,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(
                                x = ((center + (lensTravel.value - 0.5f) * 2f * sweep -
                                    lensSizePx / 2f) / density.density).dp,
                                y = 3.dp,
                            )
                            .size(LensSize)
                            .graphicsLayer {
                                alpha = lensFade.value
                                // 起手略小，扫到底时胀到最大，像一颗被推过镜面的水滴
                                val s = 0.82f + 0.18f * lensTravel.value + 0.06f * lensFade.value
                                scaleX = s
                                scaleY = s
                            }
                            .clip(RoundedCornerShape(percent = 50))
                            .background(lensBrush),
                    )
                }
            }
        }
    }
}

/**
 * Drawer style tab: the selected tab expands into a pill that shows icon + label, the others
 * collapse to bare icons. Width, colour and label all animate, so switching looks like a drawer
 * sliding open.
 */
@Composable
private fun DrawerNavItem(
    item: FreshNavItem,
    selected: Boolean,
    badge: NavBadge?,
    accent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val width by animateDpAsState(
        targetValue = when {
            pressed -> 116.dp
            selected -> 124.dp
            else -> 56.dp
        },
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "drawerWidth",
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(durationMillis = 240),
        label = "drawerPill",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) accent else accent.copy(alpha = 0.78f),
        animationSpec = tween(durationMillis = 240),
        label = "drawerContent",
    )

    Row(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .width(width)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(pillColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconModifier = Modifier.size(22.dp)
        if (badge != null) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = if (badge.tone == BadgeTone.Alert) {
                            MaterialTheme.colorScheme.error
                        } else {
                            accent
                        },
                        contentColor = Color.White,
                    ) {
                        Text(badge.count.toString())
                    }
                }
            ) {
                Icon(
                    painter = painterResource(if (selected) item.selectedIcon else item.unselectedIcon),
                    contentDescription = stringResource(item.label),
                    tint = contentColor,
                    modifier = iconModifier,
                )
            }
        } else {
            Icon(
                painter = painterResource(if (selected) item.selectedIcon else item.unselectedIcon),
                contentDescription = stringResource(item.label),
                tint = contentColor,
                modifier = iconModifier,
            )
        }

        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(animationSpec = tween(180)) +
                expandHorizontally(
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
                    expandFrom = Alignment.Start,
                ),
            exit = fadeOut(animationSpec = tween(90)) +
                shrinkHorizontally(animationSpec = tween(120), shrinkTowards = Alignment.Start),
        ) {
            Text(
                text = stringResource(item.label),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/**
 * 胶囊 tab：静息态只有图标 + 文字，没有任何缩放/光晕动画。
 * 点击反馈交给底栏上那颗扫过的玻璃透镜，这里只做颜色过渡。
 */
@Composable
private fun FreshNavItemView(
    item: FreshNavItem,
    selected: Boolean,
    badge: NavBadge?,
    accent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val contentColor by animateColorAsState(
        targetValue = if (selected) accent else accent.copy(alpha = 0.78f),
        animationSpec = tween(durationMillis = 220),
        label = "navContent",
    )

    Column(
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val iconModifier = Modifier.size(23.dp)
        if (badge != null) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = if (badge.tone == BadgeTone.Alert) {
                            MaterialTheme.colorScheme.error
                        } else {
                            accent
                        },
                        contentColor = Color.White,
                    ) {
                        Text(badge.count.toString())
                    }
                }
            ) {
                Icon(
                    painter = painterResource(if (selected) item.selectedIcon else item.unselectedIcon),
                    contentDescription = stringResource(item.label),
                    tint = contentColor,
                    modifier = iconModifier,
                )
            }
        } else {
            Icon(
                painter = painterResource(if (selected) item.selectedIcon else item.unselectedIcon),
                contentDescription = stringResource(item.label),
                tint = contentColor,
                modifier = iconModifier,
            )
        }
        Text(
            text = stringResource(item.label),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
