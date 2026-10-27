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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.ui.theme.glassTokens

/** 设计稿 .mod 图标的霓虹渐变轮换（lime/blue/pink/flame/cyan） */
private val modAccents = listOf(
    0xFF6EF08AL to 0xFF2ECC71L,
    0xFF7AA2FFL to 0xFF4D5CE5L,
    0xFFFF7AC8L to 0xFFE0409AL,
    0xFFFF7A59L to 0xFFFF4D6DL,
    0xFF5CE1E6L to 0xFF00B8D9L,
)

/**
 * Glass 模块页整页：头像标题行（x 个模块 · y 个已启用）+ .mod 卡列表（版本行徽章 + tog）
 * + 快捷操作 + .addmod 虚线入口。
 */
@Composable
fun GlassModuleList(
    modules: List<Module>,
    bottomInnerPadding: Dp,
    onToggle: (Module) -> Unit,
    onAddModule: () -> Unit,
    onRefresh: () -> Unit,
) {
    val t = glassTokens()
    val enabledCount = modules.count { it.enabled }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 13.dp, end = 13.dp, top = 8.dp, bottom = bottomInnerPadding + 96.dp),
    ) {
        item {
            GlassTopBar(
                title = stringResource(R.string.module),
                subtitle = stringResource(R.string.glass_module_count, modules.size, enabledCount),
                titleSize = 19,
            )
        }
        items(modules.size) { index ->
            val m = modules[index]
            val (a1, a2) = modAccents[Math.floorMod(index, modAccents.size)]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 11.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(t.glassPanel)
                    .border(1.dp, t.glassBorder, RoundedCornerShape(22.dp))
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // .mico：46 圆角15 玻璃块 + 渐变描边图标
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(a1).copy(alpha = if (t.isDark) 0.18f else 0.22f),
                                    Color(a2).copy(alpha = if (t.isDark) 0.08f else 0.10f),
                                )
                            )
                        )
                        .border(1.dp, Color(a1).copy(alpha = 0.35f), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, t.bg.copy(alpha = if (t.isDark) 0.55f else 0.25f))
                                )
                            ),
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_d_check),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color(a1),
                    )
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = m.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = t.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = m.description,
                        fontSize = 10.5.sp,
                        color = t.sub,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                    // .mver：版本号 + 已启用/已停用徽章（同设计稿位置）
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        Text("v" + m.version, fontSize = 9.5.sp, color = t.faint)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(if (m.enabled) R.string.glass_mod_enabled else R.string.glass_mod_disabled),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (m.enabled) t.lime else t.faint,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (m.enabled) t.lime.copy(alpha = 0.12f) else Color(0x0DFFFFFF)
                                )
                                .border(
                                    1.dp,
                                    if (m.enabled) t.lime.copy(alpha = 0.3f) else t.glassBorder,
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                GlassToggle(checked = m.enabled, onClick = { onToggle(m) })
            }
        }
        item { GlassSectionHead(stringResource(R.string.glass_quick_actions)) }
        item {
            GlassWide(
                iconRes = R.drawable.ic_d_refresh,
                title = stringResource(R.string.glass_refresh_modules),
                subtitle = stringResource(R.string.glass_refresh_modules_sub),
                accent = t.violet,
                onClick = onRefresh,
            )
        }
        item { Spacer(Modifier.height(1.dp)) }
        item {
            // .addmod：1.5 虚线圆角22 添加入口
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                if (t.isDark) Color(0xFF2E2E3C) else Color(0xFFC9C6DA),
                                if (t.isDark) Color(0xFF2E2E3C) else Color(0xFFC9C6DA),
                            )
                        ),
                        shape = RoundedCornerShape(22.dp),
                    )
                    .clickable(onClick = onAddModule)
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.module_install),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = t.sub,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.module_repos),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = t.violet,
                    )
                }
            }
        }
    }
}
