package me.weishu.kernelsu.ui.screen.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.applyUiMode
import kotlin.math.absoluteValue

private const val PAGE_COUNT = 5

private const val REPO_URL = "https://github.com/Cmry-mlc/FoxSu"
private const val WEBSITE_URL = "https://cmry-mlc.github.io/FoxSu/zh_CN/"

/**
 * 首次安装后的欢迎引导（Dialog 承载，只在 OnboardingPrefs 未标记时出现一次）。
 *
 * 视觉严格对齐设计稿 `style-picker` / `tokens.css`：
 *  - 皮肤层：Nocturne / Daylight 双模式，顶部紫罗兰光晕；
 *  - 组件层：--pk-* 令牌（卡片、描边、分段控件、幽灵按钮）；
 *  - 主按钮为设计稿的 lime→mint 渐变，副按钮为描边幽灵按钮；
 *  - 内容分五页：品牌 / 五套皮肤 / 深度定制 / 内核能力 / 开源仓库。
 */
@Composable
fun WelcomeOverlay(onFinish: () -> Unit) {
    Dialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        WelcomeContent(onFinish = onFinish)
    }
}

@Composable
private fun WelcomeContent(onFinish: () -> Unit) {
    val t = onbTokens(LocalUiMode.current)
    val pager = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(t.bg),
    ) {
        // 顶部光晕：设计稿 screen::before 的 radial-gradient
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(t.glow, Color.Transparent),
                    center = Offset(size.width * 0.18f, -size.height * 0.06f),
                    radius = size.width * 1.15f,
                ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            OnbTopBar(page = pager.currentPage, t = t)

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1,
            ) { index ->
                OnbPage(
                    index = index,
                    offset = (pager.currentPage - index + pager.currentPageOffsetFraction)
                        .absoluteValue
                        .coerceIn(0f, 1f),
                    t = t,
                )
            }

            OnbProgress(page = pager.currentPage, t = t)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GhostButton(
                    label = stringResource(R.string.onboarding_skip),
                    t = t,
                    onClick = onFinish,
                    modifier = Modifier.weight(1f),
                )
                val last = pager.currentPage == PAGE_COUNT - 1
                PrimaryButton(
                    label = stringResource(
                        if (last) R.string.onboarding_start else R.string.onboarding_next
                    ),
                    t = t,
                    onClick = {
                        if (last) {
                            onFinish()
                        } else {
                            scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.weight(1.6f),
                )
            }
        }
    }
}

/** 顶栏：狐标 + 品牌名 + 等宽计数 */
@Composable
private fun OnbTopBar(page: Int, t: OnbTokens) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(t.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.foxsu_mark),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(
            text = "FoxSu",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            color = t.tx,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = String.format("%02d", page + 1) + " / " + String.format("%02d", PAGE_COUNT),
            fontSize = 10.sp,
            letterSpacing = 1.6.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = t.tx3,
        )
    }
}

/** 底部进度：设计稿的细段式，选中段加长并染成强调色 */
@Composable
private fun OnbProgress(page: Int, t: OnbTokens) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(PAGE_COUNT) { i ->
            val active = i <= page
            val w by animateFloatAsState(
                targetValue = if (i == page) 26f else 12f,
                animationSpec = tween(240),
                label = "onbSeg",
            )
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .width(w.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (active) t.accent else t.line),
            )
        }
    }
}

/** 主按钮：设计稿 lime→mint 渐变，深色文字 */
@Composable
private fun PrimaryButton(
    label: String,
    t: OnbTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            // 品牌渐变与主界面一致（紫 → 粉），不再用设计稿早期的 lime→mint
            .background(Brush.horizontalGradient(listOf(t.accent, t.accent2)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.6.sp,
            color = Color.White,
        )
    }
}

/** 幽灵按钮：仅描边 */
@Composable
private fun GhostButton(
    label: String,
    t: OnbTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, t.ghostBd), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = t.ghostTx,
        )
    }
}

