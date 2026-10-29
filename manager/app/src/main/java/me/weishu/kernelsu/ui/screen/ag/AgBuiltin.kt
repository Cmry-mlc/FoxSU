package me.weishu.kernelsu.ui.screen.ag

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.ui.component.bottombar.useSideDock
import me.weishu.kernelsu.ui.theme.TitleInk
import me.weishu.kernelsu.ui.util.BuiltinEngine
import me.weishu.kernelsu.ui.util.BuiltinEngines
import me.weishu.kernelsu.ui.util.BuiltinReport
import me.weishu.kernelsu.ui.util.EngineState

/**
 * 内置能力 · 四个引擎的独立页面。
 *
 * 设计上刻意不是「四个开关」：每一项都是内核 / 原生层的能力，能不能用只取决于运行环境。
 * 所以页面呈现的是**状态与依据**——就绪的写清楚在用哪个实现，缺的写清楚缺什么，
 * 既不把「拨了开关其实没生效」当功能，也不把「没有 root」误报成「内核不支持」。
 */
@Composable
fun AgBuiltin(
    onBack: () -> Unit,
    /** 去模块仓库找外置 provider（Zygisk / Sui 都能靠模块补上）。 */
    onOpenRepo: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var report by remember { mutableStateOf<BuiltinReport?>(null) }
    var loading by remember { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tick) {
        loading = true
        report = BuiltinEngines.probe()
        loading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .agPageBg()
            .statusBarsPadding(),
    ) {
        // ── 固定顶栏：返回 + 刷新（与定制页同款透明头）──
        Row(
            Modifier
                .fillMaxWidth()
                .background(Ag.headSurface()).agHeadBlur()
                .padding(
                    start = if (useSideDock()) 106.dp else 18.dp,
                    end = 18.dp,
                )
                .padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AgIconBtn(AgTopIcons.Back, onClick = onBack, tint = Ag.Tx)
            Spacer(Modifier.weight(1f))
            AgIconBtn(
                icon = AgTopIcons.More,
                tint = if (loading) Ag.Tx3 else Ag.Tx,
                onClick = { if (!loading) tick++ },
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 6.dp,
                bottom = 48.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { BuiltinHero(report, loading) }

            items(report?.engines?.size ?: 4) { index ->
                val r = report?.engines?.getOrNull(index)
                if (r != null) {
                    EngineCard(
                        report = r,
                        expanded = expanded == r.engine.key,
                        onToggle = { expanded = if (expanded == r.engine.key) null else r.engine.key },
                        onOpenRepo = onOpenRepo,
                    )
                } else {
                    EngineSkeleton(index)
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                DiagnosticsCard(
                    report = report,
                    loading = loading,
                    onRefresh = { tick++ },
                    onCopy = {
                        val r = report
                        if (r == null) {
                            Toast.makeText(context, "还没有探测结果", Toast.LENGTH_SHORT).show()
                        } else {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("foxsu-builtin", BuiltinEngines.diagnosticText(r)))
                            Toast.makeText(context, "诊断信息已复制", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }

            item {
                Text(
                    "这四个引擎都跑在管理器之外：Folk Mount 由 ksud 的挂载引擎承担，" +
                        "FolkZygisk / Sui / NoMount 需要内核侧按设备 KMI 提供对应通道。" +
                        "管理器只负责探测、呈现与调用，不会用开关去假装启用。",
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = Ag.Tx3,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** 页头：渐变方块 + 标题 + 一句话说明 + 就绪统计。 */
@Composable
private fun BuiltinHero(report: BuiltinReport?, loading: Boolean) {
    Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Box(
            Modifier
                .size(64.dp)
                .shadow(14.dp, RoundedCornerShape(20.dp), spotColor = Ag.Vio.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(20.dp))
                .background(Ag.Grad),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                AgEngineIcons.Mount,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "内置能力",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.8).sp,
            style = TitleInk.style(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "四个引擎内置于管理器中，无需额外安装模块。",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = Ag.Tx2,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val total = 4
            val ready = report?.readyCount ?: 0
            Box(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (ready > 0) Ag.Success.copy(alpha = 0.12f) else Ag.TintInk)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(if (ready > 0) Ag.Success else Ag.Tx3, CircleShape),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (loading) "正在探测…" else "已就绪 $ready / $total",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ready > 0) Ag.Success else Ag.Tx2,
                    )
                }
            }
            if (report != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    report.mountBackend,
                    fontSize = 11.5.sp,
                    color = Ag.Tx3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 单个引擎卡：图标 + 名称 + 状态；点开看判定依据。 */
@Composable
private fun EngineCard(
    report: me.weishu.kernelsu.ui.util.EngineReport,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenRepo: (() -> Unit)? = null,
) {
    val accent = Ag.accent
    val stateColor = stateColorOf(report.state)
    val chev by animateFloatAsState(if (expanded) 90f else 0f, label = "builtin-chev")

    Column(
        Modifier
            .fillMaxWidth()
            .agClick(pressScale = 0.99f, onClick = onToggle)
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x0A3C2D78))
            .clip(RoundedCornerShape(20.dp))
            .background(Ag.Card)
            .border(1.dp, Ag.Line, RoundedCornerShape(20.dp))
            .padding(15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.primary.copy(alpha = 0.16f),
                                accent.secondary.copy(alpha = 0.10f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    iconOf(report.engine),
                    contentDescription = null,
                    tint = accent.primary,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    report.engine.label,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ag.Tx,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    report.engine.tag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp,
                    color = Ag.Tx3,
                    maxLines = 1,
                )
            }
            StatePill(report.state, stateColor)
            Spacer(Modifier.width(6.dp))
            Text(
                "›",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Ag.Chev,
                modifier = Modifier.rotate(chev),
            )
        }

        Spacer(Modifier.height(11.dp))
        Text(
            report.engine.summary,
            fontSize = 12.sp,
            color = Ag.Tx2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(Modifier.padding(top = 12.dp)) {
                Text(
                    report.engine.detail,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = Ag.Tx2,
                )
                Spacer(Modifier.height(10.dp))
                // 判定依据：把「为什么是这个状态」摊开写
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Ag.TintInk)
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                ) {
                    report.evidence.forEachIndexed { i, line ->
                        if (i > 0) Spacer(Modifier.height(5.dp))
                        Row {
                            Box(
                                Modifier
                                    .padding(top = 6.dp)
                                    .size(4.dp)
                                    .background(stateColor.copy(alpha = 0.8f), CircleShape),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                line,
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp,
                                color = Ag.Tx2,
                            )
                        }
                    }
                }
                // Zygisk / Sui 都能用外置模块补上：给一条真实可走的路，而不是只报「缺」
                val canInstall = onOpenRepo != null &&
                    report.state != EngineState.READY &&
                    (report.engine == BuiltinEngine.FOLK_ZYGISK || report.engine == BuiltinEngine.SUI)
                if (canInstall) {
                    Spacer(Modifier.height(10.dp))
                    BuiltinAction(
                        text = "去模块仓库找可用的 provider",
                        icon = AgSettingsIcons.Puzzle,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenRepo!!,
                    )
                }
            }
        }
    }
}

/** 未探完时的占位卡（骨架），避免列表在加载瞬间跳动。 */
@Composable
private fun EngineSkeleton(index: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ag.Card)
            .border(1.dp, Ag.Line, RoundedCornerShape(20.dp))
            .padding(15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Ag.TintInk),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.42f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Ag.TintInk),
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.26f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Ag.TintInk),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth(0.7f)
                .height(9.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Ag.TintInk),
        )
    }
}

