package me.weishu.kernelsu.ui.screen.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.rebootlistpopup.getRebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.rememberRebootAction
import me.weishu.kernelsu.ui.theme.glassTokens

/**
 * Glass 电源面板 —— 与 Glass 设计语言一致：
 *   毛玻璃面板（白玻璃/深玻璃 + 1dp 高光描边 + 24dp 柔投影）
 *   顶部霓虹渐变圆形图标 + 呼吸光环
 *   主操作「重启」为整行渐变胶囊，其余为两列玻璃胶囊
 *   底部半透明「取消」
 * 进入时面板做 0.94→1 缩放 + 淡入。
 */
@Composable
fun GlassRebootDialog(onDismiss: () -> Unit) {
    val t = glassTokens()
    val options = getRebootListOption()
    val doReboot = rememberRebootAction()
    val primary = options.firstOrNull()
    val others = options.drop(1)

    val enter = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(260)) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val s = 0.94f + 0.06f * enter.value
                    scaleX = s
                    scaleY = s
                    alpha = enter.value
                }
                .shadow(
                    24.dp,
                    RoundedCornerShape(28.dp),
                    ambientColor = t.glassShadow,
                    spotColor = t.glassShadow,
                )
                .clip(RoundedCornerShape(28.dp))
                .background(if (t.isDark) Color(0xF714141E) else Color(0xF7FFFFFF))
                .border(
                    1.dp,
                    if (t.isDark) Color(0x1FFFFFFF) else Color(0xFFFFFFFF),
                    RoundedCornerShape(28.dp),
                )
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 霓虹图标 + 呼吸光环
            Box(contentAlignment = Alignment.Center) {
                val hud = rememberInfiniteTransition(label = "reboot")
                val breath by hud.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
                    label = "breath",
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            val s = 1f + breath * 0.3f
                            scaleX = s
                            scaleY = s
                            alpha = (1f - breath) * 0.45f
                        }
                        .border(
                            1.5.dp,
                            Brush.sweepGradient(listOf(t.flame, t.violet, t.cyan, t.flame)),
                            CircleShape,
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(
                            14.dp,
                            CircleShape,
                            ambientColor = t.flame.copy(alpha = 0.32f),
                            spotColor = t.violet.copy(alpha = 0.32f),
                        )
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(t.flame, t.violet))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_d_power2),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.reboot),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = t.ink,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.glass_reboot_hint),
                fontSize = 11.sp,
                color = t.sub,
            )
            Spacer(Modifier.height(18.dp))

            // 主操作：重启（整行渐变胶囊）
            if (primary != null) {
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120), label = "press")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .shadow(
                            12.dp,
                            RoundedCornerShape(99.dp),
                            ambientColor = t.flame.copy(alpha = 0.35f),
                            spotColor = t.violet.copy(alpha = 0.35f),
                        )
                        .clip(RoundedCornerShape(99.dp))
                        .background(Brush.horizontalGradient(listOf(t.flame, t.violet)))
                        .clickable(interactionSource = interaction, indication = null) {
                            onDismiss()
                            doReboot(primary.reason)
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_d_refresh),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(primary.labelRes),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 其余选项：两列玻璃胶囊
            others.chunked(2).forEach { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    chunk.forEachIndexed { i, option ->
                        val accent = when (i) {
                            0 -> t.amber
                            else -> t.cyan
                        }
                        GlassOptionChip(
                            modifier = Modifier.weight(1f),
                            label = stringResource(option.labelRes),
                            accent = accent,
                            onClick = {
                                onDismiss()
                                doReboot(option.reason)
                            },
                        )
                    }
                    if (chunk.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(android.R.string.cancel),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = t.faint,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 10.dp),
            )
        }
    }
}

/** 玻璃选项胶囊：白色玻璃底 + 同色小圆点 + 标签，按下轻微缩放 */
@Composable
private fun GlassOptionChip(
    modifier: Modifier = Modifier,
    label: String,
    accent: Color,
    onClick: () -> Unit,
) {
    val t = glassTokens()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(120), label = "chip")
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(if (t.isDark) Color(0x0FFFFFFF) else Color(0xB3FFFFFF))
            .border(1.dp, if (t.isDark) t.glassBorder else Color(0xE6FFFFFF), RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = t.ink2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
