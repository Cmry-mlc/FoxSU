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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

            // ── 主题外观 ──
            AgGHead("主题外观")
            AgCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                AgToggleRow(
                    "启用 Monet 颜色", "跟随系统壁纸提取动态配色",
                    Ag.Mag, AgSettingsIcons.Monet,
                    trailing = {
                        var on by remember { mutableStateOf(repo.miuixMonet) }
                        AgToggle(checked = on, onCheckedChange = {
                            on = !on; repo.miuixMonet = on
                            Toast.makeText(context, if (on) "已启用 Monet 配色" else "已关闭 Monet 配色", Toast.LENGTH_SHORT).show()
                        }, small = true)
                    },
                )
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
                    "悬浮底栏", "使用 Apple 风格的悬浮底栏",
                    Ag.Teal, AgSettingsIcons.FloatingBar,
                    trailing = {
                        var on by remember { mutableStateOf(repo.enableFloatingBottomBar) }
                        AgToggle(checked = on, onCheckedChange = {
                            on = !on; settingsActions?.onSetEnableFloatingBottomBar?.invoke(on)
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
            }

            // ── 手势与显示 ──
            AgGHead("手势与显示")
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
                AgSliderRow(
                    label = "界面缩放",
                    sub = "调整全局显示比例",
                    value = repo.pageScale,
                    range = 0.7f..1.3f,
                    display = "${(repo.pageScale * 100).toInt()}%",
                    onChange = { settingsActions?.onSetPageScale?.invoke(it) },
                )
            }

            // ── 壁纸 ──
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
                        icon = AgSettingsIcons.Monet,
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

            // ── 字体（.fsel chips）──
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

            // ── 图标风格（.isel icards：几何造型）──
            AgGHead("图标风格")
            AgCard {
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
                Spacer(Modifier.height(6.dp))
            }

            // ── 启动器图标（.isel：桌面图标形象，写入 activity-alias）──
            AgGHead("启动器图标")
            AgCard {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    val launcherItems = listOf(
                        Triple("彩虹狐狸", LauncherIconManager.STYLE_FOXSU, R.drawable.fox_hero),
                        Triple("乖巧", LauncherIconManager.STYLE_FOX2, R.drawable.fox_icon_cute),
                        Triple("招手", LauncherIconManager.STYLE_FOX3, R.drawable.fox_icon_wave),
                        Triple("回眸", LauncherIconManager.STYLE_FOX4, R.drawable.fox_icon_glance),
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

            // ── 底部导航栏（.dsel 9 样式 + 开关 + 滑杆）──
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

            // ── 模块 ──
            AgGHead("模块")
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

/** .dopt：底栏样式选择卡（2 列网格） */
@Composable
private fun AgDockStyleCard(name: String, code: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .agClick(onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0x0F6C5CE7) else Color(0x8CFFFFFF))
            .border(
                1.4.dp,
                if (selected) Color(0x806C5CE7) else Ag.Line,
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

/** 底栏样式缩略预览：每种样式一个可辨识的小样 */
@Composable
private fun DockStylePreview(code: String) {
    val dot = Ag.Tx3.copy(alpha = 0.45f)
    val grad = Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFFEC4899)))
    Row(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Ag.TintInk),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (code == "v5") {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                repeat(4) { i ->
                    Box(
                        Modifier
                            .size(width = 10.dp, height = 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (i == 0) grad else androidx.compose.ui.graphics.SolidColor(dot)),
                    )
                }
            }
            return@Row
        }

        // ── 复刻自其他皮肤的 4 种：按各自底栏原样画缩略图 ──
        if (code == "editorial") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (Ag.isDark) Color(0xFF1B1A20) else Color(0xFFFFFCF6)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (Ag.isDark) Color(0xFF4A4655) else Color(0xFF1D1C22)),
                )
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(4) { i ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (i == 0) Color(0xFFE8502E) else Ag.Tx3.copy(alpha = 0.5f)
                                    ),
                            )
                            Box(
                                Modifier
                                    .size(width = 10.dp, height = 1.5.dp)
                                    .background(if (i == 0) Color(0xFFE8502E) else Color.Transparent),
                            )
                        }
                    }
                }
            }
            return@Row
        }

        if (code == "pages" || code == "glass") {
            val light = !Ag.isDark
            val pillBg = if (code == "pages") {
                if (light) Color.White else Color(0xFF1B1C1E)
            } else {
                if (light) Color(0xF2FFFFFF) else Color(0xFF14141E)
            }
            Row(
                Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
                    .height(21.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(pillBg)
                    .then(
                        if (code == "glass") Modifier.border(1.dp, Ag.Line, RoundedCornerShape(99.dp))
                        else Modifier
                    )
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Box(
                    Modifier
                        .height(13.dp)
                        .width(if (code == "pages") 26.dp else 20.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFFEC4899)))),
                )
                repeat(3) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(Ag.Tx3.copy(alpha = 0.5f)))
                }
            }
            return@Row
        }

        if (code == "nocturne") {
            Row(
                Modifier
                    .padding(horizontal = 6.dp)
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF0F1018))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(9.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Box(
                    Modifier
                        .height(13.dp)
                        .width(24.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0x338B7BFF))
                        .drawBehind {
                            val w = 10.dp.toPx()
                            val h = 1.5.dp.toPx()
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFF8B7BFF), Color(0xFF4DE2C5))
                                ),
                                topLeft = androidx.compose.ui.geometry.Offset((size.width - w) / 2f, 2.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(w, h),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f),
                            )
                        },
                )
                repeat(3) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF5D6376)))
                }
            }
            return@Row
        }

        // 第一个「选中项」小样
        Box(
            Modifier
                .padding(end = 4.dp)
                .then(
                    when (code) {
                        "v1" -> Modifier.size(width = 26.dp, height = 11.dp)
                        "v6" -> Modifier.size(15.dp)
                        "v7" -> Modifier.size(width = 18.dp, height = 11.dp)
                        "v8" -> Modifier.size(width = 20.dp, height = 12.dp)
                        "v9" -> Modifier.size(width = 20.dp, height = 12.dp)
                        "v10" -> Modifier.size(width = 18.dp, height = 11.dp)
                        "v11" -> Modifier.size(15.dp)
                        else -> Modifier.size(width = 26.dp, height = 11.dp)
                    }
                )
                .then(
                    when (code) {
                        "v6" -> Modifier
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xCC6C5CE7), Color(0x66EC4899), Color.Transparent),
                                )
                            )
                        "v7" -> Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .border(1.8.dp, Color(0x996C5CE7), RoundedCornerShape(99.dp))
                        "v8" -> Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (Ag.isDark) Color(0xFF2A2940) else Color.White)
                            .border(1.dp, Ag.Line, RoundedCornerShape(5.dp))
                        "v9" -> Modifier.clip(RoundedCornerShape(5.dp)).background(grad)
                        "v10" -> Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color(0x146C5CE7))
                            .drawBehind {
                                val w = 14.dp.toPx(); val h = 3.dp.toPx()
                                drawRoundRect(
                                    brush = Brush.horizontalGradient(listOf(Color(0xFF6C5CE7), Color(0xFFEC4899))),
                                    topLeft = androidx.compose.ui.geometry.Offset((size.width - w) / 2f, -h * 1.2f),
                                    size = androidx.compose.ui.geometry.Size(w, h),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f),
                                )
                            }
                        "v11" -> Modifier.clip(CircleShape).background(grad)
                        else -> Modifier.clip(RoundedCornerShape(99.dp)).background(grad)
                    }
                ),
        )
        // 其余三个未选中项
        repeat(3) {
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = if (code == "v1") 8.dp else 11.dp, height = 11.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(androidx.compose.ui.graphics.SolidColor(dot)),
            )
        }
    }
}