@Composable
private fun OnbPage(index: Int, offset: Float, t: OnbTokens) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = lerp(1f, 0.12f, offset)
                translationY = offset * 22.dp.toPx()
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when (index) {
                0 -> HeroPanel(t)
                1 -> SkinPanel(t)
                2 -> FeaturePanel(
                    t = t,
                    kicker = stringResource(R.string.onboarding_kicker_3),
                    title = stringResource(R.string.onboarding_title_3),
                    body = stringResource(R.string.onboarding_body_3),
                    rows = listOf(
                        OnbRow(Icons.Rounded.Wallpaper, R.string.onboarding_fe_wallpaper, R.string.onboarding_fe_wallpaper_sum),
                        OnbRow(Icons.Rounded.TextFields, R.string.onboarding_fe_font, R.string.onboarding_fe_font_sum),
                        OnbRow(Icons.Rounded.Palette, R.string.onboarding_fe_card, R.string.onboarding_fe_card_sum),
                        OnbRow(Icons.Rounded.CropSquare, R.string.onboarding_fe_radius, R.string.onboarding_fe_radius_sum),
                        OnbRow(Icons.Rounded.Menu, R.string.onboarding_fe_bar, R.string.onboarding_fe_bar_sum),
                        OnbRow(Icons.Rounded.Android, R.string.onboarding_fe_icon, R.string.onboarding_fe_icon_sum),
                    ),
                )
                3 -> FeaturePanel(
                    t = t,
                    kicker = stringResource(R.string.onboarding_kicker_4),
                    title = stringResource(R.string.onboarding_title_4),
                    body = stringResource(R.string.onboarding_body_4),
                    rows = listOf(
                        OnbRow(Icons.Rounded.Bolt, R.string.onboarding_fe_install, R.string.onboarding_fe_install_sum),
                        OnbRow(Icons.Rounded.Extension, R.string.onboarding_fe_module, R.string.onboarding_fe_module_sum),
                        OnbRow(Icons.Rounded.Shield, R.string.onboarding_fe_profile, R.string.onboarding_fe_profile_sum),
                        OnbRow(Icons.Rounded.Terminal, R.string.onboarding_fe_state, R.string.onboarding_fe_state_sum),
                        OnbRow(Icons.Rounded.Code, R.string.onboarding_fe_sulog, R.string.onboarding_fe_sulog_sum),
                    ),
                )
                else -> RepoPanel(t)
            }
        }

        val kicker = stringResource(
            when (index) {
                0 -> R.string.onboarding_kicker_1
                1 -> R.string.onboarding_kicker_2
                2 -> R.string.onboarding_kicker_3
                3 -> R.string.onboarding_kicker_4
                else -> R.string.onboarding_kicker_5
            }
        )
        val title = stringResource(
            when (index) {
                0 -> R.string.onboarding_title_1
                1 -> R.string.onboarding_title_2
                2 -> R.string.onboarding_title_3
                3 -> R.string.onboarding_title_4
                else -> R.string.onboarding_title_5
            }
        )
        val body = stringResource(
            when (index) {
                0 -> R.string.onboarding_body_1
                1 -> R.string.onboarding_body_2
                2 -> R.string.onboarding_body_3
                3 -> R.string.onboarding_body_4
                else -> R.string.onboarding_body_5
            }
        )

        Text(
            text = kicker,
            fontSize = 9.sp,
            letterSpacing = 3.sp,
            fontWeight = FontWeight.ExtraBold,
            color = t.accent,
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = title,
            fontSize = 26.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.7).sp,
            color = t.tx,
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = body,
            fontSize = 12.5.sp,
            lineHeight = 19.sp,
            color = t.tx2,
        )
        Spacer(Modifier.height(18.dp))
    }
}

/**
 * 第一页：品牌主视觉。
 *
 * 原来是一个 Canvas 画的"彩虹圆环 + 96dp 小狐狸"，环细、狐小，页面空得像占位符。
 * 现在直接上新的狐狸 logo（自带彩虹尾，本身就是"环"），去掉外圈，
 * 只留一层奶白柔光托底，让方图的边缘融进页面；尺寸放大到 208dp。
 */
