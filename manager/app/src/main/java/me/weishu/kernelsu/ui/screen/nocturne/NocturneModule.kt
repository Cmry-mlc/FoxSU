package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.Module

/**
 * 模块 —— 严格对照视觉稿 s3：
 * MODULES / 模块 + 两个圆钮（用户、下载）→ 搜索模块 → 列表 / 空态（104 光球 + 文案）→ 右下 56 FAB。
 */
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
    var query by remember { mutableStateOf("") }
    val list = remember(modules, query) {
        if (query.isBlank()) modules
        else modules.filter {
            it.name.contains(query, true) || it.description.contains(query, true)
        }
    }

    Box(Modifier.fillMaxSize()) {
        NcGlowBackground(t)
        Column(Modifier.fillMaxSize().padding(top = ncStatusTop() + 2.dp)) {
            Box(Modifier.padding(horizontal = 18.dp)) {
                NcHeader(kicker = "MODULES", title = "模块") {
                    NcIconButton(iconRes = R.drawable.nc_user, onClick = {})
                    NcIconButton(iconRes = R.drawable.nc_download, onClick = onOpenRepo)
                }
            }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = bottomInnerPadding + 100.dp),
        ) {
            item {
                NcSearchField(query, { query = it }, "搜索模块", R.drawable.nc_search_glyph)
                Spacer(Modifier.height(12.dp))
            }
            if (list.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            NcEmptyOrb()
                            Spacer(Modifier.height(18.dp))
                            Text("还没有安装模块", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = t.text1)
                            Spacer(Modifier.height(8.dp))
                            Text("点击右下角按钮，刷入 zip 模块包", fontSize = 11.sp, color = t.text3)
                        }
                    }
                }
            } else {
                item { NcSectionLabel("INSTALLED · 已安装 ${list.size}") }
                items(list, key = { it.id }) { m ->
                    ModuleRow(m, onToggle)
                    Spacer(Modifier.height(7.dp))
                }
            }
        }
        }

        // FAB 56 violet 渐变
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = bottomInnerPadding + 40.dp)
                .size(56.dp)
                .shadow(16.dp, CircleShape, ambientColor = t.violet.copy(alpha = 0.55f), spotColor = t.violet.copy(alpha = 0.55f))
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(t.violet, t.violetDeep)))
                .clickable(onClick = onAddModule),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.nc_plus), null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

/** 空态光球：104 圆 radial violet 35%→透明 + 内描边 + 外发光。 */
@Composable
private fun NcEmptyOrb() {
    val t = ncTokens()
    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(t.violet.copy(alpha = 0.35f), t.violet.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(size.width * 0.3f, size.height * 0.25f),
                    radius = size.width * 0.72f,
                ),
                radius = size.width / 2,
            )
        }
        Icon(painterResource(R.drawable.nc_grid), null, tint = t.violet, modifier = Modifier.size(38.dp))
    }
}

/** 模块行：图标盒 + 名称 + 描述 + 版本 + 开关。 */
@Composable
private fun ModuleRow(m: Module, onToggle: (Module) -> Unit) {
    val t = ncTokens()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(t.surface1)
            .padding(horizontal = 15.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NcIconBox(if (m.enabled) R.drawable.nc_grid else R.drawable.nc_chip, if (m.enabled) NcAccent.Violet else NcAccent.Default)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    m.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = t.text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                NcBadge("v${m.version}", t.text3, 0.06f)
            }
            if (m.description.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    m.description,
                    fontSize = 10.5.sp,
                    color = t.text2,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        NcSwitch(checked = m.enabled, onCheckedChange = { onToggle(m) })
    }
}
