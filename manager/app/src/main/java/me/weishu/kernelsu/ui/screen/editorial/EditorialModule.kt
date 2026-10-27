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
import me.weishu.kernelsu.data.model.Module

private val modPairs = listOf(
    0xFF8EF58AL to 0xFF2ECC71L,
    0xFF7AA2FFL to 0xFF4D5CE5L,
    0xFFFF7AC8L to 0xFFE0409AL,
    0xFFFF7A59L to 0xFFFF4D6DL,
    0xFF5CE1E6L to 0xFF00B8D9L,
)

/**
 * FoxSu Editorial 模块页：masthead + 账目式模块行 + 获取更多（仓库 / 导入 zip）。
 */
@Composable
fun EditorialModule(
    modules: List<Module>,
    bottomInnerPadding: Dp,
    onToggle: (Module) -> Unit,
    onAddModule: () -> Unit,
    onRefresh: () -> Unit,
    onOpenRepo: () -> Unit = {},
    onCleanModules: () -> Unit = onRefresh,
) {
    val t = edTokens()
    val enabled = modules.count { it.enabled }
    val repoTitle = stringResource(R.string.module_repos)
    val repoSub = stringResource(R.string.glass_repo_sub)
    val addTitle = stringResource(R.string.glass_add_module_hint)
    val addSub = stringResource(R.string.glass_add_module_sub)
    val refreshTitle = stringResource(R.string.glass_refresh_modules)
    val refreshSub = stringResource(R.string.glass_refresh_modules_sub)
    val cleanTitle = stringResource(R.string.glass_clean_modules)
    val cleanSub = stringResource(R.string.glass_clean_modules_sub)
    val chevron: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        Text("›", fontSize = 13.sp, color = t.faint)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(t.paper)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = bottomInnerPadding + 24.dp),
    ) {
        item {
            EdMasthead(
                brand = stringResource(R.string.module),
                tag = "MODULES",
                left = "${modules.size} 个模块 · $enabled 个已启用",
                right = "LIST",
                brandSize = 28,
            )
        }

        if (modules.isNotEmpty()) {
            item { EdSection("01", "已安装", topPadding = 14.dp) }
            items(modules.size) { index ->
                val m = modules[index]
                val (c1, c2) = modPairs[Math.floorMod(index, modPairs.size)]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .hairBottom(t.hair)
                        .padding(horizontal = 22.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(Color(c1), Color(c2)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (m.enabled) "✓" else "▤",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
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
                        if (m.description.isNotBlank()) {
                            Text(
                                text = m.description,
                                fontSize = 10.sp,
                                color = t.sub,
                                lineHeight = 15.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        Text(
                            text = "v${m.version}",
                            fontSize = 9.sp,
                            color = t.faint,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (m.enabled) "ENABLED" else "DISABLED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = if (m.enabled) t.lime else t.faint,
                        )
                        Spacer(Modifier.height(8.dp))
                        EdSwitch(checked = m.enabled, onToggle = { onToggle(m) })
                    }
                }
            }
        }

        item { EdSection("02", "获取更多") }
        EdRow(
            glyph = "⌕", glyphBg = t.violet.copy(alpha = 0.10f), glyphColor = t.violet,
            title = repoTitle,
            subtitle = repoSub,
            trailing = chevron,
            onClick = onOpenRepo,
        )
        EdRow(
            glyph = "＋", glyphBg = t.cyan.copy(alpha = 0.10f), glyphColor = t.cyan,
            title = addTitle,
            subtitle = addSub,
            trailing = chevron,
            onClick = onAddModule,
        )
        item { EdSection("03", "维护") }
        EdRow(
            glyph = "↻", glyphBg = t.violet.copy(alpha = 0.10f), glyphColor = t.violet,
            title = refreshTitle,
            subtitle = refreshSub,
            trailing = chevron,
            onClick = onRefresh,
        )
        EdRow(
            glyph = "⌫", glyphBg = t.acc.copy(alpha = 0.10f), glyphColor = t.acc,
            title = cleanTitle,
            subtitle = cleanSub,
            trailing = chevron,
            onClick = onCleanModules,
        )
    }
}

/** EdRow 的 LazyListScope 版本（避免在 item 外调用） */
private fun androidx.compose.foundation.lazy.LazyListScope.EdRow(
    glyph: String,
    glyphBg: Color,
    glyphColor: Color,
    title: String,
    subtitle: String,
    trailing: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    item {
        me.weishu.kernelsu.ui.screen.editorial.EdRow(
            glyph = glyph,
            glyphBg = glyphBg,
            glyphColor = glyphColor,
            title = title,
            subtitle = subtitle,
            trailing = trailing,
            onClick = onClick,
        )
    }
}
