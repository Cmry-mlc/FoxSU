package com.kuaimaosu.manager.ui.screen.themeSettings.component

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AutoAwesome
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kuaimaosu.manager.data.theme.ThemeRepository
import com.kuaimaosu.manager.domain.usecase.IsSystemLanguageSettingsUseCase
import com.kuaimaosu.manager.domain.usecase.LaunchSystemLanguageSettingsUseCase
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.ui.component.settings.SettingsChooseDialog
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.util.KuaiMaoPoetry
import com.kuaimaosu.manager.ui.viewmodel.SettingsUiAction
import com.kuaimaosu.manager.ui.viewmodel.SettingsUiState
import com.kuaimaosu.manager.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ThemeSettingsDialogs(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    val themeConfig: ThemeConfig = koinInject()
    var showCustomPicker by remember { mutableStateOf(false) }

    if (state.showThemeColorDialog) {
        val close = {
            showCustomPicker = false
            viewModel.dispatch(SettingsUiAction.SetThemeColorDialogVisible(false))
        }
        if (showCustomPicker) {
            KuaiMaoColorPickerDialog(
                title = stringResource(R.string.choose_color),
                initialColor = themeConfig.seedColor,
                onConfirm = { seedColor ->
                    viewModel.dispatch(SettingsUiAction.SetThemeColor(seedColor))
                    close()
                },
                onDismiss = { showCustomPicker = false },
            )
        } else {
            ThemeStyleDialog(
                currentColor = themeConfig.seedColor,
                customEnabled = themeConfig.useCustomThemeColor,
                onPick = { seedColor ->
                    viewModel.dispatch(SettingsUiAction.SetThemeColor(seedColor))
                    close()
                },
                onCustom = { showCustomPicker = true },
                onDismiss = close,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionDialog(
    currentLocale: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isSystemLanguageSettingsUseCase =
        koinInject<IsSystemLanguageSettingsUseCase>()
    val launchSystemLanguageSettingsUseCase =
        koinInject<LaunchSystemLanguageSettingsUseCase>()
    val context = LocalContext.current
    val languageUseSystemDefault = stringResource(R.string.language_system_default)
    val systemLanguage = stringResource(R.string.settings_language)

    if (isSystemLanguageSettingsUseCase()) {
        launchSystemLanguageSettingsUseCase(context)
        onDismiss()
    } else {
        val supportedLocales = remember {
            val locales = mutableListOf<java.util.Locale>()
            locales.add(java.util.Locale.ROOT)

            val resourceDirs = listOf(
                "ar", "bg", "de", "fa", "fr", "hu", "in", "it",
                "ja", "ko", "pl", "pt-rBR", "ru", "th", "tr",
                "uk", "vi", "zh-rCN", "zh-rTW",
            )

            resourceDirs.forEach { dir ->
                try {
                    val locale = when {
                        dir.contains("-r") -> {
                            val parts = dir.split("-r")
                            java.util.Locale.Builder()
                                .setLanguage(parts[0])
                                .setRegion(parts[1])
                                .build()
                        }

                        else -> java.util.Locale.Builder()
                            .setLanguage(dir)
                            .build()
                    }

                    val config = android.content.res.Configuration()
                    config.setLocale(locale)
                    val localizedContext = context.createConfigurationContext(config)
                    val testString = localizedContext.getString(R.string.settings_language)

                    if (testString != systemLanguage || locale.language == "en") {
                        locales.add(locale)
                    }
                } catch (_: Exception) {
                }
            }

            val sortedLocales = locales.drop(1).sortedBy { it.getDisplayName(it) }
            mutableListOf<java.util.Locale>().apply {
                add(locales.first())
                addAll(sortedLocales)
            }
        }

        val allOptions = supportedLocales.map { locale ->
            val tag = if (locale == java.util.Locale.ROOT) {
                "system"
            } else if (locale.country.isEmpty()) {
                locale.language
            } else {
                "${locale.language}_${locale.country}"
            }

            val displayName = if (locale == java.util.Locale.ROOT) {
                languageUseSystemDefault
            } else {
                locale.getDisplayName(locale)
            }

            tag to displayName
        }

        var selectedIndex by remember {
            mutableIntStateOf(allOptions.indexOfFirst { (tag, _) -> currentLocale == tag })
        }

        SettingsChooseDialog(
            show = true,
            title = stringResource(R.string.settings_language),
            items = allOptions.map { (_, displayName) -> displayName },
            selectedIndex = selectedIndex,
            onDismiss = onDismiss,
            onSelectedIndexChange = { index ->
                selectedIndex = index
                if (selectedIndex >= 0 && selectedIndex < allOptions.size) {
                    val newLocale = allOptions[selectedIndex].first
                    onLanguageSelected(newLocale)
                }
            },
        )
    }
}

/**
 * 主题风格预设：与系统主题面板一致的一组配色。
 * 点一下立即生效，「自定义」再进滑杆取色，两者共用同一套落盘逻辑。
 */
data class KuaiMaoThemePreset(val color: Int, val nameRes: Int)

val KuaiMaoThemePresets: List<KuaiMaoThemePreset> = listOf(
    // 统一是「低饱和、同明度」的一族：饱和度都在 25% 上下、亮度都在 30% 上下，
    // 以 #475C91 这块石板蓝为基准配的。之前的预设饱和度太高，装上去发飘，
    // 而且经调色板映射后色相会跑掉。
    KuaiMaoThemePreset(0xFF475C91.toInt(), R.string.theme_style_indigo),   // 石板蓝
    KuaiMaoThemePreset(0xFF3F6B70.toInt(), R.string.theme_style_teal),     // 灰青
    KuaiMaoThemePreset(0xFF476B4E.toInt(), R.string.theme_style_green),    // 松绿
    KuaiMaoThemePreset(0xFF5D5480.toInt(), R.string.theme_style_purple),   // 灰紫
    KuaiMaoThemePreset(0xFF8A5A66.toInt(), R.string.theme_style_pink),     // 豆沙
    KuaiMaoThemePreset(0xFF8C4A4A.toInt(), R.string.theme_style_red),      // 赭红
    KuaiMaoThemePreset(0xFF8A5F42.toInt(), R.string.theme_style_orange),   // 陶土
    KuaiMaoThemePreset(0xFF6B5445.toInt(), R.string.theme_style_brown),    // 胡桃
    KuaiMaoThemePreset(0xFF4A6E9E.toInt(), R.string.theme_style_azure),    // 雾蓝
    // 「墨韵生香」两组同族色：粉系与米系，每组从浅到深。
    // 每组最浅的那颗（#FFE3EE / #F7F5F3）已按需求剔除——它跟白卡几乎同色，
    // 选上去跟没选一样。剩下的浅色在亮色主题下会被自动压深（见 Theme.asAccent），
    // 否则会出现「樱花粉当文字色压在白卡上」这种读不出来的情况。
    KuaiMaoThemePreset(0xFFFFB3D0.toInt(), R.string.theme_style_sakura),    // 樱粉
    KuaiMaoThemePreset(0xFFD8CCD7.toInt(), R.string.theme_style_lotus),     // 藕灰
    KuaiMaoThemePreset(0xFFB7B7CC.toInt(), R.string.theme_style_mist),      // 雾紫
    KuaiMaoThemePreset(0xFF535369.toInt(), R.string.theme_style_ink_violet), // 墨紫
    KuaiMaoThemePreset(0xFFDFD6CE.toInt(), R.string.theme_style_oat),       // 米白
    KuaiMaoThemePreset(0xFFA9A09E.toInt(), R.string.theme_style_warm_gray), // 暖灰
    KuaiMaoThemePreset(0xFFD2A792.toInt(), R.string.theme_style_clay),      // 陶杏
    KuaiMaoThemePreset(0xFF5E5E61.toInt(), R.string.theme_style_ink_gray),  // 墨灰
    KuaiMaoThemePreset(0xFF6B7280.toInt(), R.string.theme_style_gray),     // 石墨灰（保留）
    KuaiMaoThemePreset(0xFF2A2E35.toInt(), R.string.theme_style_black),    // 墨黑
    KuaiMaoThemePreset(0xFF000000.toInt(), R.string.theme_style_pure_black), // 纯黑
)

/**
 * 主题风格列表：色点 + 名称 + 「使用 / 使用中」按钮，
 * 与参考稿一致；最后一行「自定义」进入滑杆取色。
 */
@Composable
fun ThemeStyleDialog(
    currentColor: Int,
    customEnabled: Boolean,
    onPick: (Int) -> Unit,
    onCustom: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(26.dp))
                .background(KuaiMaoColors.canvasCardSolid)
                .padding(vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.theme_style),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = KuaiMaoColors.textBody,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.TwoTone.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = KuaiMaoColors.textTer,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                KuaiMaoThemePresets.forEach { preset ->
                    ThemeStyleRow(
                        color = Color(preset.color),
                        label = stringResource(preset.nameRes),
                        active = customEnabled && currentColor == preset.color,
                        onClick = { onPick(preset.color) },
                    )
                }
                ThemeStyleRow(
                    color = KuaiMaoColors.brand,
                    label = stringResource(R.string.theme_style_custom),
                    active = false,
                    isCustom = true,
                    onClick = onCustom,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ThemeStyleRow(
    color: Color,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    isCustom: Boolean = false,
) {
    // 色块很浅或很黑时，名称仍用正文色，避免看不清
    val luminance = color.luminance()
    val labelColor = if (luminance > 0.72f || luminance < 0.12f) {
        KuaiMaoColors.textBody
    } else {
        color
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (isCustom) KuaiMaoColors.brand else color)
                .then(
                    if (!isCustom && luminance > 0.85f) {
                        Modifier.border(1.dp, KuaiMaoColors.lineField, CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isCustom) {
                Icon(
                    imageVector = Icons.TwoTone.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isCustom) KuaiMaoColors.brand else labelColor,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(9.dp),
            border = BorderStroke(
                1.dp,
                if (active) KuaiMaoColors.brand.copy(alpha = 0.55f) else KuaiMaoColors.lineField,
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            modifier = Modifier.height(30.dp),
        ) {
            Text(
                text = if (active) {
                    stringResource(R.string.theme_style_in_use)
                } else {
                    stringResource(R.string.theme_style_use)
                },
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) KuaiMaoColors.brand else KuaiMaoColors.textSec,
            )
        }
    }
}

/**
 * 取色弹窗：大圆预览 + 色相条 + 明度条，底部「自定义 / 取消 / 确定」。
 * 主题色、标题两种文字色都复用它，交互完全一致；
 * 需要精确色值时点「自定义」展开十六进制输入。
 */
@Composable
fun KuaiMaoColorPickerDialog(
    title: String,
    initialColor: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialColor, it) }
    }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(initialColor) { mutableFloatStateOf(initialHsv[2]) }
    var showHex by remember { mutableStateOf(false) }
    var hexInput by remember(initialColor) { mutableStateOf(initialColor.toHexRgb()) }

    val pickedArgb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
    val picked = Color(pickedArgb)

    fun syncFromHex(text: String) {
        val parsed = text.parseHexRgb() ?: return
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(parsed, hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(24.dp))
                .background(KuaiMaoColors.canvasCardSolid)
                .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 10.dp),
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = KuaiMaoColors.textBody,
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .shadow(10.dp, CircleShape, clip = false)
                        .background(picked, CircleShape),
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 色相：整条彩虹
            KuaiMaoGradientSlider(
                fraction = hue / 360f,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                        Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF),
                        Color(0xFFFF0000),
                    ),
                ),
                onFractionChange = { fraction ->
                    hue = fraction * 360f
                    // 从灰/黑进入时把饱和度提起来，否则拖动色相看不出变化
                    if (saturation < 0.25f) saturation = 1f
                },
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 明度：黑 → 当前色相
            KuaiMaoGradientSlider(
                fraction = value,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Black,
                        Color.hsv(hue, saturation.coerceAtLeast(0.02f), 1f),
                    ),
                ),
                onFractionChange = { value = it },
            )

            if (showHex) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = {
                        hexInput = it.sanitizeHexInput()
                        syncFromHex(hexInput)
                    },
                    label = { Text(stringResource(R.string.color_picker_hex)) },
                    placeholder = { Text("4F5BE8") },
                    singleLine = true,
                    isError = hexInput.parseHexRgb() == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { showHex = !showHex },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.theme_style_custom),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KuaiMaoColors.brand,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KuaiMaoColors.textSec,
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        // 用户可能刚敲完色值还没点确认，这里以输入框为准
                        val hexParsed = if (showHex) hexInput.parseHexRgb() else null
                        onConfirm(hexParsed ?: pickedArgb)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KuaiMaoColors.brand,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                    modifier = Modifier.height(38.dp),
                ) {
                    Text(
                        text = stringResource(R.string.confirm),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** 带渐变轨道与白色圆钮的滑杆，用于色相 / 明度。 */
@Composable
private fun KuaiMaoGradientSlider(
    fraction: Float,
    brush: Brush,
    onFractionChange: (Float) -> Unit,
) {
    var trackWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val thumbSize = 22.dp
    val thumbPx = with(density) { thumbSize.toPx() }
    val safeFraction = fraction.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .onSizeChanged { trackWidth = it.width }
            .pointerInput(trackWidth) {
                detectTapGestures { offset ->
                    if (trackWidth > 0) {
                        onFractionChange((offset.x / trackWidth).coerceIn(0f, 1f))
                    }
                }
            }
            .pointerInput(trackWidth) {
                detectHorizontalDragGestures { change, _ ->
                    if (trackWidth > 0) {
                        onFractionChange((change.position.x / trackWidth).coerceIn(0f, 1f))
                    }
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(7.dp))
                .background(brush),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = with(density) { ((trackWidth - thumbPx) * safeFraction).toDp() })
                .size(thumbSize)
                .shadow(4.dp, CircleShape, clip = false)
                .background(Color.White, CircleShape)
                .border(1.dp, Color(0x14000000), CircleShape),
        )
    }
}


