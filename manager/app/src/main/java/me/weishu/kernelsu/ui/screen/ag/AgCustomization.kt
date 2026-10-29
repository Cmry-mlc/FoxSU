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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
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
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.component.ImageCropDialog
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.screen.settings.SettingsScreenActions
import me.weishu.kernelsu.ui.util.LauncherIconManager
import me.weishu.kernelsu.ui.util.importFontFile
import me.weishu.kernelsu.ui.theme.AccentPalettes
import me.weishu.kernelsu.ui.util.savePickedImage
import java.io.File

/**
 * Dock 样式定义：设计稿 9 种 + 由其他皮肤原生底栏复刻的 4 种。
 * 复刻项让「换肤」不再改变操作习惯——样式改为全局可选。
 */
private val dockStyles = listOf(
    "d0" to "原有胶囊", "v1" to "展开药丸", "v5" to "侧边抽屉",
    "v6" to "流光光标", "v7" to "描边霓虹", "v8" to "方块卡片",
    "v9" to "渐变实底", "v10" to "顶指示条", "v11" to "浮起圆片",
    "editorial" to "纸面墨线", "pages" to "白色药丸",
    "glass" to "液态药丸", "nocturne" to "暗夜霓虹",
)

/**
 * 定制功能 · 晨曦玻璃（Aurora Glass）— 按最终设计稿 p-custom 全量重构。
 * 所有开关/滑杆接真实设置（SettingsViewModel actions + CustomizationStore）。
 */
