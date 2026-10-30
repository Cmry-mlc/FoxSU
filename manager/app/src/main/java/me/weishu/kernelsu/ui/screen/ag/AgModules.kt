package me.weishu.kernelsu.ui.screen.ag

import me.weishu.kernelsu.ui.component.bottombar.useSideDock
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Web
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.screen.module.ModuleActions
import me.weishu.kernelsu.ui.screen.module.ModuleUiState

/**
 * 晨曦玻璃 · 模块（设计稿 p-mod）。
 * 标题 + 已启用 n/m 计数联动、搜索过滤、左侧渐变色轨模块卡、开关（品牌渐变 ON）、
 * 执行/打开/卸载真实动作、FAB 导入 zip。
 */
@Composable
fun AgModules(
    uiState: ModuleUiState,
    actions: ModuleActions,
    bottomInnerPadding: androidx.compose.ui.unit.Dp,
) {
    var query by remember { mutableStateOf("") }
    val modules = if (query.isBlank()) uiState.modules else uiState.searchResults.ifEmpty {
        uiState.modules.filter { it.name.contains(query, true) || it.description.contains(query, true) }
    }
    val enabledCount = uiState.modules.count { it.enabled }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode != android.app.Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uris = mutableListOf<android.net.Uri>()
        r.data?.data?.let { uris.add(it) }
        r.data?.clipData?.let { c -> for (i in 0 until c.itemCount) c.getItemAt(i)?.uri?.let { uris.add(it) } }
        if (uris.isNotEmpty()) actions.onOpenFlash(uris)
    }
    val rails = remember {
        listOf(
            Color(0xFF10B981) to Color(0xFF34D399),
            Ag.Vio.copy(alpha = 1.0f) to Color(0xFFEC4899),
            Color(0xFFF59E0B) to Color(0xFFF97316),
            Color(0xFF14B8A6) to Color(0xFF0D9488),
            Color(0xFFEC4899) to Color(0xFFF472B6),
            Color(0xFF6366F1) to Color(0xFF8B5CF6),
        )
    }

    Box(Modifier.fillMaxSize().agPageBg()) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Ag.headSurface()).agHeadBlur()
                .padding(
                    start = if (useSideDock()) 106.dp else 18.dp,
                    end = 18.dp,
                ),
        ) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AgIconBtn(AgTopIcons.Import, onClick = actions.onRefresh)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 模块仓库入口：图标 + 文字胶囊（原来是纯图标按钮，看不出是干嘛的）
                    AgPill(icon = Icons.Rounded.Web, text = "仓库", onClick = actions.onOpenRepo)
                }
            }
            Spacer(Modifier.height(10.dp))
            // 标题 + 计数（与开关实时联动）；设计稿 margin-top:12px
            AgTitle("模", "块", count = "已启用 $enabledCount / ${uiState.modules.size}", topMargin = 12.dp)
            AgSearch(value = query, onValueChange = { query = it }, hint = "搜索模块…")
            Spacer(Modifier.height(14.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (useSideDock()) 106.dp else 18.dp,
                end = 18.dp, top = 4.dp, bottom = bottomInnerPadding + 130.dp),
        ) {
            if (modules.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 90.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("还没有安装模块", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ag.Tx)
                            Spacer(Modifier.height(6.dp))
                            Text("点击右下角按钮刷入 zip 模块包", fontSize = 12.sp, color = Ag.Tx3)
                        }
                    }
                }
            }
            items(modules.size, key = { modules[it].id }) { i ->
                val m = modules[i]
                val (r1, r2) = rails[i % rails.size]
                AgModuleCard(
                    m = m,
                    rail = Brush.verticalGradient(listOf(r1, r2)),
                    onToggle = { actions.onToggleModule(m) },
                    onExecute = { actions.onExecuteModuleAction(m) },
                    onOpenWebUi = { actions.onOpenWebUi(m) },
                    onUninstall = {
                        if (m.remove) actions.onUndoUninstallModule(m) else actions.onRequestUninstallConfirmation(m)
                    },
                )
            }
        }
    }

    // FAB：右下角导入 zip（设计稿 .fab，同容器内不受滚动裁切）
    Box(
        Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 22.dp, bottom = bottomInnerPadding + 64.dp)
    ) {
        AgFab(onClick = { picker.launch(Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/zip" }) })
    }
    }
}

/** 模块卡 .mod：5dp 渐变色轨 + 白芯 + 开关/计数联动 + 操作按钮 */
@Composable
private fun AgModuleCard(
    m: Module,
    rail: Brush,
    onToggle: () -> Unit,
    onExecute: () -> Unit,
    onOpenWebUi: () -> Unit,
    onUninstall: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .agClick(pressScale = 0.985f) {}
            .shadow(
                3.dp,
                RoundedCornerShape(24.dp),
                spotColor = if (Ag.isDark) Color(0x33000000) else Color(0x0D3C2D78),
                ambientColor = if (Ag.isDark) Color(0x1F000000) else Color(0x0A3C2D78),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (m.enabled) {
                    if (Ag.isDark) {
                        Brush.linearGradient(listOf(Color(0x2110B981), Color(0xF21E1D30)))
                    } else {
                        Brush.linearGradient(listOf(Color(0x1210B981), Color(0xE6FFFFFF)))
                    }
                } else {
                    androidx.compose.ui.graphics.SolidColor(Ag.CardSolid)
                }
            )
            .border(1.dp, Ag.Line, RoundedCornerShape(24.dp)),
    ) {
        // 左侧 5dp 渐变色轨（drawBehind 贴满卡高）
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        brush = rail,
                        topLeft = androidx.compose.ui.geometry.Offset.Zero,
                        size = androidx.compose.ui.geometry.Size(5.dp.toPx(), size.height),
                    )
                },
        )
        if (!m.enabled) {
            // 设计稿 .mod.off { opacity:.62 }
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color(0xFF000000).copy(alpha = if (Ag.isDark) 0.25f else 0.06f))
            )
        }
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        m.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Ag.Tx,
                        textDecoration = if (m.remove) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text("版本：${m.version}", fontSize = 12.sp, color = Ag.Tx2)
                    Text("作者：${m.author}", fontSize = 12.sp, color = Ag.Tx2)
                }
                Spacer(Modifier.width(10.dp))
                AgToggle(checked = m.enabled, onCheckedChange = onToggle)
            }
            if (m.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(m.description, fontSize = 12.5.sp, lineHeight = 20.sp, color = Ag.Tx2)
            }
            Spacer(Modifier.height(12.dp))
            // 模块界面「安装/刷入」入口：整条操作带也带一圈粒子科幻边，
            // 和 FAB、主页大卡片保持同一套视觉语言
            Row(
                modifier = Modifier.particleOutline(
                    shape = RoundedCornerShape(13.dp),
                    accent = Ag.Vio,
                    bleed = 7.dp,
                ).padding(vertical = 5.dp, horizontal = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (m.hasActionScript && !m.remove) {
                    AgPill(icon = Icons.Rounded.PlayArrow, text = "执行", onClick = onExecute)
                }
                if (m.hasWebUi && !m.remove) {
                    AgPill(icon = Icons.Rounded.Web, text = "打开", onClick = onOpenWebUi)
                }
                Spacer(Modifier.weight(1f))
                AgPill(
                    icon = if (m.remove) Icons.Rounded.Refresh else Icons.Rounded.Delete,
                    text = if (m.remove) "撤销" else "卸载",
                    warn = true, onClick = onUninstall,
                )
            }
        }
    }
}

/** 操作小按钮 .abtn */
@Composable
private fun AgPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, warn: Boolean = false, onClick: () -> Unit) {
    val color = if (warn) Ag.Danger else Ag.Vio
    val pillSrc = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pillPressed by pillSrc.collectIsPressedAsState()
    val pillScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pillPressed) 0.88f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioHighBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
        ),
        label = "ag-pill",
    )
    Row(
        Modifier
            .height(34.dp)
            .graphicsLayer { scaleX = pillScale; scaleY = pillScale }
            .clickable(
                interactionSource = pillSrc,
                indication = null,
                onClick = onClick,
            )
            .clip(RoundedCornerShape(11.dp))
            .background(if (warn) Color(0x0DDC2626) else Ag.Vio.copy(alpha = 0.059f))
            .border(1.dp, if (warn) Color(0x33DC2626) else Ag.Vio.copy(alpha = 0.22f), RoundedCornerShape(11.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
