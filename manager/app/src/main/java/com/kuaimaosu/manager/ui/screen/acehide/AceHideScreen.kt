package com.kuaimaosu.manager.ui.screen.acehide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.kuaimaosu.manager.data.acehide.AceHideRepository
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoRow
import com.kuaimaosu.manager.ui.component.rememberLoadingDialog
import com.kuaimaosu.manager.ui.screen.main.KuaiMaoSettingsScaffold
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.ui.util.LocalSnackbarHost
import com.kuaimaosu.manager.ui.util.showReplacingSnackbar
import org.koin.compose.koinInject

/**
 * eBPF 控制台：raw_syscalls hook getdents64，隐藏目录里名字含关键字的条目。
 *
 * 用探针目录在 App 内自证隐藏是否生效，不依赖终端。守护进程脱离 App 常驻，
 * 停止即 detach，完全可回滚。
 */
@Composable
fun AceHideScreen(bottomPadding: Dp = 0.dp) {
    val repository = koinInject<AceHideRepository>()
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHost.current
    val loadingDialog = rememberLoadingDialog()

    var state by remember { mutableStateOf(AceHideRepository.AceState()) }
    var keyword by rememberSaveable { mutableStateOf(AceHideRepository.DEFAULT_KEYWORD) }
    var proof by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    suspend fun refresh() {
        state = repository.status()
        keyword = state.keyword
    }

    LaunchedEffect(Unit) { refresh() }

    fun runStart() {
        if (busy) return
        if (keyword.isBlank()) {
            scope.launch { snackbarHost.showReplacingSnackbar("请先输入关键字") }
            return
        }
        busy = true
        scope.launch {
            loadingDialog.show()
            val result = repository.start(keyword.trim())
            loadingDialog.hide()
            busy = false
            result.onSuccess { proof = it; refresh() }
                .onFailure { proof = it.message.orEmpty(); refresh() }
            snackbarHost.showReplacingSnackbar(
                result.getOrElse { it.message ?: "启用失败" }
                    .lineSequence().firstOrNull().orEmpty().ifBlank { "操作完成" },
            )
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
            refresh()
            snackbarHost.showReplacingSnackbar(
                result.getOrElse { it.message ?: "停止失败" }.lineSequence().firstOrNull().orEmpty(),
            )
        }
    }

    fun runRefresh() {
        if (busy) return
        scope.launch { refresh() }
    }

    KuaiMaoSettingsScaffold(
        title = "eBPF 控制台",
        subtitle = "raw_syscalls · getdents64 目录项隐藏",
        bottomPadding = bottomPadding,
    ) {
        item {
            KuaiMaoCard {
                KuaiMaoRow(
                    icon = Icons.TwoTone.Security,
                    title = "守护进程状态",
                    description = if (state.running) {
                        "常驻中 (pid=${state.pid}) · 关键字 \"${state.keyword}\""
                    } else {
                        "未运行，隐藏当前未生效"
                    },
                    iconTint = if (state.running) KuaiMaoTones.green.content else KuaiMaoTones.slate.content,
                    iconContainer = if (state.running) KuaiMaoTones.green.container else KuaiMaoTones.slate.container,
                )
            }
        }

        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = "隐藏关键字",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KuaiMaoColors.text,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        singleLine = true,
                        enabled = !state.running && !busy,
                        label = { Text("如: ksu") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "隐藏名字含该关键字的目录项（如 /data/adb/ksu）。停止后可修改。",
                        fontSize = 11.sp,
                        color = KuaiMaoColors.textMuted,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { runStart() },
                    enabled = !state.running && !busy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KuaiMaoTones.green.content,
                    ),
                ) {
                    Icon(Icons.TwoTone.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("启用隐藏")
                }
                TextButton(
                    onClick = { runStop() },
                    enabled = state.running && !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("停止隐藏")
                }
                TextButton(
                    onClick = { runRefresh() },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.TwoTone.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("刷新")
                }
            }
        }

        if (proof.isNotBlank()) {
            item {
                KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = proof,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = KuaiMaoColors.text,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }

        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "守护进程日志",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KuaiMaoColors.text,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val logText = state.log.ifBlank { "（暂无日志，启用隐藏后可见）" }
                    Text(
                        text = logText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp,
                        color = KuaiMaoColors.textMuted,
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "说明：eBPF 隐藏针对 getdents64 返回的目录项名，可挡住扫 /data/adb/ksu 这类目录项的检测。" +
                    "它不改变包名、so 内部字符串或内核日志。守护进程常驻，停止即解除，不留痕。",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = KuaiMaoColors.textMuted,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}
