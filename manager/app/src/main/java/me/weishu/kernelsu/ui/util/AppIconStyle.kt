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
    2 -> AppIconVisual(R.drawable.ic_launcher_fox2_foreground, Color(0xFFDBE6DF))
    3 -> AppIconVisual(R.drawable.ic_launcher_fox3_foreground, Color(0xFFFFD3B4))
    4 -> AppIconVisual(R.drawable.ic_launcher_fox4_foreground, Color(0xFFBFE8D5))
    else -> AppIconVisual(R.drawable.ic_launcher_foreground, Color(0xFFB7E3C6))
}
