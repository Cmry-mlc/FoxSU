package com.kuaimaosu.manager.ui.component

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.KeyboardArrowLeft
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.renderBackgroundBlur
import com.kuaimaosu.manager.ui.theme.KuaiMaoDimens
import com.kuaimaosu.manager.ui.theme.KuaiMaoType
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import org.koin.compose.koinInject

/**
 * KuaiMaoSU 通用控件（二级页 / 表单页）。
 *
 * 与设计交付包 css/components.css 对应：
 * `.lx-back` 返回胶囊、`.lx-card` 卡片、`.lx-row` 行、`.lx-btn--primary` 主按钮。
 */

/** 返回胶囊：白底、品牌字、左侧细箭头 */
@Composable
fun KuaiMaoBackCapsule(
    text: String = "返回",
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(100.dp), clip = false, ambientColor = Color(0x1418263C), spotColor = Color(0x1418263C))
            .clip(RoundedCornerShape(100.dp))
            .background(KuaiMaoColors.canvasCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.TwoTone.KeyboardArrowLeft,
            contentDescription = null,
            tint = KuaiMaoColors.brand,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = KuaiMaoColors.brand,
        )
    }
}

/** 二级页标题区：24sp 大标题 + 11sp 副标题 */
@Composable
fun KuaiMaoPageTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = title, style = KuaiMaoType.pageTitle)
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = KuaiMaoType.pageSub)
        }
    }
}

/**
 * 白色卡片容器。
 *
 * 卡片底取 [KuaiMaoColors.canvasCard]：开了自定义背景时它是半透明的，
 * 再叠一层 [renderBackgroundBlur] 把壁纸的模糊版画在卡片范围内——
 * 这就是源码里「自定义背景 + 卡片模糊」的效果，壁纸透过卡片但字不受影响。
 */
@Composable
fun KuaiMaoCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, KuaiMaoDimens.rCard, clip = false, ambientColor = Color(0x0F18263C), spotColor = Color(0x1418263C))
            .clip(KuaiMaoDimens.rCard)
            .background(KuaiMaoColors.canvasCard)
            .renderBackgroundBlur(tintColor = KuaiMaoColors.canvasCardSolid)
            .padding(contentPadding),
        content = content,
    )
}

/** 列表行：图标块 + 标题/描述 + 尾部内容 */
@Composable
fun KuaiMaoRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    // 默认不写死颜色：统一走 KuaiMaoTones（跟随主题色 + 保证对比度）。
    // 只有需要语义色（危险红等）时才显式覆盖。
    iconTint: Color? = null,
    iconContainer: Color? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val alpha = if (enabled) 1f else 0.45f
    val tone = KuaiMaoTones.unified
    val resolvedTint = iconTint ?: tone.content
    val resolvedContainer = iconContainer ?: tone.container
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(resolvedContainer.copy(alpha = alpha * resolvedContainer.alpha)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedTint.copy(alpha = alpha),
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(13.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = KuaiMaoColors.textBody.copy(alpha = alpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp,
                    color = KuaiMaoColors.textMuted.copy(alpha = alpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(10.dp))
            trailing()
        } else if (selected) {
            Spacer(modifier = Modifier.width(10.dp))
            KuaiMaoCheckMark(checked = true, enabled = enabled)
        }
    }
}

/** 行分隔线 */
@Composable
fun KuaiMaoRowDivider(modifier: Modifier = Modifier) {
    // 自定义壁纸下不画分隔线：一条浅色发丝线压在花丛上会变成一根根「小白条」，
    // 原版在这种背景里也是靠行距分组、不画线。关掉壁纸就恢复。
    val cardConfig: CardConfig = koinInject()
    if (cardConfig.isCustomBackgroundEnabled) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(KuaiMaoColors.line),
    )
}

