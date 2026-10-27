package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.rebootlistpopup.getRebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.rememberRebootAction

/**
 * 重启弹层 —— 严格对照设计稿 ⑤ 重启菜单：
 * grab → POWER · 电源眉题 + 「重启」+ 右侧提示 →
 * rbpri 主项（44 r15 渐变紫图标 + 标题 / reason 副标题 + 箭头，右侧 150px 紫色光晕）→
 * rbgrid 2×2 → rbdanger EDL → rbcancel。
 *
 * 选项与副标题全部来自系统真实的 getRebootListOption()，点击即执行对应重启。
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

    NcBottomSheet(onDismiss = onDismiss) {
        // 眉题
        Text("POWER · 电源", fontSize = 8.5.sp, letterSpacing = 2.8.sp, fontWeight = FontWeight.Bold, color = t.rbKicker)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("重启", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = t.tx)
            Spacer(Modifier.weight(1f))
            Text(
                "长按电源键 3 秒\n可直接强制重启",
                fontSize = 8.5.sp,
                lineHeight = 13.sp,
                color = t.rbHint,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
            )
        }
        Spacer(Modifier.height(14.dp))

        // 主项 rbpri
        if (primary != null) {
            RbPrimary(
                title = stringResource(primary.labelRes),
                reason = primary.reason.ifEmpty { "reboot" },
                onClick = { onDismiss(); doReboot(primary.reason) },
            )
        }

        // 2×2 网格
        if (grid.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            grid.chunked(2).forEach { rowItems ->
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
        }

        // EDL rbdanger
        if (edl != null) {
            RbDanger(
                title = stringResource(edl.labelRes),
                sub = edl.reason,
                onClick = { onDismiss(); doReboot(edl.reason) },
            )
            Spacer(Modifier.height(9.dp))
        }

        // 取消
        NcCancelButton("取 消", onDismiss)
    }
}

/** rbpri —— r20 pad14，violet 渐变底 + 45% 描边 + 右侧 150px 光晕。 */
@Composable
private fun RbPrimary(title: String, reason: String, onClick: () -> Unit) {
    val t = ncTokens()
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) 0.985f else 1f,
        spring(0.6f),
        label = "pri",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .scale(sc)
            .clip(RoundedCornerShape(20.dp))
    ) {
        // 右侧 150px 紫色光晕
        Box(
            Modifier
                .size(150.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-70).dp)
        ) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to t.rbPriGlow,
                        0.65f to t.rbPriGlow.copy(alpha = t.rbPriGlow.alpha * 0.18f),
                        1.0f to Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension / 2f,
                    )
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(t.rbPriBg))
                .border(1.dp, t.rbPriBorder, RoundedCornerShape(20.dp))
                .clickable(interactionSource = src, indication = null, onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(t.vio, t.vio2))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.nc_power), null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = t.rbPriText)
                Spacer(Modifier.height(3.dp))
                Text(reason, fontSize = 9.sp, fontFamily = ncMono, color = t.rbPriSub)
            }
            Icon(
                painterResource(R.drawable.nc_chev_big),
                null,
                tint = t.rbPriArrow,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** rbgrid 单元：r16 bg 白4% border line，图标 30 r10，标题 12.5/700，副 mono 8.5 tx3。 */
@Composable
private fun RbCell(title: String, sub: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val t = ncTokens()
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        spring(0.6f),
        label = "cell",
    )
    Column(
        modifier
            .scale(sc)
            .clip(RoundedCornerShape(16.dp))
            .background(t.rbCellBg)
            .border(1.dp, t.rbCellBorder, RoundedCornerShape(16.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(t.rbCellIconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(rebootIcon(sub)), null, tint = t.rbCellIconFg, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = t.rbCellTitle, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(sub, fontSize = 8.5.sp, fontFamily = ncMono, color = t.rbCellSub, maxLines = 1)
    }
}

/** rbdanger —— 红 7% 底 + 24% 描边 + 红 14% 图标底。 */
@Composable
private fun RbDanger(title: String, sub: String, onClick: () -> Unit) {
    val t = ncTokens()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(t.rbDangerBg)
            .border(1.dp, t.rbDangerBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(t.rbDangerIconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.nc_bolt), null, tint = t.rbDangerIconFg, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = t.rbDangerTitle, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(sub, fontSize = 8.5.sp, fontFamily = ncMono, color = t.rbDangerSub, maxLines = 1)
        }
        Icon(painterResource(R.drawable.nc_chev), null, tint = t.rbDangerArrow, modifier = Modifier.size(15.dp))
    }
}

private fun rebootIcon(reason: String): Int = when {
    reason.contains("recovery", true) -> R.drawable.nc_recovery
    reason.contains("bootloader", true) || reason.contains("fastboot", true) -> R.drawable.nc_boot
    reason.contains("download", true) || reason.contains("odin", true) -> R.drawable.nc_download
    reason.contains("soft", true) -> R.drawable.nc_soft
    else -> R.drawable.nc_refresh
}
