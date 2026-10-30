package me.weishu.kernelsu.ui.screen.ag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import me.weishu.kernelsu.ui.component.bottombar.useSideDock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 支持 FoxSu —— 捐赠页。
 *
 * 这一页刻意不写「请赞助我」：先把「你已经在用的东西到底值多少」摆在最上面
 * （皮肤 / 底栏造型 / 广告数 0），再列出钱会花在哪、以及不花钱能做什么。
 *
 * 支付通道还没有接（后续走爱发电 / 微信），所以所有付款入口都如实标成
 * 「接入中」，按下去只提示、不假装成功——不做那种点了没反应的假按钮。
 */
@Composable
fun AgDonate(
    onBack: () -> Unit,
    onOpenAuthor: () -> Unit = {},
) {
    val context = LocalContext.current
    var tier by remember { mutableIntStateOf(1) }
    var customOpen by remember { mutableStateOf(false) }
    var customAmount by remember { mutableStateOf("") }

    val tiers = listOf(18, 38, 68, 128)
    val amount = if (tier >= 0) tiers[tier].toString() else customAmount.ifBlank { "—" }

    fun notReady() {
        Toast.makeText(context, "支付通道正在接入，暂未开放付款", Toast.LENGTH_SHORT).show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .agPageBg()
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Ag.headSurface())
                .agHeadBlur()
                .padding(start = if (useSideDock()) 106.dp else 18.dp, end = 18.dp)
                .padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AgIconBtn(AgTopIcons.Back, onClick = onBack, tint = Ag.Tx)
            Spacer(Modifier.weight(1f))
            AgIconBtn(AgTopIcons.User, onClick = onOpenAuthor, tint = Ag.Tx)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { DonateHero() }

            item {
                AgGHead("选择金额")
                TierGrid(
                    tiers = tiers,
                    tier = tier,
                    customAmount = customAmount,
                    onPick = { tier = it },
                    onCustom = { customOpen = true },
                )
            }

            item {
                AgGHead("付款方式")
                AgCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    PayRow("微信支付", "扫码 / 公众号赞赏", Color(0xFF07C160), onOpenAuthor)
                    PayRow("支付宝", "转账或收款码", Color(0xFF1677FF), onOpenAuthor)
                    PayRow("爱发电", "按月赞助，可随时取消", Color(0xFF946CE6), onOpenAuthor, last = true)
                }
                Spacer(Modifier.height(12.dp))
                NotReadyButton(amount) { notReady() }
                Spacer(Modifier.height(8.dp))
                Text(
                    "通道接通之前，这里不会收款。想先支持的话，页面底部的几种方式现在就能用。",
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    color = Ag.Tx3,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            item {
                AgGHead("这笔钱用在哪儿")
                AgCard(contentPadding = PaddingValues(4.dp)) {
                    MoneyRow(
                        icon = AgSettingsIcons.Shield,
                        tint = Ag.Vio,
                        title = "域名、CDN 与构建机",
                        desc = "下载服务、模块仓库镜像、每次发版的 CI 时长",
                    )
                    MoneyRow(
                        icon = AgSettingsIcons.Modules,
                        tint = Ag.Teal,
                        title = "真机回归",
                        desc = "不同 KMI、不同厂商的机器，用来验证内核与模块",
                    )
                    MoneyRow(
                        icon = AgSettingsIcons.Style,
                        tint = Ag.Amber,
                        title = "回馈上游",
                        desc = "FoxSu 站在 KernelSU、Miuix、Magic Mount-rs 之上",
                        last = true,
                    )
                }
            }

            item {
                AgGHead("不花钱也能支持")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FreeTile(Icons.Rounded.Star, "Star 项目", Modifier.weight(1f)) {
                        Toast.makeText(context, "仓库地址：github.com/FoxSu", Toast.LENGTH_SHORT).show()
                    }
                    FreeTile(Icons.Rounded.Share, "推荐给朋友", Modifier.weight(1f)) {
                        Toast.makeText(context, "截图分享给需要的人就很够了", Toast.LENGTH_SHORT).show()
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FreeTile(Icons.Rounded.BugReport, "报告问题", Modifier.weight(1f)) {
                        Toast.makeText(context, "带上日志提 Issue，比捐款更有用", Toast.LENGTH_SHORT).show()
                    }
                    FreeTile(Icons.Rounded.Code, "参与开发", Modifier.weight(1f)) { onOpenAuthor() }
                }
            }

            item { DonateFooter() }
        }
    }

    if (customOpen) {
        CustomAmountDialog(
            initial = customAmount,
            onDismiss = { customOpen = false },
            onConfirm = {
                customAmount = it
                tier = -1
                customOpen = false
            },
        )
    }
}

