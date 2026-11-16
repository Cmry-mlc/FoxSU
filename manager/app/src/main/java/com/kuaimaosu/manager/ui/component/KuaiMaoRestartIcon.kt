package com.kuaimaosu.manager.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TAU = 6.2831855f

/**
 * 重启图标：闭合外环 + 环内一段逆时针弧与箭头。
 *
 * 不用 `Icons.TwoTone.RestartAlt`：那个图形是一个缺了约 90° 的开口环，
 * 放在底栏那排实心图标里，缺口会显得像「没画完」，而且在 21dp 下缺口和箭头
 * 挤在一起，方向也不清楚。
 *
 * 这里重画成：外环完整闭合（表示「回到起点」，不是「循环播放」），
 * 环内一段加粗的逆时针弧，末端一个小三角指示转向。整体是实心块面 +
 * 圆头端点，与首页、超级用户、模块、设置四个页签的填充风格一致。
 *
 * 坐标都在 24×24 网格内手算：外环半径 8.0、线宽 1.9；内弧半径 4.25、宽 2.1，
 * 从 160° 顺时针绕到 340°（左侧 → 底部 → 右侧），末端接一个箭头，
 * 表示「从这里重新转一圈」。外环留 2dp 的内边距，21dp 下环与弧不会糊在一起。
 */
val KuaiMaoRestartIcon: ImageVector by lazy { buildRestartIcon() }

private fun buildRestartIcon(): ImageVector {
    val builder = ImageVector.Builder(
        name = "KuaiMaoRestart",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )

    // ---- 外环：完整闭合的圆 ------------------------------------------
    builder.addPath(
        pathData = PathData {
            val radius = 8.0f
            val steps = 72
            for (i in 0..steps) {
                val angle = TAU * i / steps
                val x = 12f + radius * cos(angle)
                val y = 12f + radius * sin(angle)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        },
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )

    // ---- 内弧：160° → 340°，顺时针绕到右侧 ---------------------------------
    builder.addPath(
        pathData = PathData {
            val radius = 4.25f
            val start = 160f
            val end = 340f
            val steps = 28
            for (i in 0..steps) {
                val degrees = start + (end - start) * i / steps
                val angle = degrees * (PI.toFloat() / 180f)
                val x = 12f + radius * cos(angle)
                val y = 12f + radius * sin(angle)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        },
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.1f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )

    // ---- 箭头：贴在弧的末端，沿切线方向 -------------------------------
    builder.addPath(
        pathData = PathData {
            moveTo(15.37f, 8.84f)
            lineTo(18.24f, 11.32f)
            lineTo(14.76f, 12.58f)
            close()
        },
        fill = SolidColor(Color.Black),
    )

    return builder.build()
}
