package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.rebootlistpopup.getRebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.rememberRebootAction

/**
 * 重启弹层 —— 严格对照 reboot.png：
 * 遮罩 blur + sheet 渐变 (180deg #171A25→#0E1016) r30/30/0/0 顶描边 violet 30%，
 * grab 条 → POWER · 电源眉题 + 「重启」+ 右侧提示 →
 * rbpri 主项（44 r15 渐变紫图标 + 「重启设备 / reboot · 立即执行」+ 箭头）→
 * rbgrid 2×2（软重启 / Recovery / Bootloader / Download）→ rbdanger EDL → rbcancel。
 */
@Composable
fun NcRebootSheet(onDismiss: () -> Unit) {
    val t = ncTokens()
    val options = getRebootListOption()
    val doReboot = rememberRebootAction()
    val primary = options.firstOrNull()
    val others = options.drop(1)
    val edl = others.lastOrNull { it.reason == "edl" }
    val grid = others.filter { it !== edl }

    val show = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        show.animateTo(1f, tween(420, easing = CubicBezierEasing(0.2f, 1.05f, 0.35f, 1f)))
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
    Box(Modifier.fillMaxSize()) {
        // 遮罩
        Box(
            Modifier
                .fillMaxSize()
                .background(t.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset(y = (30 * (1f - show.value)).dp)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Brush.verticalGradient(listOf(t.sheetTop, t.sheetBottom)))
                .border(1.dp, t.violet.copy(alpha = 0.30f), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 26.dp)
        ) {
            // grab
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(36.dp, 4.dp)
                    .clip(CircleShape)
                    .background(t.grab)
            )
            Spacer(Modifier.height(16.dp))
            // 眉题
            Text("POWER · 电源", fontSize = 10.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold, color = t.violet)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("重启", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = t.text1)
                Spacer(Modifier.weight(1f))
                Text(
                    "长按电源键 3 秒\n可直接强制重启",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = t.text3,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
            Spacer(Modifier.height(12.dp))

            // 主项 rbpri
            if (primary != null) {
                val src = remember { MutableInteractionSource() }
                val pressed by src.collectIsPressedAsState()
                val sc by androidx.compose.animation.core.animateFloatAsState(
                    if (pressed) 0.985f else 1f,
                    androidx.compose.animation.core.spring(0.6f),
                    label = "pri",
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .scale(sc)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(t.violet.copy(alpha = 0.18f), t.violet.copy(alpha = 0.04f), t.mint.copy(alpha = 0.05f))
                            )
                        )
                        .border(1.dp, t.violet.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .clickable(interactionSource = src, indication = null) {
                            onDismiss(); doReboot(primary.reason)
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Brush.linearGradient(listOf(t.violet, t.violetDeep))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.nc_power), null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text("重启设备", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(3.dp))
                        Text("reboot · 立即执行", fontSize = 9.sp, fontFamily = ncMono, color = t.violetSoft)
                    }
                    Icon(
                        painterResource(R.drawable.nc_chev),
                        null,
                        tint = t.violetText,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // 2×2 网格
            Spacer(Modifier.height(10.dp))
            val cells = grid.take(4)
            cells.chunked(2).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    rowItems.forEach { opt ->
                        RbCell(
                            title = stringResource(opt.labelRes),
                            sub = opt.reason,
                            modifier = Modifier.weight(1f),
                            onClick = { onDismiss(); doReboot(opt.reason) },
                        )
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(9.dp))
            }

            // EDL
            if (edl != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(t.red.copy(alpha = 0.07f))
                        .border(1.dp, t.red.copy(alpha = 0.24f), RoundedCornerShape(16.dp))
                        .clickable { onDismiss(); doReboot(edl.reason) }
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(t.red.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.nc_bolt), null, tint = t.dangerIcon, modifier = Modifier.size(15.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("EDL 深度刷机模式", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = t.dangerText)
                        Spacer(Modifier.height(2.dp))
                        Text("qualcomm 9008 · 仅救砖时使用", fontSize = 8.5.sp, color = t.dangerSub)
                    }
                    Icon(painterResource(R.drawable.nc_chev), null, tint = t.dangerChev, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.height(10.dp))
            }

            // 取消
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, t.ghostLine, RoundedCornerShape(14.dp))
                    .clickable { onDismiss() }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("取 消", fontSize = 12.sp, letterSpacing = 3.sp, color = t.text2)
            }
        }
    }
    }
}

/** rbgrid 单元：r16 bg 白4% border line，图标 30 r10，标题 12.5/700，副 mono 8.5 tx3。 */
@Composable
private fun RbCell(title: String, sub: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val t = ncTokens()
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by androidx.compose.animation.core.animateFloatAsState(
        if (pressed) 0.97f else 1f,
        androidx.compose.animation.core.spring(0.6f),
        label = "cell",
    )
    Column(
        modifier
            .scale(sc)
            .clip(RoundedCornerShape(16.dp))
            .background(t.softFill)
            .border(1.dp, t.line, RoundedCornerShape(16.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(t.softFill2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(rebootIcon(sub)), null, tint = t.iconTint, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = t.text1)
        Spacer(Modifier.height(2.dp))
        Text(sub, fontSize = 8.5.sp, fontFamily = ncMono, color = t.text3)
    }
}

private fun rebootIcon(reason: String): Int = when {
    reason.contains("recovery", true) -> R.drawable.nc_recovery
    reason.contains("bootloader", true) || reason.contains("fastboot", true) -> R.drawable.nc_boot
    reason.contains("download", true) || reason.contains("odin", true) -> R.drawable.nc_download
    reason.contains("soft", true) -> R.drawable.nc_soft
    else -> R.drawable.nc_refresh
}
