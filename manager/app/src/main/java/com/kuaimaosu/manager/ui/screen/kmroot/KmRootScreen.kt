package com.kuaimaosu.manager.ui.screen.kmroot

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.twotone.ChevronRight
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
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.ui.viewmodel.KmRootViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * 一键临时提权页面（KMRoot 引擎移植）。
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

    LaunchedEffect(Unit) { vm.refresh() }

    // 提权成功后短暂停留展示结果，然后自动返回主页。
    // 主页检测到栈深回到 1 会自动刷新内核/root 状态，无需重启管理器。
    LaunchedEffect(ui.resultOk) {
        if (ui.resultOk == true) {
            delay(1400)
            onBack()
        }
    }

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

    KuaiMaoSettingsScaffold(
        title = "一键越狱",
        subtitle = "KMRoot 引擎 · 临时 root",
        bottomPadding = bottomPadding,
    ) {
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
                    description = if (ui.mode == "slow") "慢速 98260（兼容模式）" else "快速（自动）",
                    iconTint = KuaiMaoTones.blue.content,
                    iconContainer = KuaiMaoTones.blue.container,
                    onClick = { vm.setMode(if (ui.mode == "slow") "auto" else "slow") },
                    trailing = {
                        Text(
                            text = if (ui.mode == "slow") "慢速" else "快速",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = KuaiMaoColors.brand,
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

        // 实时日志
        if (ui.logs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                LogConsole(lines = ui.logs)
            }
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "本功能移植自 KMRoot（作者 KaMuaMua2020，特此感谢），" +
                    "通过内核竞态 UAF（CVE-2026-98260）临时加载 KernelSU，重启即失效。" +
                    "仅用于自有设备；引擎适配机型有限，不适配时可能内核 panic / 重启。" +
                    "late-load 使用 KuaiMaoSU 自带的内核模块。内核模块与 ko 的 KMI 需与设备匹配。",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = KuaiMaoColors.textMuted,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Spacer(modifier = Modifier.height(bottomPadding + 12.dp))
        }
    }
}

@Composable
private fun LogConsole(lines: List<String>) {
    val state = rememberLazyListState()
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
            )
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
