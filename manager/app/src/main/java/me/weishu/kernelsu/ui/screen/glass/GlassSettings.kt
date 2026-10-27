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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.glassTokens

/** Glass 设置页一组：.shead 分组标题 + .srow 行。 */
data class GlassSettingItem(
    val iconRes: Int,
    val title: String,
    val subtitle: String,
    val accentColor: Long,
    val onClick: () -> Unit,
)

@Composable
fun GlassSettingsGroup(title: String, items: List<GlassSettingItem>) {
    val t = glassTokens()
    Column {
        // .shead：12/800 sub letterSpacing2 + 渐隐分割线
        Row(
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
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
                            listOf(if (t.isDark) Color(0x1FFFFFFF) else Color(0x2E5A5578), Color.Transparent)
                        )
                    ),
            )
        }
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(t.glassPanel)
                    .border(1.dp, t.glassBorder, RoundedCornerShape(20.dp))
                    .clickable(onClick = item.onClick)
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassChip(iconRes = item.iconRes, accent = Color(item.accentColor))
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = t.ink,
                    )
                    Text(
                        text = item.subtitle,
                        fontSize = 10.sp,
                        color = t.faint,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_d_back),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer { rotationZ = 180f },
                    tint = t.faint,
                )
            }
        }
    }
}

