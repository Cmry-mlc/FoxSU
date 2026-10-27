package me.weishu.kernelsu.ui.screen.editorial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.screen.superuser.GroupedApps
import me.weishu.kernelsu.ui.theme.glassTokens

/** 目录式渐变方块（.idx .app） */
private fun iconText(label: String): String {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) return "?"
    val first = trimmed.first()
    return if (first.code < 128) trimmed.take(2).uppercase() else trimmed.take(1)
}

private val idxPairs = listOf(
    0xFFFF7A59L to 0xFFFF4D6DL,
    0xFFFFE45CL to 0xFFFFC300L,
    0xFFC77DFFL to 0xFF8A2BE2L,
    0xFF5CE1E6L to 0xFF00B8D9L,
    0xFF7AA2FFL to 0xFF4D5CE5L,
    0xFF8E8B9CL to 0xFF55525FL,
)

@Composable
private fun EdSuperUserRow(
    index: Int,
    app: GroupedApps,
    onClick: () -> Unit,
) {
    val t = edTokens()
    val (c1, c2) = idxPairs[Math.floorMod(app.uid, idxPairs.size)]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hairBottom(t.hair)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "%02d".format(index + 1),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = t.faint,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(26.dp),
        )
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(Color(c1), Color(c2)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = iconText(app.primary.label),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.primary.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = t.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.primary.displayIdentifier,
                fontSize = 9.5.sp,
                color = t.sub,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        if (app.anyAllowSu) {
            Text(
                text = "● ROOT",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = t.lime,
                modifier = Modifier.padding(end = 10.dp),
            )
        } else {
            Text(
                text = "○ NONE",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = t.faint,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        EdSwitch(checked = app.anyAllowSu)
    }
}

/**
 * FoxSu Editorial 超级用户页：masthead + 统计带 + 已授权/未授权目录列表。
 */
@Composable
fun EditorialSuperUser(
    granted: List<GroupedApps>,
    notGranted: List<GroupedApps>,
    grantedCount: Int,
    searchValue: String,
    onSearch: (String) -> Unit,
    bottomInnerPadding: Dp,
    onOpenApp: (GroupedApps) -> Unit,
) {
    val t = edTokens()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(t.paper)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = bottomInnerPadding + 30.dp),
    ) {
        item {
            EdMasthead(
                brand = stringResource(R.string.superuser),
                tag = "INDEX",
                left = "已授权 $grantedCount · 待定 ${notGranted.size}",
                right = "按时间排序",
                brandSize = 28,
            )
        }
        item {
            EdStatBand(
                cells = listOf(
                    Triple(grantedCount.toString(), "已授权", t.lime),
                    Triple(notGranted.size.toString(), "未授权", null),
                    Triple("0", "已拒绝", t.acc),
                    Triple("24h", "日志记录", null),
                ),
            )
        }
        item { EdSearchRow(searchValue, stringResource(R.string.glass_search_apps), onSearch) }

        if (granted.isNotEmpty()) {
            item { EdSection("01", "已授权") }
            items(granted.size) { index ->
                EdSuperUserRow(index = index, app = granted[index], onClick = { onOpenApp(granted[index]) })
            }
        }
        if (notGranted.isNotEmpty()) {
            item { EdSection("02", "未授权") }
            items(notGranted.size) { index ->
                EdSuperUserRow(index = index, app = notGranted[index], onClick = { onOpenApp(notGranted[index]) })
            }
        }
    }
}
