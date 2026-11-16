package com.kuaimaosu.manager.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.zIndex
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kuaimaosu.manager.data.AppSettingsRepository
import com.kuaimaosu.manager.data.theme.MonetCompatColorSource
import com.kuaimaosu.manager.data.theme.ThemeRepository
import com.kuaimaosu.manager.ui.overscroll.StretchOverscrollCompensationState
import com.kuaimaosu.manager.ui.util.LocalBackgroundBlurAnchor
import com.kuaimaosu.manager.ui.util.LocalBackgroundBlurProvided
import com.kuaimaosu.manager.ui.util.LocalBlurState
import com.kuaimaosu.manager.ui.util.LocalPagerPage
import com.kuaimaosu.manager.ui.util.LocalPagerState
import com.kuaimaosu.manager.ui.util.LocalStretchOverscrollCompensationState
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.runtimeShaderEffect
import top.yukonga.miuix.kmp.blur.textureBlurEffect

@Stable
class ThemeConfig(
    private val defaultSeedColor: () -> Int,
) {
    var customBackgroundUri by mutableStateOf<Uri?>(null)
    var backgroundDim by mutableFloatStateOf(0f)

    /**
     * 自定义壁纸的模糊强度（0..1）。
     *
     * 0 = 原图，1 = 最糊。默认 0.5：壁纸只当底纹看，卡片上的字才读得清，
     * 也最接近源码「壁纸整体虚化 + 半透明卡片」的观感。
     */
    var backgroundBlur by mutableFloatStateOf(0.5f)
    var forceDarkMode by mutableStateOf<Boolean?>(null)
    var seedColor by mutableIntStateOf(0xFF71717A.toInt())
    var useDynamicColor by mutableStateOf(false)
    var useBuiltinMonoFont by mutableStateOf(false)
    var monetCompatSeedColor by mutableIntStateOf(defaultSeedColor())
    var dynamicColorSpec by mutableStateOf(ColorSpec.SpecVersion.SPEC_2021)
    var dynamicPaletteStyle by mutableStateOf(PaletteStyle.TonalSpot)

    var backgroundImageLoaded by mutableStateOf(false)
    var isThemeChanging by mutableStateOf(false)
    var isHighContrastMode by mutableStateOf(false)
    private val _isEnableBlur = mutableStateOf(true)
    var isEnableBlur: Boolean
        get() = _isEnableBlur.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        set(value) {
            _isEnableBlur.value = value
        }
    private val _isEnableBlurExp = mutableStateOf(true)
    var isEnableBlurExp: Boolean
        get() = _isEnableBlurExp.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        set(value) {
            _isEnableBlurExp.value = value
        }
    var isUseBackgroundSeedColor by mutableStateOf(false)
    var bottomBarStyle by mutableStateOf(BottomBarStyle.TRADITIONAL)

    /** 动态背景特效（全部程序化绘制）。 */
    var backgroundEffect by mutableStateOf(BackgroundEffect.NONE)

    /** 胶囊底栏的质感：液态玻璃 / 模糊磨砂 / 半透明。 */
    var capsuleEffect by mutableStateOf(CapsuleEffect.LIQUID_GLASS)

    /**
     * ColorOS 17 风格「流光轮廓」：大卡片与胶囊底栏描一圈贴着圆角流动的轮廓光。
     *
     * 默认关闭——它是纯装饰，开不开都不影响可读性，交给用户自己选。
     */
    var isEdgeLight by mutableStateOf(false)

    /** 应用字体：system 为系统默认，其它值为 AppFonts.catalog 里的 id。 */
    var appFontId by mutableStateOf(AppFonts.SYSTEM_ID)

    /** 首页标题上方的小字（可自定义，也可让 AI 生成）。 */
    var homeKicker by mutableStateOf(ThemeRepository.DEFAULT_HOME_KICKER)

    /** 小字是否轮播（每 6 秒淡入换一句）。 */
    var kickerTicker by mutableStateOf(true)

    /** 是否启用用户自选的主题色（未启用时保持设计稿固定品牌色）。 */
    var useCustomThemeColor by mutableStateOf(true)

    /** 首页问候标题文案，以及「双色标题」的分段位置与两种颜色。 */
    var homeTitle by mutableStateOf("KuaiMaoSU")
    var homeTitleSplit by mutableIntStateOf(5)
    var homeTitleColorPrimary by mutableIntStateOf(0xFF101322.toInt())
    var homeTitleColorAccent by mutableIntStateOf(0xFF71717A.toInt())

    fun resetBackgroundState() {
        backgroundImageLoaded = false
        isThemeChanging = true
    }

    fun reset() {
        customBackgroundUri = null
        backgroundBlur = 0.5f
        forceDarkMode = null
        seedColor = defaultSeedColor()
        useDynamicColor = false
        monetCompatSeedColor = defaultSeedColor()
        dynamicColorSpec = ColorSpec.SpecVersion.SPEC_2021
        dynamicPaletteStyle = PaletteStyle.TonalSpot
        backgroundImageLoaded = false
        isThemeChanging = false
        useCustomThemeColor = false
        homeTitle = "KuaiMaoSU"
        homeTitleSplit = 5
        homeTitleColorPrimary = 0xFF101322.toInt()
        homeTitleColorAccent = 0xFF4F5BE8.toInt()
        appFontId = AppFonts.SYSTEM_ID
        homeKicker = ThemeRepository.DEFAULT_HOME_KICKER
        kickerTicker = true
        backgroundEffect = BackgroundEffect.NONE
        capsuleEffect = CapsuleEffect.LIQUID_GLASS
        isEdgeLight = false
    }
}

private val BuiltinspaceFontFamily = FontFamily(
    Font(com.kuaimaosu.manager.R.font.jetbrains_mono),
)

@Composable
fun monospaceFontFamily(): FontFamily {
    val themeConfig = koinInject<ThemeConfig>()
    return if (themeConfig.useBuiltinMonoFont) {
        BuiltinspaceFontFamily
    } else {
        FontFamily.Monospace
    }
}

@Stable
class BackgroundRenderState {
    var imagePainter: AsyncImagePainter? by mutableStateOf(null)
    var imageBitmap: ImageBitmap? by mutableStateOf(null)
    var blurImageBitmap: ImageBitmap? by mutableStateOf(null)
    var blurViewportSize by mutableStateOf(IntSize(0, 0))
    var blurFrameTick by mutableIntStateOf(0)
    var scrollActive by mutableStateOf(false)

    /** 壁纸解码后的平均亮度（0..1），没壁纸时保持 1。 */
    var wallpaperLuminance by mutableFloatStateOf(1f)

    /** 壁纸是否已经解码完成，用来判断亮度值是否可信。 */
    var wallpaperReady by mutableStateOf(false)
    private val scrollingPagers = mutableStateMapOf<Any, Unit>()
    val pagerScrollActive: Boolean
        get() = scrollingPagers.isNotEmpty()
    var seedColor by mutableIntStateOf(0)

    internal fun updatePagerScroll(owner: Any, active: Boolean) {
        if (active) {
            scrollingPagers[owner] = Unit
        } else {
            scrollingPagers.remove(owner)
        }
    }
}

val LocalBackgroundRenderState = compositionLocalOf<BackgroundRenderState> {
    error("BackgroundRenderState is not provided")
}

