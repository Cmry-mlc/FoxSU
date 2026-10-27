package me.weishu.kernelsu.ui.screen.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.screen.settings.SettingsScreenActions
import me.weishu.kernelsu.ui.screen.settings.SettingsUiState
import me.weishu.kernelsu.ui.theme.glassTokens
import me.weishu.kernelsu.ui.uiModeLabelRes

/** .srow：20 圆角玻璃设置行 —— chip + (sT 13.5/700 + sS 10 faint) + 右侧 st 徽章 / .tog / ›箭头 */
private data class GlassSettingRow(
    val iconRes: Int,
    val title: String,
    val subtitle: String,
    val accent: Long,
    val stText: String? = null,
    val stColor: Long = 0xFF6EF08A,
    val checked: Boolean? = null,
    val showChevron: Boolean = false,
    val onClick: () -> Unit = {},
)

@Composable
private fun GlassSettingRowComposable(row: GlassSettingRow) {
    val t = glassTokens()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(t.glassPanel)
            .border(1.dp, t.glassBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = row.onClick)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassChip(iconRes = row.iconRes, accent = Color(row.accent))
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(row.title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = t.ink)
            Text(
                row.subtitle,
                fontSize = 10.sp,
                color = t.faint,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        when {
            row.stText != null -> {
                Spacer(Modifier.width(8.dp))
                GlassStBadge(text = row.stText, color = Color(row.stColor))
            }
            row.checked != null -> {
                Spacer(Modifier.width(8.dp))
                GlassToggle(checked = row.checked, gradientOn = false, onClick = row.onClick)
            }
            row.showChevron -> {
                Spacer(Modifier.width(8.dp))
                Text("›", fontSize = 16.sp, color = t.faint)
            }
        }
    }
}

@Composable
private fun GlassSettingGroup(title: String, rows: List<GlassSettingRow>) {
    Column {
        GlassSectionHead(title)
        rows.forEach { GlassSettingRowComposable(it) }
    }
}

/** Glass 设置页整页：头像标题行 + 通用 / 外观 / 安全与调试 三组（同设计稿） */
@Composable
fun GlassSettingsList(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
    onOpenCustomization: () -> Unit = {},
    onOpenSulog: () -> Unit = {},
) {
    var showStylePicker by remember { mutableStateOf(false) }
    val t = glassTokens()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 13.dp, end = 13.dp, top = 8.dp, bottom = bottomInnerPadding + 96.dp),
    ) {
        item {
            GlassTopBar(
                title = stringResource(R.string.settings),
                subtitle = stringResource(R.string.glass_settings_sub),
                titleSize = 19,
            )
        }
        item {
            GlassSettingGroup(
                title = stringResource(R.string.glass_group_general),
                rows = listOf(
                    GlassSettingRow(
                        iconRes = R.drawable.ic_d_download,
                        title = stringResource(R.string.settings_check_update),
                        subtitle = stringResource(
                            R.string.glass_version_fmt,
                            me.weishu.kernelsu.BuildConfig.VERSION_NAME
                        ),
                        accent = 0xFF5CE1E6,
                        stText = stringResource(R.string.glass_st_latest),
                        stColor = 0xFF6EF08A,
                    ),
                    GlassSettingRow(
                        iconRes = R.drawable.ic_d_refresh,
                        title = stringResource(R.string.settings_module_check_update),
                        subtitle = stringResource(R.string.settings_check_update_summary),
                        accent = 0xFFFFB35C,
                        checked = uiState.checkModuleUpdate,
                        onClick = { actions.onSetCheckModuleUpdate(!uiState.checkModuleUpdate) },
                    ),
                ),
            )
        }
        item {
            GlassSettingGroup(
                title = stringResource(R.string.glass_group_appearance),
                rows = listOf(
                    GlassSettingRow(
                        iconRes = R.drawable.ic_d_palette,
                        title = stringResource(R.string.settings_ui_mode),
                        subtitle = stringResource(uiModeLabelRes(LocalUiMode.current)),
                        accent = 0xFFFF7AC8,
                        showChevron = true,
                        onClick = { showStylePicker = true },
                    ),
                    GlassSettingRow(
                        iconRes = R.drawable.ic_d_image,
                        title = stringResource(R.string.customization_title),
                        subtitle = stringResource(R.string.customization_entry_summary),
                        accent = 0xFFC77DFF,
                        showChevron = true,
                        onClick = onOpenCustomization,
                    ),
                    GlassSettingRow(
                        iconRes = R.drawable.ic_d_tool,
                        title = stringResource(R.string.settings_theme),
                        subtitle = stringResource(R.string.settings_theme_summary),
                        accent = 0xFFFFE45C,
                        showChevron = true,
                        onClick = actions.onOpenTheme,
                    ),
                ),
            )
        }
        item {
            GlassSettingGroup(
                title = stringResource(R.string.glass_group_security),
                rows = listOf(
                    GlassSettingRow(
                        iconRes = R.drawable.ic_d_terminal,
                        title = stringResource(R.string.glass_su_compat),
                        subtitle = uiState.suCompatStatus.ifBlank {
                            stringResource(R.string.settings_sucompat_summary)
                        },
                        accent = 0xFF6EF08A,
                        stText = if (uiState.isSuEnabled) stringResource(R.string.glass_st_on)
                        else stringResource(R.string.glass_st_off),
                        stColor = if (uiState.isSuEnabled) 0xFF6EF08A else 0xFF55525F,
                    ),
                    GlassSettingRow(
                        iconRes = R.drawable.ic_d_log,
                        title = stringResource(R.string.glass_sulog),
                        subtitle = stringResource(R.string.settings_sulog_summary),
                        accent = 0xFFFFB35C,
                        showChevron = true,
                        onClick = onOpenSulog,
                    ),
                    GlassSettingRow(
                        iconRes = R.drawable.ic_d_book,
                        title = stringResource(R.string.settings_grid_about),
                        subtitle = "FoxSu Glass",
                        accent = 0xFF7AA2FF,
                        showChevron = true,
                        onClick = actions.onOpenAbout,
                    ),
                ),
            )
        }
    }

    if (showStylePicker) {
        GlassStylePickerDialog(
            current = LocalUiMode.current,
            onDismiss = { showStylePicker = false },
        )
    }
}
