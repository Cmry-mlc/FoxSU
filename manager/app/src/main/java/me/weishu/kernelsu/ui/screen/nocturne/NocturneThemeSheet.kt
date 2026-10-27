package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl

/**
 * 主题选择弹层 —— 严格对照视觉稿 theme.png：
 * sheet r30 渐变 + grab → 「主题 / 外观与强调色」→ 模式（3 张 tcard：Nocturne 暗夜 / Daylight 亮色 / 跟随系统）
 * → 强调色（6 色 30 圆，选中外环）→ 选项（跟随日出日落 开关 / 光晕强度）。
 */
@Composable
fun NcThemeSheet(onDismiss: () -> Unit) {
    val t = ncTokens()
    val repo = remember { SettingsRepositoryImpl() }
    var mode by remember { mutableStateOf(0) }          // 0 暗夜 / 1 亮色 / 2 跟随
    var accent by remember { mutableStateOf(0) }
    var sunriseSwitch by remember { mutableStateOf(false) }

    val show = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        show.animateTo(1f, tween(420, easing = CubicBezierEasing(0.2f, 1.05f, 0.35f, 1f)))
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(t.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset(y = (30 * (1f - show.value)).dp)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Brush.verticalGradient(listOf(t.sheetTop, t.sheetBottom)))
                .border(1.dp, t.violet.copy(alpha = 0.30f), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 26.dp)
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp)
                    .clip(CircleShape).background(t.grab)
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("主题", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = t.text1)
                Spacer(Modifier.weight(1f))
                Text("外观与强调色", fontSize = 10.sp, color = t.text3, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))

            SheetSectionLabel("模式")
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ModeCard("Nocturne", "暗夜", 0, mode, Modifier.weight(1f)) { mode = 0 }
                ModeCard("Daylight", "亮色", 1, mode, Modifier.weight(1f)) { mode = 1 }
                ModeCard("跟随", "系统", 2, mode, Modifier.weight(1f)) { mode = 2 }
            }

            SheetSectionLabel("强调色")
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Accents.forEachIndexed { i, (c1, c2) ->
                    Box(
                        Modifier
                            .size(34.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { accent = i },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (accent == i) {
                            Box(
                                Modifier.size(34.dp).clip(CircleShape)
                                    .border(1.6.dp, if (t.dark) Color(0x8CFFFFFF) else Color(0x995D6376), CircleShape)
                            )
                        }
                        Box(
                            Modifier.size(26.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(c1, c2)))
                        )
                    }
                }
            }

            SheetSectionLabel("选项")
            OptionRow(
                iconRes = R.drawable.nc_refresh,
                title = "跟随日出日落",
                sub = "auto switch 18:30 / 06:30",
                trailing = { NcSwitch(sunriseSwitch) { sunriseSwitch = it } },
            )
            OptionRow(
                iconRes = R.drawable.nc_palette,
                title = "光晕强度",
                sub = "glow intensity · 中",
                trailing = {
                    Icon(painterResource(R.drawable.nc_chev), null, tint = t.text3, modifier = Modifier.size(16.dp))
                },
            )
        }
    }
}

private val Accents = listOf(
    Color(0xFF8B7BFF) to Color(0xFF6A57F5),
    Color(0xFF4DE2C5) to Color(0xFF12A98D),
    Color(0xFFFF8A65) to Color(0xFFF0503C),
    Color(0xFFFFC65C) to Color(0xFFE39B1E),
    Color(0xFF6AA8FF) to Color(0xFF2D6BFF),
    Color(0xFFF58BC0) to Color(0xFFD3488F),
)

@Composable
private fun SheetSectionLabel(text: String) {
    val t = ncTokens()
    Text(
        text,
        fontSize = 9.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.ExtraBold,
        color = t.text3,
        modifier = Modifier.padding(start = 2.dp, top = 12.dp, bottom = 6.dp),
    )
}

/** tcard：r16 padding 8，缩略图 54 r11，选中 violet 75% 描边 + 3px 外环。 */
@Composable
private fun ModeCard(
    title: String,
    sub: String,
    index: Int,
    selected: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val t = ncTokens()
    val on = selected == index
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (on) t.violet.copy(alpha = 0.10f) else t.softFill)
            .border(
                width = 1.dp,
                color = if (on) t.violet.copy(alpha = 0.75f) else t.line,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 缩略图
        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    when (index) {
                        0 -> Brush.linearGradient(listOf(Color(0xFF0B0D13), Color(0xFF0B0D13)))
                        1 -> Brush.linearGradient(listOf(Color(0xFFF5F6FA), Color(0xFFF5F6FA)))
                        else -> Brush.verticalGradient(listOf(Color(0xFF161A28), Color(0xFFF5F6FA)), 0f, 220f)
                    }
                )
                .padding(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize()) {
                MiniBar(width = 0.7f, color = t.violet.copy(alpha = 0.75f))
                MiniBar(width = 0.9f, color = if (index == 1) Color(0x1F0F142D) else t.grab)
                MiniBar(width = 0.55f, color = if (index == 1) Color(0x120F142D) else Color(0x1AFFFFFF))
                if (index != 2) MiniBar(width = 0.4f, color = t.mint.copy(alpha = 0.55f))
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (on) t.violetText else t.text1, textAlign = TextAlign.Center)
        Text(sub, fontSize = 8.sp, color = t.text3, letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MiniBar(width: Float, color: Color) {
    Box(Modifier.fillMaxWidth(width).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
}

/** ritem：36 r12 图标 + 标题/副标题 + 右侧控件；主操作态 violet 渐变内描边。 */
@Composable
private fun OptionRow(
    iconRes: Int,
    title: String,
    sub: String,
    trailing: @Composable () -> Unit,
) {
    val t = ncTokens()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(horizontal = 2.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(t.softFill2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(iconRes), null, tint = t.iconTint, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = t.text1)
            Spacer(Modifier.height(2.dp))
            Text(sub, fontSize = 9.5.sp, fontFamily = ncMono, color = t.text3)
        }
        trailing()
    }
}
