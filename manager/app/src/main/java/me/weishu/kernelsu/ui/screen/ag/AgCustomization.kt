package me.weishu.kernelsu.ui.screen.ag

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.component.ImageCropDialog
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.LauncherIconManager
import me.weishu.kernelsu.ui.util.importFontFile
import me.weishu.kernelsu.ui.util.savePickedImage
import java.io.File

/**
 * 定制功能 · 晨曦玻璃（Aurora Glass）版
 * 所有开关与动作接入真实 CustomizationStore / LauncherIconManager。
 */
@Composable
internal fun AgCustomization(onBack: () -> Unit) {
    val custom = CustomizationStore.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingWallpaperCrop by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
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

    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) { importFontFile(context, uri) }
            if (saved == null) {
                Toast.makeText(context, R.string.customization_font_failed, Toast.LENGTH_SHORT).show()
            } else {
                CustomizationStore.setFontPath(saved)
                CustomizationStore.setFontStyle(4)
                Toast.makeText(context, R.string.customization_font_applied, Toast.LENGTH_SHORT).show()
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

    // ── 固定顶栏（毛玻璃，不随内容滚动）──
    Column(
        Modifier
            .fillMaxSize()
            .background(Ag.background())
            .statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xC4F7F6FB))
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AgIconBtn(Icons.AutoMirrored.Rounded.ArrowBack, onClick = onBack, tint = Ag.Tx)
                Spacer(Modifier.width(12.dp))
                Text(
                    buildString { append("定制"); append("功能") },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                    style = TextStyle(brush = Ag.TitleGrad),
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Ag.Line))
        }

        // ── 内容滚动区 ──
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(14.dp))

            // ── 预览机身（设计稿 .prevmock）──
            AgPreviewMock()

            AgGHead("背景壁纸")
            AgCard {
                AgRow(
                    stringResource(R.string.customization_wallpaper_enable),
                    stringResource(R.string.customization_wallpaper_enable_sum),
                    Ag.Teal, Icons.Rounded.Wallpaper,
                    trailing = {
                        AgToggle(
                            checked = custom.wallpaperEnabled,
                            onCheckedChange = { CustomizationStore.setWallpaperEnabled(!custom.wallpaperEnabled) },
                            small = true,
                        )
                    },
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AgPillAction(
                        text = stringResource(R.string.customization_pick_image),
                        icon = Icons.Rounded.Palette,
                        primary = true,
                        modifier = Modifier.weight(1f),
                    ) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    if (custom.wallpaperUri != null) {
                        AgPillAction(
                            text = stringResource(R.string.customization_clear),
                            icon = Icons.Rounded.DeleteOutline,
                            primary = false,
                            modifier = Modifier.weight(1f),
                        ) { CustomizationStore.setWallpaper(null) }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            AgGHead("标题栏")
            AgCard {
                Text(
                    stringResource(R.string.customization_title_bar),
                    fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Ag.Tx,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.customization_title_bar_sub),
                    fontSize = 12.5.sp, color = Ag.Tx2,
                )
                Spacer(Modifier.height(10.dp))
                AgTextField(
                    value = custom.homeTitle,
                    onValueChange = CustomizationStore::setHomeTitle,
                    hint = stringResource(R.string.customization_title_placeholder),
                )
                Spacer(Modifier.height(6.dp))
            }

            AgGHead("字体")
            AgCard {
                AgRow(
                    stringResource(R.string.customization_font),
                    stringResource(R.string.customization_font_sub),
                    Ag.Vio, Icons.Rounded.TextFields,
                )
                Spacer(Modifier.height(2.dp))
                AgFontOption(
                    name = stringResource(R.string.customization_font_system),
                    sample = CUST_FONT_SAMPLE, selected = custom.fontStyle == 0, fontFamily = null,
                ) { CustomizationStore.setFontStyle(0) }
                AgFontOption(
                    name = stringResource(R.string.customization_font_name_serif),
                    sample = CUST_FONT_SAMPLE, selected = custom.fontStyle == 1,
                    fontFamily = FontFamily(Font(R.font.noto_serif_sc)),
                ) { CustomizationStore.setFontStyle(1) }
                AgFontOption(
                    name = stringResource(R.string.customization_font_name_mono),
                    sample = CUST_FONT_SAMPLE, selected = custom.fontStyle == 2,
                    fontFamily = FontFamily(Font(R.font.jetbrains_mono)),
                ) { CustomizationStore.setFontStyle(2) }
                AgFontOption(
                    name = stringResource(R.string.customization_font_name_wenkai),
                    sample = CUST_FONT_SAMPLE, selected = custom.fontStyle == 3,
                    fontFamily = FontFamily(Font(R.font.lxgw_wenkai)),
                ) { CustomizationStore.setFontStyle(3) }
                if (custom.fontPath != null) {
                    AgFontOption(
                        name = stringResource(R.string.customization_font_custom),
                        sample = File(custom.fontPath).name,
                        selected = custom.fontStyle == 4,
                        fontFamily = runCatching { FontFamily(Font(File(custom.fontPath))) }.getOrNull(),
                    ) { CustomizationStore.setFontStyle(4) }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AgPillAction(
                        text = stringResource(R.string.customization_font_import),
                        icon = Icons.Rounded.FormatSize,
                        primary = custom.fontPath == null,
                        modifier = Modifier.weight(1f),
                    ) {
                        fontPicker.launch(
                            arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/octet-stream")
                        )
                    }
                    if (custom.fontPath != null) {
                        AgPillAction(
                            text = stringResource(R.string.customization_clear),
                            icon = Icons.Rounded.DeleteOutline,
                            primary = false,
                            modifier = Modifier.weight(1f),
                        ) {
                            CustomizationStore.setFontPath(null)
                            if (custom.fontStyle == 4) CustomizationStore.setFontStyle(0)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            AgGHead("底栏")
            AgCard {
                AgRow(
                    stringResource(R.string.customization_bottom_bar),
                    stringResource(R.string.customization_bottom_bar_sub),
                    Ag.Mag, Icons.Rounded.SmartButton,
                )
                Spacer(Modifier.height(10.dp))
                // 形态选择：胶囊 / 抽屉（真实切换 bottomBarStyle）
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AgModeSeg("胶囊", selected = custom.bottomBarStyle == 0, modifier = Modifier.weight(1f)) {
                        CustomizationStore.setBottomBarStyle(0)
                    }
                    AgModeSeg("抽屉", selected = custom.bottomBarStyle == 1, modifier = Modifier.weight(1f)) {
                        CustomizationStore.setBottomBarStyle(1)
                    }
                }
                Spacer(Modifier.height(4.dp))
                AgRow(
                    stringResource(R.string.customization_bottom_immersive),
                    stringResource(R.string.customization_bottom_immersive_sum),
                    Ag.Teal, Icons.Rounded.Layers,
                    trailing = {
                        AgToggle(
                            checked = custom.bottomBarImmersive,
                            onCheckedChange = { CustomizationStore.setBottomBarImmersive(!custom.bottomBarImmersive) },
                            small = true,
                        )
                    },
                )
                AgRow(
                    stringResource(R.string.customization_bottom_liquid_glass),
                    stringResource(R.string.customization_bottom_liquid_glass_sum),
                    Ag.Vio, Icons.Rounded.Layers,
                    trailing = {
                        AgToggle(
                            checked = custom.bottomBarLiquidGlass,
                            onCheckedChange = { CustomizationStore.setBottomBarLiquidGlass(!custom.bottomBarLiquidGlass) },
                            small = true,
                        )
                    },
                )
                AgRow(
                    stringResource(R.string.customization_nav_rainbow),
                    stringResource(R.string.customization_nav_rainbow_sum),
                    Ag.Mag, Icons.Rounded.Palette,
                    trailing = {
                        AgToggle(
                            checked = custom.navRainbow,
                            onCheckedChange = { CustomizationStore.setNavRainbow(!custom.navRainbow) },
                            small = true,
                        )
                    },
                )
                AgRow(
                    stringResource(R.string.customization_nav_monet),
                    stringResource(R.string.customization_nav_monet_sum),
                    Ag.Amber, Icons.Rounded.Palette,
                    trailing = {
                        AgToggle(
                            checked = custom.navMonet,
                            onCheckedChange = { CustomizationStore.setNavMonet(!custom.navMonet) },
                            small = true,
                        )
                    },
                )
                AgSliderRow(
                    label = stringResource(R.string.customization_bottom_alpha),
                    value = custom.bottomBarAlpha,
                    range = 0.2f..1f,
                    display = "${(custom.bottomBarAlpha * 100).toInt()}%",
                    onChange = CustomizationStore::setBottomBarAlpha,
                )
                if (custom.bottomBarStyle == 1) {
                    AgSliderRow(
                        label = stringResource(R.string.customization_radius),
                        value = custom.bottomBarRadius,
                        range = 0f..100f,
                        display = "${custom.bottomBarRadius.toInt()}dp",
                        onChange = CustomizationStore::setBottomBarRadius,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            AgGHead("启动器图标")
            AgCard {
                AgRow(
                    stringResource(R.string.customization_icon),
                    stringResource(R.string.customization_icon_hint),
                    Ag.Amber, Icons.Rounded.Refresh,
                )
                Spacer(Modifier.height(12.dp))
                val iconChoices = listOf(
                    Triple(
                        LauncherIconManager.STYLE_FOXSU to R.string.customization_icon_foxsu,
                        R.drawable.ic_launcher_foreground,
                        Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F1EE))),
                    ),
                    Triple(
                        LauncherIconManager.STYLE_FOX2 to R.string.customization_icon_fox2,
                        R.drawable.ic_launcher_fox2_foreground,
                        Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF6F1FF))),
                    ),
                    Triple(
                        LauncherIconManager.STYLE_FOX3 to R.string.customization_icon_fox3,
                        R.drawable.ic_launcher_fox3_foreground,
                        Brush.linearGradient(listOf(Color(0xFF1C1C21), Color(0xFF08080A))),
                    ),
                    Triple(
                        LauncherIconManager.STYLE_FOX4 to R.string.customization_icon_fox4,
                        R.drawable.ic_launcher_fox4_foreground,
                        Brush.linearGradient(listOf(Color(0xFF191922), Color(0xFF08080A))),
                    ),
                    Triple(
                        LauncherIconManager.STYLE_KERNELSU to R.string.customization_icon_kernelsu,
                        R.drawable.ic_launcher_kernelsu_foreground,
                        Brush.linearGradient(listOf(Color.White, Color.White)),
                    ),
                )
                iconChoices.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { (first, iconRes, bg) ->
                            val (stylePair, labelRes) = first
                            AgIconChip(
                                modifier = Modifier.weight(1f),
                                label = stringResource(labelRes),
                                selected = custom.iconStyle == stylePair,
                                background = bg,
                                iconRes = iconRes,
                            ) {
                                CustomizationStore.setIconStyle(stylePair)
                                LauncherIconManager.apply(stylePair)
                                Toast.makeText(context, R.string.customization_icon_hint, Toast.LENGTH_SHORT).show()
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                // 七彩狐狸快捷开关（与上面图标选择共享同一持久化状态）
                AgRow(
                    stringResource(R.string.customization_icon_rainbow),
                    stringResource(R.string.customization_icon_rainbow_sum),
                    Ag.Mag, Icons.Rounded.Palette,
                    trailing = {
                        AgToggle(
                            checked = custom.iconStyle == LauncherIconManager.STYLE_FOX2,
                            onCheckedChange = {
                                val style = if (custom.iconStyle == LauncherIconManager.STYLE_FOX2) {
                                    LauncherIconManager.STYLE_FOXSU
                                } else {
                                    LauncherIconManager.STYLE_FOX2
                                }
                                CustomizationStore.setIconStyle(style)
                                LauncherIconManager.apply(style)
                                Toast.makeText(context, R.string.customization_icon_hint, Toast.LENGTH_SHORT).show()
                            },
                            small = true,
                        )
                    },
                )
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(8.dp))
            AgButton(
                primary = false,
                text = stringResource(R.string.customization_reset),
                onClick = {
                    CustomizationStore.reset()
                    LauncherIconManager.apply(LauncherIconManager.STYLE_FOXSU)
                },
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** 预览机身（设计稿主题设置页 .prevmock：FoxSu 名 + 渐变卡 + 高卡 + Dock 圆点条） */
@Composable
private fun AgPreviewMock() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(200.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Ag.CardSolid)
                .border(1.dp, Ag.Line, RoundedCornerShape(26.dp))
                .padding(14.dp, 12.dp, 14.dp, 10.dp),
        ) {
            Text("FoxSu", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ag.Tx)
            Spacer(Modifier.height(8.dp))
            // 渐变小卡
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0x526C5CE7), Color(0x4DEC4899)))
                    ),
            )
            Spacer(Modifier.height(8.dp))
            // 高卡
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x0B1E1946)),
            )
            Spacer(Modifier.height(10.dp))
            // Dock 圆点条
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0x0B1E1946))
                    .padding(7.dp, 7.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Box(Modifier.size(15.dp).clip(RoundedCornerShape(5.dp)).background(Ag.Grad))
                Box(Modifier.size(15.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF1E2230)))
                Box(Modifier.size(15.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF4C5566)))
                Box(Modifier.size(15.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF8A93A6)))
            }
        }
    }
}