/** 顶部品牌卡：渐变 + 光斑，数字是这一版真实具备的能力，不是占位。 */
@Composable
private fun DonateHero() {
    val accent = Ag.accent
    Box(
        Modifier
            .fillMaxWidth()
            .height(206.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(accent.primary, accent.mid, accent.secondary)))
            .drawBehind {
                // 三个错位的白色光斑：整块渐变不至于是一条平涂的色带
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f),
                    radius = size.width * 0.42f,
                    center = Offset(size.width * 0.88f, size.height * 0.05f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = size.width * 0.26f,
                    center = Offset(size.width * 0.14f, size.height * 0.92f),
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.06f),
                    radius = size.width * 0.34f,
                    center = Offset(size.width * 0.62f, size.height * 1.06f),
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("GPL-3.0 · 开源", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("无广告", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                "支持 FoxSu",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.8).sp,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "免费、开源、无内购，也不上传任何数据。\n如果它让你的手机好用了一点，可以考虑支持一下。",
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = Color.White.copy(alpha = 0.88f),
            )

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeroStat("5", "套皮肤")
                HeroDivider()
                HeroStat("10", "种底栏")
                HeroDivider()
                HeroStat("0", "条广告")
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 2.dp))
    }
}

@Composable
private fun HeroDivider() {
    Box(
        Modifier
            .padding(horizontal = 12.dp)
            .size(width = 1.dp, height = 12.dp)
            .background(Color.White.copy(alpha = 0.3f)),
    )
}

/** 金额：4 档 + 自定义；选中用渐变描边，不选中只留发丝线。 */
@Composable
private fun TierGrid(
    tiers: List<Int>,
    tier: Int,
    customAmount: String,
    onPick: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    val accent = Ag.accent
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiers.chunked(2).forEachIndexed { rowIndex, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { colIndex, value ->
                    val index = rowIndex * 2 + colIndex
                    val selected = tier == index
                    Column(
                        Modifier
                            .weight(1f)
                            .height(74.dp)
                            .agClick { onPick(index) }
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selected) accent.primary.copy(alpha = 0.075f) else Ag.Card)
                            .border(
                                if (selected) 1.6.dp else 1.dp,
                                if (selected) accent.primary.copy(alpha = 0.55f) else Ag.Line,
                                RoundedCornerShape(18.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("¥", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) accent.primary else Ag.Tx3)
                            Spacer(Modifier.width(2.dp))
                            Text(
                                value.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                color = if (selected) accent.primary else Ag.Tx,
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            when (index) {
                                0 -> "一瓶饮料"
                                1 -> "一顿午饭"
                                2 -> "一次月费"
                                else -> "一年心意"
                            },
                            fontSize = 10.5.sp,
                            color = Ag.Tx3,
                        )
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .agClick(onClick = onCustom)
                .clip(RoundedCornerShape(16.dp))
                .background(if (tier == -1) accent.primary.copy(alpha = 0.075f) else Ag.Card)
                .border(
                    if (tier == -1) 1.6.dp else 1.dp,
                    if (tier == -1) accent.primary.copy(alpha = 0.55f) else Ag.Line,
                    RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (tier == -1 && customAmount.isNotBlank()) "¥ $customAmount" else "自定义金额",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (tier == -1) accent.primary else Ag.Tx2,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (tier == -1) "修改" else "任意金额",
                fontSize = 11.sp,
                color = Ag.Tx3,
            )
        }
    }
}

