package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.AppIconImage
import me.weishu.kernelsu.ui.screen.superuser.GroupedApps

/** Nocturne 统一 spring（设计稿 cubic-bezier(.2,1.3,.4,1) 的近似映射）。 */
fun <T> ncSpring() = spring<T>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)

/** 与 Glass 相同的状态栏避让。 */
@Composable
fun ncStatusTop(): Dp =
    androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

/** mono 值文本样式。 */
fun ncMono() = FontFamily.Monospace

/** 页面双径向光晕背景（violet 13% + mint 6%，亮色 16%/12%）。 */
@Composable
fun NcGlowBackground(t: NcTokens) {
    Box(
        Modifier
            .fillMaxSize()
            .background(t.bg)
    ) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-60).dp, y = (-90).dp)
                .size(420.dp, 260.dp)
                .background(
                    Brush.radialGradient(
                        listOf(t.violet.copy(alpha = if (t.dark) 0.13f else 0.16f), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-40).dp)
                .size(320.dp, 220.dp)
                .background(
                    Brush.radialGradient(
                        listOf(t.mint.copy(alpha = if (t.dark) 0.06f else 0.12f), Color.Transparent)
                    )
                )
        )
    }
}

/** 眉题 + 大标题头部，右侧 36×36 r13 圆钮。 */
@Composable
fun NcHeader(
    kicker: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val t = ncTokens()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 16.dp, start = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = kicker.uppercase(),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = t.violetKicker,
            )
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = t.text1,
                lineHeight = 32.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (trailing != null) trailing.invoke(this)
    }
}

/** 头部 36×36 r13 圆钮。 */
@Composable
fun NcRoundButton(iconRes: Int, onClick: () -> Unit, tint: Color? = null) {
    val t = ncTokens()
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(NcRadius.btn))
            .background(t.fill)
            .border(1.dp, t.line, RoundedCornerShape(NcRadius.btn))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint ?: t.iconTint,
        )
    }
}

/** Group r20 分组卡。 */
@Composable
fun NcPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = ncTokens()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                if (t.dark) 14.dp else 4.dp,
                RoundedCornerShape(NcRadius.panel),
                ambientColor = if (t.dark) Color(0x66000000) else Color(0x14202560),
                spotColor = if (t.dark) Color(0x66000000) else Color(0x14202560),
            )
            .clip(RoundedCornerShape(NcRadius.panel))
            .background(t.surface1)
            .border(1.dp, t.line, RoundedCornerShape(NcRadius.panel)),
        content = content,
    )
}

/** 行内 34dp r11 四色图标盒。 */
@Composable
fun NcIconBox(iconRes: Int, accent: Color, size: Dp = NcDimens.iconBox) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(NcRadius.iconBox))
            .background(accent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = accent,
        )
    }
}

/** Group 行：图标盒 + 标题/副标题 + 右侧内容；分隔线左缩进 57dp。 */
@Composable
fun NcGroupRow(
    iconRes: Int,
    accent: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showDivider: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val t = ncTokens()
    Column {
        if (showDivider) {
            Box(
                Modifier
                    .padding(start = 57.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(t.line)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = NcDimens.rowPadX, vertical = NcDimens.rowPadY + 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NcIconBox(iconRes = iconRes, accent = accent)
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = t.text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = t.text2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            trailing.invoke(this)
        }
    }
}

/** 行尾箭头。 */
@Composable
fun NcChevron() {
    val t = ncTokens()
    Text("›", fontSize = 16.sp, color = t.text3)
}

/** 值徽章（pill 8.5px 圆角）。 */
@Composable
fun NcBadge(text: String, color: Color, bgAlpha: Float = 0.15f) {
    Text(
        text = text,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.5.sp,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(NcRadius.pill))
            .background(color.copy(alpha = bgAlpha))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(NcRadius.pill))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

/** 42×25 开关（thumb 19，on 态 violet 渐变，220ms spring）。 */
@Composable
fun NcSwitch(checked: Boolean, onClick: (() -> Unit)? = null) {
    val t = ncTokens()
    val shift by animateFloatAsState(
        targetValue = if (checked) 17.dp.value else 0f,
        animationSpec = tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "ncSwitch",
    )
    Box(
        modifier = Modifier
            .size(width = 42.dp, height = 25.dp)
            .clip(RoundedCornerShape(NcRadius.pill))
            .background(
                if (checked) Brush.horizontalGradient(listOf(t.violet, t.violetDeep))
                else Brush.horizontalGradient(listOf(t.fill, t.fill))
            )
            .border(
                1.dp,
                if (checked) Color.Transparent else t.line,
                RoundedCornerShape(NcRadius.pill),
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(3.dp)
                .offset(x = Dp(shift))
                .size(19.dp)
                .shadow(if (checked) 2.dp else 0.dp, CircleShape)
                .clip(CircleShape)
                .background(if (checked) Color.White else t.text2.copy(alpha = 0.7f)),
        )
    }
}

/** CodeCard r16：左 2dp violet→mint 光条 + mono 值。 */
@Composable
fun NcCodeCard(kicker: String, value: String) {
    val t = ncTokens()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NcRadius.card))
            .background(t.surface2)
            .border(1.dp, t.line, RoundedCornerShape(NcRadius.card)),
    ) {
        Box(
            Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Brush.verticalGradient(listOf(t.violet, t.mint)))
        )
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(
                text = kicker.uppercase(),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.2.sp,
                color = t.violetText,
            )
            Text(
                text = value,
                fontSize = 9.3.sp,
                fontFamily = ncMono(),
                color = t.textMono,
                lineHeight = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** 搜索框 r15（fill 白5% + line 描边）。 */
@Composable
fun NcSearchField(value: String, onValueChange: (String) -> Unit, hint: String) {
    val t = ncTokens()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(t.fill)
            .border(1.dp, t.line, RoundedCornerShape(15.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_d_search),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = t.text3,
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 12.sp, color = t.text1),
            cursorBrush = Brush.verticalGradient(listOf(t.violet, t.violet)),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(text = hint, fontSize = 12.sp, color = t.text3)
                    }
                    inner()
                }
            },
        )
    }
}

/** 超级用户应用行：36dp 真实图标 + 名称 + 包名 + ROOT 徽章。 */
@Composable
fun NcAppRow(app: GroupedApps, onClick: () -> Unit) {
    val t = ncTokens()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NcRadius.panel))
            .background(t.surface1)
            .border(1.dp, t.line, RoundedCornerShape(NcRadius.panel))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconImage(
            modifier = Modifier
                .size(NcDimens.appAvatar)
                .clip(RoundedCornerShape(12.dp)),
            packageInfo = app.primary.packageInfo,
            label = app.primary.label,
        )
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.primary.label,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = t.text1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.primary.displayIdentifier,
                fontSize = 10.sp,
                color = t.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (app.anyAllowSu) {
            NcBadge(text = "ROOT", color = t.mint)
        } else {
            NcBadge(text = "未授权", color = t.text3)
        }
    }
}

/** 分组小标题。 */
@Composable
fun NcSectionHead(title: String) {
    val t = ncTokens()
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = t.text2,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 9.dp),
    )
}