/** 胶囊动作按钮（渐变实底 / 次级描边） */
@Composable
private fun AgPillAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier
            .height(38.dp)
            .agClick(onClick = onClick)
            .then(
                if (primary) {
                    Modifier
                        .clip(shape)
                        .background(Ag.Grad, shape)
                } else {
                    Modifier
                        .background(Color(0x126C5CE7), shape)
                        .border(1.dp, Color(0x406C5CE7), shape)
                }
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (primary) Color.White else Ag.Vio, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (primary) Color.White else Ag.Vio,
            maxLines = 1,
        )
    }
}

/** 输入框（晨曦玻璃：14dp 圆角 + 细描边，聚焦变紫） */
@Composable
private fun AgTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
) {
    var focused by remember { mutableStateOf(false) }
    val border = if (focused) Color(0x996C5CE7) else Ag.Line
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Ag.Tx,
        ),
        cursorBrush = Brush.verticalGradient(listOf(Ag.Vio, Ag.Mag)),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x0B1E1946))
                    .border(1.dp, border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (value.isEmpty()) {
                    Text(hint, fontSize = 13.sp, color = Ag.Tx3)
                }
                inner()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        interactionSource = interaction,
    )
}

/** 字体选项卡：名称 + 实时字体渲染样张 */
@Composable
private fun AgFontOption(
    name: String,
    sample: String,
    selected: Boolean,
    fontFamily: FontFamily?,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .agClick(onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0x146C5CE7) else Color.Transparent)
            .border(
                1.dp,
                if (selected) Color(0x666C5CE7) else Ag.Line,
                RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ag.Tx)
            Spacer(Modifier.height(2.dp))
            Text(
                sample,
                fontSize = 12.sp,
                color = Ag.Tx2,
                fontFamily = fontFamily,
                maxLines = 1,
            )
        }
        if (selected) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Ag.Grad),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Refresh, null,
                    tint = Color.White, modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

