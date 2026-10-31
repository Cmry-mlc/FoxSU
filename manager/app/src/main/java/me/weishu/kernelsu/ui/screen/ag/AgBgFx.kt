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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.component.ambient.AmbientPawsLayer
import me.weishu.kernelsu.ui.component.ambient.BackgroundFxLayer
import me.weishu.kernelsu.ui.component.ambient.FX_NAMES

/**
 * 背景特效选择器：猫爪 + 20 套整屏特效，互斥单选。
 *
 * 为什么猫爪也放在这里：它和 20 套背景特效抢的是主页同一个装饰槽位。
 * 原来猫爪是独立开关，加了背景特效之后再独立就会同时画两层——小爪印浮在极光上面，
 * 各自的节奏还不同步，看着像两块素材叠错了。合并成 21 选 1 之后语义才一致。
 *
 * 预览一律用真实渲染器（[BackgroundFxLayer] / [AmbientPawsLayer]），不是另画一套示意图：
 * 示意图和真机对不上是这类选择器最常见的坑。但 21 套同时逐帧跑代价太大，
 * 所以只有选中的那套常驻，其余点一下播两秒就停。
 */
@Composable
fun AgBgFx(onBack: () -> Unit) {
    val custom = CustomizationStore.current
    var previewing by remember { mutableIntStateOf(-1) }

    // 点一下播两秒：够看清运动方式，又不至于让 21 张卡一起跑
    LaunchedEffect(previewing) {
        if (previewing >= 0) {
            delay(2000)
            previewing = -1
        }
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
                .padding(start = 18.dp, end = 18.dp)
                .padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AgIconBtn(AgTopIcons.Back, onClick = onBack, tint = Ag.Tx)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("背景特效", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Ag.Tx)
                Text(
                    if (custom.bgFxStyle == 0) {
                        "当前：猫爪氛围" + if (custom.bgFxOn) "" else "（已关闭）"
                    } else {
                        "当前：第 ${custom.bgFxStyle} 套 · ${FX_NAMES[custom.bgFxStyle - 1]}" +
                            if (custom.bgFxOn) "" else "（已关闭）"
                    },
                    fontSize = 11.sp, color = Ag.Tx3,
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(2) }) {
                AgCard {
                    AgToggleRow(
                        "背景特效",
                        if (custom.bgFxOn) {
                            "开启中。主页、捐赠页与定制页头卡会显示下面选中的那一套"
                        } else {
                            "已关闭。选择仍会记住，重新打开立刻恢复；关闭后主页不再逐帧重绘这一层"
                        },
                        Ag.Mag, AgSettingsIcons.Paw,
                        trailing = {
                            AgToggle(checked = custom.bgFxOn, onCheckedChange = {
                                CustomizationStore.setBgFxOn(!custom.bgFxOn)
                            }, small = true)
                        },
                    )
                }
            }

            item(span = { GridItemSpan(2) }) {
                Text(
                    "这 21 套画在内容之上而不是垫在下面——五套皮肤的主页都各铺了不透明底色，" +
                        "垫在下面根本看不见。所以透明度都压到六成左右：看得见氛围，但不会盖住字。" +
                        "换强调色时整套跟着变，没有写死的第二种配色。",
                    fontSize = 11.5.sp, lineHeight = 17.sp, color = Ag.Tx3,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }

            items((0..20).toList()) { index ->
                BgFxCard(
                    index = index,
                    selected = custom.bgFxStyle == index,
                    playing = custom.bgFxStyle == index || previewing == index,
                    onClick = {
                        if (custom.bgFxStyle != index) {
                            CustomizationStore.setBgFxStyle(index)
                            previewing = index
                            // 选了一套但总开关关着，等于没反应——顺手把它打开
                            if (!custom.bgFxOn) CustomizationStore.setBgFxOn(true)
                        } else {
                            previewing = index
                        }
                    },
                )
            }
        }
    }
}

/** 一张特效卡：上半是真实渲染的缩略图，下半是序号与名字。 */
@Composable
private fun BgFxCard(
    index: Int,
    selected: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
) {
    val previewBg = if (Ag.isDark) Color(0xFF171625) else Color(0xFFEFEDF7)
    val name = if (index == 0) "猫爪氛围" else FX_NAMES[index - 1]

    Column(
        Modifier
            .agClick(onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    if (Ag.isDark) Ag.accent.softDark else Ag.accent.soft
                } else {
                    Ag.Card
                }
            )
            .border(
                if (selected) 1.6.dp else 1.dp,
                if (selected) Ag.Vio.copy(alpha = 0.55f) else Ag.Line,
                RoundedCornerShape(14.dp),
            )
            .padding(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(previewBg),
        ) {
            if (index == 0) {
                AmbientPawsLayer(
                    enabled = true,
                    count = 5,
                    alphaScale = 0.95f,
                )
            } else {
                BackgroundFxLayer(
                    style = index,
                    enabled = true,
                    modifier = Modifier.fillMaxSize(),
                    alphaScale = 1f,
                    playing = playing,
                )
            }

            Text(
                if (index == 0) "01" else "%02d".format(index + 1),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (Ag.isDark) Color(0x66FFFFFF) else Color(0x66000000),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp),
            )

            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Ag.Grad)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("已选", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier.padding(start = 4.dp, end = 4.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                name,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (selected) Ag.Vio else Ag.Tx,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