/** 一行付款方式：品牌色圆底图标 + 名称 + 说明 + 右侧「接入中」。 */
@Composable
private fun PayRow(
    name: String,
    desc: String,
    brand: Color,
    onClick: () -> Unit,
    last: Boolean = false,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .agClick(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(brand.copy(alpha = 0.12f))
                    .border(1.dp, brand.copy(alpha = 0.20f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.take(1),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = brand,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Ag.Tx)
                Spacer(Modifier.height(2.dp))
                Text(desc, fontSize = 11.sp, color = Ag.Tx3)
            }
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(Ag.TintInk)
                    .border(1.dp, Ag.Line, CircleShape)
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text("接入中", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Ag.Tx3)
            }
        }
        if (!last) {
            Box(
                Modifier
                    .padding(start = 64.dp, end = 14.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Ag.Line),
            )
        }
    }
}

/** 主按钮：灰底 + 「接入中」，不假装能点。 */
@Composable
private fun NotReadyButton(amount: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .agClick(onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(Ag.Vio.copy(alpha = 0.10f))
            .border(1.dp, Ag.Vio.copy(alpha = 0.22f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Ag.Vio.copy(alpha = 0.75f),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "支持 ¥$amount · 通道接入中",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Ag.Vio.copy(alpha = 0.85f),
            )
        }
    }
}

/** 「钱花在哪」的一行。 */
@Composable
private fun MoneyRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    desc: String,
    last: Boolean = false,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AgRic(tint = tint, icon = icon, size = 34.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ag.Tx)
                Spacer(Modifier.height(3.dp))
                Text(desc, fontSize = 11.5.sp, lineHeight = 16.sp, color = Ag.Tx3)
            }
        }
        if (!last) {
            Box(
                Modifier
                    .padding(start = 58.dp, end = 12.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Ag.Line),
            )
        }
    }
}

/** 「不花钱也能支持」的方块。 */
@Composable
private fun FreeTile(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(92.dp)
            .agClick(onClick = onClick)
            .clip(RoundedCornerShape(18.dp))
            .background(Ag.Card)
            .border(1.dp, Ag.Line, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Ag.Vio.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Ag.Vio, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(9.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ag.Tx, maxLines = 1)
    }
}

/** 页脚：版本、签名、许可证，以及「这页为什么一直没接通」。 */
@Composable
private fun DonateFooter() {
    val stamp = remember { SimpleDateFormat("yyyy.MM", Locale.CHINA).format(Date()) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "FoxSu · $stamp · GPL-3.0",
            fontSize = 11.sp,
            color = Ag.Tx3,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "基于 KernelSU 二次开发，内核部分与上游同源。\n捐赠是自愿的，不解锁任何功能，也不影响更新。",
            fontSize = 10.5.sp,
            lineHeight = 16.sp,
            color = Ag.Tx3.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
    }
}

/** 自定义金额输入。 */
@Composable
private fun CustomAmountDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Ag.Bg2)
                .padding(18.dp),
        ) {
            Text("自定义金额", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Ag.Tx)
            Spacer(Modifier.height(4.dp))
            Text("金额随意，1 元也是心意。", fontSize = 11.5.sp, color = Ag.Tx3)
            Spacer(Modifier.height(16.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Ag.searchBg())
                    .border(1.dp, Ag.Line, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("¥", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ag.Tx3)
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { input ->
                        text = input.filter { it.isDigit() }.take(5)
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ag.Tx,
                        fontFamily = FontFamily.Default,
                    ),
                    cursorBrush = SolidColor(Ag.Vio),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AgButton(
                    primary = false,
                    text = "取消",
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                AgButton(
                    primary = true,
                    text = "确定",
                    modifier = Modifier.weight(1f),
                    enabled = text.isNotBlank(),
                    onClick = { onConfirm(text) },
                )
            }
        }
    }
}
