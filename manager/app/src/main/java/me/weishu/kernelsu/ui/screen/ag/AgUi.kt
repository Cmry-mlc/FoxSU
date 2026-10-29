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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
    // ---- 暗色模式判定（tokens.css L3 [data-mode="dark"]）----
    val isDark: Boolean @Composable get() = when (me.weishu.kernelsu.ui.theme.LocalColorMode.current) {
        2, 5, 6 -> true   // DARK / MONET_DARK / DARK_AMOLED
        1, 4 -> false     // LIGHT / MONET_LIGHT
        else -> androidx.compose.foundation.isSystemInDarkTheme() // 跟随系统
    }

    // ---- 背景与画布 ----
    val Bg1: Color @Composable get() = if (isDark) Color(0xFF131221) else Color(0xFFF7F6FB)
    val Bg2: Color @Composable get() = if (isDark) Color(0xFF1A192E) else Color(0xFFF2F4FA)

    // ---- 表面（与背景同色系，靠透明度分层；tokens v1.2.0）----
    val Card: Color @Composable get() = if (isDark) Color(0x8C1A192E) else Color(0x8CF7F6FB)   // rgba(...,.55)
    val CardSolid: Color @Composable get() = if (isDark) Color(0xFF1E1D30) else Color(0xFFF7F6FB) // 暗色实底提亮一档

    // ---- 描边 ----
    val Line: Color @Composable get() = if (isDark) Color(0x17FFFFFF) else Color(0x121E1946)   // .09 / .07
    val TintInk: Color @Composable get() = if (isDark) Color(0x12FFFFFF) else Color(0x0D1E1946)

    // ---- 文字 ----
    val Tx: Color @Composable get() = if (isDark) Color(0xFFF2F1FA) else Color(0xFF191826)
    val Tx2: Color @Composable get() = if (isDark) Color(0xFFB2B0CC) else Color(0xFF66647A)
    val Tx3: Color @Composable get() = if (isDark) Color(0xFF7C7A98) else Color(0xFF9C9AAE)
    val Chev: Color @Composable get() = if (isDark) Color(0xFF5E5C7A) else Color(0xFFC6C4D6)

    // ---- 品牌（明暗一致）----
    val Vio = Color(0xFF6C5CE7)
    val Vio2 = Color(0xFF8E5CF6)
    val Mag = Color(0xFFEC4899)
    val Teal = Color(0xFF14B8A6)
    val Amber = Color(0xFFF59E0B)
    val Danger = Color(0xFFDC2626)
    val Success = Color(0xFF10B981)

    val Grad: Brush
        get() = Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFFA855F7), Color(0xFFEC4899)))

    val TitleGrad = Grad

    /** 页面底色线性渐变（设计稿 linear-gradient(160deg, --bg1, --bg2)） */
    @Composable
    fun background(): Brush = Brush.linearGradient(
        listOf(Bg1, Bg2),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )

    /** 固定顶栏底：设计稿 .headwrap 完全透明 + blur(14px)，无分隔线 */
    @Composable
    fun headSurface(): Color = Color.Transparent

    /** 搜索框底（.search rgba(30,25,70,.05) / 暗色 rgba(255,255,255,.06)） */
    @Composable
    fun searchBg(): Color = if (isDark) Color(0x0FFFFFFF) else Color(0x0D1E1946)

    /** 开关 OFF 轨道（明暗一致）*/
    val ToggleOff = Color(0xFFD9D8E6)

    /** .ric 图标底透明度：v/m 10%，t/a 12% */
    fun ricAlpha(tint: Color): Float =
        if (tint == Teal || tint == Amber) 0.12f else 0.10f
}

/**
 * 设计稿 `.headwrap{background:transparent;backdrop-filter:blur(14px)}`。
 *
 * 这里用「半透明雾面 + 底部渐隐」复刻视觉，而不再走 `drawBackdrop` 的实时背景采样：
 * 顶栏是每一页的首帧就要绘制的节点，实时模糊会强制整页内容先录进 RenderNode 再采样，
 * 在部分真机 GPU 上首帧直接崩溃（表现为启动页卡住后闪退）。
 * 14px 模糊在浅色底上的观感与 72%~86% 白雾几乎一致，静态实现更稳。
 */
@Composable
fun Modifier.agHeadBlur(): Modifier {
    val top = if (Ag.isDark) Color(0xF21A1826) else Color(0xF7FFFFFF)
    val mid = if (Ag.isDark) Color(0xD91A1826) else Color(0xE6FFFFFF)
    return this.drawBehind {
        drawRect(
            brush = Brush.verticalGradient(
                0f to top,
                0.62f to mid,
                1f to Color.Transparent,
                startY = 0f,
                endY = size.height,
            ),
        )
    }
}

/**
 * 页面背景：设计稿 .phone 的 linear-gradient(160deg,--bg1,--bg2)
 * + .phone::before 三向径向雾彩（紫 .14 / 品红 .09 / 青 .10）。
 * 玻璃卡之所以"透"，靠的就是这三团雾彩在背后发光。
 */
