package com.kuaimaosu.manager.ui.component

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.screen.splash.KuaiMaoMark
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors

/**
 * 首次安装引导弹窗的「是否询问过」状态。
 * 只落一个布尔值：问过就不再打扰。
 */
object KuaiMaoJailbreakOfferStore {
    private const val PREF_NAME = "kuaimao_jailbreak_offer"
    private const val KEY_ASKED = "asked"

    fun isAsked(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ASKED, false)

    fun markAsked(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ASKED, true)
            .apply()
    }
}

/** 主按钮/光环在暗底上用的主题色：深色主题色直接铺上去会糊成一团黑，先提亮再用。 */
private fun Color.onDark(): Color = when {
    luminance() < 0.05f -> lerp(this, Color.White, 0.55f)
    luminance() < 0.30f -> lerp(this, Color.White, 0.36f)
    else -> this
}

/** 保持色相，只把明度压到指定值——给 hero 底色染一层很淡的主题色。 */
private fun Color.darkTint(value: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    hsv[2] = value
    // 近白的主题色（夜间默认 #F2F2F7）饱和度接近 0，直接染会变纯灰，兜底给一点饱和。
    hsv[1] = hsv[1].coerceAtLeast(0.16f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * 首次安装时的「一键提权」引导弹窗。
 *
 * 版式是一张「引擎启动卡」：上半深色 hero（旋转光环 + 双圈 + logo + 标题 + CVE 编号），
 * 下半浅色操作区（三格参数条 + 渐变主按钮 + 低调退出）。
 * hero 恒为深色，所以文字固定白色，不跟主题色反转；操作区走 KuaiMaoColors，亮/夜/壁纸都适配。
 */
@Composable
fun KuaiMaoJailbreakOfferDialog(
    visible: Boolean,
    onJailbreak: () -> Unit,
    onLater: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            progress.animateTo(1f, spring(dampingRatio = 0.84f, stiffness = 380f))
        } else {
            progress.animateTo(0f, tween(durationMillis = 150))
        }
    }
    if (!visible && progress.value <= 0.001f) return

    val shape = RoundedCornerShape(30.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        // 暗幕：吞掉触摸，必须明确选择
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value * 0.66f }
                .background(Color.Black)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        }
                    }
                },
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 22.dp, vertical = 20.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val e = progress.value
                    alpha = e
                    val s = 0.92f + 0.08f * e
                    scaleX = s
                    scaleY = s
                    translationY = (1f - e) * 26.dp.toPx()
                }
                .shadow(elevation = 26.dp, shape = shape, clip = false)
                .clip(shape)
                .background(KuaiMaoColors.canvasCardSolid)
                .verticalScroll(rememberScrollState()),
        ) {
            HeroHeader()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
            ) {
                SpecStrip()

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "通过内核漏洞临时加载 KernelSU，重启后自动恢复，" +
                        "不改动系统分区；随时可在「设置 → 一键越狱」再次执行。",
                    fontSize = 10.5.sp,
                    lineHeight = 15.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = KuaiMaoColors.textMuted,
                )

                Spacer(modifier = Modifier.height(14.dp))
                PrimaryOfferButton(text = "立即一键提权", onClick = onJailbreak)
                Spacer(modifier = Modifier.height(2.dp))
                TextButtonQuiet(text = "稍后再说", onClick = onLater)

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(KuaiMaoColors.line),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "KuaiMao 提权引擎 · 致谢 KaMuaMua2020",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    color = KuaiMaoColors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * 顶部深色区：光晕 + 旋转弧 + 双圈 + logo + 标题。
 * 颜色全部固定白色系（这块永远是深底），只有光晕取主题色，换主题会跟着变色。
 */
