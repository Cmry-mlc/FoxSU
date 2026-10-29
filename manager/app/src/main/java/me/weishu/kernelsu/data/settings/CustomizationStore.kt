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
    /** Per-module card background picture, keyed by module id. */
    val moduleImages: Map<String, String> = emptyMap(),
    /** Opacity of the module card pictures. */
    val moduleImageAlpha: Float = 0.7f,
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
    /** Title shown at the top of the home screen; blank = the app name. */
    val homeTitle: String = "",
    /** 首页标题文字颜色：0 = 跟随主题（Ag 主题下是品牌渐变），否则 ARGB。 */
    val titleColor: Int = 0,
    /** 0 = pill bar, 1 = drawer bar that expands the selected tab. */
    val bottomBarStyle: Int = 0,
    val dockStyle: String = "d0",
    /**
     * Editorial（Material 皮肤）是否使用 Redesign 的胶囊底栏。
     * 默认 false：Editorial 保留自己的「纸面」底栏——胶囊是 Redesign 的语言，
     * 强行套在杂志版面上会破坏版式（也容易被误认成主题 BUG）。
     */
    val dockOnEditorial: Boolean = false,
    /** Tint the four home KPI cards with the built-in palette (light mode only). */
    val homePalette: Boolean = true,
    /** Corner radius of the floating bottom bar, in dp. 38 已是完整药丸（76dp 高的一半）。 */
    val bottomBarRadius: Float = 38f,
    /** 鸿蒙式沉浸光感：底栏加一层柔光渐变，更通透。 */
    val bottomBarImmersive: Boolean = false,
    /** 底栏液态玻璃：玻璃条 + 可拖动高光，完全覆盖沉浸光感。 */
    val bottomBarLiquidGlass: Boolean = false,
    // 底栏图标七彩：每个 tab 用一套随机彩色（默认关）
    val navRainbow: Boolean = false,
    // 底栏图标莫奈取色：跟随系统动态色（默认关）
    val navMonet: Boolean = false,
    /** 底栏不透明度，1f = 完全不透明（默认）。 */
    val bottomBarAlpha: Float = 1f,
    /** 玻璃质感卡面：接近全透明的白色薄片 + 白色描边。 */
    val cardGlass: Boolean = false,
    /** 是否启用全局背景图。 */
    val wallpaperEnabled: Boolean = true,
    /** 玻璃卡面专用背景图；空 = 使用系统壁纸。 */
    val glassWallpaper: String? = null,
    /** 七彩卡片：首页/模块卡全部换成柔和的七彩配色。 */
    val rainbowCards: Boolean = true,
    /** 0 = 系统默认，1 = 衬线，2 = 等宽，3 = 手写圆体。 */
    val fontStyle: Int = 0,
    /** 用户导入的系统字体文件（应用私有目录内的绝对路径）。 */
    val fontPath: String? = null,
    /** Dock 图标造型：100 圆角方（默认）/ 101 正圆 / 102 超椭圆 / 103 七彩（渐变实底）。 */
    val iconStyle: Int = 100,
    /** 桌面启动器图标形象：0 彩虹狐狸 / 1 KernelSU / 2 招手 / 3 回眸 / 4 开心。 */
    val launcherIcon: Int = 0,
    /** 全局强调色：0..6 对应 AccentPalettes（7 套取自 logo 的配色）。 */
    val accentPalette: Int = 0,
    /** 侧边胶囊停靠：0 = 左侧（默认），1 = 右侧。 */
    val dockSide: Int = 0,
)

/** Number of equal-sized KPI cards on the ColorUI home screen. */
const val KPI_CARD_COUNT = 4

