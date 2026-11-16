package com.kuaimaosu.manager.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min

/**
 * ColorOS 17「流光轮廓」（Contour Glow）。
 *
 * 系统里的做法不是一圈死描边，而是「极淡的常驻边界 + 沿着轮廓滑行的一束凝光」：
 * 常驻那层负责把半透明卡片和背景分开（没有它，玻璃卡会糊进壁纸里），
 * 凝光那层负责质感——光斑贴着圆角走，前后拖一条渐隐的尾巴，转角会自然拐弯。
 *
 * 这里用 `sweepGradient` 直接描在圆角矩形路径上：角度扫过哪一段，哪一段就被点亮，
 * 比逐帧采样路径点再用几十个小圆拼要省得多（一块卡每帧只有三笔绘制），
 * 而且不管圆角多大都不会出现「点与点之间的锯齿」。
 */
private const val CONTOUR_PERIOD_MILLIS = 6_400

/** 一条流光的形状：起跑相位、占整条周长的比例、亮度权重。 */
private class CometSpec(
    val headOffset: Float,
    val span: Float,
    val weight: Float,
)

/**
 * 主光带 + 一条更短更暗的副光带。
 *
 * 两条同时存在才不会像「一个亮点绕圈跑」：主光经过之后，副光还在另一侧慢慢亮着，
 * 观感更接近系统里那种「整圈都有呼吸感」的凝光。
 */
private val COMETS = listOf(
    CometSpec(headOffset = 0f, span = 0.16f, weight = 1f),
    CometSpec(headOffset = 0.56f, span = 0.08f, weight = 0.55f),
)

/** 三层描边：外圈柔光、中间过渡、内圈实光。宽度与强度都是视觉调出来的。 */
private class GlowPass(val width: Dp, val alpha: Float)

private val GLOW_PASSES = listOf(
    GlowPass(width = 14.dp, alpha = 0.10f),
    GlowPass(width = 4.6.dp, alpha = 0.22f),
    GlowPass(width = 1.25.dp, alpha = 0.90f),
)

/**
 * 流光相位（0..1，6.4 秒一圈）。
 *
 * 返回 [State] 而不是 Float：调用方在绘制阶段读 `.value`，动画只触发重绘、
 * 不触发重组——底栏里那排 Tab 不会被六十帧每秒地重新测量一遍。
 * 关掉开关时返回 null，动画直接停掉，不空转。
 */
@Composable
fun rememberContourPhase(enabled: Boolean): State<Float>? {
    if (!enabled) return null
    val transition = rememberInfiniteTransition(label = "contourGlow")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CONTOUR_PERIOD_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "contourPhase",
    )
}

/**
 * 给任意圆角矩形加一圈流光轮廓。
 *
 * 必须挂在 `.clip(shape)` **外面**：描边有一半落在边界之外，被裁掉就成了半条细线。
 *
 * @param cornerRadius 圆角半径；胶囊传 999.dp 即可，内部会按短边收敛。
 * @param accent 主题强调色，光带的颜色由它派生。
 * @param dark 深色卡面为 true：光带里混入更多白色，避免深底上只剩一条同色暗线。
 * @param phase 绘制阶段读取的相位，见 [rememberContourPhase]。
 */
fun Modifier.contourGlow(
    cornerRadius: Dp,
    accent: Color,
    enabled: Boolean,
    dark: Boolean,
    intensity: Float = 1f,
    phase: () -> Float,
): Modifier {
    if (!enabled) return this
    return this.drawWithContent {
        drawContent()
        drawContourGlow(
            cornerRadiusPx = cornerRadius.toPx(),
            accent = accent,
            progress = phase(),
            dark = dark,
            intensity = intensity,
        )
    }
}

