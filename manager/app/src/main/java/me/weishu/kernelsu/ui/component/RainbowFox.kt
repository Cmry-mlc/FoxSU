package me.weishu.kernelsu.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * 标题栏里的「动态七彩狐狸」。
 *
 * 做法：把全局彩色狐狸（ic_fox_global，多层渐变）整张做色相旋转矩阵，
 * 每帧换一个色相 —— 于是同一只狐狸会自然地在七彩之间流转，
 * 配合极轻微的缩放呼吸（1f ↔ 1.05f），观感就是「在呼吸」。
 *
 * @param baseHue 起始色相（0-360）。不传则每次进入随机。
 */
@Composable
fun RainbowFoxIcon(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    baseHue: Int? = null,
    breathe: Boolean = true,
) {
    // 随机起始色相：同一屏多个标题栏也是不同的颜色
    val hue = remember { baseHue ?: (0 until 360).random() }

    val transition = rememberInfiniteTransition(label = "fox-rainbow")

    // 色相缓慢流转一整圈（12s），让颜色持续变化但不至于刺眼
    val hueSpin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
        ),
        label = "fox-hue",
    )

    // 呼吸：2.6s 一个来回，幅度 5%
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fox-breath",
    )

    val scale = if (breathe) 1f + 0.05f * breath else 1f

    Image(
        painter = painterResource(R.drawable.ic_fox_global),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.colorMatrix(
            hueRotationMatrix(((hue + hueSpin) % 360f).toDouble())
        ),
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    )
}

/**
 * 生成标准 HSV 色相旋转矩阵（与 Android ColorMatrix.setRotateHue 同一套系数）。
 * 直接作用在彩色像素上，可以把整张彩色狐狸整体换色。
 */
private fun hueRotationMatrix(degrees: Double): ColorMatrix {
    val rad = Math.toRadians(degrees)
    val c = cos(rad)
    val s = sin(rad)
    return ColorMatrix(
        floatArrayOf(
            (0.213 + c * 0.787 - s * 0.213).toFloat(),
            (0.715 - c * 0.715 - s * 0.715).toFloat(),
            (0.072 - c * 0.072 + s * 0.928).toFloat(),
            0f, 0f,
            (0.213 - c * 0.213 + s * 0.143).toFloat(),
            (0.715 + c * 0.285 + s * 0.140).toFloat(),
            (0.072 - c * 0.072 - s * 0.283).toFloat(),
            0f, 0f,
            (0.213 - c * 0.213 - s * 0.787).toFloat(),
            (0.715 - c * 0.715 + s * 0.715).toFloat(),
            (0.072 + c * 0.928 + s * 0.072).toFloat(),
            0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
}
