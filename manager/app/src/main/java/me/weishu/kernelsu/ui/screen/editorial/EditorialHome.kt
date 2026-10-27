package me.weishu.kernelsu.ui.screen.editorial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.component.rebootlistpopup.getRebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.rememberRebootAction
import me.weishu.kernelsu.ui.screen.home.HomeActions
import me.weishu.kernelsu.ui.screen.home.HomeUiState
import me.weishu.kernelsu.ui.screen.home.freshIssues
import me.weishu.kernelsu.ui.uiModeLabelRes

/**
 * FoxSu Editorial 主页 —— 杂志排版：
 * 通栏 masthead（FoxSu / ROOT / JOURNAL / VOL）→ 墨色状态带 → 编号区块 + 发丝线条目
 * → 内核代码条 → 底部通栏 tab。
 */
@Composable
fun EditorialHome(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
    navigationBadge: NavigationBadgeState,
    onOpenTab: (Int) -> Unit,
    onOpenAuthor: () -> Unit = {},
) {
    val t = edTokens()
    val issues = state.freshIssues()
    var showReboot by remember { mutableStateOf(false) }
    var showStyle by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(t.paper)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scroll),
        ) {
            EdMasthead(
                brand = "FoxSu",
                tag = "ROOT",
                left = "Kernel Management Journal",
                right = "VOL.${BuildConfig.VERSION_CODE}",
                actions = listOf(
                    EdMastAction(R.drawable.ic_d_user, onOpenAuthor),
                    EdMastAction(R.drawable.ic_d_palette) { showStyle = true },
                    EdMastAction(R.drawable.ic_d_power2) { showReboot = true },
                ),
            )

            EdStatusBand(
                kicker = "CURRENT STATUS · 当前状态",
                big = if (issues.isEmpty()) "工作中" else "需注意",
                kvs = listOf(
                    Triple("版本", BuildConfig.VERSION_NAME, false),
                    Triple("模式", if (state.lkmMode == true) "LKM · 越狱" else "GKI", false),
                    Triple("界面", stringResource(uiModeLabelRes(LocalUiMode.current)), true),
                ),
            )

            EdSection("01", "设备概况")
            EdRow(
                glyph = "▢", glyphBg = t.cyan.copy(alpha = 0.10f), glyphColor = t.cyan,
                title = stringResource(R.string.home_device_model),
                subtitle = stringResource(R.string.fresh_home_device_sub),
                trailing = {
                    Text(
                        text = state.systemInfo.deviceModel.ifBlank { "—" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = t.ink,
                        maxLines = 1,
                    )
                },
            )
            EdRow(
                glyph = "◈", glyphBg = t.violet.copy(alpha = 0.10f), glyphColor = t.violet,
                title = stringResource(R.string.superuser),
                subtitle = stringResource(R.string.fresh_home_superuser_sub),
                trailing = { EdNum(navigationBadge.superuserCount.toString(), t.violet) },
                onClick = { onOpenTab(1) },
            )
            EdRow(
                glyph = "▦", glyphBg = t.rose.copy(alpha = 0.10f), glyphColor = t.rose,
                title = stringResource(R.string.module),
                subtitle = stringResource(R.string.fresh_home_module_sub),
                trailing = { EdNum(navigationBadge.moduleEnabledCount.toString(), t.rose) },
                onClick = { onOpenTab(2) },
            )

            EdSection("02", "安全状态")
            EdRow(
                glyph = "🔒", glyphBg = t.lime.copy(alpha = 0.10f), glyphColor = t.lime,
                title = "SELinux",
                subtitle = stringResource(R.string.glass_home_selinux_sub),
                trailing = {
                    EdVal(
                        text = if (state.isSELinuxPermissive) stringResource(R.string.selinux_status_permissive)
                        else stringResource(R.string.selinux_status_enforcing),
                        pill = if (state.isSELinuxPermissive) "Permissive" else "Enforcing",
                        pillColor = t.lime,
                        filled = !state.isSELinuxPermissive,
                    )
                },
            )
            EdRow(
                glyph = "⚙", glyphBg = t.amber.copy(alpha = 0.10f), glyphColor = t.amber,
                title = "Seccomp",
                subtitle = stringResource(R.string.glass_home_seccomp_sub),
                trailing = {
                    EdVal(
                        text = stringResource(seccompLabelRes(state.systemInfo.seccompStatus)),
                        pill = seccompShort(state.systemInfo.seccompStatus),
                        pillColor = t.amber,
                    )
                },
            )

            EdSection("03", "了解更多")
            EdRow(
                glyph = "♡", glyphBg = t.acc.copy(alpha = 0.10f), glyphColor = t.acc,
                title = stringResource(R.string.glass_home_support),
                subtitle = stringResource(R.string.glass_home_support_sub),
                trailing = { Text("›", fontSize = 13.sp, color = t.faint) },
                onClick = { actions.onOpenUrl("https://github.com/Cmry-mlc/FoxSu") },
            )
            EdRow(
                glyph = "❑", glyphBg = t.ink2.copy(alpha = 0.08f), glyphColor = t.ink2,
                title = stringResource(R.string.glass_home_learn),
                subtitle = stringResource(R.string.glass_home_learn_sub),
                trailing = { Text("›", fontSize = 13.sp, color = t.faint) },
                onClick = { actions.onOpenUrl("https://kernelsu.org") },
            )

            EdCode(state.systemInfo.kernelVersion.ifBlank { "—" }, label = "内核版本")
            EdCode(state.systemInfo.fingerprint.ifBlank { "—" }, label = "指纹信息")
            Spacer(Modifier.height(bottomInnerPadding + 20.dp))
        }
    }

    if (showReboot) {
        EdRebootSheet(onDismiss = { showReboot = false })
    }
    if (showStyle) {
        EdStyleSheet(onDismiss = { showStyle = false })
    }
}

