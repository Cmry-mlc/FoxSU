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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.component.rebootlistpopup.getRebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.rememberRebootAction
import me.weishu.kernelsu.ui.component.StylePickerDialog
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.screen.home.HomeActions
import me.weishu.kernelsu.ui.screen.home.HomeUiState
import me.weishu.kernelsu.ui.screen.home.HeroPhase
import me.weishu.kernelsu.ui.screen.home.heroState

/**
 * 晨曦玻璃 · 主页（设计稿 p-home 完整实现）。
 * 状态卡（渐变描边 + 白芯 + 右上 64dp 环形勾）→ 统计卡 ×2 → 信息卡 → 安全状态卡 → 支持卡。
 */
@Composable
fun AgHome(
    state: HomeUiState,
    actions: HomeActions,
    onOpenTab: (Int) -> Unit,
    onOpenAuthor: () -> Unit,
    bottomInnerPadding: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
) {
    val hero = state.heroState()
    var showReboot by remember { mutableStateOf(false) }
    val sideDock = CustomizationStore.current.dockStyle == "v5"
    val settingsVm: me.weishu.kernelsu.ui.viewmodel.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val sysDark = androidx.compose.foundation.isSystemInDarkTheme()
    val onToggleDark = {
        val repo = me.weishu.kernelsu.data.repository.SettingsRepositoryImpl()
        val cur = repo.themeMode
        val target = when (cur) {
            2, 5, 6 -> 1          // 当前深 → 浅
            1, 4 -> 2             // 当前浅 → 深
            else -> if (sysDark) 1 else 2 // 跟随系统 → 取反
        }
        settingsVm.setThemeMode(target)
        Unit
    }
    val listBottomPad = if (bottomInnerPadding == androidx.compose.ui.unit.Dp.Unspecified) {
        24.dp
    } else {
        bottomInnerPadding + 24.dp
    }

    // 固定顶栏 + 毛玻璃：不随内容滚动（dev-handoff .headwrap 要求）
    Column(
        Modifier
            .fillMaxSize()
            .background(Ag.background())
            .statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Ag.headSurface())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            // ── 顶栏：左侧渐变标题 + 右侧 作者/深色切换/电源（设计稿 .top：标题左对齐）──
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (sideDock) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_fox_global),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    "FoxSu",
                    fontSize = if (sideDock) 26.sp else 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.6).sp,
                    style = TextStyle(brush = Ag.TitleGrad),
                )
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AgIconBtn(Icons.Rounded.Person, onClick = onOpenAuthor)
                    // 深色模式切换（月亮）：浅↔深真实切换
                    AgIconBtn(Icons.Rounded.DarkMode, onClick = onToggleDark)
                    AgIconBtn(Icons.Rounded.PowerSettingsNew, onClick = { showReboot = true })
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        // ── 内容滚动区（从毛玻璃下透出）；侧边抽屉模式整体让距 106dp ──
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = if (sideDock) 106.dp else 18.dp,
                    end = 18.dp,
                ),
        ) {
            Spacer(Modifier.height(12.dp))

        // ── 状态卡：外层渐变底 2px + 内层 22dp 白芯 ──
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Ag.Grad)
                                .padding(2.dp)
                .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = Color(0x406C5CE7)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Ag.CardSolid)
                    .padding(18.dp, 18.dp, 16.dp, 16.dp),
            ) {
                AgStatusRing(ok = hero.phase == HeroPhase.WORKING || hero.phase == HeroPhase.PENDING_REBOOT)
                Column {
                    AgBadge(
                        when (hero.phase) {
                            HeroPhase.WORKING -> if (hero.jailbreakMode) "工作中 · 越狱模式" else "工作中"
                            HeroPhase.PENDING_REBOOT -> "已刷入 · 待重启"
                            HeroPhase.FLASHING -> "刷写中"
                            HeroPhase.ATTENTION -> "需注意"
                            HeroPhase.NOT_INSTALLED -> "未安装"
                            HeroPhase.FLASH_FAILED -> "刷写失败"
                            HeroPhase.UNSUPPORTED -> "设备不支持"
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.padding(end = 70.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hero.modeLabel != null) AgChip(hero.modeLabel)
                        AgChip("版本 ${state.ksuVersion ?: "—"}")
                        AgChip("${hero.versionName}${if (state.showCustomLkmBadge) " · 自定义" else ""}")
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 2.dp)) {
                        AgButton(primary = true, text = stringResource(R.string.home_check_update), onClick = actions.onCheckUpdate, modifier = Modifier.weight(1f))
                        AgButton(primary = false, text = stringResource(R.string.home_refresh_image), onClick = actions.onRefreshImage, modifier = Modifier.weight(1f), enabled = hero.canFlash)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // ── 统计卡 ×2（数字来自真实 ViewModel 数据）──
        val suVm: me.weishu.kernelsu.ui.viewmodel.SuperUserViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        val modVm: me.weishu.kernelsu.ui.viewmodel.ModuleViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        androidx.compose.runtime.LaunchedEffect(Unit) {
            suVm.loadAppList()
            modVm.fetchModuleList()
        }
        val suState by suVm.uiState.collectAsStateWithLifecycle()
        val modState by modVm.uiState.collectAsStateWithLifecycle()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AgStatCard(
                "超级用户",
                "${suState.groupedApps.count { it.anyAllowSu }}",
                "已授权应用", Modifier.weight(1f),
            ) { onOpenTab(1) }
            AgStatCard(
                "模块",
                "${modState.modules.count { it.enabled }}",
                "已启用模块", Modifier.weight(1f),
            ) { onOpenTab(2) }
        }
        Spacer(Modifier.height(14.dp))

        // ── 信息卡 ──
        AgCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
            AgRow(stringResource(R.string.home_manager_version), "${state.ksuVersion ?: "—"}（${hero.versionName}）", Ag.Vio, Icons.Rounded.Numbers)
            AgRow(stringResource(R.string.home_kernel_version), state.systemInfo.kernelVersion, Ag.Vio, Icons.Rounded.Memory)
            AgRow(stringResource(R.string.home_device_model), state.systemInfo.deviceModel, Ag.Mag, Icons.Rounded.PhoneAndroid)
            AgRow(stringResource(R.string.home_system_fingerprint), state.systemInfo.fingerprint, Ag.Teal, Icons.Rounded.Fingerprint)
        }
        Spacer(Modifier.height(14.dp))

        // ── 安全状态卡 ──
        AgCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
            AgRow(stringResource(R.string.home_selinux_status), when (state.systemInfo.selinuxStatus) {
                    "Enforcing" -> "强制执行"
                    "Permissive" -> "宽松模式"
                    "Disabled" -> "已禁用"
                    else -> "未知"
                }, Ag.Teal, Icons.Rounded.Shield)
            AgRow(
                stringResource(R.string.home_seccomp_status),
                when (state.systemInfo.seccompStatus) {
                    -1 -> "内核未开启"
                    0 -> "未启用"
                    1 -> "严格模式"
                    2 -> "过滤模式"
                    else -> "未知"
                },
                Ag.Teal, Icons.Rounded.Tune,
            )
        }
        Spacer(Modifier.height(14.dp))

        // ── 支持开发 / 了解 KernelSU ──
        AgCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
            AgRow(
                stringResource(R.string.home_support_title),
                stringResource(R.string.home_support_desc),
                Ag.Amber, Icons.Rounded.Favorite, showChev = true,
            )
            AgRow(
                stringResource(R.string.home_learn_kernelsu),
                stringResource(R.string.home_learn_kernelsu_desc),
                Ag.Vio, Icons.Rounded.Info, showChev = true,
                onClick = { actions.onOpenUrl("https://kernelsu.org/") },
            )
        }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showReboot) {
        AgRebootSheet(onDismiss = { showReboot = false })
    }
}

/** 右上 64dp 环形勾 .okring */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.AgStatusRing(ok: Boolean) {
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .padding(top = 14.dp, end = 14.dp)
            .size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (ok) androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(Color(0x2414B8A6), Color.Transparent)
                    ) else androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(Color(0x24F59E0B), Color.Transparent)
                    )
                ),
        )
        Box(
            Modifier
                .size(46.dp)
                .border(4.dp, if (ok) Ag.Success else Ag.Amber, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Check, null,
                tint = if (ok) Ag.Success else Ag.Amber,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

/** 统计卡 .stat */
@Composable
private fun androidx.compose.foundation.layout.RowScope.AgStatCard(
    label: String,
    num: String,
    sub: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AgCard(modifier, radius = 18.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), onClick = onClick) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ag.Tx2)
        Text(num, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, color = Ag.Vio, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
        Text(sub, fontSize = 11.5.sp, color = Ag.Tx3)
    }
}
