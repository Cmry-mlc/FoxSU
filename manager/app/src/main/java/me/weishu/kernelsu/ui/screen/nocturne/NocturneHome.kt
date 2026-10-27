package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.screen.glass.GlassStylePickerDialog
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.screen.home.HomeActions
import me.weishu.kernelsu.ui.screen.home.HomeUiState

/**
 * 主页 —— 严格对照视觉稿 s1：
 * KERNEL MANAGER 眉题 + FoxSu 大标题 → Hero 卡（● RUNNING · 越狱模式 / 大字「工作中」+ LKM /
 * 右上 56 环形+对勾 / VERSION·MODE·TEMP 三列）→ DEVICE · 设备 → SECURITY · 安全 → BUILD · 内核与指纹。
 */
@Composable
fun NocturneHomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
    navigationBadge: NavigationBadgeState,
    onOpenTab: (Int) -> Unit,
    onOpenAuthor: () -> Unit,
) {
    val t = ncTokens()
    val ok = state.ksuVersion != null
    var showReboot by remember { mutableStateOf(false) }
    var showStylePicker by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        NcGlowBackground(t)
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = ncStatusTop() + 2.dp, bottom = bottomInnerPadding),
        ) {
            // 页头固定在顶部（不随内容滚动）
            Box(Modifier.padding(horizontal = 18.dp)) {
                NcHeader(kicker = "KERNEL MANAGER", title = "FoxSu") {
                    NcIconButton(iconRes = R.drawable.nc_user, onClick = onOpenAuthor)
                    NcIconButton(iconRes = R.drawable.nc_palette, onClick = { showStylePicker = true })
                    NcIconButton(iconRes = R.drawable.nc_power, onClick = { showReboot = true })
                }
            }
            // 仅内容滚动
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, bottom = 86.dp),
            ) {
                NcHero(state = state, ok = ok)
            Spacer(Modifier.height(10.dp))

            NcSectionLabel("DEVICE · 设备")
            NcGroupCard {
                NcRow(
                    iconRes = R.drawable.nc_phone,
                    title = stringResource(R.string.home_device_model),
                    subtitle = state.systemInfo.deviceModel.ifBlank { "—" },
                    accent = NcAccent.Violet,
                )
            }

            NcSectionLabel("SECURITY · 安全")
            NcGroupCard {
                NcRow(
                    iconRes = R.drawable.nc_shield,
                    title = "SELinux",
                    subtitle = "安全增强策略已启用",
                    accent = NcAccent.Mint,
                    trailing = {
                        Text(
                            text = if (state.isSELinuxPermissive) "宽容" else "强制",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isSELinuxPermissive) t.amber else t.mint,
                        )
                    },
                )
                NcRow(
                    iconRes = R.drawable.nc_filter,
                    title = "Seccomp",
                    subtitle = "系统调用过滤已启用",
                    accent = NcAccent.Default,
                    showDivider = true,
                    trailing = {
                        Text("Filter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = t.text2)
                    },
                )
                NcRow(
                    iconRes = R.drawable.nc_heart,
                    title = "支持开发",
                    subtitle = "KernelSU 保持免费开源，欢迎捐赠",
                    accent = NcAccent.Red,
                    showDivider = true,
                    onClick = onOpenAuthor,
                    trailing = { NcChevron() },
                )
            }

            NcSectionLabel("BUILD · 内核与指纹")
            NcCodeCard(
                kicker = "KERNEL · 内核版本",
                value = state.systemInfo.kernelVersion.ifBlank { "—" },
                tag = "6.6 KMI",
                iconRes = R.drawable.nc_terminal,
            )
            Spacer(Modifier.height(7.dp))
            NcCodeCard(
                kicker = "FINGERPRINT · 指纹信息",
                value = state.systemInfo.fingerprint.ifBlank { "—" },
                tag = "RO",
                iconRes = R.drawable.nc_lock,
            )
            Spacer(Modifier.height(10.dp))
            }
        }
    }

    if (showReboot) NcRebootSheet(onDismiss = { showReboot = false })
    if (showStylePicker) GlassStylePickerDialog(current = LocalUiMode.current, onDismiss = { showStylePicker = false })
}

/**
 * Hero 状态卡：r24 渐变描边 + r23 内层 + 右上 180 光晕。
 */
@Composable
private fun NcHero(state: HomeUiState, ok: Boolean) {
    val t = ncTokens()
    val breath = rememberInfiniteTransition(label = "hero")
    val dot by breath.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "dot",
    )
    NcGradientBorder(radius = 24.dp) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.size(180.dp).align(Alignment.TopEnd)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(t.violet.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width * 0.6f, size.height * 0.2f),
                        radius = size.width * 0.55f,
                    ),
                    radius = size.width / 2,
                    center = center,
                )
            }
            Column(Modifier.padding(start = 17.dp, end = 17.dp, top = 15.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .alpha(dot)
                            .clip(CircleShape)
                            .background(if (ok) t.mint else t.red)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (ok) "RUNNING · 越狱模式" else "STOPPED · 未安装",
                        fontSize = 9.5.sp,
                        letterSpacing = 2.6.sp,
                        color = t.text2,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (ok) "工作中" else "未安装",
                        fontSize = 29.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.2).sp,
                        color = t.text1,
                    )
                    if (ok) {
                        Spacer(Modifier.width(9.dp))
                        Box(
                            Modifier
                                .offset(y = (-4).dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(t.mint.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (state.lkmMode == true) "LKM" else "GKI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = t.mint,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(11.dp))
                Canvas(Modifier.fillMaxWidth().height(1.dp)) { drawRect(t.line) }
                Spacer(Modifier.height(9.dp))
                Row {
                    HeroMeta("VERSION", state.ksuVersion?.let { "v$it" } ?: "—", t.text1, Modifier.weight(1f))
                    HeroMeta("MODE", if (state.lkmMode == true) "LKM" else "GKI", t.mint, Modifier.weight(1f))
                    HeroMeta("TEMP", "38°C", t.text1, Modifier.weight(1f))
                }
            }
            NcHeroRing(
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 17.dp, top = 15.dp),
                ok = ok,
            )
        }
    }
}

@Composable
private fun HeroMeta(k: String, v: String, valueColor: Color, modifier: Modifier = Modifier) {
    val t = ncTokens()
    Column(modifier) {
        Text(k, fontSize = 9.sp, letterSpacing = 1.6.sp, color = t.text3, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(v, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp, color = valueColor)
    }
}

/** 56dp 环 + 中心对勾。 */
@Composable
private fun NcHeroRing(modifier: Modifier = Modifier, ok: Boolean) {
    val t = ncTokens()
    Box(modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2
            drawArc(
                color = if (ok) t.mint.copy(alpha = 0.16f) else t.red.copy(alpha = 0.14f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(if (ok) t.mint else t.red, t.violet, if (ok) t.mint else t.red)
                ),
                startAngle = -90f, sweepAngle = if (ok) 360f else 120f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Icon(
            painter = painterResource(R.drawable.nc_check),
            contentDescription = null,
            tint = if (ok) t.mint else t.red,
            modifier = Modifier.size(20.dp),
        )
    }
}
