package me.weishu.kernelsu.ui.util

import androidx.compose.ui.graphics.Color
import me.weishu.kernelsu.R

/**
 * 应用内展示用的 Logo：跟随用户选择的启动器图标。
 *
 * FoxSu 系列（默认 / 七彩 / 夜 / 夜·七彩）现在共用同一只「全局彩色狐狸」，
 * 因此仅 KernelSU 风格保留自己的品牌图形。
 */
data class AppIconVisual(
    val foreground: Int,
    val background: Color,
)

fun appIconVisual(style: Int): AppIconVisual = when (style) {
    1 -> AppIconVisual(R.drawable.ic_launcher_kernelsu_foreground, Color(0xFF14721F))
    // 七彩：米白底 · 彩色全局狐狸
    2 -> AppIconVisual(R.drawable.ic_fox_global, Color(0xFFF8F5EF))
    // 夜：墨底 · 彩色全局狐狸
    3 -> AppIconVisual(R.drawable.ic_fox_global, Color(0xFF0B0B0C))
    // 夜·七彩：墨底 · 彩色全局狐狸
    4 -> AppIconVisual(R.drawable.ic_fox_global, Color(0xFF0B0B0C))
    // 默认：白底 · 彩色全局狐狸
    else -> AppIconVisual(R.drawable.ic_fox_global, Color.White)
}
