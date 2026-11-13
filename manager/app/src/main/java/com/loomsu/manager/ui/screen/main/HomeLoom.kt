package com.loomsu.manager.ui.screen.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loomsu.manager.ui.screen.splash.LoomMark
import com.loomsu.manager.ui.theme.LoomColors
import com.loomsu.manager.ui.theme.LoomDimens
import com.loomsu.manager.ui.theme.LoomGradients
import com.loomsu.manager.ui.theme.LoomTone
import com.loomsu.manager.ui.theme.LoomType

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
            Text(text = "WELCOME BACK", style = LoomType.kicker.copy(letterSpacing = 2.sp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Loom SU", style = LoomType.greeting)
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(6.dp, RoundedCornerShape(13.dp), clip = false)
                .clip(RoundedCornerShape(13.dp))
                .background(LoomColors.canvasCard)
                .clickable(onClick = onBellClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.TwoTone.Notifications,
                contentDescription = "通知",
                tint = LoomColors.textLabel,
                modifier = Modifier.size(19.dp),
            )
            // 未读角标：有消息才出现，数字为未读条数
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .defaultMinSize(minWidth = 14.dp, minHeight = 14.dp)
                        .clip(CircleShape)
                        .background(LoomColors.danger)
                        .padding(horizontal = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
        }
    }
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = Color(0x3318203C),
                spotColor = Color(0x5518203C),
            )
            .clip(shape)
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF181D33), Color(0xFF232C56), Color(0xFF1B2440)),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.8f, size.height),
                    ),
                )
                // 右上紫光斑
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x806C5CF0), Color.Transparent),
                        center = Offset(size.width * 0.84f, -size.height * 0.06f),
                        radius = size.width * 0.62f,
                    ),
                    radius = size.width * 0.62f,
                    center = Offset(size.width * 0.84f, -size.height * 0.06f),
                )
                // 左下余光
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x571CA672), Color.Transparent),
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
            drawPath(p1, Color(0x668CA0FF), style = Stroke(width = 1.4f))
            drawPath(p2, Color(0x388CA0FF), style = Stroke(width = 1.2f))
            drawCircle(Color(0xFF8CA0FF), radius = 3.5f, center = Offset(w * 0.94f, h * 0.42f))
        }

        Column(
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 0.dp),
        ) {
            Text(
                text = label,
                style = LoomType.heroLabel,
                color = Color(0xFFBEC8E6).copy(alpha = 0.82f),
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
                        color = Color.White,
                        maxLines = 1,
                    )
                    if (stateHint.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stateHint,
                            style = LoomType.heroHint,
                            color = Color(0xFFBECDEF).copy(alpha = 0.72f),
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
                                .background(Color.White.copy(alpha = 0.10f))
                                .drawBehind {
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.16f),
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
                                color = Color(0xFFD8DEF5),
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
                    .background(Color.White.copy(alpha = 0.12f)),
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
                    dotColor = Color(0xFF7BEFC0),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                        .align(Alignment.CenterVertically),
                )
                HeroMetric(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 18.dp),
                    label = moduleLabel,
                    value = moduleCount,
                    unit = moduleUnit,
                    dotColor = Color(0xFFFF8A8F),
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
                color = Color(0xCCBECDE0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value.toString(),
                style = LoomType.heroMetricValue,
                color = Color.White,
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = unit,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0x99BECDE0),
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
    Column(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(20.dp), clip = false, ambientColor = Color(0x1418262C), spotColor = Color(0x1A18263C))
            .clip(RoundedCornerShape(20.dp))
            .background(LoomColors.canvasCard)
            .clickable(onClick = action.onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(action.tone.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = action.tone.content,
                modifier = Modifier.size(20.dp),
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, LoomDimens.rCard, clip = false, ambientColor = Color(0x0D18263C), spotColor = Color(0x0D18263C))
            .clip(LoomDimens.rCard)
            .background(LoomColors.canvasCard)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
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
