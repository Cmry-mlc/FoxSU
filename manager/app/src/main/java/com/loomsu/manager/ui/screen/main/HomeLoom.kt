package com.loomsu.manager.ui.screen.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loomsu.manager.R
import com.loomsu.manager.data.theme.ThemeRepository
import com.loomsu.manager.ui.screen.splash.LoomMark
import com.loomsu.manager.ui.theme.LocalLoomPalette
import com.loomsu.manager.ui.theme.LoomColors
import com.loomsu.manager.ui.theme.LocalDarkBackdrop
import com.loomsu.manager.ui.theme.CardConfig
import com.loomsu.manager.ui.theme.renderBackgroundBlur
import com.loomsu.manager.ui.theme.LoomDimens
import com.loomsu.manager.ui.theme.LoomGradients
import com.loomsu.manager.ui.theme.LoomTone
import com.loomsu.manager.ui.theme.LoomType
import com.loomsu.manager.ui.theme.ThemeConfig
import com.loomsu.manager.ui.theme.shiftHue
import org.koin.compose.koinInject

/** 四宫格中的一个快捷入口 */
data class LoomQuickAction(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val tone: LoomTone,
    val onClick: () -> Unit,
)

/** 系统动态中的一行 */
data class LoomFeedItem(
    val key: String,
    val title: String,
    val desc: String,
    val value: String,
    val icon: ImageVector,
    val tone: LoomTone,
    val valueColor: Color,
    val onClick: (() -> Unit)? = null,
)

