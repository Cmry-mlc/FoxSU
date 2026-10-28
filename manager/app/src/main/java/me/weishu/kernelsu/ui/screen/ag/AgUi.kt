package me.weishu.kernelsu.ui.screen.ag

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateContentSize

/**
 * 晨曦玻璃（Aurora Glass）设计令牌 — 严格对应交付文档 tokens/设计稿：
 * 135° 品牌渐变 #6C5CE7 → #A855F7(55%) → #EC4899；暖白雾彩底 + 毛玻璃卡。
 */
object Ag {
    val Bg1 = Color(0xFFF7F6FB)
    val Bg2 = Color(0xFFF2F4FA)
    val Card = Color(0xB8FFFFFF)          // rgba(255,255,255,.72)
    val CardSolid = Color(0xFFFFFFFF)
    val Line = Color(0x121E1946)          // rgba(30,25,70,.07)
    val Tx = Color(0xFF191826)
    val Tx2 = Color(0xFF66647A)
    val Tx3 = Color(0xFF9C9AAE)
    val Vio = Color(0xFF6C5CE7)
    val Vio2 = Color(0xFF8E5CF6)
    val Mag = Color(0xFFEC4899)
    val Teal = Color(0xFF14B8A6)
    val Amber = Color(0xFFF59E0B)
    val Danger = Color(0xFFDC2626)
    val Success = Color(0xFF10B981)
    val Chev = Color(0xFFC6C4D6)

    val Grad: Brush
        get() = Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFFA855F7), Color(0xFFEC4899)))

    val Shadow = Color(0x143C2D78)        // rgba(60,45,120,.08)
    val TitleGrad = Grad

    /** 页面背景：160° #F7F6FB → #F2F4FA + 三向径向雾彩（紫→品红→青） */
    fun background(): Brush = Brush.linearGradient(listOf(Bg1, Bg2))
}

/** 雾彩背景层：三条径向渐变（右上紫 / 左中品红 / 底部青） */
@Composable
fun AgAuroraBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(560.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0x24A855F7), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(700f, -40f),
                        radius = 640f,
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0x17EC4899), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(30f, 260f),
                        radius = 560f,
                    )
                )
        )
    }
}

/** 按压 + 点击（统一触感反馈） */
@Composable
fun Modifier.agClick(pressScale: Float = 0.965f, enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) pressScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "ag-click",
    )
    return this
        .graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

/** 毛玻璃感卡片（高斯模糊由背景雾彩透出，82–90% 白面） */
@Composable
fun AgCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    if (onClick != null) {
        Column(
            modifier
                .agClick(onClick = onClick)
                .clip(shape)
                .background(Ag.Card)
                .border(1.dp, Ag.Line, shape)
                .padding(contentPadding)
                .animateContentSize(),
            content = content,
        )
    } else {
        Column(
            modifier
                .clip(shape)
                .background(Ag.Card)
                .border(1.dp, Ag.Line, shape)
                .padding(contentPadding),
            content = content,
        )
    }
}

/** 信息行：.ric 色衬图标 + 标题/描述 + chev */
@Composable
fun AgRow(
    title: String,
    desc: String,
    tint: Color,
    icon: ImageVector,
    showChev: Boolean = false,
    trailing: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val base: Modifier = Modifier.fillMaxWidth()
    Row(
        (if (onClick != null) base.agClick(onClick = onClick) else base)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AgRic(tint, icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Ag.Tx)
            if (desc.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(desc, fontSize = 12.5.sp, lineHeight = 19.sp, color = Ag.Tx2)
            }
        }
        trailing?.invoke(this)
        if (showChev) {
            Spacer(Modifier.width(6.dp))
            Text("›", fontSize = 20.sp, color = Ag.Chev, fontWeight = FontWeight.Bold)
        }
    }
}

