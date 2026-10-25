package me.weishu.kernelsu.data.settings

import android.content.Context
import androidx.core.content.edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.weishu.kernelsu.ksuApp

/**
 * User personalisation of the ColorUI look: wallpaper, card surfaces and launcher icon.
 *
 * Backed by SharedPreferences so it survives restarts, and exposed as a [StateFlow] so the
 * home screen repaints immediately while the settings screen is being used.
 */
data class Customization(
    val wallpaperUri: String? = null,
    /** 1f = fill, larger values zoom in (custom cropping). */
    val wallpaperScale: Float = 1f,
    /** -0.4f..0.4f, horizontal pan of the wallpaper. */
    val wallpaperOffsetX: Float = 0f,
    /** -0.4f..0.4f, vertical pan of the wallpaper. */
    val wallpaperOffsetY: Float = 0f,
    /** 0f..0.8f black scrim over the wallpaper so text stays readable. */
    val wallpaperDim: Float = 0.15f,
    /** Optional picture used as the backdrop of the home hero cards. */
    val heroWallpaperUri: String? = null,
    val heroScale: Float = 1f,
    val heroOffsetX: Float = 0f,
    val heroOffsetY: Float = 0f,
    /** 0f..0.8f black scrim over the hero picture, keeps text readable. */
    val heroDim: Float = 0.35f,
    /** Pictures for the four home KPI cards, index 0..3, null = follow the card surface. */
    val kpiImages: List<String?> = List(KPI_CARD_COUNT) { null },
    /** Opacity of each KPI card picture, 0.2f..1f. */
    val kpiAlphas: List<Float> = List(KPI_CARD_COUNT) { 0.7f },
    /**
     * Per-card surface override: slot 0 is the hero card, 1..4 are the KPI cards.
     * 0 = follow the global card colour.
     */
    val surfaceColors: List<Int> = List(SURFACE_SLOT_COUNT) { 0 },
    /** Per-card opacity override, -1f = follow the global card opacity. */
    val surfaceAlphas: List<Float> = List(SURFACE_SLOT_COUNT) { -1f },
    /** 0 = follow the theme scheme, otherwise an ARGB color. */
    val cardColor: Int = 0,
    /** 0.2f..1f, applied on top of [cardColor] (or the themed card surface). */
    val cardAlpha: Float = 1f,
    /** 0 = FoxSu icon, 1 = original KernelSU icon. */
    val iconStyle: Int = 0,
)

/** Number of equal-sized KPI cards on the ColorUI home screen. */
const val KPI_CARD_COUNT = 4

/** Hero card + four KPI cards + the module list cards. */
const val SURFACE_SLOT_COUNT = KPI_CARD_COUNT + 2
const val HERO_SLOT = 0
const val MODULE_SLOT = KPI_CARD_COUNT + 1

object CustomizationStore {
    private const val PREFS = "customization"
    private const val KEY_WALLPAPER = "wallpaper_uri"
    private const val KEY_SCALE = "wallpaper_scale"
    private const val KEY_OFFSET_X = "wallpaper_offset_x"
    private const val KEY_OFFSET_Y = "wallpaper_offset_y"
    private const val KEY_DIM = "wallpaper_dim"
    private const val KEY_HERO_URI = "hero_wallpaper_uri"
    private const val KEY_HERO_SCALE = "hero_wallpaper_scale"
    private const val KEY_HERO_OFFSET_X = "hero_wallpaper_offset_x"
    private const val KEY_HERO_OFFSET_Y = "hero_wallpaper_offset_y"
    private const val KEY_HERO_DIM = "hero_wallpaper_dim"
    private const val KEY_KPI_IMAGE = "kpi_image_"
    private const val KEY_KPI_ALPHA = "kpi_alpha_"
    private const val KEY_SURFACE_COLOR = "surface_color_"
    private const val KEY_SURFACE_ALPHA = "surface_alpha_"
    private const val KEY_CARD_COLOR = "card_color"
    private const val KEY_CARD_ALPHA = "card_alpha"
    private const val KEY_ICON_STYLE = "icon_style"

    val cardColorOptions = listOf(
        0x00000000, // follow theme
        0xFFFFFFFF.toInt(),
        0xFFF1F6EF.toInt(),
        0xFFE8F1FB.toInt(),
        0xFF1A201A.toInt(),
        0xFF111827.toInt(),
        0xFFFFF4E5.toInt(),
        0xFFFDE8EF.toInt(),
        0xFFE9F7EF.toInt(),
    )