/**
 * 自定义首页标题：文案 + 双色分段。
 * 第二段从第 [initialSplit] 个字符开始着色，断点可以落在任意字符上，
 * 所以「第二个字母单独换色」这种玩法也能做到。
 */
@Composable
fun HomeTitleDialog(
    initialText: String,
    initialSplit: Int,
    colorPrimary: Color,
    colorAccent: Color,
    onConfirm: (text: String, split: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var split by remember(initialText) {
        mutableIntStateOf(initialSplit.coerceIn(0, initialText.length))
    }
    val safeSplit = split.coerceIn(0, text.length)

    val preview = buildAnnotatedString {
        withStyle(SpanStyle(color = colorPrimary)) { append(text.take(safeSplit)) }
        withStyle(SpanStyle(color = colorAccent)) { append(text.drop(safeSplit)) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_title_custom)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it.take(24)
                        split = split.coerceIn(0, text.length)
                    },
                    label = { Text(stringResource(R.string.home_title_text)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.size(14.dp))

                Text(
                    text = stringResource(R.string.home_title_preview),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = preview, style = MaterialTheme.typography.headlineSmall)

                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = stringResource(R.string.home_title_split, safeSplit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = safeSplit.toFloat(),
                        onValueChange = { split = it.roundToInt().coerceIn(0, text.length) },
                        valueRange = 0f..text.length.toFloat(),
                        steps = (text.length - 1).coerceAtLeast(0),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text.ifBlank { "KuaiMaoSU" }, safeSplit) }) {
                Icon(
                    Icons.TwoTone.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/** 把 ARGB 整数转成 6 位大写十六进制（不带 #）。 */
internal fun Int.toHexRgb(): String = "%06X".format(this and 0xFFFFFF)

/** 只保留十六进制字符，最多 6 位，方便用户直接粘贴 #4F5BE8 之类的写法。 */
internal fun String.sanitizeHexInput(): String {
    val cleaned = filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    return cleaned.uppercase().take(6)
}

/** 6 位十六进制 → 不透明 ARGB；格式不对返回 null。 */
internal fun String.parseHexRgb(): Int? {
    if (length != 6) return null
    val value = toLongOrNull(16) ?: return null
    return (0xFF000000L or value).toInt()
}

/**
 * 标题上方小字：可手填，也可以让 AI / 本地生成器给一句。
 *
 * 默认是一句中文旧体诗，可手填，也可以让 AI / 本地诗集给一句。
 *
 * 「AI 生成」复用「设置 · AI 助手」里已经配好的模型，这里只留生成入口，
 * 不再重复放一套接口地址 / 密钥。
 */
@Composable
fun HomeKickerDialog(
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var text by remember(initialText) { mutableStateOf(initialText) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("标题上方小字") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(48) },
                    label = { Text("小字内容", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall,
                )

                Spacer(modifier = Modifier.size(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            text = KuaiMaoPoetry.random()
                            error = null
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("随机一句", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            // 直接复用 App 内的 AI 体（设置 · AI 助手里配置的模型），
                            // 不再单独维护一套接口地址 / 密钥。
                            loading = true
                            error = null
                            scope.launch {
                                val ai = com.kuaimaosu.manager.data.ai.AiSettingsStore.load(context)
                                if (!ai.isConfigured) {
                                    text = KuaiMaoPoetry.random()
                                    error = "未配置 AI，先给了一句本地的；去「设置 · AI 助手」填好模型后可用"
                                    loading = false
                                    return@launch
                                }
                                com.kuaimaosu.manager.data.ai.KuaiMaoAiClient.chat(
                                    ai,
                                    "只输出一句中文短诗，18 字以内，写夜色、尘埃与光，不要引号不要解释。",
                                ).onSuccess {
                                    text = it.trim().trim('"', '「', '」').take(48)
                                    error = null
                                }.onFailure {
                                    text = KuaiMaoPoetry.random()
                                    error = it.message ?: "生成失败，先用本地句子顶上"
                                }
                                loading = false
                            }
                        },
                        enabled = !loading,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.6.dp,
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                        } else {
                            Icon(
                                imageVector = Icons.TwoTone.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                        }
                        Text("AI 生成", fontSize = 12.sp)
                    }
                }

                error?.let {
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "预览",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = text.ifBlank { ThemeRepository.DEFAULT_HOME_KICKER },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                    color = KuaiMaoColors.textMuted,
                    maxLines = 2,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text.ifBlank { ThemeRepository.DEFAULT_HOME_KICKER }) }) {
                Icon(Icons.TwoTone.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
