package me.weishu.kernelsu.ui.screen.ag

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.screen.install.InstallMethod
import me.weishu.kernelsu.ui.screen.install.InstallScreenActions
import me.weishu.kernelsu.ui.screen.install.InstallUiState
import me.weishu.kernelsu.ui.util.LkmSelection

/**
 * 晨曦玻璃 · 镜像安装。
 *
 * 版式：渐变标题头 → 「当前环境」摘要卡 → 安装方式（单选大卡）→ 目标分区 → LKM / 高级选项
 * → 底部渐变主按钮。所有动作都来自 [InstallScreenActions]，点击即真实生效。
 *
 * 相对上一版的关键修正：
 * 1. 「选择镜像 / 在线下载」卡片整卡可点，直接拉起文件选择器 / 下载弹窗
 *    （之前只选中不拉起，下一步时 boot=null 会刷写出错，看起来像"进不去安装界面"）；
 * 2. 选区与卡片状态一一对应，未选方式时不显示分区 / LKM，避免空卡；
 * 3. 所有留白按 390dp 画板等比换算，和首页保持同一套栅格。
 */
@Composable
internal fun AgInstall(
    uiState: InstallUiState,
    actions: InstallScreenActions,
) {
    val boardW = LocalConfiguration.current.screenWidthDp.dp
    val edge = boardW * (18f / 390f)

    val isDownload = uiState.installMethod is InstallMethod.DownloadFile
    val partitions = if (isDownload) uiState.remoteDisplayPartitions else uiState.displayPartitions
    val partIndex = if (isDownload) uiState.remotePartitionSelectionIndex else uiState.partitionSelectionIndex
    val selectedLabel = uiState.installMethod?.label
    var advancedOpen by remember { mutableStateOf(uiState.advancedOptionsShown) }

    Column(
        Modifier
            .fillMaxSize()
            .agPageBg()
            .statusBarsPadding()
            .imePadding(),
    ) {
        // ── 顶栏：返回 + KMI 胶囊 + 渐变标题 ──
        Column(
            Modifier
                .fillMaxWidth()
                .background(Ag.headSurface())
                .agHeadBlur()
                .padding(horizontal = edge),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AgIconBtn(AgTopIcons.Back, onClick = actions.onBack)
                Spacer(Modifier.weight(1f))
                AgChip("KMI ${uiState.currentKmi.ifBlank { "自动" }}")
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "安装内核",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.6).sp,
                style = TextStyle(brush = Ag.TitleGrad),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "选择镜像来源，刷入 boot 分区后重启生效",
                fontSize = 12.5.sp,
                color = Ag.Tx2,
            )
            Spacer(Modifier.height(16.dp))
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = edge),
        ) {
            // ── 当前环境摘要 ──
            AgCard(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AgRic(Ag.Vio, AgSettingsIcons.Shield, size = 42.dp)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "当前环境",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ag.Tx,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            buildString {
                                append("KMI ")
                                append(uiState.currentKmi.ifBlank { "自动识别" })
                                if (partitions.isNotEmpty() && uiState.canSelectPartition) {
                                    append(" · 目标 ")
                                    append(partitions.getOrNull(partIndex) ?: "—")
                                }
                            },
                            fontSize = 12.5.sp,
                            color = Ag.Tx2,
                            maxLines = 1,
                        )
                    }
                    if (uiState.slotSuffix.isNotBlank()) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(Ag.TintInk)
                                .border(1.dp, Ag.Line, RoundedCornerShape(9.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        ) {
                            Text(
                                uiState.slotSuffix.trim(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ag.Tx2,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            // ── 安装方式 ──
            AgGHead("安装方式")
            uiState.installMethodOptions.forEach { method ->
                val selected = selectedLabel == method.label
                val (icon, tint) = when (method) {
                    is InstallMethod.SelectFile -> Icons.Rounded.FileOpen to Ag.Vio
                    is InstallMethod.DownloadFile -> Icons.Rounded.CloudDownload to Ag.Teal
                    is InstallMethod.DirectInstall -> Icons.Rounded.Bolt to Ag.Mag
                    is InstallMethod.DirectInstallToInactiveSlot -> Icons.Rounded.Bolt to Ag.Amber
                }
                AgMethodCard(
                    title = stringResource(method.label),
                    desc = method.summary,
                    icon = icon,
                    tint = tint,
                    selected = selected,
                    trailingHint = when (method) {
                        is InstallMethod.SelectFile -> "选择文件"
                        is InstallMethod.DownloadFile -> "输入链接"
                        else -> null
                    },
                    onClick = {
                        actions.onSelectMethod(method)
                        when (method) {
                            is InstallMethod.SelectFile -> actions.onSelectBootImage()
                            is InstallMethod.DownloadFile -> actions.onDownloadFile()
                            else -> Unit
                        }
                    },
                )
                Spacer(Modifier.height(9.dp))
            }
            Spacer(Modifier.height(6.dp))

            // ── 目标分区（仅在选择「直接刷入」/ 在线下载时出现）──
            if (partitions.isNotEmpty() && uiState.canSelectPartition) {
                AgGHead(
                    if (isDownload) stringResource(R.string.install_select_partition)
                    else "${stringResource(R.string.install_select_partition)}（${uiState.slotSuffix}）"
                )
                AgCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        partitions.forEachIndexed { i, p ->
                            val on = i == partIndex
                            Box(
                                Modifier
                                    .agClick(onClick = { actions.onSelectPartition(i) })
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (on) Ag.Grad else SolidColor(Ag.TintInk))
                                    .border(
                                        1.dp,
                                        if (on) Color.Transparent else Ag.Line,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .padding(horizontal = 15.dp, vertical = 9.dp),
                            ) {
                                Text(
                                    p,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (on) Color.White else Ag.Tx2,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── LKM 内核模块 ──
            if (uiState.installMethod != null) {
                AgGHead("内核模块")
                AgCard(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    AgRow(
                        stringResource(R.string.install_upload_lkm_file),
                        (uiState.lkmSelection as? LkmSelection.LkmUri)?.let { it.uri.toString() }
                            ?: "未选择 · 当前 KMI ${uiState.currentKmi.ifBlank { "自动" }}",
                        Ag.Mag, AgSettingsIcons.Export, showChev = true,
                        onClick = actions.onUploadLkm,
                    )
                    if (uiState.canForceBackup) {
                        AgToggleRow(
                            stringResource(R.string.install_force_backup),
                            stringResource(R.string.install_force_backup_summary),
                            Ag.Teal, Icons.Rounded.Check,
                            divider = false,
                            trailing = {
                                AgToggle(
                                    checked = uiState.forceBackup,
                                    onCheckedChange = { actions.onSelectForceBackup(!uiState.forceBackup) },
                                    small = true,
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                // ── 高级选项（折叠）──
                AgGHead("高级选项")
                AgCard(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    AgRow(
                        "高级选项",
                        "允许 Shell / 启用 ADB Root",
                        Ag.Vio, Icons.Rounded.Tune,
                        trailing = {
                            Text(
                                if (advancedOpen) "收起" else "展开",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ag.Vio,
                            )
                        },
                        divider = advancedOpen,
                        onClick = {
                            advancedOpen = !advancedOpen
                            actions.onAdvancedOptionsClicked()
                        },
                    )
                    AnimatedVisibility(
                        visible = advancedOpen,
                        enter = fadeIn(tween(160)) + expandVertically(tween(200)),
                        exit = fadeOut(tween(120)) + shrinkVertically(tween(160)),
                    ) {
                        Column {
                            AgToggleRow(
                                stringResource(R.string.allow_shell),
                                stringResource(R.string.allow_shell_summary),
                                Ag.Teal, Icons.Rounded.Check,
                                trailing = {
                                    AgToggle(
                                        checked = uiState.allowShell,
                                        onCheckedChange = { actions.onSelectAllowShell(!uiState.allowShell) },
                                        small = true,
                                    )
                                },
                            )
                            AgToggleRow(
                                stringResource(R.string.enable_adb),
                                stringResource(R.string.enable_adb_summary),
                                Ag.Mag, Icons.Rounded.Tune,
                                divider = false,
                                trailing = {
                                    AgToggle(
                                        checked = uiState.enableAdb,
                                        onCheckedChange = { actions.onSelectEnableAdb(!uiState.enableAdb) },
                                        small = true,
                                    )
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── 安全提示 ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Rounded.Info,
                    null,
                    tint = Ag.Tx3,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(14.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    "刷写前会自动备份当前 boot 分区；越狱模式（晚加载）下刷写会导致无法开机，入口会被禁用。",
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    color = Ag.Tx3,
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── 底部主按钮 ──
        Column(
            Modifier
                .fillMaxWidth()
                .background(Ag.headSurface())
                .padding(horizontal = edge),
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Ag.Line))
            Spacer(Modifier.height(12.dp))
            val ready = uiState.installMethod != null
            val shape = RoundedCornerShape(16.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .agClick(enabled = ready, onClick = { actions.onNext() })
                    .clip(shape)
                    .then(
                        if (ready) Modifier
                            .background(Ag.Grad, shape)
                            .shadow(10.dp, shape, spotColor = Color(0x4D6C5CE7), ambientColor = Color(0x1A6C5CE7))
                        else Modifier.background(SolidColor(Ag.ToggleOff), shape)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.install_next),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (ready) Color.White else Ag.Tx3,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * 安装方式卡片：44dp 渐变衬底图标 + 标题/说明 + 右侧选中环。
 * 选中态用「1.5dp 渐变描边 + 极淡紫底」表达，不再整块套渐变，避免一屏三张卡全都发光。
 */
@Composable
private fun AgMethodCard(
    title: String,
    desc: String?,
    icon: ImageVector,
    tint: Color,
    selected: Boolean,
    trailingHint: String?,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(Modifier.fillMaxWidth()) {
        // 选中态：渐变描边（外圈 1.5dp），未选中：普通发丝线
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (selected) Modifier.background(Ag.Grad, shape)
                    else Modifier.border(1.dp, Ag.Line, shape)
                )
                .padding(if (selected) 1.5.dp else 0.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(if (selected) 18.5.dp else 20.dp))
                    .background(if (selected) Color(0x0F6C5CE7) else Ag.Card)
                    .agClick(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) Ag.Grad
                            else SolidColor(tint.copy(alpha = Ag.ricAlpha(tint)))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon, null,
                        tint = if (selected) Color.White else tint,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Ag.Tx)
                    if (!desc.isNullOrBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(desc, fontSize = 12.sp, lineHeight = 17.5.sp, color = Ag.Tx2, maxLines = 2)
                    }
                }
                Spacer(Modifier.width(10.dp))
                if (selected) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Ag.Grad),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                } else {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(Ag.TintInk)
                            .border(1.dp, Ag.Line, RoundedCornerShape(99.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            trailingHint ?: "选择",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ag.Tx2,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
