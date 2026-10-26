package me.weishu.kernelsu.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.component.glassNeedsLightText
import me.weishu.kernelsu.ui.webui.MonetColorsProvider

/**
 * "ColorUI" style.
 *
 * A card-first layout (soft tinted canvas, white rounded cards, one accent color).
 * Unlike the original fixed green palette, the scheme follows the same customization as
 * the built-in styles: key color, palette style and color spec from the theme settings,
 * falling back to the system dynamic color when no key color is chosen.
 */
object ColorUiTokens {
    val Warning = Color(0xFFFFA726)
    val Danger = Color(0xFFE5484D)
}

internal val FreshShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun FreshKernelSUTheme(
    appSettings: AppSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = appSettings.colorMode.isDark || (appSettings.colorMode.isSystem && systemDarkTheme)
    // ColorUI keeps the original green as its identity, but still follows the key color
    // picked in 设置 → 主题 (or the system color on ROMs without one).
    val seedColor = if (appSettings.keyColor == 0) Color(0xFF00B00E) else Color(appSettings.keyColor)

    val baseScheme = rememberKernelSUColorScheme(
        seedColor = seedColor,
        isDark = darkTheme,
        isAmoled = appSettings.colorMode.isAmoled,
        paletteStyle = appSettings.paletteStyle,
        colorSpec = appSettings.colorSpec,
    )

    // 浅色：画布改成纯白，卡片用极浅的雾白 + 细描边来区分层次
    // 夜间：强调色保留，中性色切到安静的灰阶。
    val colorScheme = if (!darkTheme) {
        baseScheme.copy(
            surface = Color.White,
            background = Color.White,
            surfaceBright = Color.White,
            surfaceDim = Color(0xFFEDEFEC),
            surfaceContainerLowest = Color(0xFFF7F9F7),
            surfaceContainerLow = Color(0xFFF4F6F3),
            surfaceContainer = Color(0xFFF0F3EF),
            surfaceContainerHigh = Color(0xFFEBEFEA),
            surfaceContainerHighest = Color(0xFFE6EAE4),
            onSurface = Color(0xFF1A1D1A),
            onSurfaceVariant = Color(0xFF606660),
            outlineVariant = Color(0xFFE4E8E2),
        )
    } else {
        // Pitch-black canvas + #1E1E1E cards: the same look on every screen, with pure white
        // titles and a mid-grey secondary so nothing blends into the background.
        baseScheme.copy(
            surface = Color.Black,
            surfaceDim = Color.Black,
            background = Color.Black,
            surfaceContainerLowest = Color(0xFF0A0A0A),
            surfaceContainerLow = Color(0xFF141414),
            surfaceContainer = Color(0xFF181818),
            surfaceContainerHigh = Color(0xFF1E1E1E),
            surfaceContainerHighest = Color(0xFF262626),
            onSurface = Color(0xFFF5F5F5),
            onSurfaceVariant = Color(0xFFA6A6A6),
            onBackground = Color(0xFFF5F5F5),
            outline = Color(0xFF8A8A8A),
            outlineVariant = Color(0xFF333333),
            inverseSurface = Color(0xFFEDEDED),
            inverseOnSurface = Color(0xFF1B1B1B),
        )
    }

    val animatedColorScheme = colorScheme.animateAsState()

    // 玻璃卡面 + 深色壁纸：整体文字转白，避免深色字贴在照片上看不清
    val glassLightText = glassNeedsLightText()
    val textScheme = if (glassLightText) {
        animatedColorScheme.copy(
            onSurface = Color.White,
            onSurfaceVariant = Color.White.copy(alpha = 0.76f),
            surfaceVariant = Color.White.copy(alpha = 0.12f),
            outlineVariant = Color.White.copy(alpha = 0.26f),
            outline = Color.White.copy(alpha = 0.5f),
        )
    } else {
        animatedColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = textScheme,
        motionScheme = MotionScheme.expressive(),
        typography = typographyWithFont(CustomizationStore.current.fontStyle),
        shapes = FreshShapes,
        content = {
            MonetColorsProvider.UpdateCss(animatedColorScheme)
            content()
        }
    )
}
