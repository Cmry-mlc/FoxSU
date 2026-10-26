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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
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
import me.weishu.kernelsu.ui.component.GlassSheen
import me.weishu.kernelsu.ui.component.cardContentColor
import me.weishu.kernelsu.ui.component.freshCardColor
import me.weishu.kernelsu.ui.component.isNightScheme
import me.weishu.kernelsu.ui.component.rememberCardSurface
import me.weishu.kernelsu.ui.component.ImageCropDialog
import me.weishu.kernelsu.ui.component.scaffoldBackgroundColor
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.LauncherIconManager
import me.weishu.kernelsu.ui.util.importFontFile
import me.weishu.kernelsu.ui.util.savePickedImage
import java.io.File

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

    val fontPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
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

    Scaffold(
        containerColor = scaffoldBackgroundColor(MaterialTheme.colorScheme.surface),
        topBar = {
            // 设计稿 .phead：40dp 白圆返回钮（带投影）+ 24/800 标题，padding 20x(20/10)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(if (isNightScheme()) 0.dp else 2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(freshCardColor())
                        .clickable(onClick = dropUnlessResumed { navigator.pop() }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = LocalContentColor.current,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.customization_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = cardContentColor(),
                )
            }
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
                        accent = Color(0xFF3E7A4C),
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
                        accent = Color(0xFF9A7A1E),
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
                                color = LocalContentColor.current,
                            )
                            Text(
                                text = stringResource(R.string.customization_glass_wallpaper_sum),
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalContentColor.current.copy(alpha = 0.62f),
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
                        accent = Color(0xFF6C5FC7),
                        icon = Icons.Filled.Title,
                        title = stringResource(R.string.customization_title_bar),
                        subtitle = stringResource(R.string.customization_title_bar_sub),
                    ) {
                        // 设计稿：1.5px #e4e2ee 描边、圆角14、13px 字号、占位用 sub 色
                        OutlinedTextField(
                            value = custom.homeTitle,
                            onValueChange = CustomizationStore::setHomeTitle,
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.customization_title_placeholder),
                                    fontSize = 13.sp,
                                    color = LocalContentColor.current.copy(alpha = 0.5f),
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = freshAccent(),
                                unfocusedBorderColor = if (isNightScheme()) Color(0xFF3A3A48) else Color(0xFFE4E2EE),
                                focusedTextColor = LocalContentColor.current,
                                unfocusedTextColor = LocalContentColor.current,
                                cursorColor = freshAccent(),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item {
                    SectionCard(
                        accent = Color(0xFF3E7A4C),
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
                            if (custom.fontPath != null) {
                                item {
                                    StyleChip(
                                        label = stringResource(R.string.customization_font_custom),
                                        selected = custom.fontStyle == 4,
                                        onClick = { CustomizationStore.setFontStyle(4) },
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton(
                                text = stringResource(R.string.customization_font_import),
                                icon = Icons.Filled.TextFields,
                                filled = custom.fontPath == null,
                                onClick = {
                                    fontPicker.launch(
                                        arrayOf(
                                            "font/ttf",
                                            "font/otf",
                                            "application/x-font-ttf",
                                            "application/octet-stream",
                                        )
                                    )
                                },
                            )
                            if (custom.fontPath != null) {
                                PillButton(
                                    text = stringResource(R.string.customization_clear),
                                    icon = Icons.Filled.DeleteOutline,
                                    filled = false,
                                    onClick = {
                                        CustomizationStore.setFontPath(null)
                                        if (custom.fontStyle == 4) CustomizationStore.setFontStyle(0)
                                    },
                                )
                            }
                        }
                        if (custom.fontPath != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = File(custom.fontPath).name +
                                    if (custom.fontStyle == 4) "" else stringResource(R.string.customization_font_idle_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalContentColor.current.copy(alpha = 0.62f),
                            )
                        }
                    }
                }

                item {
                    SectionCard(
                        accent = Color(0xFF3F6AA6),
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
                        ToggleRow(
                            title = stringResource(R.string.customization_bottom_immersive),
                            subtitle = stringResource(R.string.customization_bottom_immersive_sum),
                            checked = custom.bottomBarImmersive,
                            onChange = CustomizationStore::setBottomBarImmersive,
                        )

                        ToggleRow(
                            title = stringResource(R.string.customization_bottom_liquid_glass),
                            subtitle = stringResource(R.string.customization_bottom_liquid_glass_sum),
                            checked = custom.bottomBarLiquidGlass,
                            onChange = CustomizationStore::setBottomBarLiquidGlass,
                        )

                        ToggleRow(
                            title = stringResource(R.string.customization_nav_rainbow),
                            subtitle = stringResource(R.string.customization_nav_rainbow_sum),
                            checked = custom.navRainbow,
                            onChange = CustomizationStore::setNavRainbow,
                        )

                        ToggleRow(
                            title = stringResource(R.string.customization_nav_monet),
                            subtitle = stringResource(R.string.customization_nav_monet_sum),
                            checked = custom.navMonet,
                            onChange = CustomizationStore::setNavMonet,
                        )

                        SliderRow(
                            label = stringResource(R.string.customization_bottom_alpha),
                            value = custom.bottomBarAlpha,
                            range = 0.2f..1f,
                            display = "${(custom.bottomBarAlpha * 100).toInt()}%",
                            onChange = CustomizationStore::setBottomBarAlpha,
                        )

                        // 胶囊形态固定成药丸（全圆角），只有抽屉形态才开放圆角
                        if (custom.bottomBarStyle == 1) {
                            SliderRow(
                                label = stringResource(R.string.customization_radius),
                                value = custom.bottomBarRadius,
                                range = 0f..100f,
                                display = "${custom.bottomBarRadius.toInt()}dp",
                                onChange = CustomizationStore::setBottomBarRadius,
                            )
                        }
                    }
                }

                item {
                    SectionCard(
                        accent = Color(0xFFB0508A),
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

/** 设计稿薄荷主色（浅 #3E7A4C / 夜 #8FD6A4），替代主题绿 primary。 */
@Composable
private fun freshAccent(): Color = if (isNightScheme()) Color(0xFF8FD6A4) else Color(0xFF3E7A4C)

@Composable
private fun freshAccentContainer(): Color = if (isNightScheme()) Color(0xFF1F2F26) else Color(0xFFDDEEDC)

@Composable
private fun freshAccentContent(): Color = if (isNightScheme()) Color(0xFF1F2F26) else Color.White

@Composable
private fun PreviewCard(custom: Customization) {
    // 设计稿：26 圆角容器，薄荷渐变 135° #ddefda → #bfe0c9，padding 30x22
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (isNightScheme()) {
                    Brush.linearGradient(listOf(Color(0xFF20301F), Color(0xFF2C3F2E)))
                } else {
                    Brush.linearGradient(listOf(Color(0xFFDDEFDA), Color(0xFFBFE0C9)))
                }
            )
            .padding(horizontal = 22.dp, vertical = 30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 白卡：圆角20、padding 16x22、0 6 18 投影
            Column(
                modifier = Modifier
                    .shadow(if (isNightScheme()) 0.dp else 6.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isNightScheme()) Color(0xFF2A2A36) else Color.White)
                    .padding(horizontal = 22.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = freshAccent(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.customization_preview_hint),
                    fontSize = 10.5.sp,
                    color = if (isNightScheme()) Color(0xFF9A97AB) else Color(0xFF8E8B9C),
                )
            }
            // 白胶囊：圆角99、padding 12x20，内嵌 LKM 徽章 + 工作模式
            Row(
                modifier = Modifier
                    .shadow(if (isNightScheme()) 0.dp else 6.dp, RoundedCornerShape(99.dp))
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (isNightScheme()) Color(0xFF2A2A36) else Color.White)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "LKM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = freshAccent(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isNightScheme()) Color(0xFF1F2F26) else Color(0xFFDDEEDC)
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.customization_preview_card),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isNightScheme()) Color(0xFFECEAF2) else Color(0xFF2B2B33),
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color = Color(0xFF3E7A4C),
    content: @Composable () -> Unit,
) {
    // 卡片自身决定文字明暗：白卡深字、深卡白字，避免白卡白字
    val cardContent = cardContentColor()
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = rememberCardSurface(freshCardColor()),
        contentColor = cardContent,
        border = freshCardBorder(),
        shadowElevation = if (isNightScheme()) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            GlassSheen(Modifier.matchParentSize())
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 设计稿 .whead .chip：40dp 白底、圆角13、带 0 2px 6px rgba(60,55,90,.10) 投影
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(if (isNightScheme()) 0.dp else 2.dp, RoundedCornerShape(13.dp))
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = if (isNightScheme()) 0.10f else 1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LocalContentColor.current,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        color = LocalContentColor.current.copy(alpha = 0.55f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
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
    // 设计稿 .slabel（12/700 左右分栏，% 薄荷色）+ .slider（10px 高圆角99 轨道）
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LocalContentColor.current,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = display,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = freshAccent(),
            )
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
                activeTrackColor = freshAccent(),
                inactiveTrackColor = if (isNightScheme()) Color(0xFF2A2A36) else Color(0xFFECECF2),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
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
        filled -> freshAccent()
        else -> freshAccentContainer().copy(alpha = 0.6f)
    }
    val content = when {
        !enabled -> MaterialTheme.colorScheme.outline
        filled -> freshAccentContent()
        else -> freshAccent()
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
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StyleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            freshAccent()
        } else {
            if (isNightScheme()) Color(0xFF24242E) else Color(0xFFECECF2)
        },
        contentColor = if (selected) {
            freshAccentContent()
        } else {
            if (isNightScheme()) Color(0xFFB5B2C2) else Color(0xFF55525F)
        },
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
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
            freshAccentContainer()
        } else {
            if (isNightScheme()) Color(0xFF24242E) else Color.White
        },
        border = if (selected) {
            BorderStroke(1.5.dp, freshAccent())
        } else {
            BorderStroke(1.dp, if (isNightScheme()) Color(0xFF3A3A48) else Color(0xFFEDEBF3))
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
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) freshAccent() else LocalContentColor.current,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = freshAccent(),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** 通用开关行：标题 + 说明 + 右侧开关。 */
/** 设计稿 .rowline：开关行，上边 1px 分隔线，标题 13.5/700，说明 10.5 sub 色。 */
@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = if (isNightScheme()) Color(0xFF2A2A36) else Color(0xFFF2F0F6))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalContentColor.current,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    color = LocalContentColor.current.copy(alpha = 0.55f),
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

/** 启动器图标选项：设计稿 18 圆角、选中薄荷底 + 2dp 薄荷描边、40dp 白底圆角14 图标块。 */
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
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            if (isNightScheme()) Color(0xFF1F2F26) else Color(0xFFDDEEDC)
        } else {
            freshCardColor()
        },
        contentColor = cardContentColor(),
        border = if (selected) {
            BorderStroke(2.dp, freshAccent())
        } else {
            BorderStroke(1.5.dp, if (isNightScheme()) Color(0xFF2F2F3C) else Color(0xFFECECF2))
        },
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(if (isNightScheme()) 0.dp else 2.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(background),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) {
                    freshAccent()
                } else {
                    LocalContentColor.current
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