private fun DrawScope.drawContourGlow(
    cornerRadiusPx: Float,
    accent: Color,
    progress: Float,
    dark: Boolean,
    intensity: Float,
) {
    val w = size.width
    val h = size.height
    if (w < 8f || h < 8f) return

    // 往里收半像素：描边居中在路径上，不收就会有一半被父级的裁剪吃掉。
    val inset = 0.6.dp.toPx()
    val maxRadius = (min(w, h) / 2f - inset).coerceAtLeast(0f)
    val radius = cornerRadiusPx.coerceIn(0f, maxRadius)
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(inset, inset, w - inset, h - inset),
                cornerRadius = CornerRadius(radius, radius),
            ),
        )
    }
    val core = lerp(accent, Color.White, if (dark) 0.72f else 0.42f)
    val base = lerp(accent, Color.White, 0.30f)

    // 常驻边界：不用描边色块，只用一层薄光，这样关掉流光也不会留下生硬的线。
    drawPath(
        path = path,
        color = base.copy(alpha = 0.15f * intensity),
        style = Stroke(width = 1.dp.toPx()),
    )

    // 扫掠渐变是按「角度」上色的，而同样一段角度在直边和圆角上跨过的周长并不相等：
    // 直接给角度占比，窄边上会整条亮起来。这里按周长换算——先量出光带首尾两点
    // 在路径上的角度，再用这个角度区间去扫，光带长度就与形状无关了。
    val measure = PathMeasure().apply { setPath(path, false) }
    val total = measure.length
    if (total <= 0f) return
    val center = Offset(w / 2f, h / 2f)

    for (comet in COMETS) {
        val headDist = ((progress + comet.headOffset).mod(1f)) * total
        val tailDist = (headDist - total * comet.span).mod(total)
        val span = (
            angleOf(measure.getPosition(headDist), center) -
                angleOf(measure.getPosition(tailDist), center) + 1f
            ).mod(1f)
        val headPoint = measure.getPosition(headDist)
        val head = angleOf(headPoint, center)
        for (pass in GLOW_PASSES) {
            val peak = pass.alpha * comet.weight * intensity
            if (peak <= 0.002f) continue
            drawPath(
                path = path,
                brush = Brush.sweepGradient(
                    *cometStops(head, span, accent, core, peak),
                ),
                style = Stroke(
                    width = pass.width.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
        // 光头的凝光：光带最亮处再压一小团柔光，看起来是「一束光停在边上」，
        // 而不是一条被画亮的线。
        val haloRadius = 11.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    core.copy(alpha = 0.34f * comet.weight * intensity),
                    accent.copy(alpha = 0.10f * comet.weight * intensity),
                    Color.Transparent,
                ),
                center = headPoint,
                radius = haloRadius,
            ),
            radius = haloRadius,
            center = headPoint,
        )
    }
}

/** 路径上某点相对中心的扫掠角（0..1，0 = 三点钟方向，顺时针）。 */
private fun angleOf(point: Offset, center: Offset): Float {
    val angle = atan2(point.y - center.y, point.x - center.x)
    return (angle / (2f * PI.toFloat())).mod(1f)
}

/**
 * 一条流光的扫掠渐变。
 *
 * 头部最亮（混白）、尾巴按平方归零；头部之后紧跟一个透明锚点，光带才不会
 * 从头部一路淡到绕完一整圈才消失。首尾各钉一个全透明锚点，跨过 0° 接缝时
 * 也不会在三点钟方向闪出一条硬边。
 */
private fun cometStops(
    head: Float,
    span: Float,
    accent: Color,
    core: Color,
    peak: Float,
): Array<Pair<Float, Color>> {
    val count = 16
    val stops = ArrayList<Pair<Float, Color>>(count + 3)
    for (i in 0 until count) {
        val t = i / (count - 1f)
        val pos = (head - span + span * t).mod(1f)
        // 端点自己带锚点，0 / 1 附近的采样丢掉，避免重复位置
        if (pos < 0.004f || pos > 0.996f) continue
        val tint = if (t > 0.66f) lerp(accent, core, (t - 0.66f) / 0.34f) else accent
        stops += pos to tint.copy(alpha = peak * t * t)
    }
    val clear = accent.copy(alpha = 0f)
    // 头部之后的急停：没有它，光带会从最亮处缓慢衰减整整一圈。
    val stopPos = (head + 0.022f).mod(1f)
    if (stopPos > 0.004f && stopPos < 0.996f) stops += stopPos to clear
    stops.sortBy { it.first }
    stops.add(0, 0f to clear)
    stops.add(1f to clear)
    return stops.toTypedArray()
}
