package com.kuaimaosu.manager.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kuaimaosu.manager.ui.theme.KuaiMaoDimens
import com.kuaimaosu.manager.ui.theme.LocalKuaiMaoPalette
import kotlinx.coroutines.delay

/**
 * 骨架屏的统一节奏。
 *
 * 全页所有骨架块共用这一个 0..1 的进度值：1.5s 一轮，
 * 所以视觉上是「一道光带扫过整页」，而不是每块各扫各的、节奏互相错开。
 */
@Composable
fun rememberSkeletonPhase(): Float {
    val transition = rememberInfiniteTransition(label = "skeletonShine")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeletonPhase",
    )
    return phase
}

/** 骨架底色的两种墨色：深色页面上用白，浅色页面上用黑，透明度都压得很低。 */
@Composable
private fun skeletonInk(): Pair<Color, Color> {
    val dark = LocalKuaiMaoPalette.current.canvasStage.luminance() < 0.5f
    return if (dark) {
        Color.White.copy(alpha = 0.075f) to Color.White.copy(alpha = 0.17f)
    } else {
        Color.Black.copy(alpha = 0.055f) to Color.White.copy(alpha = 0.85f)
    }
}

/**
 * 一块骨架。斜向光带按 [phase] 从左上扫到右下，与整页其它骨架同一节奏。
 *
 * 光带不能做成矩形平移：那样在窄条上会像一条白杠滑过去。
 * 这里用 45° 的线性渐变，起点终点跟着 phase 平移，扫过时边界是软的。
 */
@Composable
fun SkeletonBone(
    modifier: Modifier = Modifier,
    shape: Shape = KuaiMaoDimens.rSm,
    phase: Float = rememberSkeletonPhase(),
) {
    val (base, shine) = skeletonInk()
    Box(
        modifier = modifier
            .clip(shape)
            .background(base)
            .drawBehind {
                val band = (size.width + size.height) * 0.34f
                val travel = size.width + band * 2f
                val x = -band + travel * phase
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, shine, Color.Transparent),
                        start = Offset(x - band, 0f),
                        end = Offset(x + band, size.height),
                    ),
                )
            },
    )
}

/** 圆形头像位（应用图标 / 用户头像）。 */
@Composable
fun SkeletonAvatar(
    size: Dp,
    modifier: Modifier = Modifier,
    phase: Float = rememberSkeletonPhase(),
) {
    SkeletonBone(
        modifier = modifier.size(size),
        shape = CircleShape,
        phase = phase,
    )
}

/** 一行文字条。宽度用比例给，短条看起来才像文字而不是色块。 */
@Composable
fun SkeletonLine(
    widthFraction: Float,
    height: Dp = 12.dp,
    modifier: Modifier = Modifier,
    phase: Float = rememberSkeletonPhase(),
) {
    SkeletonBone(
        modifier = modifier.fillMaxWidth(widthFraction).height(height),
        shape = RoundedCornerShape(height / 2),
        phase = phase,
    )
}

/** 矩形块（模块封面、图表、按钮位）。 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    phase: Float = rememberSkeletonPhase(),
) {
    SkeletonBone(
        modifier = modifier,
        shape = RoundedCornerShape(corner),
        phase = phase,
    )
}

/**
 * 逐块浮现：第 index 块比第一块晚 index × 60ms 出现，
 * 淡入的同时从下方 8dp 滑到位。整页从上到下像被「铺」出来。
 */
@Composable
fun RevealItem(
    index: Int,
    modifier: Modifier = Modifier,
    stepMillis: Long = 60L,
    content: @Composable () -> Unit,
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // 第 9 块以后不再递增：列表长了也不该让最后一条等一秒才出现。
        val delayMillis = minOf(index, 8) * stepMillis
        if (delayMillis > 0) delay(delayMillis)
        started = true
    }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "revealItem",
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 8.dp.toPx()
        },
    ) {
        content()
    }
}

/**
 * 骨架 → 内容的交叉切换。
 *
 * 真实内容**始终**参与测量（只是 alpha 为 0），骨架用 matchParentSize 盖在上面。
 * 这样页面高度从头到尾由真实内容决定，切换过程中不会先塌一下再撑开——
 * 这也是「整页不跳动」的唯一可靠写法：不要让骨架去撑高度。
 * 骨架退场（200ms）比内容入场（260ms）快一点，中间不会出现两层都半透明糊成一团。
 */
@Composable
fun SkeletonSwap(
    loading: Boolean,
    modifier: Modifier = Modifier,
    skeleton: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val skeletonAlpha by animateFloatAsState(
        targetValue = if (loading) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "skeletonOut",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (loading) 0f else 1f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "contentIn",
    )
    Box(modifier = modifier) {
        Box(modifier = Modifier.graphicsLayer { alpha = contentAlpha }) { content() }
        if (skeletonAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = skeletonAlpha },
            ) {
                skeleton()
            }
        }
    }
}

/**
 * 骨架至少露脸一小会儿。
 *
 * 本地读内核状态往往几十毫秒就回来了，直接切会让骨架闪一下反而更廉价；
 * 这里给一个下限，低于它宁可多停一会儿。
 */
@Composable
fun rememberSkeletonVisible(
    loaded: Boolean,
    minimumMillis: Long = 340L,
): Boolean {
    var visible by remember { mutableStateOf(!loaded) }
    LaunchedEffect(loaded) {
        if (!loaded) {
            visible = true
        } else if (visible) {
            delay(minimumMillis)
            visible = false
        }
    }
    return visible
}