private fun seccompShort(status: Int): String = when (status) {
    2 -> "Strict"
    1 -> "Filter"
    -1 -> "Off"
    else -> "N/A"
}

@androidx.annotation.StringRes
private fun seccompLabelRes(status: Int): Int = when (status) {
    2 -> R.string.seccomp_status_strict
    1 -> R.string.seccomp_status_filter
    -1 -> R.string.seccomp_status_disabled
    else -> R.string.seccomp_status_not_supported
}

/** 编辑风重启面板（.ovl + .sheet：墨色描边、编号选项、底部分隔按钮） */
@Composable
fun EdRebootSheet(onDismiss: () -> Unit) {
    val t = edTokens()
    val options = getRebootListOption()
    val doReboot = rememberRebootAction()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(t.paper)
                .border(1.5.dp, t.ink, RoundedCornerShape(20.dp))
                .padding(top = 16.dp, bottom = 14.dp),
        ) {
            // .sh-kick
            Text(
                text = "REBOOT · 电源菜单",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.5.sp,
                color = t.acc,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
            // .sh-t：标题 + 右侧说明
            Row(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 3.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = stringResource(R.string.reboot),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = t.ink,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "选择一种重启方式",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = t.sub,
                    maxLines = 1,
                )
            }
            options.forEachIndexed { index, option ->
                val desc = rebootDesc(option.reason)
                val primary = index == 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .hairBottom(t.hair)
                        .clickable {
                            onDismiss()
                            doReboot(option.reason)
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "%02d".format(index + 1),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = t.faint,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(22.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(option.labelRes),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (primary) t.acc else t.ink,
                            maxLines = 1,
                        )
                        Text(
                            text = desc,
                            fontSize = 8.5.sp,
                            color = t.sub,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                    Text(
                        text = if (primary) "↻" else "›",
                        fontSize = 13.sp,
                        color = t.faint,
                    )
                }
            }
            // .cancel：墨色描边圆角按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, t.ink, RoundedCornerShape(10.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(android.R.string.cancel),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = t.ink,
                )
            }
        }
    }
}

/** 重启方式说明（对应设计稿 .rd 文案） */
private fun rebootDesc(reason: String): String = when (reason) {
    "" -> "完整重启系统 Reboot"
    "userspace" -> "仅重启系统界面 Userspace"
    "soft_reboot" -> "仅重启系统界面 Soft Reboot"
    "recovery" -> "进入第三方 Recovery 模式"
    "bootloader" -> "Fastboot 引导模式"
    "download" -> "Odin 刷机下载模式"
    "edl" -> "高通紧急下载模式（9008）"
    else -> ""
}

/** 界面风格切换（编辑风） */
@Composable
fun EdStyleSheet(onDismiss: () -> Unit) {
    val t = edTokens()
    val repo = remember { me.weishu.kernelsu.data.repository.SettingsRepositoryImpl() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(t.paper)
                .border(1.5.dp, t.ink, RoundedCornerShape(20.dp))
                .padding(vertical = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_ui_mode),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = t.ink,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            )
            me.weishu.kernelsu.ui.UiMode.entries.forEach { mode ->
                val selected = mode == LocalUiMode.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            repo.uiMode = mode.value
                            onDismiss()
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(uiModeLabelRes(mode)),
                        fontSize = 13.5.sp,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.Normal,
                        color = if (selected) t.acc else t.ink,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) Text("●", fontSize = 10.sp, color = t.acc)
                }
            }
        }
    }
}
