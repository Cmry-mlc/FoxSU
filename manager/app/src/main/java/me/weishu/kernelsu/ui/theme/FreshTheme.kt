package me.weishu.kernelsu.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import me.weishu.kernelsu.ui.webui.MonetColorsProvider

/**
 * "Fresh" UI style.
 *
 * A light, card-based layout inspired by system cleaner apps: soft green-grey canvas,
 * white rounded cards and a single vivid green accent. The palette is intentionally
 * fixed (it does not follow the user key color) so the style stays recognisable.
 */
object FreshPalette {
    val Green = Color(0xFF00B00E)
    val GreenBright = Color(0xFF2FD63C)
    val GreenDeep = Color(0xFF00910C)

    val Amber = Color(0xFFFFA726)
    val Red = Color(0xFFE5484D)

    val CardLight = Color(0xFFFFFFFF)
    val CanvasLight = Color(0xFFF3F8F1)
    val CardDark = Color(0xFF1A201A)
    val CanvasDark = Color(0xFF0E120E)
}

internal val FreshLightColorScheme = lightColorScheme(
    primary = FreshPalette.Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F5D9),
    onPrimaryContainer = Color(0xFF00390A),
    inversePrimary = FreshPalette.GreenBright,
    secondary = Color(0xFF4F6350),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1EFE1),
    onSecondaryContainer = Color(0xFF0C1F0E),
    tertiary = Color(0xFF3B6470),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBFE9F7),
    onTertiaryContainer = Color(0xFF001F27),
    background = FreshPalette.CanvasLight,
    onBackground = Color(0xFF171D16),
    surface = FreshPalette.CanvasLight,
    onSurface = Color(0xFF171D16),
    surfaceVariant = Color(0xFFDDE5DA),
    onSurfaceVariant = Color(0xFF414940),
    surfaceTint = FreshPalette.Green,
    inverseSurface = Color(0xFF2C322C),
    inverseOnSurface = Color(0xFFEDF1EA),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF71796F),
    outlineVariant = Color(0xFFE1E9DD),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFBFDF7),
    surfaceDim = Color(0xFFD8DED5),
    surfaceContainerLowest = FreshPalette.CardLight,
    surfaceContainerLow = Color(0xFFF7FBF4),
    surfaceContainer = Color(0xFFF1F6EF),
    surfaceContainerHigh = Color(0xFFEBF1E9),
    surfaceContainerHighest = Color(0xFFE5EBE3),
)

internal val FreshDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FE05A),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF00530F),
    onPrimaryContainer = Color(0xFFC6F5C9),
    inversePrimary = Color(0xFF006E14),
    secondary = Color(0xFFB5CCB7),
    onSecondary = Color(0xFF213523),
    secondaryContainer = Color(0xFF374B39),
    onSecondaryContainer = Color(0xFFD1E8D2),
    tertiary = Color(0xFFA4CDDB),
    onTertiary = Color(0xFF033641),
    tertiaryContainer = Color(0xFF214C58),
    onTertiaryContainer = Color(0xFFBFE9F7),
    background = FreshPalette.CanvasDark,
    onBackground = Color(0xFFE1E4DC),
    surface = FreshPalette.CanvasDark,
    onSurface = Color(0xFFE1E4DC),
    surfaceVariant = Color(0xFF414940),
    onSurfaceVariant = Color(0xFFC0C9BE),
    surfaceTint = Color(0xFF4FE05A),
    inverseSurface = Color(0xFFE1E4DC),
    inverseOnSurface = Color(0xFF2C322C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8B938A),
    outlineVariant = Color(0xFF2A312A),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF353B34),
    surfaceDim = Color(0xFF0E120E),
    surfaceContainerLowest = Color(0xFF090C09),
    surfaceContainerLow = Color(0xFF151A15),
    surfaceContainer = FreshPalette.CardDark,
    surfaceContainerHigh = Color(0xFF242A24),
    surfaceContainerHighest = Color(0xFF2F352F),
)

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

    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val colorScheme = if (darkTheme) FreshDarkColorScheme else FreshLightColorScheme

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        shapes = FreshShapes,
        content = {
            MonetColorsProvider.UpdateCss(colorScheme)
            content()
        }
    )
}
