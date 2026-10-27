package me.weishu.kernelsu.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.ui.UiMode

/**
 * 界面风格选择器（组件设计稿 style-picker.html 的 Compose 实现）。
 *
 * 规格对齐：
 * - 面板 max 352dp、圆角 24、渐变底 + 紫罗兰顶部光晕 + hairline 描边
 * - 顶部 grab / kicker 8.5sp·字距 2.8 / 标题 18sp / 右上两行说明
 * - 三段明暗控件（深色 / 浅色 / 跟随系统），只联动面板本身
 * - 横滚皮肤卡 90×132·r14·padding 7，缩略图 82 高·r10，间距 7，左右内边距 15
 * - 选中：描边 violet 80% + 12% 底色 + 3px 环 + 勾选章 17 圆（延迟 0.06s 入场）
 * - 说明卡 min-height 52，点击卡片即写入 ui_mode（无需确认、无需重启）
 * - 主按钮 #C9F45C → #6FE9C4 渐变，次按钮幽灵描边
 */
private data class PickTokens(
    val g1: Color,
    val g2: Color,
    val border: Color,
    val glow: Color,
    val grab: Color,
    val kicker: Color,
    val title: Color,
    val caption: Color,
    val segBg: Color,
    val segBorder: Color,
    val segText: Color,
    val segOnBg: Color,
    val segOnText: Color,
    val segOnBorder: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val label: Color,
    val sub: Color,
    val status: Color,
    val statusOn: Color,
    val selBorder: Color,
    val selBg: Color,
    val selRing: Color,
    val infoBg: Color,
    val infoBorder: Color,
    val infoText: Color,
    val infoBold: Color,
    val hint: Color,
    val ghostText: Color,
    val ghostBorder: Color,
    val checkBg: Color,
    val checkText: Color,
)

private val PickDark = PickTokens(
    g1 = Color(0xFF171A25),
    g2 = Color(0xFF090B10),
    border = Color(0x578B7BFF),
    glow = Color(0x528B7BFF),
    grab = Color(0x29FFFFFF),
    kicker = Color(0xFF7B6FE8),
    title = Color(0xFFF3F4F8),
    caption = Color(0xFF5D6376),
    segBg = Color(0x0DFFFFFF),
    segBorder = Color(0x0FFFFFFF),
    segText = Color(0xFF7C8294),
    segOnBg = Color(0x338B7BFF),
    segOnText = Color(0xFFCFC7FF),
    segOnBorder = Color(0x598B7BFF),
    cardBg = Color(0x0AFFFFFF),
    cardBorder = Color(0x12FFFFFF),
    label = Color(0xFFE8EAF0),
    sub = Color(0xFF5D6376),
    status = Color(0xFF9C90FF),
    statusOn = Color(0xFF4DE2C5),
    selBorder = Color(0xCC8B7BFF),
    selBg = Color(0x1F8B7BFF),
    selRing = Color(0x1F8B7BFF),
    infoBg = Color(0x0AFFFFFF),
    infoBorder = Color(0x0FFFFFFF),
    infoText = Color(0xFF9BA0B2),
    infoBold = Color(0xFFE8EAF0),
    hint = Color(0xFF4E5466),
    ghostText = Color(0xFF9BA0B2),
    ghostBorder = Color(0x1FFFFFFF),
    checkBg = Color(0xFF4DE2C5),
    checkText = Color(0xFF08110E),
)

private val PickLight = PickTokens(
    g1 = Color(0xFAFFFFFF),
    g2 = Color(0xFCF5F6FB),
    border = Color(0x476A57F5),
    glow = Color(0x298B7BFF),
    grab = Color(0x240F142D),
    kicker = Color(0xFF6A57F5),
    title = Color(0xFF12141B),
    caption = Color(0xFF9AA0B0),
    segBg = Color(0xFFF1F2F8),
    segBorder = Color(0x120F142D),
    segText = Color(0xFF8A90A2),
    segOnBg = Color(0xFFFFFFFF),
    segOnText = Color(0xFF5B49E0),
    segOnBorder = Color(0x386A57F5),
    cardBg = Color(0xFFFFFFFF),
    cardBorder = Color(0x140F142D),
    label = Color(0xFF1A1C24),
    sub = Color(0xFF9AA0B0),
    status = Color(0xFF6A57F5),
    statusOn = Color(0xFF0FA98B),
    selBorder = Color(0xBF6A57F5),
    selBg = Color(0x0F6A57F5),
    selRing = Color(0x1A6A57F5),
    infoBg = Color(0xFFF7F8FC),
    infoBorder = Color(0x120F142D),
    infoText = Color(0xFF6A7080),
    infoBold = Color(0xFF1A1C24),
    hint = Color(0xFFA9AEBB),
    ghostText = Color(0xFF6A7080),
    ghostBorder = Color(0x1F0F142D),
    checkBg = Color(0xFF0FA98B),
    checkText = Color(0xFFFFFFFF),
)