/** 顶部问候行：头像 + WELCOME BACK / Loom SU + 通知铃铛 */
@Composable
fun LoomGreetingRow(
    modifier: Modifier = Modifier,
    onAvatarClick: () -> Unit = {},
    onBellClick: () -> Unit = {},
    notificationCount: Int = 0,
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val kicker = themeConfig.homeKicker.ifBlank { ThemeRepository.DEFAULT_HOME_KICKER }
    val title = themeConfig.homeTitle.ifBlank { "Loom SU" }
    val split = themeConfig.homeTitleSplit.coerceIn(0, title.length)
    // 标题两段颜色是用户自选的十六进制色，默认值按白底设计（#101322）。
    // 夜间画布是纯黑，这一对颜色会直接消失，所以深色下按亮度把它提亮，
    // 但仍保留两段之间的明暗差，不至于两段变成同一种白。
    // 深浅不只看主题：开了自定义壁纸且壁纸偏暗时，这里同样要换成浅字，
    // 否则「Loom SU」是深蓝压在暗花丛上，整块标题糊成一团。
    val isDarkCanvas = LocalDarkBackdrop.current
    val titlePrimary = themeConfig.homeTitleColorPrimary.adaptTitleInk(isDarkCanvas, 0.92f)
    val titleAccent = themeConfig.homeTitleColorAccent.adaptTitleInk(isDarkCanvas, 0.62f)
    val titleText = buildAnnotatedString {
        withStyle(SpanStyle(color = titlePrimary)) {
            append(title.take(split))
        }
        withStyle(SpanStyle(color = titleAccent)) {
            append(title.drop(split))
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(LoomGradients.avatar)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center,
        ) {
            LoomMark(progress = 1f, modifier = Modifier.size(34.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            LoomKickerTicker(
                text = kicker,
                animated = themeConfig.kickerTicker,
                modifier = Modifier.widthIn(max = 200.dp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = titleText, style = LoomType.greeting)
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                // 不再用 elevation 阴影：离屏层 + 半透明底在部分 ROM 上会
                // 连带画出一块方形白底（图标周围那个小白块就是这么来的）。
                // 层次改由 1dp 描边提供。
                .background(LoomColors.canvasCard)
                .border(
                    width = 1.dp,
                    color = LoomColors.line.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(13.dp),
                )
                .clickable(onClick = onBellClick),
            contentAlignment = Alignment.Center,
        ) {
            // 用项目内自绘的单色铃铛：material icon 在部分设备上会把
            // 矢量填充区域带成白色方底，换成纯 path 后不会有底板。
            Icon(
                painter = painterResource(R.drawable.ic_loom_bell),
                contentDescription = "通知",
                tint = LoomColors.textLabel,
                modifier = Modifier.size(19.dp),
            )
            // 未读角标：正圆 13dp，贴在按钮右上角内侧
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-5).dp, y = 5.dp)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(LoomColors.danger),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (notificationCount > 9) "9+" else notificationCount.toString(),
                        fontSize = 7.5.sp,
                        lineHeight = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/**
 * 标题上方的小字：一条持续向左流动的跑马灯。
 *
 * 用 basicMarquee 让整行文字循环滚动（内容超出 200dp 才滚，默认文案一定会超），
 * 关掉开关就退回单行省略，字号、字距、位置都不变。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoomKickerTicker(
    text: String,
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
    val style = LoomType.kicker.copy(letterSpacing = 2.sp)
    val scroll = if (animated) {
        Modifier.basicMarquee(
            iterations = Int.MAX_VALUE,
            velocity = 26.dp,
            initialDelayMillis = 700,
        )
    } else {
        Modifier
    }
    Text(
        text = text,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier.then(scroll),
    )
}

/**
 * 内核状态大卡（对应设计稿的深色状态卡）。
 *
 * 深靛渐变 + 双光斑 + 右下弧线，主体是状态与两项指标。
 * [modeTags] 是真实运行模式的短标签（LKM / 内置 / 越狱模式 / 架构）。
 * 整张卡片就是一个入口：未工作时点它进入安装页，已工作时进入系统信息页。
 */
@Composable
fun LoomHeroCard(
    modifier: Modifier = Modifier,
    label: String,
    stateText: String,
    stateHint: String,
    badgeText: String?,
    badgeColor: Color,
    superuserCount: Int,
    moduleCount: Int,
    superuserLabel: String = "超级用户",
    moduleLabel: String = "模块",
    superuserUnit: String = "已授权",
    moduleUnit: String = "已安装",
    modeTags: List<String> = emptyList(),
    isActivated: Boolean = false,
    onClick: () -> Unit = {},
) {
    val shape = LoomDimens.rHero
    val cardConfig: CardConfig = koinInject()
    val glass = cardConfig.isCustomBackgroundEnabled
    val isDarkCanvas = LocalLoomPalette.current.canvasStage.luminance() < 0.5f
    // 亮色 + 自定义壁纸：大卡要跟下面那些卡片一样是「白底磨砂」，不能还是黑板。
    // 深色 + 壁纸才继续用深色磨砂。
    val lightGlass = glass && !isDarkCanvas
    // 卡片上的文字：亮色磨砂用墨色，深色卡用白字。
    val onHero = if (lightGlass) LoomColors.text else Color.White
    val onHeroSoft = if (lightGlass) {
        LoomColors.text.copy(alpha = 0.72f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }
    val onHeroFaint = if (lightGlass) {
        LoomColors.text.copy(alpha = 0.58f)
    } else {
        Color.White.copy(alpha = 0.62f)
    }
    val heroHairline = if (lightGlass) LoomColors.line else Color.White.copy(alpha = 0.12f)
    // 自定义壁纸下大卡改成半透明磨砂：壁纸的模糊版垫在下面，
    // 深色渐变只留一层薄色，不再是压在壁纸上的一块实心黑板。
    val fillAlpha = if (glass) 0.38f else 1f
    // 卡片颜色跟随主题色：以纯黑为底按比例混入强调色，
    // 所以默认（黑色主题）就是黑卡，换成绿/紫/红时会变成对应的深色卡片，
    // 而原有的弧线与光斑全部保留，只换色不动形。
    val accent = MaterialTheme.colorScheme.primary
    // 夜间画布是纯黑，强调色也换成了中性近白：这时若继续按比例混色，
    // 卡片会变成一块中灰板，白字反而发虚。深色下直接给固定的黑灰卡面，
    // 让它在纯黑背景上靠一层微亮和阴影浮起来。
    val deepBase = if (isDarkCanvas) Color(0xFF161618) else lerp(Color(0xFF05060A), accent, 0.30f)
    val midBase = if (isDarkCanvas) Color(0xFF242427) else lerp(Color(0xFF0E1120), accent, 0.50f)
    val edgeBase = if (isDarkCanvas) Color(0xFF121214) else lerp(Color(0xFF05060A), accent, 0.22f)
    val glowAlpha = if (isDarkCanvas) 0.16f else 0.52f
    val glowSecondary = accent.shiftHue(150f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                // 磨砂卡不要再压一层 18dp 的厚影，否则又变成一块浮在壁纸上的黑板
                elevation = if (glass) 6.dp else 18.dp,
                shape = shape,
                clip = false,
                ambientColor = if (isDarkCanvas) Color.Black else accent.copy(alpha = 0.18f),
                spotColor = if (isDarkCanvas) Color.Black else accent.copy(alpha = 0.28f),
            )
            .clip(shape)
            .then(
                if (glass) {
                    Modifier.renderBackgroundBlur(
                        tintColor = if (lightGlass) Color.White else Color.Black,
                    )
                } else {
                    Modifier
                },
            )
            .drawBehind {
                if (lightGlass) {
                    // 亮色壁纸：白卡上只留一层极淡的强调色光斑，其余交给磨砂层
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.86f, size.height * 0.02f),
                            radius = size.width * 0.6f,
                        ),
                        radius = size.width * 0.6f,
                        center = Offset(size.width * 0.86f, size.height * 0.02f),
                    )
                    return@drawBehind
                }
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            deepBase.copy(alpha = fillAlpha),
                            midBase.copy(alpha = fillAlpha),
                            edgeBase.copy(alpha = fillAlpha),
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.8f, size.height),
                    ),
                )
                // 右上主光斑
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = glowAlpha * fillAlpha), Color.Transparent),
                        center = Offset(size.width * 0.84f, -size.height * 0.06f),
                        radius = size.width * 0.62f,
                    ),
                    radius = size.width * 0.62f,
                    center = Offset(size.width * 0.84f, -size.height * 0.06f),
                )
                // 左下余光（互补色）
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowSecondary.copy(alpha = glowAlpha * 0.62f * fillAlpha),
                            Color.Transparent,
                        ),
                        center = Offset(-size.width * 0.08f, size.height * 1.08f),
                        radius = size.width * 0.52f,
                    ),
                    radius = size.width * 0.52f,
                    center = Offset(-size.width * 0.08f, size.height * 1.08f),
                )
            }
            .clickable(onClick = onClick),
    ) {
        // 右下角弧线装饰：铺满卡片但不参与测量
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val p1 = Path().apply {
                moveTo(w * 0.52f, h * 1.02f)
                cubicTo(w * 0.72f, h * 0.52f, w * 0.9f, h * 0.9f, w * 1.3f, h * 0.24f)
            }
            val p2 = Path().apply {
                moveTo(w * 0.55f, h * 1.18f)
                cubicTo(w * 0.76f, h * 0.66f, w * 0.94f, h * 1.04f, w * 1.32f, h * 0.42f)
            }
            val arcAlpha = if (lightGlass) 0.14f else 0.42f
            drawPath(p1, accent.copy(alpha = arcAlpha), style = Stroke(width = 1.4f))
            drawPath(p2, accent.copy(alpha = arcAlpha * 0.52f), style = Stroke(width = 1.2f))
            drawCircle(lerp(accent, Color.White, 0.45f), radius = 3.5f, center = Offset(w * 0.94f, h * 0.42f))
        }

        Column(
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 0.dp),
        ) {
            Text(
                text = label,
                style = LoomType.heroLabel,
                color = onHeroFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stateText,
                        style = LoomType.heroState,
                        color = onHero,
                        maxLines = 1,
                    )
                    if (stateHint.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stateHint,
                            style = LoomType.heroHint,
                            color = onHeroSoft,
                            maxLines = 1,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 状态角标：圆形底色与配色保持不变，中间只显示 ✓ / ×
                if (!badgeText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.18f))
                            .drawBehind {
                                drawCircle(
                                    color = badgeColor.copy(alpha = 0.38f),
                                    style = Stroke(width = 1.dp.toPx()),
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isActivated) Icons.TwoTone.Check else Icons.TwoTone.Close,
                            contentDescription = badgeText,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            if (modeTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    modeTags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(
                                    if (lightGlass) Color.Black.copy(alpha = 0.05f)
                                    else Color.White.copy(alpha = 0.10f),
                                )
                                .drawBehind {
                                    drawRoundRect(
                                        color = if (lightGlass) Color.Black.copy(alpha = 0.06f)
                                        else Color.White.copy(alpha = 0.16f),
                                        style = Stroke(width = 1.dp.toPx()),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = tag,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (lightGlass) LoomColors.textBody else Color.White.copy(alpha = 0.86f),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (modeTags.isEmpty()) 18.dp else 16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(heroHairline),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp, bottom = 18.dp),
            ) {
                HeroMetric(
                    modifier = Modifier.weight(1f),
                    label = superuserLabel,
                    value = superuserCount,
                    unit = superuserUnit,
                    // 小方点也走主题色：近黑主题下用大比例白混出浅灰，保证在深卡上可见
                    dotColor = if (lightGlass) accent else lerp(accent, Color.White, 0.78f),
                    ink = onHero,
                    inkSoft = onHeroSoft,
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(heroHairline)
                        .align(Alignment.CenterVertically),
                )
                HeroMetric(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 18.dp),
                    label = moduleLabel,
                    value = moduleCount,
                    unit = moduleUnit,
                    dotColor = if (lightGlass) lerp(accent, Color.Black, 0.2f) else lerp(accent, Color.White, 0.50f),
                    ink = onHero,
                    inkSoft = onHeroSoft,
                )
            }
        }
    }
}

