package me.weishu.kernelsu.ui.screen.nocturne

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.Module

private val moduleAccents = listOf("violet", "mint", "amber", "red")

/** Nocturne 模块页：头部 + 模块行（图标盒 + 开关）+ 右下 FAB 56dp violet 渐变。 */
@Composable
fun NocturneModuleScreen(
    modules: List<Module>,
    bottomInnerPadding: Dp,
    onToggle: (Module) -> Unit,
    onAddModule: () -> Unit,
    onRefresh: () -> Unit,
    onOpenRepo: () -> Unit,
) {
    val t = ncTokens()
    val enabledCount = modules.count { it.enabled }
    Box(Modifier.fillMaxSize()) {
        NcGlowBackground(t)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = NcDimens.pageX,
                end = NcDimens.pageX,
                top = ncStatusTop() + 8.dp,
                bottom = bottomInnerPadding + 110.dp,
            ),
        ) {
            item {
                NcHeader(
                    kicker = "Modules",
                    title = stringResource(R.string.module),
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            NcRoundButton(iconRes = R.drawable.ic_d_refresh, onClick = onRefresh)
                            NcRoundButton(iconRes = R.drawable.ic_d_book, onClick = onOpenRepo)
                        }
                    },
                )
            }
            item {
                Text(
                    text = "共 ${modules.size} 个 · 已启用 $enabledCount",
                    fontSize = 11.sp,
                    color = t.text2,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            items(modules.size) { index ->
                val m = modules[index]
                val accent = when (moduleAccents[index % moduleAccents.size]) {
                    "mint" -> t.mint
                    "amber" -> t.amber
                    "red" -> t.red
                    else -> t.violet
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(NcRadius.panel))
                        .background(t.surface1)
                        .border(1.dp, t.line, RoundedCornerShape(NcRadius.panel))
                        .padding(horizontal = 15.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NcIconBox(iconRes = R.drawable.ic_d_grid, accent = accent)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = m.name,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = t.text1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (m.description.isNotBlank()) {
                            Text(
                                text = m.description,
                                fontSize = 10.sp,
                                color = t.text2,
                                lineHeight = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        Text(
                            text = "v" + m.version,
                            fontSize = 9.5.sp,
                            color = t.text3,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    NcSwitch(checked = m.enabled, onClick = { onToggle(m) })
                }
            }
            if (modules.isEmpty()) {
                item {
                    Text(
                        text = "尚未安装任何模块",
                        fontSize = 12.sp,
                        color = t.text3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                    )
                }
            }
        }
        // FAB 56dp violet 渐变
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = bottomInnerPadding + 40.dp)
                .size(NcDimens.fab)
                .shadow(10.dp, RoundedCornerShape(NcRadius.pill), spotColor = t.violetDeep.copy(alpha = 0.45f))
                .clip(RoundedCornerShape(NcRadius.pill))
                .background(Brush.linearGradient(listOf(t.violet, t.violetDeep)))
                .clickable(onClick = onAddModule),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_d_plus),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.White,
            )
        }
    }
}