@Composable
internal fun AgCustomization(
    onBack: () -> Unit,
    settingsActions: SettingsScreenActions? = null,
) {
    val custom = CustomizationStore.current
    val repo = remember { SettingsRepositoryImpl() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingWallpaperCrop by remember { mutableStateOf<String?>(null) }
    var wallpaperOn by remember { mutableStateOf(custom.wallpaperEnabled) }

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
                wallpaperOn = true
                pendingWallpaperCrop = null
            },
            onDismiss = { pendingWallpaperCrop = null },
        )
    }

    // ── 固定顶栏（透明 + 无分隔线）──
    Column(
        Modifier
            .fillMaxSize()
            .agPageBg()
            .statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Ag.headSurface()).agHeadBlur()
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AgIconBtn(AgTopIcons.Back, onClick = onBack, tint = Ag.Tx)
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── 内容滚动区 ──
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            // 大标题（设计稿 margin-top:22px）
            Text(
                buildString { append("定制"); append("功能") },
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.6).sp,
                style = androidx.compose.ui.text.TextStyle(brush = Ag.TitleGrad),
                modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 16.dp),
            )

            // ── 狐狸 Logo 头卡（.foxhero：280dp 渐变卡，狐狸完整居中，fox-hero.png 透明底）──
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (Ag.isDark) {
                            Brush.verticalGradient(listOf(Color(0xFF2B2444), Color(0xFF3D2C52), Color(0xFF4A2F55)))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xFFEAE4FF), Color(0xFFF6E7F8), Color(0xFFFFE7EE)))
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.fox_hero),
                    contentDescription = "FoxSu",
                    modifier = Modifier
                        .height(240.dp)
                        .shadow(12.dp, RoundedCornerShape(40.dp), spotColor = Color(0x66B48CFF), ambientColor = Color(0x66B48CFF)),
                )
            }

            // ══════════ 全局强调色 ══════════
            AgGHead("全局强调色")
            AgCard {
                AgSubLabel("配色（取自 FoxSu logo 的七彩）")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(AccentPalettes.size) { i ->
                        item {
                            AgAccentChip(
                                palette = AccentPalettes[i],
                                selected = custom.accentPalette == i,
                            ) { CustomizationStore.setAccentPalette(i) }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "选一套主色，按钮、选中态、底栏指示器与渐变会同时换色，五套界面风格一并生效。",
                    fontSize = 11.5.sp, lineHeight = 17.sp, color = Ag.Tx2,
                )
                Spacer(Modifier.height(12.dp))
            }

            // ══════════ 标题栏 ══════════
            AgGHead("标题栏")
            AgCard {
                AgSubLabel("标题文字")
                AgTitleField(
                    value = custom.homeTitle,
                    onValueChange = CustomizationStore::setHomeTitle,
                    hint = "留空则显示 FoxSu",
                )
                if (custom.homeTitle.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "恢复默认",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ag.Vio,
                        modifier = Modifier
                            .agClick { CustomizationStore.setHomeTitle("") }
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))
                AgSubLabel("标题颜色")
                AgTitleColorRow(
                    selected = custom.titleColor,
                    onPick = CustomizationStore::setTitleColor,
                )

                // 自定义取色：色相 / 饱和度 / 明度三条滑轨，拖动即实时生效
                var showTitlePicker by remember { mutableStateOf(false) }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (showTitlePicker) "收起取色器" else "自定义颜色…",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ag.Vio,
                        modifier = Modifier
                            .agClick { showTitlePicker = !showTitlePicker }
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                    )
                }
                if (showTitlePicker) {
                    AgTitlePicker(
                        initial = custom.titleColor,
                        onPick = CustomizationStore::setTitleColor,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "当前：${hexOf(custom.titleColor)}　·　五套界面风格同时生效",
                        fontSize = 11.sp,
                        color = Ag.Tx2,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            // ══════════ 显示与效果 ══════════
            AgGHead("显示与效果")
            AgCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                AgToggleRow(
                    "模糊", "启用顶栏和底栏的模糊效果",
                    Ag.Vio, AgSettingsIcons.Blur,
                    trailing = {
                        var on by remember { mutableStateOf(repo.enableBlur) }
                        AgToggle(checked = on, onCheckedChange = {
                            on = !on; settingsActions?.onSetEnableBlur?.invoke(on)
                        }, small = true)
                    },
                )
                AgToggleRow(
                    "导航栏角标", "在导航栏显示已授权应用和已启用模块数量",
                    Ag.Amber, AgSettingsIcons.Badge,
                    trailing = {
                        var on by remember { mutableStateOf(repo.enableNavigationBadge) }
                        AgToggle(checked = on, onCheckedChange = {
                            on = !on; settingsActions?.onSetEnableNavigationBadge?.invoke(on)
                        }, small = true)
                    },
                )
                AgSliderRow(
                    label = "界面缩放",
                    sub = "调整全局显示比例",
                    value = repo.pageScale,
                    range = 0.7f..1.3f,
                    display = "${(repo.pageScale * 100).toInt()}%",
                    onChange = { settingsActions?.onSetPageScale?.invoke(it) },
                )
            }

            // ══════════ 手势操作 ══════════
            AgGHead("手势操作")
            AgCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                AgToggleRow(
                    "预测性返回手势", "启用对预测性返回手势的支持",
                    Ag.Vio, AgSettingsIcons.PredictiveBack,
                    trailing = {
                        var on by remember { mutableStateOf(repo.enablePredictiveBack) }
                        AgToggle(checked = on, onCheckedChange = {
                            on = !on; settingsActions?.onSetEnablePredictiveBack?.invoke(on)
                        }, small = true)
                    },
                )
                AgToggleRow(
                    "横移返回手势", "从屏幕边缘横移返回上一页",
                    Ag.Mag, AgSettingsIcons.SwipeBack,
                    trailing = {
                        var on by remember { mutableStateOf(repo.enableSwipeDismiss) }
                        AgToggle(checked = on, onCheckedChange = {
                            on = !on; settingsActions?.onSetEnableSwipeDismiss?.invoke(on)
                        }, small = true)
                    },
                )
                AgToggleRow(
                    "页面切换手势", "切换页面时的手势拦截方式",
                    Ag.Teal, AgSettingsIcons.PagerGesture,
                    trailing = {
                        var mode by remember { mutableStateOf(repo.pagerInterceptionMode) }
                        AgPick(
                            text = when (mode) {
                                1 -> "跨轴拦截"; 2 -> "仅边缘"; else -> "无拦截"
                            } + " ⇅",
                            onClick = {
                                val next = (mode + 1) % 3
                                mode = next
                                settingsActions?.onSetPagerInterceptionMode?.invoke(next)
                            },
                        )
                    },
                )
            }

            // ══════════ 背景壁纸 ══════════
            AgGHead("背景壁纸")
            AgCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                AgRow(
                    stringResource(R.string.customization_wallpaper_enable),
                    stringResource(R.string.customization_wallpaper_enable_sum),
                    Ag.Teal, AgSettingsIcons.Wallpaper,
                    trailing = {
                        AgToggle(checked = wallpaperOn, onCheckedChange = {
                            wallpaperOn = !wallpaperOn
                            CustomizationStore.setWallpaperEnabled(wallpaperOn)
                        }, small = true)
                    },
                )
                Row(modifier = Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AgPillAction(
                        text = stringResource(R.string.customization_pick_image),
                        icon = AgSettingsIcons.Wallpaper,
                        primary = true,
                        modifier = Modifier.weight(1f),
                    ) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    if (custom.wallpaperUri != null) {
                        AgPillAction(
                            text = stringResource(R.string.customization_clear),
                            icon = Icons.Rounded.Refresh,
                            primary = false,
                            modifier = Modifier.weight(1f),
                        ) { CustomizationStore.setWallpaper(null) }
                    }
                }
            }

            // ══════════ 字体 ══════════
            AgGHead("字体")
            AgCard {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    item {
                        AgFontChip("思源宋体", "字A", FontFamily(Font(R.font.noto_serif_sc)), custom.fontStyle == 1) {
                            CustomizationStore.setFontStyle(1)
                        }
                    }
                    item {
                        AgFontChip("等宽字体", "字A", FontFamily(Font(R.font.jetbrains_mono)), custom.fontStyle == 2) {
                            CustomizationStore.setFontStyle(2)
                        }
                    }
                    item {
                        AgFontChip("霞鹜文楷", "字A", FontFamily(Font(R.font.lxgw_wenkai)), custom.fontStyle == 3) {
                            CustomizationStore.setFontStyle(3)
                        }
                    }
                    if (custom.fontPath != null) {
                        item {
                            AgFontChip(
                                "自定义", File(custom.fontPath).name.take(2),
                                runCatching { FontFamily(Font(File(custom.fontPath))) }.getOrNull(),
                                custom.fontStyle == 4,
                            ) { CustomizationStore.setFontStyle(4) }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                AgButton(
                    primary = true,
                    text = "导入字体文件",
                    onClick = {
                        fontPicker.launch(
                            arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/octet-stream")
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
            }

            // ══════════ 应用图标 ══════════
            AgGHead("应用图标")
            AgCard {
                AgSubLabel("图标风格")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    val geoItems = listOf(
                        "圆角方" to 9f, "正圆" to -1f, "超椭圆" to 12f, "七彩" to 9f,
                    )
                    // 几何风格：作用于 Dock 图标形状（用 iconStyle 持久化 100+）
                    val curShape = custom.iconStyle.takeIf { it in 100..103 } ?: 100
                    geoItems.forEachIndexed { i, (name, radius) ->
                        val code = 100 + i
                        item {
                            AgIconCard(
                                name = name,
                                shapeRadius = radius,
                                rainbow = i == 3,
                                selected = curShape == code,
                            ) {
                                CustomizationStore.setIconStyle(code)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                AgSubLabel("启动器图标")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    val launcherItems = listOf(
                        Triple("默认", LauncherIconManager.STYLE_FOXSU, R.drawable.ic_fox_global),
                        Triple("乖巧", LauncherIconManager.STYLE_FOX2, R.drawable.ic_launcher_fox2_foreground),
                        Triple("招手", LauncherIconManager.STYLE_FOX3, R.drawable.ic_launcher_fox3_foreground),
                        Triple("回眸", LauncherIconManager.STYLE_FOX4, R.drawable.ic_launcher_fox4_foreground),
                    )
                    launcherItems.forEach { (name, style, res) ->
                        item {
                            AgLauncherIconCard(
                                name = name,
                                iconRes = res,
                                selected = custom.launcherIcon == style,
                            ) {
                                CustomizationStore.setLauncherIcon(style)
                                LauncherIconManager.apply(style)
                                Toast.makeText(context, R.string.customization_icon_hint, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // ══════════ 底部导航栏 ══════════
            AgGHead("底部导航栏")
            AgCard {
                // 2 列样式选择器
                dockStyles.chunked(2).forEach { rowPair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowPair.forEach { (code, name) ->
                            AgDockStyleCard(
                                name = name,
                                code = code,
                                selected = custom.dockStyle == code,
                                modifier = Modifier.weight(1f),
                            ) { CustomizationStore.setDockStyle(code) }
                        }
                        if (rowPair.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
                AgRow(
                    "胶囊停靠", "侧边抽屉样式下，胶囊停靠在屏幕左侧还是右侧",
                    Ag.Vio, AgSettingsIcons.FloatingBar,
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("左", "右").forEachIndexed { i, label ->
                                val on = custom.dockSide == i
                                Box(
                                    Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(if (on) Ag.Grad else androidx.compose.ui.graphics.SolidColor(Color.Transparent))
                                        .border(
                                            1.dp,
                                            if (on) Color.Transparent else Ag.Line,
                                            RoundedCornerShape(99.dp),
                                        )
                                        .agClick { CustomizationStore.setDockSide(i) }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (on) Color.White else Ag.Tx2,
                                    )
                                }
                            }
                        }
                    },
                )
                AgToggleRow(
                    "Editorial 使用胶囊底栏",
                    "关闭时 Editorial 主题保留原生纸面底栏（推荐：胶囊会打乱杂志版式）",
                    Ag.Amber, AgSettingsIcons.FloatingBar,
                    trailing = {
                        AgToggle(checked = custom.dockOnEditorial, onCheckedChange = {
                            CustomizationStore.setDockOnEditorial(!custom.dockOnEditorial)
                        }, small = true)
                    },
                )
                AgToggleRow(
                    "沉浸浅感", "通透半透明底：更透彻并带玻璃质感高光",
                    Ag.Teal, AgSettingsIcons.FloatingBar,
                    trailing = {
                        AgToggle(checked = custom.bottomBarImmersive, onCheckedChange = {
                            CustomizationStore.setBottomBarImmersive(!custom.bottomBarImmersive)
                        }, small = true)
                    },
                )
                AgToggleRow(
                    "底栏液态玻璃", "实时折射的玻璃质感高光",
                    Ag.Vio, AgSettingsIcons.Blur,
                    trailing = {
                        AgToggle(checked = custom.bottomBarLiquidGlass, onCheckedChange = {
                            CustomizationStore.setBottomBarLiquidGlass(!custom.bottomBarLiquidGlass)
                        }, small = true)
                    },
                )
                AgToggleRow(
                    "七彩底栏图标", "给底栏每个图标一套随机彩色",
                    Ag.Mag, Icons.Rounded.Palette,
                    trailing = {
                        AgToggle(checked = custom.navRainbow, onCheckedChange = {
                            CustomizationStore.setNavRainbow(!custom.navRainbow)
                        }, small = true)
                    },
                )
                AgSliderRow(
                    label = "底栏不透明度",
                    value = custom.bottomBarAlpha,
                    range = 0.3f..1f,
                    display = "${(custom.bottomBarAlpha * 100).toInt()}%",
                    onChange = CustomizationStore::setBottomBarAlpha,
                )
                AgSliderRow(
                    label = "圆角",
                    value = custom.bottomBarRadius,
                    range = 12f..40f,
                    display = "${custom.bottomBarRadius.toInt()}px",
                    onChange = CustomizationStore::setBottomBarRadius,
                )
            }

            // ══════════ 内容显示 ══════════
            AgGHead("内容显示")
            AgCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                AgSliderRow(
                    label = "模块描述最大行数",
                    sub = "超过此行数的模块介绍将自动折叠，点击卡片可展开全文",
                    value = repo.moduleDescriptionMaxLines.toFloat(),
                    range = 1f..8f,
                    display = "${repo.moduleDescriptionMaxLines} 行",
                    steps = 6,
                    onChange = { settingsActions?.onSetModuleDescriptionMaxLines?.invoke(it.toInt()) },
                )
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
            Spacer(Modifier.height(140.dp))
        }
    }
}

/** 标题文字输入框：圆角 14、内衬底、与页面同色系的描边 */
@Composable
private fun AgTitleField(value: String, onValueChange: (String) -> Unit, hint: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Ag.searchBg())
            .border(1.dp, Ag.Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                color = Ag.Tx,
                fontWeight = FontWeight.Bold,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Ag.Vio),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(hint, fontSize = 13.sp, color = Ag.Tx3)
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            Text(
                "✕",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Ag.Tx3,
                modifier = Modifier.agClick { onValueChange("") }.padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
}

/** 标题颜色选择：跟随主题 + 7 套品牌色（与「全局强调色」同一份色卡） */
@Composable
private fun AgTitleColorRow(selected: Int, onPick: (Int) -> Unit) {
    val chips = buildList {
        add(Triple("跟随主题", 0, null as Color?))
        AccentPalettes.forEach { p ->
            add(Triple(p.label, p.primary.toArgb(), p.primary as Color?))
        }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        chips.forEach { (label, argb, solid) ->
            item {
            val on = selected == argb
            Column(
                Modifier.agClick { onPick(argb) }.width(58.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(solid?.let { androidx.compose.ui.graphics.SolidColor(it) } ?: Ag.Grad)
                        .border(if (on) 3.dp else 1.dp, if (on) Color.White else Ag.Line, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (on) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    label,
                    fontSize = 10.sp,
                    fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (on) Ag.Vio else Ag.Tx2,
                    maxLines = 1,
                )
            }
            }
        }
    }
}

/** 自定义取色器：色相 / 饱和度 / 明度三轨，任何时候拖动都即时回调 */
@Composable
private fun AgTitlePicker(initial: Int, onPick: (Int) -> Unit) {
    val start = if (initial == 0) 0xFF6C5CE7.toInt() else initial
    // 只在「打开取色器」时取一次初值：若跟着每次回调重建，拖动时滑块会被重置成跳动
    val hsv0 = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(start, it) } }
    var h by remember { mutableStateOf(hsv0[0]) }
    var sat by remember { mutableStateOf(hsv0[1]) }
    var val0 by remember { mutableStateOf(hsv0[2]) }

    fun colorOf(hh: Float, ss: Float, vv: Float): Color =
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hh, ss, vv)))

    fun emit() = onPick(android.graphics.Color.HSVToColor(floatArrayOf(h, sat, val0)))

    Column(Modifier.padding(top = 8.dp)) {
        AgTuneBar(
            brush = Brush.horizontalGradient(
                listOf(
                    colorOf(0f, 1f, 1f), colorOf(60f, 1f, 1f), colorOf(120f, 1f, 1f),
                    colorOf(180f, 1f, 1f), colorOf(240f, 1f, 1f), colorOf(300f, 1f, 1f),
                    colorOf(359.9f, 1f, 1f),
                )
            ),
            frac = h / 360f,
        ) { f -> h = (f * 359.9f).coerceIn(0f, 359.9f); emit() }

        AgTuneBar(
            brush = Brush.horizontalGradient(listOf(colorOf(h, 0f, val0), colorOf(h, 1f, val0))),
            frac = sat,
        ) { f -> sat = f.coerceIn(0f, 1f); emit() }

        AgTuneBar(
            brush = Brush.horizontalGradient(
                listOf(Color.Black, colorOf(h, sat, 1f))
            ),
            frac = val0,
        ) { f -> val0 = f.coerceIn(0f, 1f); emit() }
    }
}

/** 取色滑轨：圆角轨道 + 白色描边圆钮（画在 drawBehind 里，避免 offset 换算） */
@Composable
private fun AgTuneBar(brush: Brush, frac: Float, onChange: (Float) -> Unit) {
    var widthPx by remember { mutableStateOf(1f) }
    val lineColor = Ag.Line
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .height(24.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { off ->
                    onChange((off.x / widthPx).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    onChange((change.position.x / widthPx).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(brush)
                .border(1.dp, Ag.Line, RoundedCornerShape(99.dp))
        )
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .drawBehind {
                    val r = 8.dp.toPx()
                    val cx = (frac * size.width).coerceIn(r, size.width - r)
                    drawCircle(Color.White, r, androidx.compose.ui.geometry.Offset(cx, size.height / 2f))
                    drawCircle(
                        lineColor, r, androidx.compose.ui.geometry.Offset(cx, size.height / 2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()),
                    )
                }
        )
    }
}

/** 取色结果的十六进制文本，#RRGGBB */
private fun hexOf(argb: Int): String =
    if (argb == 0) "#跟随主题" else String.format("#%06X", argb and 0xFFFFFF)

/** 卡片内的小节标题：把同一张卡里的「图标风格 / 启动器图标」分开 */
@Composable
private fun AgSubLabel(text: String) {
    Text(
        text,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Ag.Tx2,
        modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
    )
}

/** .dopt：底栏样式选择卡（2 列网格） */
@Composable
private fun AgDockStyleCard(name: String, code: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .agClick(onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Ag.Vio.copy(alpha = 0.059f) else Color(0x8CFFFFFF))
            .border(
                1.4.dp,
                if (selected) Ag.Vio.copy(alpha = 0.502f) else Ag.Line,
                RoundedCornerShape(16.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        // 预览条：按样式渲染差异化缩略图（设计稿 .dprev .dp 各变体）
        DockStylePreview(code)
        Text(
            name,
            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
            color = if (selected) Ag.Vio else Ag.Tx2,
        )
    }
}

/**
 * 底栏样式缩略预览。
 *
 * 所有样式的预览条尺寸完全一致（同一宽高、同一圆角、同一项间距），
 * 只有「胶囊底色 / 描边 / 指示器造型」随样式变化 —— 和真机上统一后的底栏一一对应。
 */
@Composable
private fun DockStylePreview(code: String) {
    val ac = Ag.accent
    val dark = Ag.isDark
    val grad = Brush.linearGradient(listOf(ac.primary, ac.mid, ac.secondary))
    val dotColor = Ag.Tx3.copy(alpha = 0.45f)
    val pill = RoundedCornerShape(99.dp)

    // 胶囊底 / 描边：只在这里分叉，尺寸一律不参与
    val pillBg: Color = when (code) {
        "editorial" -> if (dark) Color(0xFF1B1A20) else Color(0xFFFFFCF6)
        "pages" -> if (dark) Color(0xFF1B1C1E) else Color.White
        "glass" -> if (dark) Color(0xFF14141E) else Color(0xF2FFFFFF)
        "nocturne" -> Color(0xFF0F1018)
        "v7", "v10" -> Color.Transparent
        else -> if (dark) Color(0xFF1F1E32) else Color.White
    }
    val pillBorder: Color? = when (code) {
        "glass" -> Color(0x33FFFFFF)
        "nocturne" -> Color(0x26FFFFFF)
        "v7" -> ac.primary.copy(alpha = 0.35f)
        "pages" -> if (dark) Color(0x29FFFFFF) else Color(0x14111111)
        else -> null
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Ag.TintInk),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .padding(horizontal = 7.dp)
                .fillMaxWidth()
                .height(23.dp)
                .clip(pill)
                .background(pillBg)
                .then(if (pillBorder != null) Modifier.border(1.dp, pillBorder, pill) else Modifier)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // 选中项小样：全部落在同一个 26×15 的视觉盒子里
            Box(Modifier.size(width = 26.dp, height = 15.dp), contentAlignment = Alignment.Center) {
                when (code) {
                    "v6" -> Box(
                        Modifier.size(14.dp).clip(CircleShape).background(
                            Brush.radialGradient(
                                listOf(ac.primary, ac.primary.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                    )
                    "v7" -> Box(
                        Modifier.size(width = 22.dp, height = 14.dp)
                            .clip(pill).border(1.6.dp, ac.primary.copy(alpha = 0.55f), pill)
                    )
                    "v8" -> Box(
                        Modifier.size(width = 22.dp, height = 14.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (dark) Color(0xFF2A2940) else Color.White)
                            .border(1.dp, Ag.Line, RoundedCornerShape(5.dp))
                    )
                    "v9" -> Box(
                        Modifier.size(width = 22.dp, height = 14.dp)
                            .clip(RoundedCornerShape(5.dp)).background(grad)
                    )
                    "v10" -> Box(
                        Modifier.size(width = 22.dp, height = 14.dp)
                            .clip(pill)
                            .background(ac.primary.copy(alpha = 0.12f))
                            .drawBehind {
                                val w = 14.dp.toPx(); val h = 2.5.dp.toPx()
                                drawRoundRect(
                                    brush = Brush.horizontalGradient(listOf(ac.primary, ac.secondary)),
                                    topLeft = androidx.compose.ui.geometry.Offset((size.width - w) / 2f, 1.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(w, h),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f),
                                )
                            }
                    )
                    "v11" -> Box(Modifier.size(14.dp).clip(CircleShape).background(grad))
                    // 侧边抽屉：同样的胶囊里放一枚竖向小条，暗示竖排
                    "v5" -> Box(
                        Modifier.size(width = 9.dp, height = 15.dp)
                            .clip(RoundedCornerShape(4.dp)).background(grad)
                    )
                    // Editorial：纸面 + 墨线，选中项是一道短横线
                    "editorial" -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(ac.primary))
                        Box(Modifier.size(width = 14.dp, height = 2.dp).clip(pill).background(ac.primary))
                    }
                    "glass" -> Box(
                        Modifier.size(width = 24.dp, height = 15.dp)
                            .clip(pill)
                            .background(ac.soft)
                            .drawBehind {
                                val w = 14.dp.toPx(); val h = 2.dp.toPx()
                                drawRoundRect(
                                    brush = Brush.horizontalGradient(listOf(ac.primary, ac.secondary)),
                                    topLeft = androidx.compose.ui.geometry.Offset((size.width - w) / 2f, 1.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(w, h),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f),
                                )
                            }
                    )
                    "nocturne" -> Box(
                        Modifier.size(width = 24.dp, height = 15.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(ac.primary.copy(alpha = 0.22f))
                            .drawBehind {
                                val w = 12.dp.toPx(); val h = 1.6.dp.toPx()
                                drawRoundRect(
                                    brush = Brush.horizontalGradient(listOf(ac.primary, ac.secondary)),
                                    topLeft = androidx.compose.ui.geometry.Offset((size.width - w) / 2f, 1.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(w, h),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f),
                                )
                            }
                    )
                    // d0 / v1 / pages：渐变实底药丸
                    else -> Box(
                        Modifier.size(width = 24.dp, height = 15.dp).clip(pill).background(grad)
                    )
                }
            }
            repeat(3) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
            }
        }
    }
}

/** 强调色卡：一枚渐变圆片 + 名称，选中显示对勾 */
@Composable
private fun AgAccentChip(
    palette: me.weishu.kernelsu.ui.theme.AccentPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier.agClick(onClick = onClick).width(62.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .shadow(
                    if (selected) 8.dp else 2.dp,
                    CircleShape,
                    spotColor = palette.primary.copy(alpha = 0.45f),
                    ambientColor = palette.primary.copy(alpha = 0.18f),
                )
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(palette.primary, palette.mid, palette.secondary)))
                .border(if (selected) 3.dp else 0.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            palette.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (selected) Ag.Vio else Ag.Tx2,
            maxLines = 1,
        )
    }
}

/** .fchip：字体选择卡 */
@Composable
private fun AgFontChip(name: String, glyph: String, family: FontFamily?, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .agClick(onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Ag.Vio.copy(alpha = 0.059f) else Color(0x8CFFFFFF))
            .border(1.4.dp, if (selected) Ag.Vio.copy(alpha = 0.502f) else Ag.Line, RoundedCornerShape(16.dp))
            .padding(14.dp, 14.dp, 14.dp, 11.dp)
            .width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            glyph,
            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
            color = if (selected) Ag.Vio else Ag.Tx,
            fontFamily = family,
        )
        Text(name, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (selected) Ag.Vio else Ag.Tx2)
    }
}

/** .icard：图标风格卡（几何形状） */
@Composable
private fun AgIconCard(name: String, shapeRadius: Float, rainbow: Boolean, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .agClick(onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Ag.Vio.copy(alpha = 0.059f) else Color(0x8CFFFFFF))
            .border(1.4.dp, if (selected) Ag.Vio.copy(alpha = 0.502f) else Ag.Line, RoundedCornerShape(16.dp))
            .padding(13.dp, 13.dp, 13.dp, 10.dp)
            .width(74.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        val shape = if (shapeRadius < 0) CircleShape else RoundedCornerShape(shapeRadius.toInt())
        Box(
            Modifier
                .size(38.dp)
                .shadow(4.dp, shape, spotColor = Ag.Vio.copy(alpha = 0.278f), ambientColor = Ag.Vio.copy(alpha = 0.278f))
                .clip(shape)
                .background(
                    if (rainbow) {
                        Brush.sweepGradient(
                            listOf(
                                Ag.Vio.copy(alpha = 1.0f), Color(0xFFEC4899), Color(0xFF14B8A6),
                                Color(0xFFF59E0B), Ag.Vio.copy(alpha = 1.0f),
                            ),
                        )
                    } else {
                        Brush.linearGradient(listOf(Color(0xFF8E5CF6), Color(0xFFEC4899)))
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AgSettingsIcons.Gear, null, tint = Color.White, modifier = Modifier.size(19.dp))
        }
        Text(name, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (selected) Ag.Vio else Ag.Tx2)
    }
}

/** .icard：启动器图标卡（完整展示狐狸图，白底浅描边，选中紫框） */
@Composable
private fun AgLauncherIconCard(name: String, iconRes: Int, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .agClick(onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Ag.Vio.copy(alpha = 0.059f) else Color(0x8CFFFFFF))
            .border(1.4.dp, if (selected) Ag.Vio.copy(alpha = 0.502f) else Ag.Line, RoundedCornerShape(16.dp))
            .padding(13.dp, 13.dp, 13.dp, 10.dp)
            .width(74.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .shadow(3.dp, RoundedCornerShape(13.dp), spotColor = Ag.Vio.copy(alpha = 0.2f), ambientColor = Ag.Vio.copy(alpha = 0.102f))
                .clip(RoundedCornerShape(13.dp))
                .background(if (Ag.isDark) Color(0xFF2A2940) else Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = name,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.size(34.dp),
            )
        }
        Text(name, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (selected) Ag.Vio else Ag.Tx2)
    }
}

/**
 * 滑杆行 .sliderrow：t 14.5/700 + v 13/800 紫
 * 轨道自绘：8px 圆角矩形 rgba(30,25,70,.08)；thumb 20dp 渐变圆 + 3px 白边 + 紫色投影。
 */
@Composable
private fun AgSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit,
    sub: String? = null,
    steps: Int = 0,
) {
    val clamped = value.coerceIn(range.start, range.endInclusive)
    val span = range.endInclusive - range.start
    val frac = if (span <= 0f) 0f else ((clamped - range.start) / span).coerceIn(0f, 1f)
    val trackColor = if (Ag.isDark) Color(0x1FFFFFFF) else Color(0x141E1946)
    var trackWidth by remember { mutableStateOf(0.dp) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    fun snap(raw: Float): Float {
        if (steps <= 0) return raw.coerceIn(range.start, range.endInclusive)
        val step = span / (steps + 1)
        return (range.start + Math.round((raw - range.start) / step) * step)
            .coerceIn(range.start, range.endInclusive)
    }

    Column(Modifier.padding(top = 14.dp, bottom = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Ag.Tx)
                if (sub != null) {
                    Spacer(Modifier.height(1.dp))
                    Text(sub, fontSize = 11.5.sp, color = Ag.Tx2)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(display, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Ag.Vio)
                Text("›", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ag.Tx3)
            }
        }
        Spacer(Modifier.height(8.dp))
        // 自绘滑杆：8px 轨道 + 20dp 渐变圆钮（3px 白边）
        Box(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .onSizeChanged { trackWidth = with(density) { it.width.toDp() } }
                .pointerInput(range, steps) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        onChange(snap(range.start + (down.position.x / w).coerceIn(0f, 1f) * span))
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { ch ->
                                onChange(snap(range.start + (ch.position.x / w).coerceIn(0f, 1f) * span))
                                ch.consume()
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(trackColor)
            )
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Ag.Grad)
            )
            Box(
                Modifier
                    .offset(x = ((trackWidth - 20.dp) * frac).coerceAtLeast(0.dp))
                    .size(20.dp)
                    .shadow(4.dp, CircleShape, spotColor = Ag.Vio.copy(alpha = 0.4f), ambientColor = Ag.Vio.copy(alpha = 0.302f))
                    .clip(CircleShape)
                    .background(Ag.Grad)
                    .border(3.dp, Color.White, CircleShape),
            )
        }
    }
}

/** .pick：右侧可点值 */
@Composable
private fun AgPick(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Ag.Vio,
        modifier = Modifier.agClick(onClick = onClick),
    )
}

/** 胶囊动作按钮（渐变实底 / 次级描边） */
@Composable
private fun AgPillAction(
    text: String,
    icon: ImageVector,
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
                        .background(Ag.Vio.copy(alpha = 0.071f), shape)
                        .border(1.dp, Ag.Vio.copy(alpha = 0.251f), shape)
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
