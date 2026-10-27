package me.weishu.kernelsu.ui.screen.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.uiModeLabelRes
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.screen.home.HomeActions
import me.weishu.kernelsu.ui.screen.home.HomeUiState
import me.weishu.kernelsu.ui.screen.home.freshIssues
import me.weishu.kernelsu.ui.theme.glassTokens
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext


/** FoxSu Glass 主页：黑/白玻璃 + 双霓虹描边英雄卡 + 霓虹统计卡。 */
@Composable
fun GlassHome(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
    navigationBadge: NavigationBadgeState,
    onOpenTab: (Int) -> Unit,
) {
    val t = glassTokens()
    val issues = state.freshIssues()
    var showStylePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 13.dp, end = 13.dp, top = 8.dp, bottom = bottomInnerPadding + 90.dp),
    ) {
        item {
            GlassTopBar(
                title = "FoxSu",
                subtitle = versionSubtitle(state),
                onStyleClick = { showStylePicker = true },
            )
        }
        item {
            GlassHeroCard(
                state = state,
                issueCount = issues.size,
                onInstall = actions.onInstallClick,
                onJailbreak = actions.onJailbreakClick,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                GlassNCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_phone,
                    title = stringResource(R.string.home_device_model),
                    value = state.systemInfo.deviceModel,
                    caption = stringResource(R.string.fresh_home_device_sub),
                    accent = t.cyan,
                )
                GlassNCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_palette,
                    title = stringResource(R.string.settings_ui_mode),
                    value = stringResource(uiModeLabelRes(LocalUiMode.current)),
                    caption = stringResource(R.string.fresh_home_style_switch),
                    accent = t.violet,
                    onClick = { showStylePicker = true },
                )
            }
        }
        item { Spacer(Modifier.height(11.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                GlassNCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_shield,
                    title = stringResource(R.string.superuser),
                    value = navigationBadge.superuserCount.toString(),
                    caption = stringResource(R.string.fresh_home_superuser_sub),
                    accent = t.lime,
                    onClick = { onOpenTab(1) },
                )
                GlassNCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_grid,
                    title = stringResource(R.string.module),
                    value = navigationBadge.moduleEnabledCount.toString(),
                    caption = stringResource(R.string.fresh_home_module_sub),
                    accent = t.pink,
                    onClick = { onOpenTab(2) },
                )
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            GlassWide(
                iconRes = R.drawable.ic_d_chip,
                title = stringResource(R.string.home_kernel),
                subtitle = state.systemInfo.kernelVersion,
                accent = t.blue,
            )
        }
        item {
            GlassWide(
                iconRes = R.drawable.ic_d_chip,
                title = stringResource(R.string.home_fingerprint),
                subtitle = state.systemInfo.fingerprint,
                accent = t.mint,
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

@Composable
private fun versionSubtitle(state: HomeUiState): String {
    val ctx = LocalContext.current
    return ctx.getString(R.string.app_name) + " · " + BuildConfigVersion()
}

private fun BuildConfigVersion(): String = me.weishu.kernelsu.BuildConfig.VERSION_NAME

/** .top：46 圆头像 + 22/800 标题 + 11 sub 副标题 + 右侧两个 38x34 胶囊按钮 */
@Composable
fun GlassTopBar(
    title: String,
    subtitle: String,
    onStyleClick: () -> Unit = {},
) {
    val t = glassTokens()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 16.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (t.isDark) Color(0xFF050508) else Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = t.flame,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = t.ink)
            Text(subtitle, fontSize = 11.sp, color = t.sub, modifier = Modifier.padding(top = 3.dp))
        }
        Spacer(Modifier.weight(1f))
        GlassTopButton(iconRes = R.drawable.ic_d_palette, onClick = onStyleClick)
    }
}

@Composable
private fun GlassTopButton(iconRes: Int, onClick: () -> Unit) {
    val t = glassTokens()
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 34.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (t.isDark) Color(0x0DFFFFFF) else Color(0x99FFFFFF))
            .border(1.dp, t.glassBorder, RoundedCornerShape(99.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = t.ink2,
        )
    }
}

