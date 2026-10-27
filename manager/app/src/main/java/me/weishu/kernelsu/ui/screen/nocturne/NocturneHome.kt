package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.component.rebootlistpopup.getRebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.rememberRebootAction
import me.weishu.kernelsu.ui.screen.home.HomeActions
import me.weishu.kernelsu.ui.screen.home.HomeUiState

/** Nocturne 主页：光晕背景 + Hero 状态卡 + Group 分组 + CodeCard + 重启弹层。 */
@Composable
fun NocturneHomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
    navigationBadge: NavigationBadgeState,
    onOpenTab: (Int) -> Unit,
    onOpenAuthor: () -> Unit,
) {
    val t = ncTokens()
    var showRebootSheet by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        NcGlowBackground(t)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = NcDimens.pageX,
                end = NcDimens.pageX,
                top = ncStatusTop() + 8.dp,
                bottom = bottomInnerPadding + 96.dp,
            ),
        ) {
            item {
                NcHeader(
                    kicker = "FoxSu · Nocturne",
                    title = "夜幕",
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            NcRoundButton(iconRes = R.drawable.ic_d_user, onClick = onOpenAuthor)
                            NcRoundButton(
                                iconRes = R.drawable.ic_d_power2,
                                onClick = { showRebootSheet = true },
                                tint = t.red,
                            )
                        }
                    },
                )
            }
            item { NcHeroCard(state = state) }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                NcPanel {
                    NcGroupRow(
                        iconRes = R.drawable.ic_d_phone,
                        accent = t.violet,
                        title = stringResource(R.string.home_device_model),
                        subtitle = state.systemInfo.deviceModel,
                    )
                    NcGroupRow(
                        iconRes = R.drawable.ic_d_android,
                        accent = t.mint,
                        title = "管理器版本",
                        subtitle = BuildConfig.VERSION_NAME,
                        showDivider = true,
                    )
                    NcGroupRow(
                        iconRes = R.drawable.ic_d_lock,
                        accent = if (state.isSELinuxPermissive) t.amber else t.lime,
                        title = "SELinux",
                        subtitle = state.systemInfo.selinuxStatus.ifBlank { "—" },
                        showDivider = true,
                    ) {
                        NcBadge(
                            text = if (state.isSELinuxPermissive) "Permissive" else "Enforcing",
                            color = if (state.isSELinuxPermissive) t.amber else t.mint,
                        )
                    }
                    NcGroupRow(
                        iconRes = R.drawable.ic_d_shield,
                        accent = t.violet,
                        title = stringResource(R.string.superuser),
                        subtitle = "已授权 ${navigationBadge.superuserCount} 个应用",
                        showDivider = true,
                        onClick = { onOpenTab(1) },
                    ) { NcChevron() }
                    NcGroupRow(
                        iconRes = R.drawable.ic_d_grid,
                        accent = t.amber,
                        title = stringResource(R.string.module),
                        subtitle = "已启用 ${navigationBadge.moduleEnabledCount} 个模块",
                        showDivider = true,
                        onClick = { onOpenTab(2) },
                    ) { NcChevron() }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item { NcCodeCard(kicker = "内核版本", value = state.systemInfo.kernelVersion.ifBlank { "—" }) }
            item { Spacer(Modifier.height(10.dp)) }
            item { NcCodeCard(kicker = "系统指纹", value = state.systemInfo.fingerprint.ifBlank { "—" }) }
        }
    }

    if (showRebootSheet) {
        NcRebootBottomSheet(onDismiss = { showRebootSheet = false })
    }
}

/** Hero 状态卡 r24：外层 1dp 渐变描边 + 内层 r23 渐变填充；呼吸状态点 + 大字版本 + LKM 徽章 + 右上 56dp 环形进度。 */
@Composable
private fun NcHeroCard(state: HomeUiState) {
    val t = ncTokens()
    val ok = state.ksuVersion != null
    val ring = rememberInfiniteTransition(label = "dot")
    val breath by ring.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "breath",
    )

    // 外层 Box padding 1dp + 渐变底 = 描边；内层同圆角渐变填充
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NcRadius.hero))
            .background(
                Brush.linearGradient(
                    listOf(t.violet.copy(alpha = 0.55f), t.mint.copy(alpha = 0.35f), t.violet.copy(alpha = 0.55f))
                )
            )
            .padding(1.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NcRadius.hero - 1.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            if (t.dark) Color(0xFF181B26) else Color(0xFFFFFFFF),
                            if (t.dark) Color(0xFF0D0F16) else Color(0xFFF7F8FD),
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(Modifier.padding(end = 62.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 状态点：2.4s 呼吸
                    Box(
                        Modifier
                            .size(9.dp)
                            .graphicsLayer { alpha = breath }
                            .clip(CircleShape)
                            .background(if (ok) t.mint else t.red)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (ok) "运行中" else "未安装",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = if (ok) t.mint else t.red,
                    )
                }
                Text(
                    text = "KernelSU",
                    fontSize = 11.sp,
                    color = t.text2,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.ksuVersion?.toString() ?: "—",
                        fontSize = 29.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = t.text1,
                    )
                    Spacer(Modifier.width(10.dp))
                    NcBadge(
                        text = if (state.lkmMode == true) "LKM" else "GKI",
                        color = t.mint,
                        bgAlpha = 0.12f,
                    )
                }
                Text(
                    text = "v${BuildConfig.VERSION_NAME} · UAPI ${state.kernelUAPIVersion ?: "?"}",
                    fontSize = 10.sp,
                    color = t.text2,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            // 右上 56dp 环形进度
            NcRingProgress(
                modifier = Modifier.align(Alignment.TopEnd),
                progress = if (ok) 0.86f else 0.12f,
                color1 = t.violet,
                color2 = t.mint,
                track = t.line,
                ready = ok,
            )
        }
    }
}