class BackgroundManager(
    private val settings: AppSettingsRepository,
    private val config: ThemeConfig,
    private val cardConfig: CardConfig,
) {
    private val tag = "BackgroundManager"

    fun saveBackgroundDim(dim: Float) {
        config.backgroundDim = dim
        settings.putFloat("background_dim", dim)
    }

    fun saveBackgroundBlur(blur: Float) {
        val value = blur.coerceIn(0f, 1f)
        config.backgroundBlur = value
        settings.putFloat("background_blur", value)
    }

    fun saveEnableBlur(enable: Boolean) {
        config.isEnableBlur = enable
        settings.putBoolean("enable_blur", enable)
    }

    fun saveEnableBlurExp(enable: Boolean) {
        config.isEnableBlurExp = enable
        settings.putBoolean("enable_blur_exp", enable)
    }

    fun saveBottomBarStyle(style: BottomBarStyle) {
        config.bottomBarStyle = style
        settings.putInt("bottom_bar_style", style.ordinal)
    }

    fun saveCapsuleEffect(effect: CapsuleEffect) {
        config.capsuleEffect = effect
        settings.putString("capsule_effect", effect.id)
    }

    fun saveEdgeLight(enable: Boolean) {
        config.isEdgeLight = enable
        settings.putBoolean("edge_light", enable)
    }

    fun saveBackgroundEffect(effect: BackgroundEffect) {
        config.backgroundEffect = effect
        settings.putString("background_effect", effect.id)
    }

    fun saveUseBackgroundSeedColor(enable: Boolean) {
        config.isUseBackgroundSeedColor = enable
        settings.putBoolean("use_background_seed_color", enable)
    }

    fun saveEnableHighContrastMode(enable: Boolean) {
        config.isHighContrastMode = enable
        settings.putBoolean("high_contrast_mode", enable)
    }

    suspend fun saveAndApplyCustomBackground(
        context: Context,
        uri: Uri,
    ): Boolean {
        val appContext = context.applicationContext
        return try {
            val finalUri = withContext(Dispatchers.IO) {
                copyImageToInternalStorage(appContext, uri)
            } ?: return false

            saveBackgroundUri(finalUri)
            config.customBackgroundUri = finalUri
            cardConfig.updateBackground(true)
            withContext(Dispatchers.IO) {
                clearBackgroundBlurCache(appContext)
            }
            config.backgroundImageLoaded = false

            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "保存背景失败: ${e.message}", e)
            false
        }
    }

    fun clearCustomBackground(context: Context) {
        saveBackgroundUri(null)
        config.customBackgroundUri = null
        cardConfig.updateBackground(false)
        clearBackgroundBlurCache(context)
        config.backgroundImageLoaded = false
    }

    fun loadCustomBackground() {
        val prefs = settings
        // 一次性校准：自定义背景的观感定版为「卡片 50% / 模糊 50% / 暗度 0」。
        // 老版本把这些值存成了 100% / 45% / 其它，升级后卡片整片不透明，
        // 壁纸等于没开。这里只在第一次升级时抹平一次，之后用户怎么调就怎么存。
        if (!prefs.getBoolean("custom_bg_defaults_v2", false)) {
            prefs.putBoolean("custom_bg_defaults_v2", true)
            prefs.putFloat("background_blur", 0.5f)
            prefs.putFloat("background_dim", 0f)
            prefs.putFloat("card_alpha", 0.5f)
            prefs.putBoolean("is_custom_alpha_set", false)
            cardConfig.cardAlpha = 0.5f
        }
        // 再校准一次暗度：v2 之后这个值又被写成了 30%，壁纸被压暗、
        // 上面的字反而更糊。暗度归零，模糊仍按 50% 那档走。
        if (!prefs.getBoolean("custom_bg_defaults_v3", false)) {
            prefs.putBoolean("custom_bg_defaults_v3", true)
            prefs.putFloat("background_dim", 0f)
        }
        val uriString = prefs.getString("custom_background", null)

        val newUri = uriString?.toUri()

        if (config.customBackgroundUri?.toString() != newUri?.toString()) {
            Log.d(tag, "加载自定义背景: $uriString")
            config.customBackgroundUri = newUri
            config.backgroundImageLoaded = false
            cardConfig.updateBackground(newUri != null)
        }

        config.backgroundDim = prefs.getFloat("background_dim", 0f).coerceIn(0f, 1f)
        config.backgroundBlur = prefs.getFloat("background_blur", 0.5f).coerceIn(0f, 1f)
        // 卡片磨砂常开：关掉的话卡片只是一层半透明白，壁纸的花纹会直接穿到字上。
        // 现在由「背景模糊」滑杆统一控制糊的程度，不再单独给一个开关。
        config.isEnableBlurExp = true
        config.isUseBackgroundSeedColor = prefs.getBoolean("use_background_seed_color", false)
        config.isHighContrastMode = prefs.getBoolean("high_contrast_mode", false)
    }

    private fun saveBackgroundUri(uri: Uri?) {
        settings.putString("custom_background", uri?.toString())
    }

    fun clearBackgroundBlurCache(context: Context) {
        runCatching {
            backgroundBlurCacheFile(context).delete()
            legacyBackgroundBlurCacheDir(context).deleteRecursively()
        }.onFailure {
            Log.w(tag, "Failed to clear background blur cache: ${it.message}")
        }
    }

    private fun copyImageToInternalStorage(context: Context, uri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "custom_background.jpg"
            val file = File(context.filesDir, fileName)

            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(8 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
            inputStream.close()

            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e(tag, "复制图片失败: ${e.message}", e)
            null
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KernelSUTheme(
    dpi: Int = 0,
    darkTheme: Boolean? = null,
    dynamicColor: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val themeConfig = koinInject<ThemeConfig>()
    val themeRepository = koinInject<ThemeRepository>()
    val backgroundManager = koinInject<BackgroundManager>()
    val cardConfig = koinInject<CardConfig>()
    val settings = koinInject<AppSettingsRepository>()
    val backgroundRenderState = remember { BackgroundRenderState() }
    val resolvedDarkTheme = darkTheme ?: isInDarkTheme(themeConfig.forceDarkMode)
    val resolvedDynamicColor = dynamicColor ?: themeConfig.useDynamicColor
    val context = LocalContext.current
    val systemIsDark = isSystemInDarkTheme()

    ThemeInitializer(
        context = context,
        systemIsDark = systemIsDark,
        themeConfig = themeConfig,
        themeRepository = themeRepository,
        backgroundManager = backgroundManager,
        cardConfig = cardConfig,
        settings = settings,
    )

    val colorScheme = createColorScheme(
        resolvedDarkTheme,
        resolvedDynamicColor,
        themeConfig,
        backgroundRenderState,
    )

    // 全局应用字体：字体文件在私有目录，缺失时自动退回系统字体
    val appFontFamily = rememberAppFontFamily()

    SystemBarController(resolvedDarkTheme)

    val systemDensity = LocalDensity.current

    val density = remember(systemDensity, dpi) {
        if (dpi <= 0f) {
            systemDensity
        } else {
            val targetDensity = dpi / 160f
            Density(density = targetDensity, fontScale = systemDensity.fontScale)
        }
    }

    val kuaimaoPalette = remember(
        resolvedDarkTheme,
        themeConfig.useCustomThemeColor,
        themeConfig.seedColor,
    ) {
        kuaimaoPaletteFor(themeConfig, resolvedDarkTheme)
    }

    CompositionLocalProvider(
        LocalDensity provides density,
        LocalBackgroundRenderState provides backgroundRenderState,
        // 暗夜模式：整棵 UI 树切换到深色调色板；自选主题色时 brand 一族跟随 primary
        LocalKuaiMaoPalette provides kuaimaoPalette,
        LocalKuaiMaoFontFamily provides appFontFamily,
        // Text 不写 style 时也会走这一层，保证「换字体」是全 App 生效
        LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = appFontFamily),
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = generateTypography(themeConfig, appFontFamily),
        ) {
            val backgroundMotionConnection = remember(backgroundRenderState) {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        backgroundRenderState.scrollActive = true
                        return Offset.Zero
                    }

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity,
                    ): Velocity {
                        backgroundRenderState.scrollActive = false
                        return Velocity.Zero
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(backgroundMotionConnection),
            ) {
                BackgroundLayer(themeConfig, settings, backgroundRenderState)
                content()
            }
        }
    }
}

@Composable
private fun ThemeInitializer(
    context: Context,
    systemIsDark: Boolean,
    themeConfig: ThemeConfig,
    themeRepository: ThemeRepository,
    backgroundManager: BackgroundManager,
    cardConfig: CardConfig,
    settings: AppSettingsRepository,
) {
    LaunchedEffect(systemIsDark) {
        if (themeConfig.forceDarkMode == null) {
            cardConfig.apply {
                load()
                setThemeDefaults(systemIsDark)
                save()
            }
        }
    }

    LaunchedEffect(Unit) {
        // Composition can start before application startup has finished preloading
        // DataStore. Read it here first so the persisted background URI is never
        // mistaken for an unset value during process recreation.
        settings.preload()
        themeConfig.forceDarkMode = themeRepository.loadThemeMode()
        themeConfig.seedColor = themeRepository.loadSeedColor()
        themeConfig.useDynamicColor = themeRepository.loadDynamicColorState()
        themeConfig.dynamicColorSpec = themeRepository.loadDynamicColorSpec()
        themeConfig.dynamicPaletteStyle = themeRepository.loadDynamicPaletteStyle(
            themeConfig.dynamicColorSpec,
        )
        // 「启用模糊」开关持久化，默认开；关掉时底栏退回实色，卡片也不再磨砂。
        themeConfig.isEnableBlur = settings.getBoolean("enable_blur", true)
        themeConfig.bottomBarStyle = BottomBarStyle.fromOrdinal(settings.getInt("bottom_bar_style", 0))
        themeConfig.backgroundEffect = BackgroundEffect.fromId(settings.getString("background_effect", null))
        themeConfig.capsuleEffect = CapsuleEffect.fromId(settings.getString("capsule_effect", null))
        themeConfig.isEdgeLight = settings.getBoolean("edge_light", false)
        cardConfig.load()
        backgroundManager.loadCustomBackground()
    }

    MonetCompatInitializer(themeConfig, themeRepository)
}

@Composable
private fun MonetCompatInitializer(themeConfig: ThemeConfig, themeRepository: ThemeRepository) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
    val source = koinInject<MonetCompatColorSource>()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(source, themeConfig) {
        source.colors.collect { color ->
            themeConfig.monetCompatSeedColor = color
            themeConfig.seedColor = themeRepository.loadSeedColor()
        }
    }
    LaunchedEffect(source, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Allow the cached theme to draw before starting wallpaper IPC and palette work.
            withFrameNanos { }
            withFrameNanos { }
            source.refresh()
        }
    }
}

