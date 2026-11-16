package com.kuaimaosu.manager.ui.screen.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import com.kuaimaosu.manager.ui.util.KuaiMaoPoetry
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.data.theme.ThemeRepository
import com.kuaimaosu.manager.ui.screen.splash.KuaiMaoMark
import com.kuaimaosu.manager.ui.theme.LocalKuaiMaoPalette
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.LocalDarkBackdrop
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.contourGlow
import com.kuaimaosu.manager.ui.theme.rememberContourPhase
import com.kuaimaosu.manager.ui.theme.renderBackgroundBlur
import com.kuaimaosu.manager.ui.theme.KuaiMaoDimens
import com.kuaimaosu.manager.ui.theme.KuaiMaoGradients
import com.kuaimaosu.manager.ui.theme.KuaiMaoTone
import com.kuaimaosu.manager.ui.theme.KuaiMaoType
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import org.koin.compose.koinInject

/** 四宫格中的一个快捷入口 */
data class KuaiMaoQuickAction(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val tone: KuaiMaoTone,
    val onClick: () -> Unit,
)

/** 系统动态中的一行 */
data class KuaiMaoFeedItem(
    val key: String,
    val title: String,
    val desc: String,
    val value: String,
    val icon: ImageVector,
    val tone: KuaiMaoTone,
    val valueColor: Color,
    /** 说明文字允许的行数；「支持开发」「了解更多」这类整句说明用 2 行。 */
    val descMaxLines: Int = 1,
    /** 右侧是否补一个箭头，表示点击后离开当前页。 */
    val trailingChevron: Boolean = false,
    val onClick: (() -> Unit)? = null,
)

