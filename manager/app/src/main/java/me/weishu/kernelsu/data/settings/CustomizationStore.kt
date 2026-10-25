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
    /** 0 = follow the theme scheme, otherwise an ARGB color. */
    val cardColor: Int = 0,
    /** 0.2f..1f, applied on top of [cardColor] (or the themed card surface). */
    val cardAlpha: Float = 1f,
    /** 0 = FoxSu icon, 1 = original KernelSU icon. */
    val iconStyle: Int = 0,
)

object CustomizationStore {
    private const val PREFS = "customization"
    private const val KEY_WALLPAPER = "wallpaper_uri"
    private const val KEY_SCALE = "wallpaper_scale"
    private const val KEY_OFFSET_X = "wallpaper_offset_x"
    private const val KEY_OFFSET_Y = "wallpaper_offset_y"
    private const val KEY_DIM = "wallpaper_dim"
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
    fun setCardColor(color: Int) = update { it.copy(cardColor = color) }
    fun setCardAlpha(alpha: Float) = update { it.copy(cardAlpha = alpha) }
    fun setIconStyle(style: Int) = update { it.copy(iconStyle = style) }

    fun reset() = update { Customization() }

    private fun update(transform: (Customization) -> Customization) {
        val next = transform(current)
        prefs.edit {
            putString(KEY_WALLPAPER, next.wallpaperUri)
            putFloat(KEY_SCALE, next.wallpaperScale)
            putFloat(KEY_OFFSET_X, next.wallpaperOffsetX)
            putFloat(KEY_OFFSET_Y, next.wallpaperOffsetY)
            putFloat(KEY_DIM, next.wallpaperDim)
            putInt(KEY_CARD_COLOR, next.cardColor)
            putFloat(KEY_CARD_ALPHA, next.cardAlpha)
            putInt(KEY_ICON_STYLE, next.iconStyle)
        }
        current = next
    }
}
