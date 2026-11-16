package com.kuaimaosu.manager.ui.screen.about

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Code
import androidx.compose.material.icons.twotone.Description
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.ui.component.KuaiMaoBackCapsule
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoPageTitle
import com.kuaimaosu.manager.ui.component.KuaiMaoRow
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.navigation.Route
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoType
import com.kuaimaosu.manager.ui.theme.blurSource
import com.kuaimaosu.manager.ui.util.adaptiveScaffoldWindowInsets

/**
 * 一个上游项目。
 *
 * [license] 是协议短名，[licenseUrl] 指向协议全文；[avatar] 为 null 时
 * 用项目首字母画一个圆形色块代替（比如 Jetpack Compose 没有单独的组织头像）。
 */
private data class Credit(
    val name: String,
    val author: String,
    val repo: String,
    val license: String,
    val licenseUrl: String,
    val note: String,
    @DrawableRes val avatar: Int? = null,
    val initial: String = "",
)

/**
 * 本项目的直接上游。
 *
 * 单独成组而不是塞进「内核与系统」：它既是内核方案的来源，也是整个管理器
 * 与 ksud 的起点，混在其它项目里会让人以为它只是一项依赖。
 */
private val baseCredits = listOf(
    Credit(
        name = "BakaSU",
        author = "BakaSU Team（原 ReSukiSU）",
        repo = "https://github.com/Baka-SU/BakaSU",
        license = "GPL-3.0",
        licenseUrl = "https://www.gnu.org/licenses/gpl-3.0.html",
        note = "本项目的直接上游，曾用名 ReSukiSU；管理器、ksud 与内核方案都基于它开发",
        avatar = R.drawable.credit_bakasu,
    ),
)

private val kernelCredits = listOf(
    Credit(
        name = "KernelSU",
        author = "tiann",
        repo = "https://github.com/tiann/KernelSU",
        license = "GPL-3.0",
        licenseUrl = "https://www.gnu.org/licenses/gpl-3.0.html",
        note = "内核级 root 方案的起点，su 与模块机制都来自这里",
        avatar = R.drawable.credit_tiann,
    ),
    Credit(
        name = "KernelSU Next",
        author = "rifsxd",
        repo = "https://github.com/rifsxd/KernelSU-Next",
        license = "GPL-3.0",
        licenseUrl = "https://www.gnu.org/licenses/gpl-3.0.html",
        note = "本管理器所基于的分支，提供了大量兼容性与功能改进",
        avatar = R.drawable.credit_rifsxd,
    ),
    Credit(
        name = "SUSFS",
        author = "simonpunk",
        repo = "https://github.com/simonpunk/susfs4ksu",
        license = "GPL-2.0",
        licenseUrl = "https://www.gnu.org/licenses/old-licenses/gpl-2.0.html",
        note = "内核态隐藏与路径屏蔽，网页端检测的克星",
        avatar = R.drawable.credit_simonpunk,
    ),
)

private val uiCredits = listOf(
    Credit(
        name = "miuix-kmp",
        author = "YuKongA",
        repo = "https://github.com/YuKongA/miuix-kmp",
        license = "Apache-2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        note = "毛玻璃实时模糊与转场动画的实现基础",
        avatar = R.drawable.credit_yukonga,
    ),
    Credit(
        name = "Jetpack Compose",
        author = "Google · AndroidX",
        repo = "https://github.com/androidx/androidx",
        license = "Apache-2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        note = "整套界面的声明式 UI 与动画系统",
        initial = "A",
    ),
    Credit(
        name = "Koin",
        author = "InsertKoinIO",
        repo = "https://github.com/InsertKoinIO/koin",
        license = "Apache-2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        note = "依赖注入，把仓库、用例和页面解耦",
        avatar = R.drawable.credit_insertkoinio,
    ),
    Credit(
        name = "Coil",
        author = "coil-kt",
        repo = "https://github.com/coil-kt/coil",
        license = "Apache-2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        note = "Compose 原生的图片加载",
        avatar = R.drawable.credit_coilkt,
    ),
    Credit(
        name = "MonetCompat",
        author = "KieronQuinn",
        repo = "https://github.com/KieronQuinn/MonetCompat",
        license = "Apache-2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        note = "让取色机制在所有系统版本上保持一致",
        avatar = R.drawable.credit_kieronquinn,
    ),
)

private val toolCredits = listOf(
    Credit(
        name = "libsu",
        author = "topjohnwu",
        repo = "https://github.com/topjohnwu/libsu",
        license = "Apache-2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        note = "稳定的 root shell 与守护进程通信",
        avatar = R.drawable.credit_topjohnwu,
    ),
    Credit(
        name = "AboutLibraries",
        author = "mikepenz",
        repo = "https://github.com/mikepenz/AboutLibraries",
        license = "Apache-2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        note = "自动汇总全部依赖与协议，本页之外的完整清单由它生成",
        avatar = R.drawable.credit_mikepenz,
    ),
)