/** 形态分段按钮（.msegb） */
@Composable
private fun AgModeSeg(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(42.dp)
            .agClick(onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0x1A6C5CE7) else Ag.CardSolid)
            .border(
                1.dp,
                if (selected) Color(0x4D6C5CE7) else Ag.Line,
                RoundedCornerShape(14.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Ag.Vio else Ag.Tx2,
        )
    }
}

/** 滑杆行（.slabel + 渐变轨道） */
@Composable
private fun AgSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ag.Tx, modifier = Modifier.weight(1f))
            Text(display, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ag.Vio)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Ag.Vio,
                inactiveTrackColor = Color(0xFFECECF2),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

/** 启动器图标选择块 */
@Composable
private fun AgIconChip(
    label: String,
    selected: Boolean,
    background: Brush,
    iconRes: Int,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
) {
    Column(
        modifier
            .agClick(onClick = onSelect)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0x146C5CE7) else Color.Transparent)
            .border(
                1.dp,
                if (selected) Color(0x666C5CE7) else Ag.Line,
                RoundedCornerShape(16.dp),
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(background)
                .border(
                    1.dp,
                    if (selected) Color(0xFF6C5CE7) else Ag.Line,
                    RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(38.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = if (selected) Ag.Vio else Ag.Tx2, maxLines = 1)
    }
}

/** 字体样张文案 */
private const val CUST_FONT_SAMPLE = "FoxSu 定制功能 123 ABC"
