package com.kuaimaosu.manager.ui.screen.about

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Difference
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.component.KuaiMaoBackCapsule
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoPageTitle
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.theme.BackgroundEffect
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoType
import com.kuaimaosu.manager.ui.theme.blurSource
import com.kuaimaosu.manager.ui.util.adaptiveScaffoldWindowInsets

/** 一条改动：一句话说明改了什么，后面补一句为什么 / 怎么做的。 */
private class Change(
    val title: String,
    val detail: String,
)

/** 一组改动：按「改动的部位」分组，组内一条一个卡片行。 */
private class ChangeGroup(
    val title: String,
    val items: List<Change>,
)

/**
 * 修改清单。
 *
 * 本项目以 GPL-3.0 发布，协议要求分发修改版时说明改了什么，所以这一页不是宣传页，
 * 而是一张如实的差异表：只列**相对上游 BakaSU 真正动过的地方**，
 * 分组顺序按「品牌 → 界面 → 交互 → 个性化 → 功能 → 合规」排，
 * 每组一张卡、一条一段，条与条之间一条缩进发丝线。
 */
private val changeGroups = listOf(
    ChangeGroup(
        title = "品牌与身份",
        items = listOf(
            Change(
                title = "更名 KuaiMaoSU",
                detail = "应用名、包名（com.kuaimaosu.manager）与模块名统一替换，" +
                    "ksud 重新编译，清掉上游品牌在代码与内核日志里的残留。",
            ),
            Change(
                title = "重新绘制图标",
                detail = "以 K 与猫头剪影合成新图标，输出多密度位图并缩进自适应图标安全区，" +
                    "标题栏与启动页使用同一枚标记。",
            ),
            Change(
                title = "启动页重做",
                detail = "一笔水墨扫过纸面后再浮出图标与标题；按壁纸亮度自动切换" +
                    "「白纱深墨 / 黑纱白墨」，亮色壁纸下不会再闪过一块黑屏。",
            ),
        ),
    ),
    ChangeGroup(
        title = "界面重做",
        items = listOf(
            Change(
                title = "统一的设计语言",
                detail = "把配色、圆角、字重与行高收敛成一套令牌，主页、设置、" +
                    "超级用户与模块页按同一套规范重排，不再混着上游各时期的写法。",
            ),
            Change(
                title = "主页大卡片",
                detail = "跟随触摸位置的 3D 透视倾斜与径向流光，松手用弹簧回正；" +
                    "卡面漂浮一层极淡的尘埃粒子。",
            ),
            Change(
                title = "信息流骨架屏",
                detail = "首屏先给与真实内容同形的骨架，每 1.5 秒扫过一次光，" +
                    "真实内容按 60ms 阶梯淡入上移，整页不跳动、不闪白。",
            ),
            Change(
                title = "底栏三种形态",
                detail = "传统栏贴底、胶囊栏浮起、侧边栏竖排，在设置里直接切换；" +
                    "胶囊栏支持整条拖动换页，重启入口与其它页签同一套图标与字号。",
            ),
            Change(
                title = "页面转场动画",
                detail = "新增层叠、上浮、立方三档转场，只改图形层，" +
                    "系统的手势驱动返回与预测式返回仍然可用。",
            ),
        ),
    ),
    ChangeGroup(
        title = "交互与动画",
        items = listOf(
            Change(
                title = "横向换页手势",
                detail = "主页四个页签，以及安装、模块详情、SUSFS 等页面统一支持横滑换页，" +
                    "竖向滚动与列表点击不受影响。",
            ),
            Change(
                title = "开关动画",
                detail = "滑块带弹性、按住时拉长，轨道颜色渐变并从滑块位置扩散一圈薄光；" +
                    "只作展示的开关也会播放动画。",
            ),
            Change(
                title = "重启面板",
                detail = "重启、软重启、Recovery、Bootloader、Download、EDL 收进一个不高的" +
                    "六宫格面板；开启模糊时是磨砂玻璃，关闭时为实色。",
            ),
            Change(
                title = "流光轮廓",
                detail = "ColorOS 17 风格的边缘凝光：一束光沿着圆角滑行，转角自然拐弯，" +
                    "作用于大卡片与胶囊底栏，可在外观设置里关闭。",
            ),
        ),
    ),
    ChangeGroup(
        title = "个性化",
        items = listOf(
            Change(
                title = "在线字体",
                detail = "思源黑体、霞鹜文楷等多套字体可在线下载，多个镜像依次重试，" +
                    "下载后校验文件头再启用；选择器改成双列卡片，用字体本身预览。",
            ),
            Change(
                title = "背景特效",
                detail = "共 ${BackgroundEffect.effectCount} 种程序化特效，全部按主题色实时绘制；" +
                    "选择器改为三列大预览，默认收起，点开才展开。",
            ),
            Change(
                title = "DPI 与标题",
                detail = "DPI 滑块收进可展开区，避免在主页误触；顶栏标题的诗词每次进入随机一首。",
            ),
        ),
    ),
    ChangeGroup(
        title = "功能",
        items = listOf(
            Change(
                title = "内置挂载控制器",
                detail = "把 OverlayFS 与 Magic Mount 做进设置，直接切换生效，" +
                    "不需要再安装额外模块；逐模块的挂载规则也在同一页管理。",
            ),
            Change(
                title = "免责声明",
                detail = "首次启动弹出四条风险声明，实色卡片、带应用图标，" +
                    "同意状态按版本记录，条款改版会重新询问。",
            ),
            Change(
                title = "关于页补齐",
                detail = "新增开源致谢（上游作者、头像、仓库与协议）、修改清单与本页之外，" +
                    "完整依赖清单仍由 AboutLibraries 依构建产物生成。",
            ),
        ),
    ),
    ChangeGroup(
        title = "合规",
        items = listOf(
            Change(
                title = "协议与署名",
                detail = "整体以 GPL-3.0 发布，保留上游版权声明；" +
                    "内核部分与各依赖各自保留 GPL-2.0 / GPL-3.0 / Apache-2.0 原始协议。",
            ),
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModificationsScreen() {
    val navigator = LocalNavigator.current

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
                    title = "修改清单",
                    subtitle = "相对上游 BakaSU，我们改了什么",
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
            item {
                KuaiMaoCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(KuaiMaoColors.brand.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.Difference,
                                contentDescription = null,
                                tint = KuaiMaoColors.brand,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "基于 BakaSU 开发",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = KuaiMaoColors.text,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "KuaiMaoSU 是在 BakaSU（原 ReSukiSU）的基础上开发的：内核方案、" +
                            "ksud 与管理器的骨架都来自上游，本页只列我们改动过的部分。" +
                            "GPL-3.0 要求分发修改版时说明改动，这一页就是那张清单。",
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = KuaiMaoColors.textBody,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            changeGroups.forEach { group ->
                item {
                    Text(
                        text = group.title,
                        style = KuaiMaoType.sectionTitle,
                        modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
                    )
                }
                item {
                    KuaiMaoCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                        group.items.forEachIndexed { index, change ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(11.dp),
                            ) {
                                // 每行一个序号方块：比圆点更容易看出这是一份清单
                                Box(
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(KuaiMaoColors.brand.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = (index + 1).toString().padStart(2, '0'),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = KuaiMaoColors.brand,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = change.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = KuaiMaoColors.text,
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = change.detail,
                                        fontSize = 10.5.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = KuaiMaoColors.textBody.copy(alpha = 0.82f),
                                    )
                                }
                            }
                            if (index != group.items.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 31.dp)
                                        .height(1.dp)
                                        .background(KuaiMaoColors.line.copy(alpha = 0.6f)),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Text(
                    text = "这份清单随版本更新。源码与提交记录：github.com/Cmry-mlc/KuaiMaoSU",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = KuaiMaoColors.textMuted,
                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
                )
            }
        }
    }
}