/** .fchip：字体选择卡 */
@Composable
private fun AgFontChip(name: String, glyph: String, family: FontFamily?, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .agClick(onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0x0F6C5CE7) else Color(0x8CFFFFFF))
            .border(1.4.dp, if (selected) Color(0x806C5CE7) else Ag.Line, RoundedCornerShape(16.dp))
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
            .background(if (selected) Color(0x0F6C5CE7) else Color(0x8CFFFFFF))
            .border(1.4.dp, if (selected) Color(0x806C5CE7) else Ag.Line, RoundedCornerShape(16.dp))
            .padding(13.dp, 13.dp, 13.dp, 10.dp)
            .width(74.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        val shape = if (shapeRadius < 0) CircleShape else RoundedCornerShape(shapeRadius.toInt())
        Box(
            Modifier
                .size(38.dp)
                .shadow(4.dp, shape, spotColor = Color(0x476C5CE7), ambientColor = Color(0x476C5CE7))
                .clip(shape)
                .background(
                    if (rainbow) {
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFF6C5CE7), Color(0xFFEC4899), Color(0xFF14B8A6),
                                Color(0xFFF59E0B), Color(0xFF6C5CE7),
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
            .background(if (selected) Color(0x0F6C5CE7) else Color(0x8CFFFFFF))
            .border(1.4.dp, if (selected) Color(0x806C5CE7) else Ag.Line, RoundedCornerShape(16.dp))
            .padding(13.dp, 13.dp, 13.dp, 10.dp)
            .width(74.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .shadow(3.dp, RoundedCornerShape(13.dp), spotColor = Color(0x336C5CE7), ambientColor = Color(0x1A6C5CE7))
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
                    .shadow(4.dp, CircleShape, spotColor = Color(0x666C5CE7), ambientColor = Color(0x4D6C5CE7))
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
