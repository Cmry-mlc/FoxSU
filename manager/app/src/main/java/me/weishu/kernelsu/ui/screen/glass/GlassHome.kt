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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

/** FoxSu Glass 主页：环境光晕 + 白色玻璃面板 + 霓虹描边英雄卡 + 2×2 统计卡。 */
@Composable
fun GlassHome(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
    navigationBadge: NavigationBadgeState,
    onOpenTab: (Int) -> Unit,
) {
    val issues = state.freshIssues()
    var showStylePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 13.dp, end = 13.dp, top = 4.dp, bottom = bottomInnerPadding + 88.dp),
    ) {
        item {
            GlassTopBar(
                title = "FoxSu",
                subtitle = stringResource(R.string.glass_home_subtitle),
                showDot = state.isRootAvailable,
                onStyleClick = { showStylePicker = true },
                onActionClick = actions.onInstallClick,
                actionIconRes = R.drawable.ic_d_power2,
            )
        }
        item {
            GlassHeroCard(state = state, issueCount = issues.size, onInstall = actions.onInstallClick)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                GlassNCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_phone,
                    title = stringResource(R.string.home_device_model),
                    value = state.systemInfo.deviceModel,
                    caption = stringResource(R.string.fresh_home_device_sub),
                    accent = glassTokens().mint,
                )
                GlassNCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_palette,
                    title = stringResource(R.string.settings_ui_mode),
                    value = stringResource(uiModeLabelRes(LocalUiMode.current))
                        .removePrefix("FoxSu "),
                    caption = stringResource(R.string.fresh_home_style_switch),
                    accent = glassTokens().cyan,
                    onClick = { showStylePicker = true },
                )
            }
        }
        item { Spacer(Modifier.height(11.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                GlassNCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_shield,
                    title = stringResource(R.string.superuser),
                    value = navigationBadge.superuserCount.toString(),
                    caption = stringResource(R.string.fresh_home_superuser_sub),
                    accent = glassTokens().violet,
                    onClick = { onOpenTab(1) },
                )
                GlassNCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_grid,
                    title = stringResource(R.string.module),
                    value = navigationBadge.moduleEnabledCount.toString(),
                    caption = stringResource(R.string.fresh_home_module_sub),
                    accent = glassTokens().pink,
                    onClick = { onOpenTab(2) },
                )
            }
        }
        item { Spacer(Modifier.height(11.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                GlassStatusCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_lock,
                    title = stringResource(R.string.glass_home_selinux),
                    subtitle = if (state.isSELinuxPermissive) "Permissive" else "Enforcing",
                    accent = glassTokens().blue,
                    badge = if (state.isSELinuxPermissive) "Permissive" else "Enforcing",
                    badgeColor = if (state.isSELinuxPermissive) glassTokens().amber else glassTokens().lime,
                )
                GlassStatusCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_sliders,
                    title = stringResource(R.string.home_seccomp_status),
                    subtitle = seccompLabel(state.systemInfo.seccompStatus),
                    accent = glassTokens().amber,
                    badge = seccompShort(state.systemInfo.seccompStatus),
                    badgeColor = glassTokens().amber,
                )
            }
        }
        item { Spacer(Modifier.height(11.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                GlassLinkCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_heart,
                    title = stringResource(R.string.glass_home_support),
                    subtitle = stringResource(R.string.glass_home_support_sub),
                    accent = glassTokens().flame,
                    onClick = { actions.onOpenUrl("https://github.com/Cmry-mlc/FoxSu") },
                )
                GlassLinkCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_d_book,
                    title = stringResource(R.string.glass_home_learn),
                    subtitle = stringResource(R.string.glass_home_learn_sub),
                    accent = glassTokens().lemon,
                    onClick = { actions.onOpenUrl("https://kernelsu.org") },
                )
            }
        }
        item { Spacer(Modifier.height(11.dp)) }
        item {
            GlassKernelRow(
                kernelVersion = state.systemInfo.kernelVersion,
                fingerprint = state.systemInfo.fingerprint,
            )
        }
    }

    if (showStylePicker) {
        GlassStylePickerDialog(current = LocalUiMode.current, onDismiss = { showStylePicker = false })
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

/**
 * .top：46 圆白底头像（狐头 + 右下 lime 点）+ 22/800 标题 + 11 sub 副标题 + 右侧 38×34 白圆按钮
 */
@Composable
fun GlassTopBar(
    title: String,
    subtitle: String,
    showDot: Boolean = false,
    onStyleClick: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null,
    actionIconRes: Int = R.drawable.ic_d_user,
    titleSize: Int = 22,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val t = glassTokens()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 14.dp, start = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(46.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
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
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(t.lime),
                )
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = titleSize.sp,
                fontWeight = FontWeight.ExtraBold,
                color = t.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                color = t.sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        if (trailing != null) {
            trailing.invoke(this)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onStyleClick != null) {
                    GlassTopButton(iconRes = R.drawable.ic_d_palette, onClick = onStyleClick)
                }
                if (onActionClick != null) {
                    GlassTopButton(iconRes = actionIconRes, onClick = onActionClick)
                }
            }
        }
    }
}

