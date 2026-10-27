package me.weishu.kernelsu.ui.screen.nocturne

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.screen.home.HomeActions
import me.weishu.kernelsu.ui.screen.home.HomeUiState

/**
 * 主页 —— 严格对照视觉稿 ① 主页：
 * KERNEL MANAGER 眉题 + FoxSu 大标题 → Hero 卡（● RUNNING · 越狱模式 / 大字「工作中」+ LKM 徽章 /
 * 右上 56 环形+对勾 / VERSION·MODE·UAPI 三列）→ DEVICE · 设备 → SECURITY · 安全 → BUILD · 内核与指纹。
 *
 * 所有数值均取自真实运行状态，无任何设计稿假数据。
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
                        title = "设备型号",
                        subtitle = state.systemInfo.deviceModel.ifBlank { Build.MODEL },
                        accent = NcAccentTone.Violet,
                        trailing = {
                            Text(
                                text = Build.MODEL,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = t.tx2,
                            )
                        },
                    )
                    NcRow(
                        iconRes = R.drawable.nc_chip,
                        title = "处理器内核",
                        subtitle = state.systemInfo.kernelVersion.ifBlank { "—" },
                        accent = NcAccentTone.Default,
                        showDivider = true,
                    )
                }

                NcSectionLabel("SECURITY · 安全")
                NcGroupCard {
                    NcRow(
                        iconRes = R.drawable.nc_lock,
                        title = "SELinux",
                        subtitle = if (state.isSELinuxPermissive) "安全策略处于宽容模式" else "安全增强策略已启用",
                        accent = NcAccentTone.Mint,
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
                        subtitle = if (state.systemInfo.seccompStatus >= 0) "系统调用过滤已开启" else "系统调用过滤状态未知",
                        accent = NcAccentTone.Default,
                        showDivider = true,
                        trailing = {
                            Text(
                                text = if (state.systemInfo.seccompStatus >= 0) "Filter" else "未知",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = t.tx2,
                            )
                        },
                    )
                    NcRow(
                        iconRes = R.drawable.nc_heart,
                        title = "支持开发",
                        subtitle = "KernelSU 保持免费开源，欢迎捐赠",
                        accent = NcAccentTone.Red,
                        showDivider = true,
                        onClick = onOpenAuthor,
                        trailing = { NcChevron() },
                    )
                }

                NcSectionLabel("BUILD · 内核与指纹")
                NcCodeCard(
                    kicker = "KERNEL · 内核版本",
                    value = state.systemInfo.kernelVersion.ifBlank { "—" },
                    tag = kernelTag(state.systemInfo.kernelVersion),
                    iconRes = R.drawable.nc_terminal,
                )
                Spacer(Modifier.height(7.dp))
                NcCodeCard(
                    kicker = "FINGERPRINT · 指纹信息",
                    value = state.systemInfo.fingerprint.ifBlank { "—" },
                    tag = fingerprintTag(state.systemInfo.fingerprint),
                    iconRes = R.drawable.nc_lock,
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        // 重启弹层：覆盖整屏（含底栏），放在最外层 Box 最后绘制
        if (showReboot) NcRebootSheet(onDismiss = { showReboot = false })
    }

    if (showStylePicker) {
        me.weishu.kernelsu.ui.screen.glass.GlassStylePickerDialog(
            current = me.weishu.kernelsu.ui.LocalUiMode.current,
            onDismiss = { showStylePicker = false },
        )
    }
}

/** 从真实内核版本推导 KMI 标签，例如 "6.6.118-android15-…" → "6.6 KMI"。 */
private fun kernelTag(kernelVersion: String): String {
    val seg = kernelVersion.substringBefore('-').split('.').filter { it.isNotEmpty() }
    if (seg.size < 2) return "KMI"
    return "${seg[0]}.${seg[1]} KMI"
}

/** 从真实指纹推导签名类型标签。 */
private fun fingerprintTag(fingerprint: String): String = when {
    fingerprint.contains("release-keys") -> "RO"
    fingerprint.contains("test-keys") -> "TEST"
    fingerprint.isBlank() -> "—"
    else -> "USER"
}

/**
 * Hero 状态卡：r24 渐变描边（150deg）+ r23 内层（165deg）+ 右上 180px 紫色透明扩散光晕。
 */
@Composable
private fun NcHero(state: HomeUiState, ok: Boolean) {
    val t = ncTokens()
    NcGradientBorder(
        radius = 24.dp,
        borderColors = t.heroBorder,
        borderStops = t.heroBorderStops,
        borderAngle = 150f,
        cardColors = t.heroCard,
        cardStops = t.heroCardStops,
        cardAngle = 165f,
    ) {
        Box(Modifier.fillMaxWidth()) {
            // 右上角 180px 紫色光晕（向外透明扩散）
            Box(
                Modifier
                    .size(180.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = (-70).dp)
            ) {
                NcHeroGlow(t, Modifier.fillMaxSize())
            }
            Column(Modifier.padding(start = 17.dp, end = 17.dp, top = 15.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NcPulseDot(color = if (ok) t.mint else t.red)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (ok) "RUNNING · 越狱模式" else "STOPPED · 未安装",
                        fontSize = 9.5.sp,
                        letterSpacing = 2.6.sp,
                        color = t.heroStatus,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (ok) "工作中" else "未安装",
                        fontSize = 29.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.2).sp,
                        color = t.heroBig,
                    )
                    if (ok) {
                        Spacer(Modifier.width(9.dp))
                        Box(
                            Modifier
                                .offset(y = (-4).dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(t.heroBadgeBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (state.lkmMode == true) "LKM" else "GKI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = t.heroBadgeText,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(11.dp))
                androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(1.dp)) { drawRect(t.line) }
                Spacer(Modifier.height(9.dp))
                Row {
                    HeroMeta(
                        "VERSION",
                        state.systemInfo.managerVersion.ifBlank { "—" },
                        t.heroBig,
                        Modifier.weight(1f),
                    )
                    HeroMeta(
                        "MODE",
                        if (state.lkmMode == true) "LKM" else "GKI",
                        t.mint,
                        Modifier.weight(1f),
                    )
                    HeroMeta(
                        "UAPI",
                        state.kernelUAPIVersion?.toString() ?: "—",
                        t.heroBig,
                        Modifier.weight(1f),
                    )
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
private fun HeroMeta(k: String, v: String, valueColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val t = ncTokens()
    Column(modifier) {
        Text(k, fontSize = 9.sp, letterSpacing = 1.6.sp, color = t.heroMetaKey, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(
            v,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
            color = valueColor,
            maxLines = 1,
        )
    }
}