@Composable
private fun BackgroundLayer(
    themeConfig: ThemeConfig,
    settings: AppSettingsRepository,
    renderState: BackgroundRenderState,
) {
    val backgroundUri = remember(themeConfig.customBackgroundUri) {
        themeConfig.customBackgroundUri
    }

    LaunchedEffect(themeConfig.customBackgroundUri) {
        if (themeConfig.customBackgroundUri == null) {
            renderState.imageBitmap = null
            renderState.imagePainter = null
            renderState.blurImageBitmap = null
            renderState.wallpaperReady = false
            renderState.wallpaperLuminance = 1f
            renderState.seedColor = 0
            settings.remove("cached_seed_color")
        }
    }

    // Translucent surfaces must re-sample/re-map the background only while content is actually
    // moving: nested-scroll drags/flings (scrollActive) and horizontal pager swipes, which
    // translate layers without re-recording backdrop nodes (pagerScrollActive). At rest the loop
    // is suspended and no frame is produced, so hwui stays idle. The tick must never be bumped
    // from a draw callback, which would self-sustain through consumers inside the source subtree.
    LaunchedEffect(Unit) {
        snapshotFlow {
            renderState.imageBitmap != null &&
                (renderState.scrollActive || renderState.pagerScrollActive)
        }.collect { active ->
            if (!active) return@collect
            while (
                renderState.imageBitmap != null &&
                (renderState.scrollActive || renderState.pagerScrollActive)
            ) {
                withFrameNanos { }
                renderState.blurFrameTick = if (renderState.blurFrameTick == Int.MAX_VALUE) {
                    0
                } else {
                    renderState.blurFrameTick + 1
                }
            }
        }
    }

    // 全局画布层：只铺底色。动态背景特效由每个路由页的根节点绘制（见 NavContainer），
    // 那里同时也是「不透明兜底」，转场时上下两页不会互相透视。
    val canvasColor = KuaiMaoColors.canvasApp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(-2f)
            .onSizeChanged { size ->
                if (
                    size.width > 0 &&
                    size.height > 0 &&
                    renderState.blurViewportSize != size
                ) {
                    renderState.blurViewportSize = size
                    renderState.blurImageBitmap = null
                }
            }
            .drawBehind { drawRect(canvasColor) },
    )

    // 自定义背景图盖在画布之上；没有壁纸时上面的 drawBehind 就是最终底色。
    backgroundUri?.let { uri ->
        BackgroundInitializer(
            uri = uri,
            themeConfig = themeConfig,
            settings = settings,
            renderState = renderState,
        )
    }
}

private const val BACKGROUND_BLUR_RADIUS = 45f

/**
 * Captures background content for blurEffect child nodes,
 * It will only work when blurState available
 * @return modified modifier
 */
@Composable
fun Modifier.blurSource(): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this

    return LocalBlurState.current?.let {
        this.then(Modifier.layerBackdrop(it))
    } ?: this
}

/**
 * Render blur when backdrop available
 * Falls back to redrawing the app background below the content so translucent bars do not show
 * scrolling content through them when blur is disabled or unavailable.
 * @param compensateHorizontalOverscroll reverse horizontal stretch in the sampled background
 * @param compensateVerticalOverscroll reverse vertical stretch in the sampled background
 * @param useFixedSurfaceBoundsForOverscroll use this surface's bounds with the published stretch
 * amount; fixed navigation surfaces use this to stay visually stationary outside the scroller
 * @return modified modifier
 */
@Composable
fun Modifier.blurEffect(
    compensateHorizontalOverscroll: Boolean = true,
    compensateVerticalOverscroll: Boolean = false,
    useFixedSurfaceBoundsForOverscroll: Boolean = false,
    // 固定着色不透明度：导航栏这类常驻控件不该跟着「卡片透明度」一起变透
    tintAlphaOverride: Float? = null,
    // 固定着色：导航栏用卡片底色，避免和页面背景色不一致
    tintColorOverride: Color? = null,
    // 模糊半径：磨砂与液态玻璃用不同强度
    blurRadius: Float = 25f,
    // 忽略全局「启用模糊」开关：胶囊底栏的液态玻璃是独立材质，不应被总开关关掉
    ignoreGlobalToggle: Boolean = false,
    // 采样区域形状：顶栏 / 传统底栏是矩形，胶囊底栏要裁成胶囊形
    shape: Shape = RectangleShape,
): Modifier {
    val themeConfig = koinInject<ThemeConfig>()
    val cardConfig = koinInject<CardConfig>()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        (!themeConfig.isEnableBlur && !ignoreGlobalToggle)
    ) {
        return renderBackgroundFallback(
            compensateHorizontalOverscroll = compensateHorizontalOverscroll,
            compensateVerticalOverscroll = compensateVerticalOverscroll,
            useFixedSurfaceBoundsForOverscroll = useFixedSurfaceBoundsForOverscroll,
        )
    }

    val renderState = LocalBackgroundRenderState.current
    val backgroundAnchor = LocalBackgroundBlurAnchor.current
    val stretchOverscrollState = LocalStretchOverscrollCompensationState.current
    var coordinates by remember {
        mutableStateOf<LayoutCoordinates?>(null)
    }
    val panelBounds = remember { PanelLayoutBounds() }

    return LocalBlurState.current?.let { backdrop ->
        // 顶栏 / 传统底栏都经过这里：三档质感必须在这里真的改变模糊半径与膜厚，
        // 否则「模糊磨砂 / 半透明」选了跟没选一样。
        val effect = themeConfig.capsuleEffect
        val material = effect.barMaterial
        val baseTintAlpha = tintAlphaOverride ?: if (cardConfig.isCustomBackgroundEnabled) {
            cardConfig.cardAlpha
        } else {
            0.8f
        }
        val blurTintAlpha = when (effect) {
            // 磨砂要糊成柔光、几乎不透出内容，膜给足厚度。
            CapsuleEffect.FROSTED -> maxOf(baseTintAlpha, 0.92f)
            // 半透明只是一层薄膜，背景轮廓要透得上来。
            CapsuleEffect.TRANSLUCENT -> (baseTintAlpha * material.tintScale)
                .coerceIn(0.28f, 0.55f)
            CapsuleEffect.LIQUID_GLASS -> baseTintAlpha
        }
        // 0 半径在部分 ROM 上会被 RenderEffect 拒绝，半透明档给 1px 近似「不模糊」。
        val effectiveBlurRadius = if (material.blurScale <= 0f) {
            1f
        } else {
            blurRadius * material.blurScale
        }

        // 膜色跟随当前底色的明暗：暗壁纸下顶栏文字是白的，膜必须换成深色，
        // 否则「白字压白膜」，之前修好的文字对比度会再次失效。
        val blendColor =
            (tintColorOverride ?: barSurfaceColor()).copy(alpha = blurTintAlpha)

        this.then(
            Modifier
                .onGloballyPositioned { newCoordinates ->
                    coordinates = newCoordinates.takeIf { it.isAttached }
                    panelBounds.update(newCoordinates)
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        renderState.blurFrameTick
                        stretchOverscrollState?.version
                        panelBounds.bounds

                        textureBlurEffect(
                            blurRadiusX = effectiveBlurRadius,
                            colors = BlurColors(
                                blendColors = listOf(
                                    BlendColorEntry(color = blendColor),
                                ),
                            ),
                        )

                        val boundsInBackground =
                            coordinates?.boundsInBackgroundNow(backgroundAnchor)
                        val stretchCompensation = stretchOverscrollState
                            ?.resolveBitmapCompensation(
                                backgroundCoordinates = backgroundAnchor,
                                compensateHorizontal = compensateHorizontalOverscroll,
                                compensateVertical = compensateVerticalOverscroll,
                                compensationCoordinates = coordinates
                                    .takeIf { useFixedSurfaceBoundsForOverscroll },
                            )

                        if (boundsInBackground != null && stretchCompensation != null) {
                            stretchOverscrollCompensationEffect(
                                boundsInBackground = boundsInBackground,
                                compensation = stretchCompensation,
                            )
                        }
                    },
                ),
        )
    } ?: renderBackgroundFallback(
        compensateHorizontalOverscroll = compensateHorizontalOverscroll,
        compensateVerticalOverscroll = compensateVerticalOverscroll,
        useFixedSurfaceBoundsForOverscroll = useFixedSurfaceBoundsForOverscroll,
    )
}