    private val prefs by lazy {
        ksuApp.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /** Compose-observable snapshot so every screen repaints as soon as it changes. */
    var current by mutableStateOf(read())
        private set

    private fun read(): Customization = with(prefs) {
        Customization(
            wallpaperUri = getString(KEY_WALLPAPER, null),
            wallpaperScale = getFloat(KEY_SCALE, 1f),
            wallpaperOffsetX = getFloat(KEY_OFFSET_X, 0f),
            wallpaperOffsetY = getFloat(KEY_OFFSET_Y, 0f),
            wallpaperDim = getFloat(KEY_DIM, 0.15f),
            heroWallpaperUri = getString(KEY_HERO_URI, null),
            heroScale = getFloat(KEY_HERO_SCALE, 1f),
            heroOffsetX = getFloat(KEY_HERO_OFFSET_X, 0f),
            heroOffsetY = getFloat(KEY_HERO_OFFSET_Y, 0f),
            heroDim = getFloat(KEY_HERO_DIM, 0.35f),
            kpiImages = List(KPI_CARD_COUNT) { getString(KEY_KPI_IMAGE + it, null) },
            kpiAlphas = List(KPI_CARD_COUNT) { getFloat(KEY_KPI_ALPHA + it, 0.7f) },
            surfaceColors = List(SURFACE_SLOT_COUNT) { getInt(KEY_SURFACE_COLOR + it, 0) },
            surfaceAlphas = List(SURFACE_SLOT_COUNT) { getFloat(KEY_SURFACE_ALPHA + it, -1f) },
            cardColor = getInt(KEY_CARD_COLOR, 0),
            cardAlpha = getFloat(KEY_CARD_ALPHA, 1f),
            iconStyle = getInt(KEY_ICON_STYLE, 0),
        )
    }

    fun setWallpaper(uri: String?) = update { it.copy(wallpaperUri = uri) }
    fun setScale(scale: Float) = update { it.copy(wallpaperScale = scale) }
    fun setOffsetX(offset: Float) = update { it.copy(wallpaperOffsetX = offset) }
    fun setOffsetY(offset: Float) = update { it.copy(wallpaperOffsetY = offset) }
    fun setDim(dim: Float) = update { it.copy(wallpaperDim = dim) }
    fun setHeroWallpaper(uri: String?) = update { it.copy(heroWallpaperUri = uri) }
    fun setHeroScale(scale: Float) = update { it.copy(heroScale = scale) }
    fun setHeroOffsetX(offset: Float) = update { it.copy(heroOffsetX = offset) }
    fun setHeroOffsetY(offset: Float) = update { it.copy(heroOffsetY = offset) }
    fun setHeroDim(dim: Float) = update { it.copy(heroDim = dim) }
    fun setKpiImage(index: Int, uri: String?) = update {
        if (index !in 0 until KPI_CARD_COUNT) it else it.copy(
            kpiImages = it.kpiImages.toMutableList().also { list -> list[index] = uri }
        )
    }

    fun setKpiAlpha(index: Int, alpha: Float) = update {
        if (index !in 0 until KPI_CARD_COUNT) it else it.copy(
            kpiAlphas = it.kpiAlphas.toMutableList().also { list -> list[index] = alpha }
        )
    }

    fun setSurfaceColor(index: Int, color: Int) = update {
        if (index !in 0 until SURFACE_SLOT_COUNT) it else it.copy(
            surfaceColors = it.surfaceColors.toMutableList().also { list -> list[index] = color }
        )
    }

    fun setSurfaceAlpha(index: Int, alpha: Float) = update {
        if (index !in 0 until SURFACE_SLOT_COUNT) it else it.copy(
            surfaceAlphas = it.surfaceAlphas.toMutableList().also { list -> list[index] = alpha }
        )
    }

    fun setCardColor(color: Int) = update { it.copy(cardColor = color) }
    fun setCardAlpha(alpha: Float) = update { it.copy(cardAlpha = alpha) }
    fun setIconStyle(style: Int) = update { it.copy(iconStyle = style) }

    fun reset() = update { Customization() }

    private fun update(transform: (Customization) -> Customization) {
        val next = transform(current)
        // Dragging a colour slider fires many identical values; skip the disk write for them.
        if (next == current) return
        prefs.edit {
            putString(KEY_WALLPAPER, next.wallpaperUri)
            putFloat(KEY_SCALE, next.wallpaperScale)
            putFloat(KEY_OFFSET_X, next.wallpaperOffsetX)
            putFloat(KEY_OFFSET_Y, next.wallpaperOffsetY)
            putFloat(KEY_DIM, next.wallpaperDim)
            putString(KEY_HERO_URI, next.heroWallpaperUri)
            putFloat(KEY_HERO_SCALE, next.heroScale)
            putFloat(KEY_HERO_OFFSET_X, next.heroOffsetX)
            putFloat(KEY_HERO_OFFSET_Y, next.heroOffsetY)
            putFloat(KEY_HERO_DIM, next.heroDim)
            repeat(KPI_CARD_COUNT) { index ->
                putString(KEY_KPI_IMAGE + index, next.kpiImages.getOrNull(index))
                putFloat(KEY_KPI_ALPHA + index, next.kpiAlphas.getOrNull(index) ?: 0.7f)
            }
            repeat(SURFACE_SLOT_COUNT) { index ->
                putInt(KEY_SURFACE_COLOR + index, next.surfaceColors.getOrNull(index) ?: 0)
                putFloat(KEY_SURFACE_ALPHA + index, next.surfaceAlphas.getOrNull(index) ?: -1f)
            }
            putInt(KEY_CARD_COLOR, next.cardColor)
            putFloat(KEY_CARD_ALPHA, next.cardAlpha)
            putInt(KEY_ICON_STYLE, next.iconStyle)
        }
        current = next
    }
}
