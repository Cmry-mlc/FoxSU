package com.kuaimaosu.manager.ui.screen.themeSettings.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Block
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.theme.BackgroundEffect
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.drawKuaiMaoEffect

/**
 * 背景特效选择器：三列大格，每格都是一块真的在动的预览。
 *
 * 上一版是四列 62dp 的小格：特效本身是细线、粒子、光点，缩到那么小基本看不出差别，
 * 名字也被挤成 10sp 的一行。现在每格加高到 92dp、三列排布，名称压在预览底部，
 * 选中的那格描主题色边并在右上角打勾——一眼能比较出「猫爪 / 星尘 / 流星」的区别。
 */
@Composable
fun BackgroundEffectPicker(
    selected: BackgroundEffect,
    onSelect: (BackgroundEffect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = BackgroundEffect.entries
    val accent = KuaiMaoColors.brand

    // 所有格子共用一条时间轴：只有一套动画在跑，预览还是动的
    val transition = rememberInfiniteTransition(label = "effectPicker")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pickerTime",
    )
    val beat by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pickerBeat",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        entries.chunked(COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { effect ->
                    EffectTile(
                        effect = effect,
                        selected = effect == selected,
                        accent = accent,
                        time = time,
                        beat = beat,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(effect) },
                    )
                }
                // 最后一行不满三格时补空位，保证每格等宽
                repeat(COLUMNS - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private const val COLUMNS = 3

@Composable
private fun EffectTile(
    effect: BackgroundEffect,
    selected: Boolean,
    accent: Color,
    time: Float,
    beat: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val previewDark = KuaiMaoColors.canvasSubtle.luminance() < 0.5f
    val labelColor = if (previewDark) Color.White else Color(0xFF101318)
    val closing = effect == BackgroundEffect.NONE

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(shape)
            .background(KuaiMaoColors.canvasSubtle)
            .border(
                width = if (selected) 1.8.dp else 0.8.dp,
                color = if (selected) accent else KuaiMaoColors.line,
                shape = shape,
            )
            .clickable(onClick = onClick),
    ) {
        if (!closing) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawKuaiMaoEffect(
                    effect = effect,
                    accent = accent,
                    time = time,
                    beat = beat,
                    amplitude = 0.95f,
                    dark = previewDark,
                )
            }
        }

        // 名称压在最底部：先铺一层由透明到画布色的渐变，字才不会被特效的线条穿过。
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(34.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            if (previewDark) Color.Black.copy(alpha = 0.55f)
                            else Color.White.copy(alpha = 0.82f),
                        ),
                    ),
                ),
        )

        if (closing) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Block,
                    contentDescription = null,
                    tint = KuaiMaoColors.textTer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Text(
            text = effect.label,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            color = if (selected) accent else labelColor.copy(alpha = 0.92f),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
        )

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(15.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Check,
                    contentDescription = null,
                    tint = if (accent.luminance() < 0.55f) Color.White else Color(0xFF101318),
                    modifier = Modifier.size(10.dp),
                )
            }
        }
    }
}
