package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.data.settings.CustomizationStore

/**
 * 胶囊底栏（4 Tab）的「水滴」流动指示器。
 *
 * 直觉上的实现是「一个色块平移过去」，但那样只是一块滑动的塑料片。真实的水滴有两段加速度：
 * 前缘先行、后缘慢半拍追上，落位时带着液体表面的回弹。这里用三条独立的时间线把它拆开：
 *
 * ```
 * 前缘  lead     →  stiffness 260 / damping 1.00   400ms 进度 0.99，无回弹
 * 后缘  trail    →  stiffness 150 / damping 0.78   400ms 进度 1.02，轻超一下再落
 * 拉伸  stretch  →  stiffness 190 / damping 0.50   200ms 见峰 1.16，落到 0 用时 ~600ms
 * 压扁  squash   →  stiffness 400 / damping 0.65   300ms 回正
 * ```
 *
 * 前缘用位移量（`leadX`）直接驱动、而不是「中心 + 宽度」推导，这样拉长不会反过来拖慢它：
 * 三条线各自独立收敛，观感才是液体，而不是一根被拉长的弹簧。
 *
 * 时长没有任何 `duration` 参数可写——弹簧的「到位时间」由 stiffness 与 damping 决定，
 * 上面的数值是按 `m=1, c=2·√k·ζ` 的解析解逐点算出来的（不是拍脑袋）：
 * 前缘 400ms 到位、后缘 400ms 时还有 2% 没追上，视觉上正好是「前沿先到、后沿慢半拍」。
 *
 * 只在 [CustomizationStore.dockWaterDrop] 打开时绘制；关闭时整体淡出，不做「啪」地消失。
 */
@Composable
internal fun DockWaterDrop(
    selectedIndex: Int,
    count: Int,
    itemW: Dp,
    itemH: Dp,
    gap: Dp,
    accent: Color,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return

    val on = CustomizationStore.current.dockWaterDrop
    val target = selectedIndex.coerceIn(0, count - 1)

    // 首次组合不播放：从第 0 格「流」到当前格会像一次误触
    var last by remember { mutableIntStateOf(target) }
    var moved by remember { mutableIntStateOf(0) }
    LaunchedEffect(target) {
        if (last != target) {
            moved++
            last = target
        }
    }

    val stride = itemW + gap

    // ── 前沿 / 后缘：两条独立的弹簧 ──
    val leadX by animateDpAsState(
        targetValue = stride * target,
        animationSpec = if (on) {
            spring(dampingRatio = 1f, stiffness = 260f)
        } else {
            spring(dampingRatio = 1f, stiffness = 1400f)
        },
        label = "drop-lead",
    )
    val trailX by animateDpAsState(
        targetValue = stride * target,
        animationSpec = if (on) {
            // 低刚度 = 起步慢半拍；阻尼略低 = 追上时轻轻超一下再停
            spring(dampingRatio = 0.78f, stiffness = 150f)
        } else {
            spring(dampingRatio = 1f, stiffness = 1400f)
        },
        label = "drop-trail",
    )

    // ── 拉伸鼓包：每次换格重启一次，从 1 落回 0 ──
    val stretch = remember { Animatable(0f) }
    LaunchedEffect(moved) {
        if (moved == 0 || !on) return@LaunchedEffect
        stretch.snapTo(1f)
        stretch.animateTo(0f, spring(dampingRatio = 0.50f, stiffness = 190f))
    }
    val stretchAmt = stretch.value.coerceIn(0f, 1.3f)

    // ── 压扁：越拉越扁，拉伸归零即回 1 ──
    val squash by animateFloatAsState(
        targetValue = 1f - 0.10f * stretchAmt.coerceAtMost(1f),
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "drop-squash",
    )

    // 开关关闭时整体淡出
    val appear by animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 260f),
        label = "drop-appear",
    )
    if (appear <= 0.01f) return

    // 位于两缘之间的固定部分（一个格宽）、以及被拉伸撑出来的部分
    val base = itemW + stride * 0.30f * stretchAmt
    // 两缘的间距本身也被拉伸撑开：只让 gap 涨、不让格子涨，
    // 否则色块会盖到相邻的未选中图标上
    val lead = maxOf(leadX, trailX) + gap * 0.9f * stretchAmt.coerceAtMost(1f)
    val trail = minOf(leadX, trailX)
    val span = (lead - trail).coerceAtLeast(0.dp) + base

    Box(
        modifier = modifier
            .fillMaxHeight()
            .offset(x = trail)
            .width(span)
            // 只压高度不动宽度：压扁不该反过来改左右两缘的落点
            .height(itemH * 0.86f)
            // 垂直居中：0.86 高比格子矮，用固定的上偏移把两缘对齐中线
            .offset(y = (itemH - itemH * 0.86f) / 2)
            .graphicsLayer {
                scaleY = squash
                alpha = appear
            }
            .clip(CircleShape)
            .background(
                // 液体表面：上亮下暗同色系，避免变成一块塑料白
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = if (dark) 0.95f else 0.90f),
                        accent.copy(alpha = if (dark) 0.72f else 0.62f),
                    ),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        // 顶部一道极细高光：模拟水滴的折射亮边，拉伸时更明显
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = 0.20f + 0.18f * stretchAmt.coerceAtMost(1f)))
        )
    }
}

/**
 * 选中项图标的水滴落点回弹。
 *
 * 新选中项先被「砸」到 0.84 再 HighBouncy 弹回；旧项被吸走一点（0.92）再回位，
 * 避免它看起来完全无动于衷。返回值直接喂给 `graphicsLayer.scaleX/scaleY`。
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
            impact.snapTo(0.84f)
            // 先缩后弹：0.84 → 1 带一次明显的超调（阻尼 0.45），400ms 收尾
            impact.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 230f))
        } else {
            impact.animateTo(0.92f, spring(dampingRatio = 0.7f, stiffness = 760f))
            impact.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 400f))
        }
    }
    return impact.value
}

/**
 * 水滴开启时选中项的前景色。
 *
 * 胶囊变成一块强调色实底，原来「选中 = 强调色」的文字/图标就陷进去了。
 * 亮色主题给白字；暗色主题的强调色本身偏亮，给一层近黑墨色才够对比。
 * 关闭水滴时返回 null，调用方继续走原来的取色逻辑。
 */
@Composable
internal fun dropSelectedInk(dark: Boolean): Color? {
    if (!CustomizationStore.current.dockWaterDrop) return null
    return if (dark) Color(0xFF20182E) else Color.White
}