/** 38×34 白色圆胶囊按钮（.tbtn） */
@Composable
fun GlassTopButton(iconRes: Int, onClick: () -> Unit) {
    val t = glassTokens()
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 34.dp)
            .shadow(4.dp, RoundedCornerShape(99.dp))
            .clip(RoundedCornerShape(99.dp))
            .background(if (t.isDark) Color(0x14FFFFFF) else Color.White)
            .border(1.dp, if (t.isDark) Color(0x1AFFFFFF) else Color(0xD9FFFFFF), RoundedCornerShape(99.dp))
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
 * .hero：26 圆角白玻璃 + 120° 霓虹渐变描边。
 * 内容顺序：gline 渐变小字 → 23/800 状态 → 「版本：xxx」+ tag 组同一行 → hint；
 * 右侧 60 圆白 seal（violet 对勾）垂直居中并压住 tag 行。
 */
@Composable
fun GlassHeroCard(state: HomeUiState, issueCount: Int, onInstall: () -> Unit) {
    val t = glassTokens()
    val hasIssue = issueCount > 0
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(27.dp))
            .background(t.heroGrad)
            .padding(1.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (t.isDark) Color(0x0AFFFFFF) else Color(0xEBFFFFFF)),
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            Column(modifier = Modifier.padding(end = 58.dp)) {
                Text(
                    text = stringResource(
                        if (hasIssue) R.string.glass_hero_attention else R.string.glass_hero_working
                    ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    style = TextStyle(brush = t.glineGrad),
                )
                Text(
                    text = stringResource(
                        if (hasIssue) R.string.glass_status_issue else R.string.glass_status_ok
                    ),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = t.ink,
                    modifier = Modifier.padding(top = 7.dp),
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
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(t.flame)
                                .clickable(onClick = onInstall)
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
                // .verrow：版本文字 + tag 组（横向排列，可换行）
                Row(
                    modifier = Modifier.padding(top = 12.dp),
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
                    GlassTag(
                        text = stringResource(R.string.glass_tag_jailbreak),
                        color = t.cyan,
                        bgAlpha = 0.12f,
                    )
                }
                Text(
                    text = stringResource(R.string.fresh_home_hero_hint),
                    fontSize = 10.5.sp,
                    color = if (t.isDark) t.faint else Color(0xFFA39AB2),
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            // .seal：60 圆白底 + violet 对勾，垂直居中于整个英雄卡
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(60.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(if (t.isDark) Color(0xFF0B0B12) else Color.White),
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
    }
    Spacer(Modifier.height(13.dp))
}

/** .tag：霓虹亮字 + 同色半透明底 + 同色描边 */
@Composable
fun GlassTag(text: String, color: Color, bgAlpha: Float = 0.15f) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        maxLines = 1,
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
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * .ncard：22 圆角玻璃统计卡。右上角一枚大半径淡色霓虹圆（74px，右下 1/4 内扣），
 * 内容自上而下：38 chip → 16 间距 → cT 11.5 sub → cV 17/800 彩色值 → 15 间距 → cS 10 faint
 */
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
            .background(if (t.isDark) t.panel2 else Color(0xD9FFFFFF))
            .border(1.dp, if (t.isDark) t.glassBorder else Color(0xCCFFFFFF), RoundedCornerShape(22.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
    ) {
        // ::after 右上角色晕 —— 74 圆，右/上各溢出 22px，被父级裁切
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 22.dp, y = (-22).dp)
                .size(74.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (t.isDark) 0.16f else 0.13f)),
        )
        Column(modifier = Modifier.padding(15.dp)) {
            GlassChip(iconRes = iconRes, accent = accent, shadowed = true)
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = t.sub)
            Text(
                value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                lineHeight = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            Spacer(Modifier.height(15.dp))
            Text(caption, fontSize = 10.sp, color = t.faint, maxLines = 1)
        }
    }
}

