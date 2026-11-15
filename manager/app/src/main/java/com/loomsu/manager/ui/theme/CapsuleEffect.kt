package com.loomsu.manager.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * 胶囊底栏的质感。
 *
 * 三档共用同一套液态指示块（选中项会随手指流动、拉伸），
 * 区别只在胶囊本体的材质：是否做背景折射、糊多少、透多少。
 * 底栏与顶栏都取同一份 [barMaterial]，选了哪一档就得真的变一档。
 */
enum class CapsuleEffect(
    val id: String,
    val label: String,
    val summary: String,
) {
    /** 液态玻璃：背景实时模糊 + 顶部高光 + 内侧边缘折射感，最接近参考视频。 */
    LIQUID_GLASS(
        id = "liquid_glass",
        label = "液态玻璃",
        summary = "背景实时折射 + 高光描边，选中块随手指流动",
    ),

    /** 模糊磨砂：重磨砂，背景糊成一层柔光，几乎不透出内容。 */
    FROSTED(
        id = "frosted",
        label = "模糊磨砂",
        summary = "重磨砂质感，背景糊成柔光，不透视内容",
    ),

    /** 半透明：不做模糊，只留一层半透薄膜。 */
    TRANSLUCENT(
        id = "translucent",
        label = "半透明",
        summary = "不模糊，只留半透薄膜，能看见背景轮廓",
    ),
    ;

    companion object {
        fun fromId(id: String?): CapsuleEffect = entries.find { it.id == id } ?: LIQUID_GLASS
    }
}

/**
 * 三档质感在「常驻栏」上的渲染参数。
 *
 * 以前 [CapsuleEffect] 只被当成「开 / 关模糊」的布尔用，液态玻璃和模糊磨砂
 * 走的是完全一样的分支，半透明也只是把模糊关掉，所以选了跟没选一样。
 * 这里把三档的模糊强度、膜厚、折射开关集中定义，胶囊底栏、传统底栏、
 * 顶栏都从这里取值，保证同一个选项在三处表现一致。
 */
@Immutable
data class BarMaterial(
    /** 模糊半径倍率；0 表示完全不做模糊（半透明档）。 */
    val blurScale: Float,
    /** 着色膜厚度倍率。 */
    val tintScale: Float,
    /** 是否启用折射与高光描边，只有液态玻璃有。 */
    val refraction: Boolean,
)

val CapsuleEffect.barMaterial: BarMaterial
    get() = when (this) {
        CapsuleEffect.LIQUID_GLASS -> BarMaterial(
            blurScale = 1f,
            tintScale = 1f,
            refraction = true,
        )

        // 重磨砂：半径接近液态玻璃的两倍，膜更实，背景糊成一层柔光。
        CapsuleEffect.FROSTED -> BarMaterial(
            blurScale = 1.9f,
            tintScale = 1.16f,
            refraction = false,
        )

        // 半透明：不模糊、不折射，只留一层薄到能看见背景轮廓的膜。
        CapsuleEffect.TRANSLUCENT -> BarMaterial(
            blurScale = 0f,
            tintScale = 0.52f,
            refraction = false,
        )
    }

/**
 * 常驻栏（顶栏 / 底栏）的膜底色。
 *
 * 暗底配深膜、亮底配浅膜：自定义壁纸偏暗时顶栏文字是白的，
 * 若膜仍用浅色卡面就会变成「白字压白膜」，之前反复修好的对比度又坏了。
 */
@Composable
@ReadOnlyComposable
fun barSurfaceColor(): Color =
    if (LocalDarkBackdrop.current) Color(0xFF1C1C1C) else LoomColors.canvasCardSolid
