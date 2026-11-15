package com.kuaimaosu.manager.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * 动态背景特效。
 *
 * 全部由 Canvas 程序化绘制，不依赖位图资源，所以体积为零、任意分辨率都清晰，
 * 颜色都从主题强调色（accent）派生，换主题色时背景会一起变色。
 * [NONE] 表示关闭，始终排在列表第一位。
 *
 * 这里只保留「在浅色与深色画布上都看得见、且不难看」的十几套：
 * 之前那批网格 / 光束 / 颗粒 / 彩屑 / 岩浆 / 漩涡不是像施工围挡，就是几乎看不见，已经删掉。
 */
enum class BackgroundEffect(val id: String, val label: String) {
    NONE("none", "无"),
    CATPAW("catpaw", "猫爪"),
    MESH("mesh", "流体"),
    BUBBLES("bubbles", "气泡"),
    RIPPLES("ripples", "涟漪"),
    STARS("stars", "星尘"),
    CONSTELLATION("constellation", "星链"),
    METEOR("meteor", "流星"),
    SNOW("snow", "飘雪"),
    RAIN("rain", "细雨"),
    ORBITS("orbits", "轨道"),
    BOKEH("bokeh", "光斑"),
    FIREFLY("firefly", "萤火"),
    PETALS("petals", "花瓣"),
    SILK("silk", "绸带"),
    MIST("mist", "薄雾"),
    ;

    companion object {
        fun fromId(id: String?): BackgroundEffect = entries.find { it.id == id } ?: NONE

        /** 真正的「特效」数量，不含关闭项。 */
        val effectCount: Int get() = entries.size - 1
    }
}

private const val TAU = 2f * PI.toFloat()

/** 确定性伪随机：同一个 index 永远拿到同一个值，避免每帧抖动。 */
private fun noise(index: Int, seed: Int): Float {
    val x = sin(index * 12.9898f + seed * 78.233f) * 43758.5453f
    return x - floor(x)
}

/** 色相偏移，用来从主题色派生互补色。中性色（纯黑/白主题）会补一点彩度，避免背景全灰。 */
internal fun Color.shiftHue(degrees: Float): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees).mod(360f)
    if (hsv[1] < 0.22f) hsv[1] = 0.55f
    if (hsv[2] < 0.35f) hsv[2] = 0.62f
    return Color(AndroidColor.HSVToColor(hsv))
}

/**
 * 统一的特效绘制入口。
 *
 * [time] 为 0..1 的线性时间（长周期，负责位移动画），[beat] 为 0..1 的快周期
 * （负责闪烁、脉动）。拆成两个时钟是为了让「缓慢流动」和「快速呼吸」互不干扰。
 *
 * [dark] 决定「粒子」用什么颜色：浅色画布上用深墨点（白色点在浅灰底上等于没画，
 * 之前星空/飘雪/细雨/星链在日间完全看不见就是这个原因），深色画布上才用白点。
 *
 * 所有位移都用「整数倍频率」，这样 26 秒循环回到起点时画面是连续的，
 * 不会每 26 秒整体跳一下。
 */
