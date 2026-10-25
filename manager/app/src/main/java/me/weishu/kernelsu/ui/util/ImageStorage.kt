package me.weishu.kernelsu.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Copies an image picked through the photo picker into the app's private storage, downscaled so
 * it stays cheap to decode later. Returns the absolute path, or null when the stream is unreadable.
 */
fun savePickedImage(
    context: Context,
    uri: Uri,
    fileName: String,
    maxSide: Int = 2000,
    quality: Int = 92,
): String? = runCatching {
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it)
    } ?: return null

    val scaled = if (bitmap.width > maxSide || bitmap.height > maxSide) {
        val ratio = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    } else {
        bitmap
    }

    val file = File(context.filesDir, fileName)
    FileOutputStream(file).use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
    }
    if (scaled !== bitmap) scaled.recycle()
    bitmap.recycle()
    file.absolutePath
}.getOrNull()