@Composable
private fun Modifier.renderBackgroundFallback(
    compensateHorizontalOverscroll: Boolean,
    compensateVerticalOverscroll: Boolean,
    useFixedSurfaceBoundsForOverscroll: Boolean,
): Modifier {
    val themeConfig = koinInject<ThemeConfig>()
    val renderState = LocalBackgroundRenderState.current
    var coordinates by remember {
        mutableStateOf<LayoutCoordinates?>(null)
    }
    val panelBounds = remember { PanelLayoutBounds() }

    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer
    val dimColor = backgroundColor.copy(alpha = themeConfig.backgroundDim)
    val backgroundBitmap = renderState.imageBitmap
    val backgroundAnchor = LocalBackgroundBlurAnchor.current
    val pagerPage = LocalPagerPage.current
    val pagerState = if (pagerPage != null) LocalPagerState.current else null
    val layoutDirection = LocalLayoutDirection.current
    val stretchOverscrollState = LocalStretchOverscrollCompensationState.current
    val stretchRenderer = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            StretchBackgroundRenderer()
        } else {
            null
        }
    }

    return this
        .onGloballyPositioned { newCoordinates ->
            coordinates = newCoordinates.takeIf { it.isAttached }
            panelBounds.update(newCoordinates)
        }
        .drawWithContent {
            renderState.blurFrameTick
            stretchOverscrollState?.version
            panelBounds.bounds

            val boundsInBackground = coordinates?.boundsInBackgroundNow(backgroundAnchor)
                ?: coordinates?.localBoundsInWindowNow()
            val viewportSize = backgroundAnchor
                ?.takeIf { it.isAttached && it.size.width > 0 && it.size.height > 0 }
                ?.size
                ?: renderState.blurViewportSize
            val currentPagerState = pagerState?.takeIf { state ->
                pagerPage != null && pagerPage in 0 until state.pageCount
            }
            val hasPagerPage = currentPagerState != null
            val pagerViewportSize = currentPagerState?.layoutInfo?.viewportSize?.takeIf {
                it.width > 0 && it.height > 0
            }
            val pagerViewportWidth = pagerViewportSize?.width ?: viewportSize.width
            val pageOffset = if (currentPagerState != null && pagerPage != null) {
                currentPagerState.getOffsetDistanceInPages(pagerPage)
            } else {
                0f
            }
            val physicalPageOffset = pageOffset * pagerViewportWidth *
                if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
            val offsetBoundsInBackground = boundsInBackground?.let { bounds ->
                if (hasPagerPage) {
                    val leadingNavigationWidth =
                        (viewportSize.width - pagerViewportWidth).coerceAtLeast(0)
                    val pagerViewportLeft = if (layoutDirection == LayoutDirection.Ltr) {
                        leadingNavigationWidth.toFloat()
                    } else {
                        0f
                    }

                    // Equivalent to translating the backdrop painter by -physicalPageOffset:
                    // crop from the page offset and account for a leading landscape navigation rail
                    // so the background remains fixed while the page moves.
                    Rect(
                        left = pagerViewportLeft + physicalPageOffset,
                        top = bounds.top,
                        right = pagerViewportLeft + physicalPageOffset + bounds.width,
                        bottom = bounds.bottom,
                    )
                } else {
                    bounds
                }
            }
            val bitmapBounds = if (backgroundBitmap != null && offsetBoundsInBackground != null) {
                offsetBoundsInBackground.mapToBitmapBounds(
                    bitmap = backgroundBitmap,
                    viewportSize = viewportSize,
                )
            } else {
                null
            }
            // Fixed navigation surfaces are outside the pager's stretch RenderNode. Their fallback
            // draws the original, unstretched bitmap, so applying the published inverse mapping
            // here would introduce movement instead of cancelling it.
            val stretchCompensation = if (useFixedSurfaceBoundsForOverscroll) {
                null
            } else {
                stretchOverscrollState
                    ?.resolveBitmapCompensation(
                        backgroundCoordinates = backgroundAnchor,
                        compensateHorizontal = compensateHorizontalOverscroll,
                        compensateVertical = compensateVerticalOverscroll,
                    )
                    ?.mapToBitmapCoordinates(
                        bitmap = backgroundBitmap,
                        viewportSize = viewportSize,
                    )
            }

            if (backgroundBitmap != null && bitmapBounds != null) {
                if (stretchCompensation != null) {
                    // in fact no need check sdk int. but make lint happy
                    if (stretchRenderer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        stretchRenderer.draw(
                            scope = this,
                            bitmap = backgroundBitmap,
                            boundsInBackground = bitmapBounds,
                            compensation = stretchCompensation,
                        )
                    } else {
                        drawStretchCompensatedBitmap(
                            bitmap = backgroundBitmap,
                            boundsInBackground = bitmapBounds,
                            compensation = stretchCompensation,
                        )
                    }
                } else {
                    drawBitmapIntersection(
                        bitmap = backgroundBitmap,
                        boundsInBackground = bitmapBounds,
                    )
                }
                drawRect(color = dimColor)
            } else {
                drawRect(color = backgroundColor)
            }

            drawContent()
        }
}

private fun Rect.mapToBitmapBounds(
    bitmap: ImageBitmap,
    viewportSize: IntSize,
): Rect? {
    if (
        bitmap.width <= 0 ||
        bitmap.height <= 0 ||
        viewportSize.width <= 0 ||
        viewportSize.height <= 0
    ) {
        return null
    }

    val scale = maxOf(
        viewportSize.width / bitmap.width.toFloat(),
        viewportSize.height / bitmap.height.toFloat(),
    )
    val renderedWidth = bitmap.width * scale
    val renderedHeight = bitmap.height * scale
    val renderedLeft = (viewportSize.width - renderedWidth) / 2f
    val renderedTop = (viewportSize.height - renderedHeight) / 2f

    return Rect(
        left = (left - renderedLeft) / scale,
        top = (top - renderedTop) / scale,
        right = (right - renderedLeft) / scale,
        bottom = (bottom - renderedTop) / scale,
    )
}

private class PanelLayoutBounds {
    var bounds: Rect? by mutableStateOf<Rect?>(null)
        private set

    fun update(coordinates: LayoutCoordinates?) {
        val attached = coordinates?.takeIf { it.isAttached } ?: return
        if (attached.size.width <= 0 || attached.size.height <= 0) return

        val newBounds = attached.localBoundsInWindowNow() ?: return
        if (bounds != newBounds) bounds = newBounds
    }
}

@Composable
fun Modifier.renderBackgroundBlur(
    tintColor: Color? = null,
    // 膜厚：默认跟随卡片透明度；底栏这类导航控件要更实一点，单独给值。
    tintAlpha: Float? = null,
): Modifier {
    if (LocalBackgroundBlurProvided.current) return this

    val themeConfig = koinInject<ThemeConfig>()
    val cardConfig = koinInject<CardConfig>()
    val renderState = LocalBackgroundRenderState.current
    // 没有背景层（既无自定义壁纸，也无背景特效）时，这一层完全没有意义：
    // 它仍会拿上一次缓存的模糊位图 + 一层 tint 画在控件上，结果就是白底上
    // 多出一块方角的白斑（标题栏那颗铃铛按钮上最明显）。
    // 有背景才需要磨砂，没有就直接返回，保持透明干净的实色控件。
    val hasBackdrop = cardConfig.isCustomBackgroundEnabled ||
        themeConfig.backgroundEffect != BackgroundEffect.NONE
    if (!hasBackdrop) return this
    // 自定义壁纸下卡片一定要磨砂，否则半透明卡只是把花纹直接透到字上。
    if (!themeConfig.isEnableBlurExp && !cardConfig.isCustomBackgroundEnabled) return this

    var coordinates by remember {
        mutableStateOf<LayoutCoordinates?>(null)
    }

    val panelBounds = remember { PanelLayoutBounds() }

    val tintColor = (tintColor ?: MaterialTheme.colorScheme.surfaceBright).copy(
        alpha = (tintAlpha ?: cardConfig.cardAlpha).coerceIn(0f, 1f),
    )
    val backgroundBlurAnchor = LocalBackgroundBlurAnchor.current
    val stretchOverscrollState = LocalStretchOverscrollCompensationState.current
    val stretchRenderer = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            StretchBackgroundRenderer()
        } else {
            null
        }
    }

    return this
        .onGloballyPositioned { newCoordinates ->
            coordinates = newCoordinates.takeIf { it.isAttached }
            panelBounds.update(newCoordinates)
        }
        .drawWithContent {
            renderState.blurFrameTick
            stretchOverscrollState?.version
            panelBounds.bounds

            val currentBitmap = renderState.blurImageBitmap
            val currentBoundsInBackground = coordinates?.boundsInBackgroundNow(backgroundBlurAnchor)
            val stretchCompensation = stretchOverscrollState
                ?.resolveBitmapCompensation(backgroundBlurAnchor)

            if (
                currentBitmap != null &&
                currentBoundsInBackground != null &&
                currentBoundsInBackground.width > 0f &&
                currentBoundsInBackground.height > 0f
            ) {
                if (stretchCompensation != null) {
                    // in fact no need check sdk int. but make lint happy
                    if (stretchRenderer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        stretchRenderer.draw(
                            scope = this,
                            bitmap = currentBitmap,
                            boundsInBackground = currentBoundsInBackground,
                            compensation = stretchCompensation,
                        )
                    } else {
                        drawStretchCompensatedBitmap(
                            bitmap = currentBitmap,
                            boundsInBackground = currentBoundsInBackground,
                            compensation = stretchCompensation,
                        )
                    }
                } else {
                    drawBitmapIntersection(
                        bitmap = currentBitmap,
                        boundsInBackground = currentBoundsInBackground,
                    )
                }

                drawRect(
                    color = tintColor,
                    blendMode = BlendMode.SrcOver,
                )
            }

            drawContent()
        }
}

private fun LayoutCoordinates.boundsInBackgroundNow(
    backgroundCoordinates: LayoutCoordinates?,
): Rect? {
    if (!isAttached || size.width <= 0 || size.height <= 0) return null

    return backgroundCoordinates
        ?.takeIf { it.isAttached && it.size.width > 0 && it.size.height > 0 }
        ?.let { boundsInCoordinatesNow(it) }
        ?: localBoundsInWindowNow()
}

private fun LayoutCoordinates.boundsInCoordinatesNow(
    targetCoordinates: LayoutCoordinates,
): Rect? {
    fun localToTarget(point: Offset): Offset {
        val screenPoint = localToScreen(point)
        if (!screenPoint.isUsable()) return Offset.Unspecified

        return targetCoordinates.screenToLocal(screenPoint)
    }

    val width = size.width.toFloat()
    val height = size.height.toFloat()

    return boundsFromCorners(
        topLeft = localToTarget(Offset.Zero),
        topRight = localToTarget(Offset(width, 0f)),
        bottomLeft = localToTarget(Offset(0f, height)),
        bottomRight = localToTarget(Offset(width, height)),
    )
}

private fun LayoutCoordinates.localBoundsInWindowNow(): Rect? {
    val width = size.width.toFloat()
    val height = size.height.toFloat()

    return boundsFromCorners(
        topLeft = localToWindow(Offset.Zero),
        topRight = localToWindow(Offset(width, 0f)),
        bottomLeft = localToWindow(Offset(0f, height)),
        bottomRight = localToWindow(Offset(width, height)),
    )
}

private fun boundsFromCorners(
    topLeft: Offset,
    topRight: Offset,
    bottomLeft: Offset,
    bottomRight: Offset,
): Rect? {
    if (
        !topLeft.isUsable() ||
        !topRight.isUsable() ||
        !bottomLeft.isUsable() ||
        !bottomRight.isUsable()
    ) {
        return null
    }

    return Rect(
        left = minOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x),
        top = minOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y),
        right = maxOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x),
        bottom = maxOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y),
    )
}

