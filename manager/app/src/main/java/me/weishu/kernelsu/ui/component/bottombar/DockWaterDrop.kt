package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import me.weishu.kernelsu.data.settings.CustomizationStore

/**
 * 底栏的**滑动指示器**：选中项背后那块色块，从一个格子「走」到下一个格子。
 *
 * 这是底栏的基础行为，不是特效开关——Miuix 原版和 Material3 的 NavigationBar 都是这样：
 * 切换时色块平移过去，而不是在旧位置淡出、新位置淡入。之前 FoxSu 的胶囊是后者，
 * 所以看起来「没有那个效果」。
 *
 * 位置来源只有一处——**pager 自己的连续滚动位置**：
 *
 * ```
 * pageFloat = pagerState.currentPage + pagerState.currentPageOffsetFraction
 * ```
 *
 * - 手指拖动页面时 `currentPageOffsetFraction` 实时变化，色块直接 `snapTo` 跟手，
 *   拖到哪它就在哪，不会滞后半格；
 * - 松手后 pager 用自己的 spring 收尾，色块跟着 pager 的轨迹走完，
 *   末尾再用一条 stiffness 260 的弹簧对齐到整格（≈400ms，无回弹）；
 * - 点图标切页时 pager 自己会做滚动动画，于是色块自动获得同一段动画，
 *   不需要在两处各写一套时长，也不会出现「点了图标但色块先跑、页面后到」。
 *
 * 「水滴」（[CustomizationStore.dockWaterDrop]）叠加的只是液体细节：
 * 切换瞬间横向撑开一个鼓包、高度压扁一点、落位时新图标先缩后弹。
 * 关掉水滴，滑动本身仍然在。
 */
@Composable
internal fun DockSlideIndicator(
    pagerState: PagerState,
    count: Int,
    slotW: Dp,
    slotH: Dp,
    gap: Dp,
    plateW: Dp,
    plateH: Dp,
    shape: Shape,
    brush: Brush,
    highlight: Color,
    modifier: Modifier = Modifier,
) {
    if (count <= 1) return

    val waterDrop = CustomizationStore.current.dockWaterDrop
    val strideDp = (slotW + gap).value
    val insetX = ((slotW - plateW) / 2).value
    val insetY = ((slotH - plateH) / 2).value

    // 位置用 dp 值存（不是 px）：密度变化时不用重新换算，动画不会因为单位切换而跳
    val x = remember { Animatable(pagerState.currentPage * strideDp) }

    // 切换次数：只在水滴模式下用来触发鼓包。
    // 用 currentPage 的变化计数，而不是「位置差」——点击切页时 x 已经贴着 pager 走到位了，
    // 位移差永远接近 0，靠它计数会漏掉每一次点击。
    var moves by remember { mutableIntStateOf(0) }
    var lastPage by remember { mutableIntStateOf(pagerState.currentPage) }

    LaunchedEffect(pagerState, strideDp, count) {
        snapshotFlow {
            Triple(
                pagerState.isScrollInProgress,
                pagerState.currentPage,
                pagerState.currentPageOffsetFraction,
            )
        }.collectLatest { (scrolling, page, fraction) ->
            if (page != lastPage) {
                lastPage = page
                moves++
            }
            val max = (count - 1).toFloat()
            if (scrolling) {
                // 拖动 / pager 正在自己滚：严格跟手，一格都不许滞后
                x.snapTo((page + fraction).coerceIn(0f, max) * strideDp)
            } else {
                x.animateTo(
                    page.coerceIn(0, count - 1) * strideDp,
                    spring(dampingRatio = 1f, stiffness = 260f),
                )
            }
        }
    }

    // ── 水滴：鼓包 + 压扁 ──
    val stretch = remember { Animatable(0f) }
    var seenMoves by remember { mutableIntStateOf(0) }
    LaunchedEffect(moves) {
        if (!waterDrop || moves == seenMoves) {
            seenMoves = moves
            return@LaunchedEffect
        }
        seenMoves = moves
        stretch.snapTo(1f)
        stretch.animateTo(0f, spring(dampingRatio = 0.50f, stiffness = 190f))
    }
    val stretchAmt = if (waterDrop) stretch.value.coerceIn(0f, 1.3f) else 0f

    val squash by animateFloatAsState(
        targetValue = 1f - 0.10f * stretchAmt.coerceAtMost(1f),
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "dock-slide-squash",
    )

    // 鼓包只在左右两侧长出去，不撑高
    val grow = strideDp * 0.16f * stretchAmt
    val left = x.value + insetX - grow / 2f
    val width = (plateW.value + grow).dp

    Box(modifier) {
        Box(
            modifier = Modifier
                .offset(x = left.dp, y = insetY.dp)
                .width(width)
                .height(plateH)
                .graphicsLayer { scaleY = squash }
                .clip(shape)
                .background(brush),
            contentAlignment = Alignment.Center,
        ) {
            // 顶部一道极细高光：反射亮边，鼓包时更明显
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .background(highlight.copy(alpha = 0.20f + 0.18f * stretchAmt.coerceAtMost(1f))),
            )
        }
    }
}

/**
 * 选中项图标的水滴落点回弹。
 *
 * 新选中项先被「砸」到 0.84 再带超调弹回；旧项被吸走一点（0.92）再回位，
 * 避免它看起来完全无动于衷。返回值直接喂给 `graphicsLayer.scaleX/scaleY`。
 *
 * 未开启水滴时恒为 1f，调用方无需分支。
 */
@Composable
internal fun rememberDropIconScale(selected: Boolean): Float {
    val on = CustomizationStore.current.dockWaterDrop
    val impact = remember { Animatable(1f) }
    LaunchedEffect(selected, on) {
        if (!on) {
            impact.snapTo(1f)
            return@LaunchedEffect
        }
        if (selected) {
            // 先缩后弹：0.84 → 1 带一次明显的超调（阻尼 0.45），400ms 收尾
            impact.snapTo(0.84f)
            impact.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 230f))
        } else {
            impact.snapTo(1f)
        }
    }
    return impact.value
}

/**
 * 滑动指示器压住图标时，选中项的前景色。
 *
 * 色块是一整块渐变实底，原来「选中 = 强调色」的文字/图标会陷进去。
 * 亮色主题给白字；暗色主题的强调色本身偏亮，给一层近黑墨色才够对比。
 * 返回 null 表示调用方该用自己原来的取色。
 */
@Composable
internal fun slidePlateInk(dark: Boolean): Color = if (dark) Color(0xFF20182E) else Color.White