/** 勾选标记：选中为品牌渐变方块 + 白勾，未选中为浅灰描边 */
@Composable
fun KuaiMaoCheckMark(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.45f
    // 夜间强调色接近白色：白底白勾会消失，改用中性灰底 + 白勾
    val bright = KuaiMaoColors.brand.luminance() > 0.72f
    if (checked) {
        Box(
            modifier = modifier
                .size(21.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(
                    Brush.linearGradient(
                        colors = if (bright) {
                            listOf(Color(0xFF6E6E73), Color(0xFF8E8E93))
                        } else {
                            listOf(KuaiMaoColors.brand, KuaiMaoColors.brandDeep)
                        },
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                )
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.TwoTone.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(21.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(KuaiMaoColors.canvasSubtle.copy(alpha = alpha)),
        )
    }
}

/** 单选圆点 */
@Composable
fun KuaiMaoRadioMark(
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.45f
    Box(
        modifier = modifier
            .size(21.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
                if (selected) {
                    (if (KuaiMaoColors.brand.luminance() > 0.72f) Color(0xFF8E8E93) else KuaiMaoColors.brand)
                        .copy(alpha = alpha)
                } else {
                    KuaiMaoColors.canvasSubtle.copy(alpha = alpha)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White),
            )
        }
    }
}

/**
 * 主按钮：16dp 圆角、50dp 高。
 * 未自选主题色时保持设计稿的深靛渐变；选了主题色后整块跟随品牌色，
 * 这样模块页等处的「安装模块」按钮也会一起换色。
 */
@Composable
fun KuaiMaoPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    // 主按钮永远跟随全局主题色：不再有「未开启自选色就用深蓝」的分支，
    // 否则改主题色时安装页 / LKM 修补按钮会一直是深蓝。
    val shadowTint = KuaiMaoColors.brand
    // 夜间主按钮：强调色是白的，白底白字看不见，改成浅灰底 + 深色字
    val bright = KuaiMaoColors.brand.luminance() > 0.72f
    val labelInk = if (bright) Color(0xFF101013) else Color.White
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(
                elevation = if (enabled) 14.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false,
                ambientColor = shadowTint.copy(alpha = 0.20f),
                spotColor = shadowTint.copy(alpha = 0.26f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) {
                    Brush.linearGradient(
                        colors = if (bright) {
                            listOf(Color(0xFFE9E9EE), Color(0xFFF7F7FA))
                        } else {
                            listOf(KuaiMaoColors.brand, KuaiMaoColors.brandDeep)
                        },
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFC9CDDA), Color(0xFFD6D9E4)),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    )
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = if (enabled) labelInk else labelInk.copy(alpha = 0.9f),
        )
    }
}

/** 次按钮：白底、浅阴影 */
@Composable
fun KuaiMaoGhostButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = Color(0x0F18263C), spotColor = Color(0x1418263C))
            .clip(RoundedCornerShape(16.dp))
            .background(KuaiMaoColors.canvasCard)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Black,
            color = KuaiMaoColors.textLabel,
        )
    }
}

/** 分段切换：白卡内一枚滑动胶囊 */
@Composable
fun KuaiMaoSegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100.dp))
            .background(KuaiMaoColors.canvasCard)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        if (selected) {
                            // 跟随全局主题色；夜间强调色是白的，选中块改用深灰胶囊 + 白字
                            Brush.linearGradient(
                                colors = if (KuaiMaoColors.brand.luminance() > 0.72f) {
                                    listOf(Color(0xFF4A4A4F), Color(0xFF35353A))
                                } else {
                                    listOf(KuaiMaoColors.brand, KuaiMaoColors.brandDeep)
                                },
                                start = Offset.Zero,
                                end = Offset.Infinite,
                            )
                        } else {
                            SolidColor(Color.Transparent)
                        },
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (selected) Color.White else KuaiMaoColors.textSec,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * 设计稿开关：44×26 胶囊，开启为品牌渐变 + 白色滑块右移，关闭为浅灰 + 滑块左移。
 */