private data class StretchBitmapCompensation(
    val horizontalAmount: Float,
    val horizontalOrigin: Float,
    val horizontalSize: Float,
    val verticalAmount: Float,
    val verticalOrigin: Float,
    val verticalSize: Float,
)

private fun StretchBitmapCompensation.mapToBitmapCoordinates(
    bitmap: ImageBitmap?,
    viewportSize: IntSize,
): StretchBitmapCompensation? {
    if (
        bitmap == null ||
        bitmap.width <= 0 ||
        bitmap.height <= 0 ||
        viewportSize.width <= 0 ||
        viewportSize.height <= 0
    ) {
        return null
    }

    val scale = maxOf(
        viewportSize.width / bitmap.width.toFloat(),
        viewportSize.height / bitmap.height.toFloat(),
    )
    val renderedLeft = (viewportSize.width - bitmap.width * scale) / 2f
    val renderedTop = (viewportSize.height - bitmap.height * scale) / 2f

    return StretchBitmapCompensation(
        horizontalAmount = horizontalAmount,
        horizontalOrigin = (horizontalOrigin - renderedLeft) / scale,
        horizontalSize = horizontalSize / scale,
        verticalAmount = verticalAmount,
        verticalOrigin = (verticalOrigin - renderedTop) / scale,
        verticalSize = verticalSize / scale,
    )
}

private fun StretchOverscrollCompensationState.resolveBitmapCompensation(
    backgroundCoordinates: LayoutCoordinates?,
    compensateHorizontal: Boolean = true,
    compensateVertical: Boolean = true,
    compensationCoordinates: LayoutCoordinates? = null,
): StretchBitmapCompensation? {
    val compensationBounds = compensationCoordinates
        ?.boundsInBackgroundNow(backgroundCoordinates)
    val horizontalAxis = horizontal
        ?.takeIf { compensateHorizontal && it.amount != 0f }
    val verticalAxis = vertical
        ?.takeIf { compensateVertical && it.amount != 0f }
    val horizontalBounds = horizontalAxis?.let { axis ->
        compensationBounds
            ?.takeIf { it.width > 0f }
            ?: axis.coordinates
                .boundsInBackgroundNow(backgroundCoordinates)
                ?.takeIf { it.width > 0f }
    }
    val verticalBounds = verticalAxis?.let { axis ->
        compensationBounds
            ?.takeIf { it.height > 0f }
            ?: axis.coordinates
                .boundsInBackgroundNow(backgroundCoordinates)
                ?.takeIf { it.height > 0f }
    }

    if (horizontalBounds == null && verticalBounds == null) return null

    return StretchBitmapCompensation(
        horizontalAmount = if (horizontalBounds != null) horizontalAxis.amount else 0f,
        horizontalOrigin = horizontalBounds?.left ?: 0f,
        horizontalSize = horizontalBounds?.width ?: 1f,
        verticalAmount = if (verticalBounds != null) verticalAxis.amount else 0f,
        verticalOrigin = verticalBounds?.top ?: 0f,
        verticalSize = verticalBounds?.height ?: 1f,
    )
}

private fun BackdropEffectScope.stretchOverscrollCompensationEffect(
    boundsInBackground: Rect,
    compensation: StretchBitmapCompensation,
) {
    val inputScale = downscaleFactor.coerceAtLeast(1).toFloat()
    val inputPadding = padding

    runtimeShaderEffect(
        key = "StretchOverscrollCompensation",
        shaderString = STRETCH_BACKGROUND_SHADER,
        uniformShaderName = "background",
    ) {
        setFloatUniform(
            "cardOrigin",
            boundsInBackground.left - inputPadding,
            boundsInBackground.top - inputPadding,
        )
        setFloatUniform("cardScale", inputScale, inputScale)
        setFloatUniform(
            "inputOrigin",
            (-boundsInBackground.left + inputPadding) / inputScale,
            (-boundsInBackground.top + inputPadding) / inputScale,
        )
        setFloatUniform("inputScale", 1f / inputScale, 1f / inputScale)
        setFloatUniform(
            "stretchAmount",
            compensation.horizontalAmount,
            compensation.verticalAmount,
        )
        setFloatUniform(
            "stretchOrigin",
            compensation.horizontalOrigin,
            compensation.verticalOrigin,
        )
        setFloatUniform(
            "stretchSize",
            compensation.horizontalSize,
            compensation.verticalSize,
        )
    }
}

private data class BitmapDrawSegment(
    val srcStart: Int,
    val srcEnd: Int,
    val dstStart: Int,
    val dstEnd: Int,
)

private fun buildBitmapDrawSegments(
    sourceStart: Float,
    sourceEnd: Float,
    bitmapSize: Int,
    destinationSize: Float,
): List<BitmapDrawSegment> {
    val sourceSpan = sourceEnd - sourceStart
    val destinationEnd = ceil(destinationSize).toInt()

    if (bitmapSize <= 0 || sourceSpan <= 0f || destinationEnd <= 0) {
        return emptyList()
    }

    fun mapToDestination(value: Float): Float = (value - sourceStart) / sourceSpan * destinationSize

    val segments = mutableListOf<BitmapDrawSegment>()

    // Emulate Shader.TileMode.CLAMP for areas that are above / left of the bitmap.
    if (sourceStart < 0f) {
        val dstEnd = ceil(mapToDestination(0f).coerceIn(0f, destinationSize))
            .toInt()
            .coerceIn(0, destinationEnd)

        if (dstEnd > 0) {
            segments += BitmapDrawSegment(
                srcStart = 0,
                srcEnd = 1,
                dstStart = 0,
                dstEnd = dstEnd,
            )
        }
    }

    val clippedStart = sourceStart.coerceIn(0f, bitmapSize.toFloat())
    val clippedEnd = sourceEnd.coerceIn(0f, bitmapSize.toFloat())

    if (clippedEnd > clippedStart) {
        val srcStart = floor(clippedStart).toInt().coerceIn(0, bitmapSize - 1)
        val srcEnd = ceil(clippedEnd).toInt().coerceIn(srcStart + 1, bitmapSize)
        val dstStart = floor(mapToDestination(clippedStart).coerceIn(0f, destinationSize))
            .toInt()
            .coerceIn(0, destinationEnd)
        val dstEnd = ceil(mapToDestination(clippedEnd).coerceIn(0f, destinationSize))
            .toInt()
            .coerceIn(0, destinationEnd)

        if (dstEnd > dstStart) {
            segments += BitmapDrawSegment(
                srcStart = srcStart,
                srcEnd = srcEnd,
                dstStart = dstStart,
                dstEnd = dstEnd,
            )
        }
    }

    // Emulate Shader.TileMode.CLAMP for areas that are below / right of the bitmap.
    if (sourceEnd > bitmapSize.toFloat()) {
        val dstStart = floor(mapToDestination(bitmapSize.toFloat()).coerceIn(0f, destinationSize))
            .toInt()
            .coerceIn(0, destinationEnd)

        if (destinationEnd > dstStart) {
            segments += BitmapDrawSegment(
                srcStart = bitmapSize - 1,
                srcEnd = bitmapSize,
                dstStart = dstStart,
                dstEnd = destinationEnd,
            )
        }
    }

    // The whole destination is outside one side of the source bitmap.
    if (segments.isEmpty()) {
        val src = if (sourceEnd <= 0f) 0 else bitmapSize - 1
        segments += BitmapDrawSegment(
            srcStart = src,
            srcEnd = src + 1,
            dstStart = 0,
            dstEnd = destinationEnd,
        )
    }

    return segments
}

private fun ContentDrawScope.drawBitmapIntersection(
    bitmap: ImageBitmap,
    boundsInBackground: Rect,
) {
    val bitmapWidth = bitmap.width
    val bitmapHeight = bitmap.height

    if (
        bitmapWidth <= 0 ||
        bitmapHeight <= 0 ||
        size.width <= 0f ||
        size.height <= 0f ||
        boundsInBackground.width <= 0f ||
        boundsInBackground.height <= 0f
    ) {
        return
    }

    val xSegments = buildBitmapDrawSegments(
        sourceStart = boundsInBackground.left,
        sourceEnd = boundsInBackground.right,
        bitmapSize = bitmapWidth,
        destinationSize = size.width,
    )
    val ySegments = buildBitmapDrawSegments(
        sourceStart = boundsInBackground.top,
        sourceEnd = boundsInBackground.bottom,
        bitmapSize = bitmapHeight,
        destinationSize = size.height,
    )

    for (xSegment in xSegments) {
        for (ySegment in ySegments) {
            val srcWidth = xSegment.srcEnd - xSegment.srcStart
            val srcHeight = ySegment.srcEnd - ySegment.srcStart
            val dstWidth = xSegment.dstEnd - xSegment.dstStart
            val dstHeight = ySegment.dstEnd - ySegment.dstStart

            if (srcWidth <= 0 || srcHeight <= 0 || dstWidth <= 0 || dstHeight <= 0) {
                continue
            }

            drawImage(
                image = bitmap,
                srcOffset = IntOffset(xSegment.srcStart, ySegment.srcStart),
                srcSize = IntSize(srcWidth, srcHeight),
                dstOffset = IntOffset(xSegment.dstStart, ySegment.dstStart),
                dstSize = IntSize(dstWidth, dstHeight),
                blendMode = BlendMode.SrcOver,
            )
        }
    }
}

