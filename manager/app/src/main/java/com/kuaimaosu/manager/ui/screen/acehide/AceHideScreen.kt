package com.kuaimaosu.manager.ui.screen.acehide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Bolt
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material.icons.twotone.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.kuaimaosu.manager.data.acehide.AceHideRepository
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoRow
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.component.KuaiMaoSwitch
import com.kuaimaosu.manager.ui.component.rememberLoadingDialog
import com.kuaimaosu.manager.ui.screen.main.KuaiMaoSettingsScaffold
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.ui.util.LocalSnackbarHost
import com.kuaimaosu.manager.ui.util.showReplacingSnackbar
import org.koin.compose.koinInject

/**
 * eBPF 控制台：三个模块 + 检测项矩阵。
 *
 *  - dents   getdents64 目录项过滤，多关键字
 *  - procfs  /proc/modules、/proc/kallsyms 读空
 *  - stealth 守护进程自身伪装（常开）
 *
 * 所有结论都来自 App 内实测命令，不依赖终端。
 */
@Composable
fun AceHideScreen(bottomPadding: Dp = 0.dp) {
    val repository = koinInject<AceHideRepository>()
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHost.current
    val loadingDialog = rememberLoadingDialog()

    var state by remember { mutableStateOf(AceHideRepository.AceState()) }
    var keywords by rememberSaveable { mutableStateOf(AceHideRepository.DEFAULT_KEYWORDS) }
    var dents by rememberSaveable { mutableStateOf(true) }
    var procfs by rememberSaveable { mutableStateOf(true) }
    var proof by remember { mutableStateOf("") }
    var checks by remember { mutableStateOf(listOf<AceHideRepository.CheckItem>()) }
    var busy by remember { mutableStateOf(false) }

    suspend fun refresh() {
        state = repository.status()
        keywords = state.keywords
        dents = state.dentsOn
        procfs = state.procfsOn
    }

    LaunchedEffect(Unit) { refresh() }

    fun snack(msg: String) = scope.launch { snackbarHost.showReplacingSnackbar(msg) }

    fun runStart() {
        if (busy) return
        if (keywords.isBlank()) { snack("请先输入关键字"); return }
        busy = true
        scope.launch {
            loadingDialog.show()
            val result = repository.start(keywords, dents, procfs)
            loadingDialog.hide()
            busy = false
            proof = result.getOrElse { it.message ?: "启用失败" }
            refresh()
            snack(result.getOrElse { it.message ?: "启用失败" }.lineSequence().firstOrNull().orEmpty())
        }
    }

    fun runStop() {
        if (busy) return
        busy = true
        scope.launch {
            loadingDialog.show()
            val result = repository.stop()
            loadingDialog.hide()
            busy = false
            proof = ""
            checks = emptyList()
            refresh()
            snack(result.getOrElse { it.message ?: "停止失败" }.lineSequence().firstOrNull().orEmpty())
        }
    }

    fun runCheck() {
        if (busy) return
        busy = true
        scope.launch {
            loadingDialog.show()
            checks = repository.selfCheck()
            loadingDialog.hide()
            busy = false
            val pass = checks.count { it.verdict == AceHideRepository.Verdict.PASS }
            snack("检测矩阵：$pass/${checks.size} 项已阻断")
        }
    }

    KuaiMaoSettingsScaffold(
        title = "eBPF 控制台",
        subtitle = "目录项过滤 · /proc 表屏蔽 · 进程隐身",
        bottomPadding = bottomPadding,
    ) {
        item {
            KuaiMaoCard { KuaiMaoRow(
                icon = Icons.TwoTone.Security,
                title = "守护进程",
                description = if (state.running) {
                    "常驻中 (pid=${state.pid}) · dents=${if (state.dentsOk) "OK" else "FAIL"}" +
                        " · procfs=${if (state.procfsOk) "OK" else "FAIL"}"
                } else {
                    "未运行，所有 hook 已解除"
                },
                iconTint = if (state.running) KuaiMaoTones.green.content else KuaiMaoTones.slate.content,
                iconContainer = if (state.running) KuaiMaoTones.green.container else KuaiMaoTones.slate.container,
            ) }
        }

        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("隐藏关键字", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = KuaiMaoColors.text)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keywords,
                        onValueChange = { keywords = it },
                        singleLine = true,
                        enabled = !state.running && !busy,
                        label = { Text("逗号分隔，最多 4 个") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "前缀匹配：填 ksu 会同时挡住 ksud、ksu_backup。建议 ksu,kernelsu,susfs",
                        fontSize = 11.sp,
                        color = KuaiMaoColors.textMuted,
                    )
                }
            }
        }

        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                Column {
                    KuaiMaoRow(
                        icon = Icons.TwoTone.VisibilityOff,
                        title = "目录项隐藏 (dents)",
                        description = "hook getdents64：ls / find / ps 遍历时，名字命中的项直接不返回",
                        trailing = {
                            KuaiMaoSwitch(
                                checked = dents,
                                enabled = !state.running && !busy,
                                onCheckedChange = { dents = it },
                            )
                        },
                    )
                    KuaiMaoRowDivider()
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Security,
                        title = "/proc 表屏蔽 (procfs)",
                        description = "把 /proc/modules、/proc/kallsyms 的打开重定向到 /dev/null，模块表读空",
                        trailing = {
                            KuaiMaoSwitch(
                                checked = procfs,
                                enabled = !state.running && !busy,
                                onCheckedChange = { procfs = it },
                            )
                        },
                    )
                    KuaiMaoRowDivider()
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Bolt,
                        title = "进程隐身 (stealth)",
                        description = "comm=kworker/u8:2，cmdline 擦除，pid/日志走中性路径（常开）",
                        trailing = {
                            KuaiMaoSwitch(checked = true, enabled = false, onCheckedChange = null)
                        },
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { runStart() },
                    enabled = !state.running && !busy && (dents || procfs),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = KuaiMaoTones.green.content),
                ) {
                    Icon(Icons.TwoTone.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("启用")
                }
                TextButton(onClick = { runStop() }, enabled = state.running && !busy, modifier = Modifier.weight(1f)) {
                    Text("停止")
                }
                TextButton(onClick = { scope.launch { refresh() } }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.TwoTone.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("刷新")
                }
            }
        }

        if (proof.isNotBlank()) {
            item {
                KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("自证", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = KuaiMaoColors.text)
                        Spacer(Modifier.height(6.dp))
                        Text(proof, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp, color = KuaiMaoColors.text)
                    }
                }
            }
        }

        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("检测项矩阵", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = KuaiMaoColors.text)
                        TextButton(onClick = { runCheck() }, enabled = state.running && !busy) {
                            Text("实测")
                        }
                    }
                    Text(
                        "逐项跑真实检测命令，给出 PASS / 未覆盖。未覆盖项会写明原因，不粉饰。",
                        fontSize = 11.sp,
                        color = KuaiMaoColors.textMuted,
                    )
                    if (checks.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        checks.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.Top,
                            ) {
                                Text(
                                    text = when (item.verdict) {
                                        AceHideRepository.Verdict.PASS -> "PASS"
                                        AceHideRepository.Verdict.FAIL -> "FAIL"
                                        AceHideRepository.Verdict.UNCOVERED -> "未覆盖"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (item.verdict) {
                                        AceHideRepository.Verdict.PASS -> KuaiMaoTones.green.content
                                        AceHideRepository.Verdict.FAIL -> KuaiMaoColors.amber
                                        AceHideRepository.Verdict.UNCOVERED -> KuaiMaoColors.textMuted
                                    },
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                                Column {
                                    Text(item.name, fontSize = 11.sp, color = KuaiMaoColors.text)
                                    Text(item.detail, fontSize = 10.sp, color = KuaiMaoColors.textMuted)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("守护进程日志", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = KuaiMaoColors.text)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.log.ifBlank { "（暂无日志，启用后可见）" },
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp,
                        color = KuaiMaoColors.textMuted,
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "覆盖边界：eBPF 只改「内核回给用户的数据」——目录项列表、/proc 文件内容。" +
                    "改不了 syscall 返回值（stat 仍会成功）、改不了包名查询、改不了属性服务；" +
                    "这几项要靠 SUSFS 或模块侧解决，矩阵里会标「未覆盖」。",
                fontSize = 11.sp,
                color = KuaiMaoColors.textMuted,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}
