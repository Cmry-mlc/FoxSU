package me.weishu.kernelsu.ui.screen.customization

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.settings.Customization
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.component.WallpaperBackground
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.LauncherIconManager
import java.io.File
import java.io.FileOutputStream

@Composable
fun CustomizationScreen() {
    val navigator = LocalNavigator.current
    val custom by CustomizationStore.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) { saveWallpaper(context, uri) }
            if (saved == null) {
                Toast.makeText(context, R.string.customization_wallpaper_failed, Toast.LENGTH_SHORT).show()
            } else {
                CustomizationStore.setWallpaper(saved)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.customization_title)) },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { PreviewCard(custom) }

            item {
                SectionCard(title = stringResource(R.string.customization_wallpaper)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PillButton(
                            text = stringResource(R.string.customization_pick_image),
                            icon = Icons.Filled.Photo,
                            onClick = {
                                picker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        )
                        PillButton(
                            text = stringResource(R.string.customization_clear),
                            icon = Icons.Filled.DeleteOutline,
                            enabled = custom.wallpaperUri != null,
                            onClick = { CustomizationStore.setWallpaper(null) },
                        )
                    }

                    if (custom.wallpaperUri != null) {
                        Spacer(Modifier.height(6.dp))
                        SliderRow(
                            label = stringResource(R.string.customization_scale),
                            value = custom.wallpaperScale,
                            range = 1f..2.5f,
                            onChange = CustomizationStore::setScale,
                        )
                        SliderRow(
                            label = stringResource(R.string.customization_offset),
                            value = custom.wallpaperOffsetY,
                            range = -0.4f..0.4f,
                            onChange = CustomizationStore::setOffsetY,
                        )
                    }
                    SliderRow(
                        label = stringResource(R.string.customization_dim),
                        value = custom.wallpaperDim,
                        range = 0f..0.8f,
                        onChange = CustomizationStore::setDim,
                    )
                }
            }

            item {
                SectionCard(title = stringResource(R.string.customization_card)) {
                    Text(
                        text = stringResource(R.string.customization_card_color),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(CustomizationStore.cardColorOptions.size) { index ->
                            val value = CustomizationStore.cardColorOptions[index]
                            val selected = custom.cardColor == value
                            val color = if (value == 0) {
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            } else {
                                Color(value)
                            }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                        shape = CircleShape,
                                    )
                                    .clickable { CustomizationStore.setCardColor(value) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (value == 0) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = stringResource(R.string.customization_follow_theme),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    SliderRow(
                        label = stringResource(R.string.customization_card_alpha),
                        value = custom.cardAlpha,
                        range = 0.2f..1f,
                        onChange = CustomizationStore::setCardAlpha,
                    )
                }
            }

            item {
                SectionCard(title = stringResource(R.string.customization_icon)) {
                    IconOption(
                        label = stringResource(R.string.customization_icon_foxsu),
                        selected = custom.iconStyle == LauncherIconManager.STYLE_FOXSU,
                        background = Brush.linearGradient(
                            listOf(Color(0xFF4AE75A), Color(0xFF008F06))
                        ),
                        iconRes = R.drawable.ic_launcher_foreground,
                        onSelect = {
                            CustomizationStore.setIconStyle(LauncherIconManager.STYLE_FOXSU)
                            LauncherIconManager.apply(LauncherIconManager.STYLE_FOXSU)
                            Toast.makeText(context, R.string.customization_icon_hint, Toast.LENGTH_SHORT).show()
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    IconOption(
                        label = stringResource(R.string.customization_icon_kernelsu),
                        selected = custom.iconStyle == LauncherIconManager.STYLE_KERNELSU,
                        background = Brush.linearGradient(listOf(Color.White, Color.White)),
                        iconRes = R.drawable.ic_launcher_kernelsu_foreground,
                        onSelect = {
                            CustomizationStore.setIconStyle(LauncherIconManager.STYLE_KERNELSU)
                            LauncherIconManager.apply(LauncherIconManager.STYLE_KERNELSU)
                            Toast.makeText(context, R.string.customization_icon_hint, Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }

            item {
                PillButton(
                    text = stringResource(R.string.customization_reset),
                    icon = Icons.Filled.Refresh,
                    onClick = {
                        CustomizationStore.reset()
                        LauncherIconManager.apply(LauncherIconManager.STYLE_FOXSU)
                    },
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(custom: Customization) {
    val cardColor = if (custom.cardColor == 0) {
        MaterialTheme.colorScheme.surfaceContainerLowest
    } else {
        Color(custom.cardColor)
    }.copy(alpha = custom.cardAlpha)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.height(210.dp)) {
            if (custom.wallpaperUri != null) {
                WallpaperBackground(customization = custom)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surface,
                                )
                            )
                        ),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(shape = RoundedCornerShape(18.dp), color = cardColor) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.customization_preview_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Surface(shape = RoundedCornerShape(18.dp), color = cardColor) {
                    Row(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "LKM",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.customization_preview_card),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
        )
    }
}

@Composable
private fun PillButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (enabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (enabled) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.outline
        },
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun IconOption(
    label: String,
    selected: Boolean,
    background: Brush,
    iconRes: Int,
    onSelect: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(background),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun saveWallpaper(context: android.content.Context, uri: Uri): String? = runCatching {
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it)
    } ?: return null

    val maxSide = 2000
    val scaled = if (bitmap.width > maxSide || bitmap.height > maxSide) {
        val ratio = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true,
        )
    } else {
        bitmap
    }

    val file = File(context.filesDir, "wallpaper.jpg")
    FileOutputStream(file).use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }
    if (scaled !== bitmap) scaled.recycle()
    bitmap.recycle()
    file.absolutePath
}.getOrNull()
