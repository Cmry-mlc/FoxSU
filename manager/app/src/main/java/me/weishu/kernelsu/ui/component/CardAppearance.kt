package me.weishu.kernelsu.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.settings.CustomizationStore

/** Resolved colours for one card: global settings with an optional per-card override. */
data class CardSurface(
    val container: Color,
    val content: Color,
    val accent: Color,
    val border: BorderStroke?,
)

/**
 * Slot 0 is the hero card, 1..4 are the KPI cards, 5 is the module list.
 * A card either follows the global card colour / opacity or overrides it from the
 * appearance sheet (long press).
 */
@Composable
fun resolveCardSurface(slot: Int): CardSurface {
    val custom = CustomizationStore.current
    val overrideColor = custom.surfaceColors.getOrNull(slot) ?: 0
    val effectiveColor = if (overrideColor != 0) overrideColor else custom.cardColor
    val alphaOverride = custom.surfaceAlphas.getOrNull(slot) ?: -1f
    val alpha = if (alphaOverride >= 0f) alphaOverride else custom.cardAlpha

    val base = if (effectiveColor == 0) freshCardColor() else Color(effectiveColor)
    val content = when {
        effectiveColor == 0 -> MaterialTheme.colorScheme.onSurface
        base.luminance() > 0.55f -> Color(0xFF141A14)
        else -> Color.White
    }
    val accent = if (effectiveColor == 0) MaterialTheme.colorScheme.primary else content
    val border = if (effectiveColor == 0) freshCardBorder() else null
    return CardSurface(base.copy(alpha = alpha), content, accent, border)
}

/**
 * Bottom-sheet style editor for a single card: surface colour (presets + free HSV picker),
 * surface opacity and an optional background picture.
 */
@Composable
fun CardAppearanceDialog(
    slot: Int,
    title: String,
    description: String,
    hasImage: Boolean,
    imageAlpha: Float,
    imageAlphaRange: ClosedFloatingPointRange<Float>,
    imageAlphaLabel: String,
    onImageAlphaChange: (Float) -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onDismiss: () -> Unit,
    showImage: Boolean = true,
) {
    val custom = CustomizationStore.current
    val surfaceColor = custom.surfaceColors.getOrNull(slot) ?: 0
    val surfaceAlpha = (custom.surfaceAlphas.getOrNull(slot) ?: -1f)
        .let { if (it >= 0f) it else custom.cardAlpha }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = freshCardColor(),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.fresh_home_card_color),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(8.dp))
                CardColorSection(
                    selected = surfaceColor,
                    onSelect = { CustomizationStore.setSurfaceColor(slot, it) },
                )

                DialogSliderRow(
                    label = stringResource(R.string.fresh_home_card_alpha),
                    value = surfaceAlpha,
                    range = 0.2f..1f,
                    onChange = { CustomizationStore.setSurfaceAlpha(slot, it) },
                )

                if (showImage) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.customization_wallpaper),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            text = stringResource(R.string.customization_pick_image),
                            icon = Icons.Filled.Image,
                            filled = true,
                            onClick = onPickImage,
                        )
                        if (hasImage) {
                            PillButton(
                                text = stringResource(R.string.customization_clear),
                                icon = Icons.Filled.Delete,
                                filled = false,
                                onClick = onRemoveImage,
                            )
                        }
                    }
                    if (hasImage) {
                        DialogSliderRow(
                            label = imageAlphaLabel,
                            value = imageAlpha,
                            range = imageAlphaRange,
                            onChange = onImageAlphaChange,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onDismiss),
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.fresh_home_cancel),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun CardColorSection(selected: Int, onSelect: (Int) -> Unit) {
    val presets = CustomizationStore.cardColorOptions
    val isPreset = selected == 0 || presets.contains(selected)
    var customOpen by remember { mutableStateOf(!isPreset) }
    val themeSeed = MaterialTheme.colorScheme.primary.toArgb()
    var hsv by remember { mutableStateOf(argbToHsv(if (selected == 0) themeSeed else selected)) }

    CardColorSwatches(
        selected = selected,
        onSelect = {
            customOpen = false
            onSelect(it)
        },
        customColor = if (isPreset) null else selected,
        customOpen = customOpen,
        onCustomClick = {
            customOpen = !customOpen
            if (!isPreset) hsv = argbToHsv(selected)
        },
    )

    if (!customOpen) return

    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(hsvToArgb(hsv)))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "#" + Integer.toHexString(hsvToArgb(hsv) and 0xFFFFFF)
                .uppercase()
                .padStart(6, '0'),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }

    Spacer(Modifier.height(6.dp))
    GradientColorSlider(
        label = stringResource(R.string.fresh_home_hue),
        value = hsv[0],
        range = 0f..360f,
        gradient = Brush.horizontalGradient(
            listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
            )
        ),
        onChange = {
            hsv = floatArrayOf(it, hsv[1], hsv[2])
            onSelect(hsvToArgb(hsv))
        },
    )
    GradientColorSlider(
        label = stringResource(R.string.fresh_home_saturation),
        value = hsv[1],
        range = 0f..1f,
        gradient = Brush.horizontalGradient(
            listOf(
                Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], 0f, 1f))),
                Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], 1f, 1f))),
            )
        ),
        onChange = {
            hsv = floatArrayOf(hsv[0], it, hsv[2])
            onSelect(hsvToArgb(hsv))
        },
    )
    GradientColorSlider(
        label = stringResource(R.string.fresh_home_brightness),
        value = hsv[2],
        range = 0.05f..1f,
        gradient = Brush.horizontalGradient(
            listOf(
                Color.Black,
                Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], 1f))),
                Color.White,
            )
        ),
        onChange = {
            hsv = floatArrayOf(hsv[0], hsv[1], it)
            onSelect(hsvToArgb(hsv))
        },
    )
}

@Composable
private fun GradientColorSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    gradient: Brush,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = (value * 100).toInt().toString() + "%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(gradient),
            )
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun CardColorSwatches(
    selected: Int,
    onSelect: (Int) -> Unit,
    customColor: Int?,
    customOpen: Boolean,
    onCustomClick: () -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CustomizationStore.cardColorOptions.forEach { value ->
            val isSelected = value == selected
            val color = if (value == 0) freshCardColor() else Color(value)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape,
                    )
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    value == 0 -> Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.customization_follow_theme),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )

                    isSelected -> Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (color.luminance() > 0.55f) Color(0xFF141A14) else Color.White,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }

        val rainbow = listOf(
            Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF22C55E),
            Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFFA855F7), Color(0xFFEF4444),
        )
        val active = customColor != null || customOpen
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .then(
                    if (customColor != null) {
                        Modifier.background(Color(customColor))
                    } else {
                        Modifier.background(Brush.sweepGradient(rainbow))
                    }
                )
                .border(
                    width = if (active) 3.dp else 1.dp,
                    color = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = CircleShape,
                )
                .clickable(onClick = onCustomClick),
            contentAlignment = Alignment.Center,
        ) {
            if (customColor == null) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = stringResource(R.string.fresh_home_custom_color),
                    tint = Color.White,
                    modifier = Modifier.size(17.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = if (Color(customColor).luminance() > 0.55f) Color(0xFF141A14) else Color.White,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
private fun PillButton(
    text: String,
    icon: ImageVector,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val container = if (filled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    }
    val content = if (filled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun argbToHsv(argb: Int): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    return hsv
}

private fun hsvToArgb(hsv: FloatArray): Int =
    android.graphics.Color.HSVToColor(
        floatArrayOf(hsv[0], hsv[1].coerceIn(0f, 1f), hsv[2].coerceIn(0.05f, 1f))
    )
