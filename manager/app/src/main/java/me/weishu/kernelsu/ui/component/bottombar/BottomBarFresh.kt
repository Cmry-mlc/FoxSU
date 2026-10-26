package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
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

/** 胶囊外侧留白，决定底栏的「长度」。 */
private val CapsuleSidePadding = 24.dp

/** 选中滑块：横向占满一个 tab，纵向上下各留 3dp。 */
private val PillInset = 3.dp

/** miuix TabRow 用的是 200ms 线性 tween，滑块匀速平移，不做过冲。 */
private const val PillSlideMillis = 200

/** 按下时的下沉量，松手用弹簧推回。 */
private const val PressDip = 0.94f

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
        // 胶囊形态固定成药丸（高度的一半），与参考图一致
        val capsule = !drawerStyle
        val barShape = if (capsule) {
            RoundedCornerShape(percent = 50)
        } else {
            RoundedCornerShape(custom.bottomBarRadius.dp)
        }
        val immersive = custom.bottomBarImmersive
        val nightBar = isNightScheme()
        val userAlpha = custom.bottomBarAlpha
        val onBackdrop = hasWallpaperBackdrop()
        // 底栏液态玻璃（设置项，默认关闭）
        val glassBackdrop = LocalAppBackdrop.current?.takeIf { custom.bottomBarLiquidGlass }
        val glass = glassBackdrop != null

        val barColor = when {
            custom.cardGlass -> Color(0xFF101014).copy(alpha = 0.46f * userAlpha)
            nightBar -> Color(if (immersive) 0xFF17181A else 0xFF1B1C1E)
                .copy(alpha = (if (immersive) 0.86f else 1f) * userAlpha)
            capsule -> if (onBackdrop) {
                Color(0xFF20242A).copy(alpha = (if (immersive) 0.34f else 0.46f) * userAlpha)
            } else {
                Color.White.copy(alpha = (if (immersive) 0.82f else 1f) * userAlpha)
            }
            immersive -> freshCardColor().copy(alpha = 0.66f * userAlpha)
            else -> freshCardColor().copy(alpha = userAlpha)
        }

        // ---- 滑块坐标系统 ----
        val density = LocalDensity.current
        val horizontalPaddingDp = if (capsule) 10.dp else 8.dp
        val horizontalPaddingPx = with(density) { horizontalPaddingDp.toPx() }
        var barWidthPx by remember { mutableIntStateOf(0) }
        val tabWidthPx = if (barWidthPx <= 0) {
            0f
        } else {
            ((barWidthPx - horizontalPaddingPx * 2f) / items.size).coerceAtLeast(1f)
        }
        val tabWidthDp = with(density) { tabWidthPx.toDp() }

        // 滑块位置：跟手拖动时是小数，松手吸附到最近的 tab
        val pillPos = remember { Animatable(mainPagerState.selectedPage.toFloat()) }
        var dragging by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(mainPagerState.selectedPage) {
            if (dragging == 0f) {
                // 与 miuix TabRow 一致的匀速平移
                pillPos.animateTo(
                    targetValue = mainPagerState.selectedPage.toFloat(),
                    animationSpec = tween(durationMillis = PillSlideMillis, easing = LinearEasing),
                )
            }
        }

        // 按下反馈：滑块整条压扁一点，图标沉下去一点，松手弹簧推回
        var pressedSlot by remember { mutableIntStateOf(-1) }
        val pillDip by animateFloatAsState(
            targetValue = if (pressedSlot >= 0) 1f else 0f,
            animationSpec = if (pressedSlot >= 0) {
                tween(durationMillis = 90, easing = FastOutLinearInEasing)
            } else {
                spring(dampingRatio = 0.34f, stiffness = 520f)
            },
            label = "pillDip",
        )
        val iconDip by animateFloatAsState(
            targetValue = if (pressedSlot >= 0) 1f else 0f,
            animationSpec = if (pressedSlot >= 0) {
                tween(durationMillis = 90, easing = FastOutLinearInEasing)
            } else {
                spring(dampingRatio = 0.40f, stiffness = 480f)
            },
            label = "iconDip",
        )

        val accentFor: (Int) -> Color = { index -> LaunchTint.navColor(index) }
        val pillColor = accentFor(mainPagerState.selectedPage).let {
            if (nightBar || custom.cardGlass) it.copy(alpha = 0.20f) else it.copy(alpha = 0.13f)
        }

        val scope = rememberCoroutineScope()
        val barModifier = (if (capsule) Modifier.widthIn(max = BarMaxWidth) else Modifier.fillMaxWidth())
            .then(
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
                        // 液态玻璃专属手势：在底栏上左右拖，滑块跟手；松手吸附到最近的 tab
                        if (glass && tabWidthPx > 0f) {
                            Modifier.pointerInput(tabWidthPx, items.size) {
                                fun indexAt(x: Float): Float =
                                    ((x - horizontalPaddingPx) / tabWidthPx - 0.5f)
                                        .coerceIn(0f, (items.size - 1).toFloat())

                                detectHorizontalDragGestures(
                                    onDragStart = { down: Offset ->
                                        dragging = 1f
                                        scope.launch { pillPos.snapTo(indexAt(down.x)) }
                                    },
                                    onDragCancel = {
                                        dragging = 0f
                                        scope.launch {
                                            pillPos.animateTo(
                                                mainPagerState.selectedPage.toFloat(),
                                                tween(PillSlideMillis, easing = LinearEasing),
                                            )
                                        }
                                    },
                                    onDragEnd = {
                                        dragging = 0f
                                        val target = pillPos.value
                                            .roundToInt()
                                            .coerceIn(0, items.size - 1)
                                        scope.launch {
                                            pillPos.animateTo(
                                                target.toFloat(),
                                                tween(PillSlideMillis, easing = LinearEasing),
                                            )
                                        }
                                        if (target != mainPagerState.selectedPage) {
                                            mainPagerState.animateToPage(target)
                                        }
                                    },
                                    onHorizontalDrag = { change: PointerInputChange, _: Float ->
                                        val target = indexAt(change.position.x)
                                        scope.launch { pillPos.snapTo(target) }
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

                // 磨砂高光：只有磨砂底栏才加，纯白胶囊保持干净（不再有那层白影子）
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

                // 选中滑块：横向一个 tab 宽，纯色淡染，跟着选中项匀速平移
                if (tabWidthPx > 0f) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = ((horizontalPaddingPx + tabWidthPx * pillPos.value) /
                                    density.density).dp,
                                y = PillInset,
                            )
                            .width(tabWidthDp.coerceAtLeast(1.dp))
                            .height(56.dp)
                            .graphicsLayer {
                                // 按下时整条压下去，松手弹簧弹回（不做左右拉伸）
                                scaleX = 1f - (1f - PressDip) * pillDip * 0.55f
                                scaleY = 1f - (1f - PressDip) * pillDip
                            }
                            .background(pillColor, RoundedCornerShape(percent = 50)),
                    )
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
                        if (drawerStyle) {
                            DrawerNavItem(
                                item = item,
                                selected = selected,
                                badge = badgeFor(index, navigationBadge),
                                accent = accentFor(index),
                                onClick = {
                                    if (!selected) mainPagerState.animateToPage(index)
                                },
                            )
                        } else {
                            FreshNavItemView(
                                item = item,
                                selected = selected,
                                badge = badgeFor(index, navigationBadge),
                                accent = accentFor(index),
                                iconDip = iconDip,
                                onClick = {
                                    if (!selected) mainPagerState.animateToPage(index)
                                },
                                onPressChange = { down ->
                                    pressedSlot = if (down) index else -1
                                },
                            )
                        }
                    }
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
 * 胶囊 tab：静息态只有图标 + 文字。按下时图标往下沉一点、略收，松手由外层弹簧推回；
 * 没有放大、没有光晕、没有任何白色影子。
 */
@Composable
private fun FreshNavItemView(
    item: FreshNavItem,
    selected: Boolean,
    badge: NavBadge?,
    accent: Color,
    iconDip: Float,
    onClick: () -> Unit,
    onPressChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(pressed) { onPressChange(pressed) }

    val contentColor by animateColorAsState(
        targetValue = if (selected) accent else accent.copy(alpha = 0.72f),
        animationSpec = tween(durationMillis = 200),
        label = "navContent",
    )

    Column(
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 3.dp)
            .graphicsLayer {
                // 只有「当前按下的这一项」往下沉一点点（y 轴正方向 = 屏幕下方）
                val dip = if (pressed) iconDip else 0f
                translationY = 2.dp.toPx() * dip
                val s = 1f - 0.07f * dip
                scaleX = s
                scaleY = s
            },
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
