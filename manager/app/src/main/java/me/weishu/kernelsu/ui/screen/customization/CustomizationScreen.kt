package me.weishu.kernelsu.ui.screen.customization

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.settings.Customization
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.component.WallpaperBackground
import me.weishu.kernelsu.ui.component.WallpaperBackdrop
import me.weishu.kernelsu.ui.component.freshCardBorder
import me.weishu.kernelsu.ui.component.freshCardColor
import me.weishu.kernelsu.ui.component.rememberCardSurface
import me.weishu.kernelsu.ui.component.ImageCropDialog
import me.weishu.kernelsu.ui.component.scaffoldBackgroundColor
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.LauncherIconManager
import me.weishu.kernelsu.ui.util.savePickedImage

@Composable
fun CustomizationScreen() {
    val navigator = LocalNavigator.current
    val custom = CustomizationStore.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingWallpaperCrop by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                savePickedImage(context, uri, "wallpaper_raw.jpg", maxSide = 2400)
            }
            if (saved == null) {
                Toast.makeText(context, R.string.customization_wallpaper_failed, Toast.LENGTH_SHORT).show()
            } else {
                pendingWallpaperCrop = saved
            }
        }
    }

    val glassPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                savePickedImage(context, uri, "glass_wallpaper.jpg", maxSide = 2000)
            }
            if (saved == null) {
                Toast.makeText(context, R.string.customization_wallpaper_failed, Toast.LENGTH_SHORT).show()
            } else {
                CustomizationStore.setGlassWallpaper(saved)
            }
        }
    }

    val heroPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                savePickedImage(context, uri, "hero_card.jpg", maxSide = 1400)
            }
            if (saved == null) {
                Toast.makeText(context, R.string.customization_wallpaper_failed, Toast.LENGTH_SHORT).show()
            } else {
                CustomizationStore.setHeroWallpaper(saved)
            }
        }
    }

    pendingWallpaperCrop?.let { raw ->
        ImageCropDialog(
            sourcePath = raw,
            aspectRatio = 0.5625f,
            outputName = "wallpaper.jpg",
            onConfirm = { cropped ->
                CustomizationStore.setWallpaper(cropped)
                CustomizationStore.setWallpaperEnabled(true)
                pendingWallpaperCrop = null
            },
            onDismiss = { pendingWallpaperCrop = null },
        )
    }

    Scaffold(
        containerColor = scaffoldBackgroundColor(MaterialTheme.colorScheme.surface),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.customization_title)) },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        WallpaperBackdrop {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { PreviewCard(custom) }



                item {
                    SectionCard(
                        icon = Icons.Filled.Wallpaper,
                        title = stringResource(R.string.customization_global_wallpaper),
                        subtitle = stringResource(R.string.customization_global_wallpaper_sub),
                    ) {
                        ToggleRow(
                            title = stringResource(R.string.customization_wallpaper_enable),
                            subtitle = stringResource(R.string.customization_wallpaper_enable_sum),
                            checked = custom.wallpaperEnabled,
                            onChange = CustomizationStore::setWallpaperEnabled,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton(
                                text = stringResource(R.string.customization_pick_image),
                                icon = Icons.Filled.Photo,
                                filled = true,
                                onClick = {
                                    picker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            )
                            if (custom.wallpaperUri != null) {
                                PillButton(
                                    text = stringResource(R.string.customization_clear),
                                    icon = Icons.Filled.DeleteOutline,
                                    filled = false,
                                    onClick = { CustomizationStore.setWallpaper(null) },
                                )
                            }
                        }
                    }
                }

                item {
                    SectionCard(
                        icon = Icons.Filled.Wallpaper,
                        title = stringResource(R.string.customization_hero_title),
                        subtitle = stringResource(R.string.customization_hero_sub),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton(
                                text = stringResource(R.string.customization_pick_image),
                                icon = Icons.Filled.Photo,
                                filled = true,
                                onClick = {
                                    heroPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            )
                            PillButton(
                                text = stringResource(R.string.customization_clear),
                                icon = Icons.Filled.DeleteOutline,
                                filled = false,
                                enabled = custom.heroWallpaperUri != null,
                                onClick = { CustomizationStore.setHeroWallpaper(null) },
                            )
                        }

                        if (custom.heroWallpaperUri != null) {
                            SliderRow(
                                label = stringResource(R.string.customization_scale),
                                value = custom.heroScale,
                                range = 1f..2.5f,
                                display = "${(custom.heroScale * 100).toInt()}%",
                                onChange = CustomizationStore::setHeroScale,
                            )
                            SliderRow(
                                label = stringResource(R.string.customization_offset_x),
                                value = custom.heroOffsetX,
                                range = -0.4f..0.4f,
                                display = "${(custom.heroOffsetX * 100).toInt()}%",
                                onChange = CustomizationStore::setHeroOffsetX,
                            )
                            SliderRow(
                                label = stringResource(R.string.customization_offset),
                                value = custom.heroOffsetY,
                                range = -0.4f..0.4f,
                                display = "${(custom.heroOffsetY * 100).toInt()}%",
                                onChange = CustomizationStore::setHeroOffsetY,
                            )
                        }
                        SliderRow(
                            label = stringResource(R.string.customization_dim),
                            value = custom.heroDim,
                            range = 0f..0.8f,
                            display = "${(custom.heroDim * 100).toInt()}%",
                            onChange = CustomizationStore::setHeroDim,
                        )
                    }
                }

                item {
                    SectionCard(
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.customization_card),
                        subtitle = stringResource(R.string.customization_card_sub),
                    ) {
                        ToggleRow(
                            title = stringResource(R.string.customization_card_glass),
                            subtitle = stringResource(R.string.customization_card_glass_sum),
                            checked = custom.cardGlass,
                            onChange = CustomizationStore::setCardGlass,
                        )
                        ToggleRow(
                            title = stringResource(R.string.customization_rainbow),
                            subtitle = stringResource(R.string.customization_rainbow_sum),
                            checked = custom.rainbowCards,
                            onChange = CustomizationStore::setRainbowCards,
                        )
                        Spacer(Modifier.height(6.dp))
                        if (custom.cardGlass) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.customization_glass_wallpaper),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.customization_glass_wallpaper_sum),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PillButton(
                                    text = stringResource(R.string.customization_pick_image),
                                    icon = Icons.Filled.Photo,
                                    filled = true,
                                    onClick = {
                                        glassPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                )
                                if (custom.glassWallpaper != null) {
                                    PillButton(
                                        text = stringResource(R.string.customization_clear),
                                        icon = Icons.Filled.DeleteOutline,
                                        filled = false,
                                        onClick = { CustomizationStore.setGlassWallpaper(null) },
                                    )
                                }
                            }
                        }

                        SliderRow(
                            label = stringResource(R.string.customization_card_alpha),
                            value = custom.cardAlpha,
                            range = 0.2f..1f,
                            display = "${(custom.cardAlpha * 100).toInt()}%",
                            onChange = CustomizationStore::setCardAlpha,
                        )
                    }
                }

                item {
                    SectionCard(
                        icon = Icons.Filled.Title,
                        title = stringResource(R.string.customization_title_bar),
                        subtitle = stringResource(R.string.customization_title_bar_sub),
                    ) {
                        OutlinedTextField(
                            value = custom.homeTitle,
                            onValueChange = CustomizationStore::setHomeTitle,
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.customization_title_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item {
                    SectionCard(
                        icon = Icons.Filled.TextFields,
                        title = stringResource(R.string.customization_font),
                        subtitle = stringResource(R.string.customization_font_sub),
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(4) { index ->
                                StyleChip(
                                    label = stringResource(fontLabelRes(index)),
                                    selected = custom.fontStyle == index,
                                    onClick = { CustomizationStore.setFontStyle(index) },
                                )
                            }
                        }
                    }
                }

                item {
                    SectionCard(
                        icon = Icons.Filled.SmartButton,
                        title = stringResource(R.string.customization_bottom_bar),
                        subtitle = stringResource(R.string.customization_bottom_bar_sub),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StyleChip(
                                label = stringResource(R.string.customization_bottom_style_pill),
                                selected = custom.bottomBarStyle == 0,
                                onClick = { CustomizationStore.setBottomBarStyle(0) },
                            )
                            StyleChip(
                                label = stringResource(R.string.customization_bottom_style_drawer),
                                selected = custom.bottomBarStyle == 1,
                                onClick = { CustomizationStore.setBottomBarStyle(1) },
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.customization_bottom_immersive),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.customization_bottom_immersive_sum),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = custom.bottomBarImmersive,
                                onCheckedChange = CustomizationStore::setBottomBarImmersive,
                            )
                        }

                        SliderRow(
                            label = stringResource(R.string.customization_radius),
                            value = custom.bottomBarRadius,
                            range = 0f..100f,
                            display = "${custom.bottomBarRadius.toInt()}dp",
                            onChange = CustomizationStore::setBottomBarRadius,
                        )
                    }
                }

                item {
                    SectionCard(
                        icon = Icons.Filled.Photo,
                        title = stringResource(R.string.customization_icon),
                        subtitle = stringResource(R.string.customization_icon_hint),
                    ) {
                        val iconChoices = listOf(
                            IconChoice(
                                style = LauncherIconManager.STYLE_FOXSU,
                                labelRes = R.string.customization_icon_foxsu,
                                iconRes = R.drawable.ic_launcher_foreground,
                                background = Brush.linearGradient(
                                    listOf(Color(0xFFEFFBF3), Color(0xFFB7E3C6))
                                ),
                            ),
                            IconChoice(
                                style = LauncherIconManager.STYLE_FOX2,
                                labelRes = R.string.customization_icon_fox2,
                                iconRes = R.drawable.ic_launcher_fox2_foreground,
                                background = Brush.linearGradient(
                                    listOf(Color(0xFFF6F8F7), Color(0xFFDBE6DF))
                                ),
                            ),
                            IconChoice(
                                style = LauncherIconManager.STYLE_FOX3,
                                labelRes = R.string.customization_icon_fox3,
                                iconRes = R.drawable.ic_launcher_fox3_foreground,
                                background = Brush.linearGradient(
                                    listOf(Color(0xFFFFF3E7), Color(0xFFFFD3B4))
                                ),
                            ),
                            IconChoice(
                                style = LauncherIconManager.STYLE_FOX4,
                                labelRes = R.string.customization_icon_fox4,
                                iconRes = R.drawable.ic_launcher_fox4_foreground,
                                background = Brush.linearGradient(
                                    listOf(Color(0xFFEDFAF3), Color(0xFFBFE8D5))
                                ),
                            ),
                            IconChoice(
                                style = LauncherIconManager.STYLE_KERNELSU,
                                labelRes = R.string.customization_icon_kernelsu,
                                iconRes = R.drawable.ic_launcher_kernelsu_foreground,
                                background = Brush.linearGradient(listOf(Color.White, Color.White)),
                            ),
                        )
                        iconChoices.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                row.forEach { choice ->
                                    IconChip(
                                        modifier = Modifier.weight(1f),
                                        label = stringResource(choice.labelRes),
                                        selected = custom.iconStyle == choice.style,
                                        background = choice.background,
                                        iconRes = choice.iconRes,
                                        onSelect = {
                                            CustomizationStore.setIconStyle(choice.style)
                                            LauncherIconManager.apply(choice.style)
                                            Toast.makeText(
                                                context,
                                                R.string.customization_icon_hint,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        },
                                    )
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                item {
                    PillButton(
                        text = stringResource(R.string.customization_reset),
                        icon = Icons.Filled.Refresh,
                        filled = false,
                        onClick = {
                            CustomizationStore.reset()
                            LauncherIconManager.apply(LauncherIconManager.STYLE_FOXSU)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(custom: Customization) {
    val cardColor = if (custom.cardColor == 0) {
        freshCardColor()
    } else {
        Color(custom.cardColor)
    }.copy(alpha = custom.cardAlpha)

    val onCard = if (custom.cardColor != 0 && Color(custom.cardColor).luminance() < 0.55f) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.height(220.dp)) {
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
                    .padding(horizontal = 26.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(shape = RoundedCornerShape(18.dp), color = cardColor, contentColor = onCard) {
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
                            color = onCard.copy(alpha = 0.7f),
                        )
                    }
                }
                Surface(shape = RoundedCornerShape(18.dp), color = cardColor, contentColor = onCard) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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
                            color = onCard.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = rememberCardSurface(freshCardColor()),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = display,
                style = MaterialTheme.typography.labelMedium,
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
    icon: ImageVector,
    filled: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val container = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainer
        filled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    }
    val content = when {
        !enabled -> MaterialTheme.colorScheme.outline
        filled -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content,
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
private fun StyleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

private class IconChoice(
    val style: Int,
    val labelRes: Int,
    val iconRes: Int,
    val background: Brush,
)

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
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
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

/** 通用开关行：标题 + 说明 + 右侧开关。 */
@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** 九宫格胶囊：小图标 + 名称，选中时描边 + 主题色底。 */
@Composable
private fun IconChip(
    label: String,
    selected: Boolean,
    background: Brush,
    iconRes: Int,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            freshCardColor()
        },
        border = if (selected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            freshCardBorder()
        },
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onSelect),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(background),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

/** 字体选项文案。 */
private fun fontLabelRes(index: Int): Int = when (index) {
    1 -> R.string.customization_font_serif
    2 -> R.string.customization_font_mono
    3 -> R.string.customization_font_round
    else -> R.string.customization_font_system
}