/** 56dp 环形进度。 */
@Composable
private fun NcRingProgress(
    modifier: Modifier = Modifier,
    progress: Float,
    color1: Color,
    color2: Color,
    track: Color,
    ready: Boolean,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) { anim.animateTo(progress, tween(700)) }
    Canvas(modifier.size(NcDimens.ring)) {
        val stroke = 6.dp.toPx()
        val inset = stroke / 2
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawArc(
            brush = Brush.sweepGradient(listOf(color1, color2, color1)),
            startAngle = -90f,
            sweepAngle = 360f * anim.value,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawCircle(
            color = if (ready) color2 else color1,
            radius = 3.dp.toPx(),
            center = Offset(size.width / 2, size.height / 2),
        )
    }
}

/**
 * Nocturne 重启弹层：r30 顶部圆角、渐变深底 + 顶部 violet 描边、
 * 主重启（渐变紫图标盒 44 r15）+ 2×2 网格 + EDL 危险项 + ghost 取消。进场 translateY 30→0 / 420ms。
 */
@Composable
fun NcRebootBottomSheet(onDismiss: () -> Unit) {
    val t = ncTokens()
    val options = getRebootListOption()
    val doReboot = rememberRebootAction()
    val primary = options.first()
    val others = options.drop(1)
    val edl = others.lastOrNull { it.reason == "edl" }
    val grid = others.filter { it !== edl }

    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(420)) }

    Box(
        Modifier
            .fillMaxSize()
            .background(t.bg.copy(alpha = 0.6f * enter.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = 30.dp.toPx() * (1f - enter.value) }
                .clip(RoundedCornerShape(topStart = NcRadius.sheet, topEnd = NcRadius.sheet))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (t.dark) Color(0xF5181B26) else Color(0xFFFFFFFF),
                            if (t.dark) Color(0xFA0A0C11) else Color(0xFFF4F5FA),
                        )
                    )
                )
                .border(
                    1.dp,
                    t.violet.copy(alpha = 0.35f),
                    RoundedCornerShape(topStart = NcRadius.sheet, topEnd = NcRadius.sheet),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 18.dp, vertical = 20.dp),
        ) {
            // 主重启
            val pInteraction = remember { MutableInteractionSource() }
            val pressed by pInteraction.collectIsPressedAsState()
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (pressed) 0.985f else 1f, tween(120), label = "pri"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.horizontalGradient(listOf(t.violet.copy(alpha = 0.18f), t.violetDeep.copy(alpha = 0.14f))))
                    .border(1.dp, t.violet.copy(alpha = 0.45f), RoundedCornerShape(15.dp))
                    .clickable(interactionSource = pInteraction, indication = null) {
                        onDismiss(); doReboot(primary.reason)
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Brush.linearGradient(listOf(t.violet, t.violetDeep))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_d_power2),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.width(13.dp))
                Column {
                    Text(
                        text = stringResource(primary.labelRes),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = t.text1,
                    )
                    Text(
                        text = "执行完整重启",
                        fontSize = 10.sp,
                        color = t.text2,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            // 2×2 网格子项
            grid.chunked(2).forEach { chunk ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    chunk.forEach { option ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(NcRadius.card))
                                .background(t.fill)
                                .clickable { onDismiss(); doReboot(option.reason) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(t.violet.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_d_refresh),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = t.violetText,
                                )
                            }
                            Spacer(Modifier.width(9.dp))
                            Text(
                                text = stringResource(option.labelRes),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = t.text1,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (chunk.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(9.dp))
            }
            // EDL 危险项
            if (edl != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NcRadius.card))
                        .background(t.red.copy(alpha = 0.07f))
                        .border(1.dp, t.red.copy(alpha = 0.24f), RoundedCornerShape(NcRadius.card))
                        .clickable { onDismiss(); doReboot(edl.reason) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(t.red.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_d_power2),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = t.red,
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text(
                            text = stringResource(edl.labelRes),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (t.dark) Color(0xFFFFB3A9) else t.red,
                        )
                        Text(
                            text = "紧急救砖模式，请谨慎操作",
                            fontSize = 9.sp,
                            color = t.text3,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                }
                Spacer(Modifier.height(9.dp))
            }
            // ghost 取消
            Text(
                text = "取 消",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = t.text2,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 11.dp),
            )
        }
    }
}