/** 七彩配色：依次覆盖大卡、四张 KPI 与模块卡。 */
val RainbowCards = listOf(
    0xFFDDEBFA.toInt() to 0xFF17324D.toInt(), // 雾蓝
    0xFFE2F5E6.toInt() to 0xFF14401F.toInt(), // 薄荷
    0xFFFFF3D2.toInt() to 0xFF4A3710.toInt(), // 奶油黄
    0xFFFFE4E0.toInt() to 0xFF4A1F1A.toInt(), // 蜜桃
    0xFFEFE6FB.toInt() to 0xFF2E2148.toInt(), // 薰衣草
    0xFFDFF3F3.toInt() to 0xFF0F3A3A.toInt(), // 湖水
    0xFFFCE8F3.toInt() to 0xFF4A1733.toInt(), // 樱粉
    0xFFE8F1D8.toInt() to 0xFF2F3A12.toInt(), // 青柠
    0xFFEDE9DC.toInt() to 0xFF3A3418.toInt(), // 沙金
    0xFFF6E3D2.toInt() to 0xFF431F0C.toInt(), // 焦糖
    0xFFE0E7F7.toInt() to 0xFF1C2550.toInt(), // 靛蓝
    0xFFF2E2F7.toInt() to 0xFF3C1440.toInt(), // 紫罗兰
)

/** 夜间七彩：深色卡片底 + 亮色图标，保证在黑底上也是一眼能认出的彩色卡片。 */
val RainbowCardsDark = listOf(
    0xFF172A3D.toInt() to 0xFF9EC9F0.toInt(), // 雾蓝
    0xFF14301F.toInt() to 0xFF9BE0B4.toInt(), // 薄荷
    0xFF34290F.toInt() to 0xFFF0CE83.toInt(), // 奶油黄
    0xFF381A16.toInt() to 0xFFF2A99C.toInt(), // 蜜桃
    0xFF251A3D.toInt() to 0xFFC4A9F5.toInt(), // 薰衣草
    0xFF10302F.toInt() to 0xFF8ADCD8.toInt(), // 湖水
    0xFF36142A.toInt() to 0xFFF1A6CD.toInt(), // 樱粉
    0xFF232B12.toInt() to 0xFFCBDD97.toInt(), // 青柠
    0xFF2C2814.toInt() to 0xFFE4D69B.toInt(), // 沙金
    0xFF32190E.toInt() to 0xFFEDB489.toInt(), // 焦糖
    0xFF161E42.toInt() to 0xFFA3B4F0.toInt(), // 靛蓝
    0xFF2E1235.toInt() to 0xFFDDA6E8.toInt(), // 紫罗兰
)

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
    private const val KEY_MODULE_IMAGE_PREFIX = "module_image::"
    private const val KEY_MODULE_IMAGE_ALPHA = "module_image_alpha"
    private const val KEY_SURFACE_COLOR = "surface_color_"
    private const val KEY_SURFACE_ALPHA = "surface_alpha_"
    private const val KEY_CARD_COLOR = "card_color"
    private const val KEY_CARD_ALPHA = "card_alpha"
    private const val KEY_HOME_TITLE = "home_title"
    private const val KEY_TITLE_COLOR = "title_color"
    private const val KEY_BOTTOM_STYLE = "bottom_bar_style"