@Composable
private fun HeroPanel(t: OnbTokens) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            // 奶白柔光：把 logo 的方形底晕开，避免出现一块方图
            Canvas(Modifier.size(272.dp)) {
                val cream = if (t.dark) Color(0x33FFFDF7) else Color(0xFFFFFDF7)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(cream, cream.copy(alpha = 0.55f), Color.Transparent),
                        center = center,
                        radius = size.minDimension / 2f,
                    ),
                    radius = size.minDimension / 2f,
                )
            }
            Image(
                painter = painterResource(R.drawable.foxsu_mark),
                contentDescription = "FoxSu",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(208.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(
                R.string.onboarding_chip_1,
                R.string.onboarding_chip_2,
                R.string.onboarding_chip_3,
            ).forEach { res ->
                Text(
                    text = stringResource(res),
                    fontSize = 9.5.sp,
                    letterSpacing = 1.1.sp,
                    fontWeight = FontWeight.Bold,
                    color = t.tx2,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(t.chipBg)
                        .border(BorderStroke(1.dp, t.chipBd), RoundedCornerShape(99.dp))
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** 第二页：五套皮肤卡片，点击即写库即时生效 */
@Composable
private fun SkinPanel(t: OnbTokens) {
    val current = LocalUiMode.current
    var applied by remember { mutableStateOf(current) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 8.dp),
        ) {
            items(OnbSkins, key = { it.id }) { skin ->
                SkinCard(
                    skin = skin,
                    selected = skin.mode == applied,
                    t = t,
                    onClick = {
                        applied = skin.mode
                        applyUiMode(skin.mode)
                    },
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        SkinInfo(
            skin = OnbSkins.first { it.mode == applied },
            t = t,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.onboarding_theme_tap),
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Bold,
            color = t.tx3,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

/** 皮肤缩略卡：设计稿 .pk 规格 —— 82 高预览 + 名称 + 状态 */
@Composable
private fun SkinCard(
    skin: OnbSkin,
    selected: Boolean,
    t: OnbTokens,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) t.selBg else t.cardBg)
            .border(
                BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) t.selBd else t.cardBd),
                RoundedCornerShape(15.dp),
            )
            .clickable(onClick = onClick)
            .padding(7.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(skin.bg),
        ) {
            // 缩略预览：标题块 + 两条内容条 + 一个强调块
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(7.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(skin.accent.copy(alpha = 0.9f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(skin.bg.let { Color.White.copy(alpha = if (skin.dark) 0.16f else 0.5f) }),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(skin.bg.let { Color.White.copy(alpha = if (skin.dark) 0.12f else 0.32f) }),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(skin.mint.copy(alpha = 0.85f)),
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(t.mint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_d_check),
                        contentDescription = null,
                        tint = if (t.dark) Color.White else Color(0xFF08110E),
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = skin.label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = t.tx,
            maxLines = 1,
        )
        Text(
            text = skin.sub,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            color = t.tx3,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = if (selected) stringResource(R.string.onboarding_theme_inuse) else " ",
            fontSize = 7.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp,
            color = t.mint,
            maxLines = 1,
        )
    }
}

/** 皮肤说明卡：设计稿 .pkinfo */
@Composable
private fun SkinInfo(skin: OnbSkin, t: OnbTokens) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(t.infoBg)
            .border(BorderStroke(1.dp, t.infoBd), RoundedCornerShape(14.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Column {
            Text(
                text = skin.label + " · " + skin.sub,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = t.infoB,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = stringResource(skin.descRes),
                fontSize = 10.5.sp,
                lineHeight = 17.sp,
                color = t.infoTx,
            )
        }
    }
}

private data class OnbRow(
    val icon: ImageVector,
    val titleRes: Int,
    val sumRes: Int,
)

/** 能力清单面板：设计稿 .row 规格（图标盒 + 标题 + 说明） */
@Composable
private fun FeaturePanel(
    t: OnbTokens,
    kicker: String,
    title: String,
    body: String,
    rows: List<OnbRow>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(t.cardBg)
                .border(BorderStroke(1.dp, t.cardBd), RoundedCornerShape(20.dp))
                .padding(vertical = 4.dp),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(t.accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = row.icon,
                            contentDescription = null,
                            tint = t.accent,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(row.titleRes),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = t.tx,
                        )
                        Text(
                            text = stringResource(row.sumRes),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = t.tx3,
                        )
                    }
                }
            }
        }
    }
}

/** 第五页：开源仓库 —— 品牌卡 + 官网 / GitHub 两个入口（只写按钮名，不露出裸链接） */
@Composable
private fun RepoPanel(t: OnbTokens) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))

        // 品牌卡：直接用 FoxSu Redesign 的紫粉渐变，跟主界面同一套语言
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(t.accent, t.accent2)))
                .drawBehind {
                    // 右上角一团柔光，避免整块纯渐变发闷
                    val r = size.width * 0.62f
                    val c = Offset(size.width * 0.84f, size.height * 0.10f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0x40FFFFFF), Color.Transparent),
                            center = c,
                            radius = r,
                        ),
                        radius = r,
                        center = c,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.20f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.foxsu_mark),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(66.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "FoxSu",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.onboarding_brand_tagline),
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    color = Color.White.copy(alpha = 0.86f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        RepoLink(
            icon = Icons.Rounded.Language,
            label = stringResource(R.string.onboarding_website),
            sub = stringResource(R.string.onboarding_website_sub),
            t = t,
            primary = true,
        ) { uriHandler.openUri(WEBSITE_URL) }

        Spacer(Modifier.height(10.dp))

        RepoLink(
            icon = Icons.Rounded.Code,
            label = stringResource(R.string.onboarding_github),
            sub = stringResource(R.string.onboarding_github_sub),
            t = t,
            primary = false,
        ) { uriHandler.openUri(REPO_URL) }

        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.onboarding_repo_license_short),
            fontSize = 9.5.sp,
            letterSpacing = 0.4.sp,
            color = t.tx3,
            textAlign = TextAlign.Center,
        )
    }
}

/** 仓库入口按钮：主按钮走品牌渐变，次按钮走描边，都不显示网址 */
@Composable
private fun RepoLink(
    icon: ImageVector,
    label: String,
    sub: String,
    t: OnbTokens,
    primary: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(17.dp))
            .then(
                if (primary) {
                    Modifier.background(Brush.horizontalGradient(listOf(t.accent, t.accent2)))
                } else {
                    Modifier
                        .background(t.cardBg)
                        .border(BorderStroke(1.dp, t.cardBd), RoundedCornerShape(17.dp))
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (primary) Color.White.copy(alpha = 0.18f) else t.chipBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (primary) Color.White else t.tx2,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (primary) Color.White else t.tx,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = sub,
                fontSize = 10.sp,
                color = if (primary) Color.White.copy(alpha = 0.78f) else t.tx3,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = null,
            tint = if (primary) Color.White.copy(alpha = 0.9f) else t.tx3,
            modifier = Modifier.size(15.dp),
        )
    }
}
