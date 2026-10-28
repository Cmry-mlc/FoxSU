package me.weishu.kernelsu.ui.screen.ag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.screen.install.InstallMethod
import me.weishu.kernelsu.ui.screen.install.InstallScreenActions
import me.weishu.kernelsu.ui.screen.install.InstallUiState
import me.weishu.kernelsu.ui.util.LkmSelection

/**
 * 晨曦玻璃 · 镜像安装（按 Aurora Glass 设计语言实现）。
 * 安装方式选择（三选一卡片）→ 分区选择 chips → LKM 文件 → 高级选项折叠 → 渐变「下一步」。
 * 全部动作来自 InstallScreenActions，真实有效。
 */
@Composable
internal fun AgInstall(
    uiState: InstallUiState,
    actions: InstallScreenActions,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ag.background())
            .statusBarsPadding(),
    ) {
        // ── 固定顶栏：返回 + 渐变标题 ──
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xC4F7F6FB))
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AgIconBtn(Icons.AutoMirrored.Rounded.ArrowBack, onClick = actions.onBack)
            }
            AgTitle("安", "装")
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 18.dp),
        ) {
            // ── 安装方式 ──
            AgGHead("安装方式")
            uiState.installMethodOptions.forEach { method ->
                val selected = uiState.installMethod?.label == method.label
                val (icon, iconTint) = when (method) {
                    is InstallMethod.SelectFile -> Icons.Rounded.FileOpen to Ag.Vio
                    is InstallMethod.DownloadFile -> Icons.Rounded.CloudDownload to Ag.Teal
                    is InstallMethod.DirectInstall -> Icons.Rounded.Bolt to Ag.Mag
                    is InstallMethod.DirectInstallToInactiveSlot -> Icons.Rounded.Bolt to Ag.Amber
                }
                AgMethodCard(
                    title = stringResource(method.label),
                    desc = method.summary,
                    icon = icon,
                    tint = iconTint,
                    selected = selected,
                    hasAction = when (method) {
                        is InstallMethod.SelectFile -> true
                        is InstallMethod.DownloadFile -> true
                        else -> false
                    },
                    onPick = {
                        when (method) {
                            is InstallMethod.SelectFile -> actions.onSelectBootImage()
                            is InstallMethod.DownloadFile -> actions.onDownloadFile()
                            else -> {}
                        }
                    },
                    onClick = { actions.onSelectMethod(method) },
                )
                Spacer(Modifier.height(10.dp))
            }

            // ── 分区选择 ──
            val isDownload = uiState.installMethod is InstallMethod.DownloadFile
            val partitions = if (isDownload) uiState.remoteDisplayPartitions else uiState.displayPartitions
            val partIndex = if (isDownload) uiState.remotePartitionSelectionIndex else uiState.partitionSelectionIndex
            if (partitions.isNotEmpty()) {
                AgGHead(
                    if (isDownload) stringResource(R.string.install_select_partition)
                    else "${stringResource(R.string.install_select_partition)}（${uiState.slotSuffix}）"
                )
                AgCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        partitions.forEachIndexed { i, p ->
                            val on = i == partIndex && uiState.canSelectPartition
                            Box(
                                Modifier
                                    .agClick(enabled = uiState.canSelectPartition, onClick = { actions.onSelectPartition(i) })
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (on) Ag.Grad else androidx.compose.ui.graphics.SolidColor(Color(0x0D1E1946))
                                    )
                                    .border(
                                        1.dp,
                                        if (on) Color.Transparent else Ag.Line,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                            ) {
                                Text(
                                    p,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    color = if (on) Color.White else Ag.Tx2,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // ── LKM 文件 ──
            if (uiState.installMethod != null) {
                AgGHead("LKM 内核模块")
                AgCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    AgRow(
                        stringResource(R.string.install_upload_lkm_file),
                        (uiState.lkmSelection as? LkmSelection.LkmUri)?.let { it.uri.toString() }
                            ?: "当前 KMI：${uiState.currentKmi.ifBlank { "自动" }}",
                        Ag.Mag, Icons.Rounded.Upload, showChev = true,
                        onClick = actions.onUploadLkm,
                    )
                    if (uiState.canForceBackup) {
                        AgRow(
                            stringResource(R.string.install_force_backup),
                            stringResource(R.string.install_force_backup_summary),
                            Ag.Teal, Icons.Rounded.Check,
                            trailing = {
                                AgToggle(checked = uiState.forceBackup, onCheckedChange = { actions.onSelectForceBackup(!uiState.forceBackup) }, small = true)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))

                // ── 高级选项 ──
                var advanced by remember { mutableStateOf(uiState.advancedOptionsShown) }
                AgGHead("高级选项")
                AgCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    AgRow(
                        "高级选项",
                        "允许 Shell / 启用 ADB Root",
                        Ag.Vio, Icons.Rounded.Tune, showChev = true,
                        onClick = { advanced = !advanced; actions.onAdvancedOptionsClicked() },
                    )
                    if (advanced) {
                        AgRow(
                            stringResource(R.string.allow_shell),
                            stringResource(R.string.allow_shell_summary),
                            Ag.Teal, Icons.Rounded.Check,
                            trailing = { AgToggle(checked = uiState.allowShell, onCheckedChange = { actions.onSelectAllowShell(!uiState.allowShell) }, small = true) },
                        )
                        AgRow(
                            stringResource(R.string.enable_adb),
                            stringResource(R.string.enable_adb_summary),
                            Ag.Mag, Icons.Rounded.Tune,
                            trailing = { AgToggle(checked = uiState.enableAdb, onCheckedChange = { actions.onSelectEnableAdb(!uiState.enableAdb) }, small = true) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── 底部渐变主按钮 ──
        Box(Modifier.fillMaxWidth().padding(18.dp, 12.dp, 18.dp, 16.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .agClick(enabled = uiState.installMethod != null, onClick = { actions.onNext() })
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (uiState.installMethod != null) Ag.Grad
                        else androidx.compose.ui.graphics.SolidColor(Color(0xFFD9D8E6))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.install_next),
                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (uiState.installMethod != null) Color.White else Ag.Tx3,
                )
            }
        }
    }
}

/** 安装方式卡片（渐变选中描边） */
@Composable
private fun AgMethodCard(
    title: String,
    desc: String?,
    icon: ImageVector,
    tint: Color,
    selected: Boolean,
    hasAction: Boolean,
    onPick: () -> Unit,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.clip(shape).background(Ag.Grad).padding(2.dp) else Modifier)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(if (selected) 16.dp else 18.dp))
                .background(Ag.CardSolid)
                .border(1.dp, if (selected) Color.Transparent else Ag.Line, shape)
                .agClick(onClick = onClick)
                .padding(14.dp, 13.dp, 14.dp, 13.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AgRic(tint, icon)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Ag.Tx)
                    if (!desc.isNullOrBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(desc, fontSize = 12.sp, lineHeight = 18.sp, color = Ag.Tx2)
                    }
                }
                if (hasAction) {
                    Box(
                        Modifier
                            .agClick(onClick = onPick)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x126C5CE7))
                            .border(1.dp, Color(0x386C5CE7), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text("选择", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ag.Vio)
                    }
                }
                if (selected) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Ag.Grad),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
