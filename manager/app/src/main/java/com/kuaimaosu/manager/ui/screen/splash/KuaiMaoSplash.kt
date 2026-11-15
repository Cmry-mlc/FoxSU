package com.kuaimaosu.manager.ui.screen.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.theme.BackgroundEffect
import com.kuaimaosu.manager.ui.theme.BackgroundEffectLayer
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.theme.isInDarkTheme
import org.koin.compose.koinInject

/**
 * KuaiMaoSU 冷启动首屏。
 *
 * 底色跟随明暗主题（浅色纯白 / 深色深靛），标题文字、分色与配色全部读同一份
 * 「主页标题」设置，所以改标题、改两个颜色，启动页会一起变。
 * 背景上还会叠当前选中的动态特效，跟主界面保持同一套视觉。
 */
@Composable
fun KuaiMaoSplash(
    modifier: Modifier = Modifier,
    stateText: String = "",
) {
    val themeConfig: ThemeConfig = koinInject()
    val isInDark = isInDarkTheme(themeConfig.forceDarkMode)
    val progress = remember { Animatable(0f) }
    // 光斑缓慢呼吸：即使没开全局特效，启动页也不会是一张死图
    val breathe by rememberInfiniteTransition(label = "splashGlow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splashGlowValue",
    )

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        )
    }

    // 与主界面共用同一份标题配置：文字、切分位置、两个颜色都一致
    val title = themeConfig.homeTitle.ifBlank { "KuaiMaoSU" }
    val split = themeConfig.homeTitleSplit.coerceIn(0, title.length)
    // 深色启动屏是纯黑，用户按白底挑的深色标题会直接消失，按亮度往白里提一档
    fun splashInk(argb: Int, lift: Float): Color {
        val raw = Color(argb)
        return if (isInDark && raw.luminance() < 0.55f) lerp(raw, Color.White, lift) else raw
    }
    val titlePrimary = splashInk(themeConfig.homeTitleColorPrimary, 0.88f)
    val titleAccent = splashInk(themeConfig.homeTitleColorAccent, 0.55f)
    val titleText = remember(title, split, titlePrimary, titleAccent) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = titlePrimary)) { append(title.take(split)) }
            withStyle(SpanStyle(color = titleAccent)) { append(title.drop(split)) }
        }
    }

    // 与全站画布同色：浅色 #F0F1F3、深色纯黑，冷启动到首屏不会闪一下别的底色
    val background = if (isInDark) Color(0xFF000000) else Color(0xFFF0F1F3)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background),
    ) {
        // 动态背景特效：与主界面同一个组件、同一个枚举
        if (themeConfig.backgroundEffect != BackgroundEffect.NONE) {
            BackgroundEffectLayer(
                effect = themeConfig.backgroundEffect,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 光斑：右上紫、左下绿，与大卡同一光源；浅色主题下压到很淡，避免发脏
        Canvas(modifier = Modifier.fillMaxSize()) {
            val violet = if (isInDark) Color(0x806C5CF0) else Color(0x2E6C5CF0)
            val jade = if (isInDark) Color(0x571CA672) else Color(0x241CA672)
            val swell = 0.94f + 0.10f * breathe
            val topCenter = Offset(size.width * 0.84f, size.height * (0.06f - 0.02f * breathe))
            val topRadius = size.minDimension * 0.62f * swell
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(violet, Color.Transparent),
                    center = topCenter,
                    radius = topRadius,
                ),
                radius = topRadius,
                center = topCenter,
            )
            val bottomCenter = Offset(size.width * 0.02f, size.height * (0.94f - 0.03f * breathe))
            val bottomRadius = size.minDimension * 0.55f * (1.04f - 0.08f * breathe)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(jade, Color.Transparent),
                    center = bottomCenter,
                    radius = bottomRadius,
                ),
                radius = bottomRadius,
                center = bottomCenter,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            KuaiMaoMark(
                progress = progress.value,
                modifier = Modifier.size(104.dp),
                warpColor = if (isInDark) Color(0xFF8CF0FF) else Color(0xFF3E8CE8),
                weftColor = if (isInDark) Color(0xFFA78BFF) else Color(0xFF6B57E0),
                knotColor = if (isInDark) Color(0xFFFFD9A0) else Color(0xFFE9A23B),
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = titleText,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "KERNEL ROOT WORKBENCH",
                color = titlePrimary.copy(alpha = if (isInDark) 0.40f else 0.52f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.2.sp,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stateText,
                color = titlePrimary.copy(alpha = if (isInDark) 0.35f else 0.45f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 经纬标记：经线落下 → 纬线折出 → 纬纱穿过 → 结点亮起。 */
@Composable
fun KuaiMaoMark(
    progress: Float,
    modifier: Modifier = Modifier,
    warpColor: Color = Color(0xFF8CF0FF),
    weftColor: Color = Color(0xFFA78BFF),
    knotColor: Color = Color(0xFFFFD9A0),
) {
    Canvas(modifier = modifier) {
        drawKuaiMaoMark(progress, warpColor, weftColor, knotColor)
    }
}

private fun DrawScope.drawKuaiMaoMark(
    progress: Float,
    warpColor: Color,
    weftColor: Color,
    knotColor: Color,
) {
    val unit = size.minDimension / 108f
    fun p(x: Float, y: Float) = Offset(x * unit, y * unit)

    // 经线：自上而下生长
    val warpT = (progress / 0.45f).coerceIn(0f, 1f)
    if (warpT > 0f) {
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(warpColor, warpColor.copy(alpha = 0.72f)),
                startY = 34f * unit,
                endY = 70f * unit,
            ),
            start = p(44f, 34f),
            end = p(44f, 34f + (69f - 34f) * warpT),
            strokeWidth = 9.5f * unit,
            cap = StrokeCap.Round,
        )
    }

    // 纬线：自左而右折出，构成 L 的横笔
    val weftT = ((progress - 0.35f) / 0.45f).coerceIn(0f, 1f)
    if (weftT > 0f) {
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(weftColor, weftColor.copy(alpha = 0.80f)),
                startX = 44f * unit,
                endX = 76f * unit,
            ),
            start = p(44f, 69f),
            end = p(44f + (76f - 44f) * weftT, 69f),
            strokeWidth = 9.5f * unit,
            cap = StrokeCap.Round,
        )
    }

    // 穿过经线的纬纱
    val yarnT = ((progress - 0.6f) / 0.25f).coerceIn(0f, 1f)
    if (yarnT > 0f) {
        drawLine(
            color = weftColor.copy(alpha = 0.55f),
            start = p(33f, 50f),
            end = p(33f + (58f - 33f) * yarnT, 50f),
            strokeWidth = 3.2f * unit,
            cap = StrokeCap.Round,
        )
    }

    // 结点
    val knotT = ((progress - 0.72f) / 0.28f).coerceIn(0f, 1f)
    if (knotT > 0f) {
        val radius = 5.2f * unit * (0.6f + 0.4f * knotT)
        drawCircle(
            color = knotColor.copy(alpha = 0.18f * knotT),
            radius = radius * 2.6f,
            center = p(44f, 50f),
        )
        drawCircle(
            color = knotColor.copy(alpha = knotT),
            radius = radius,
            center = p(44f, 50f),
        )
        drawCircle(
            color = knotColor.copy(alpha = 0.4f * knotT),
            radius = radius * 1.7f,
            center = p(44f, 50f),
            style = Stroke(width = 1.2f * unit),
        )
    }
}