/** .chip：38 圆角12 玻璃 chip（亮色下白底带投影） */
@Composable
fun GlassChip(iconRes: Int, accent: Color, shadowed: Boolean = false) {
    val t = glassTokens()
    Box(
        modifier = Modifier
            .size(38.dp)
            .then(if (shadowed && !t.isDark) Modifier.shadow(3.dp, RoundedCornerShape(12.dp)) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(if (t.isDark) Color(0x0FFFFFFF) else Color.White)
            .border(1.dp, if (t.isDark) t.glassBorder else Color(0xE6FFFFFF), RoundedCornerShape(12.dp)),
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

/**
 * .wide（两列版）：20 圆角玻璃，chip + 标题（13/800）+ 副标题（10.5 sub）+ 右侧 st 徽章。
 * 用于 SELinux / Seccomp 这类状态条。
 */
@Composable
fun GlassStatusCard(
    modifier: Modifier = Modifier,
    iconRes: Int,
    title: String,
    subtitle: String,
    accent: Color,
    badge: String,
    badgeColor: Color,
) {
    val t = glassTokens()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (t.isDark) t.panel2 else Color(0xD9FFFFFF))
            .border(1.dp, if (t.isDark) t.glassBorder else Color(0xCCFFFFFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassChip(iconRes = iconRes, accent = accent, shadowed = true)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = t.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                fontSize = 10.sp,
                color = t.sub,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        GlassStBadge(text = badge, color = badgeColor)
    }
}

/** .wide（两列版，带箭头）：chip + 标题/副标题 + › */
@Composable
fun GlassLinkCard(
    modifier: Modifier = Modifier,
    iconRes: Int,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    val t = glassTokens()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (t.isDark) t.panel2 else Color(0xD9FFFFFF))
            .border(1.dp, if (t.isDark) t.glassBorder else Color(0xCCFFFFFF), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassChip(iconRes = iconRes, accent = accent, shadowed = true)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = t.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                fontSize = 10.sp,
                color = t.sub,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text("›", fontSize = 17.sp, color = t.faint)
    }
}

/** 单列整行 .wide：内核版本 */
@Composable
fun GlassKernelRow(kernelVersion: String, fingerprint: String) {
    val t = glassTokens()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (t.isDark) t.panel2 else Color(0xD9FFFFFF))
            .border(1.dp, if (t.isDark) t.glassBorder else Color(0xCCFFFFFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassChip(iconRes = R.drawable.ic_d_chip, accent = t.lime, shadowed = true)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.home_kernel), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = t.ink)
            Text(
                text = if (kernelVersion.isNotBlank()) kernelVersion else fingerprint,
                fontSize = 10.sp,
                color = t.sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** 通用单列 .wide（模块页快捷操作用） */
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
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (t.isDark) t.panel2 else Color(0xD9FFFFFF))
            .border(1.dp, if (t.isDark) t.glassBorder else Color(0xCCFFFFFF), RoundedCornerShape(20.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassChip(iconRes = iconRes, accent = accent, shadowed = true)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = t.ink, maxLines = 1)
            Text(
                subtitle,
                fontSize = 10.sp,
                color = t.sub,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing.invoke(this)
        } else if (showChevron) {
            Text("›", fontSize = 17.sp, color = t.faint)
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
                            .background(
                                if (mode == current) t.violet.copy(alpha = 0.1f) else Color.Transparent
                            )
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