private const val STRETCH_BACKGROUND_SHADER = """
    uniform shader background;
    uniform float2 cardOrigin;
    uniform float2 cardScale;
    uniform float2 inputOrigin;
    uniform float2 inputScale;
    uniform float2 stretchAmount;
    uniform float2 stretchOrigin;
    uniform float2 stretchSize;

    float reverseMapStart(float overscroll, float sourcePosition) {
        float numerator =
            (-sourcePosition * overscroll * overscroll) -
            (2.0 * sourcePosition * overscroll) -
            sourcePosition;
        float denominator =
            1.0 +
            (0.3 * overscroll) +
            (0.7 * sourcePosition * overscroll * overscroll) +
            (0.7 * sourcePosition * overscroll);
        return -(numerator / denominator);
    }

    float reverseMapEnd(float overscroll, float sourcePosition) {
        float numerator =
            (0.3 * overscroll * overscroll) -
            (0.3 * sourcePosition * overscroll * overscroll) +
            (1.3 * sourcePosition * overscroll) -
            overscroll -
            sourcePosition;
        float denominator =
            (0.7 * sourcePosition * overscroll * overscroll) -
            (0.7 * sourcePosition * overscroll) -
            (0.7 * overscroll * overscroll) +
            overscroll -
            1.0;
        return numerator / denominator;
    }

    float reverseStretch(float overscroll, float sourcePosition) {
        float distanceStretched = 1.0 / (1.0 + abs(overscroll));
        float distanceDiff = distanceStretched - 1.0;

        if (overscroll > 0.0) {
            float mappedPosition = reverseMapStart(overscroll, sourcePosition);
            if (mappedPosition <= 1.0) {
                return mappedPosition;
            } else if (mappedPosition >= distanceStretched) {
                return mappedPosition - distanceDiff;
            }
        }

        if (overscroll < 0.0) {
            float mappedPosition = reverseMapEnd(overscroll, sourcePosition);
            if (mappedPosition >= 0.0) {
                return mappedPosition;
            } else {
                return mappedPosition + distanceDiff;
            }
        }

        return sourcePosition;
    }

    float compensateAxis(float position, float amount, float origin, float extent) {
        if (amount == 0.0 || extent <= 0.0) {
            return position;
        }
        float normalized = (position - origin) / extent;
        return origin + reverseStretch(amount, normalized) * extent;
    }

    half4 main(float2 coord) {
        float2 position = cardOrigin + coord * cardScale;
        float2 samplePosition = float2(
            compensateAxis(position.x, stretchAmount.x, stretchOrigin.x, stretchSize.x),
            compensateAxis(position.y, stretchAmount.y, stretchOrigin.y, stretchSize.y)
        );
        return background.eval(inputOrigin + samplePosition * inputScale);
    }
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class StretchBackgroundRenderer {
    private val runtimeShader = RuntimeShader(STRETCH_BACKGROUND_SHADER)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        shader = runtimeShader
    }
    private var inputBitmap: Bitmap? = null

    fun draw(
        scope: ContentDrawScope,
        bitmap: ImageBitmap,
        boundsInBackground: Rect,
        compensation: StretchBitmapCompensation,
    ) {
        val androidBitmap = bitmap.asAndroidBitmap()
        if (inputBitmap !== androidBitmap) {
            inputBitmap = androidBitmap
            runtimeShader.setInputShader(
                "background",
                BitmapShader(androidBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP),
            )
        }

        with(scope) {
            runtimeShader.setFloatUniform(
                "cardOrigin",
                boundsInBackground.left,
                boundsInBackground.top,
            )
            runtimeShader.setFloatUniform(
                "cardScale",
                boundsInBackground.width / size.width,
                boundsInBackground.height / size.height,
            )
            runtimeShader.setFloatUniform("inputOrigin", 0f, 0f)
            runtimeShader.setFloatUniform("inputScale", 1f, 1f)
            runtimeShader.setFloatUniform(
                "stretchAmount",
                compensation.horizontalAmount,
                compensation.verticalAmount,
            )
            runtimeShader.setFloatUniform(
                "stretchOrigin",
                compensation.horizontalOrigin,
                compensation.verticalOrigin,
            )
            runtimeShader.setFloatUniform(
                "stretchSize",
                compensation.horizontalSize,
                compensation.verticalSize,
            )
            drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
        }
    }
}

private val stretchFallbackPaint =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

private data class BitmapSourceInterval(
    val start: Int,
    val end: Int,
)

private fun bitmapSourceInterval(
    first: Float,
    second: Float,
    bitmapSize: Int,
): BitmapSourceInterval? {
    if (bitmapSize <= 0) return null

    val lower = minOf(first, second)
    val upper = maxOf(first, second)
    if (upper <= 0f) return BitmapSourceInterval(0, 1)
    if (lower >= bitmapSize) return BitmapSourceInterval(bitmapSize - 1, bitmapSize)

    val start = floor(lower.coerceAtLeast(0f)).toInt().coerceIn(0, bitmapSize - 1)
    val end = ceil(upper.coerceAtMost(bitmapSize.toFloat()))
        .toInt()
        .coerceIn(start + 1, bitmapSize)
    return BitmapSourceInterval(start, end)
}

private fun reverseStretchPosition(
    amount: Float,
    normalizedInput: Float,
): Float {
    if (amount == 0f) return normalizedInput

    val distanceStretched = 1f / (1f + abs(amount))
    val distanceDifference = distanceStretched - 1f

    if (amount > 0f) {
        val numerator =
            (-normalizedInput * amount * amount) -
                (2f * normalizedInput * amount) -
                normalizedInput
        val denominator =
            1f +
                (0.3f * amount) +
                (0.7f * normalizedInput * amount * amount) +
                (0.7f * normalizedInput * amount)
        val output = -(numerator / denominator)
        return if (output <= 1f) output else output - distanceDifference
    }

    val numerator =
        (0.3f * amount * amount) -
            (0.3f * normalizedInput * amount * amount) +
            (1.3f * normalizedInput * amount) -
            amount -
            normalizedInput
    val denominator =
        (0.7f * normalizedInput * amount * amount) -
            (0.7f * normalizedInput * amount) -
            (0.7f * amount * amount) +
            amount -
            1f
    val output = numerator / denominator
    return if (output >= 0f) output else output + distanceDifference
}

private fun compensateBackgroundPosition(
    position: Float,
    amount: Float,
    origin: Float,
    extent: Float,
): Float {
    if (amount == 0f || extent <= 0f) return position
    val normalized = (position - origin) / extent
    return origin + reverseStretchPosition(amount, normalized) * extent
}

private fun ContentDrawScope.drawStretchCompensatedBitmap(
    bitmap: ImageBitmap,
    boundsInBackground: Rect,
    compensation: StretchBitmapCompensation,
) {
    if (size.width <= 0f || size.height <= 0f) return

    // Android 12/12L have no RuntimeShader API. A fine mesh is visually continuous for the
    // already-blurred bitmap while preserving the same inverse mapping.
    val xCellCount = if (compensation.horizontalAmount == 0f) {
        1
    } else {
        ceil(size.width / 16f).toInt().coerceAtLeast(1)
    }
    val yCellCount = if (compensation.verticalAmount == 0f) {
        1
    } else {
        ceil(size.height / 16f).toInt().coerceAtLeast(1)
    }
    val androidBitmap = bitmap.asAndroidBitmap()
    val canvas = drawContext.canvas.nativeCanvas

    for (xIndex in 0 until xCellCount) {
        val destinationLeft = size.width * xIndex / xCellCount
        val destinationRight = size.width * (xIndex + 1) / xCellCount
        val backgroundLeft =
            boundsInBackground.left + boundsInBackground.width * destinationLeft / size.width
        val backgroundRight =
            boundsInBackground.left + boundsInBackground.width * destinationRight / size.width
        val sourceX = bitmapSourceInterval(
            first = compensateBackgroundPosition(
                position = backgroundLeft,
                amount = compensation.horizontalAmount,
                origin = compensation.horizontalOrigin,
                extent = compensation.horizontalSize,
            ),
            second = compensateBackgroundPosition(
                position = backgroundRight,
                amount = compensation.horizontalAmount,
                origin = compensation.horizontalOrigin,
                extent = compensation.horizontalSize,
            ),
            bitmapSize = bitmap.width,
        ) ?: continue

        for (yIndex in 0 until yCellCount) {
            val destinationTop = size.height * yIndex / yCellCount
            val destinationBottom = size.height * (yIndex + 1) / yCellCount
            val backgroundTop =
                boundsInBackground.top + boundsInBackground.height * destinationTop / size.height
            val backgroundBottom =
                boundsInBackground.top + boundsInBackground.height * destinationBottom / size.height
            val sourceY = bitmapSourceInterval(
                first = compensateBackgroundPosition(
                    position = backgroundTop,
                    amount = compensation.verticalAmount,
                    origin = compensation.verticalOrigin,
                    extent = compensation.verticalSize,
                ),
                second = compensateBackgroundPosition(
                    position = backgroundBottom,
                    amount = compensation.verticalAmount,
                    origin = compensation.verticalOrigin,
                    extent = compensation.verticalSize,
                ),
                bitmapSize = bitmap.height,
            ) ?: continue

            canvas.drawBitmap(
                androidBitmap,
                android.graphics.Rect(
                    sourceX.start,
                    sourceY.start,
                    sourceX.end,
                    sourceY.end,
                ),
                RectF(
                    destinationLeft,
                    destinationTop,
                    destinationRight,
                    destinationBottom,
                ),
                stretchFallbackPaint,
            )
        }
    }
}

private fun Offset.isUsable(): Boolean = x.isFinite() && y.isFinite()

private suspend fun Bitmap.extractSeedColor(
    maxColors: Int = 128,
    fallbackColorArgb: Int = -12417548,
): Int = withContext(Dispatchers.IO) {
    val scaledBitmap = this@extractSeedColor.scale(128, 128)

    val width = scaledBitmap.width
    val height = scaledBitmap.height
    val pixels = IntArray(width * height)
    scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val colorToCountMap: Map<Int, Int> = QuantizerCelebi.quantize(pixels, maxColors)
    val sortedColors: List<Int> = Score.score(colorToCountMap, 10, fallbackColorArgb, true)

    if (scaledBitmap != this@extractSeedColor) {
        scaledBitmap.recycle()
    }

    sortedColors.firstOrNull() ?: fallbackColorArgb
}

private fun Bitmap.softwareFastBlur(radius: Int): Bitmap {
    if (radius < 1) return this

    val w = width
    val h = height
    val pix = IntArray(w * h)
    getPixels(pix, 0, w, 0, 0, w, h)

    val wm = w - 1
    val hm = h - 1
    val wh = w * h
    val div = radius + radius + 1

    val r = IntArray(wh)
    val g = IntArray(wh)
    val b = IntArray(wh)
    var rsum: Int
    var gsum: Int
    var bsum: Int
    var p: Int
    var yp: Int
    var yi: Int
    val vmin = IntArray(w.coerceAtLeast(h))

    var divsum = (div + 1) shr 1
    divsum *= divsum
    val dv = IntArray(256 * divsum)
    for (i in 0 until 256 * divsum) {
        dv[i] = i / divsum
    }

    var yw = 0
    yi = 0

    val stack = Array(div) { IntArray(3) }
    var stackpointer: Int
    var stackstart: Int
    var sir: IntArray
    var rbs: Int
    val r1 = radius + 1
    var routsum: Int
    var goutsum: Int
    var boutsum: Int
    var rinsum: Int
    var ginsum: Int
    var binsum: Int

    for (y in 0 until h) {
        bsum = 0
        gsum = 0
        rsum = 0
        boutsum = 0
        goutsum = 0
        routsum = 0
        binsum = 0
        ginsum = 0
        rinsum = 0
        for (i in -radius..radius) {
            p = pix[yi + wm.coerceAtMost(i.coerceAtLeast(0))]
            sir = stack[i + radius]
            sir[0] = (p and 0xff0000) shr 16
            sir[1] = (p and 0x00ff00) shr 8
            sir[2] = p and 0x0000ff
            rbs = r1 - abs(i)
            rsum += sir[0] * rbs
            gsum += sir[1] * rbs
            bsum += sir[2] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
        }
        stackpointer = radius

        for (x in 0 until w) {
            r[yi] = dv[rsum]
            g[yi] = dv[gsum]
            b[yi] = dv[bsum]

            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum

            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]

            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]

            if (y == 0) vmin[x] = (x + radius + 1).coerceAtMost(wm)
            p = pix[yw + vmin[x]]

            sir[0] = (p and 0xff0000) shr 16
            sir[1] = (p and 0x00ff00) shr 8
            sir[2] = p and 0x0000ff

            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]

            rsum += rinsum
            gsum += ginsum
            bsum += binsum

            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer % div]

            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]

            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]

            yi++
        }
        yw += w
    }

    for (x in 0 until w) {
        bsum = 0
        gsum = 0
        rsum = 0
        boutsum = 0
        goutsum = 0
        routsum = 0
        binsum = 0
        ginsum = 0
        rinsum = 0
        yp = -radius * w
        for (i in -radius..radius) {
            yi = (yp.coerceAtLeast(0)) + x
            sir = stack[i + radius]
            sir[0] = r[yi]
            sir[1] = g[yi]
            sir[2] = b[yi]
            rbs = r1 - abs(i)
            rsum += r[yi] * rbs
            gsum += g[yi] * rbs
            bsum += b[yi] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            if (i < hm) yp += w
        }
        yi = x
        stackpointer = radius
        for (y in 0 until h) {
            pix[yi] = (-0x1000000 and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]
            if (x == 0) vmin[y] = (y + r1).coerceAtMost(hm) * w
            p = x + vmin[y]
            sir[0] = r[p]
            sir[1] = g[p]
            sir[2] = b[p]
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer]
            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]
            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]
            yi += w
        }
    }

    val outputBitmap = createBitmap(w, h)
    outputBitmap.setPixels(pix, 0, w, 0, 0, w, h)
    return outputBitmap
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Bitmap.blurBitmap(blurRadius: Float): Bitmap {
    val outputBitmap = createBitmap(width, height)
    val outputCanvas = Canvas(outputBitmap)

    if (outputCanvas.isHardwareAccelerated) {
        val renderNode = RenderNode("BlurEffectNode").apply {
            setPosition(0, 0, width, height)
            setRenderEffect(
                RenderEffect.createBlurEffect(
                    blurRadius,
                    blurRadius,
                    Shader.TileMode.CLAMP,
                ),
            )
        }

        val recordingCanvas = renderNode.beginRecording()
        recordingCanvas.drawBitmap(this, 0f, 0f, null)
        renderNode.endRecording()

        outputCanvas.drawRenderNode(renderNode)
    } else {
        val radiusInt = blurRadius.toInt().coerceIn(1, 25)
        return this.softwareFastBlur(radiusInt)
    }

    return outputBitmap
}

@RequiresApi(Build.VERSION_CODES.S)
private suspend fun Bitmap.createBackgroundBlurImage(
    context: Context,
    viewportSize: IntSize,
    blurRadius: Float,
): ImageBitmap = withContext(Dispatchers.Default) {
    val cacheFile = backgroundBlurCacheFile(context)

    BitmapFactory.decodeFile(cacheFile.absolutePath)?.let { cachedBitmap ->
        if (
            cachedBitmap.width == viewportSize.width &&
            cachedBitmap.height == viewportSize.height
        ) {
            return@withContext cachedBitmap.asImageBitmap()
        }

        cachedBitmap.recycle()
    }

    val blurSource = createBackgroundBlurSource(viewportSize)

    val blurredBitmap = try {
        blurSource.blurBitmap(blurRadius)
    } finally {
        if (blurSource !== this@createBackgroundBlurImage) {
            blurSource.recycle()
        }
    }

    saveBackgroundBlurCache(cacheFile, blurredBitmap)
    blurredBitmap.asImageBitmap()
}

private fun Bitmap.createBackgroundBlurSource(viewportSize: IntSize): Bitmap {
    val targetWidth = viewportSize.width
    val targetHeight = viewportSize.height

    if (
        targetWidth <= 0 ||
        targetHeight <= 0 ||
        (width == targetWidth && height == targetHeight)
    ) {
        return this
    }

    val scale = maxOf(
        targetWidth / width.toFloat(),
        targetHeight / height.toFloat(),
    )
    val scaledWidth = width * scale
    val scaledHeight = height * scale
    val left = (targetWidth - scaledWidth) / 2f
    val top = (targetHeight - scaledHeight) / 2f

    return createBitmap(targetWidth, targetHeight).also { outputBitmap ->
        Canvas(outputBitmap).drawBitmap(
            this,
            null,
            RectF(left, top, left + scaledWidth, top + scaledHeight),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
    }
}

private fun backgroundBlurCacheFile(context: Context): File = File(context.filesDir, "blured_custom_background.jpg")

private fun legacyBackgroundBlurCacheDir(context: Context): File = File(context.filesDir, "background_blur_cache")

private fun saveBackgroundBlurCache(cacheFile: File, bitmap: Bitmap) {
    runCatching {
        cacheFile.parentFile?.mkdirs()
        val tempFile = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
        FileOutputStream(tempFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        }
        if (!tempFile.renameTo(cacheFile)) {
            tempFile.copyTo(cacheFile, overwrite = true)
            tempFile.delete()
        }
    }.onFailure {
        Log.w("ThemeSystem", "Failed to save background blur cache: ${it.message}")
    }
}

@Composable
private fun BackgroundInitializer(
    uri: Uri,
    themeConfig: ThemeConfig,
    settings: AppSettingsRepository,
    renderState: BackgroundRenderState,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cardConfig: CardConfig = koinInject()

    val dynamicColorFromSystem =
        if (Build.VERSION.SDK_INT >= 31) {
            colorResource(id = android.R.color.system_accent1_500).toArgb()
        } else {
            themeConfig.monetCompatSeedColor
        }

    val calcedCachedSeedColor =
        settings.getInt("cached_seed_color", dynamicColorFromSystem)

    LaunchedEffect(themeConfig.isEnableBlurExp, renderState.blurViewportSize) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            (themeConfig.isEnableBlurExp || cardConfig.isCustomBackgroundEnabled) &&
            renderState.blurViewportSize.width > 0 &&
            renderState.blurViewportSize.height > 0
        ) {
            renderState.imagePainter?.let {
                if (it.state !is AsyncImagePainter.State.Success) return@let

                val bitmap = (it.state as AsyncImagePainter.State.Success).result.drawable.toBitmap()
                renderState.blurImageBitmap = bitmap.createBackgroundBlurImage(
                    context = context,
                    viewportSize = renderState.blurViewportSize,
                    blurRadius = BACKGROUND_BLUR_RADIUS,
                )
            }
        } else {
            renderState.blurImageBitmap = null
        }
    }

    renderState.imagePainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false)
            .crossfade(true)
            .build(),
        onError = { error ->
            Log.e("ThemeSystem", "背景加载失败: ${error.result.throwable.message}")
            renderState.imageBitmap = null
            renderState.wallpaperReady = false
            themeConfig.customBackgroundUri = null
        },
        onSuccess = {
            val bitmap = it.result.drawable.toBitmap()
            renderState.imageBitmap = bitmap.asImageBitmap()
            // 壁纸整体亮度：决定画在壁纸上的标题该用深字还是浅字。
            renderState.wallpaperLuminance = bitmap.averageLuminance()
            renderState.wallpaperReady = true
            Log.d("ThemeSystem", "背景加载成功")
            themeConfig.backgroundImageLoaded = true
            themeConfig.isThemeChanging = false

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (themeConfig.isEnableBlurExp || cardConfig.isCustomBackgroundEnabled) &&
                renderState.blurViewportSize.width > 0 &&
                renderState.blurViewportSize.height > 0
            ) {
                coroutineScope.launch {
                    renderState.blurImageBitmap = bitmap.createBackgroundBlurImage(
                        context = context,
                        viewportSize = renderState.blurViewportSize,
                        blurRadius = BACKGROUND_BLUR_RADIUS,
                    )
                }
            } else {
                renderState.blurImageBitmap = null
            }

            renderState.seedColor = calcedCachedSeedColor
            coroutineScope.launch {
                renderState.seedColor = bitmap.extractSeedColor(
                    fallbackColorArgb = calcedCachedSeedColor,
                )

                settings.putInt("cached_seed_color", renderState.seedColor)
            }
        },
    )
}

@Composable
private fun generateTypography(
    themeConfig: ThemeConfig,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
): androidx.compose.material3.Typography {
    val darkMode = isInDarkTheme(themeConfig.forceDarkMode)

    fun generateShadow(originalShadow: Shadow?): Shadow? {
        if (!themeConfig.isHighContrastMode) return originalShadow
        val shadow = originalShadow ?: Shadow(
            offset = Offset(1.5f, 1.5f),
            blurRadius = 0f,
        )
        return shadow.copy(
            color = if (darkMode) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f),
        )
    }

    fun TextStyle.applyShadow() = this.copy(
        shadow = generateShadow(shadow),
        fontFamily = fontFamily,
    )
    val typography = MaterialTheme.typography

    return typography.copy(
        displayLarge = typography.displayLarge.applyShadow(),
        displayMedium = typography.displayMedium.applyShadow(),
        displaySmall = typography.displaySmall.applyShadow(),
        headlineLarge = typography.headlineLarge.applyShadow(),
        headlineMedium = typography.headlineMedium.applyShadow(),
        headlineSmall = typography.headlineSmall.applyShadow(),
        titleLarge = typography.titleLarge.applyShadow(),
        titleMedium = typography.titleMedium.applyShadow(),
        titleSmall = typography.titleSmall.applyShadow(),
        bodyLarge = typography.bodyLarge.applyShadow(),
        bodyMedium = typography.bodyMedium.applyShadow(),
        bodySmall = typography.bodySmall.applyShadow(),
        labelLarge = typography.labelLarge.applyShadow(),
        labelMedium = typography.labelMedium.applyShadow(),
        labelSmall = typography.labelSmall.applyShadow(),
        displayLargeEmphasized = typography.displayLargeEmphasized.applyShadow(),
        displayMediumEmphasized = typography.displayMediumEmphasized.applyShadow(),
        displaySmallEmphasized = typography.displaySmallEmphasized.applyShadow(),
        headlineLargeEmphasized = typography.headlineLargeEmphasized.applyShadow(),
        headlineMediumEmphasized = typography.headlineMediumEmphasized.applyShadow(),
        headlineSmallEmphasized = typography.headlineSmallEmphasized.applyShadow(),
        titleLargeEmphasized = typography.titleLargeEmphasized.applyShadow(),
        titleMediumEmphasized = typography.titleMediumEmphasized.applyShadow(),
        titleSmallEmphasized = typography.titleSmallEmphasized.applyShadow(),
        bodyLargeEmphasized = typography.bodyLargeEmphasized.applyShadow(),
        bodyMediumEmphasized = typography.bodyMediumEmphasized.applyShadow(),
        bodySmallEmphasized = typography.bodySmallEmphasized.applyShadow(),
        labelLargeEmphasized = typography.labelLargeEmphasized.applyShadow(),
        labelMediumEmphasized = typography.labelMediumEmphasized.applyShadow(),
        labelSmallEmphasized = typography.labelSmallEmphasized.applyShadow(),
    )
}

@Composable
private fun createColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    themeConfig: ThemeConfig,
    renderState: BackgroundRenderState,
): ColorScheme {
    // KuaiMaoSU：默认使用 XDesign 固定色板，保证与设计稿一致；
    // 仅当用户显式开启「动态取色 / 背景取色」时才走 MaterialKolor 生成。
    if (!dynamicColor) {
        val base = if (darkTheme) KuaiMaoDarkColorScheme else KuaiMaoLightColorScheme
        // 用户自选主题色：只替换主色/辅助色一组，保留设计稿的中性底色。
        if (!themeConfig.useCustomThemeColor) return base
        return base.withSeedAccents(Color(themeConfig.seedColor), darkTheme)
    }

    val seedColor =
        when {
            themeConfig.isUseBackgroundSeedColor && renderState.seedColor != 0 -> {
                renderState.seedColor
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                colorResource(id = android.R.color.system_accent1_500).toArgb()
            }

            else -> {
                themeConfig.monetCompatSeedColor
            }
        }

    return dynamicColorScheme(
        seedColor = Color(seedColor),
        isDark = darkTheme,
        style = themeConfig.dynamicPaletteStyle,
        specVersion = themeConfig.dynamicColorSpec,
    )
}

/**
 * 用用户自选的种子色生成一组强调色，再套回 KuaiMao 固定色板。
 * 只覆盖 primary / secondary / tertiary 三组，surface 与文字层级保持设计稿不变，
 * 这样页面底、卡片、正文都不会被染成一片彩色。
 */
private fun ColorScheme.withSeedAccents(seed: Color, dark: Boolean): ColorScheme {
    // 直接用用户选的那颗颜色，不再经 MaterialKolor 的色调映射：
    // 之前选了 #475C91，生成出来会偏成另一个色相（偏紫/偏灰），
    // 色板里看到的是一个色、装到界面上是另一个色。
    val accent = seed.visibleOn(dark)
    val onAccent = if (accent.luminance() < 0.55f) Color.White else Color(0xFF101318)
    // 容器色只做明度上的加减，色相保持不变，深浅模式看到的是同一个颜色。
    val container = if (dark) {
        lerp(accent, Color(0xFF121214), 0.78f)
    } else {
        lerp(accent, Color.White, 0.86f)
    }
    val onContainer = if (dark) Color(0xFFF2F2F7) else accent
    return copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        inversePrimary = accent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = container,
        onSecondaryContainer = onContainer,
        tertiary = accent,
        onTertiary = onAccent,
        tertiaryContainer = container,
        onTertiaryContainer = onContainer,
    )
}

/** 深色画布上万一压到几乎看不见（亮度 < 0.05），只抬明度、不动色相。 */
private fun Color.visibleOn(dark: Boolean): Color =
    if (dark && luminance() < 0.05f) lerp(this, Color.White, 0.18f) else this

/**
 * KuaiMao 语义色板：把 brand 一族指向自选主题色的 primary，
 * 让选中态图标、按钮、链接、渐变这些「品牌色元素」一起跟着变色。
 */
private fun kuaimaoPaletteFor(themeConfig: ThemeConfig, dark: Boolean): KuaiMaoPalette {
    val base = if (dark) KuaiMaoDarkPalette else KuaiMaoLightPalette
    if (!themeConfig.useCustomThemeColor) return base
    // 同样不做色调映射：亮色、夜间都是同一个色值，只在「色板软底」上按明暗调整。
    val accent = Color(themeConfig.seedColor).visibleOn(dark)
    val soft = if (dark) {
        lerp(accent, Color(0xFF121214), 0.78f)
    } else {
        lerp(accent, Color.White, 0.88f)
    }
    return base.copy(
        brand = accent,
        brandSoft = soft,
        brandDeep = accent,
    )
}

@Composable
private fun SystemBarController(darkMode: Boolean) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb(),
            ) { darkMode },
            navigationBarStyle = if (darkMode) {
                SystemBarStyle.dark(Color.Transparent.toArgb())
            } else {
                SystemBarStyle.light(
                    Color.Transparent.toArgb(),
                    Color.Transparent.toArgb(),
                )
            },
        )
    }
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(themeMode: Boolean?): Boolean = when (themeMode) {
    // 强制深色
    true -> true

    // 强制浅色
    false -> false

    null -> isSystemInDarkTheme() // 跟随系统
}


/**
 * 壁纸平均亮度（0..1）。
 *
 * 缩到 24×24 再取加权平均就够了：这里只是用来判断「这张壁纸够不够暗」，
 * 不需要精确，也不能在主线程上遍历原图。
 */
private fun Bitmap.averageLuminance(): Float {
    if (width <= 0 || height <= 0) return 1f
    val side = 24
    val scaled = runCatching { Bitmap.createScaledBitmap(this, side, side, true) }.getOrNull()
        ?: return 1f
    val pixels = IntArray(side * side)
    scaled.getPixels(pixels, 0, side, 0, 0, side, side)
    var sum = 0f
    pixels.forEach { c ->
        val r = ((c shr 16) and 0xFF) / 255f
        val g = ((c shr 8) and 0xFF) / 255f
        val b = (c and 0xFF) / 255f
        sum += 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
    if (scaled !== this) scaled.recycle()
    return (sum / (side * side)).coerceIn(0f, 1f)
}
