package me.weishu.kernelsu.ui.screen.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.screen.superuser.GroupedApps
import me.weishu.kernelsu.ui.theme.glassTokens

/** Glass 超级用户页头部：26/800 标题 + lime 计数胶囊 + 搜索条。 */
@Composable
fun GlassSuperUserHeader(grantedCount: Int, searchValue: String, onSearch: (String) -> Unit) {
    val t = glassTokens()
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.superuser),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = t.ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(me.weishu.kernelsu.R.string.glass_granted_count, grantedCount),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
            androidx.compose.foundation.text.BasicTextField(
                value = searchValue,
                onValueChange = onSearch,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 11.5.sp,
                    color = t.ink,
                ),
                cursorBrush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(t.violet, t.violet)),
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
}

/** .urow：20 圆角玻璃应用行，40dp 圆角13 图标 + ROOT 标签 + 开关在 trailing */
@Composable
fun GlassSuperUserRow(
    app: GroupedApps,
    onClick: () -> Unit,
) {
    val t = glassTokens()
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
                text = if (app.apps.size > 1) ownerName(app.uid) else app.primary.label,
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
        if (app.anyAllowSu) {
            Text(
                text = "ROOT",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = t.lime,
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(t.lime.copy(alpha = 0.12f))
                    .border(1.dp, t.lime.copy(alpha = 0.3f), RoundedCornerShape(7.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
    }
}

private fun ownerName(uid: Int): String = "UID $uid"
