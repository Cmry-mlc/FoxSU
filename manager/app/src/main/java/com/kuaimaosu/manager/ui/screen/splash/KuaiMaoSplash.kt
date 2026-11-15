package com.kuaimaosu.manager.ui.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.kuaimaosu.manager.R
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
                plate = true,
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

/**
 * KuaiMaoSU 品牌标记：白色 K 与猫头（与桌面图标同一个图形）。
 *
 * 图形取自品牌标志原图，不再是旧的「经纬」示意，所以标题栏、启动页和
 * 桌面图标看到的是同一个符号。
 *
 * @param plate 是否自带黑色圆角底板。标题栏外面已经有深色头像底，
 *              不需要再叠一层；启动页背景是浅色，需要底板才能看清白 K。
 * @param progress 0→1 的入场进度，做淡入与轻微放大，替代旧的逐笔绘制。
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun KuaiMaoMark(
    progress: Float,
    modifier: Modifier = Modifier,
    plate: Boolean = false,
    warpColor: Color = Color(0xFF8CF0FF),
    weftColor: Color = Color(0xFFA78BFF),
    knotColor: Color = Color(0xFFFFD9A0),
) {
    val t = progress.coerceIn(0f, 1f)
    val scale = 0.88f + 0.12f * t
    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = 0.30f + 0.70f * t
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (plate) {
                    Modifier
                        .clip(RoundedCornerShape(percent = 22))
                        .background(Color.Black)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_kuaimao_mark),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(if (plate) 0.78f else 1f),
        )
    }
}