private const val KEY_DOCK_STYLE = "dock_style"
    private const val KEY_DOCK_EDITORIAL = "dock_on_editorial"
    private const val KEY_HOME_PALETTE = "home_palette"
    private const val KEY_BOTTOM_RADIUS = "bottom_bar_radius"
    private const val KEY_BOTTOM_IMMERSIVE = "bottom_bar_immersive"
    private const val KEY_BOTTOM_LIQUID_GLASS = "bottom_bar_liquid_glass"
    private const val KEY_NAV_RAINBOW = "nav_rainbow"
    private const val KEY_NAV_MONET = "nav_monet"
    private const val KEY_BOTTOM_ALPHA = "bottom_bar_alpha"
    private const val KEY_FONT_PATH = "font_path"
    private const val KEY_CARD_GLASS = "card_glass"
    private const val KEY_WALLPAPER_ENABLED = "wallpaper_enabled"
    private const val KEY_GLASS_WALLPAPER = "glass_wallpaper"
    private const val KEY_RAINBOW = "rainbow_cards"
    private const val KEY_FONT_STYLE = "font_style"
    private const val KEY_ICON_STYLE = "icon_style"
    private const val KEY_LAUNCHER_ICON = "launcher_icon"
    private const val KEY_ACCENT_PALETTE = "accent_palette"
    private const val KEY_DOCK_SIDE = "dock_side"

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
            moduleImages = all.entries
                .filter { it.key.startsWith(KEY_MODULE_IMAGE_PREFIX) && it.value is String }
                .associate { it.key.removePrefix(KEY_MODULE_IMAGE_PREFIX) to it.value as String },
            moduleImageAlpha = getFloat(KEY_MODULE_IMAGE_ALPHA, 0.7f),
            surfaceColors = List(SURFACE_SLOT_COUNT) { getInt(KEY_SURFACE_COLOR + it, 0) },
            surfaceAlphas = List(SURFACE_SLOT_COUNT) { getFloat(KEY_SURFACE_ALPHA + it, -1f) },
            cardColor = getInt(KEY_CARD_COLOR, 0),
            cardAlpha = getFloat(KEY_CARD_ALPHA, 1f),
            homeTitle = getString(KEY_HOME_TITLE, "").orEmpty(),
            titleColor = getInt(KEY_TITLE_COLOR, 0),
            bottomBarStyle = getInt(KEY_BOTTOM_STYLE, 0),
            dockStyle = getString(KEY_DOCK_STYLE, "d0").orEmpty(),
            dockOnEditorial = getBoolean(KEY_DOCK_EDITORIAL, false),
            homePalette = getBoolean(KEY_HOME_PALETTE, true),
            bottomBarRadius = getFloat(KEY_BOTTOM_RADIUS, 38f),
            bottomBarImmersive = getBoolean(KEY_BOTTOM_IMMERSIVE, false),
            bottomBarLiquidGlass = getBoolean(KEY_BOTTOM_LIQUID_GLASS, false),
            navRainbow = getBoolean(KEY_NAV_RAINBOW, false),
            navMonet = getBoolean(KEY_NAV_MONET, false),
            bottomBarAlpha = getFloat(KEY_BOTTOM_ALPHA, 1f),
            cardGlass = getBoolean(KEY_CARD_GLASS, false),
            wallpaperEnabled = getBoolean(KEY_WALLPAPER_ENABLED, true),
            glassWallpaper = getString(KEY_GLASS_WALLPAPER, null),
            rainbowCards = getBoolean(KEY_RAINBOW, true),
            fontStyle = getInt(KEY_FONT_STYLE, 0),
            fontPath = getString(KEY_FONT_PATH, null),
            iconStyle = getInt(KEY_ICON_STYLE, 100),
            // 老版本把启动器形象塞在 iconStyle(0/2/3/4) 里，首次升级时继承过来
            launcherIcon = if (contains(KEY_LAUNCHER_ICON)) {
                getInt(KEY_LAUNCHER_ICON, 0)
            } else {
                getInt(KEY_ICON_STYLE, 100).takeIf { it in 0..4 } ?: 0
            },
            accentPalette = getInt(KEY_ACCENT_PALETTE, 0),
            dockSide = getInt(KEY_DOCK_SIDE, 0),
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
    fun setModuleImage(moduleId: String, uri: String?) = update {
        val images = it.moduleImages.toMutableMap()
        if (uri == null) images.remove(moduleId) else images[moduleId] = uri
        it.copy(moduleImages = images)
    }
    fun setModuleImageAlpha(alpha: Float) = update { it.copy(moduleImageAlpha = alpha) }
    fun setCardAlpha(alpha: Float) = update { it.copy(cardAlpha = alpha) }
    fun setHomeTitle(title: String) = update { it.copy(homeTitle = title.take(24)) }

    /** 0 = 跟随主题；否则写入 ARGB。 */
    fun setTitleColor(argb: Int) = update { it.copy(titleColor = argb) }
    fun setBottomBarStyle(style: Int) = update { it.copy(bottomBarStyle = style) }
    fun setDockStyle(style: String) = update { it.copy(dockStyle = style) }
    fun setDockOnEditorial(enabled: Boolean) = update { it.copy(dockOnEditorial = enabled) }

    fun setHomePalette(enabled: Boolean) = update { it.copy(homePalette = enabled) }
    fun setBottomBarRadius(radius: Float) = update { it.copy(bottomBarRadius = radius) }
    fun setBottomBarImmersive(enabled: Boolean) = update { it.copy(bottomBarImmersive = enabled) }
    fun setBottomBarLiquidGlass(enabled: Boolean) = update { it.copy(bottomBarLiquidGlass = enabled) }
    fun setNavRainbow(enabled: Boolean) = update { it.copy(navRainbow = enabled) }
    fun setNavMonet(enabled: Boolean) = update { it.copy(navMonet = enabled) }
    fun setBottomBarAlpha(alpha: Float) = update { it.copy(bottomBarAlpha = alpha.coerceIn(0.2f, 1f)) }
    fun setCardGlass(enabled: Boolean) = update { it.copy(cardGlass = enabled) }
    fun setWallpaperEnabled(enabled: Boolean) = update { it.copy(wallpaperEnabled = enabled) }
    fun setGlassWallpaper(uri: String?) = update { it.copy(glassWallpaper = uri) }
    fun setRainbowCards(enabled: Boolean) = update { it.copy(rainbowCards = enabled) }
    fun setFontStyle(style: Int) = update { it.copy(fontStyle = style) }
    fun setFontPath(path: String?) = update { it.copy(fontPath = path) }
    fun setIconStyle(style: Int) = update { it.copy(iconStyle = style) }
    fun setLauncherIcon(style: Int) = update { it.copy(launcherIcon = style) }
    fun setAccentPalette(index: Int) = update { it.copy(accentPalette = index.coerceIn(0, 6)) }
    fun setDockSide(side: Int) = update { it.copy(dockSide = side.coerceIn(0, 1)) }

    fun reset() = update { Customization() }

    private fun update(transform: (Customization) -> Customization) {
        val next = transform(current)
        // Dragging a colour slider fires many identical values; skip the disk write for them.
        if (next == current) return
        // 逐模块图片：先清掉旧键再写，避免残留已删除模块的图片
        val staleModuleKeys = prefs.all.keys.filter { it.startsWith(KEY_MODULE_IMAGE_PREFIX) }
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
            staleModuleKeys.forEach { remove(it) }
            next.moduleImages.forEach { (id, uri) -> putString(KEY_MODULE_IMAGE_PREFIX + id, uri) }
            putFloat(KEY_MODULE_IMAGE_ALPHA, next.moduleImageAlpha)
            putString(KEY_HOME_TITLE, next.homeTitle)
            putInt(KEY_TITLE_COLOR, next.titleColor)
            putInt(KEY_BOTTOM_STYLE, next.bottomBarStyle)
            putString(KEY_DOCK_STYLE, next.dockStyle)
            putBoolean(KEY_DOCK_EDITORIAL, next.dockOnEditorial)
            putBoolean(KEY_HOME_PALETTE, next.homePalette)
            putFloat(KEY_BOTTOM_RADIUS, next.bottomBarRadius)
            putBoolean(KEY_BOTTOM_IMMERSIVE, next.bottomBarImmersive)
            putBoolean(KEY_BOTTOM_LIQUID_GLASS, next.bottomBarLiquidGlass)
            putBoolean(KEY_NAV_RAINBOW, next.navRainbow)
            putBoolean(KEY_NAV_MONET, next.navMonet)
            putFloat(KEY_BOTTOM_ALPHA, next.bottomBarAlpha)
            putBoolean(KEY_CARD_GLASS, next.cardGlass)
            putBoolean(KEY_WALLPAPER_ENABLED, next.wallpaperEnabled)
            putString(KEY_GLASS_WALLPAPER, next.glassWallpaper)
            putBoolean(KEY_RAINBOW, next.rainbowCards)
            putInt(KEY_FONT_STYLE, next.fontStyle)
            putString(KEY_FONT_PATH, next.fontPath)
            putInt(KEY_ICON_STYLE, next.iconStyle)
            putInt(KEY_LAUNCHER_ICON, next.launcherIcon)
            putInt(KEY_ACCENT_PALETTE, next.accentPalette)
            putInt(KEY_DOCK_SIDE, next.dockSide)
        }
        current = next
    }
}
