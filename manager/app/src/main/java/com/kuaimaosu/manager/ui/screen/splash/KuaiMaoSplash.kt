package com.kuaimaosu.manager.ui.screen.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.ui.theme.BackgroundEffect
import com.kuaimaosu.manager.ui.theme.BackgroundEffectLayer
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.LocalBackgroundRenderState
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.theme.isInDarkTheme
import kotlin.math.PI
import kotlin.math.sin
import org.koin.compose.koinInject

/** 水墨扩散的时长。要比 SPLASH_MIN_DURATION_MS 短一点，否则没画完就被淡出盖掉。 */
private const val INK_DURATION_MS = 1350

/**
 * KuaiMaoSU 冷启动首屏。
 *
 * 分两拍：先是墨从中心晕开、墨丝抽出来、墨点溅出去，
 * 墨团铺开之后，图标与标题才从这团墨里浮出来。
 *
 * 底色优先用自定义壁纸（亮壁纸压一层白纱、暗壁纸压一层黑纱），
 * 没有壁纸才退回主题底色，所以「亮色壁纸 + 浅色界面」不会仍然是一块深色屏。
 */
@Composable
fun KuaiMaoSplash(
    modifier: Modifier = Modifier,
    stateText: String = "",
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val renderState = LocalBackgroundRenderState.current
    val darkTheme = isInDarkTheme(themeConfig.forceDarkMode)

    val wallpaper = if (cardConfig.isCustomBackgroundEnabled) renderState.imageBitmap else null
    // 有壁纸就听壁纸的：亮壁纸用深墨 + 深字，暗壁纸（或夜间）用白墨 + 白字。
    val darkCanvas = if (wallpaper != null) renderState.wallpaperLuminance < 0.52f else darkTheme

    val ink = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        // 线性推进，各段各自的缓动在绘制里算，避免整体节奏被一条曲线拉平。
        ink.animateTo(1f, tween(durationMillis = INK_DURATION_MS, easing = LinearEasing))
    }
    // 壁纸是异步解码的，到位后淡进来，不要「啪」地换一张底。
    val wallpaperIn by animateFloatAsState(
        targetValue = if (wallpaper != null) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "splashWallpaper",
    )

    // 光斑呼吸：即使水墨已经散完，画面也不会是一张死图
    val breathe by rememberInfiniteTransition(label = "splashGlow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splashGlowValue",
    )

    // 与主界面共用同一份标题配置：文字、切分位置、两个颜色都一致
    val title = themeConfig.homeTitle.ifBlank { "KuaiMaoSU" }
    val split = themeConfig.homeTitleSplit.coerceIn(0, title.length)
    fun splashInk(argb: Int, lift: Float): Color {
        val raw = Color(argb)
        return if (darkCanvas && raw.luminance() < 0.55f) {
            lerp(raw, Color.White, lift)
        } else if (!darkCanvas && raw.luminance() > 0.62f) {
            // 亮画布上白字会消失，往下压同样一档
            lerp(raw, Color(0xFF101318), lift)
        } else {
            raw
        }
    }
    val titlePrimary = splashInk(themeConfig.homeTitleColorPrimary, 0.88f)
    val titleAccent = splashInk(themeConfig.homeTitleColorAccent, 0.55f)
    val titleText = remember(title, split, titlePrimary, titleAccent) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = titlePrimary)) { append(title.take(split)) }
            withStyle(SpanStyle(color = titleAccent)) { append(title.drop(split)) }
        }
    }

    val baseColor = if (darkCanvas) Color(0xFF000000) else Color(0xFFF0F1F3)
    // 墨：暗画布用偏冷的白墨，亮画布用近黑的墨
    val inkColor = if (darkCanvas) Color(0xFFEAF0FF) else Color(0xFF10141A)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor),
    ) {
        // ---- 自定义壁纸 -------------------------------------------------
        if (wallpaper != null) {
            Image(
                bitmap = wallpaper,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = wallpaperIn },
            )
            // 纱：亮壁纸压白、暗壁纸压黑。墨与字都靠这层把对比度拉回来，
            // 否则白墨落在亮壁纸上、黑墨落在暗壁纸的边缘上都会糊掉。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (darkCanvas) {
                            Color.Black.copy(alpha = 0.40f * wallpaperIn)
                        } else {
                            Color.White.copy(alpha = 0.36f * wallpaperIn)
                        },
                    ),
            )
        }

        // 动态背景特效：与主界面同一个组件、同一个枚举
        if (themeConfig.backgroundEffect != BackgroundEffect.NONE) {
            BackgroundEffectLayer(
                effect = themeConfig.backgroundEffect,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 光斑：右上紫、左下绿，与大卡同一光源；浅色下压到很淡，避免发脏
        Canvas(modifier = Modifier.fillMaxSize()) {
            val violet = if (darkCanvas) Color(0x806C5CF0) else Color(0x2E6C5CF0)
            val jade = if (darkCanvas) Color(0x571CA672) else Color(0x241CA672)
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

        // ---- 水墨 -------------------------------------------------------
        InkStroke(
            progress = { ink.value },
            ink = inkColor,
            modifier = Modifier.fillMaxSize(),
        )

        // ---- 墨散开后浮出的图标与标题 -----------------------------------
        // alpha / 缩放都在 graphicsLayer 里读动画值：每帧只更新图层，不重组文字。
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    val p = ink.value
                    val appear = ((p - 0.50f) / 0.30f).coerceIn(0f, 1f)
                    alpha = appear
                    val s = 0.86f + 0.14f * appear
                    scaleX = s
                    scaleY = s
                    translationY = (1f - appear) * 12.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            KuaiMaoMark(
                progress = 1f,
                modifier = Modifier.size(104.dp),
                plate = true,
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = titleText,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = ((ink.value - 0.62f) / 0.28f).coerceIn(0f, 1f)
                },
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "KERNEL ROOT WORKBENCH",
                color = titlePrimary.copy(alpha = if (darkCanvas) 0.40f else 0.52f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.2.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = ((ink.value - 0.70f) / 0.24f).coerceIn(0f, 1f)
                },
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
                color = titlePrimary.copy(alpha = if (darkCanvas) 0.35f else 0.45f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = ((ink.value - 0.78f) / 0.20f).coerceIn(0f, 1f)
                },
            )
        }
    }
}