/** 皮肤卡的缩略图配色（照设计稿 §5.1 的 preview 数据）。 */
private data class PickSkin(
    val mode: UiMode,
    val name: String,
    val tag: String,
    val desc: Int,
    val bg: Color,
    val ink: Color,
    val block: Color,
    val ring: Color,
    val accent: Color,
    val mint: Color,
)

private val PickSkins = listOf(
    PickSkin(
        mode = UiMode.Nocturne,
        name = "Nocturne",
        tag = "暗夜",
        desc = R.string.style_picker_desc_nocturne,
        bg = Color(0xFF0A0C12),
        ink = Color(0xFFFFFFFF),
        block = Color(0x388B7BFF),
        ring = Color(0x00000000),
        accent = Color(0xB38B7BFF),
        mint = Color(0x804DE2C5),
    ),
    PickSkin(
        mode = UiMode.Glass,
        name = "Daylight",
        tag = "亮色",
        desc = R.string.style_picker_desc_daylight,
        bg = Color(0xFFF4F5FA),
        ink = Color(0xFF0F142D),
        block = Color(0xFFFFFFFF),
        ring = Color(0x140F142D),
        accent = Color(0x736A57F5),
        mint = Color(0x730FA98B),
    ),
    PickSkin(
        mode = UiMode.Material,
        name = "Editorial",
        tag = "纸面",
        desc = R.string.style_picker_desc_editorial,
        bg = Color(0xFFF5F3ED),
        ink = Color(0xFF1D1C22),
        block = Color(0xFF1D1C22),
        ring = Color(0x00000000),
        accent = Color(0xFFE8502E),
        mint = Color(0xFFE8502E),
    ),
    PickSkin(
        mode = UiMode.Fresh,
        name = "Pages",
        tag = "极简",
        desc = R.string.style_picker_desc_pages,
        bg = Color(0xFFFFFFFF),
        ink = Color(0xFF111111),
        block = Color(0x14111111),
        ring = Color(0x00000000),
        accent = Color(0xFF2D6BFF),
        mint = Color(0xFF2D6BFF),
    ),
    PickSkin(
        mode = UiMode.Miuix,
        name = "Miuix",
        tag = "小米",
        desc = R.string.style_picker_desc_miuix,
        bg = Color(0xFFEFEDF8),
        ink = Color(0xFF1C1A2E),
        block = Color(0xFFFFFFFF),
        ring = Color(0x1A2D285A),
        accent = Color(0x8C6E56F8),
        mint = Color(0x8012B981),
    ),
)

