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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.cos
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
    // 单色标题（默认）用同一档亮度，避免开屏上又出现「前面白、后面灰」。
    val titleAccent = if (themeConfig.homeTitleColorAccent == themeConfig.homeTitleColorPrimary) {
        titlePrimary
    } else {
        splashInk(themeConfig.homeTitleColorAccent, 0.55f)
    }
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
        InkBloom(
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

private const val TAU = 6.2831855f

/** 墨滴接触水面的时刻（占整段动画的比例），前后节奏都以它为锚点。 */
private const val INK_IMPACT = 0.16f

/** 0→1 的平滑过渡。 */
private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    if (edge1 <= edge0) return if (x < edge0) 0f else 1f
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * 一团墨的轮廓：半径按三道低频正弦起伏，再压扁一点。
 *
 * 墨在水里、在纸上都不会是正圆，边缘总在几个方向上鼓起、在另几个方向凹进去。
 * 三道正弦（3 次、5 次谐波）能造出这种不对称；再用中点 + 二次贝塞尔连起来，
 * 轮廓就是圆滑的，没有多边形折角。
 */
private fun inkBlobPath(
    cx: Float,
    cy: Float,
    radius: Float,
    wobble: Float,
    phase: Float,
    squeeze: Float = 0.88f,
): Path {
    val n = 12
    val xs = FloatArray(n)
    val ys = FloatArray(n)
    for (i in 0 until n) {
        val a = TAU * i / n
        val wob = 0.62f * sin(3f * a + phase) + 0.38f * sin(5f * a + phase * 1.7f)
        val r = radius * (1f + wobble * wob)
        xs[i] = cx + cos(a) * r
        ys[i] = cy + sin(a) * r * squeeze
    }
    val path = Path()
    path.moveTo((xs[n - 1] + xs[0]) / 2f, (ys[n - 1] + ys[0]) / 2f)
    for (i in 0 until n) {
        val j = (i + 1) % n
        path.quadraticBezierTo(
            xs[i], ys[i],
            (xs[i] + xs[j]) / 2f, (ys[i] + ys[j]) / 2f,
        )
    }
    path.close()
    return path
}

/**
 * 一滴墨。
 *
 * 前两版都不对：一版是「墨团 + 直射线」，像光芒；一版是一笔横贯全屏的笔触，
 * 像一道灰杠。共同的问题是**把墨画成了有明确边界的图形**。
 *
 * 这一版画的是「墨滴落进水里」：
 *
 *  1. **落** —— 墨滴从上方坠下，带一条细尾；
 *  2. **触** —— 接触水面，压出一圈很淡的水纹；
 *  3. **绽** —— 落点先浓成一小团墨核，再由内向外散成三层越来越淡的墨云，
 *     每层的轮廓都是不规则闭合曲线，不是圆；
 *  4. **散** —— 图标浮起时墨被水冲淡，只留一层很浅的雾。
 *
 * 刻意不画触须、不画墨丝：一分叉就像水母，墨的意象立刻就没了。
 *
 * 所有浓度都压在 0.34 以内：墨是**淡**的才高级，亮了就成了发光的球。
 *
 * [progress] 是读取函数而不是值：在 draw 阶段取，避免整屏每帧重组。
 */
@Composable
private fun InkBloom(
    progress: () -> Float,
    ink: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val p = progress().coerceIn(0f, 1f)
        if (p <= 0.001f) return@Canvas

        val r0 = size.minDimension
        val cx = size.width / 2f
        val cy = size.height * 0.455f
        val fall = (p / INK_IMPACT).coerceIn(0f, 1f)
        val bloom = smoothstep(INK_IMPACT, 0.64f, p)
        val life = 1f - 0.90f * smoothstep(0.56f, 1f, p)

        // ---- 落墨：墨滴与细尾 -----------------------------------------
        if (p < INK_IMPACT + 0.04f) {
            val dropAlpha = 0.55f * smoothstep(0f, 0.35f, fall) *
                (1f - smoothstep(0.86f, 1f, fall))
            if (dropAlpha > 0.002f) {
                val y = cy - r0 * 0.48f + r0 * 0.48f * (fall * fall)
                val tail = r0 * 0.075f * (1f - fall) * (1f - fall * 0.45f)
                drawLine(
                    color = ink.copy(alpha = dropAlpha * 0.18f),
                    start = Offset(cx, y - tail),
                    end = Offset(cx, y - 1.dp.toPx()),
                    strokeWidth = 1.7.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = ink.copy(alpha = dropAlpha),
                    radius = (2.0f + 0.7f * fall).dp.toPx(),
                    center = Offset(cx, y),
                )
            }
        }

        // ---- 水纹：接触水面后推开的一圈 --------------------------------
        val ripple = smoothstep(INK_IMPACT, 0.66f, p)
        if (ripple > 0.001f && ripple < 0.999f) {
            val rx = r0 * (0.09f + 0.30f * ripple)
            val ry = rx * 0.34f
            drawOval(
                color = ink.copy(alpha = 0.10f * (1f - ripple) * (1f - ripple)),
                topLeft = Offset(cx - rx, cy - ry),
                size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        // ---- 墨云：三层，由内向外越来越淡 ------------------------------
        // scale / 浓度 / 起伏 / 相位：外层更大更淡、边缘起伏更夸张
        val layers = arrayOf(
            floatArrayOf(0.300f, 0.30f, 0.16f, 0.40f),
            floatArrayOf(0.400f, 0.13f, 0.22f, 1.90f),
            floatArrayOf(0.500f, 0.055f, 0.27f, 3.60f),
        )
        layers.forEachIndexed { index, layer ->
            val grow = smoothstep(INK_IMPACT + index * 0.03f, 0.60f + index * 0.05f, p)
            if (grow <= 0.01f) return@forEachIndexed
            val radius = r0 * layer[0] * (0.32f + 0.68f * grow)
            val density = layer[1] * grow * life
            if (radius <= 0.5f || density <= 0.002f) return@forEachIndexed
            // 墨团不是静止的：洇的时候轮廓会慢慢变，相位跟着进度走
            val path = inkBlobPath(
                cx = cx,
                cy = cy + r0 * (0.012f * index),
                radius = radius,
                wobble = layer[2],
                phase = layer[3] + p * 0.65f,
                squeeze = 0.82f,
            )
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to ink.copy(alpha = density),
                        0.42f to ink.copy(alpha = density * 0.80f),
                        0.74f to ink.copy(alpha = density * 0.32f),
                        1.00f to Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
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
