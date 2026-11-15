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
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.theme.BackgroundEffect
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.drawKuaiMaoEffect

/**
 * 背景特效选择器：四列网格，一次看全所有特效。
 *
 * 之前是左右翻页的大卡：一次只看得见一张，两边还各露出半张被缩小、
 * 被降低透明度的卡，配上底部那条进度条，整块区域很碎。现在改成等宽小格：
 * 每格一个实时预览 + 名称，选中的那格描主题色边、右上角一个对勾，
 * 所有特效一眼能横向比较，也不用滑。
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                // 最后一行不满四格时补空位，保证每格等宽
                repeat(COLUMNS - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private const val COLUMNS = 4

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
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(shape)
                .background(KuaiMaoColors.canvasSubtle)
                .border(
                    width = if (selected) 1.6.dp else 0.8.dp,
                    color = if (selected) accent else KuaiMaoColors.line,
                    shape = shape,
                )
                .clickable(onClick = onClick),
        ) {
            val previewDark = KuaiMaoColors.canvasSubtle.luminance() < 0.5f
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawKuaiMaoEffect(
                    effect = effect,
                    accent = accent,
                    time = time,
                    beat = beat,
                    amplitude = 0.9f,
                    dark = previewDark,
                )
            }

            if (effect == BackgroundEffect.NONE) {
                Text(
                    text = "关闭",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = KuaiMaoColors.textMuted,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Check,
                        contentDescription = null,
                        tint = if (accent.luminance() < 0.55f) Color.White else Color(0xFF101318),
                        modifier = Modifier.size(9.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = effect.label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else KuaiMaoColors.textTer,
            maxLines = 1,
            softWrap = false,
        )
    }
}