@Composable
fun StylePickerDialog(
    current: UiMode,
    onDismiss: () -> Unit,
) {
    val repo = remember { SettingsRepositoryImpl() }
    val appDark = isAppDark()
    var panelMode by remember { mutableStateOf(if (appDark) 0 else 1) }
    val t = if (panelMode == 2) (if (appDark) PickDark else PickLight)
    else if (panelMode == 0) PickDark else PickLight

    var selected by remember { mutableStateOf(current) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val enter by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "picker-enter",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f * enter))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .widthIn(max = 352.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = (1f - enter) * 24.dp.toPx()
                        alpha = enter
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(t.g1, t.g2)))
                    .border(1.dp, t.border, RoundedCornerShape(24.dp))
                    .drawBehind {
                        // 顶部紫罗兰光晕：radial 360×130 at 50%,-35%
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(t.glow, Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(size.width / 2f, -size.height * 0.02f),
                                radius = size.width * 0.95f,
                            ),
                            size = androidx.compose.ui.geometry.Size(size.width, 160.dp.toPx()),
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(top = 8.dp, bottom = 16.dp),
            ) {
                // grab
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .width(30.dp)
                        .height(3.5.dp)
                        .clip(CircleShape)
                        .background(t.grab)
                        .align(Alignment.CenterHorizontally),
                )

                // 头部
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, end = 15.dp, top = 9.dp, bottom = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.style_picker_kicker),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.8.sp,
                            color = t.kicker,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.style_picker_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.4).sp,
                            color = t.title,
                        )
                    }
                    Text(
                        text = stringResource(R.string.style_picker_caption),
                        fontSize = 8.5.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        color = t.caption,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                // 明暗三段
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, end = 15.dp, bottom = 11.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(t.segBg)
                        .border(1.dp, t.segBorder, RoundedCornerShape(11.dp))
                        .padding(2.5.dp),
                ) {
                    listOf(
                        R.string.style_picker_dark,
                        R.string.style_picker_light,
                        R.string.style_picker_follow_system,
                    ).forEachIndexed { index, res ->
                        val on = panelMode == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (on) t.segOnBg else Color.Transparent)
                                .then(
                                    if (on) Modifier.border(1.dp, t.segOnBorder, RoundedCornerShape(9.dp))
                                    else Modifier
                                )
                                .clickable { panelMode = index }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(res),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp,
                                color = if (on) t.segOnText else t.segText,
                            )
                        }
                    }
                }

                // 皮肤卡
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 15.dp, end = 15.dp, top = 2.dp, bottom = 12.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(PickSkins, key = { it.mode.value }) { skin ->
                        SkinCard(
                            skin = skin,
                            selected = skin.mode == selected,
                            tokens = t,
                            onClick = {
                                selected = skin.mode
                                applyUiMode(skin.mode)
                                repo.uiMode = skin.mode.value
                            },
                        )
                    }
                }

                // 说明
                val descSkin = PickSkins.firstOrNull { it.mode == selected } ?: PickSkins.first()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(t.infoBg)
                        .border(1.dp, t.infoBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = stringResource(descSkin.desc),
                        fontSize = 10.sp,
                        lineHeight = 17.sp,
                        color = t.infoText,
                    )
                }

                Text(
                    text = stringResource(R.string.style_picker_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    textAlign = TextAlign.Center,
                    color = t.hint,
                )

                // 按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, end = 15.dp, top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.6f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFC9F45C), Color(0xFF6FE9C4)),
                                )
                            )
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.style_picker_done),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.6.sp,
                            color = Color(0xFF0B0D12),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, t.ghostBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                applyUiMode(UiMode.Fresh)
                                repo.uiMode = UiMode.Fresh.value
                                selected = UiMode.Fresh
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.style_picker_reset),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = t.ghostText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinCard(
    skin: PickSkin,
    selected: Boolean,
    tokens: PickTokens,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(140),
        label = "skin-press",
    )

    Box(
        modifier = Modifier
            .width(90.dp)
            .height(132.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) tokens.selBg else tokens.cardBg)
            .border(
                width = 1.dp,
                color = if (selected) tokens.selBorder else tokens.cardBorder,
                shape = RoundedCornerShape(14.dp),
            )
            .then(
                if (selected) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = tokens.selRing,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(17.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()),
                            topLeft = androidx.compose.ui.geometry.Offset(-3.dp.toPx(), -3.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(
                                size.width + 6.dp.toPx(),
                                size.height + 6.dp.toPx(),
                            ),
                        )
                    }
                } else Modifier
            )
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(7.dp),
    ) {
        Column {
            // 缩略图
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(skin.bg)
                    .padding(horizontal = 7.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.64f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(skin.accent),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(skin.block)
                        .border(1.dp, skin.ring, RoundedCornerShape(6.dp)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(skin.ink.copy(alpha = 0.14f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.70f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(skin.ink.copy(alpha = 0.09f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.40f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(skin.mint),
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = skin.name,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = tokens.label,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = skin.tag,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    color = tokens.sub,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    if (selected) R.string.style_picker_in_use else R.string.style_picker_switch
                ),
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                color = if (selected) tokens.statusOn else tokens.status,
            )
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(tokens.checkBg)
                    .alpha(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✓",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = tokens.checkText,
                )
            }
        }
    }
}

/** 当前 App 是否处于暗色（跟随系统时取系统值）。 */
@Composable
private fun isAppDark(): Boolean {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val repo = remember { SettingsRepositoryImpl() }
    val mode = me.weishu.kernelsu.ui.theme.ColorMode.fromValue(repo.themeMode)
    return if (mode.isSystem) dark else mode.isDark
}
