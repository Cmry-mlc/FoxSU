package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.data.settings.CustomizationStore

/**
 * 底栏图标的点击特效（替代原来那圈发黑的按压内阴影）。
 *
 * 内阴影在 48dp 的小圆片上是一块脏灰，用户反馈「太难看」，所以整块换掉，
 * 改成三件事，全部围绕图标本身发生：
 *   1. 图标先被"压"一下（0.86）再用 HighBouncy 弹簧弹回，带一次可见的回弹；
 *   2. 从图标中心荡开一圈同色光环，半径扩到 1.9 倍并淡出——像水面的一圈涟漪；
 *   3. 同时闪一下选中色的小光点，作为"点到了"的确认。
 *
 * 档位 [CustomizationStore.current.fxLevel] 为 0 时全部关掉，只留系统点击。
 */
@Composable
fun DockIconTapFx(
    pressed: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 21.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val level = CustomizationStore.current.fxLevel
    if (level <= 0) {
        Box(modifier, contentAlignment = Alignment.Center, content = content)
        return
    }

    // 按压弹簧：按下压、松手弹（一次超过 1.0 的回弹）
    val squeeze = remember { Animatable(0f) }
    // 涟漪：0 隐藏 → 1 完全张开淡出
    val ripple = remember { Animatable(1f) }

    LaunchedEffect(pressed) {
        if (pressed) {
            squeeze.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 1800f))
            ripple.snapTo(0f)
        } else {
            squeeze.animateTo(
                0f,
                spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = 520f),
            )
            ripple.animateTo(1f, tween(420))
        }
    }

    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            if (ripple.value < 1f) {
                val t = ripple.value
                val base = size.minDimension / 2f
                val radius = base * (0.9f + t * 1.05f)
                val alpha = (1f - t) * 0.55f
                // 外圈：一圈细描边涟漪
                drawCircle(
                    color = accent.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = (2.2f * (1f - t)).dp.toPx().coerceAtLeast(0.6f)),
                )
                // 内芯：一层极淡的实心柔光，跟着涟漪一起散开
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            accent.copy(alpha = alpha * 0.55f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    val s = 1f - squeeze.value * 0.14f
                    scaleX = s
                    scaleY = s
                },
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** 中心光点：点击时闪一下，颜色取选中色。 */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTapSpark(
    accent: Color,
    progress: Float,
    at: Offset = center,
) {
    if (progress <= 0f || progress >= 1f) return
    val grow = 1f - kotlin.math.abs(progress * 2f - 1f)
    drawCircle(
        color = accent.copy(alpha = 0.28f * grow),
        radius = 7.dp.toPx() * (0.6f + progress * 0.9f),
        center = at,
    )
}
