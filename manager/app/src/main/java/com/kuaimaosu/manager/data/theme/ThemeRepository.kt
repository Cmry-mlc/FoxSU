package com.kuaimaosu.manager.data.theme

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.kuaimaosu.manager.data.AppSettingsRepository
import com.kuaimaosu.manager.domain.model.coerceCompatibleWith

class ThemeRepository(
    private val settings: AppSettingsRepository,
    private val monetCompatColorSource: MonetCompatColorSource,
) {
    fun saveThemeMode(forceDark: Boolean?) {
        settings.putString(
            "theme_mode",
            when (forceDark) {
                true -> "dark"
                false -> "light"
                null -> "system"
            },
        )
    }

    fun loadThemeMode(): Boolean? = when (settings.getString("theme_mode", "system")) {
        "dark" -> true
        "light" -> false
        else -> null
    }

    fun saveSeedColor(seedColor: Int) {
        settings.putInt("theme_seed_color", seedColor)
    }

    fun loadSeedColor(): Int {
        if (!settings.contains("theme_seed_color")) {
            val legacyThemeName = settings.getString("theme_colors", "default")
                ?: "default"
            legacySeedColor(legacyThemeName)?.let { migratedSeedColor ->
                settings.putInt("theme_seed_color", migratedSeedColor)
                return migratedSeedColor
            }
            return DEFAULT_SEED_COLOR
        }

        return settings.getInt("theme_seed_color", DEFAULT_SEED_COLOR)
    }

    fun defaultSeedColor(): Int = monetCompatColorSource.seedColor()

    /** 是否启用了用户自选主题色（关闭时保持设计稿的品牌色）。 */
    fun saveUseCustomThemeColor(enabled: Boolean) {
        settings.putBoolean("use_custom_theme_color", enabled)
    }

    fun loadUseCustomThemeColor(): Boolean = settings.getBoolean("use_custom_theme_color", true)

    fun saveHomeTitle(text: String) {
        settings.putString("home_title", text)
    }

    fun loadHomeTitle(): String = settings.getString("home_title", DEFAULT_HOME_TITLE)
        ?.takeIf { it.isNotBlank() } ?: DEFAULT_HOME_TITLE

    fun saveHomeTitleSplit(index: Int) {
        settings.putInt("home_title_split", index)
    }

    fun loadHomeTitleSplit(): Int = settings.getInt("home_title_split", DEFAULT_HOME_TITLE_SPLIT)

    fun saveHomeTitleColors(primary: Int, accent: Int) {
        settings.putInt("home_title_color_primary", primary)
        settings.putInt("home_title_color_accent", accent)
    }

    fun loadHomeTitleColorPrimary(): Int = settings.getInt(
        "home_title_color_primary",
        DEFAULT_HOME_TITLE_COLOR_PRIMARY,
    )

    fun loadHomeTitleColorAccent(): Int = settings.getInt(
        "home_title_color_accent",
        DEFAULT_HOME_TITLE_COLOR_ACCENT,
    )

    /** 标题上方的小字，默认是一句诗句。 */
    fun saveHomeKicker(text: String) {
        settings.putString("home_kicker", text)
    }

    fun loadHomeKicker(): String = settings.getString("home_kicker", DEFAULT_HOME_KICKER)
        ?.takeIf { it.isNotBlank() } ?: DEFAULT_HOME_KICKER

    /** 小字是否轮播：开启后每 6 秒换一句（第一句始终是用户自己填的那句）。 */
    fun saveKickerTicker(enabled: Boolean) {
        settings.putBoolean("kicker_ticker", enabled)
    }

    fun loadKickerTicker(): Boolean = settings.getBoolean("kicker_ticker", true)

    fun saveAppFont(id: String) {
        settings.putString("app_font_id", id)
    }

    fun loadAppFont(): String = settings.getString("app_font_id", "system") ?: "system"

    /** 诗句生成用的 OpenAI 兼容接口配置（留空则只用本地生成器）。 */
    fun saveAiConfig(endpoint: String, apiKey: String, model: String) {
        settings.putString("ai_endpoint", endpoint)
        settings.putString("ai_key", apiKey)
        settings.putString("ai_model", model)
    }

    fun loadAiEndpoint(): String = settings.getString("ai_endpoint", "") ?: ""

    fun loadAiKey(): String = settings.getString("ai_key", "") ?: ""

    fun loadAiModel(): String = settings.getString("ai_model", "gpt-4o-mini") ?: "gpt-4o-mini"

    companion object {
        /** 默认全局色：近黑。整套强调色默认走黑色系，卡片因此也是黑卡。 */
        val DEFAULT_SEED_COLOR = 0xFF71717A.toInt()

        const val DEFAULT_HOME_KICKER = "此心安处是吾乡。"
        const val DEFAULT_HOME_TITLE = "KuaiMaoSU"
        const val DEFAULT_HOME_TITLE_SPLIT = 5
        val DEFAULT_HOME_TITLE_COLOR_PRIMARY = 0xFF101322.toInt()
        val DEFAULT_HOME_TITLE_COLOR_ACCENT = 0xFF4F5BE8.toInt()
    }

    fun saveDynamicColorState(enabled: Boolean) {
        settings.putBoolean("use_dynamic_color", enabled)
    }

    fun loadDynamicColorState(): Boolean = settings.getBoolean("use_dynamic_color", true)

    fun saveDynamicColorSpec(
        spec: ColorSpec.SpecVersion,
        currentPaletteStyle: PaletteStyle,
    ): PaletteStyle {
        settings.putString("dynamic_color_spec", spec.name)
        val compatibleStyle = currentPaletteStyle.coerceCompatibleWith(spec)
        if (compatibleStyle != currentPaletteStyle) {
            settings.putString("dynamic_palette_style", compatibleStyle.name)
        }
        return compatibleStyle
    }

    fun loadDynamicColorSpec(): ColorSpec.SpecVersion {
        val specName = settings.getString(
            "dynamic_color_spec",
            ColorSpec.SpecVersion.SPEC_2021.name,
        )
        return ColorSpec.SpecVersion.entries
            .find { it.name == specName }
            ?: ColorSpec.SpecVersion.SPEC_2021
    }

    fun saveDynamicPaletteStyle(
        style: PaletteStyle,
        spec: ColorSpec.SpecVersion,
    ): PaletteStyle {
        val compatibleStyle = style.coerceCompatibleWith(spec)
        settings.putString("dynamic_palette_style", compatibleStyle.name)
        return compatibleStyle
    }

    fun loadDynamicPaletteStyle(spec: ColorSpec.SpecVersion): PaletteStyle {
        val styleName = settings.getString(
            "dynamic_palette_style",
            PaletteStyle.TonalSpot.name,
        )
        val storedStyle = PaletteStyle.entries
            .find { it.name == styleName }
            ?: PaletteStyle.TonalSpot
        val compatibleStyle = storedStyle.coerceCompatibleWith(spec)
        if (compatibleStyle != storedStyle) {
            settings.putString("dynamic_palette_style", compatibleStyle.name)
        }
        return compatibleStyle
    }

    private fun legacySeedColor(name: String): Int? = when (name.lowercase()) {
        "green" -> 0xFF4C662B.toInt()
        "purple" -> 0xFF7C4E7E.toInt()
        "orange" -> 0xFF8B4F24.toInt()
        "pink" -> 0xFF8C4A60.toInt()
        "gray" -> 0xFF5B5C5C.toInt()
        "yellow" -> 0xFF6D5E0F.toInt()
        else -> null
    }
}
