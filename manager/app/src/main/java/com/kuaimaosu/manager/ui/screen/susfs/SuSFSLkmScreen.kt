package com.kuaimaosu.manager.ui.screen.susfs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Bolt
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Memory
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.kuaimaosu.manager.data.susfs.SusfsLkmRepository
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoRow
import com.kuaimaosu.manager.ui.component.KuaiMaoSwitch
import com.kuaimaosu.manager.ui.component.rememberLoadingDialog
import com.kuaimaosu.manager.ui.screen.main.KuaiMaoSettingsScaffold
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.ui.util.LocalSnackbarHost
import com.kuaimaosu.manager.ui.util.showReplacingSnackbar
import org.koin.compose.koinInject

/**
 * SUSFS 内核模块（susfs_guard_lkm）控制页。
 *
 * 只处理「加载 / 卸载 / 开机自动加载」这一件事：内核没有内建 SUSFS 时，
 * 加载 ksud 里内嵌的守卫模块即可获得 SUSFS 能力；加载之后，SuSFS 配置页
 * 的隐藏项才会真正生效。
 */
@Composable
fun SuSFSLkmScreen(bottomPadding: Dp = 0.dp) {
    val repository = koinInject<SusfsLkmRepository>()
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHost.current
    val loadingDialog = rememberLoadingDialog()

    var state by remember { mutableStateOf(SusfsLkmRepository.LkmState()) }
    var busy by remember { mutableStateOf(false) }
    var selectedKmi by rememberSaveable { mutableStateOf<String?>(null) }
    var showKmiPicker by remember { mutableStateOf(false) }
    var kmiOptions by remember { mutableStateOf<List<String>>(emptyList()) }

    suspend fun refresh() {
        state = repository.status()
        if (state.autoloadKmi != null) selectedKmi = state.autoloadKmi
    }

    LaunchedEffect(Unit) { refresh() }

    fun runAction(
        successMessage: String,
        block: suspend () -> Result<String>,
    ) {
        if (busy) return
        busy = true
        scope.launch {
            loadingDialog.show()
            val result = block()
            loadingDialog.hide()
            busy = false
            refresh()
            // 主页大卡的「SUSFS LKM 模式」标签跟的是 repository.loaded，加载 / 卸载会自己更新。
            snackbarHost.showReplacingSnackbar(
                result.getOrElse { it.message ?: "操作失败" }.lineSequence().firstOrNull().orEmpty()
                    .ifBlank { successMessage },
            )
        }
    }

    KuaiMaoSettingsScaffold(
        title = "SUSFS LKM",
        subtitle = "动态加载 · 开机自动加载",
        bottomPadding = bottomPadding,
    ) {
        item {
            KuaiMaoCard {
                KuaiMaoRow(
                    icon = Icons.TwoTone.Security,
                    title = "模块状态",
                    description = when {
                        state.loaded -> state.version?.let { "SUSFS $it · 已在内核中生效" } ?: "已加载"
                        state.available -> "内核已内建 SUSFS，无需加载模块"
                        else -> "未加载，SUSFS 隐藏能力当前不可用"
                    },
                    iconTint = if (state.loaded || state.available) {
                        KuaiMaoTones.green.content
                    } else {
                        KuaiMaoTones.slate.content
                    },
                    iconContainer = if (state.loaded || state.available) {
                        KuaiMaoTones.green.container
                    } else {
                        KuaiMaoTones.slate.container
                    },
                    trailing = {
                        StatusBadge(
                            text = when {
                                state.loaded -> "已加载"
                                state.available -> "内建"
                                else -> "未加载"
                            },
                            active = state.loaded || state.available,
                        )
                    },
                )
            }
        }

        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                if (state.loaded) {
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Delete,
                        title = "卸载模块",
                        description = "从内核移除 susfs_guard_lkm，隐藏能力随即失效",
                        iconTint = KuaiMaoTones.rose.content,
                        iconContainer = KuaiMaoTones.rose.container,
                        enabled = !busy,
                        onClick = {
                            runAction("已卸载") { repository.unload() }
                        },
                        trailing = { Chevron() },
                    )
                } else {
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Bolt,
                        title = "加载模块",
                        description = if (state.available) {
                            "内核已有 SUSFS，通常不需要再加载"
                        } else {
                            "把内置的 susfs_guard_lkm 加载进当前内核"
                        },
                        iconTint = KuaiMaoTones.green.content,
                        iconContainer = KuaiMaoTones.green.container,
                        enabled = !busy,
                        onClick = {
                            runAction("已加载") { repository.load(selectedKmi) }
                        },
                        trailing = { Chevron() },
                    )
                }
            }
        }

        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                KuaiMaoRow(
                    icon = Icons.TwoTone.Security,
                    title = "开机自动加载",
                    description = "post-fs-data 阶段自动加载，重启后无需手动操作",
                    trailing = {
                        KuaiMaoSwitch(
                            checked = state.autoload,
                            enabled = !busy,
                            onCheckedChange = { enabled ->
                                val kmi = selectedKmi ?: state.autoloadKmi
                                runAction(if (enabled) "已开启" else "已关闭") {
                                    repository.setAutoload(enabled, kmi)
                                }
                            },
                        )
                    },
                )
            }
        }

        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                KuaiMaoRow(
                    icon = Icons.TwoTone.Memory,
                    title = "内核版本（KMI）",
                    description = selectedKmi ?: "自动检测，加载失败时手动指定",
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            if (kmiOptions.isEmpty()) {
                                kmiOptions = repository.supportedKmi()
                            }
                            showKmiPicker = true
                        }
                    },
                    trailing = { Chevron() },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "模块加载后，SuSFS 配置页里的隐藏项才会对内核生效。" +
                    "如果内核本身已内建 SUSFS，则无需加载，这里的操作会直接跳过。",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = KuaiMaoColors.textMuted,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }

    if (showKmiPicker) {
        AlertDialog(
            onDismissRequest = { showKmiPicker = false },
            title = { Text("选择内核版本") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (kmiOptions.isEmpty()) {
                        Text("没有可用的模块，请确认 ksud 已更新")
                    } else {
                        kmiOptions.forEach { kmi ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedKmi = kmi
                                        showKmiPicker = false
                                    }
                                    .padding(vertical = 11.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = kmi,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (kmi == selectedKmi) {
                                        KuaiMaoColors.brand
                                    } else {
                                        KuaiMaoColors.text
                                    },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showKmiPicker = false }) { Text("关闭") }
            },
        )
    }
}

@Composable
private fun StatusBadge(text: String, active: Boolean) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (active) KuaiMaoTones.green.content else KuaiMaoColors.textMuted,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .then(
                if (active) {
                    Modifier.background(KuaiMaoTones.green.container)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.TwoTone.ChevronRight,
        contentDescription = null,
        tint = KuaiMaoColors.chevron,
        modifier = Modifier.size(16.dp),
    )
}