/** .hero：玻璃卡 + 120° 霓虹描边 + gline 渐变小字 + tag 组 + 右侧 seal 圆 */
@Composable
fun GlassHeroCard(
    state: HomeUiState,
    issueCount: Int,
    onInstall: () -> Unit,
    onJailbreak: () -> Unit,
) {
    val t = glassTokens()
    val lkm = state.lkmMode == true
    if (issueCount > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(t.amber.copy(alpha = 0.14f))
                .border(1.dp, t.amber.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_d_shield),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = t.amber,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.fresh_home_issue_count, issueCount),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = t.amber,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.fresh_home_install),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = t.ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(t.flame.copy(alpha = 0.9f))
                    .clickable(onClick = onInstall)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(11.dp))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 13.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(t.glassPanel)
            .border(1.dp, t.glassBorder, RoundedCornerShape(26.dp))
            .padding(20.dp),
    ) {
        Column {
            Text("FOXSU GLASS", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp,
                style = androidx.compose.ui.text.TextStyle(
                    brush = t.glineGrad,
                ))
            Text(
                text = if (lkm) "LKM" else "GKI",
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                color = t.ink,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassTag(
                    text = if (lkm) "LKM" else "GKI",
                    color = t.amber,
                )
                GlassTag(
                    text = if (state.isRootAvailable) "ROOT" else "NO ROOT",
                    color = t.violet,
                )
                GlassTag(
                    text = "v" + BuildConfigVersion(),
                    color = t.cyan,
                )
            }
            Text(
                text = stringResource(R.string.glass_home_hint),
                fontSize = 10.5.sp,
                color = t.faint,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        // seal：右侧 60 圆
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(60.dp)
                .clip(CircleShape)
                .background(if (t.isDark) Color(0xFF0B0B12) else Color(0xBFFFFFFF))
                .border(1.dp, t.glassBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = t.violet,
            )
        }
    }
}

@Composable
fun GlassTag(text: String, color: Color) {
    val t = glassTokens()
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = if (t.isDark) 0.14f else 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** .ncard：22 圆角玻璃卡，右上角霓虹光晕 */
@Composable
fun GlassNCard(
    modifier: Modifier = Modifier,
    iconRes: Int,
    title: String,
    value: String,
    caption: String,
    accent: Color,
    onClick: (() -> Unit)? = null,
) {
    val t = glassTokens()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(t.panel2)
            .border(1.dp, t.glassBorder, RoundedCornerShape(22.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(15.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(74.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (t.isDark) 0.16f else 0.28f)),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            GlassChip(iconRes = iconRes, accent = accent)
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = t.sub)
            Text(
                value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                lineHeight = 22.sp,
                maxLines = 2,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(caption, fontSize = 10.sp, color = t.faint, modifier = Modifier.padding(top = 15.dp))
        }
    }
}

/** .chip：38 圆角12 玻璃 chip */
@Composable
fun GlassChip(iconRes: Int, accent: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (glassTokens().isDark) Color(0x0FFFFFFF) else Color(0x99FFFFFF))
            .border(1.dp, glassTokens().glassBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = accent,
        )
    }
}

/** .wide：20 圆角横条 */
@Composable
fun GlassWide(
    iconRes: Int,
    title: String,
    subtitle: String,
    accent: Color,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val t = glassTokens()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(t.glassPanel)
            .border(1.dp, t.glassBorder, RoundedCornerShape(20.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassChip(iconRes = iconRes, accent = accent)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = t.ink)
            Text(
                subtitle,
                fontSize = 10.5.sp,
                color = t.sub,
                lineHeight = 14.sp,
                maxLines = 2,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        trailing?.invoke(this)
    }
}

/** 界面风格切换弹窗（Glass 版） */
@Composable
fun GlassStylePickerDialog(
    current: UiMode,
    onDismiss: () -> Unit,
) {
    val repo = remember { me.weishu.kernelsu.data.repository.SettingsRepositoryImpl() }
    val t = glassTokens()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (t.isDark) Color(0xFF16161F) else Color.White,
        titleContentColor = t.ink,
        textContentColor = t.ink2,
        title = { Text(stringResource(R.string.settings_ui_mode), fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                UiMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                repo.uiMode = mode.value
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(uiModeLabelRes(mode)),
                            fontSize = 14.sp,
                            fontWeight = if (mode == current) FontWeight.Bold else FontWeight.Normal,
                            color = if (mode == current) t.violet else t.ink2,
                            modifier = Modifier.weight(1f),
                        )
                        if (mode == current) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_d_shield),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = t.violet,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel), color = t.sub) }
        },
    )
}
