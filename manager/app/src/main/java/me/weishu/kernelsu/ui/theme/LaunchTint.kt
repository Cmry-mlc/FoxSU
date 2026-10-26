package me.weishu.kernelsu.ui.theme

import androidx.compose.ui.graphics.Color
import me.weishu.kernelsu.data.settings.RainbowCards

/**
 * 每次冷启动随机一套配色：标题栏的白底圆/胶囊用 [chip]，底栏图标用 [navColor]，
 * 主页/设置页/模块卡的七彩顺序也会被重新打乱，所以每次打开 App 观感都不一样。
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

    /** 底栏用的一组亮色，顺序与 [RainbowCards] 一一对应。 */
    private val navAccents = listOf(
        Color(0xFF3C7FC4), // 雾蓝
        Color(0xFF2F9E5B), // 薄荷
        Color(0xFFCC9412), // 奶油黄
        Color(0xFFD95F4C), // 蜜桃
        Color(0xFF7C5AD6), // 薰衣草
        Color(0xFF15938F), // 湖水
    )

    /** 打乱后的索引顺序：卡片配色和底栏配色共用，保证同一套色系呼应。 */
    private val order: List<Int> = RainbowCards.indices.shuffled()

    /** 取第 [index] 套七彩（循环）。 */
    fun rainbowPair(index: Int): Pair<Int, Int> = RainbowCards[order[index % order.size]]

    /** 每张卡片的浅色底。 */
    fun rainbowSurface(index: Int): Color = Color(rainbowPair(index).first)

    /** 每张卡片的深色文字/图标色。 */
    fun rainbowContent(index: Int): Color = Color(rainbowPair(index).second)

    /** 底栏第 [index] 个图标的颜色：同色系但更亮，白底胶囊上更清爽。 */
    fun navColor(index: Int): Color = navAccents[order[index % order.size]]
}