fun DrawScope.drawKuaiMaoEffect(
    effect: BackgroundEffect,
    accent: Color,
    time: Float,
    beat: Float,
    amplitude: Float = 1f,
    dark: Boolean = false,
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f || effect == BackgroundEffect.NONE) return

    // 主题色是黑/白这类中性色时，特效会几乎看不见。
    // 这时候退回一套冷色底，保证「选了就有东西」；主题色一旦有彩度就跟着主题走。
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(accent.toArgb(), hsv)
    val neutralAccent = hsv[1] < 0.20f || hsv[2] < 0.10f
    val base = if (neutralAccent) Color(0xFF4F7BE8) else accent

    val accent = base
    val second = base.shiftHue(64f)
    val third = base.shiftHue(196f)
    val a = amplitude.coerceIn(0f, 1.6f)
    val t = time
    val f = beat

    // 墨点：浅色用深蓝黑、深色用白，两边都能看见。浅色底上再收一点透明度，避免发脏。
    val ink = if (dark) Color.White else Color(0xFF232838)
    val inkScale = if (dark) 1f else 0.78f
    fun inkAt(alpha: Float): Color = ink.copy(alpha = (alpha * inkScale).coerceIn(0f, 1f))

    when (effect) {
        BackgroundEffect.NONE -> Unit

        // 猫爪：一枚枚爪印从底部慢慢浮起，左右轻摆，越往上越淡
        BackgroundEffect.CATPAW -> {
            val pawColor = lerp(ink, accent, 0.45f)
            for (i in 0 until 11) {
                val cycles = if (noise(i, 5) > 0.45f) 2f else 1f
                val p = (t * cycles + noise(i, 2)) % 1f
                val y = h * (1.16f - p * 1.34f)
                val x = w * (0.10f + 0.80f * noise(i, 3)) +
                    sin(t * TAU + i * 1.7f) * w * 0.035f
                val r = minOf(w, h) * (0.026f + 0.020f * noise(i, 4))
                // 底部淡入、顶部淡出，中间最实
                val fade = when {
                    p < 0.14f -> p / 0.14f
                    p > 0.72f -> (1f - p) / 0.28f
                    else -> 1f
                }
                val alpha = fade.coerceIn(0f, 1f) * 0.30f * a
                val rot = sin(t * TAU + i * 1.9f) * 16f
                drawPaw(
                    center = Offset(x, y),
                    radius = r,
                    color = pawColor.copy(alpha = alpha),
                    rotationDeg = rot,
                )
            }
        }

        // 流体：四团径向光斑互相穿插，是最耐看的一档
        BackgroundEffect.MESH -> {
            for (i in 0 until 4) {
                val ang = t * TAU + i * (TAU / 4f) + i * 0.6f
                val cx = w * (0.5f + 0.30f * cos(ang))
                val cy = h * (0.46f + 0.24f * sin(ang * 2f))
                val r = minOf(w, h) * (0.46f + 0.10f * sin(ang * 2f + i))
                val color = when (i % 3) {
                    0 -> accent
                    1 -> second
                    else -> third
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.24f * a), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(cx, cy),
                )
            }
        }

        // 气泡：自下而上，越往上越淡
        BackgroundEffect.BUBBLES -> {
            for (i in 0 until 16) {
                val cycles = if (i % 2 == 0) 2f else 1f
                val p = (t * cycles + noise(i, 2)) % 1f
                val y = h * (1.05f - p * 1.15f)
                val x = w * (0.05f + 0.9f * noise(i, 3)) + sin(t * TAU + i) * 14f
                val r = minOf(w, h) * (0.018f + 0.032f * noise(i, 4))
                drawCircle(
                    color = inkAt(0.10f * a),
                    radius = r,
                    center = Offset(x, y),
                )
                drawCircle(
                    color = accent.copy(alpha = 0.30f * (1f - p) * a),
                    radius = r,
                    center = Offset(x, y),
                    style = Stroke(width = 1.3f),
                )
            }
        }

        // 涟漪：三个定点不断向外扩散的同心圆
        BackgroundEffect.RIPPLES -> {
            val sources = listOf(
                Offset(w * 0.24f, h * 0.30f),
                Offset(w * 0.78f, h * 0.52f),
                Offset(w * 0.44f, h * 0.80f),
            )
            sources.forEachIndexed { s, center ->
                val maxR = minOf(w, h) * 0.30f
                for (i in 0 until 4) {
                    val p = ((t + s * 0.19f + i * 0.25f) % 1f)
                    drawCircle(
                        color = accent.copy(alpha = 0.18f * (1f - p) * a),
                        radius = maxR * p,
                        center = center,
                        style = Stroke(width = 1.3f),
                    )
                }
            }
        }

        // 星尘：静态星点 + 独立相位闪烁
        BackgroundEffect.STARS -> {
            for (i in 0 until 110) {
                val x = noise(i, 1) * w
                val y = noise(i, 2) * h
                val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * TAU * 3f + noise(i, 3) * TAU))
                val r = 0.5f + noise(i, 4) * 1.7f
                val color = if (i % 5 == 0) accent.copy(alpha = 0.65f * a) else inkAt(0.55f * a)
                drawCircle(color = color, radius = r, center = Offset(x, y), alpha = twinkle)
            }
        }

        // 星链：漂浮光点按距离连线，近则亮
        BackgroundEffect.CONSTELLATION -> {
            val count = 24
            val pts = List(count) { i ->
                Offset(
                    x = w * (0.08f + 0.84f * noise(i, 1)) + sin(t * TAU + i) * 16f,
                    y = h * (0.08f + 0.84f * noise(i, 2)) + cos(t * TAU + i) * 16f,
                )
            }
            val reach = w * 0.24f
            for (i in 0 until count) {
                for (j in i + 1 until count) {
                    val d = (pts[i] - pts[j]).getDistance()
                    if (d < reach) {
                        drawLine(
                            color = inkAt(0.20f * (1f - d / reach) * a),
                            start = pts[i],
                            end = pts[j],
                            strokeWidth = 1f,
                        )
                    }
                }
            }
            pts.forEachIndexed { i, p ->
                drawCircle(
                    color = if (i % 4 == 0) accent.copy(alpha = 0.8f * a) else inkAt(0.55f * a),
                    radius = 1.8f,
                    center = p,
                )
            }
        }

        // 流星：六条斜向拖尾，错峰划过
        BackgroundEffect.METEOR -> {
            for (i in 0 until 6) {
                val cycles = if (i % 2 == 0) 2f else 1f
                val p = (t * cycles + noise(i, 2)) % 1f
                val sx = w * (1.15f - p * 1.5f)
                val sy = h * (0.05f + noise(i, 3) * 0.55f) + p * h * 0.35f
                val tail = minOf(w, h) * (0.12f + noise(i, 4) * 0.10f)
                val color = if (i % 3 == 0) accent else ink
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, color.copy(alpha = 0.65f * a)),
                        start = Offset(sx, sy),
                        end = Offset(sx + tail * 0.8f, sy - tail),
                    ),
                    start = Offset(sx, sy),
                    end = Offset(sx + tail * 0.8f, sy - tail),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round,
                )
            }
        }

        // 飘雪：缓慢下落 + 左右摇摆
        BackgroundEffect.SNOW -> {
            for (i in 0 until 42) {
                val cycles = if (i % 3 == 0) 2f else 1f
                val p = (t * cycles + noise(i, 2)) % 1f
                val y = p * h * 1.08f - h * 0.04f
                val x = noise(i, 3) * w + sin(t * TAU + i) * 18f
                drawCircle(
                    color = inkAt((0.28f + 0.40f * noise(i, 4)) * a),
                    radius = 1f + noise(i, 6) * 2.2f,
                    center = Offset(x, y),
                )
            }
        }

        // 细雨：斜向短线条快速下落
        BackgroundEffect.RAIN -> {
            for (i in 0 until 46) {
                val cycles = if (i % 2 == 0) 3f else 2f
                val p = (t * cycles + noise(i, 2)) % 1f
                val y = -h * 0.1f + p * h * 1.2f
                val x = noise(i, 3) * w * 1.05f + y * 0.05f
                val len = h * (0.03f + noise(i, 4) * 0.05f)
                drawLine(
                    color = inkAt(0.22f * a),
                    start = Offset(x, y),
                    end = Offset(x - len * 0.12f, y + len),
                    strokeWidth = 1.2f,
                    cap = StrokeCap.Round,
                )
            }
        }

        // 轨道：三层椭圆轨道 + 沿线运行的光点
        BackgroundEffect.ORBITS -> {
            val cx = w * 0.5f
            val cy = h * 0.42f
            val baseSize = minOf(w, h)
            for (i in 0 until 3) {
                val rx = baseSize * (0.26f + i * 0.14f)
                val ry = baseSize * (0.10f + i * 0.06f)
                drawOval(
                    color = accent.copy(alpha = 0.12f * a),
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = 1f),
                )
                val ang = t * TAU + i * (TAU / 3f)
                val px = cx + rx * cos(ang)
                val py = cy + ry * sin(ang)
                drawCircle(
                    color = (if (i == 1) second else accent).copy(alpha = 0.85f * a),
                    radius = 3.2f,
                    center = Offset(px, py),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.24f * a), Color.Transparent),
                        center = Offset(px, py),
                        radius = 16f,
                    ),
                    radius = 16f,
                    center = Offset(px, py),
                )
            }
        }

        // 光斑：几团失焦的大光点缓慢漂移
        BackgroundEffect.BOKEH -> {
            for (i in 0 until 9) {
                val ang = t * TAU + i * 1.1f
                val cx = w * (0.10f + 0.80f * noise(i, 1)) + sin(ang) * w * 0.05f
                val cy = h * (0.08f + 0.84f * noise(i, 2)) + cos(ang) * h * 0.03f
                val r = minOf(w, h) * (0.05f + 0.11f * noise(i, 3))
                val color = when (i % 3) {
                    0 -> accent
                    1 -> second
                    else -> third
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.16f * a),
                            color.copy(alpha = 0.06f * a),
                            Color.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(cx, cy),
                )
            }
        }

        // 萤火：十几个小光点在原地游走，明灭不同步
        BackgroundEffect.FIREFLY -> {
            val warm = lerp(accent, Color(0xFFFFD98A), 0.55f)
            for (i in 0 until 16) {
                val x = w * (0.06f + 0.88f * noise(i, 1)) + sin(t * TAU + i * 1.3f) * w * 0.06f
                val y = h * (0.08f + 0.84f * noise(i, 2)) + cos(t * TAU + i * 0.7f) * h * 0.05f
                val breathe = 0.35f + 0.65f * (0.5f + 0.5f * sin(f * TAU + noise(i, 3) * TAU))
                val color = if (i % 4 == 0) warm else accent
                val r = 1.2f + noise(i, 4) * 1.8f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.50f * breathe * a), Color.Transparent),
                        center = Offset(x, y),
                        radius = r * 6f,
                    ),
                    radius = r * 6f,
                    center = Offset(x, y),
                )
                drawCircle(
                    color = color.copy(alpha = 0.75f * breathe * a),
                    radius = r,
                    center = Offset(x, y),
                )
            }
        }

        // 花瓣：一片片斜着飘落，边走边翻面
        BackgroundEffect.PETALS -> {
            val petal = lerp(ink, accent, 0.55f)
            for (i in 0 until 14) {
                val cycles = if (i % 2 == 0) 2f else 1f
                val p = (t * cycles + noise(i, 2)) % 1f
                val y = -h * 0.06f + p * h * 1.14f
                val x = noise(i, 3) * w + sin(t * TAU + i) * w * 0.06f
                val r = minOf(w, h) * (0.010f + 0.010f * noise(i, 4))
                val flat = 0.42f + 0.36f * abs(sin(t * TAU * 2f + i * 1.7f))
                rotate(degrees = sin(t * TAU + i) * 40f, pivot = Offset(x, y)) {
                    drawOval(
                        color = petal.copy(alpha = 0.34f * a),
                        topLeft = Offset(x - r, y - r * flat),
                        size = Size(r * 2f, r * flat * 2f),
                    )
                }
            }
        }

        // 绸带：三条斜向柔光带自下而上扫过
        BackgroundEffect.SILK -> {
            rotate(degrees = -24f, pivot = Offset(w / 2f, h / 2f)) {
                for (i in 0 until 3) {
                    val phase = (t + i / 3f) % 1f
                    val bandY = h * (1.15f - phase * 1.5f) + i * h * 0.12f
                    val color = when (i) {
                        0 -> accent
                        1 -> second
                        else -> third
                    }
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                color.copy(alpha = 0.15f * a),
                                Color.Transparent,
                            ),
                            startY = bandY - h * 0.12f,
                            endY = bandY + h * 0.12f,
                        ),
                        topLeft = Offset(-w * 0.5f, bandY - h * 0.12f),
                        size = Size(w * 2f, h * 0.24f),
                    )
                }
            }
        }

        // 薄雾：几团极大的柔光互相重叠，只让底色缓慢呼吸
        BackgroundEffect.MIST -> {
            for (i in 0 until 5) {
                val ang = t * TAU + i * 1.4f
                val cx = w * (0.25f + 0.5f * noise(i, 1)) + sin(ang) * w * 0.10f
                val cy = h * (0.25f + 0.5f * noise(i, 2)) + cos(ang) * h * 0.08f
                val r = minOf(w, h) * (0.55f + 0.20f * noise(i, 3))
                val color = when (i % 3) {
                    0 -> accent
                    1 -> second
                    else -> third
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.09f * a), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(cx, cy),
                )
            }
        }

    }
}