/** 色衬图标容器 .ric：38dp 圆角 12 + 10% tint 底 */
@Composable
fun AgRic(tint: Color, icon: ImageVector, size: Dp = 38.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

/** 顶栏 40dp 圆角玻璃图标按钮 .icbtn */
@Composable
fun AgIconBtn(icon: ImageVector, onClick: () -> Unit, tint: Color = Ag.Tx2) {
    Box(
        Modifier
            .size(40.dp)
            .agClick(onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(Ag.Card)
            .border(1.dp, Ag.Line, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

/** 品牌渐变主标题：32sp/800，em 部分渐变 */
@Composable
fun AgTitle(plain: String, em: String, modifier: Modifier = Modifier, count: String? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            buildString { append(plain); append(em) },
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.6).sp,
            style = TextStyle(brush = Ag.TitleGrad),
        )
        if (count != null) {
            Spacer(Modifier.width(10.dp))
            Text(count, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ag.Tx3, modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}

/** 呼吸点徽标 .badge */
@Composable
fun AgBadge(text: String) {
    val t = rememberInfiniteTransition(label = "ag-badge")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "dot")
    Row(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Color(0x1A10B981))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .graphicsLayer { shadowElevation = 0f }
                .background(Ag.Success, CircleShape)
                .border((2 + 3 * p).dp, Color(0x0D10B981), CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0E9F6E))
    }
}

/** 信息胶囊 .chip */
@Composable
fun AgChip(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Color(0x0D1E1946))
            .border(1.dp, Ag.Line, RoundedCornerShape(99.dp))
            .padding(horizontal = 11.dp, vertical = 5.dp),
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ag.Tx2)
    }
}

/** 主/次操作按钮 .gbtn */
@Composable
fun AgButton(primary: Boolean, text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(38.dp)
            .agClick(enabled = enabled, onClick = onClick)
            .then(
                if (primary) Modifier
                    .background(Ag.Grad, shape)
                    .shadow(6.dp, shape, spotColor = Color(0x4D6C5CE7), ambientColor = Color(0x4D6C5CE7))
                else Modifier
                    .background(Color(0x126C5CE7), shape)
                    .border(1.dp, Color(0x406C5CE7), shape)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (primary) Color.White else Ag.Vio)
    }
}

/** 渐变开关 .tgl（52×30 / sm 46×27） */
@Composable
fun AgToggle(checked: Boolean, onCheckedChange: () -> Unit, small: Boolean = false) {
    val w = if (small) 46.dp else 52.dp
    val h = if (small) 27.dp else 30.dp
    val knob = if (small) 21.dp else 24.dp
    val pad = 3.dp
    val travel = w - knob - pad * 2
    val off by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(250),
        label = "ag-tgl",
    )
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val press by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "ag-tgl-press",
    )
    Box(
        Modifier
            .width(w)
            .height(h)
            .graphicsLayer { scaleX = press; scaleY = press }
            .clickable(interactionSource = src, indication = null, onClick = onCheckedChange)
            .clip(RoundedCornerShape(99.dp))
            .background(if (checked) Ag.Grad else androidx.compose.ui.graphics.SolidColor(Color(0xFFD9D8E6))),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .graphicsLayer { translationX = travel.toPx() * off }
                .padding(start = pad)
                .size(knob)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

/** 搜索框 .search */
@Composable
fun AgSearch(value: String, onValueChange: (String) -> Unit, hint: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x0D1E1946))
            .border(1.dp, Ag.Line, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = Ag.Tx3, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Ag.Tx, fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(hint, fontSize = 14.sp, color = Ag.Tx3)
                    }
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close, null, tint = Ag.Tx3,
                modifier = Modifier
                    .size(16.dp)
                    .agClick { onValueChange("") },
            )
        }
    }
}

/** 小节标题 .ghead */
@Composable
fun AgGHead(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp,
        color = Ag.Tx3,
        modifier = Modifier.padding(start = 6.dp, top = 20.dp, bottom = 10.dp),
    )
}

/** 品牌渐变 FAB .fab */
@Composable
fun AgFab(onClick: () -> Unit) {
    Box(
        Modifier
            .size(58.dp)
            .agClick(pressScale = 0.9f, onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(Ag.Grad)
            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color(0x666C5CE7)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

/** 包名 → 稳定渐变色对（App 图标底衬，取自设计稿配色） */
fun gradientFor(seed: String): Pair<Color, Color> {
    val palettes = listOf(
        Color(0xFF22C1F2) to Color(0xFF1D7AF3),
        Color(0xFF2DD4BF) to Color(0xFF0D9488),
        Color(0xFF64748B) to Color(0xFF2B3648),
        Color(0xFF111827) to Color(0xFF1E3A8A),
        Color(0xFFA78BFA) to Color(0xFF7C3AED),
        Color(0xFF6C5CE7) to Color(0xFFEC4899),
        Color(0xFFF59E0B) to Color(0xFFF97316),
        Color(0xFF10B981) to Color(0xFF34D399),
    )
    return palettes[kotlin.math.abs(seed.hashCode()) % palettes.size]
}
