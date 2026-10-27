package me.weishu.kernelsu.ui.screen.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.screen.home.HomeActions
import me.weishu.kernelsu.ui.screen.home.HomeUiState
import me.weishu.kernelsu.ui.screen.home.freshIssues
import me.weishu.kernelsu.ui.theme.glassTokens
import me.weishu.kernelsu.ui.uiModeLabelRes

/** FoxSu Glass 主页：环境光晕背景 + 黑/白玻璃面板 + 双霓虹描边英雄卡。 */
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
        contentPadding = PaddingValues(start = 13.dp, end = 13.dp, top = 8.dp, bottom = bottomInnerPadding + 96.dp),
    ) {
        item {
            GlassTopBar(
                title = "FoxSu",
                subtitle = stringResource(R.string.glass_settings_sub),
                showDot = state.isRootAvailable,
                onStyleClick = { showStylePicker = true },
                onActionClick = actions.onInstallClick,
                actionIconRes = R.drawable.ic_d_download,
            )
        }
        item {
            GlassHeroCard(
                state = state,
                issueCount = issues.size,
                onInstall = actions.onInstallClick,
            )
        }
        item {
            Spacer(Modifier.height(1.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                GlassNCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_phone,
                    title = stringResource(R.string.home_device_model),
                    value = state.systemInfo.deviceModel,
                    caption = stringResource(R.string.fresh_home_device_sub),
                    accent = t.mint,
                )
                GlassNCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_palette,
                    title = stringResource(R.string.settings_ui_mode),
                    value = stringResource(uiModeLabelRes(LocalUiMode.current)),
                    caption = stringResource(R.string.fresh_home_style_switch),
                    accent = t.cyan,
                    onClick = { showStylePicker = true },
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                GlassNCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_shield,
                    title = stringResource(R.string.superuser),
                    value = navigationBadge.superuserCount.toString(),
                    caption = stringResource(R.string.fresh_home_superuser_sub),
                    accent = t.violet,
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
        item {
            Spacer(Modifier.height(1.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                GlassWide(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_lock,
                    title = stringResource(R.string.glass_home_selinux),
                    subtitle = state.systemInfo.selinuxStatus,
                    accent = t.blue,
                    trailing = {
                        GlassStBadge(
                            text = if (state.isSELinuxPermissive) "Permissive" else "Enforcing",
                            color = if (state.isSELinuxPermissive) t.amber else t.lime,
                        )
                    },
                )
                GlassWide(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_sliders,
                    title = stringResource(R.string.home_seccomp_status),
                    subtitle = seccompLabel(state.systemInfo.seccompStatus),
                    accent = t.amber,
                    trailing = {
                        GlassStBadge(
                            text = seccompShort(state.systemInfo.seccompStatus),
                            color = t.amber,
                        )
                    },
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                GlassWide(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_heart,
                    title = stringResource(R.string.glass_home_support),
                    subtitle = stringResource(R.string.glass_home_support_sub),
                    accent = t.flame,
                    showChevron = true,
                    onClick = { actions.onOpenUrl("https://github.com/Cmry-mlc/FoxSu") },
                )
                GlassWide(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_book,
                    title = stringResource(R.string.glass_home_learn),
                    subtitle = stringResource(R.string.glass_home_learn_sub),
                    accent = t.lemon,
                    showChevron = true,
                    onClick = { actions.onOpenUrl("https://kernelsu.org") },
                )
            }
        }
        item {
            GlassWide(
                iconRes = R.drawable.ic_d_chip,
                title = stringResource(R.string.home_kernel),
                subtitle = state.systemInfo.kernelVersion,
                accent = t.lime,
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

private fun seccompLabel(status: Int): String = when (status) {
    2 -> "Strict"
    1 -> "Filter"
    -1 -> "Disabled"
    else -> "Not supported"
}

private fun seccompShort(status: Int): String = when (status) {
    2 -> "Strict"
    1 -> "Filter"
    -1 -> "Off"
    else -> "N/A"
}

/** .top：46 圆头像（狐头 + lime 在线点）+ 22/800 标题 + 11 sub + 右侧 38x34 胶囊按钮 */
@Composable
fun GlassTopBar(
    title: String,
    subtitle: String,
    showDot: Boolean = false,
    onStyleClick: () -> Unit = {},
    onActionClick: (() -> Unit)? = null,
    actionIconRes: Int = R.drawable.ic_d_user,
    titleSize: Int = 22,
) {
    val t = glassTokens()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 16.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(46.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (t.isDark) Color(0xFF050508) else Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_monochrome),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = t.flame,
                )
            }
            if (showDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(11.dp)
                        .border(2.dp, if (t.isDark) Color(0xFF16161F) else Color.White, CircleShape)
                        .clip(CircleShape)
                        .background(t.lime),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = titleSize.sp, fontWeight = FontWeight.ExtraBold, color = t.ink)
            Text(subtitle, fontSize = 11.sp, color = t.sub, modifier = Modifier.padding(top = 3.dp))
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassTopButton(iconRes = R.drawable.ic_d_palette, onClick = onStyleClick)
            if (onActionClick != null) {
                GlassTopButton(iconRes = actionIconRes, onClick = onActionClick)
            }
        }
    }
}

@Composable
fun GlassTopButton(iconRes: Int, onClick: () -> Unit) {
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
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = t.ink2,
        )
    }
}

/**
 * .hero：26 圆角黑/白玻璃卡 + 120° 霓虹渐变描边（flame→violet→cyan）。
 * 内容：gline 渐变小字 → 23/800 状态标题 → 版本 + tag 组 → hint；右侧 60 圆 seal（violet 对勾）。
 */
@Composable
fun GlassHeroCard(
    state: HomeUiState,
    issueCount: Int,
    onInstall: () -> Unit,
) {
    val t = glassTokens()
    val hasIssue = issueCount > 0
    // 外层：霓虹渐变描边（1dp 内衬）；内层：玻璃面板
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(27.dp))
            .background(t.heroGrad)
            .padding(1.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (t.isDark) Color(0x08FFFFFF) else Color(0x8CFFFFFF)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // .gline 渐变小字
            Text(
                text = stringResource(if (hasIssue) R.string.glass_hero_attention else R.string.glass_hero_working),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                style = TextStyle(brush = t.glineGrad),
            )
            Text(
                text = if (hasIssue) stringResource(R.string.glass_status_issue) else stringResource(R.string.glass_status_ok),
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                color = t.ink,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (hasIssue) {
                Row(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(t.amber.copy(alpha = 0.15f))
                        .border(1.dp, t.amber.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.fresh_home_issue_count, issueCount),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = t.amber,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.fresh_home_install),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = t.ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(t.flame.copy(alpha = 0.9f))
                            .clickable(onClick = onInstall)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }
            // .verrow：版本文字 + tag 组
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.glass_version_fmt, BuildConfig.VERSION_NAME),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = t.ink2,
                )
                GlassTag(
                    text = stringResource(R.string.glass_tag_custom),
                    color = t.amber,
                    bgAlpha = 0.15f,
                )
                GlassTag(
                    text = if (state.lkmMode == true) "LKM" else "GKI",
                    color = t.violet,
                    bgAlpha = 0.12f,
                )
                if (state.lkmMode == true && state.isLateLoadMode) {
                    GlassTag(
                        text = stringResource(R.string.glass_tag_jailbreak),
                        color = t.cyan,
                        bgAlpha = 0.12f,
                    )
                }
            }
            Text(
                text = stringResource(R.string.fresh_home_hero_hint),
                fontSize = 10.5.sp,
                color = if (t.isDark) t.faint else Color(0xFFA39AB2),
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        // .seal：右侧 60 圆 + violet 对勾 + 霓虹光晕
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .size(60.dp)
                .clip(CircleShape)
                .background(if (t.isDark) Color(0xFF0B0B12) else Color(0xBFFFFFFF))
                .border(1.dp, Color(0x14FFFFFF), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_d_check),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = t.violet,
            )
        }
    }
    Spacer(Modifier.height(13.dp))
}