/** 底部诊断卡：内核 / 管理器版本、挂载后端、刷新与复制。 */
@Composable
private fun DiagnosticsCard(
    report: BuiltinReport?,
    loading: Boolean,
    onRefresh: () -> Unit,
    onCopy: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ag.Card)
            .border(1.dp, Ag.Line, RoundedCornerShape(20.dp))
            .padding(15.dp),
    ) {
        Text(
            "诊断",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            color = Ag.Tx3,
        )
        Spacer(Modifier.height(10.dp))
        DiagRow("内核版本", report?.kernelVersion ?: "—")
        DiagRow("管理版本", report?.managerVersion ?: "—")
        DiagRow("挂载后端", report?.mountBackend ?: "—")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            BuiltinAction(
                text = if (loading) "探测中…" else "重新探测",
                icon = AgSettingsIcons.CheckUpdate,
                enabled = !loading,
                modifier = Modifier.weight(1f),
            ) { onRefresh() }
            BuiltinAction(
                text = "复制诊断",
                icon = AgSettingsIcons.Modules,
                modifier = Modifier.weight(1f),
            ) { onCopy() }
        }
    }
}

/** 卡片内的小动作按钮（AgPillAction 是定制页的 private 组件，这里按同规格另写一个）。 */
@Composable
private fun BuiltinAction(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(38.dp)
            .agClick(enabled = enabled, onClick = onClick)
            .clip(RoundedCornerShape(13.dp))
            .background(Ag.TintInk)
            .border(1.dp, Ag.Line, RoundedCornerShape(13.dp)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) Ag.Vio else Ag.Tx3,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Ag.Tx else Ag.Tx3,
            maxLines = 1,
        )
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.5.sp, color = Ag.Tx2)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = Ag.Tx,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun StatePill(state: EngineState, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(color.copy(alpha = if (state == EngineState.READY) 0.12f else 0.10f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).background(color, CircleShape))
            Spacer(Modifier.width(5.dp))
            Text(
                BuiltinEngines.stateLabel(state),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun stateColorOf(state: EngineState): Color = when (state) {
    EngineState.READY -> Ag.Success
    EngineState.OFF -> Ag.Tx2
    EngineState.MISSING -> Ag.Amber
    EngineState.UNKNOWN -> Ag.Tx3
}

private fun iconOf(engine: BuiltinEngine): ImageVector = when (engine) {
    BuiltinEngine.FOLK_MOUNT -> AgEngineIcons.Mount
    BuiltinEngine.FOLK_ZYGISK -> AgEngineIcons.Zygisk
    BuiltinEngine.SUI -> AgEngineIcons.Sui
    BuiltinEngine.NOMOUNT -> AgEngineIcons.NoMount
}
