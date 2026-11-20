package com.kuaimaosu.manager.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones

/**
 * 提权成功提示弹窗。
 *
 * 临时 root 是往「当前已运行的进程」里注入的，KuaiMaoSU 自身在提权前就已启动，
 * 因此它进程内的「是否已 root」状态不会随提权刷新 —— 必须重启一次 KuaiMaoSU，
 * 让它以新的 root 环境重新初始化，管理器的 root 状态才会正确显示。
 * 这个弹窗就是把这件事明确告诉用户，并给出「立即重启」和「稍后」两个出口。
 */
@Composable
fun RootSuccessDialog(
    visible: Boolean,
    onRestart: () -> Unit,
    onLater: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) progress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 420f))
        else progress.animateTo(0f, tween(durationMillis = 160))
    }
    if (!visible && progress.value <= 0.001f) return

    val shape = RoundedCornerShape(26.dp)
    val accent = KuaiMaoTones.green.content
    val onAccent = if (accent.luminance() < 0.62f) Color.White else Color(0xFF101013)
    val btnStart = lerp(accent, Color.White, 0.10f)
    val btnEnd = lerp(accent, onAccent, 0.28f)

    Box(modifier = Modifier.fillMaxSize()) {
        // 暗幕：吞掉触摸，必须明确选择重启或稍后
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value * 0.66f }
                .background(Color.Black)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        }
                    }
                },
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 380.dp)
                .graphicsLayer {
                    val e = progress.value
                    alpha = e
                    val s = 0.90f + 0.10f * e
                    scaleX = s
                    scaleY = s
                    translationY = (1f - e) * 20.dp.toPx()
                }
                .clip(shape)
                .background(KuaiMaoColors.canvasCardSolid)
                .border(width = 1.dp, color = KuaiMaoColors.line.copy(alpha = 0.55f), shape = shape),
        ) {
            // 顶部深色 hero：成功徽标
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                KuaiMaoTones.green.container,
                                KuaiMaoColors.canvasCardSolid,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f))
                        .border(width = 2.dp, color = accent.copy(alpha = 0.55f), shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.TwoTone.CheckCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(46.dp),
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) {
                Text(
                    text = "提权成功",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = KuaiMaoColors.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "CVE-2026-98260 · 临时 root 已就绪",
                    fontSize = 11.5.sp,
                    color = KuaiMaoTones.green.content,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "需要重启 KuaiMaoSU",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = KuaiMaoColors.textBody,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "本次提权是向正在运行的进程注入 root，KuaiMaoSU 自身在此之前就已启动，" +
                        "所以当前界面还不会显示 root 状态。重启一次 KuaiMaoSU，让它以新的 root 环境" +
                        "重新初始化，管理器状态就会正确显示。临时 root 重启后依然有效（重启手机才失效）。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = KuaiMaoColors.textMuted,
                    textAlign = TextAlign.Start,
                )

                Spacer(Modifier.height(18.dp))
                // 主按钮：立即重启
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(btnStart, accent, btnEnd)))
                        .clickable(interactionSource = interaction, indication = null, onClick = onRestart),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.TwoTone.RestartAlt,
                            contentDescription = null,
                            tint = onAccent,
                            modifier = Modifier.size(19.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "立即重启 KuaiMaoSU",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = onAccent,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                // 次按钮：稍后
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onLater,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "稍后手动重启",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = KuaiMaoColors.textMuted,
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}
