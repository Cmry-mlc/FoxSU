package com.kuaimaosu.manager.ui.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Brush
import androidx.compose.material.icons.twotone.Inventory2
import androidx.compose.material.icons.twotone.Public
import androidx.compose.material.icons.twotone.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush as GradientBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.ui.component.KuaiMaoBackCapsule
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoPageTitle
import com.kuaimaosu.manager.ui.component.KuaiMaoRow
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoType
import com.kuaimaosu.manager.ui.theme.blurSource
import com.kuaimaosu.manager.ui.util.adaptiveScaffoldWindowInsets

/**
 * 图标与视觉。
 *
 * 单独一页而不是把署名塞进「关于」某一行的小字里：图形资产是有版权的，
 * 谁画的、按什么协议分发、哪些是从上游继承的，都要写清楚，而不是一句
 * 「图标作者 XXX」带过。
 *
 * 三块内容：
 *  1. 应用图标本体 —— 放大展示，配作者名；
 *  2. 图标来源 —— 本项目的图标 / 上游继承的图标 / 第三方图标，分别署名；
 *  3. 使用约定 —— 二次分发时保留署名。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconCreditsScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current

    Scaffold(
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                KuaiMaoBackCapsule(onClick = { navigator.pop() })
                Spacer(modifier = Modifier.height(14.dp))
                KuaiMaoPageTitle(
                    title = "图标与视觉",
                    subtitle = "图形资产与署名",
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .blurSource(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            // ---- 应用图标本体 -----------------------------------------
            item {
                KuaiMaoCard(contentPadding = PaddingValues(vertical = 26.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // 图标下面垫一圈主题色光晕，深色壁纸下才不像一块贴上去的黑方块
                        Box(
                            modifier = Modifier.size(148.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = GradientBrush.radialGradient(
                                            colors = listOf(
                                                KuaiMaoColors.brand.copy(alpha = 0.20f),
                                                Color.Transparent,
                                            ),
                                        ),
                                        shape = CircleShape,
                                    ),
                            )
                            Image(
                                modifier = Modifier
                                    .size(112.dp)
                                    .clip(RoundedCornerShape(32.dp)),
                                painter = rememberDrawablePainter(
                                    drawable = ContextCompat.getDrawable(
                                        context,
                                        R.drawable.ic_brand_logo,
                                    ),
                                ),
                                contentDescription = "KuaiMaoSU 应用图标",
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "KuaiMaoSU",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.2).sp,
                            color = KuaiMaoColors.text,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "应用图标 · 尘梦如烟",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = KuaiMaoColors.textMuted,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        // 图标含义一句话：黑底、白色 K、猫耳剪影
                        Text(
                            text = "黑底上的白色 K 与猫耳剪影，取「快猫」二字，\n" +
                                "图形在桌面图标与开屏标识中共用同一套比例。",
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = KuaiMaoColors.textMuted,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            item {
                Text(
                    text = "图标作者",
                    style = KuaiMaoType.sectionTitle,
                    modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
                )
            }

            item {
                KuaiMaoCard {
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Brush,
                        title = "尘梦如烟",
                        description = "应用图标、品牌标识与开屏图形的设计者",
                    )
                    KuaiMaoRowDivider()
                    // 作者没有公开仓库，这里只写归属，不放一个点了没反应的箭头
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Inventory2,
                        title = "版权归属",
                        description = "图形版权归作者所有，随本项目以 GPL-3.0 分发",
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            item {
                Text(
                    text = "图标来源",
                    style = KuaiMaoType.sectionTitle,
                    modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
                )
            }

            item {
                KuaiMaoCard {
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Widgets,
                        title = "Material Symbols",
                        description = "界面功能图标 · Google · Apache-2.0",
                    )
                    KuaiMaoRowDivider()
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Inventory2,
                        title = "BakaSU 原有图标",
                        description = "从上游继承的矢量与位图资源，原样保留 · GPL-3.0",
                    )
                    KuaiMaoRowDivider()
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Public,
                        title = "第三方图标",
                        description = "模块、应用与系统图标归各自作者与项目所有",
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ---- 使用约定 ----------------------------------------------
            item {
                KuaiMaoCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(KuaiMaoColors.brand.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.Public,
                                contentDescription = null,
                                tint = KuaiMaoColors.brand,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "二次分发请保留署名",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = KuaiMaoColors.text,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "如果你基于本项目重新打包分发，请保留本页的应用图标署名、" +
                            "上游 BakaSU 的版权声明，以及 GPL-3.0 要求的完整源码。" +
                            "移除署名后再分发不符合本项目的授权条件。",
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = KuaiMaoColors.textBody,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "快猫 · KuaiMaoSU · 图标 尘梦如烟 · GPL-3.0",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = KuaiMaoColors.textMuted,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
    }
}
