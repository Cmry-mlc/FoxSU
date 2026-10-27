package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.screen.superuser.GroupedApps

/** Nocturne 超级用户：大标题头部 + 已授权胶囊 + 搜索框 r15 + 应用行分组。 */
@Composable
fun NocturneSuperUserScreen(
    granted: List<GroupedApps>,
    notGranted: List<GroupedApps>,
    grantedCount: Int,
    searchValue: String,
    onSearch: (String) -> Unit,
    bottomInnerPadding: Dp,
    onOpenApp: (GroupedApps) -> Unit,
) {
    val t = ncTokens()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = NcDimens.pageX,
            end = NcDimens.pageX,
            top = ncStatusTop() + 8.dp,
            bottom = bottomInnerPadding + 96.dp,
        ),
    ) {
        item {
            NcHeader(
                kicker = "Superuser",
                title = stringResource(R.string.superuser),
                trailing = {
                    NcBadge(text = "已授权 $grantedCount", color = t.mint)
                },
            )
        }
        item {
            NcSearchField(
                value = searchValue,
                onValueChange = onSearch,
                hint = "搜索应用…",
            )
            Spacer(Modifier.height(12.dp))
        }
        items(granted.size) { index ->
            Box(Modifier.padding(bottom = 9.dp)) {
                NcAppRow(app = granted[index], onClick = { onOpenApp(granted[index]) })
            }
        }
        if (notGranted.isNotEmpty()) {
            item { NcSectionHead("未授权应用") }
            items(notGranted.size) { index ->
                Box(Modifier.padding(bottom = 9.dp)) {
                    NcAppRow(app = notGranted[index], onClick = { onOpenApp(notGranted[index]) })
                }
            }
        }
        if (granted.isEmpty() && notGranted.isEmpty()) {
            item {
                Text(
                    text = "暂无应用",
                    fontSize = 12.sp,
                    color = t.text3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
