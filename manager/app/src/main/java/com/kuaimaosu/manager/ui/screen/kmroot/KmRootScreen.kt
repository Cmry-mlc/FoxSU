package com.kuaimaosu.manager.ui.screen.kmroot

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Bolt
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Devices
import androidx.compose.material.icons.twotone.ErrorOutline
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material.icons.twotone.ManageAccounts
import androidx.compose.material.icons.twotone.SettingsSuggest
import androidx.compose.material.icons.twotone.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuaimaosu.manager.data.kmroot.KmShizuku
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoPrimaryButton
import com.kuaimaosu.manager.ui.component.KuaiMaoRow
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.screen.main.KuaiMaoSettingsScaffold
import com.kuaimaosu.manager.ui.component.RootSuccessDialog
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.ui.viewmodel.KmRootViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * 一键临时提权页面（自研 98260 引擎）。
 *
 * 通过 Shizuku/Stellar 的 shell 域触发内核 UAF，临时加载 KernelSU，
 * 并用 KuaiMaoSU 自带、内嵌全套 ko 的 ksud 做 late-load。重启后失效。
 */
@Composable
fun KmRootScreen(
    bottomPadding: Dp = 0.dp,
    onBack: () -> Unit = {},
    vm: KmRootViewModel = koinViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showDevices by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.refresh() }

    if (showDevices) {
        SupportedDevicesSheet(
            currentKernel = ui.kernel,
            onDismiss = { showDevices = false },
        )
    }

    // 提权成功后弹出「需重启 KuaiMaoSU」提示（由 resultOk 触发，见下方 RootSuccessDialog）。
    // 临时 root 注入的是已运行进程，APP 自身需重启才能刷新 root 状态显示。

    val shizukuReady = ui.shizukuState == KmShizuku.State.READY
    val headline = when {
        ui.resultOk == true -> "提权成功"
        ui.resultOk == false -> "未成功"
        ui.running -> ui.stage
        shizukuReady -> "就绪"
        else -> "需配置 shell 服务"
    }
    val description = when {
        ui.resultOk == true -> ui.resultMessage
        ui.resultOk == false -> ui.resultMessage
        ui.running -> "引擎正在执行，请勿锁屏或切换应用"
        shizukuReady -> "已获取 shell 域，可发起一键临时提权"
        else -> ui.shizukuText
    }

    Box(modifier = Modifier.fillMaxSize()) {
    KuaiMaoSettingsScaffold(
        title = "一键越狱",
        subtitle = "CVE-2026-98260 · 临时 root · v${com.kuaimaosu.manager.BuildConfig.VERSION_NAME}",
        bottomPadding = bottomPadding,
    ) {
        // 引擎 hero 卡：CVE 徽标 + 当前状态，作为整页视觉重心
        item {
            EngineHero(
                running = ui.running,
                ok = ui.resultOk,
                headline = headline,
                pct = ui.pct,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            KuaiMaoCard {
                KuaiMaoRow(
                    icon = Icons.TwoTone.Lock,
                    title = "引擎状态",
                    description = description,
                    iconTint = when {
                        ui.resultOk == true -> KuaiMaoTones.green.content
                        ui.resultOk == false -> KuaiMaoTones.rose.content
                        shizukuReady -> KuaiMaoTones.violet.content
                        else -> KuaiMaoTones.amber.content
                    },
                    iconContainer = when {
                        ui.resultOk == true -> KuaiMaoTones.green.container
                        ui.resultOk == false -> KuaiMaoTones.rose.container
                        shizukuReady -> KuaiMaoTones.violet.container
                        else -> KuaiMaoTones.amber.container
                    },
                    trailing = {
                        Badge(
                            text = headline,
                            tone = when {
                                ui.resultOk == true -> KuaiMaoTones.green
                                ui.running -> KuaiMaoTones.blue
                                ui.resultOk == false || !shizukuReady -> KuaiMaoTones.amber
                                else -> KuaiMaoTones.green
                            },
                        )
                    },
                )
                if (ui.running || ui.pct > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { ui.pct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(100.dp)),
                        color = KuaiMaoColors.brand,
                        trackColor = KuaiMaoColors.textMuted.copy(alpha = 0.15f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // shell 服务未就绪时，给出配置入口
        if (!shizukuReady) {
            item {
                KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                    KuaiMaoRow(
                        icon = Icons.TwoTone.SettingsSuggest,
                        title = "shell 服务（Shizuku / Stellar）",
                        description = ui.shizukuText,
                        iconTint = KuaiMaoTones.amber.content,
                        iconContainer = KuaiMaoTones.amber.container,
                        onClick = { vm.openShizukuManager() },
                        trailing = {
                            Icon(
                                Icons.TwoTone.ChevronRight,
                                contentDescription = null,
                                tint = KuaiMaoColors.chevron,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                    if (ui.shizukuState == KmShizuku.State.NO_PERMISSION) {
                        KuaiMaoRowDivider()
                        KuaiMaoRow(
                            icon = Icons.TwoTone.CheckCircle,
                            title = "重新发起授权",
                            description = "弹窗点「允许」后回来",
                            iconTint = KuaiMaoTones.blue.content,
                            iconContainer = KuaiMaoTones.blue.container,
                            onClick = { vm.requestPermission() },
                        )
                    }
                }
            }
        }

        // 管理器 / 模式
        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                KuaiMaoRow(
                    icon = Icons.TwoTone.ManageAccounts,
                    title = "ROOT 管理器",
                    description = if (ui.managerInstalled) "已安装，late-load 用本管理器的 ksud" else "未安装（无法 late-load）",
                    iconTint = KuaiMaoTones.violet.content,
                    iconContainer = KuaiMaoTones.violet.container,
                    trailing = {
                        Text(
                            text = ui.managerLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = KuaiMaoColors.text,
                        )
                    },
                )
                KuaiMaoRowDivider()
                KuaiMaoRow(
                    icon = Icons.TwoTone.Bolt,
                    title = "提权模式",
                    description = if (ui.mode == "zero") "zero（只清零，兼容模式）" else "fake（完整 root + 管理器）",
                    iconTint = KuaiMaoTones.blue.content,
                    iconContainer = KuaiMaoTones.blue.container,
                    onClick = { vm.setMode(if (ui.mode == "zero") "fake" else "zero") },
                    trailing = {
                        Text(
                            text = if (ui.mode == "zero") "zero" else "fake",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = KuaiMaoColors.brand,
                        )
                    },
                )
                KuaiMaoRowDivider()
                KuaiMaoRow(
                    icon = Icons.TwoTone.Devices,
                    title = "支持机型",
                    description = if (ui.kernel.isNotBlank())
                        "当前内核 ${ui.kernel.take(24)}${if (ui.kernel.length > 24) "…" else ""} · 点击查看完整清单"
                    else "点击查看 ${SupportedDevices.totalDevices} 款已验证机型与内核",
                    iconTint = KuaiMaoTones.violet.content,
                    iconContainer = KuaiMaoTones.violet.container,
                    onClick = { showDevices = true },
                    trailing = {
                        Icon(
                            Icons.TwoTone.ChevronRight,
                            contentDescription = null,
                            tint = KuaiMaoColors.textMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }

        // 启动 / 停止按钮
        item {
            Spacer(modifier = Modifier.height(16.dp))
            if (ui.running) {
                KuaiMaoPrimaryButton(text = "停止", onClick = { vm.stop() })
            } else {
                KuaiMaoPrimaryButton(
                    text = "一键越狱",
                    enabled = shizukuReady,
                    onClick = { vm.start() },
                )
            }
        }

        // 实时日志（常驻显示，未提权也能看到"复制日志"入口）
        item {
            Spacer(modifier = Modifier.height(14.dp))
            LogConsole(lines = ui.logs)
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "本功能利用内核竞态 UAF（CVE-2026-98260）临时加载 KernelSU，重启即失效。" +
                    "漏洞研究参考 KaMuaMua2020、AzirX1337、YuKongA，特此感谢。" +
                    "仅用于自有设备；引擎按内核精确匹配偏移，不适配时不会强跑。",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = KuaiMaoColors.textMuted,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Spacer(modifier = Modifier.height(bottomPadding + 12.dp))
        }
    }

    // 提权成功提示：需重启 KuaiMaoSU 才会显示 root 状态
    RootSuccessDialog(
        visible = ui.showSuccessDialog,
        onRestart = { vm.restartApp() },
        onLater = { vm.dismissSuccess() },
    )
    }
}

@Composable
private fun LogConsole(lines: List<String>) {
    val state = rememberLazyListState()
    val context = LocalContext.current
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) state.animateScrollToItem(lines.size - 1)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KuaiMaoColors.textMuted.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.TwoTone.Terminal,
                contentDescription = null,
                tint = KuaiMaoColors.textMuted,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "运行日志",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = KuaiMaoColors.textMuted,
                modifier = Modifier.weight(1f),
            )
            // 一键复制全部日志，方便真机跑完直接回传排障。
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(KuaiMaoColors.brand)
                    .clickable {
                        val text = lines.joinToString("\n")
                        if (text.isBlank()) {
                            Toast.makeText(context, "暂无日志", Toast.LENGTH_SHORT).show()
                        } else {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("提权运行日志", text))
                            Toast.makeText(context, "已复制 ${lines.size} 行日志", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.TwoTone.ContentCopy,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "复制日志",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(count = lines.size) { i ->
                Text(
                    text = lines[i],
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = KuaiMaoColors.text,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

/** 主题色锁明度染到深底：只借色相，近白补一点饱和，避免深 hero 里出现浅色块。 */
@Composable
private fun engineTint(value: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(KuaiMaoColors.brand.toArgb(), hsv)
    hsv[2] = value
    hsv[1] = hsv[1].coerceAtLeast(0.18f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** 引擎 hero 卡：整页视觉重心。深色底固定白字，光环用主题色染色，运行时旋转。 */
@Composable
private fun EngineHero(running: Boolean, ok: Boolean?, headline: String, pct: Int) {
    val spin = androidx.compose.animation.core.rememberInfiniteTransition()
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(5200, easing = androidx.compose.animation.core.LinearEasing),
        ),
    )
    val glow = engineTint(0.42f)
    val ring = engineTint(0.62f)
    val heroBg = Color(0xFF12151C)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(heroBg),
    ) {
        // 右上柔光
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = glow.copy(alpha = if (running) 0.30f else 0.18f),
                radius = 92.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(size.width - 8.dp.toPx(), 8.dp.toPx()),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 旋转光环徽标
            Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { rotationZ = angle },
                ) {
                    drawCircle(color = ring.copy(alpha = 0.85f), radius = 38.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx()))
                    drawArc(color = Color.White.copy(alpha = 0.9f), startAngle = 0f,
                        sweepAngle = 70f, useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                }
                Icon(
                    Icons.TwoTone.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CVE-2026-98260",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ring,
                    letterSpacing = 0.6.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (ok == true) "已取得临时 root" else if (running) "提权进行中" else headline,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 细进度条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.14f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((if (ok == true) 100f else pct.toFloat()).coerceIn(0f, 100f) / 100f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(ring),
                    )
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, tone: com.kuaimaosu.manager.ui.theme.KuaiMaoTone) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = tone.content,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(tone.container)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}
