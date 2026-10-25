package me.weishu.kernelsu.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import me.weishu.kernelsu.data.settings.Customization
import me.weishu.kernelsu.data.settings.CustomizationStore
import java.io.File

/** Loads the wallpaper with a sampled decode so a 4K photo does not blow up the heap. */
fun decodeWallpaper(path: String, maxSize: Int = 1600): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) {
        sample *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
}.getOrNull()

@Composable
fun rememberWallpaper(path: String?): ImageBitmap? = remember(path) {
    path?.takeIf { File(it).exists() }?.let { decodeWallpaper(it) }
}

/**
 * Draws the user wallpaper (zoom + vertical pan) and the dim scrim that keeps text readable.
 */
@Composable
fun WallpaperBackground(
    customization: Customization,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberWallpaper(customization.wallpaperUri) ?: return

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = customization.wallpaperScale
                    scaleX = scale
                    scaleY = scale
                    translationX = customization.wallpaperOffsetX * size.width
                    translationY = customization.wallpaperOffsetY * size.height
                },
        )
        if (customization.wallpaperDim > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = customization.wallpaperDim)),
            )
        }
    }
}

/**
 * Paints the wallpaper (if any) behind [content]; used by every main screen so the chosen
 * background is not limited to the home tab.
 */
@Composable
fun WallpaperBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val custom = CustomizationStore.current
    if (custom.wallpaperUri == null) {
        content()
        return
    }
    Box(modifier = modifier.fillMaxSize()) {
        WallpaperBackground(customization = custom)
        content()
    }
}

/** True when the user picked a wallpaper, so scaffolds can go transparent. */
@Composable
@ReadOnlyComposable
fun hasCustomWallpaper(): Boolean = CustomizationStore.current.wallpaperUri != null

/** Scaffold container that is transparent while a wallpaper is in use. */
@Composable
@ReadOnlyComposable
fun scaffoldBackgroundColor(fallback: Color): Color =
    if (hasCustomWallpaper()) Color.Transparent else fallback