/** .tag：tag.y/w/c 配色 —— 霓虹亮字 + 半透明同色底 + 同色描边 */
@Composable
fun GlassTag(text: String, color: Color, bgAlpha: Float = 0.15f) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = bgAlpha))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** .st：行尾状态徽章 */
@Composable
fun GlassStBadge(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** .ncard：22 圆角玻璃统计卡 + 右上角 74 圆模糊霓虹光晕 */
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
                .offset(x = 10.dp, y = (-10).dp)
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

/** .chip：38 圆角12 玻璃 chip，18 图标 */
@Composable
fun GlassChip(iconRes: Int, accent: Color) {
    val t = glassTokens()
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (t.isDark) Color(0x0FFFFFFF) else Color(0x99FFFFFF))
            .border(1.dp, t.glassBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = accent,
        )
    }
}

/** .wide：20 圆角横条 + 右侧可插 st 徽章/箭头 */
@Composable
fun GlassWide(
    modifier: Modifier = Modifier,
    iconRes: Int,
    title: String,
    subtitle: String,
    accent: Color,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val t = glassTokens()
    Row(
        modifier = modifier
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
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing.invoke(this)
        } else if (showChevron) {
            Spacer(Modifier.width(8.dp))
            Text("›", fontSize = 16.sp, color = t.faint)
        }
    }
}

/** 界面风格切换弹窗（Glass 版） */
@Composable
fun GlassStylePickerDialog(
    current: me.weishu.kernelsu.ui.UiMode,
    onDismiss: () -> Unit,
) {
    val repo = remember { me.weishu.kernelsu.data.repository.SettingsRepositoryImpl() }
    val t = glassTokens()
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (t.isDark) Color(0xFF16161F) else Color.White,
        titleContentColor = t.ink,
        textContentColor = t.ink2,
        title = { Text(stringResource(R.string.settings_ui_mode), fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                me.weishu.kernelsu.ui.UiMode.entries.forEach { mode ->
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
                                painter = painterResource(R.drawable.ic_d_check),
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
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel), color = t.sub)
            }
        },
    )
}