/** 顶部问候行：头像 + WELCOME BACK / KuaiMaoSU + 通知铃铛 */
@Composable
fun KuaiMaoGreetingRow(
    modifier: Modifier = Modifier,
    onAvatarClick: () -> Unit = {},
    onBellClick: () -> Unit = {},
    notificationCount: Int = 0,
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val kicker = themeConfig.homeKicker.ifBlank { ThemeRepository.DEFAULT_HOME_KICKER }
    val title = themeConfig.homeTitle.ifBlank { "KuaiMaoSU" }
    val split = themeConfig.homeTitleSplit.coerceIn(0, title.length)
    // 标题两段颜色是用户自选的十六进制色，默认值按白底设计（#101322）。
    // 夜间画布是纯黑，这一对颜色会直接消失，所以深色下按亮度把它提亮，
    // 但仍保留两段之间的明暗差，不至于两段变成同一种白。
    // 深浅不只看主题：开了自定义壁纸且壁纸偏暗时，这里同样要换成浅字，
    // 否则「KuaiMaoSU」是深蓝压在暗花丛上，整块标题糊成一团。
    val isDarkCanvas = LocalDarkBackdrop.current
    val titlePrimary = themeConfig.homeTitleColorPrimary.adaptTitleInk(isDarkCanvas, 0.92f)
    val titleAccent = themeConfig.homeTitleColorAccent.adaptTitleInk(isDarkCanvas, 0.62f)
    val titleText = buildAnnotatedString {
        withStyle(SpanStyle(color = titlePrimary)) {
            append(title.take(split))
        }
        withStyle(SpanStyle(color = titleAccent)) {
            append(title.drop(split))
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(KuaiMaoGradients.avatar)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center,
        ) {
            KuaiMaoMark(progress = 1f, modifier = Modifier.size(34.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            KuaiMaoKickerTicker(
                text = kicker,
                animated = themeConfig.kickerTicker,
                modifier = Modifier.widthIn(max = 200.dp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = titleText, style = KuaiMaoType.greeting)
        }

        Spacer(modifier = Modifier.weight(1f))

        // 自定义壁纸下不要再铺 canvasCard：夜间它是 #1C1C1C，压在壁纸上就是
        // 图标外面套了一个黑方块（截图里那个「黑圈」）。改成极薄的磨砂玻璃，
        // 只留一层雾气和细边，铃铛本身清清楚楚。
        val bellOnWallpaper = cardConfig.isCustomBackgroundEnabled
        val bellShape = RoundedCornerShape(13.dp)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(bellShape)
                // 不再用 elevation 阴影：离屏层 + 半透明底在部分 ROM 上会
                // 连带画出一块方形白底（图标周围那个小白块就是这么来的）。
                // 层次改由 1dp 描边提供。
                .background(
                    if (bellOnWallpaper) {
                        KuaiMaoColors.canvasCard.copy(alpha = 0.24f)
                    } else {
                        KuaiMaoColors.canvasCard
                    },
                )
                .then(
                    if (bellOnWallpaper) {
                        Modifier.renderBackgroundBlur(
                            tintColor = KuaiMaoColors.canvasCardSolid,
                            tintAlpha = 0.24f,
                        )
                    } else {
                        Modifier
                    },
                )
                .border(
                    width = 1.dp,
                    color = KuaiMaoColors.line.copy(alpha = if (bellOnWallpaper) 0.5f else 0.8f),
                    shape = bellShape,
                )
                .clickable(onClick = onBellClick),
            contentAlignment = Alignment.Center,
        ) {
            // 用项目内自绘的单色铃铛：material icon 在部分设备上会把
            // 矢量填充区域带成白色方底，换成纯 path 后不会有底板。
            Icon(
                painter = painterResource(R.drawable.ic_kuaimao_bell),
                contentDescription = "通知",
                tint = KuaiMaoColors.textLabel,
                modifier = Modifier.size(19.dp),
            )
            // 未读角标：正圆 13dp，贴在按钮右上角内侧
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-5).dp, y = 5.dp)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(KuaiMaoColors.danger),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (notificationCount > 9) "9+" else notificationCount.toString(),
                        fontSize = 7.5.sp,
                        lineHeight = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/**
 * 标题上方的小字：一条持续向左流动的跑马灯，并且每隔一段时间换一句诗。
 *
 * 用 basicMarquee 让整行文字循环滚动（内容超出 200dp 才滚，默认文案一定会超），
 * 关掉开关就退回单行省略，字号、字距、位置都不变。
 *
 * 换句和滚动是两个独立的事：第一句永远是用户填的那句，之后按 [KuaiMaoPoetry]
 * 随机取，取到和上一句重复的就再抽一次。切换用淡入淡出，不做位移，
 * 避免标题行跟着上下跳。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KuaiMaoKickerTicker(
    text: String,
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
    var current by remember(text) { mutableStateOf(text) }
    LaunchedEffect(text) {
        current = text
        var last = text
        while (true) {
            delay(KICKER_ROTATE_MILLIS)
            var next = KuaiMaoPoetry.random()
            var guard = 0
            while ((next == last || next == text) && guard < 8) {
                next = KuaiMaoPoetry.random()
                guard++
            }
            current = next
            last = next
        }
    }

    val style = KuaiMaoType.kicker.copy(letterSpacing = 2.sp)
    val scroll = if (animated) {
        Modifier.basicMarquee(
            iterations = Int.MAX_VALUE,
            velocity = 26.dp,
            initialDelayMillis = 700,
        )
    } else {
        Modifier
    }
    AnimatedContent(
        targetState = current,
        transitionSpec = {
            fadeIn(animationSpec = tween(520)) togetherWith fadeOut(animationSpec = tween(320))
        },
        label = "kicker",
        modifier = modifier,
    ) { line ->
        Text(
            text = line,
            style = style,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = scroll,
        )
    }
}

/** 小字换句间隔：够读完一句，又不至于半天不动。 */
private const val KICKER_ROTATE_MILLIS = 9_000L

/** 大卡 3D 倾斜的最大角度。再大文字会斜得读不清，7° 已是能看出来的上限。 */
private const val HERO_TILT_MAX_DEG = 7f

/** 一圈，粒子做正弦位移时用。 */
private const val HERO_TAU = 6.2831855f

/**
 * 大卡上的一颗漂浮尘埃。
 *
 * 坐标全部是卡片内的归一化比例（0..1），换机型、换字号都不用改数值；
 * [driftX] / [driftY] 是它绕锚点画的椭圆半径，[frequency] 与 [twinkleFrequency]
 * 只取整数——这样 30s 时间轴回到 0 时，位移与亮度都正好落回起点，循环无接缝。
 */
private class HeroDust(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    val driftX: Float,
    val driftY: Float,
    val frequency: Int,
    val phase: Float,
    val alpha: Float,
    val twinkleFrequency: Int,
    val twinklePhase: Float,
)

/**
 * 生成大卡上的尘埃群。
 *
 * 第一颗就是原来弧线右侧那颗孤零零的白点，现在让它绕原位慢慢漂；
 * 另外 21 颗随机撒满整张卡，锚点、大小、速度、亮度全部错开，
 * 但种子写死，所以每次进页面看到的是同一片星尘，不会「重刷」。
 */
private fun buildHeroDust(): List<HeroDust> {
    val random = Random(20261116)
    val list = ArrayList<HeroDust>(22)
    list += HeroDust(
        x = 0.94f,
        y = 0.42f,
        radiusDp = 2.6f,
        driftX = 0.032f,
        driftY = 0.046f,
        frequency = 1,
        phase = 0f,
        alpha = 0.56f,
        twinkleFrequency = 2,
        twinklePhase = 0.25f,
    )
    repeat(21) {
        list += HeroDust(
            x = 0.06f + random.nextFloat() * 0.88f,
            y = 0.05f + random.nextFloat() * 0.88f,
            radiusDp = 0.8f + random.nextFloat() * 1.5f,
            driftX = 0.012f + random.nextFloat() * 0.045f,
            driftY = 0.014f + random.nextFloat() * 0.050f,
            frequency = 1 + random.nextInt(3),
            phase = random.nextFloat(),
            alpha = 0.16f + random.nextFloat() * 0.34f,
            twinkleFrequency = 1 + random.nextInt(3),
            twinklePhase = random.nextFloat(),
        )
    }
    return list
}

/**
 * 内核状态大卡（对应设计稿的深色状态卡）。
 *
 * 深靛渐变 + 双光斑 + 右下弧线，主体是状态与两项指标。
 * [modeTags] 是真实运行模式的短标签（LKM / 内置 / 越狱模式 / 架构）。
 * 整张卡片就是一个入口：未工作时点它进入安装页，已工作时进入系统信息页。
 */
@Composable
fun KuaiMaoHeroCard(
    modifier: Modifier = Modifier,
    label: String,
    stateText: String,
    stateHint: String,
    badgeText: String?,
    badgeColor: Color,
    superuserCount: Int,
    moduleCount: Int,
    superuserLabel: String = "超级用户",
    moduleLabel: String = "模块",
    superuserUnit: String = "已授权",
    moduleUnit: String = "已安装",
    modeTags: List<String> = emptyList(),
    quickActions: List<KuaiMaoQuickAction> = emptyList(),
    isActivated: Boolean = false,
    onClick: () -> Unit = {},
) {
    val shape = KuaiMaoDimens.rHero
    val cardConfig: CardConfig = koinInject()
    val themeConfig: ThemeConfig = koinInject()
    // 流光轮廓：跟设置里的开关联动，关着的时候连相位动画都不创建。
    val contourPhase = rememberContourPhase(themeConfig.isEdgeLight)
    val glass = cardConfig.isCustomBackgroundEnabled
    val isDarkCanvas = LocalKuaiMaoPalette.current.canvasStage.luminance() < 0.5f
    // 亮色 + 自定义壁纸：大卡要跟下面那些卡片一样是「白底磨砂」，不能还是黑板。
    // 深色 + 壁纸才继续用深色磨砂。
    val lightGlass = glass && !isDarkCanvas
    // 卡片上的文字：亮色磨砂用墨色，深色卡用白字。
    val onHero = if (lightGlass) KuaiMaoColors.text else Color.White
    val onHeroSoft = if (lightGlass) {
        KuaiMaoColors.text.copy(alpha = 0.72f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }
    val onHeroFaint = if (lightGlass) {
        KuaiMaoColors.text.copy(alpha = 0.58f)
    } else {
        Color.White.copy(alpha = 0.62f)
    }
    val heroHairline = if (lightGlass) KuaiMaoColors.line else Color.White.copy(alpha = 0.12f)
    // 自定义壁纸下大卡改成半透明磨砂：壁纸的模糊版垫在下面，
    // 深色渐变只留一层薄色，不再是压在壁纸上的一块实心黑板。
    val fillAlpha = if (glass) 0.38f else 1f
    // 卡片颜色跟随主题色：以纯黑为底按比例混入强调色，
    // 所以默认（黑色主题）就是黑卡，换成绿/紫/红时会变成对应的深色卡片，
    // 而原有的弧线与光斑全部保留，只换色不动形。
    val accent = MaterialTheme.colorScheme.primary
    // 夜间画布是纯黑，强调色也换成了中性近白：这时若继续按比例混色，
    // 卡片会变成一块中灰板，白字反而发虚。深色下直接给固定的黑灰卡面，
    // 让它在纯黑背景上靠一层微亮和阴影浮起来。
    val deepBase = if (isDarkCanvas) Color(0xFF161618) else lerp(Color(0xFF05060A), accent, 0.30f)
    val midBase = if (isDarkCanvas) Color(0xFF242427) else lerp(Color(0xFF0E1120), accent, 0.50f)
    val edgeBase = if (isDarkCanvas) Color(0xFF121214) else lerp(Color(0xFF05060A), accent, 0.22f)
    val glowAlpha = if (isDarkCanvas) 0.16f else 0.52f

    // ---- 3D 透视倾斜 + 触摸流光 -------------------------------------
    // 倾斜量用 Animatable 直接喂给 graphicsLayer：拖动时 snapTo 只改那一层
    // 的旋转矩阵，不触发重组，卡片里几十个文字节点不会被反复 measure。
    val density = LocalDensity.current.density
    // 手指位置归一化到 -1..1：跟手时直接写这两个值，只走绘制层，不重组。
    var aimX by remember { mutableFloatStateOf(0f) }
    var aimY by remember { mutableFloatStateOf(0f) }
    // grip = 跟手强度。1 表示完全跟随手指，松手后弹簧退回 0，卡片就回正；
    // 欠阻尼的 spring 会越过 0 变负一点，于是卡片先反向歪一丝再停住，就是回弹。
    val grip = remember { Animatable(1f) }
    // 反射光强：按下淡入，松手淡出，避免手指一抬整片光突然消失。
    val reflect = remember { Animatable(0f) }
    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    val tiltScope = rememberCoroutineScope()

    // ---- 动效 --------------------------------------------------------
    // 按压回弹：整卡缩到 98.4%，松手弹回。卡片是首页最大的可点区域，
    // 没有这点反馈时点空白处完全不知道自己点到了。
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.984f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 720f),
        label = "heroPress",
    )
    // 呼吸与扫光共用一条无限循环：呼吸 9s 往返、扫光 12s 一轮，
    // 两个周期互质，画面不会每隔几秒回到完全一样的帧。
    val halo = rememberInfiniteTransition(label = "heroHalo")
    val breath by halo.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroBreath",
    )
    val sweep by halo.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "heroSweep",
    )
    // 状态角标的扩散环：只有「已激活」才画，2.2s 一轮。
    val pulse by halo.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "heroPulse",
    )
    // 尘埃的时间轴：30s 走一圈。粒子只取它的整数倍频做正弦位移，
    // 所以从 1 跳回 0 的那一帧，每颗点都正好回到起点，画面上看不出接缝。
    val dustTick by halo.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "heroDust",
    )
    // 锚点、半径、相位一次算好（固定种子），换主题、点卡片、转屏都不会重新洗牌。
    val dust = remember { buildHeroDust() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            // 缩放与 3D 倾斜共用一个 layer：阴影、磨砂、光斑整块跟着变，
            // 否则会出现「卡缩了影子不缩」「卡歪了高光还正着」。
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                // 朝手指方向倾：手指在右，右手边向后倒；手指在下，下沿向后倒。
                rotationX = -aimY * grip.value * HERO_TILT_MAX_DEG
                rotationY = aimX * grip.value * HERO_TILT_MAX_DEG
                // 默认 cameraDistance 是按小控件定的，7° 就能看出鱼眼；
                // 放大到 16 倍密度，透视感刚好，边缘不会被拉长。
                cameraDistance = 16f * density
                transformOrigin = TransformOrigin.Center
            }
            // 跟随手指：按下即倾斜、拖动时贴着手走，抬手弹簧回正。
            // 这里只读坐标不消费事件，点击 / 长按仍然照常交给下面的 clickable。
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val height = size.height.toFloat().coerceAtLeast(1f)

                    // 只写普通状态：拖动时不做动画，要的是「贴在手指上」，
                    // 中间再套一层阻尼会像拖着一块橡皮。
                    fun aimTo(point: Offset) {
                        touchPoint = point
                        aimX = ((point.x / width) * 2f - 1f).coerceIn(-1f, 1f)
                        aimY = ((point.y / height) * 2f - 1f).coerceIn(-1f, 1f)
                    }

                    aimTo(down.position)
                    tiltScope.launch {
                        // 上一次回正可能还没停，先取消再快速拉到跟手。
                        grip.animateTo(1f, tween(120))
                        reflect.animateTo(1f, tween(140))
                    }

                    while (true) {
                        val change = awaitPointerEvent().changes
                            .firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        aimTo(change.position)
                    }

                    // 抬手：grip 用低阻尼 spring 退回 0，会略微过冲再停住，
                    // 观感是卡片弹回原位，而不是硬切回平面。
                    tiltScope.launch {
                        grip.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(dampingRatio = 0.42f, stiffness = 240f),
                        )
                    }
                    tiltScope.launch {
                        reflect.animateTo(0f, tween(360))
                        touchPoint = null
                    }
                }
            }
            .shadow(
                // 磨砂卡不要再压一层 18dp 的厚影，否则又变成一块浮在壁纸上的黑板
                elevation = if (glass) 6.dp else 18.dp,
                shape = shape,
                clip = false,
                ambientColor = if (isDarkCanvas) Color.Black else accent.copy(alpha = 0.18f),
                spotColor = if (isDarkCanvas) Color.Black else accent.copy(alpha = 0.28f),
            )
            // 流光轮廓必须挂在 clip 外面：描边一半在边界外，被裁掉就只剩半条线。
            .contourGlow(
                cornerRadius = 26.dp,
                accent = accent,
                enabled = contourPhase != null,
                dark = !lightGlass,
                intensity = if (lightGlass) 0.86f else 1f,
                phase = { contourPhase?.value ?: 0f },
            )
            .clip(shape)
            .then(
                if (glass) {
                    Modifier.renderBackgroundBlur(
                        tintColor = if (lightGlass) Color.White else Color.Black,
                    )
                } else {
                    Modifier
                },
            )
            .drawBehind {
                // 呼吸系数：0.94 → 1.12，光斑半径与浓度同时涨落。
                val breathe = 0.94f + 0.18f * breath

                if (lightGlass) {
                    // 亮色壁纸：白卡上只留一层极淡的强调色光斑，其余交给磨砂层
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.10f * breathe), Color.Transparent),
                            center = Offset(size.width * 0.86f, size.height * 0.02f),
                            radius = size.width * 0.6f * breathe,
                        ),
                        radius = size.width * 0.6f * breathe,
                        center = Offset(size.width * 0.86f, size.height * 0.02f),
                    )
                } else {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                deepBase.copy(alpha = fillAlpha),
                                midBase.copy(alpha = fillAlpha),
                                edgeBase.copy(alpha = fillAlpha),
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width * 0.8f, size.height),
                        ),
                    )
                    // 右上主光斑（跟着呼吸涨落）
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = glowAlpha * fillAlpha * (0.82f + 0.28f * breath)),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.84f, -size.height * 0.06f),
                            radius = size.width * 0.62f * breathe,
                        ),
                        radius = size.width * 0.62f * breathe,
                        center = Offset(size.width * 0.84f, -size.height * 0.06f),
                    )
                    // 左下余光（互补色，反向呼吸，两块光不会同时最亮）
                    // 已去掉：偏红的补色光在深卡上像一层脏雾，只留右上主光斑。
                    // 扫光：12s 一轮，其中前 40% 真正扫过，其余时间停在卡外。
                    val travel = (sweep - 0.06f) / 0.42f
                    if (travel in 0f..1f) {
                        val band = size.width * 0.46f
                        val x = -band + (size.width + band * 2f) * travel
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.03f),
                                    Color.Transparent,
                                ),
                                start = Offset(x, 0f),
                                end = Offset(x + band, size.height),
                            ),
                        )
                    }
                }
                // 手指位置的径向反射：一大一小两圈叠着画，
                // 外圈负责把周围提亮，内圈是玻璃上那点锐利的反光核心。
                val point = touchPoint
                val strength = reflect.value
                if (point != null && strength > 0.01f) {
                    val reflectInk = if (lightGlass) accent else Color.White
                    val haloRadius = size.width * 0.66f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                reflectInk.copy(
                                    alpha = (if (lightGlass) 0.26f else 0.18f) * strength,
                                ),
                                reflectInk.copy(alpha = 0.05f * strength),
                                Color.Transparent,
                            ),
                            center = point,
                            radius = haloRadius,
                        ),
                        radius = haloRadius,
                        center = point,
                    )
                    val coreRadius = size.width * 0.20f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                reflectInk.copy(alpha = 0.20f * strength),
                                Color.Transparent,
                            ),
                            center = point,
                            radius = coreRadius,
                        ),
                        radius = coreRadius,
                        center = point,
                    )
                }
                // 顶部玻璃高光边：必须最后画，否则会被上面的满版底色盖住。
                val edgeInk = if (lightGlass) {
                    Color.Black.copy(alpha = 0.07f)
                } else {
                    Color.White.copy(alpha = 0.34f)
                }
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, edgeInk, Color.Transparent),
                    ),
                    start = Offset(0f, 0.5.dp.toPx()),
                    end = Offset(size.width, 0.5.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
    ) {
        // 右下角弧线装饰：铺满卡片但不参与测量
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val p1 = Path().apply {
                moveTo(w * 0.52f, h * 1.02f)
                cubicTo(w * 0.72f, h * 0.52f, w * 0.9f, h * 0.9f, w * 1.3f, h * 0.24f)
            }
            val p2 = Path().apply {
                moveTo(w * 0.55f, h * 1.18f)
                cubicTo(w * 0.76f, h * 0.66f, w * 0.94f, h * 1.04f, w * 1.32f, h * 0.42f)
            }
            val arcAlpha = if (lightGlass) 0.14f else 0.42f
            drawPath(p1, accent.copy(alpha = arcAlpha), style = Stroke(width = 1.4f))
            drawPath(p2, accent.copy(alpha = arcAlpha * 0.52f), style = Stroke(width = 1.2f))

            // 沿着主弧缓慢滑行的一颗流光点：位置跟 12s 的扫光共用相位，
            // 扫光从左上掠过时，光点正好走到弧的右半段，两处高光互相呼应。
            val measure = PathMeasure()
            measure.setPath(p1, false)
            if (measure.length > 0f) {
                val dot = measure.getPosition(measure.length * (0.10f + 0.80f * sweep))
                drawCircle(accent.copy(alpha = 0.30f), radius = 9f, center = dot)
                drawCircle(lerp(accent, Color.White, 0.72f), radius = 2.4f, center = dot)
            }

            // ---- 整卡漂浮的尘埃 --------------------------------------
            // 每颗点绕自己的锚点走一个小椭圆，亮度再按另一条相位呼吸，
            // 于是永远找不到两颗同步的点，看着是「浮」而不是「闪」。
            // 时间取自 30s 的 dustTick，频率一律取整数，循环回 0 时位置无接缝。
            val dustInk = if (lightGlass) accent else Color.White
            dust.forEach { p ->
                val angle = (dustTick * p.frequency + p.phase) * HERO_TAU
                val cx = (p.x + sin(angle) * p.driftX) * w
                val cy = (p.y + cos(angle) * p.driftY) * h
                val twinkle = 0.55f + 0.45f * sin(
                    (dustTick * p.twinkleFrequency + p.twinklePhase) * HERO_TAU,
                )
                val radius = p.radiusDp.dp.toPx()
                // 大颗粒外面套一圈柔光：远看是薄雾，近看才是一颗点。
                if (p.radiusDp > 1.7f) {
                    drawCircle(
                        color = dustInk.copy(alpha = p.alpha * twinkle * 0.22f),
                        radius = radius * 3.2f,
                        center = Offset(cx, cy),
                    )
                }
                drawCircle(
                    color = dustInk.copy(alpha = p.alpha * twinkle),
                    radius = radius,
                    center = Offset(cx, cy),
                )
            }
        }

        Column(
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 0.dp),
        ) {
            Text(
                text = label,
                style = KuaiMaoType.heroLabel,
                color = onHeroFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stateText,
                        style = KuaiMaoType.heroState,
                        color = onHero,
                        maxLines = 1,
                    )
                    if (stateHint.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stateHint,
                            style = KuaiMaoType.heroHint,
                            color = onHeroSoft,
                            maxLines = 1,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 状态角标：圆形底色与配色保持不变，中间只显示 ✓ / ×。
                // 已激活时外面套一层向外扩散的环，作为「正在运行」的呼吸。
                if (!badgeText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(26.dp)
                            .drawBehind {
                                if (isActivated) {
                                    drawCircle(
                                        color = badgeColor.copy(alpha = 0.30f * (1f - pulse)),
                                        radius = size.minDimension * 0.5f + 10.dp.toPx() * pulse,
                                        style = Stroke(width = 1.2.dp.toPx()),
                                    )
                                }
                            },
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.18f))
                                .drawBehind {
                                    drawCircle(
                                        color = badgeColor.copy(alpha = 0.38f),
                                        style = Stroke(width = 1.dp.toPx()),
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isActivated) Icons.TwoTone.Check else Icons.TwoTone.Close,
                                contentDescription = badgeText,
                                tint = badgeColor,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            if (modeTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    modeTags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(
                                    if (lightGlass) Color.Black.copy(alpha = 0.05f)
                                    else Color.White.copy(alpha = 0.10f),
                                )
                                .drawBehind {
                                    drawRoundRect(
                                        color = if (lightGlass) Color.Black.copy(alpha = 0.06f)
                                        else Color.White.copy(alpha = 0.16f),
                                        style = Stroke(width = 1.dp.toPx()),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = tag,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (lightGlass) KuaiMaoColors.textBody else Color.White.copy(alpha = 0.86f),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (modeTags.isEmpty()) 18.dp else 16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(heroHairline),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp, bottom = if (quickActions.isEmpty()) 18.dp else 16.dp),
            ) {
                HeroMetric(
                    modifier = Modifier.weight(1f),
                    label = superuserLabel,
                    value = superuserCount,
                    unit = superuserUnit,
                    // 小方点也走主题色：近黑主题下用大比例白混出浅灰，保证在深卡上可见
                    dotColor = if (lightGlass) accent else lerp(accent, Color.White, 0.78f),
                    ink = onHero,
                    inkSoft = onHeroSoft,
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(heroHairline)
                        .align(Alignment.CenterVertically),
                )
                HeroMetric(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 18.dp),
                    label = moduleLabel,
                    value = moduleCount,
                    unit = moduleUnit,
                    dotColor = if (lightGlass) lerp(accent, Color.Black, 0.2f) else lerp(accent, Color.White, 0.50f),
                    ink = onHero,
                    inkSoft = onHeroSoft,
                )
            }

            // 四个快捷入口并进卡片底部：省掉一行独立卡片，整块留白也更整。
            if (quickActions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(heroHairline),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    quickActions.forEach { action ->
                        HeroQuickItem(
                            action = action,
                            lightGlass = lightGlass,
                            ink = onHero,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 大卡内部的快捷入口：图标 + 下方文字，和卡片同一块底。
 *
 * 不再给图标单独描边——深卡上白描边会画成一圈灰框，之前四宫格踩过这个坑。
 */
@Composable
private fun HeroQuickItem(
    action: KuaiMaoQuickAction,
    lightGlass: Boolean,
    ink: Color,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // 按下时整块缩到 92%，图标底板同时提亮一档，手感比单纯换色明显。
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "quickPress",
    )
    val tileAlpha = when {
        pressed -> if (lightGlass) 0.12f else 0.20f
        else -> if (lightGlass) 0.05f else 0.10f
    }
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = action.onClick,
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(15.dp))
                // 平色底板：之前试过竖向渐变，深卡上会显出一层灰雾，反而脏。
                .background(
                    if (lightGlass) {
                        Color.Black.copy(alpha = tileAlpha)
                    } else {
                        Color.White.copy(alpha = tileAlpha)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = ink.copy(alpha = if (pressed) 1f else 0.94f),
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = action.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = ink.copy(alpha = 0.88f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HeroMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    unit: String,
    dotColor: Color,
    ink: Color = Color.White,
    inkSoft: Color = Color.White,
) {
    // 数字滚动：进场从 0 数到实际值，之后数量变化也跟着滚，不再硬切。
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val shown by animateIntAsState(
        targetValue = if (started) value else 0,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "metricValue",
    )
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(dotColor),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = KuaiMaoType.heroMetricLabel,
                color = ink.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = shown.toString(),
                style = KuaiMaoType.heroMetricValue,
                color = ink,
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = unit,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ink.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

/** 四宫格快捷入口 */
@Composable
fun KuaiMaoQuickGrid(
    actions: List<KuaiMaoQuickAction>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { action ->
            QuickTile(action = action, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickTile(action: KuaiMaoQuickAction, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .shadow(4.dp, shape, clip = false, ambientColor = Color(0x1418262C), spotColor = Color(0x1A18263C))
            .clip(shape)
            // 开了背景（壁纸 / 特效）时卡片是半透明的，壁纸再走一层模糊，
            // 特效和壁纸才能从卡片底下透出来。
            .background(KuaiMaoColors.canvasCard)
            .renderBackgroundBlur(tintColor = KuaiMaoColors.canvasCardSolid)
            // 不描边：壁纸 + 夜间模式下 line 是深色，压在模糊卡片上就是一圈黑框。
            .clickable(onClick = action.onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 图标底板只铺一层浅底，不描边：其它页面（系统动态、设置行）的
        // 图标底板都没有边，这里加了会在四个角上多出一圈灰黑轮廓。
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(action.tone.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = action.tone.content,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = action.label,
            style = KuaiMaoType.quickLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 区块标题：左标题 + 右动作 */
@Composable
fun KuaiMaoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = KuaiMaoType.sectionTitle)
        Spacer(modifier = Modifier.weight(1f))
        if (!actionText.isNullOrBlank()) {
            Text(
                text = actionText,
                style = KuaiMaoType.sectionAction,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

/** 系统动态列表卡 */
@Composable
fun KuaiMaoFeedCard(
    items: List<KuaiMaoFeedItem>,
    modifier: Modifier = Modifier,
) {
    // 自定义壁纸下不画行间发丝线：浅色细线压在花丛上会连成一排「小白条」。
    val cardConfig: CardConfig = koinInject()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, KuaiMaoDimens.rCard, clip = false, ambientColor = Color(0x0D18263C), spotColor = Color(0x0D18263C))
            .clip(KuaiMaoDimens.rCard)
            .background(KuaiMaoColors.canvasCard)
            .renderBackgroundBlur(tintColor = KuaiMaoColors.canvasCardSolid)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0 && !cardConfig.isCustomBackgroundEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(KuaiMaoColors.line),
                )
            }
            FeedRow(item = item)
        }
    }
}

@Composable
private fun FeedRow(item: KuaiMaoFeedItem) {
    val titleStyle = KuaiMaoType.rowTitle
    val descStyle = KuaiMaoType.rowDesc
    val valueStyle = KuaiMaoType.rowValue
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (item.onClick != null) {
                    Modifier.clickable(onClick = item.onClick)
                } else {
                    Modifier
                },
            ),
    ) {
        // 左右排布要求「标题 + 间隙 + 值」都能单行放下；内核版本、驱动版本这类
        // 长串天然放不下，硬排会把标题压成「内…」并把值推出卡片。
        // 所以先量一遍：放不下就把标题列收窄到自身文字宽度，让值在右侧换行，
        // 排版和上面几行完全一致，只是版本号多占一行。
        val iconAndGaps = with(density) { (42 + 13 + 10).dp.roundToPx() }
        val textSpace = (constraints.maxWidth - iconAndGaps).coerceAtLeast(1)
        val gap = with(density) { 16.dp.roundToPx() }
        fun measureWidth(text: String, style: TextStyle): Int =
            measurer.measure(
                text = AnnotatedString(text),
                style = style,
                maxLines = 1,
                softWrap = false,
            ).size.width
        val titleWidth = measureWidth(item.title, titleStyle)
        val valueWidth = measureWidth(item.value, valueStyle)
        val descWidth = if (item.desc.isNotBlank()) measureWidth(item.desc, descStyle) else 0
        val inline = titleWidth + gap + valueWidth <= textSpace
        // 标题列最多占 55%，再长就省略，保证右侧至少还能排两行版本号。
        val labelWidth = minOf(maxOf(titleWidth, descWidth), (textSpace * 0.55f).toInt())

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(item.tone.container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.tone.content,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(13.dp))

            Column(
                modifier = if (inline) {
                    Modifier.weight(1f)
                } else {
                    Modifier.width(with(density) { labelWidth.toDp() })
                },
            ) {
                Text(
                    text = item.title,
                    style = titleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.desc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.desc,
                        style = descStyle,
                        maxLines = item.descMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (item.value.isNotBlank()) {
                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = item.value,
                    style = valueStyle,
                    color = item.valueColor,
                    // 一行放得下就单行；放不下在剩余宽度里换行，右对齐，最多三行。
                    maxLines = if (inline) 1 else 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = if (inline) Modifier else Modifier.weight(1f),
                )
            }

            if (item.trailingChevron) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.TwoTone.ChevronRight,
                    contentDescription = null,
                    tint = KuaiMaoColors.textMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** 设置页 / 二级页使用的分类行 */
@Composable
fun KuaiMaoCategoryRow(
    title: String,
    desc: String,
    icon: ImageVector,
    tone: KuaiMaoTone,
    count: Int? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(tone.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(21.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = KuaiMaoColors.textBody)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                desc,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = KuaiMaoColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (count != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(KuaiMaoColors.canvasSubtle)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    color = KuaiMaoColors.textTer,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        Icon(
            imageVector = Icons.TwoTone.ChevronRight,
            contentDescription = null,
            tint = KuaiMaoColors.chevron,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** 进度条式状态胶囊（用于「安全 / 风险」等短标签） */
@Composable
fun KuaiMaoPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    container: Color = color.copy(alpha = 0.12f),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Black, color = color)
    }
}

/** 轻量淡入位移，供首屏元素依次入场 */
@Composable
fun kuaimaoRiseAlpha(visible: Boolean, delayMillis: Int = 0): Float {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 420,
            delayMillis = delayMillis,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "kuaimaoRise",
    )
    return alpha
}

/** 供页面复用的标准间距 */
val KuaiMaoPageGutter: Dp = KuaiMaoDimens.pageGutter

/**
 * 标题文字在深色画布上的自适应。
 *
 * 用户自选的主/次两段颜色通常按白底挑的（默认 #101322 / #71717A），
 * 直接放到纯黑背景上等于隐形。深色下把过暗的颜色往白里提，
 * [amount] 控制提亮幅度：主段提得多（接近纯白），次段提得少（保留层次）。
 */
private fun Int.adaptTitleInk(darkCanvas: Boolean, amount: Float): Color {
    val raw = Color(this)
    if (!darkCanvas || raw.luminance() >= 0.55f) return raw
    return lerp(raw, Color.White, amount)
}