@Composable
fun KuaiMaoSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    val alpha = if (enabled) 1f else 0.5f
    val trackWidth = 44.dp
    val thumb = 20.dp
    val padding = 3.dp
    // 夜间强调色是白色：直接用白轨道 + 白滑块会糊成一整块，所以亮度太高时
    // 把轨道换成中性灰，滑块仍是白色，选中状态一眼能看出来。
    val brightAccent = KuaiMaoColors.brand.luminance() > 0.72f
    val trackStart = if (brightAccent) Color(0xFF6E6E73) else KuaiMaoColors.brand
    val trackEnd = if (brightAccent) Color(0xFF8E8E93) else KuaiMaoColors.brandDeep
    val checkInk = if (brightAccent) Color(0xFF3A3A3E) else Color(0xFF4F5BE8)
    val offset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumb - padding * 2 else 0.dp,
        animationSpec = tween(durationMillis = 180),
        label = "kuaimaoSwitchThumb",
    )
    Box(
        modifier = modifier
            .size(width = trackWidth, height = 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(
                if (checked) {
                    Brush.linearGradient(
                        colors = listOf(
                            trackStart.copy(alpha = alpha),
                            trackEnd.copy(alpha = alpha),
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    )
                } else {
                    SolidColor(KuaiMaoColors.canvasSubtle.copy(alpha = alpha))
                },
            )
            .then(
                if (onCheckedChange != null && enabled) {
                    Modifier.clickable { onCheckedChange(!checked) }
                } else {
                    Modifier
                },
            )
            .padding(padding),
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset)
                .size(thumb)
                .shadow(2.dp, RoundedCornerShape(10.dp), clip = false)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = alpha)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.TwoTone.Check,
                    contentDescription = null,
                    // 白色滑块上固定用深一些的强调色，避免暗色主题下对比度不足
                    tint = checkInk,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

/** 输入框：浅底、14dp 圆角、无描边（对应设计稿 .fld）。 */
@Composable
fun KuaiMaoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KuaiMaoColors.canvasField)
            // 和卡片同一套磨砂：壁纸下的搜索框不能是块实心砖
            .renderBackgroundBlur(tintColor = KuaiMaoColors.canvasFieldSolid)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = KuaiMaoColors.textMuted,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = TextStyle(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = KuaiMaoColors.textBody,
            ),
            cursorBrush = SolidColor(KuaiMaoColors.brand),
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 字段小标题（对应设计稿 .fldlab）。 */
@Composable
fun KuaiMaoFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.6.sp,
        color = KuaiMaoColors.textMuted,
        modifier = modifier.padding(top = 14.dp, bottom = 7.dp),
    )
}


/** 页面右上角的白色方形图标按钮（40dp · 14dp 圆角）。 */
@Composable
fun KuaiMaoSquareIconButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp), clip = false, ambientColor = Color(0x1218263C), spotColor = Color(0x1618263C))
            .clip(RoundedCornerShape(14.dp))
            .background(KuaiMaoColors.canvasCard)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = KuaiMaoColors.textSec,
            modifier = Modifier.size(19.dp),
        )
    }
}

/**
 * 强调色上的前景色：深色强调色配白字，浅色强调色（暗色模式下的主色）配深字。
 * 选中态统一走这个函数，避免出现「浅底白字」看不清的组合。
 */
@Composable
fun onAccentContent(): Color {
    val primary = MaterialTheme.colorScheme.primary
    return if (primary.luminance() < 0.55f) Color.White else Color(0xFF101318)
}

/** 圆角胶囊选择组（默认 / 模版 / 自定义），选中为实心品牌底 + 白字。 */
@Composable
fun KuaiMaoChips(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else KuaiMaoColors.canvasSubtle)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 18.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 11.5.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                    color = if (selected) onAccentContent() else KuaiMaoColors.textSec,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 浮层菜单面板：白卡 + 20dp 圆角 + 柔和投影。 */
@Composable
fun KuaiMaoMenuPanel(
    modifier: Modifier = Modifier,
    width: Dp = 196.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .width(width)
            .shadow(20.dp, RoundedCornerShape(20.dp), clip = false, ambientColor = Color(0x33101828), spotColor = Color(0x3D101828))
            .clip(RoundedCornerShape(20.dp))
            // 菜单是浮层：必须压住背后的壁纸和特效，这里不能用会变半透明的 canvasCard
            .background(KuaiMaoColors.canvasCardSolid)
            // 白底页面上的白菜单，没有描边就分不清边界（看起来像内容叠在一起）。
            .border(1.dp, KuaiMaoColors.line.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
            .padding(vertical = 6.dp),
        content = content,
    )
}

/** 菜单项：13sp，选中态浅紫底 + 对勾。 */
@Composable
fun KuaiMaoMenuItem(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    showCheck: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            color = if (selected) onAccentContent() else KuaiMaoColors.textLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected && showCheck) {
            Icon(
                imageVector = Icons.TwoTone.Check,
                contentDescription = null,
                tint = onAccentContent(),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** 菜单分组小标题。 */
@Composable
fun KuaiMaoMenuLabel(text: String) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.2.sp,
        color = KuaiMaoColors.textMuted,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}