@Composable
fun Modifier.agPageBg(): Modifier {
    val b1 = Ag.Bg1
    val b2 = Ag.Bg2
    return this.drawBehind {
        // 底色：160deg 斜向渐变
        drawRect(
            brush = Brush.linearGradient(
                listOf(b1, b2),
                start = Offset(0f, 0f),
                end = Offset(size.width * 0.34f, size.height * 0.94f),
            )
        )
        // 紫色雾彩：at 110% -5%，rgba(168,85,247,.14)
        drawRect(
            brush = Brush.radialGradient(
                0f to Color(0x24A855F7),
                0.65f to Color(0x00A855F7),
                1f to Color(0x00A855F7),
                center = Offset(size.width * 1.10f, -size.height * 0.05f),
                radius = size.width * 1.08f,
            )
        )
        // 品红雾彩：at -15% 25%，rgba(236,72,153,.09)
        drawRect(
            brush = Brush.radialGradient(
                0f to Color(0x17EC4899),
                0.65f to Color(0x00EC4899),
                1f to Color(0x00EC4899),
                center = Offset(size.width * -0.15f, size.height * 0.25f),
                radius = size.width * 0.92f,
            )
        )
        // 青色雾彩：at 50% 110%，rgba(20,184,166,.10)
        drawRect(
            brush = Brush.radialGradient(
                0f to Color(0x1A14B8A6),
                0.65f to Color(0x0014B8A6),
                1f to Color(0x0014B8A6),
                center = Offset(size.width * 0.50f, size.height * 1.10f),
                radius = size.width * 1.03f,
            )
        )
    }
}

/** 旧的纯色背景（保留兼容） */
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
                        center = Offset(700f, -40f),
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
                        center = Offset(30f, 260f),
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

/** 毛玻璃感卡片（高斯模糊由背景雾彩透出，82–90% 白面）+ 轻影 0 2px 10px rgba(60,45,120,.04) */
@Composable
fun AgCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    // 液态玻璃全局生效：开启后所有玻璃卡改为半透明白 + 亮描边（雾彩背景透出）
    val glass = me.weishu.kernelsu.data.settings.CustomizationStore.current.cardGlass
    val glassCard = Ag.Card.copy(alpha = if (glass) 0.55f else Ag.Card.alpha)
    val glassLine = if (glass) Color(0x33FFFFFF) else Ag.Line
    val dark = Ag.isDark
    val shadowColor = if (dark) Color(0x40000000) else Color(0x0A3C2D78)
    val ambient = if (dark) Color(0x26000000) else Color(0x073C2D78)
    if (onClick != null) {
        Column(
            modifier
                .agClick(onClick = onClick)
                .shadow(2.dp, shape, spotColor = shadowColor, ambientColor = ambient)
                .clip(shape)
                .background(glassCard)
                .border(1.dp, glassLine, shape)
                .padding(contentPadding)
                .animateContentSize(),
            content = content,
        )
    } else {
        Column(
            modifier
                .shadow(2.dp, shape, spotColor = shadowColor, ambientColor = ambient)
                .clip(shape)
                .background(glassCard)
                .border(1.dp, glassLine, shape)
                .padding(contentPadding),
            content = content,
        )
    }
}

/** 信息行：.ric 色衬图标 + 标题/描述 + chev（行底 1px 分隔线，最后一行无线） */
@Composable
fun AgRow(
    title: String,
    desc: String,
    tint: Color,
    icon: ImageVector,
    showChev: Boolean = false,
    divider: Boolean = true,
    trailing: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val base: Modifier = Modifier.fillMaxWidth()
    Column(
        (if (onClick != null) base.agClick(onClick = onClick) else base)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
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
        if (divider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Ag.Line)
            )
        }
    }
}

/**
 * 开关行 .trow：设计稿 padding 15px 0 + 1px 分隔线（末行无线），
 * 与 .row（padding 14px 0 + 14px gap）区分。
 */
@Composable
fun AgToggleRow(
    title: String,
    desc: String,
    tint: Color,
    icon: ImageVector,
    divider: Boolean = true,
    trailing: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val base: Modifier = Modifier.fillMaxWidth()
    Column(base) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AgRic(tint, icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Ag.Tx)
                if (desc.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(desc, fontSize = 12.5.sp, lineHeight = 18.sp, color = Ag.Tx2)
                }
            }
            trailing()
        }
        if (divider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Ag.Line)
            )
        }
    }
}

/** 色衬图标容器 .ric：38dp 圆角 12 + 10%/12% tint 底 + 19px 描边图标 */
@Composable
fun AgRic(tint: Color, icon: ImageVector, size: Dp = 38.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = Ag.ricAlpha(tint))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

/** 顶栏 40dp 圆角玻璃图标按钮 .icbtn（18px 描边图标） */
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
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** 品牌渐变主标题：32sp/800，em 部分渐变 */
@Composable
fun AgTitle(
    plain: String,
    em: String,
    modifier: Modifier = Modifier,
    count: String? = null,
    topMargin: Dp = 24.dp,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = topMargin, bottom = 16.dp),
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
            Spacer(Modifier.width(8.dp))
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
            .background(Ag.TintInk)
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
                    .clip(shape)
                    .background(Ag.Grad, shape)
                    .shadow(6.dp, shape, spotColor = Color(0x4D6C5CE7), ambientColor = Color(0x4D6C5CE7))
                else Modifier
                    .background(Color(0x126C5CE7), shape)
                    .border(1.dp, Color(0x406C5CE7), shape)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (primary && enabled) {
            // 渐变动态色彩：高光带循环流动
            val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "ag-btn-flow")
            val shift by t.animateFloat(
                initialValue = -1f,
                targetValue = 2f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(2600, easing = androidx.compose.animation.core.LinearEasing),
                ),
                label = "ag-btn-flow-v",
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .graphicsLayer { translationX = 300.dp.toPx() * shift }
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent,
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(0f, 10000f),
                        ),
                    ),
            )
        }
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
            .background(if (checked) Ag.Grad else androidx.compose.ui.graphics.SolidColor(Ag.ToggleOff)),
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
            .background(Ag.searchBg())
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
