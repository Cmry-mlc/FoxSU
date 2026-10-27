package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nocturne 通用组件 —— 严格对照 /tmp/nc_ui/foxsu-nocturne-ui.html 视觉稿 CSS 数值实现。
 */

fun <T> ncSpring() = spring<T>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)

@Composable
fun ncStatusTop(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

val ncMono: FontFamily = FontFamily.Monospace

/** 全屏背景：底色 + 左上 violet 13% (420×260) + 右上 mint 6% (320×220) 双径向光晕。 */
@Composable
fun NcGlowBackground(t: NcTokens) {
    Box(Modifier.fillMaxSize().background(t.bg)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(t.violet.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(size.width * 0.2f, 0f),
                    radius = 420f,
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(t.mint.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.width, size.height * 0.12f),
                    radius = 320f,
                )
            )
        }
    }
}

/** 顶部页头 .hd：眉题 9.5/ls3 + 标题 30/800 + 右侧 36 r13 圆钮。 */
@Composable
fun NcHeader(
    kicker: String,
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val t = ncTokens()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 0.dp).padding(top = 13.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = kicker,
                fontSize = 9.5.sp,
                letterSpacing = 3.sp,
                color = t.text3,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                lineHeight = 32.sp,
                color = t.text1,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            actions()
        }
    }
}

/** 36×36 r13 圆角按钮。 */
@Composable
fun NcIconButton(
    iconRes: Int,
    onClick: () -> Unit,
    tint: Color? = null,
    primary: Boolean = false,
) {
    val t = ncTokens()
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(0.55f, Spring.StiffnessMedium), label = "ib")
    Box(
        Modifier
            .size(36.dp)
            .scale(scale)
            .clip(RoundedCornerShape(13.dp))
            .background(if (primary) Brush.linearGradient(listOf(t.violet, t.violetDeep)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
            .then(
                if (primary) Modifier
                else Modifier
                    .background(t.fill)
                    .border(1.dp, t.line, RoundedCornerShape(13.dp))
            )
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint ?: if (primary) Color.White else t.iconTint,
            modifier = Modifier.size(17.dp),
        )
    }
}

/** 分区标题 .sec —— 9px / ls 2 / tx3 / 800。 */
@Composable
fun NcSectionLabel(text: String, modifier: Modifier = Modifier) {
    val t = ncTokens()
    Text(
        text = text,
        fontSize = 9.sp,
        letterSpacing = 2.sp,
        color = t.text3,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier.padding(start = 2.dp, top = 14.dp, bottom = 6.dp),
    )
}

/** 分组卡 .grp —— r20，底 surface1（暗）/ 白（亮）+ 描边。 */
@Composable
fun NcGroupCard(content: @Composable ColumnScope.() -> Unit) {
    val t = ncTokens()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(t.surface1)
            .border(1.dp, t.line, RoundedCornerShape(20.dp))
            .padding(vertical = 3.dp),
        content = content,
    )
}

/**
 * 分组行 .row —— padding 8.5/15，gap 12；图标 34 r11；标题 13/650；副标题 10.5 tx2。
 * 与上一行之间画 1px 分隔线（left 57 / right 16）。
 */
@Composable
fun NcRow(
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    subtitleMono: Boolean = false,
    accent: NcAccent = NcAccent.Default,
    showDivider: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val t = ncTokens()
    Column(Modifier.fillMaxWidth()) {
        if (showDivider) {
            Canvas(Modifier.fillMaxWidth().padding(start = 57.dp, end = 16.dp).height(1.dp)) {
                drawRect(t.line)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 15.dp, vertical = 8.5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NcIconBox(iconRes, accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.1).sp,
                    color = t.text1,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        fontSize = if (subtitleMono) 9.5.sp else 10.5.sp,
                        color = if (subtitleMono) t.textMono.copy(alpha = 0.85f) else t.text2,
                        fontFamily = if (subtitleMono) ncMono else null,
                        lineHeight = if (subtitleMono) 14.sp else 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(10.dp))
                trailing()
            }
        }
    }
}

/** 34×34 r11 图标盒（四色系）。 */
@Composable
fun NcIconBox(iconRes: Int, accent: NcAccent, size: Dp = 34.dp) {
    val t = ncTokens()
    val (bg, fg) = when (accent) {
        NcAccent.Default -> t.fill to t.iconTint
        NcAccent.Violet -> t.violet.copy(alpha = 0.14f) to t.violetText
        NcAccent.Mint -> t.mint.copy(alpha = 0.12f) to t.mint
        NcAccent.Red -> t.red.copy(alpha = 0.12f) to t.red
        NcAccent.Amber -> t.amber.copy(alpha = 0.12f) to t.amber
    }
    Box(
        Modifier.size(size).clip(RoundedCornerShape(11.dp)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), null, tint = fg, modifier = Modifier.size(17.dp))
    }
}

