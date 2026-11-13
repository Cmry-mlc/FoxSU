package com.loomsu.manager.ui.component

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loomsu.manager.ui.theme.LoomColors
import com.loomsu.manager.ui.theme.LoomDimens
import com.loomsu.manager.ui.theme.LoomType

/**
 * Loom SU 通用控件（二级页 / 表单页）。
 *
 * 与设计交付包 css/components.css 对应：
 * `.lx-back` 返回胶囊、`.lx-card` 卡片、`.lx-row` 行、`.lx-btn--primary` 主按钮。
 */

/** 返回胶囊：白底、品牌字、左侧细箭头 */
@Composable
fun LoomBackCapsule(
    text: String = "返回",
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(100.dp), clip = false, ambientColor = Color(0x1418263C), spotColor = Color(0x1418263C))
            .clip(RoundedCornerShape(100.dp))
            .background(LoomColors.canvasCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.TwoTone.KeyboardArrowLeft,
            contentDescription = null,
            tint = LoomColors.brand,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = LoomColors.brand,
        )
    }
}

/** 二级页标题区：24sp 大标题 + 11sp 副标题 */
@Composable
fun LoomPageTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = title, style = LoomType.pageTitle)
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = LoomType.pageSub)
        }
    }
}

/** 白色卡片容器 */
@Composable
fun LoomCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, LoomDimens.rCard, clip = false, ambientColor = Color(0x0F18263C), spotColor = Color(0x1418263C))
            .clip(LoomDimens.rCard)
            .background(LoomColors.canvasCard)
            .padding(contentPadding),
        content = content,
    )
}

/** 列表行：图标块 + 标题/描述 + 尾部内容 */
@Composable
fun LoomRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = LoomColors.violet,
    iconContainer: Color = LoomColors.violetSoft,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val alpha = if (enabled) 1f else 0.45f
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
                    .background(iconContainer.copy(alpha = alpha)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint.copy(alpha = alpha),
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
                color = LoomColors.textBody.copy(alpha = alpha),
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
                    color = LoomColors.textMuted.copy(alpha = alpha),
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
            LoomCheckMark(checked = true, enabled = enabled)
        }
    }
}

/** 行分隔线 */
@Composable
fun LoomRowDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LoomColors.line),
    )
}

/** 勾选标记：选中为品牌渐变方块 + 白勾，未选中为浅灰描边 */
@Composable
fun LoomCheckMark(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.45f
    if (checked) {
        Box(
            modifier = modifier
                .size(21.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(LoomColors.brand, LoomColors.brandDeep),
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
                .background(LoomColors.canvasSubtle.copy(alpha = alpha)),
        )
    }
}

/** 单选圆点 */
@Composable
fun LoomRadioMark(
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
                if (selected) LoomColors.brand.copy(alpha = alpha) else LoomColors.canvasSubtle.copy(alpha = alpha),
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

/** 主按钮：深靛渐变、16dp 圆角、48dp 高 */
@Composable
fun LoomPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(
                elevation = if (enabled) 14.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false,
                ambientColor = Color(0x33222945),
                spotColor = Color(0x33222945),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF222945), Color(0xFF333D6E)),
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
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.9f),
        )
    }
}

/** 次按钮：白底、浅阴影 */
@Composable
fun LoomGhostButton(
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
            .background(LoomColors.canvasCard)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Black,
            color = LoomColors.textLabel,
        )
    }
}

/** 分段切换：白卡内一枚滑动胶囊 */
@Composable
fun LoomSegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100.dp))
            .background(LoomColors.canvasCard)
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
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF222945), Color(0xFF333D6E)),
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
                    color = if (selected) Color.White else LoomColors.textSec,
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
fun LoomSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    val alpha = if (enabled) 1f else 0.5f
    val trackWidth = 44.dp
    val thumb = 20.dp
    val padding = 3.dp
    val offset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumb - padding * 2 else 0.dp,
        animationSpec = tween(durationMillis = 180),
        label = "loomSwitchThumb",
    )
    Box(
        modifier = modifier
            .size(width = trackWidth, height = 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(
                if (checked) {
                    Brush.linearGradient(
                        colors = listOf(
                            LoomColors.brand.copy(alpha = alpha),
                            LoomColors.brandDeep.copy(alpha = alpha),
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    )
                } else {
                    SolidColor(LoomColors.canvasSubtle.copy(alpha = alpha))
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
                    // 白色滑块上固定用深一些的品牌色，避免暗色主题下对比度不足
                    tint = Color(0xFF4F5BE8),
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

/** 输入框：浅底、14dp 圆角、无描边（对应设计稿 .fld）。 */
@Composable
fun LoomTextField(
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
            .background(LoomColors.canvasField)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = LoomColors.textMuted,
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
                color = LoomColors.textBody,
            ),
            cursorBrush = SolidColor(LoomColors.brand),
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
fun LoomFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.6.sp,
        color = LoomColors.textMuted,
        modifier = modifier.padding(top = 14.dp, bottom = 7.dp),
    )
}


/** 页面右上角的白色方形图标按钮（40dp · 14dp 圆角）。 */
@Composable
fun LoomSquareIconButton(
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
            .background(LoomColors.canvasCard)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = LoomColors.textSec,
            modifier = Modifier.size(19.dp),
        )
    }
}

/** 圆角胶囊选择组（默认 / 模版 / 自定义），选中为浅紫底 + 品牌字。 */
@Composable
fun LoomChips(
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
                    .background(if (selected) LoomColors.brandSoft else LoomColors.canvasSubtle)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 18.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 11.5.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                    color = if (selected) LoomColors.brandDeep else LoomColors.textSec,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 浮层菜单面板：白卡 + 20dp 圆角 + 柔和投影。 */
@Composable
fun LoomMenuPanel(
    modifier: Modifier = Modifier,
    width: Dp = 196.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .width(width)
            .shadow(18.dp, RoundedCornerShape(20.dp), clip = false, ambientColor = Color(0x24101828), spotColor = Color(0x2E101828))
            .clip(RoundedCornerShape(20.dp))
            .background(LoomColors.canvasCard)
            .padding(vertical = 6.dp),
        content = content,
    )
}

/** 菜单项：13sp，选中态浅紫底 + 对勾。 */
@Composable
fun LoomMenuItem(
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
            .background(if (selected) LoomColors.brandSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            color = if (selected) LoomColors.brandDeep else LoomColors.textLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected && showCheck) {
            Icon(
                imageVector = Icons.TwoTone.Check,
                contentDescription = null,
                tint = LoomColors.brandDeep,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** 菜单分组小标题。 */
@Composable
fun LoomMenuLabel(text: String) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.2.sp,
        color = LoomColors.textMuted,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}
