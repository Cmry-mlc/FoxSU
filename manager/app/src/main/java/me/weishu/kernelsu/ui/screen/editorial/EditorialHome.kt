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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

            EdCode(state.systemInfo.kernelVersion.ifBlank { "—" }, label = "KERNEL")
            EdCode(state.systemInfo.fingerprint.ifBlank { "—" }, label = "FINGERPRINT")
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

/** 编辑风重启面板（弹层 + 上浮缩放进入） */
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
                .padding(vertical = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.reboot),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = t.ink,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            )
            Text(
                text = stringResource(R.string.glass_reboot_hint),
                fontSize = 10.5.sp,
                color = t.sub,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
            )
            Spacer(Modifier.height(10.dp))
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            doReboot(option.reason)
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(option.labelRes),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = t.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text("›", fontSize = 13.sp, color = t.faint)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(android.R.string.cancel),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = t.sub,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 10.dp),
            )
        }
    }
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
