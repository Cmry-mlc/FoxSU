package com.loomsu.manager.ui.screen.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/**
 * Loom SU 冷启动首屏。
 *
 * 视觉取自设计稿的「深色卡面」基调：深靛渐变 + 双光斑，
 * 中央是经纬交织的品牌符号，线条逐条「织」出来后结点亮起。
 */
@Composable
fun LoomSplash(
    modifier: Modifier = Modifier,
    stateText: String = "",
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 与系统启动窗口同色（res/values/colors.xml · loom_ink），
            // 冷启动时只有 logo 淡入，不会先看到一块底色再切换。
            .background(Color(0xFF181D33)),
    ) {
        // 光斑：右上紫、左下绿，与大卡保持同一光源
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x806C5CF0), Color.Transparent),
                    center = Offset(size.width * 0.84f, size.height * 0.06f),
                    radius = size.minDimension * 0.62f,
                ),
                radius = size.minDimension * 0.62f,
                center = Offset(size.width * 0.84f, size.height * 0.06f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x571CA672), Color.Transparent),
                    center = Offset(size.width * 0.02f, size.height * 0.94f),
                    radius = size.minDimension * 0.55f,
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.02f, size.height * 0.94f),
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LoomMark(
                progress = progress.value,
                modifier = Modifier.size(104.dp),
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Loom SU",
                color = Color(0xFFF4F6FF),
                fontSize = 27.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "KERNEL ROOT WORKBENCH",
                color = Color(0x66BEC8E6),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.2.sp,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stateText,
                color = Color(0x59BEC8E6),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 经纬标记：经线落下 → 纬线折出 → 纬纱穿过 → 结点亮起。 */
@Composable
fun LoomMark(
    progress: Float,
    modifier: Modifier = Modifier,
    warpColor: Color = Color(0xFF8CF0FF),
    weftColor: Color = Color(0xFFA78BFF),
    knotColor: Color = Color(0xFFFFD9A0),
) {
    Canvas(modifier = modifier) {
        drawLoomMark(progress, warpColor, weftColor, knotColor)
    }
}

private fun DrawScope.drawLoomMark(
    progress: Float,
    warpColor: Color,
    weftColor: Color,
    knotColor: Color,
) {
    val unit = size.minDimension / 108f
    fun p(x: Float, y: Float) = Offset(x * unit, y * unit)

    // 经线：自上而下生长
    val warpT = (progress / 0.45f).coerceIn(0f, 1f)
    if (warpT > 0f) {
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(warpColor, Color(0xFF5A87FF)),
                startY = 34f * unit,
                endY = 70f * unit,
            ),
            start = p(44f, 34f),
            end = p(44f, 34f + (69f - 34f) * warpT),
            strokeWidth = 9.5f * unit,
            cap = StrokeCap.Round,
        )
    }

    // 纬线：自左而右折出，构成 L 的横笔
    val weftT = ((progress - 0.35f) / 0.45f).coerceIn(0f, 1f)
    if (weftT > 0f) {
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(weftColor, Color(0xFF6B57E0)),
                startX = 44f * unit,
                endX = 76f * unit,
            ),
            start = p(44f, 69f),
            end = p(44f + (76f - 44f) * weftT, 69f),
            strokeWidth = 9.5f * unit,
            cap = StrokeCap.Round,
        )
    }

    // 穿过经线的纬纱
    val yarnT = ((progress - 0.6f) / 0.25f).coerceIn(0f, 1f)
    if (yarnT > 0f) {
        drawLine(
            color = weftColor.copy(alpha = 0.55f),
            start = p(33f, 50f),
            end = p(33f + (58f - 33f) * yarnT, 50f),
            strokeWidth = 3.2f * unit,
            cap = StrokeCap.Round,
        )
    }

    // 结点
    val knotT = ((progress - 0.72f) / 0.28f).coerceIn(0f, 1f)
    if (knotT > 0f) {
        val radius = 5.2f * unit * (0.6f + 0.4f * knotT)
        drawCircle(
            color = knotColor.copy(alpha = 0.18f * knotT),
            radius = radius * 2.6f,
            center = p(44f, 50f),
        )
        drawCircle(
            color = knotColor.copy(alpha = knotT),
            radius = radius,
            center = p(44f, 50f),
        )
        drawCircle(
            color = knotColor.copy(alpha = 0.4f * knotT),
            radius = radius * 1.7f,
            center = p(44f, 50f),
            style = Stroke(width = 1.2f * unit),
        )
    }
}
