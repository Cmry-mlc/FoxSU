package me.weishu.kernelsu.ui.screen.ag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.component.StylePickerDialog
import me.weishu.kernelsu.ui.component.rebootlistpopup.getRebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.rememberRebootAction
import me.weishu.kernelsu.ui.screen.settings.SettingsScreenActions
import me.weishu.kernelsu.ui.screen.settings.SettingsUiState
import me.weishu.kernelsu.ui.util.DynamicManager
import kotlinx.coroutines.launch

/**
 * 晨曦玻璃 · 设置（设计稿 p-set）。
 * 通用 / 模块与 Profile / Root 与安全 / 关于 四组；开关与入口全部接真实功能。
 */
@Composable
fun AgSettings(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: androidx.compose.ui.unit.Dp,
    onOpenCustomization: () -> Unit,
    onOpenAuthor: () -> Unit,
) {
    val context = LocalContext.current
    var showStyle by remember { mutableStateOf(false) }
    var showReboot by remember { mutableStateOf(false) }
    var showDynamic by remember { mutableStateOf(false) }
    var dynamicBusy by remember { mutableStateOf(false) }
    var dynamicMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val reboot = rememberRebootAction()

    val styleName = when (LocalUiMode.current.value) {
        "miuix" -> "晨曦玻璃"
        "fresh" -> "Pages"
        "material" -> "Editorial"
        "glass" -> "Daylight"
        "nocturne" -> "Nocturne"
        else -> "晨曦玻璃"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ag.background())
            .statusBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (CustomizationStore.current.dockStyle == "v5") 106.dp else 18.dp,
                end = 18.dp, bottom = bottomInnerPadding + 110.dp),
        ) {
            item { AgTitle("设", "置", topMargin = 12.dp) }

            // ── 通用 ──
            item { AgGHead("通用") }
            item {
                AgCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    AgToggleRow(
                        stringResource(R.string.settings_check_update), stringResource(R.string.settings_check_update_summary),
                        Ag.Vio, AgSettingsIcons.CheckUpdate,
                        trailing = { AgToggle(checked = uiState.checkUpdate, onCheckedChange = { actions.onSetCheckUpdate(!uiState.checkUpdate) }) },
                    )
                    AgToggleRow(
                        stringResource(R.string.settings_module_check_update), stringResource(R.string.settings_check_update_summary),
                        Ag.Mag, AgSettingsIcons.Modules,
                        trailing = { AgToggle(checked = uiState.checkModuleUpdate, onCheckedChange = { actions.onSetCheckModuleUpdate(!uiState.checkModuleUpdate) }) },
                    )
                    AgRow(
                        "界面风格", "选择应用的界面风格",
                        Ag.Vio, AgSettingsIcons.Style,
                        trailing = {
                            Text("$styleName ⇅", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ag.Vio)
                        },
                        onClick = { showStyle = true },
                    )
                    AgRow(
                        "定制功能", "背景壁纸、卡片样式与启动器图标",
                        Ag.Amber, AgSettingsIcons.Customize, showChev = true, onClick = onOpenCustomization,
                    )
                }
            }

            // ── 模块与 Profile ──
            item { AgGHead("模块与 Profile") }
            item {
                AgCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    AgRow(
                        stringResource(R.string.settings_profile_template), stringResource(R.string.settings_profile_template_summary),
                        Ag.Teal, AgSettingsIcons.Modules, showChev = true, onClick = actions.onOpenProfileTemplate)
                    AgToggleRow(
                        "卸载模块（内核级）", "在内核给需要的应用卸载模块",
                        Ag.Vio, AgSettingsIcons.Moon,
                        trailing = { AgToggle(checked = uiState.isKernelUmountEnabled, onCheckedChange = { actions.onSetKernelUmountEnabled(!uiState.isKernelUmountEnabled) }) },
                    )
                    AgToggleRow(
                        "默认卸载模块", "启用模块默认卸载行为",
                        Ag.Mag, AgSettingsIcons.Modules,
                        trailing = { AgToggle(checked = uiState.isDefaultUmountModules, onCheckedChange = { actions.onSetDefaultUmountModules(!uiState.isDefaultUmountModules) }) },
                    )
                    AgToggleRow(
                        "启用 Web 调试", "允许模块 WebUI 的调试模式",
                        Ag.Amber, AgSettingsIcons.LocalTemplate,
                        trailing = { AgToggle(checked = uiState.enableWebDebugging, onCheckedChange = { actions.onSetEnableWebDebugging(!uiState.enableWebDebugging) }) },
                        divider = false,
                    )
                }
            }

            // ── Root 与安全 ──
            item { AgGHead("Root 与安全") }
            item {
                AgCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    AgToggleRow(
                        "默认 SU 授权", uiState.suCompatStatus.ifBlank { if (uiState.isSuEnabled) "已启用" else "已停用" },
                        Ag.Vio, AgSettingsIcons.Shield,
                        trailing = { AgToggle(checked = uiState.isSuEnabled, onCheckedChange = { actions.onSetSuCompatMode(if (uiState.isSuEnabled) 1 else 0) }) },
                    )
                    AgToggleRow(
                        "隐藏 SELinux 修改", "阻止应用检测 SELinux 修改",
                        Ag.Teal, AgSettingsIcons.EyeOff,
                        trailing = { AgToggle(checked = uiState.isSelinuxHideEnabled, onCheckedChange = { actions.onSetSelinuxHideEnabled(!uiState.isSelinuxHideEnabled) }) },
                    )
                    AgToggleRow(
                        "SU 日志", "将 Root 相关事件记录到 KernelSU 的 SU 日志",
                        Ag.Mag, AgSettingsIcons.Log,
                        trailing = { AgToggle(checked = uiState.isSulogEnabled, onCheckedChange = { actions.onSetSulogEnabled(!uiState.isSulogEnabled) }) },
                    )
                    AgRow(
                        "设为动态管理器",
                        "把当前 App 登记为内核管理器，换包名/签名后也能识别",
                        Ag.Vio, AgSettingsIcons.Shield, showChev = true,
                        onClick = { showDynamic = true },
                    )
                    AgRow(
                        "重启管理", "重启 / 软重启 / Recovery / Bootloader / EDL",
                        Ag.Amber, AgSettingsIcons.SoftReboot, showChev = true, onClick = { showReboot = true }, divider = false)
                }
            }

            // ── 关于 ──
            item { AgGHead("关于") }
            item {
                AgCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    AgRow(
                        "FoxSu Manager",
                        "版本 ${me.weishu.kernelsu.BuildConfig.VERSION_NAME} · 基于 KernelSU 开源项目",
                        Ag.Vio, AgSettingsIcons.Manager, showChev = true, onClick = onOpenAuthor,
                    )
                    AgRow(
                        stringResource(R.string.home_support_title), stringResource(R.string.home_support_desc),
                        Ag.Mag, AgHomeIcons.Heart, showChev = true,
                        onClick = { actions.onOpenAbout() },
                    )
                    AgRow(
                        "GitHub 仓库", "查看源码与提交 Issue",
                        Ag.Amber, AgSettingsIcons.Github, showChev = true,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Cmry-mlc/FoxSu"))
                                )
                            }
                        },
                    )
                    AgRow(
                        "加入频道", "QQ 频道【FoxSu】交流与反馈",
                        Ag.Teal, AgSettingsIcons.Chat, showChev = true,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://pd.qq.com/s/8glxclfk6?b=9"))
                                )
                            }
                        }, divider = false)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showStyle) {
        StylePickerDialog(current = LocalUiMode.current, onDismiss = { showStyle = false })
    }
    if (showReboot) {
        AgRebootSheet(onDismiss = { showReboot = false })
    }

    if (showDynamic) {
        AlertDialog(
            onDismissRequest = { if (!dynamicBusy) showDynamic = false },
            title = { Text("设为动态管理器", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    dynamicMsg ?: "KernelSU 默认按「包名 + 签名」认管理器。\n" +
                        "设为动态管理器后，内核直接登记当前 App（appid ${DynamicManager.selfAppId()}），" +
                        "换包名或被重新签名也能被识别。\n\n" +
                        "需要内核开放 ksu_debug_manager_appid 接口；不支持时会如实提示。",
                    fontSize = 13.5.sp, lineHeight = 21.sp,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !dynamicBusy,
                    onClick = {
                        dynamicBusy = true
                        scope.launch {
                            val r = DynamicManager.registerSelf(context.packageName)
                            dynamicMsg = r.getOrElse { it.message ?: "设置失败" }
                            dynamicBusy = false
                        }
                    },
                ) { Text(if (dynamicBusy) "设置中…" else "设为动态管理器") }
            },
            dismissButton = {
                TextButton(enabled = !dynamicBusy, onClick = { showDynamic = false; dynamicMsg = null }) {
                    Text("关闭")
                }
            },
        )
    }
}
