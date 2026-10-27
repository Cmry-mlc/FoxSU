package me.weishu.kernelsu.ui.screen.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.rebootlistpopup.getRebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.rememberRebootAction
import me.weishu.kernelsu.ui.theme.glassTokens

/**
 * Glass 重启面板 —— 电源键弹出的真正重启界面（不是刷写镜像页）。
 * 选项来自 getRebootListOption()：重启 / 用户空间重启 / 软重启 / Recovery / Bootloader / Download / EDL，
 * 首项为主操作（霓虹渐变胶囊），其余为玻璃行。
 */
@Composable
fun GlassRebootDialog(onDismiss: () -> Unit) {
    val t = glassTokens()
    val options = getRebootListOption()
    val doReboot = rememberRebootAction()
    val primary = options.firstOrNull()
    val others = options.drop(1)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    24.dp,
                    RoundedCornerShape(28.dp),
                    ambientColor = t.glassShadow,
                    spotColor = t.glassShadow,
                )
                .clip(RoundedCornerShape(28.dp))
                .background(if (t.isDark) Color(0xF216161F) else Color(0xF7FFFFFF))
                .border(1.dp, t.glassBorder, RoundedCornerShape(28.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 图标：霓虹渐变圆
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(
                        12.dp,
                        CircleShape,
                        ambientColor = t.flame.copy(alpha = 0.35f),
                        spotColor = t.violet.copy(alpha = 0.35f),
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
            Spacer(Modifier.height(12.dp))
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
            Spacer(Modifier.height(16.dp))

            // 主操作：重启
            if (primary != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(t.flame, t.violet)))
                        .clickable {
                            onDismiss()
                            doReboot(primary.reason)
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(primary.labelRes),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            // 其余选项：两列玻璃行
            others.chunked(2).forEach { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    chunk.forEach { option ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(t.chipBg)
                                .border(1.dp, t.chipBorder, RoundedCornerShape(14.dp))
                                .clickable {
                                    onDismiss()
                                    doReboot(option.reason)
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(option.labelRes),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = t.ink2,
                                maxLines = 1,
                            )
                        }
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
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}
