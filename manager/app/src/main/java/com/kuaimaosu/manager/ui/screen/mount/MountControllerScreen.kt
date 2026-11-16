package com.kuaimaosu.manager.ui.screen.mount

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Layers
import androidx.compose.material.icons.twotone.Memory
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material.icons.twotone.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.kuaimaosu.manager.data.mount.MountRepository
import com.kuaimaosu.manager.data.mount.MountRepository.MountModule
import com.kuaimaosu.manager.data.mount.MountRepository.MountStatus
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoRadioMark
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
 * 内置挂载控制器：模块挂载方式在 ksud 里完成（OverlayFS / Magic Mount），
 * 这里只切换全局后端与逐模块规则。挂载发生在开机 post-fs-data 阶段，
 * 所以改动都是「保存 → 重启生效」。
 */
@Composable
fun MountControllerScreen(bottomPadding: Dp = 0.dp) {
    val repository = koinInject<MountRepository>()
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHost.current
    val loadingDialog = rememberLoadingDialog()

    var status by remember { mutableStateOf(MountStatus()) }
    var modules by remember { mutableStateOf<List<MountModule>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MountModule?>(null) }

    suspend fun refresh() {
        status = repository.refreshStatus()
        modules = repository.modules()
    }

    LaunchedEffect(Unit) { refresh() }

    fun run(block: suspend () -> Result<*>) {
        if (busy) return
        busy = true
        scope.launch {
            loadingDialog.show()
            val result = block()
            loadingDialog.hide()
            busy = false
            refresh()
            val message = result.exceptionOrNull()?.message
                ?.lineSequence()?.firstOrNull().orEmpty()
                .ifBlank { "已保存，重启后生效" }
            snackbarHost.showReplacingSnackbar(message)
        }
    }

    KuaiMaoSettingsScaffold(
        title = "挂载控制",
        subtitle = "OverlayFS · Magic Mount",
        bottomPadding = bottomPadding,
    ) {
        item {
            KuaiMaoCard {
                KuaiMaoRow(
                    icon = Icons.TwoTone.Memory,
                    title = "内置挂载",
                    description = if (status.enabled) {
                        "开机时由 ksud 直接挂载模块，不依赖 metamodule"
                    } else {
                        "已关闭，模块内容不会进入系统"
                    },
                    trailing = {
                        KuaiMaoSwitch(
                            checked = status.enabled,
                            enabled = !busy,
                            onCheckedChange = { enabled ->
                                run { repository.setEnabled(enabled) }
                            },
                        )
                    },
                )
                KuaiMaoRowDivider()
                KuaiMaoRow(
                    title = "当前状态",
                    description = statusDescription(status),
                    iconTint = if (status.mounted) {
                        KuaiMaoTones.green.content
                    } else {
                        KuaiMaoTones.slate.content
                    },
                    iconContainer = if (status.mounted) {
                        KuaiMaoTones.green.container
                    } else {
                        KuaiMaoTones.slate.container
                    },
                )
            }
        }

        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                KuaiMaoRow(
                    icon = Icons.TwoTone.Layers,
                    title = "全局后端",
                    description = "对所有模块生效，覆盖逐模块设置",
                    trailing = {
                        Text(
                            text = modeLabel(status.backend),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KuaiMaoColors.textMuted,
                        )
                    },
                )
                KuaiMaoRowDivider()
                BackendOption(
                    title = "自动",
                    description = "优先 OverlayFS，不可用时回退 Magic Mount",
                    selected = status.backend == MountRepository.MODE_AUTO,
                    enabled = !busy && status.enabled,
                    onClick = { run { repository.setBackend(MountRepository.MODE_AUTO) } },
                )
                BackendOption(
                    title = "OverlayFS",
                    description = if (status.overlaySupported) {
                        "按子目录叠加，挂载点最少"
                    } else {
                        "当前内核没有 overlay 文件系统，会回退"
                    },
                    selected = status.backend == MountRepository.MODE_OVERLAY,
                    enabled = !busy && status.enabled,
                    onClick = { run { repository.setBackend(MountRepository.MODE_OVERLAY) } },
                )
                BackendOption(
                    title = "Magic Mount",
                    description = "逐文件挂载，可替换分区根目录下的文件",
                    selected = status.backend == MountRepository.MODE_MAGIC,
                    enabled = !busy && status.enabled,
                    onClick = { run { repository.setBackend(MountRepository.MODE_MAGIC) } },
                )
                BackendOption(
                    title = "不挂载",
                    description = "只保留模块文件，不向系统挂载",
                    selected = status.backend == MountRepository.MODE_NONE,
                    enabled = !busy && status.enabled,
                    onClick = { run { repository.setBackend(MountRepository.MODE_NONE) } },
                )
            }
        }

        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                KuaiMaoRow(
                    icon = Icons.TwoTone.Tune,
                    title = "逐模块规则",
                    description = if (status.backend == MountRepository.MODE_AUTO) {
                        "${modules.size} 个模块"
                    } else {
                        "全局后端已覆盖，逐模块设置暂不生效"
                    },
                )
                if (modules.isEmpty()) {
                    KuaiMaoRowDivider()
                    KuaiMaoRow(
                        title = "没有需要挂载的模块",
                        description = "模块目录下没有可挂载的分区内容",
                        enabled = false,
                    )
                }
                modules.forEach { module ->
                    KuaiMaoRowDivider()
                    KuaiMaoRow(
                        title = module.name,
                        description = "模式 ${modeLabel(module.mode)} · 生效 ${backendLabel(module.backend)}",
                        icon = Icons.TwoTone.Layers,
                        iconTint = KuaiMaoTones.blue.content,
                        iconContainer = KuaiMaoTones.blue.container,
                        enabled = !busy,
                        selected = false,
                        onClick = { editing = module },
                        trailing = {
                            Text(
                                text = backendLabel(module.backend),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KuaiMaoColors.textMuted,
                            )
                        },
                    )
                }
            }
        }

        if (status.disabled) {
            item {
                KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Security,
                        title = "防砖保护已触发",
                        description = "连续 3 次开机未完成，挂载已停用；修好模块后重置",
                        iconTint = KuaiMaoTones.rose.content,
                        iconContainer = KuaiMaoTones.rose.container,
                        enabled = !busy,
                        onClick = { run { repository.resetProtection() } },
                        trailing = {
                            Text(
                                text = "重置",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KuaiMaoColors.brand,
                            )
                        },
                    )
                }
            }
        }

        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                KuaiMaoRow(
                    icon = Icons.TwoTone.Refresh,
                    title = "卸载全部挂载",
                    description = "拆掉本控制器建立的所有挂载点，模块随即失效",
                    enabled = !busy && status.mounted,
                    onClick = { run { repository.unmount() } },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "挂载在开机 post-fs-data 阶段执行，切换后端或修改逐模块规则后需要重启生效。" +
                    (if (status.metamodule) "检测到 metamodule，内置控制器会让位给它的挂载脚本。" else ""),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = KuaiMaoColors.textMuted,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }

    editing?.let { module ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(module.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ModeOption(
                        title = "自动",
                        description = "跟随全局后端与设备能力",
                        selected = module.mode == MountRepository.MODE_AUTO,
                    ) {
                        editing = null
                        run { repository.setModuleMode(module.id, MountRepository.MODE_AUTO) }
                    }
                    ModeOption(
                        title = "OverlayFS",
                        description = "只叠加子目录，分区根的文件无法替换",
                        selected = module.mode == MountRepository.MODE_OVERLAY,
                    ) {
                        editing = null
                        run { repository.setModuleMode(module.id, MountRepository.MODE_OVERLAY) }
                    }
                    ModeOption(
                        title = "Magic Mount",
                        description = "逐文件挂载，兼容分区根目录改动",
                        selected = module.mode == MountRepository.MODE_MAGIC,
                    ) {
                        editing = null
                        run { repository.setModuleMode(module.id, MountRepository.MODE_MAGIC) }
                    }
                    ModeOption(
                        title = "不挂载",
                        description = "这个模块的内容不进入系统",
                        selected = module.mode == MountRepository.MODE_NONE,
                    ) {
                        editing = null
                        run { repository.setModuleMode(module.id, MountRepository.MODE_NONE) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editing = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun BackendOption(
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    KuaiMaoRow(
        title = title,
        description = description,
        enabled = enabled,
        onClick = onClick,
        trailing = { KuaiMaoRadioMark(selected = selected, enabled = enabled) },
    )
}

@Composable
private fun ModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) KuaiMaoColors.brand else KuaiMaoColors.text,
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = KuaiMaoColors.textMuted,
            )
        }
        KuaiMaoRadioMark(selected = selected)
    }
}

private fun modeLabel(mode: String): String = when (mode) {
    MountRepository.MODE_OVERLAY -> "OverlayFS"
    MountRepository.MODE_MAGIC -> "Magic Mount"
    MountRepository.MODE_NONE -> "不挂载"
    else -> "自动"
}

private fun backendLabel(backend: String): String = when (backend) {
    MountRepository.MODE_OVERLAY -> "OverlayFS"
    MountRepository.MODE_MAGIC -> "Magic Mount"
    MountRepository.MODE_NONE -> "未挂载"
    else -> "自动"
}

private fun statusDescription(status: MountStatus): String = when {
    status.metamodule -> "检测到 metamodule，内置控制器暂不接管"
    status.disabled -> "防砖保护已触发，本次开机未挂载"
    status.mounted -> "${
        status.applied.ifBlank { "已挂载" }
    } · ${status.mountCount} 个挂载点 · ${status.moduleCount} 个模块"
    status.backend == MountRepository.MODE_NONE -> "已关闭挂载，模块内容不会进入系统"
    else -> "本次开机未挂载，重启后按当前设置生效"
}
