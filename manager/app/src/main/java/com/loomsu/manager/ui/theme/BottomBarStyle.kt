package com.loomsu.manager.ui.theme

enum class BottomBarStyle {
    /** 传统底栏：贴底全宽、顶部一条分隔线，选中项带胶囊底。 */
    TRADITIONAL,

    /** 胶囊底栏：带阴影与圆角的浮岛。 */
    CAPSULE,

    /** 侧边栏：竖排导航，放在屏幕左侧。 */
    RAIL,
    ;

    companion object {
        // 旧版本持久化：0 = Material3 底栏，1 = 浮岛底栏。
        // 新语义沿用这两个下标：0 → 传统、1 → 胶囊，新增 2 → 侧边。
        fun fromOrdinal(ordinal: Int): BottomBarStyle = entries.getOrElse(ordinal) { TRADITIONAL }
    }
}