enum class NcAccent { Default, Violet, Mint, Red, Amber }

/** 右侧 chevron（15，stroke #3F4557）。 */
@Composable
fun NcChevron(tint: Color? = null) {
    val t = ncTokens()
    Icon(
        painter = painterResource(me.weishu.kernelsu.R.drawable.nc_chev),
        contentDescription = null,
        tint = tint ?: t.text3.copy(alpha = 0.7f),
        modifier = Modifier.size(15.dp),
    )
}

/** 小徽章 .badge —— 8.5/800/padding 3-8/r6。 */
@Composable
fun NcBadge(text: String, color: Color, bgAlpha: Float = 0.12f) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = bgAlpha))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, color = color)
    }
}

/** 代码卡 .codecard —— r16 渐变底 + 左侧 2px violet→mint 光条 + 眉题 + mono 值。 */
@Composable
fun NcCodeCard(kicker: String, value: String, tag: String? = null, iconRes: Int? = null) {
    val t = ncTokens()
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF0F1219), Color(0xFF0B0D13)))
            )
            .border(1.dp, t.violet.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
    ) {
        // 左侧 2px 光条
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .width(2.dp)
                .fillMaxHeight()
                .background(Brush.verticalGradient(listOf(t.violet.copy(alpha = 0.9f), t.mint.copy(alpha = 0.35f))))
        )
        Column(Modifier.padding(start = 13.dp, end = 13.dp, top = 10.dp, bottom = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (iconRes != null) {
                    Icon(painterResource(iconRes), null, tint = Color(0xFF9E93FF), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(7.dp))
                }
                Text(
                    kicker,
                    fontSize = 8.5.sp,
                    letterSpacing = 2.2.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF9E93FF),
                )
                if (tag != null) {
                    Spacer(Modifier.weight(1f))
                    Text(tag, fontSize = 8.sp, letterSpacing = 1.2.sp, color = Color(0xFF4E5466))
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = value,
                fontFamily = ncMono,
                fontSize = 9.3.sp,
                lineHeight = 15.sp,
                letterSpacing = (-0.15).sp,
                color = Color(0xFFB9BFCB),
            )
        }
    }
}

/** 开关 .sw —— 42×25，thumb 19，on 渐变 #6A57F5→#8B7BFF。 */
@Composable
fun NcSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val t = ncTokens()
    val offset by animateFloatAsState(if (checked) 20f else 3f, tweenSwitch(), label = "thumb")
    Box(
        Modifier
            .size(42.dp, 25.dp)
            .clip(CircleShape)
            .background(if (checked) Brush.horizontalGradient(listOf(t.violetDeep, t.violet)) else Brush.horizontalGradient(listOf(t.fill, t.fill)))
            .clickable { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(x = offset.dp, y = 3.dp)
                .size(19.dp)
                .clip(CircleShape)
                .background(if (checked) Color.White else Color(0xFF8C93A5))
        )
    }
}

private fun tweenSwitch() = androidx.compose.animation.core.tween<Float>(220, easing = NcEasing)

private val NcEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 1.2f, 0.4f, 1f)

/** 搜索框 .search —— r15 fill 5% + 发丝边框。 */
@Composable
fun NcSearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, iconRes: Int) {
    val t = ncTokens()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(t.fill)
            .border(1.dp, t.line, RoundedCornerShape(15.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(iconRes), null, tint = t.text3, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(9.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, fontSize = 12.sp, color = t.text3)
            }
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = t.text1),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(t.violet),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** stat3 数据条 —— 等分 + 竖线分隔，值 21/800。 */
@Composable
fun NcStat3(items: List<Triple<String, String, Color?>>) {
    val t = ncTokens()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(t.surface1)
            .border(1.dp, t.line, RoundedCornerShape(20.dp))
            .padding(horizontal = 15.dp, vertical = 14.dp)
    ) {
        items.forEachIndexed { i, (k, v, c) ->
            if (i > 0) {
                Box(Modifier.width(1.dp).height(34.dp).background(t.line).align(Alignment.CenterVertically))
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(k, fontSize = 8.5.sp, letterSpacing = 1.6.sp, color = t.text3, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(
                    v,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp,
                    color = c ?: t.text1,
                )
            }
        }
    }
}

/** Hero 卡外层的渐变描边（padding 1dp 技巧）。 */
@Composable
fun NcGradientBorder(
    radius: Dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val t = ncTokens()
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.linearGradient(
                    colors = listOf(t.violet.copy(alpha = 0.75f), t.mint.copy(alpha = 0.35f), Color.White.copy(alpha = 0.06f)),
                )
            )
            .padding(1.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(radius - 1.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF141724), Color(0xFF0E1018)), start = Offset.Zero, end = Offset(0f, 600f))),
            content = content,
        )
    }
}
