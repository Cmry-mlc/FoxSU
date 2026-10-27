package me.weishu.kernelsu.ui.screen.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import kotlin.math.absoluteValue

private const val PAGE_COUNT = 3

/**
 * 首次安装后的欢迎引导（Dialog 承载，只在 OnboardingPrefs 未标记时出现一次）。
 *
 * 版式取自应用自身：顶部是小狐徽标 + 等宽步骤计数，正文大字标题 + 说明，
 * 底部一条三段式进度条和主色药丸按钮；配色全部跟随当前主题。
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
    val pager = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val accent = cs.primary
    val narrow = LocalConfiguration.current.screenWidthDp < 380

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.05f),
                    radius = size.width * 0.95f,
                )
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.09f), Color.Transparent),
                    center = Offset(size.width * 0.04f, size.height * 0.94f),
                    radius = size.width * 0.8f,
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = if (narrow) 22.dp else 28.dp),
        ) {
            OnboardingHeader(page = pager.currentPage, accent = accent)

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1,
            ) { index ->
                OnboardingPage(
                    index = index,
                    offset = (pager.currentPage - index + pager.currentPageOffsetFraction)
                        .absoluteValue
                        .coerceIn(0f, 1f),
                    accent = accent,
                    narrow = narrow,
                )
            }

            SegmentedProgress(page = pager.currentPage, accent = accent)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = cs.onBackground.copy(alpha = 0.48f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onFinish)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                )
                Spacer(Modifier.weight(1f))
                val last = pager.currentPage == PAGE_COUNT - 1
                Surface(
                    shape = CircleShape,
                    color = accent,
                    contentColor = cs.onPrimary,
                    onClick = {
                        if (last) {
                            onFinish()
                        } else {
                            scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                        }
                    },
                ) {
                    Text(
                        text = stringResource(
                            if (last) R.string.onboarding_start else R.string.onboarding_next
                        ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 26.dp, vertical = 13.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeader(page: Int, accent: Color) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(cs.onBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_fox_glyph),
                contentDescription = null,
                tint = cs.background,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "FoxSu",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            color = cs.onBackground,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "0${page + 1} / 0$PAGE_COUNT",
            fontSize = 10.sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Bold,
            color = accent.copy(alpha = 0.9f),
        )
    }
}

/** 分段进度：三段圆角条，当前段拉长 */
@Composable
private fun SegmentedProgress(page: Int, accent: Color) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(PAGE_COUNT) { i ->
            val active = i <= page
            val w by animateFloatAsState(
                targetValue = if (i == page) 30f else 16f,
                animationSpec = tween(260),
                label = "seg",
            )
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .width(w.dp)
                    .background(
                        color = if (active) accent else cs.onBackground.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

@Composable
private fun OnboardingPage(index: Int, offset: Float, accent: Color, narrow: Boolean) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = lerp(1f, 0.15f, offset)
                translationY = offset * 26.dp.toPx()
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (index) {
                0 -> FoxMarkIllustration(accent)
                1 -> FlashFlowIllustration(accent)
                else -> CapabilityIllustration(accent)
            }
        }
        Text(
            text = stringResource(
                when (index) {
                    0 -> R.string.onboarding_kicker_1
                    1 -> R.string.onboarding_kicker_2
                    else -> R.string.onboarding_kicker_3
                }
            ),
            fontSize = 9.5.sp,
            letterSpacing = 2.4.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(
                when (index) {
                    0 -> R.string.onboarding_title_1
                    1 -> R.string.onboarding_title_2
                    else -> R.string.onboarding_title_3
                }
            ),
            fontSize = if (narrow) 26.sp else 29.sp,
            lineHeight = if (narrow) 32.sp else 35.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.8).sp,
            color = cs.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(
                when (index) {
                    0 -> R.string.onboarding_body_1
                    1 -> R.string.onboarding_body_2
                    else -> R.string.onboarding_body_3
                }
            ),
            fontSize = 13.5.sp,
            lineHeight = 21.sp,
            color = cs.onBackground.copy(alpha = 0.64f),
        )
        Spacer(Modifier.height(20.dp))
    }
}

/** 第一页：狐首徽标 + 环绕七彩细弧 */
@Composable
private fun FoxMarkIllustration(accent: Color) {
    val cs = MaterialTheme.colorScheme
    Box(modifier = Modifier.size(206.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val d = size.minDimension
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFFFF7A59), Color(0xFFFFC65C), Color(0xFF6EF0A0),
                        Color(0xFF5CE1E6), Color(0xFF7AA2FF), Color(0xFFC77DFF),
                        Color(0xFFFF7AC8), Color(0xFFFF7A59),
                    )
                ),
                radius = d / 2 - 2.dp.toPx(),
                style = Stroke(width = 1.4.dp.toPx()),
                alpha = 0.7f,
            )
            drawCircle(color = accent.copy(alpha = 0.08f), radius = d / 2 - 24.dp.toPx())
        }
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            tint = cs.onBackground,
            modifier = Modifier.size(104.dp),
        )
    }
}

/** 第二页：✕ → ✓ 的刷写前后对照 */
@Composable
private fun FlashFlowIllustration(accent: Color) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusBubble(
            color = cs.error,
            icon = R.drawable.ic_d_cross,
            label = stringResource(R.string.home_not_installed),
            container = cs.error.copy(alpha = 0.10f),
        )
        Column(
            modifier = Modifier
                .width(78.dp)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
            ) {
                val y = size.height / 2
                drawLine(
                    color = accent.copy(alpha = 0.55f),
                    start = Offset(0f, y),
                    end = Offset(size.width - 11.dp.toPx(), y),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = accent.copy(alpha = 0.55f),
                    start = Offset(size.width - 13.dp.toPx(), y - 4.dp.toPx()),
                    end = Offset(size.width - 6.dp.toPx(), y),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = accent.copy(alpha = 0.55f),
                    start = Offset(size.width - 13.dp.toPx(), y + 4.dp.toPx()),
                    end = Offset(size.width - 6.dp.toPx(), y),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            Text(
                text = stringResource(R.string.onboarding_flash_action),
                fontSize = 9.sp,
                letterSpacing = 0.8.sp,
                color = cs.onBackground.copy(alpha = 0.45f),
            )
        }
        StatusBubble(
            color = Color(0xFF2FA36B),
            icon = R.drawable.ic_d_check,
            label = stringResource(R.string.home_working),
            container = Color(0xFF2FA36B).copy(alpha = 0.12f),
        )
    }
}

@Composable
private fun StatusBubble(color: Color, icon: Int, label: String, container: Color) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .background(container, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(74.dp)) {
                drawCircle(
                    color = color.copy(alpha = 0.55f),
                    radius = size.minDimension / 2 - 1.dp.toPx(),
                    style = Stroke(width = 1.4.dp.toPx()),
                )
            }
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = cs.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
        )
    }
}

/** 第三页：能力清单 */
@Composable
private fun CapabilityIllustration(accent: Color) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(0.88f),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf(
            Icons.Rounded.Shield to R.string.onboarding_cap_superuser,
            Icons.Rounded.Widgets to R.string.onboarding_cap_module,
            Icons.Rounded.Lock to R.string.onboarding_cap_security,
        ).forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cs.onBackground.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(accent.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = pair.first,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(pair.second),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = cs.onBackground.copy(alpha = 0.86f),
                )
            }
        }
    }
}
