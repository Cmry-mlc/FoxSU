package me.weishu.kernelsu.ui.util

import androidx.compose.ui.graphics.Color
import me.weishu.kernelsu.R

/** 应用内展示用的 Logo：跟随用户选择的启动器图标。 */
data class AppIconVisual(
    val foreground: Int,
    val background: Color,
)

fun appIconVisual(style: Int): AppIconVisual = when (style) {
    1 -> AppIconVisual(R.drawable.ic_launcher_kernelsu_foreground, Color(0xFF14721F))
    // 七彩：白底彩虹描边
    2 -> AppIconVisual(R.drawable.ic_launcher_fox2_foreground, Color.White)
    // 夜：墨底月白描边
    3 -> AppIconVisual(R.drawable.ic_launcher_fox3_foreground, Color(0xFF0B0B0C))
    4 -> AppIconVisual(R.drawable.ic_launcher_fox4_foreground, Color(0xFF0B0B0C))
    // 默认：白底墨线
    else -> AppIconVisual(R.drawable.ic_launcher_foreground, Color.White)
}
