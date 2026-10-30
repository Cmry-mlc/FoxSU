package me.weishu.kernelsu.ui.screen.ag

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.dp

/**
 * 主页大卡片的 3D 触摸特效：
 * - 手指按下后，卡片跟着触摸点做透视倾斜（最大 ±7°，cameraDistance 拉远避免棱角失真）；
 * - 触摸点处有一团径向反射流光跟着手指走；
 * - 松手后 rotation 用 Spring 阻尼回正，流光淡出。
 *
 * 只改 rotation/绘制，不消费点击以外的手势，卡片本身的功能不受影响。
 */
fun Modifier.tiltCardFx(): Modifier = tiltTransform()

private fun Modifier.tiltTransform(): Modifier = composed {
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    var light by remember { mutableStateOf(Offset(-999f, -999f)) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    this
        .graphicsLayer {
            rotationX = rotX.value
            rotationY = rotY.value
            cameraDistance = 24f * density
        }
        .drawWithContent {
            drawContent()
            if (glow.value > 0.01f) {
                drawRect(
                    Brush.radialGradient(
                        0f to Color.White.copy(alpha = 0.18f * glow.value),
                        0.45f to Color.White.copy(alpha = 0.05f * glow.value),
                        1f to Color.Transparent,
                        center = light,
                        radius = size.width * 0.55f,
                    )
                )
            }
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                light = down.position
                scope.launch { glow.animateTo(1f, tween(160)) }
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (change.changedToUp()) {
                        change.consume()
                        break
                    }
                    val pos = change.position
                    light = pos
                    val nx = (pos.x - size.width / 2f) / (size.width / 2f)
                    val ny = (pos.y - size.height / 2f) / (size.height / 2f)
                    val rx = (-ny * 7f).coerceIn(-7f, 7f)
                    val ry = (nx * 7f).coerceIn(-7f, 7f)
                    scope.launch {
                        rotY.snapTo(ry)
                        rotX.snapTo(rx)
                    }
                    change.consume()
                }
                scope.launch {
                    // 松手：Spring 阻尼回正 + 流光淡出
                    rotX.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 420f))
                    rotY.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 420f))
                    glow.animateTo(0f, tween(340))
                }
            }
        }
}

/**
 * 科幻粒子边：彩色光点沿按钮圆角路径流动，拖尾逐颗变淡，
 * 尾段沿法线方向向外飘散进虚空（飘散距离随拖尾加深）。
 *
 * 用 android PathMeasure 取路径坐标，16 颗粒子一帧画完，只在原地循环。
 */
@Composable
fun ParticleEdgeOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "p-edge")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "p-edge-phase",
    )
    val colors = listOf(
        Color(0xFFFF7A5C), Color(0xFFFFD84D), Color(0xFF7FD896),
        Color(0xFFA88BFF), Color(0xFFEC4899), Color(0xFF38BDF8),
    )
    Canvas(modifier) { drawParticleEdge(phase, colors) }
}

private fun DrawScope.drawParticleEdge(phase: Float, colors: List<Color>) {
    val r = size.height / 2f
    val composePath = Path().apply {
        addRoundRect(
            RoundRect(
                0f, 0f, size.width, size.height,
                CornerRadius(r, r),
            )
        )
    }
    val pm = android.graphics.PathMeasure(composePath.asAndroidPath(), false)
    val len = pm.length
    if (len <= 0f) return
    val head = phase * (len + 90f)
    val tail = 16
    val gap = 8.5f
    val driftMax = 9.dp.toPx()
    val pos = FloatArray(2)
    val tan = FloatArray(2)
    for (i in 0 until tail) {
        val d = head - i * gap
        if (d < 0f) continue
        val t = i.toFloat() / tail
        pm.getPosTan(d.coerceAtMost(len), pos, tan)
        // 法线方向（头在左上顺时针走时法线朝外）
        val nx = -tan[1]
        val ny = tan[0]
        val drift = t * t * driftMax * (if (i % 2 == 0) 1f else -0.55f)
        val beyond = (d - len).coerceAtLeast(0f) / 90f
        val alpha = ((1f - t) * (1f - t) * (1f - beyond)).coerceIn(0f, 1f)
        if (alpha < 0.02f) continue
        drawCircle(
            color = colors[i % colors.size].copy(alpha = alpha * 0.85f),
            radius = (2.6f - t * 1.6f).dp.toPx(),
            center = Offset(pos[0] + nx * drift, pos[1] + ny * drift),
        )
    }
    // 头部主光点：亮白芯 + 外圈第一色，视觉焦点
    if (head <= len) {
        pm.getPosTan(head, pos, tan)
        drawCircle(Color(0x66FFFFFF), radius = 5.dp.toPx(), center = Offset(pos[0], pos[1]))
        drawCircle(colors[0], radius = 2.8.dp.toPx(), center = Offset(pos[0], pos[1]))
    }
}

/**
 * 按压「深度内阴影」：四边各画一条向内衰减的暗色渐变，
 * alpha 由按压动画驱动，松手随回弹一起淡出。画在 clip 内即成内阴影。
 */
fun DrawScope.drawInnerShadow(alpha: Float) {
    if (alpha <= 0.01f) return
    val c = Color.Black.copy(alpha = 0.30f * alpha)
    val w = size.width
    val h = size.height
    drawRect(
        Brush.verticalGradient(0f to c, 0.28f to Color.Transparent, startY = 0f, endY = h * 0.35f),
        size = androidx.compose.ui.geometry.Size(w, h * 0.35f),
    )
    drawRect(
        Brush.verticalGradient(0f to Color.Transparent, 1f to c, startY = h * 0.65f, endY = h),
        topLeft = Offset(0f, h * 0.65f),
        size = androidx.compose.ui.geometry.Size(w, h * 0.35f),
    )
    drawRect(
        Brush.horizontalGradient(0f to c, 0.22f to Color.Transparent, startX = 0f, endX = w * 0.3f),
        size = androidx.compose.ui.geometry.Size(w * 0.3f, h),
    )
    drawRect(
        Brush.horizontalGradient(0f to Color.Transparent, 1f to c, startX = w * 0.7f, endX = w),
        topLeft = Offset(w * 0.7f, 0f),
        size = androidx.compose.ui.geometry.Size(w * 0.3f, h),
    )
}

/**
 * 共享元素缩放：从 origin（触发按钮的屏幕矩形）放大到自身位置，progress 0→1；
 * 关闭时把 progress 动画回 0 即「缩回原按钮」。
 */
fun Modifier.zoomFromOrigin(origin: androidx.compose.ui.geometry.Rect, progress: Float): Modifier = graphicsLayer {
    if (progress >= 0.999f || origin.isEmpty) return@graphicsLayer
    val startScale = (origin.width / size.width).coerceIn(0.06f, 1f)
    val sc = startScale + (1f - startScale) * progress
    scaleX = sc
    scaleY = sc
    val selfCenter = Offset(size.width / 2f, size.height / 2f)
    translationX = (origin.center.x - selfCenter.x) * (1f - progress)
    translationY = (origin.center.y - selfCenter.y) * (1f - progress)
    alpha = (progress * 2.6f).coerceIn(0f, 1f)
}