@Composable
private fun HeroMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    unit: String,
    dotColor: Color,
    ink: Color = Color.White,
    inkSoft: Color = Color.White,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(dotColor),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = LoomType.heroMetricLabel,
                color = ink.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value.toString(),
                style = LoomType.heroMetricValue,
                color = ink,
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = unit,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ink.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

/** 四宫格快捷入口 */
@Composable
fun LoomQuickGrid(
    actions: List<LoomQuickAction>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { action ->
            QuickTile(action = action, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickTile(action: LoomQuickAction, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .shadow(4.dp, shape, clip = false, ambientColor = Color(0x1418262C), spotColor = Color(0x1A18263C))
            .clip(shape)
            // 开了背景（壁纸 / 特效）时卡片是半透明的，壁纸再走一层模糊，
            // 特效和壁纸才能从卡片底下透出来。
            .background(LoomColors.canvasCard)
            .renderBackgroundBlur(tintColor = LoomColors.canvasCardSolid)
            .border(1.dp, LoomColors.line.copy(alpha = 0.75f), shape)
            .clickable(onClick = action.onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(action.tone.container)
                .border(1.dp, action.tone.content.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = action.tone.content,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = action.label,
            style = LoomType.quickLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 区块标题：左标题 + 右动作 */
@Composable
fun LoomSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = LoomType.sectionTitle)
        Spacer(modifier = Modifier.weight(1f))
        if (!actionText.isNullOrBlank()) {
            Text(
                text = actionText,
                style = LoomType.sectionAction,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

/** 系统动态列表卡 */
@Composable
fun LoomFeedCard(
    items: List<LoomFeedItem>,
    modifier: Modifier = Modifier,
) {
    // 自定义壁纸下不画行间发丝线：浅色细线压在花丛上会连成一排「小白条」。
    val cardConfig: CardConfig = koinInject()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, LoomDimens.rCard, clip = false, ambientColor = Color(0x0D18263C), spotColor = Color(0x0D18263C))
            .clip(LoomDimens.rCard)
            .background(LoomColors.canvasCard)
            .renderBackgroundBlur(tintColor = LoomColors.canvasCardSolid)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0 && !cardConfig.isCustomBackgroundEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(LoomColors.line),
                )
            }
            FeedRow(item = item)
        }
    }
}

@Composable
private fun FeedRow(item: LoomFeedItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (item.onClick != null) {
                    Modifier.clickable(onClick = item.onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(item.tone.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.tone.content,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(13.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = LoomType.rowTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.desc.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.desc,
                    style = LoomType.rowDesc,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = item.value,
            style = LoomType.rowValue,
            color = item.valueColor,
            maxLines = 1,
        )
    }
}

/** 设置页 / 二级页使用的分类行 */
@Composable
fun LoomCategoryRow(
    title: String,
    desc: String,
    icon: ImageVector,
    tone: LoomTone,
    count: Int? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(tone.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(21.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = LoomColors.textBody)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                desc,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = LoomColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (count != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(LoomColors.canvasSubtle)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    color = LoomColors.textTer,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        Icon(
            imageVector = Icons.TwoTone.ChevronRight,
            contentDescription = null,
            tint = LoomColors.chevron,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** 进度条式状态胶囊（用于「安全 / 风险」等短标签） */
@Composable
fun LoomPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    container: Color = color.copy(alpha = 0.12f),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Black, color = color)
    }
}

/** 轻量淡入位移，供首屏元素依次入场 */
@Composable
fun loomRiseAlpha(visible: Boolean, delayMillis: Int = 0): Float {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 420,
            delayMillis = delayMillis,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "loomRise",
    )
    return alpha
}

/** 供页面复用的标准间距 */
val LoomPageGutter: Dp = LoomDimens.pageGutter

/**
 * 标题文字在深色画布上的自适应。
 *
 * 用户自选的主/次两段颜色通常按白底挑的（默认 #101322 / #71717A），
 * 直接放到纯黑背景上等于隐形。深色下把过暗的颜色往白里提，
 * [amount] 控制提亮幅度：主段提得多（接近纯白），次段提得少（保留层次）。
 */
private fun Int.adaptTitleInk(darkCanvas: Boolean, amount: Float): Color {
    val raw = Color(this)
    if (!darkCanvas || raw.luminance() >= 0.55f) return raw
    return lerp(raw, Color.White, amount)
}