@Composable
private fun HeroHeader() {
    val accent = KuaiMaoColors.brand.onDark()
    val base = Color(0xFF0B0D12)

    // 旋转的一小段弧：引擎"在转"的感觉，比静态圆环活一点。
    val spin by rememberInfiniteTransition(label = "offer-spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "offer-spin-angle",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(base, KuaiMaoColors.brand.darkTint(0.20f), base),
                ),
            ),
    ) {
        // 右上主光晕 + 左下补光
        Canvas(modifier = Modifier.matchParentSize()) {
            val r = size.width * 0.78f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(size.width * 0.86f, size.height * 0.04f),
                    radius = r,
                ),
                radius = r,
                center = Offset(size.width * 0.86f, size.height * 0.04f),
            )
            val r2 = size.width * 0.52f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.06f, size.height * 0.98f),
                    radius = r2,
                ),
                radius = r2,
                center = Offset(size.width * 0.06f, size.height * 0.98f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 22.dp),
        ) {
            // 顶部标识行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(accent, CircleShape),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KuaiMao 引擎",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = Color.White.copy(alpha = 0.80f),
                    )
                }
                Text(
                    text = "首次使用",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = Color.White.copy(alpha = 0.38f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(104.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // 静态双圈
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val outer = size.minDimension / 2f
                        drawCircle(
                            color = Color.White.copy(alpha = 0.10f),
                            radius = outer - 0.7.dp.toPx(),
                            style = Stroke(width = 1.2.dp.toPx()),
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = outer - 9.dp.toPx(),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                    // 旋转弧
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = spin },
                    ) {
                        val stroke = 1.8.dp.toPx()
                        drawArc(
                            brush = Brush.sweepGradient(
                                0f to Color.Transparent,
                                0.14f to accent.copy(alpha = 0.10f),
                                0.34f to accent,
                                1f to Color.Transparent,
                            ),
                            startAngle = -90f,
                            sweepAngle = 118f,
                            useCenter = false,
                            topLeft = Offset(stroke, stroke),
                            size = androidx.compose.ui.geometry.Size(
                                size.width - stroke * 2f,
                                size.height - stroke * 2f,
                            ),
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                    // 内芯：一圈柔光 + 白色 logo
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.22f),
                                        accent.copy(alpha = 0.04f),
                                        Color.Transparent,
                                    ),
                                ),
                                CircleShape,
                            ),
                    )
                    KuaiMaoMark(progress = 1f, modifier = Modifier.size(54.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "一键临时提权",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "CVE-2026-98260 · 内核竞态 UAF",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = Color.White.copy(alpha = 0.52f),
                )
            }
        }
    }
}

/** 三格参数条：一眼看清「要不要解锁 BL」「是不是临时的」「还缺什么」。 */
@Composable
private fun SpecStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KuaiMaoColors.canvasField)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpecCell(value = "免解锁", label = "BL", modifier = Modifier.weight(1f))
        SpecDivider()
        SpecCell(value = "重启失效", label = "ROOT", modifier = Modifier.weight(1f))
        SpecDivider()
        SpecCell(value = "需授权", label = "SHELL", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SpecCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.1).sp,
            color = KuaiMaoColors.text,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp,
            color = KuaiMaoColors.textMuted,
        )
    }
}

@Composable
private fun SpecDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(26.dp)
            .background(KuaiMaoColors.line),
    )
}

/**
 * 主按钮：主题色渐变 + 一道缓慢扫过的高光。
 * 前景色按底色亮度反推，避免夜间（主题色近白）出现白底白字。
 */
@Composable
private fun PrimaryOfferButton(text: String, onClick: () -> Unit) {
    val accent = KuaiMaoColors.brand
    val onAccent = if (accent.luminance() < 0.62f) Color.White else Color(0xFF101013)
    val start = lerp(accent, Color.White, 0.08f)
    val end = lerp(accent, onAccent, 0.26f)
    val interaction = remember { MutableInteractionSource() }

    val sweep by rememberInfiniteTransition(label = "offer-sweep").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, delayMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "offer-sweep-x",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Brush.horizontalGradient(listOf(start, accent, end)))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        val travel = maxWidth
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(64.dp)
                .graphicsLayer {
                    translationX = (sweep * 2.1f - 0.5f) * travel.toPx()
                    rotationZ = 16f
                    alpha = 0.55f
                }
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.2.sp,
                color = onAccent,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.TwoTone.KeyboardArrowRight,
                contentDescription = null,
                tint = onAccent.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** 次级出口：不做成第二个实心按钮，避免和主按钮抢注意力。 */
@Composable
private fun TextButtonQuiet(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = KuaiMaoColors.textMuted,
        )
    }
}
