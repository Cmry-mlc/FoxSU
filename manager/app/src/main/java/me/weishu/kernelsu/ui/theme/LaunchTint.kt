package me.weishu.kernelsu.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 每次冷启动随机一套配色：标题栏的白底圆/胶囊用 [chip]，底栏选中图标用同色系的 [accent]，
 * 两者始终成对出现，保证“标题栏与底栏颜色一致”但又有呼吸感。
 */
object LaunchTint {
    private val pairs = listOf(
        Color(0xFFE2F5E6) to Color(0xFF2E8B4F),
        Color(0xFFEFE6FB) to Color(0xFF7C4DDB),
        Color(0xFFFFF3D2) to Color(0xFFC08415),
        Color(0xFFFFE4E0) to Color(0xFFD2604F),
        Color(0xFFDDEBFA) to Color(0xFF2F6FB5),
        Color(0xFFDFF3F3) to Color(0xFF14807F),
    )

    private val picked = pairs.random()

    val chip: Color = picked.first
    val accent: Color = picked.second
}
