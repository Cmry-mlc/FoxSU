package me.weishu.kernelsu.ui.screen.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.screen.superuser.GroupedApps
import me.weishu.kernelsu.ui.theme.glassTokens

/** 设计稿 .uact 中每个应用轮换的霓虹渐变（flame/lemon/violet/cyan/blue） */
private val neonPairs = listOf(
    0xFFFF7A59L to 0xFFFF4D6DL,
    0xFFFFE45CL to 0xFFFFC300L,
    0xFFC77DFFL to 0xFF8A2BE2L,
    0xFF5CE1E6L to 0xFF00B8D9L,
    0xFF7AA2FFL to 0xFF4D5CE5L,
)

private fun neonFor(uid: Int): Pair<Color, Color> {
    val (a, b) = neonPairs[Math.floorMod(uid, neonPairs.size)]
    return Color(a) to Color(b)
}

/** .urow：20 圆角玻璃应用行 —— 40 圆角13 图标 + 包名副标题 + 渐变 ROOT 徽章 + .tog 开关 */
@Composable
fun GlassSuperUserRow(
    app: GroupedApps,
    onClick: () -> Unit,
) {
    val t = glassTokens()
    val (c1, c2) = neonFor(app.uid)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(t.glassPanel)
            .border(1.dp, t.glassBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        me.weishu.kernelsu.ui.component.AppIconImage(
            packageInfo = app.primary.packageInfo,
            label = app.primary.label,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp)),
        )
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.primary.label,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = t.ink,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text = app.primary.displayIdentifier,
                fontSize = 9.5.sp,
                color = t.faint,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        // .uact：渐变 ROOT 徽章 + .tog 开关
        if (app.anyAllowSu) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(Brush.horizontalGradient(listOf(c1, c2)))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "ROOT",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = if (t.isDark) Color(0xFF0D0D14) else Color(0xFF3A3A46),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        GlassToggle(checked = app.anyAllowSu, gradientOn = app.anyAllowSu, on1 = c1, on2 = c2)
    }
}

/** .tog：42x24 开关，on 时霓虹渐变底 + 白圆点 */
@Composable
fun GlassToggle(
    checked: Boolean,
    gradientOn: Boolean = true,
    on1: Color = Color(0xFFFF7A59),
    on2: Color = Color(0xFFC77DFF),
    onClick: (() -> Unit)? = null,
) {
    val t = glassTokens()
    Box(
        modifier = Modifier
            .size(width = 42.dp, height = 24.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(
                when {
                    checked && gradientOn -> Brush.horizontalGradient(listOf(on1, on2))
                    checked -> Brush.horizontalGradient(listOf(t.flame, t.violet))
                    else -> Brush.horizontalGradient(listOf(t.togOff, t.togOff))
                }
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(3.dp)
                .offset(x = if (checked) 18.dp else 0.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(if (checked) Color.White else t.togThumbOff),
        )
    }
}

/** .shead：分组标题 + 渐隐分割线（超级用户/模块/设置共用） */
@Composable
fun GlassSectionHead(title: String) {
    val t = glassTokens()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            color = t.sub,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            if (t.isDark) Color(0x1FFFFFFF) else Color(0x2E5A5578),
                            Color.Transparent
                        )
                    )
                ),
        )
    }
}

/** 超级用户页整页（Glass）：头像标题行 + 搜索条 + 已授权/未授权分组列表 */
@Composable
fun GlassSuperUserList(
    granted: List<GroupedApps>,
    notGranted: List<GroupedApps>,
    grantedCount: Int,
    searchValue: String,
    onSearch: (String) -> Unit,
    bottomInnerPadding: Dp,
    onOpenApp: (GroupedApps) -> Unit,
) {
    val t = glassTokens()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 13.dp, end = 13.dp, top = 8.dp, bottom = bottomInnerPadding + 96.dp),
    ) {
        item {
            GlassTopBar(
                title = stringResource(R.string.superuser),
                subtitle = stringResource(R.string.glass_superuser_sub),
                titleSize = 19,
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.glass_granted_count, grantedCount),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = t.lime,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(t.lime.copy(alpha = 0.1f))
                        .border(1.dp, t.lime.copy(alpha = 0.25f), RoundedCornerShape(99.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (t.isDark) Color(0x0AFFFFFF) else Color(0x80FFFFFF))
                    .border(1.dp, t.glassBorder, RoundedCornerShape(99.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_d_search),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = t.faint,
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = searchValue,
                    onValueChange = onSearch,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 11.5.sp, color = t.ink),
                    cursorBrush = Brush.verticalGradient(listOf(t.violet, t.violet)),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (searchValue.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.glass_search_apps),
                                    fontSize = 11.5.sp,
                                    color = t.faint,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        }
        if (granted.isNotEmpty()) {
            items(granted.size) { index ->
                GlassSuperUserRow(app = granted[index], onClick = { onOpenApp(granted[index]) })
            }
        }
        if (notGranted.isNotEmpty()) {
            item { GlassSectionHead(stringResource(R.string.glass_group_not_granted)) }
            items(notGranted.size) { index ->
                GlassSuperUserRow(app = notGranted[index], onClick = { onOpenApp(notGranted[index]) })
            }
        }
    }
}
