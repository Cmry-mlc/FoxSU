package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R

/**
 * 关于作者 —— 严格对照视觉稿 s6：
 * AUTHOR · 造物者 / 关于 + 返回/资料按钮 → acard（66 头像 + 虚线外环 + 名字 @handle +
 * mint 两行角色 + 徽章行）→ aquote 引言 → stat3 四列 → CONTACT · 联系 → PROJECTS · 作品。
 */
@Composable
fun NocturneAuthorScreen(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val t = ncTokens()

    Box(Modifier.fillMaxSize()) {
        NcGlowBackground(t)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = ncStatusTop() + 2.dp, bottom = 40.dp),
        ) {
            NcHeader(kicker = "AUTHOR · 造物者", title = "关于") {
                NcIconButton(iconRes = R.drawable.nc_back, onClick = onBack)
                NcIconButton(iconRes = R.drawable.nc_user, onClick = {}, primary = true)
            }

            // acard
            NcGradientBorder(radius = 24.dp) {
                Row(
                    Modifier.padding(horizontal = 17.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 66 头像 + 虚线外环
                    Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color(0x38FFFFFF),
                                radius = size.width / 2 - 3.dp.toPx(),
                                style = Stroke(
                                    width = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                                ),
                            )
                        }
                        Box(
                            Modifier
                                .size(66.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF8B7BFF), Color(0xFF6A57F5)))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("FOX", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Color.White)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("尘梦如烟", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp, color = t.text1)
                            Spacer(Modifier.width(7.dp))
                            Text("@foxsu", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = t.text2)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "FOXSU 维护者 ·\nINTERFACE DESIGNER",
                            fontSize = 9.sp,
                            letterSpacing = 1.8.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = t.mint,
                        )
                        Spacer(Modifier.height(9.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AuthorChip("MAINTAINER", t.violetText, t.violet.copy(alpha = 0.16f))
                            AuthorChip("GPL-3.0", t.text2)
                            AuthorChip("SINCE 2020", t.text2)
                        }
                    }
                }
            }

            // aquote
            Spacer(Modifier.height(14.dp))
            Row(Modifier.padding(horizontal = 14.dp)) {
                Box(Modifier.width(2.dp).height(46.dp).background(t.violet.copy(alpha = 0.55f)))
                Spacer(Modifier.width(10.dp))
                Text(
                    "感谢使用 FoxSu。这是基于 KernelSU 管理器深度定制的版本，为 KernelSU 管理器重新设计了界面风格。",
                    fontSize = 10.5.sp,
                    lineHeight = 18.sp,
                    color = t.text2,
                )
            }

            Spacer(Modifier.height(12.dp))
            NcStat3(
                listOf(
                    Triple("项目", "12", t.text1),
                    Triple("版本", BuildConfig.VERSION_NAME, t.violet),
                    Triple("年限", "6y", t.mint),
                )
            )

            NcSectionLabel("CONTACT · 联系")
            NcGroupCard {
                NcRow(
                    iconRes = R.drawable.nc_link,
                    title = "GitHub",
                    subtitle = "github.com/Cmry-mlc/FoxSu",
                    subtitleMono = true,
                    accent = NcAccent.Violet,
                    onClick = { onOpenUrl("https://github.com/Cmry-mlc/FoxSu") },
                    trailing = { NcChevron() },
                )
                NcRow(
                    iconRes = R.drawable.nc_sliders,
                    title = "Telegram",
                    subtitle = "@foxsu_dev",
                    subtitleMono = true,
                    accent = NcAccent.Default,
                    showDivider = true,
                    onClick = { onOpenUrl("https://t.me/foxsu_dev") },
                    trailing = { NcChevron() },
                )
                NcRow(
                    iconRes = R.drawable.nc_shield,
                    title = "电子邮箱",
                    subtitle = "fox@foxsu.dev",
                    subtitleMono = true,
                    accent = NcAccent.Mint,
                    showDivider = true,
                    onClick = { onOpenUrl("mailto:fox@foxsu.dev") },
                    trailing = { NcChevron() },
                )
            }

            NcSectionLabel("PROJECTS · 作品")
            NcGroupCard {
                NcRow(
                    iconRes = R.drawable.nc_power,
                    title = "FoxSu Manager",
                    subtitle = "内核级 Root 管理器 · 主项目",
                    accent = NcAccent.Violet,
                    onClick = { onOpenUrl("https://github.com/Cmry-mlc/FoxSu") },
                    trailing = { Text("★ 32.8k", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = t.violet) },
                )
                NcRow(
                    iconRes = R.drawable.nc_palette,
                    title = "Nocturne Theme",
                    subtitle = "暗夜主题引擎 · 本界面",
                    accent = NcAccent.Mint,
                    showDivider = true,
                    onClick = { onOpenUrl("https://github.com/Cmry-mlc/FoxSu") },
                    trailing = { Text("★ 6.1k", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = t.mint) },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AuthorChip(text: String, color: Color, bg: Color = Color(0x0FFFFFFF)) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.9.sp, color = color)
    }
}