/**
 * 一枚猫爪印：下面一颗扁圆掌垫 + 上面四颗脚趾，整体按 [rotationDeg] 旋转。
 * [radius] 是「掌垫半宽」的基准，脚趾按它等比缩放。
 */
private fun DrawScope.drawPaw(
    center: Offset,
    radius: Float,
    color: Color,
    rotationDeg: Float,
) {
    rotate(degrees = rotationDeg, pivot = center) {
        // 掌垫：一颗略扁的椭圆
        drawOval(
            color = color,
            topLeft = Offset(center.x - radius * 0.64f, center.y - radius * 0.10f),
            size = Size(radius * 1.28f, radius * 0.96f),
        )
        // 四颗脚趾：两高两低，贴着掌垫上方排开
        val toes = listOf(
            -0.86f to -0.48f,
            -0.30f to -0.80f,
            0.30f to -0.80f,
            0.86f to -0.48f,
        )
        toes.forEach { (dx, dy) ->
            drawOval(
                color = color,
                topLeft = Offset(
                    center.x + dx * radius - radius * 0.27f,
                    center.y + dy * radius - radius * 0.23f,
                ),
                size = Size(radius * 0.54f, radius * 0.46f),
            )
        }
    }
}

/**
 * 背景特效层：自己驱动时间轴，铺满整屏。
 */
@Composable
fun BackgroundEffectLayer(
    effect: BackgroundEffect,
    modifier: Modifier = Modifier,
    amplitude: Float = 1f,
) {
    if (effect == BackgroundEffect.NONE) return
    val accent = MaterialTheme.colorScheme.primary
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val transition = rememberInfiniteTransition(label = "bgEffect")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bgTime",
    )
    val beat by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bgBeat",
    )
    Canvas(modifier = modifier) {
        drawKuaiMaoEffect(effect, accent, time, beat, amplitude, dark)
    }
}
