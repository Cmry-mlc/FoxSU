package me.weishu.kernelsu.ui.screen.module

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.ui.theme.isInDarkTheme

/**
 * 模块卡片点击后的操作面板。
 *
 * 卡片本身原来只有「展开描述」这一种反馈，点了等于没反应；这里把模块的可执行动作集中到
 * 一张面板上：主按钮「执行」跑模块自带的 action.sh，次按钮「取消」直接关掉。
 * 模块没带 action.sh（或处于停用 / 待更新 / 待卸载）时「执行」置灰并说明原因。
 */
@Composable
fun ModuleActionSheet(
    module: Module?,
    onDismiss: () -> Unit,
    onExecute: (Module) -> Unit,
) {
    if (module == null) return

    val dark = isInDarkTheme()
    val sheetBg = if (dark) Color(0xFF1B1C20) else Color(0xFFF7F6FA)
    val blockBg = if (dark) Color(0xFF26272C) else Color(0xFFFFFFFF)
    val fg = if (dark) Color(0xFFF2F1F6) else Color(0xFF1B1B22)
    val fgDim = if (dark) Color(0xFF9C9AA8) else Color(0xFF6B6879)
    val accent = if (dark) Color(0xFFB9AEFF) else Color(0xFF5B4FD6)
    val accentBg = if (dark) Color(0xFF3B3560) else Color(0xFF6C5FC7)
    val disabledBg = if (dark) Color(0xFF2E2F35) else Color(0xFFE7E5EF)
    val disabledFg = if (dark) Color(0xFF6E6C7A) else Color(0xFFA9A6B8)

    val runnable = module.hasActionScript && module.enabled && !module.remove && !module.update

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(sheetBg)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    )
                    .padding(20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(fgDim.copy(alpha = 0.35f)),
                )

                Spacer(Modifier.height(18.dp))

                // 模块身份
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF8B7CF6), Color(0xFF5B4FD6))
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_d_grid),
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = Color.White,
                        )
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = module.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = fg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "v${module.version}",
                                fontSize = 11.sp,
                                color = fgDim,
                                maxLines = 1,
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (module.enabled) {
                                            Color(0xFF2FA36B).copy(alpha = if (dark) 0.22f else 0.12f)
                                        } else {
                                            fgDim.copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = stringResource(
                                        if (module.enabled) R.string.module_enabled else R.string.module_disabled
                                    ),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (module.enabled) Color(0xFF2FA36B) else fgDim,
                                )
                            }
                        }
                    }
                }

                if (module.description.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(blockBg)
                            .padding(horizontal = 13.dp, vertical = 11.dp),
                    ) {
                        Text(
                            text = module.description,
                            fontSize = 11.5.sp,
                            lineHeight = 18.sp,
                            color = fgDim,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // 不可执行时的原因
                if (!runnable) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(
                            when {
                                !module.hasActionScript -> R.string.module_action_sheet_no_action
                                module.remove -> R.string.module_action_sheet_removing
                                !module.enabled -> R.string.module_action_sheet_disabled
                                else -> R.string.module_action_sheet_pending
                            }
                        ),
                        fontSize = 10.5.sp,
                        lineHeight = 16.sp,
                        color = fgDim,
                    )
                }

                Spacer(Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 取消
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(blockBg)
                            .border(1.dp, fgDim.copy(alpha = 0.18f), RoundedCornerShape(15.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.module_action_cancel),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = fg,
                        )
                    }
                    // 执行
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(if (runnable) accentBg else disabledBg)
                            .then(
                                if (runnable) {
                                    Modifier.clickable {
                                        onDismiss()
                                        onExecute(module)
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.module_action_sheet_run),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (runnable) Color.White else disabledFg,
                        )
                    }
                }
            }
        }
    }
}
