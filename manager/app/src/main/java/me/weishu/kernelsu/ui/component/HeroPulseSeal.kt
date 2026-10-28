package me.weishu.kernelsu.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * 首页大卡片右缘的「滑入滑出」大对勾（视频演示特效）：
 * 一个大号圆形对勾锚在卡片右缘，循环地滑出卡外（只露出弧线）再滑回卡内，
 * 同时带轻微的透明度呼吸。卡片自身的圆角裁切会自然形成「滑出被裁」的效果。
 *
 * @param ok true = 对勾（工作中/待重启），false = 叉号（未安装/失败/不支持）
 */
@Composable
fun BoxScope.HeroPulseSeal(
    ok: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    sealSize: Dp = 84.dp,
    ringWidth: Dp = 7.dp,
) {
    val transition = rememberInfiniteTransition(label = "hero-seal")
    // 0 = 完整在卡内，1 = 大半滑出卡外（只剩左弧线）
    val slide by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hero-seal-slide",
    )
    val breath by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hero-seal-breath",
    )

    Box(
        modifier = modifier
            .align(Alignment.CenterEnd)
            .graphicsLayer {
                // 滑出距离约为自身直径的 62%，滑出时稍微变淡
                translationX = sealSize.toPx() * 0.62f * slide
                alpha = 1f - 0.25f * slide
            }
            .size(sealSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(sealSize)
                .graphicsLayer {
                    scaleX = breath
                    scaleY = breath
                }
                .border(ringWidth, tint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (ok) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(sealSize * 0.52f)
                    .alpha(1f - slide * 0.35f),
            )
        }
    }
}


/**
 * 大卡片右缘图形的「滑出 / 滑回」循环动画（0 = 卡内，1 = 滑出 distance）。
 */
@Composable
fun Modifier.heroSlideOut(distance: Dp, periodMs: Int = 1900): Modifier {
    val transition = rememberInfiniteTransition(label = "hero-slide")
    val slide by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMs),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hero-slide-value",
    )
    return graphicsLayer {
        translationX = distance.toPx() * slide
        alpha = 1f - 0.25f * slide
    }
}
