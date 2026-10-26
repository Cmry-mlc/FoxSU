package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * 按下反馈：整条胶囊朝按下的那一侧收一收，纵向也跟着压扁一点点，松手弹回。
 * 数值来自参考视频逐帧测量（宽度 ~249px → ~237px，约 5%）。
 */
private const val PressShrinkX = 0.95f
private const val PressShrinkY = 0.985f

/** 收缩约 230ms 到位；回弹用带一点过冲的减速曲线，约 620ms 收干净。 */
private val PressInEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private val PressOutEasing = CubicBezierEasing(0.16f, 1.1f, 0.3f, 1f)
private const val PressInMillis = 230
private const val PressOutMillis = 620

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

        // ---- 手势坐标系统 ----
        val density = LocalDensity.current
        val horizontalPaddingDp = if (capsule) 10.dp else 8.dp
        val horizontalPaddingPx = with(density) { horizontalPaddingDp.toPx() }
        var barWidthPx by remember { mutableIntStateOf(0) }
        val tabWidthPx = if (barWidthPx <= 0) {
            0f
        } else {
            ((barWidthPx - horizontalPaddingPx * 2f) / items.size).coerceAtLeast(1f)
        }

        // 液态玻璃拖动时用的位置游标（已无滑块，只用于手势吸附）
        val dragPos = remember { Animatable(mainPagerState.selectedPage.toFloat()) }
        var dragging by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(mainPagerState.selectedPage) {
            if (dragging == 0f) dragPos.snapTo(mainPagerState.selectedPage.toFloat())
        }

        // ---- 按下反馈：整条胶囊朝按下的那一侧收缩 ----
        // pressBias: -1 = 按在左侧（重心左移），+1 = 按在右侧
        var pressedIndex by remember { mutableIntStateOf(-1) }
        var pressBias by remember { mutableFloatStateOf(0f) }
        val press = remember { Animatable(0f) }
        LaunchedEffect(pressedIndex) {
            if (pressedIndex >= 0) {
                press.animateTo(1f, tween(PressInMillis, easing = PressInEasing))
            } else {
                press.animateTo(0f, tween(PressOutMillis, easing = PressOutEasing))
            }
        }

        val scope = rememberCoroutineScope()
        // 液态玻璃不再额外补投影 / 白色蒙版：玻璃层本身就有折射和边缘高光，
        // 再叠一层黑白会在深色壁纸上糊出一圈白雾或黑边。
        val barModifier = if (capsule) Modifier.widthIn(max = BarMaxWidth) else Modifier.fillMaxWidth()

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
                    .graphicsLayer {
                        // 横向收缩 + 轻微纵向压扁；按下时朝被按住的那一侧让一点
                        val p = press.value
                        scaleX = 1f - (1f - PressShrinkX) * p
                        scaleY = 1f - (1f - PressShrinkY) * p
                        translationX = pressBias * 3.dp.toPx() * p
                    }
                    .then(
                        // 液态玻璃专属手势：在底栏上左右拖，松手吸附到最近的 tab
                        if (glass && tabWidthPx > 0f) {
                            Modifier.pointerInput(tabWidthPx, items.size) {
                                fun indexAt(x: Float): Float =
                                    ((x - horizontalPaddingPx) / tabWidthPx - 0.5f)
                                        .coerceIn(0f, (items.size - 1).toFloat())

                                detectHorizontalDragGestures(
                                    onDragStart = { down: Offset ->
                                        dragging = 1f
                                        scope.launch { dragPos.snapTo(indexAt(down.x)) }
                                    },
                                    onDragCancel = {
                                        dragging = 0f
                                        scope.launch {
                                            dragPos.animateTo(
                                                mainPagerState.selectedPage.toFloat(),
                                                spring(dampingRatio = 0.6f, stiffness = 320f),
                                            )
                                        }
                                    },
                                    onDragEnd = {
                                        dragging = 0f
                                        val target = dragPos.value
                                            .roundToInt()
                                            .coerceIn(0, items.size - 1)
                                        scope.launch {
                                            dragPos.animateTo(
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
                                        scope.launch { dragPos.snapTo(target) }
                                    },
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                // 液态玻璃层：采样页面内容做模糊 + 色彩提亮 + 边缘折射；
                // 不压任何白色/黑色蒙版，避免在深色壁纸上糊出一层白雾
                if (glassBackdrop != null) {
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
                            )
                    )
                }

                // 磨砂高光：只有磨砂底栏才加，纯白胶囊保持干净
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

                // 图标严格等分：每个 tab 占同样宽度并居中，所以开关液态玻璃前后图标中心不变
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
                                accent = LaunchTint.navColor(index),
                                onClick = {
                                    if (!selected) mainPagerState.animateToPage(index)
                                },
                            )
                        } else {
                            FreshNavItemView(
                                modifier = Modifier.weight(1f),
                                item = item,
                                selected = selected,
                                badge = badgeFor(index, navigationBadge),
                                accent = LaunchTint.navColor(index),
                                onClick = {
                                    if (!selected) mainPagerState.animateToPage(index)
                                },
                                onPressChange = { down ->
                                    if (down) {
                                        pressedIndex = index
                                        // 以底栏中点为轴：按左边重心左移，按右边右移
                                        val center = items.size / 2f - 0.5f
                                        pressBias = ((index - center) / center).coerceIn(-1f, 1f)
                                    } else {
                                        pressedIndex = -1
                                    }
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
 * 胶囊 tab：只有一个图标 + 一行文字，没有滑块、没有按下色块、没有光晕。
 * 按下反馈全部交给外层：整条胶囊朝按下的那一侧收缩，松手弹回。
 */
@Composable
private fun FreshNavItemView(
    modifier: Modifier = Modifier,
    item: FreshNavItem,
    selected: Boolean,
    badge: NavBadge?,
    accent: Color,
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

    // 等分宽度：保证图标中心正好落在底栏 tab 中心（液态玻璃下的对齐就靠这个）
    Column(
        modifier = modifier
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
