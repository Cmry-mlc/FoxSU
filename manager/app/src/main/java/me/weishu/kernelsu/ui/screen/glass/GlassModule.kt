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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.ui.theme.glassTokens

/** Glass 模块页：.mod 卡列表 + .addmod 添加入口。 */
@Composable
fun GlassModuleList(
    modules: List<Module>,
    bottomInnerPadding: Dp,
    onModuleClick: (Module) -> Unit,
    onToggle: (Module) -> Unit,
    onAddModule: () -> Unit,
) {
    val t = glassTokens()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 13.dp, end = 13.dp, top = 10.dp, bottom = bottomInnerPadding + 90.dp),
    ) {
        items(modules.size) { index ->
            val m = modules[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 11.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(t.glassPanel)
                    .border(1.dp, t.glassBorder, RoundedCornerShape(22.dp))
                    .clickable { onModuleClick(m) }
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (t.isDark) Color(0x0DFFFFFF) else Color(0x8CFFFFFF))
                        .border(1.dp, t.glassBorder, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_d_tool),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (m.enabled) t.lime else t.faint,
                    )
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = m.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = t.ink,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (m.enabled) "ENABLED" else "DISABLED",
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
                    Text(
                        text = m.description,
                        fontSize = 10.5.sp,
                        color = t.sub,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                    Text(
                        text = "v" + m.version,
                        fontSize = 9.5.sp,
                        color = t.faint,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                // .tog 开关：42x24，on 状态 flame→violet 渐变
                Box(
                    modifier = Modifier
                        .size(width = 42.dp, height = 24.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (m.enabled) t.switchGrad else androidx.compose.ui.graphics.SolidColor(t.togOff))
                        .clickable { onToggle(m) },
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(3.dp)
                            .offset(x = if (m.enabled) 18.dp else 0.dp)
                            .size(18.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (m.enabled) Color.White else t.togThumbOff),
                    )
                }
            }
        }
        item {
            // .addmod 虚线添加入口
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        1.5.dp,
                        if (t.isDark) Color(0xFF2E2E3C) else Color(0xFFC9C6DA),
                        RoundedCornerShape(22.dp),
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
                        color = t.faint,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "+",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = t.violet,
                    )
                }
            }
        }
    }
}