/**
 * 开源致谢。
 *
 * 按「内核 / 界面 / 工具」分三组，每项一张卡：头像、项目名、作者、
 * 协议徽章、一句它在本项目里做了什么。点整行跳到对应仓库。
 * 底部单独说明本项目的协议与源码，避免只「拿来」不「回馈」。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen() {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current

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
                    title = "开源致谢",
                    subtitle = "站在这些项目与作者的肩膀上",
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
                // 开篇：先把「谢」说清楚，再列清单
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
                                imageVector = Icons.TwoTone.Code,
                                contentDescription = null,
                                tint = KuaiMaoColors.brand,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "感谢每一位上游作者",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = KuaiMaoColors.text,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "KuaiMaoSU 不是从零写起的，它基于 BakaSU 开发。" +
                            "下面这些项目提供了内核方案、界面框架与基础工具，" +
                            "本项目的每一行代码都建立在它们之上。" +
                            "如果你也从中受益，请给它们点一颗 Star。",
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = KuaiMaoColors.textBody,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            creditSection(
                title = "项目基础",
                credits = baseCredits,
                onOpen = { uriHandler.openUri(it) },
            )
            creditSection(
                title = "内核与系统",
                credits = kernelCredits,
                onOpen = { uriHandler.openUri(it) },
            )
            creditSection(
                title = "界面与框架",
                credits = uiCredits,
                onOpen = { uriHandler.openUri(it) },
            )
            creditSection(
                title = "工具",
                credits = toolCredits,
                onOpen = { uriHandler.openUri(it) },
            )

            item {
                Text(
                    text = "协议与合规",
                    style = KuaiMaoType.sectionTitle,
                    modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
                )
            }

            item {
                KuaiMaoCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Code,
                        title = "本项目的源码",
                        description = "GPL-3.0 · 修改与分发需同样开源",
                        onClick = { uriHandler.openUri("https://github.com/Cmry-mlc/FoxSu") },
                    )
                    KuaiMaoRowDivider()
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Description,
                        title = "完整依赖与协议清单",
                        description = "由 AboutLibraries 依据构建产物自动生成",
                        onClick = { navigator.push(Route.OpenSourceLicense) },
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                KuaiMaoCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "协议说明",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = KuaiMaoColors.text,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "KuaiMaoSU 以 GPL-3.0 协议发布：你可以自由使用、修改与再分发，" +
                            "但分发修改版时必须同样开源，并保留原作者版权声明。" +
                            "本项目包含的上游代码各自保留其原始协议（GPL-3.0 / GPL-2.0 / Apache-2.0），" +
                            "完整清单与协议全文见上一项。",
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = KuaiMaoColors.textBody,
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "再次感谢所有上游作者与贡献者。",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = KuaiMaoColors.textMuted,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
    }
}

/** 一组致谢：分组标题 + 一张卡片，卡里每个项目一行，行间一条缩进发丝线。 */
private fun androidx.compose.foundation.lazy.LazyListScope.creditSection(
    title: String,
    credits: List<Credit>,
    onOpen: (String) -> Unit,
) {
    item {
        Text(
            text = title,
            style = KuaiMaoType.sectionTitle,
            modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
        )
    }
    item {
        KuaiMaoCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
            credits.forEachIndexed { index, credit ->
                CreditRow(credit = credit, onClick = { onOpen(credit.repo) })
                if (index != credits.lastIndex) {
                    // 发丝线从文字起始位置开始，头像那一列留空，看起来更整齐
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 60.dp)
                            .height(1.dp)
                            .background(KuaiMaoColors.line.copy(alpha = 0.6f)),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CreditRow(
    credit: Credit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (credit.avatar != null) {
            Image(
                painter = painterResource(credit.avatar),
                contentDescription = null,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(1.dp, KuaiMaoColors.line.copy(alpha = 0.7f), CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(KuaiMaoColors.brand.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = credit.initial,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = KuaiMaoColors.brand,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = credit.name,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = KuaiMaoColors.text,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(6.dp))
                LicenseChip(license = credit.license)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "by ${credit.author}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = KuaiMaoColors.textMuted,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = credit.note,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
                color = KuaiMaoColors.textBody.copy(alpha = 0.78f),
                maxLines = 2,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.TwoTone.ChevronRight,
            contentDescription = null,
            tint = KuaiMaoColors.textMuted.copy(alpha = 0.55f),
            modifier = Modifier.size(16.dp),
        )
    }
}

/** 协议徽章：GPL 系用琥珀色，Apache 用蓝色，一眼能分开。 */
@Composable
private fun LicenseChip(license: String) {
    val color = if (license.startsWith("GPL")) {
        KuaiMaoColors.warning
    } else {
        KuaiMaoColors.brand
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 5.dp, vertical = 1.5.dp),
    ) {
        Text(
            text = license,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            color = color,
            maxLines = 1,
        )
    }
}
