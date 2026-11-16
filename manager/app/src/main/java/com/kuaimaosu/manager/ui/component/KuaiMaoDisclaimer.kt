package com.kuaimaosu.manager.ui.component

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.screen.splash.KuaiMaoMark
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors

/**
 * 免责声明的「已同意」状态。
 *
 * 和通知已读一样只落一个布尔值：同意过的版本不再打扰，
 * 换版本号（[KEY_VERSION]）时会重新弹一次，方便以后改了条款再让用户确认。
 */
object KuaiMaoDisclaimerStore {
    private const val PREF_NAME = "kuaimao_disclaimer"
    private const val KEY_ACCEPTED = "accepted"
    // 条款内容改版就换这个值，用户会重新看到弹窗
    private const val KEY_VERSION = "accepted_version"
    private const val CURRENT_VERSION = 1

    fun isAccepted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ACCEPTED, false) &&
            prefs.getInt(KEY_VERSION, 0) == CURRENT_VERSION
    }

    fun accept(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACCEPTED, true)
            .putInt(KEY_VERSION, CURRENT_VERSION)
            .apply()
    }
}

private val disclaimerClauses = listOf(
    "本软件仅供学习、研究与技术交流使用，请勿用于任何违反法律法规、平台规则，" +
        "或损害他人合法权益的用途。因违规使用产生的一切责任由使用者自行承担。",
    "使用本软件可能涉及修改系统分区、刷写内核、获取提权等高风险操作，" +
        "由此导致的设备损坏、系统不稳定、数据丢失、保修失效或账号受限等后果，" +
        "均由使用者自行承担，开发者不承担任何直接或间接责任。",
    "请在充分了解风险、并已备份重要数据的前提下操作。任何一步不确定，" +
        "请先停止操作并查阅文档，不要盲目执行命令或刷入来路不明的文件。",
    "继续使用即表示你已年满 18 周岁（或已获得监护人同意），" +
        "并已完整阅读、理解并同意本声明的全部内容。",
)

/**
 * 首次进入时的免责声明。
 *
 * 这一版刻意**不做**磨砂与半透明：声明是要被读完的东西，背后透出壁纸只会
 * 让正文更难读。整张卡片是实色底，靠「顶部一层主题色渐晕 + 应用图标」
 * 撑起仪式感，正文按条分隔，条与条之间一条发丝线，读完才点得下去。
 *
 * 不做点击遮罩关闭：必须明确点「我已知晓」才继续，所以遮罩只是一层
 * 吞掉触摸的暗幕。卡片入场是「缩放 + 上浮 + 淡入」的弹簧，四条条款再依次浮现。
 */
@Composable
fun KuaiMaoDisclaimerDialog(
    visible: Boolean,
    onAccept: () -> Unit,
    onExit: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            progress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 420f))
        } else {
            progress.animateTo(0f, tween(durationMillis = 160))
        }
    }
    if (!visible && progress.value <= 0.001f) return

    val shape = RoundedCornerShape(28.dp)
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 暗幕：只吞触摸，不响应点击
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value * 0.62f }
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
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 380.dp)
                .graphicsLayer {
                    val e = progress.value
                    alpha = e
                    val s = 0.90f + 0.10f * e
                    scaleX = s
                    scaleY = s
                    translationY = (1f - e) * 20.dp.toPx()
                }
                .clip(shape)
                // 实色底：不磨砂、不半透明。外框压一层极淡的描边，
                // 在深色壁纸与浅色壁纸上都能看出卡的边界。
                .background(KuaiMaoColors.canvasCardSolid)
                .border(
                    width = 1.dp,
                    color = KuaiMaoColors.line.copy(alpha = 0.55f),
                    shape = shape,
                ),
        ) {
            // ---- 顶部：图标 + 标题 -------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                KuaiMaoColors.brand.copy(alpha = 0.13f),
                                KuaiMaoColors.brand.copy(alpha = 0.04f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .padding(top = 22.dp, bottom = 16.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 图标后面压一团柔光，让它「亮」起来而不是贴在上面
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            KuaiMaoColors.brand.copy(alpha = 0.22f),
                                            Color.Transparent,
                                        ),
                                    ),
                                    shape = CircleShape,
                                ),
                        )
                        KuaiMaoMark(
                            progress = 1f,
                            modifier = Modifier.size(62.dp),
                            plate = true,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "免责声明",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                        color = KuaiMaoColors.text,
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "使用前请完整阅读以下内容",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = KuaiMaoColors.textMuted,
                    )
                }
            }

            // ---- 条款：一条一段，条间发丝线 -----------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 330.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
            ) {
                disclaimerClauses.forEachIndexed { index, clause ->
                    // 条款跟着卡片一起进场，但比卡片再晚一点，一条接一条浮出来
                    val appear = ((progress.value - 0.16f - index * 0.06f) / 0.30f)
                        .coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = appear
                                translationY = (1f - appear) * 7.dp.toPx()
                            }
                            .padding(vertical = 10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(19.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(KuaiMaoColors.brand.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = (index + 1).toString().padStart(2, '0'),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = KuaiMaoColors.brand,
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = clause,
                            fontSize = 11.5.sp,
                            lineHeight = 17.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = KuaiMaoColors.textBody,
                        )
                    }
                    if (index != disclaimerClauses.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 29.dp)
                                .height(1.dp)
                                .background(KuaiMaoColors.line.copy(alpha = 0.55f)),
                        )
                    }
                }
            }

            // ---- 底部操作 -----------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DialogButton(
                    text = "退出应用",
                    background = KuaiMaoColors.canvasField,
                    foreground = KuaiMaoColors.textBody,
                    modifier = Modifier.weight(1f),
                    onClick = onExit,
                )
                DialogButton(
                    text = "我已知晓",
                    background = KuaiMaoColors.brand,
                    foreground = Color.White,
                    modifier = Modifier.weight(1.25f),
                    onClick = onAccept,
                )
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = foreground,
        )
    }
}