/** 一笔墨从起笔到收笔所占的进度。 */
private const val INK_SWEEP_END = 0.54f

/** 0→1 的平滑过渡，比线性更像墨慢慢洇开。 */
private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    if (edge1 <= edge0) return if (x < edge0) 0f else 1f
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** 笔身上采样点的个数：够了就不必再加，多了只是白算。 */
private const val INK_STEPS = 64

/**
 * 一笔墨。
 *
 * 之前那版是「墨团 + 十几根直射线 + 溅点」：直射线看着像光芒，一团光斑加放射线，
 * 怎么调都不像水墨。这一版换成一笔真正的笔触：
 *
 *  1. **起笔** —— 笔尖从左侧落纸，由细到粗，边缘带一点毛刺（干笔）；
 *  2. **行笔** —— 最厚的一段扫过屏幕中央，中段轻拱，收笔时向上提；
 *  3. **洇** —— 笔身外再套一层更宽、更淡的同形轮廓，模拟墨在宣纸上渗出的那圈；
 *  4. **淡** —— 图标浮起时墨褪成很浅的一层，不跟文字抢。
 *
 * 笔形是「中线 + 变化的半宽」围成的闭合路径，半宽由压力曲线乘几道低频正弦，
 * 所以边缘是起伏的毛边，而不是一条光滑的椭圆弧。
 *
 * [progress] 是读取函数而不是值：在 draw 阶段取，避免整屏每帧重组。
 */
@Composable
private fun InkStroke(
    progress: () -> Float,
    ink: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val p = progress().coerceIn(0f, 1f)
        val sweep = smoothstep(0f, INK_SWEEP_END, p)
        if (sweep <= 0.002f) return@Canvas

        val baseY = size.height * 0.455f
        val startX = size.width * 0.075f
        val endX = size.width * 0.925f
        val halfMax = size.minDimension * 0.052f
        // 起笔后最浓，图标浮出时褪到很浅；留一点残余，画面不会突然变空。
        val inkAlpha = 1f - 0.90f * smoothstep(0.46f, 0.94f, p)

        val n = INK_STEPS + 1
        val xs = FloatArray(n)
        val ys = FloatArray(n)
        val hs = FloatArray(n)
        for (i in 0 until n) {
            val u = sweep * i / INK_STEPS
            // 压力曲线：起笔轻 → 行笔厚 → 收笔提起来
            val head = smoothstep(0f, 0.09f, u)
            val body = 0.58f + 0.42f * smoothstep(0.08f, 0.32f, u)
            val tail = 1f - 0.84f * smoothstep(0.70f, 1f, u)
            // 毛边：几道低频起伏，幅度跟着笔画厚度一起收敛
            val rough = 1f + 0.075f * sin(u * 19.7f) +
                0.045f * sin(u * 37.3f + 1.7f) + 0.028f * sin(u * 61.1f + 0.4f)
            xs[i] = startX + (endX - startX) * u
            // 中段轻拱、收笔上扬：一笔才不像尺子画出来的横线
            ys[i] = baseY - size.height * 0.010f * sin(u * PI.toFloat()) -
                size.height * 0.020f * u * u
            hs[i] = halfMax * (body * head * tail * rough).coerceAtLeast(0f)
        }

        // 同一个中线，换个半宽就是另一条轮廓：洇边与笔身共用一次采样。
        fun pathFor(scale: Float): Path {
            val path = Path()
            path.moveTo(xs[0], ys[0] - hs[0] * scale)
            for (i in 1 until n) path.lineTo(xs[i], ys[i] - hs[i] * scale)
            for (i in n - 1 downTo 0) path.lineTo(xs[i], ys[i] + hs[i] * scale)
            path.close()
            return path
        }

        // 洇边：宽一圈、淡得多，就是墨渗出去的那层
        drawPath(path = pathFor(1.55f), color = ink.copy(alpha = 0.05f * inkAlpha))
        drawPath(path = pathFor(1f), color = ink.copy(alpha = 0.24f * inkAlpha))

        // 笔尖：行笔时笔肚停在最前面，带一小团柔光
        val tipAlpha = 0.15f * inkAlpha * (1f - smoothstep(0.84f, 1f, sweep))
        if (tipAlpha > 0.002f) {
            val tip = Offset(xs[n - 1], ys[n - 1])
            val tipR = halfMax * 2.2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ink.copy(alpha = tipAlpha), Color.Transparent),
                    center = tip,
                    radius = tipR,
                ),
                radius = tipR,
                center = tip,
            )
        }
    }
}

/**
 * KuaiMaoSU 品牌标记：白色 K 与猫头（与桌面图标同一个图形）。
 *
 * @param plate 是否自带黑色圆角底板。标题栏外面已经有深色头像底，
 *              不需要再叠一层；启动页背景是浅色，需要底板才能看清白 K。
 * @param progress 0→1 的入场进度，做淡入与轻微放大。
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun KuaiMaoMark(
    progress: Float,
    modifier: Modifier = Modifier,
    plate: Boolean = false,
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
