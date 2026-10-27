package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 底部弹层容器 .sheet —— 严格对照设计稿：
 * · r30/30/0/0
 * · 渐变底 rgba(24,27,38,.96) → rgba(10,12,17,.98)
 * · 顶部 violet 35% 描边
 * · 顶部 150px 紫色光晕 radial-gradient(320px 120px at 50% -30%, rgba(139,123,255,.35), transparent 70%)
 * · grab 指示条
 * · 出场：translateY 30 → 0，420ms，cubic-bezier(.2,1.05,.35,1)；遮罩 250ms
 */
@Composable
fun NcBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = ncTokens()
    val show = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        show.animateTo(1f, tween(420, easing = CubicBezierEasing(0.2f, 1.05f, 0.35f, 1f)))
    }

    Box(Modifier.fillMaxSize()) {
        // 遮罩 .ovl
        Box(
            Modifier
                .fillMaxSize()
                .background(t.scrim.copy(alpha = t.scrim.alpha * show.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onDismiss() }
        )

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset(y = (30 * (1f - show.value)).dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(Brush.verticalGradient(listOf(t.sheetTop, t.sheetBottom)))
                    .border(
                        1.dp,
                        t.sheetBorder,
                        RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                    ),
            ) {
                // 顶部 150px 紫色光晕
                Box(Modifier.fillMaxWidth().height(150.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.radialGradient(
                                0.0f to t.sheetTopGlow,
                                0.7f to Color.Transparent,
                                1.0f to Color.Transparent,
                                center = Offset(size.width / 2f, -30f),
                                radius = 320f,
                            )
                        )
                    }
                }
                // grab
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .offset(y = (-6).dp)
                        .size(36.dp, 4.dp)
                        .clip(CircleShape)
                        .background(t.grab)
                )
                Column(
                    Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 26.dp),
                    content = content,
                )
            }
        }
    }
}
