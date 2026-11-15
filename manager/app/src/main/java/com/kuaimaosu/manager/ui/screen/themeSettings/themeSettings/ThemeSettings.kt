package com.kuaimaosu.manager.ui.screen.themeSettings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Android
import androidx.compose.material.icons.twotone.Animation
import androidx.compose.material.icons.twotone.Badge
import androidx.compose.material.icons.twotone.BlurOn
import androidx.compose.material.icons.twotone.Brush
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.ColorLens
import androidx.compose.material.icons.twotone.Contrast
import androidx.compose.material.icons.twotone.DarkMode
import androidx.compose.material.icons.twotone.DesignServices
import androidx.compose.material.icons.twotone.AutoAwesome
import androidx.compose.material.icons.twotone.Autorenew
import androidx.compose.material.icons.twotone.Dock
import androidx.compose.material.icons.twotone.Layers
import androidx.compose.material.icons.twotone.Draw
import androidx.compose.material.icons.twotone.FormatColorFill
import androidx.compose.material.icons.twotone.FormatSize
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.LightMode
import androidx.compose.material.icons.twotone.Opacity
import androidx.compose.material.icons.twotone.Palette
import androidx.compose.material.icons.twotone.Pin
import androidx.compose.material.icons.twotone.Style
import androidx.compose.material.icons.twotone.SwapHoriz
import androidx.compose.material.icons.twotone.Swipe
import androidx.compose.material.icons.twotone.Translate
import androidx.compose.material.icons.twotone.ViewCarousel
import androidx.compose.material.icons.twotone.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.yalantis.ucrop.UCrop
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.domain.model.availablePaletteStyles
import com.kuaimaosu.manager.ui.component.ConfirmResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.twotone.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoMenuItem
import com.kuaimaosu.manager.ui.component.KuaiMaoMenuPanel
import com.kuaimaosu.manager.ui.component.KuaiMaoRow
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.screen.main.KuaiMaoSwitchRow
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTone
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.ui.theme.KuaiMaoType

import com.kuaimaosu.manager.ui.component.KuaiMaoBackCapsule
import com.kuaimaosu.manager.ui.component.KuaiMaoPageTitle
import com.kuaimaosu.manager.ui.component.KeyPointSlider
import com.kuaimaosu.manager.ui.component.rememberConfirmDialog
import com.kuaimaosu.manager.ui.component.settings.AppBackButton
import com.kuaimaosu.manager.ui.component.settings.SegmentedColumn
import com.kuaimaosu.manager.ui.component.settings.SegmentedColumnScope
import com.kuaimaosu.manager.ui.component.settings.SettingsBaseWidget
import com.kuaimaosu.manager.ui.component.settings.SettingsChooseDialog
import com.kuaimaosu.manager.ui.component.settings.SettingsChooseWidget
import com.kuaimaosu.manager.ui.component.settings.SettingsJumpPageWidget
import com.kuaimaosu.manager.ui.component.settings.SettingsSwitchWidget
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.screen.themeSettings.component.LanguageSelectionDialog
import com.kuaimaosu.manager.data.theme.ThemeRepository
import com.kuaimaosu.manager.ui.screen.themeSettings.component.BackgroundEffectPicker
import com.kuaimaosu.manager.ui.screen.themeSettings.component.KuaiMaoFontSection
import com.kuaimaosu.manager.ui.screen.themeSettings.component.HomeKickerDialog
import com.kuaimaosu.manager.ui.screen.themeSettings.component.HomeTitleDialog
import com.kuaimaosu.manager.ui.screen.themeSettings.component.KuaiMaoColorPickerDialog
import com.kuaimaosu.manager.ui.screen.themeSettings.component.toHexRgb
import com.kuaimaosu.manager.ui.screen.themeSettings.component.ThemeSettingsDialogs
import com.kuaimaosu.manager.ui.screen.themeSettings.crop.BackgroundCropActivity
import com.kuaimaosu.manager.ui.theme.BackgroundManager
import com.kuaimaosu.manager.ui.theme.BottomBarStyle
import com.kuaimaosu.manager.ui.theme.CapsuleEffect
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.theme.blurEffect
import com.kuaimaosu.manager.ui.theme.blurSource
import com.kuaimaosu.manager.ui.util.adaptiveScaffoldWindowInsets
import com.kuaimaosu.manager.ui.viewmodel.HomeUiAction
import com.kuaimaosu.manager.ui.viewmodel.HomeUiState
import com.kuaimaosu.manager.ui.viewmodel.HomeViewModel
import com.kuaimaosu.manager.ui.viewmodel.ModuleUiAction
import com.kuaimaosu.manager.ui.viewmodel.ModuleUiState
import com.kuaimaosu.manager.ui.viewmodel.ModuleViewModel
import com.kuaimaosu.manager.ui.viewmodel.PredictiveBackAnimation
import com.kuaimaosu.manager.ui.viewmodel.PredictiveBackExitDirection
import com.kuaimaosu.manager.ui.viewmodel.SettingsUiAction
import com.kuaimaosu.manager.ui.viewmodel.SettingsUiState
import com.kuaimaosu.manager.ui.viewmodel.SettingsViewModel
import com.kuaimaosu.manager.ui.viewmodel.dpiFriendlyNameRes
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@SuppressLint(
    "LocalContextConfigurationRead",
    "LocalContextResourcesRead",
    "ObsoleteSdkInt",
)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeSettingsScreen(
    settingsViewModel: SettingsViewModel,
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    // 顶部滚动行为
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // 创建设置状态管理器
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val homeViewModel = koinViewModel<HomeViewModel>()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    // 动态取色已从界面移除：把历史开启状态关掉，避免主题继续跟随壁纸跳色
    LaunchedEffect(settingsState.useDynamicColor) {
        if (settingsState.useDynamicColor) {
            settingsViewModel.dispatch(SettingsUiAction.SetDynamicColor(false))
        }
    }

    // Image selection and cropping
    var pendingBackgroundImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingExternalCropOutputUri by remember { mutableStateOf<Uri?>(null) }
    var showCropMethodDialog by remember { mutableStateOf(false) }

    val externalCropUnavailable = stringResource(R.string.background_external_crop_unavailable)
    val backgroundCropFailed = stringResource(R.string.background_crop_failed)

    val uCropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                UCrop.getOutput(data)?.let {
                    settingsViewModel.dispatch(
                        SettingsUiAction.SetCustomBackground(it.toString()),
                    )
                }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            Toast.makeText(
                context,
                backgroundCropFailed,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val externalCropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val outputUri = pendingExternalCropOutputUri ?: result.data?.data
            outputUri?.let {
                settingsViewModel.dispatch(
                    SettingsUiAction.SetCustomBackground(it.toString()),
                )
            }
        }
        pendingExternalCropOutputUri = null
    }

    val launchInAppCrop = { sourceUri: Uri ->
        val outputUri = createBackgroundCropOutputUri(context, "background_crop_ucrop")
        val (screenWidth, screenHeight) = context.backgroundCropSize()
        val intent = Intent(context, BackgroundCropActivity::class.java).apply {
            putExtra(UCrop.EXTRA_INPUT_URI, sourceUri)
            putExtra(UCrop.EXTRA_OUTPUT_URI, outputUri)
            putExtra(UCrop.EXTRA_ASPECT_RATIO_X, screenWidth.toFloat())
            putExtra(UCrop.EXTRA_ASPECT_RATIO_Y, screenHeight.toFloat())
            putExtra(UCrop.EXTRA_MAX_SIZE_X, screenWidth)
            putExtra(UCrop.EXTRA_MAX_SIZE_Y, screenHeight)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        uCropLauncher.launch(intent)
    }

    val launchExternalCrop = { sourceUri: Uri ->
        val uri = createBackgroundCropOutputUri(context, "background_crop_external")
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        val (screenWidth, screenHeight) = context.backgroundCropSize()
        val intent = Intent("com.android.camera.action.CROP").apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            putExtra("crop", "true")
            putExtra("aspectX", screenWidth)
            putExtra("aspectY", screenHeight)
            putExtra("outputX", screenWidth)
            putExtra("outputY", screenHeight)
            putExtra("return-data", false)
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }

        try {
            pendingExternalCropOutputUri = uri
            externalCropLauncher.launch(intent)
        } catch (t: Throwable) {
            Log.e("ThemeSettings", "External Crop failed, fallback to in-app crop", t)
            pendingExternalCropOutputUri = null
            Toast.makeText(
                context,
                externalCropUnavailable,
                Toast.LENGTH_SHORT,
            ).show()
            launchInAppCrop(sourceUri)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            pendingBackgroundImageUri = it
            showCropMethodDialog = true
        }
    }

    SettingsChooseDialog(
        show = showCropMethodDialog,
        title = stringResource(R.string.background_crop_method_title),
        items = listOf(
            stringResource(R.string.background_crop_method_in_app),
            stringResource(R.string.background_crop_method_external),
        ),
        selectedIndex = 0,
        onDismiss = {
            showCropMethodDialog = false
            pendingBackgroundImageUri = null
        },
        onSelectedIndexChange = { index ->
            pendingBackgroundImageUri?.let { sourceUri ->
                when (index) {
                    0 -> launchInAppCrop(sourceUri)
                    1 -> launchExternalCrop(sourceUri)
                }
            }
        },
    )

    // 各种设置对话框
    ThemeSettingsDialogs(
        state = settingsState,
        viewModel = settingsViewModel,
    )

    val navigator = LocalNavigator.current

    LaunchedEffect(Unit) {
        scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
    }

    Scaffold(
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        // 返回胶囊 + 标题放进固定 topBar，滚动列表时不再跟着内容一起走
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                KuaiMaoBackCapsule(onClick = { navigator.pop() })
                Spacer(modifier = Modifier.height(14.dp))
                KuaiMaoPageTitle(
                    title = stringResource(R.string.theme_settings),
                    subtitle = "外观、背景与交互手势",
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .blurSource(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 28.dp,
            ),
        ) {
            item {
                KuaiMaoAppearanceSection(
                    state = settingsState,
                    viewModel = settingsViewModel,
                    pickImageLauncher = pickImageLauncher,
                    coroutineScope = coroutineScope,
                )
            }

            item {
                KuaiMaoFontSection(modifier = Modifier.padding(top = 22.dp))
            }

            item {
                KuaiMaoHomeTitleSection()
            }

            item {
                KuaiMaoGestureSection(
                    state = settingsState,
                    viewModel = settingsViewModel,
                )
            }

            item {
                KuaiMaoCustomizationSection(
                    homeUiState = homeUiState,
                    homeViewModel = homeViewModel,
                )
            }
        }
    }
}

/**
 * 主页标题：自定义文案 + 双色。
 * 两种颜色复用主题色选择器的同一套交互（10 个预设 + 十六进制输入），
 * 分段位置在标题弹窗里拖动，第二段可以从任意字符开始着色。
 */
@Composable
private fun KuaiMaoHomeTitleSection() {
    val themeConfig: ThemeConfig = koinInject()
    val themeRepository: ThemeRepository = koinInject()

    var showTitleDialog by remember { mutableStateOf(false) }
    var showKickerDialog by remember { mutableStateOf(false) }
    var showPrimaryPicker by remember { mutableStateOf(false) }
    var showAccentPicker by remember { mutableStateOf(false) }

    Column {
        KuaiMaoGroupTitle(text = stringResource(R.string.home_title_section))

        KuaiMaoCard {
            KuaiMaoRow(
                icon = Icons.TwoTone.AutoAwesome,
                title = "标题上方小字",
                description = "支持手填，也可以让 AI 现写一句",
                onClick = { showKickerDialog = true },
                trailing = {
                    Text(
                        text = themeConfig.homeKicker,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = KuaiMaoColors.textSec,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp),
                    )
                },
            )
            KuaiMaoRowDivider()
            // 滚动开关：关掉后小字静止显示
            KuaiMaoSwitchRow(
                icon = Icons.TwoTone.Autorenew,
                title = "小字滚动",
                description = "一行小字持续向左流动；关闭后静止显示",
                checked = themeConfig.kickerTicker,
                tone = KuaiMaoTones.blue,
                onCheckedChange = { checked ->
                    themeConfig.kickerTicker = checked
                    themeRepository.saveKickerTicker(checked)
                },
            )
            KuaiMaoRowDivider()
            KuaiMaoRow(
                icon = Icons.TwoTone.Style,
                title = stringResource(R.string.home_title_text),
                description = stringResource(R.string.home_title_split, themeConfig.homeTitleSplit),
                onClick = { showTitleDialog = true },
                trailing = {
                    Text(
                        text = themeConfig.homeTitle,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = KuaiMaoColors.textSec,
                        maxLines = 1,
                    )
                },
            )
            KuaiMaoRowDivider()
            KuaiMaoRow(
                icon = Icons.TwoTone.FormatColorFill,
                title = stringResource(R.string.home_title_color_primary),
                description = "#" + themeConfig.homeTitleColorPrimary.toHexRgb(),
                onClick = { showPrimaryPicker = true },
                trailing = { KuaiMaoColorDot(themeConfig.homeTitleColorPrimary) },
            )
            KuaiMaoRowDivider()
            KuaiMaoRow(
                icon = Icons.TwoTone.ColorLens,
                title = stringResource(R.string.home_title_color_accent),
                description = "#" + themeConfig.homeTitleColorAccent.toHexRgb(),
                onClick = { showAccentPicker = true },
                trailing = { KuaiMaoColorDot(themeConfig.homeTitleColorAccent) },
            )
        }
    }

    if (showKickerDialog) {
        HomeKickerDialog(
            initialText = themeConfig.homeKicker,
            onConfirm = { value ->
                themeConfig.homeKicker = value
                themeRepository.saveHomeKicker(value)
                showKickerDialog = false
            },
            onDismiss = { showKickerDialog = false },
        )
    }

    if (showTitleDialog) {
        HomeTitleDialog(
            initialText = themeConfig.homeTitle,
            initialSplit = themeConfig.homeTitleSplit,
            colorPrimary = Color(themeConfig.homeTitleColorPrimary),
            colorAccent = Color(themeConfig.homeTitleColorAccent),
            onConfirm = { text, split ->
                themeConfig.homeTitle = text
                themeConfig.homeTitleSplit = split
                themeRepository.saveHomeTitle(text)
                themeRepository.saveHomeTitleSplit(split)
                showTitleDialog = false
            },
            onDismiss = { showTitleDialog = false },
        )
    }

    if (showPrimaryPicker) {
        KuaiMaoColorPickerDialog(
            title = stringResource(R.string.home_title_color_primary),
            initialColor = themeConfig.homeTitleColorPrimary,
            onConfirm = { color ->
                themeConfig.homeTitleColorPrimary = color
                themeRepository.saveHomeTitleColors(color, themeConfig.homeTitleColorAccent)
                showPrimaryPicker = false
            },
            onDismiss = { showPrimaryPicker = false },
        )
    }

    if (showAccentPicker) {
        KuaiMaoColorPickerDialog(
            title = stringResource(R.string.home_title_color_accent),
            initialColor = themeConfig.homeTitleColorAccent,
            onConfirm = { color ->
                themeConfig.homeTitleColorAccent = color
                themeRepository.saveHomeTitleColors(themeConfig.homeTitleColorPrimary, color)
                showAccentPicker = false
            },
            onDismiss = { showAccentPicker = false },
        )
    }
}

/** 22dp 圆点色块：设置行右侧展示当前颜色。 */
@Composable
private fun KuaiMaoColorDot(color: Int) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(Color(color)),
    )
}

/** 分组标题：与二级页一致的小标题样式。 */
@Composable
private fun KuaiMaoGroupTitle(text: String, top: Dp = 22.dp) {
    Text(
        text = text,
        style = KuaiMaoType.sectionTitle,
        modifier = Modifier.padding(start = 2.dp, top = top, bottom = 10.dp),
    )
}

/**
 * KuaiMao 风格的选择行：右侧显示当前值 + 箭头，点击弹出白卡菜单。
 * 所有选项直接来自调用方，不改变原有 dispatch 逻辑。
 */
@Composable
private fun KuaiMaoChoiceRow(
    icon: ImageVector,
    title: String,
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    description: String? = null,
    tone: KuaiMaoTone = KuaiMaoTones.violet,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = options.getOrElse(selectedIndex) { "" }

    Box(modifier = modifier.fillMaxWidth()) {
        KuaiMaoRow(
            icon = icon,
            title = title,
            description = description,
            iconTint = tone.content,
            iconContainer = tone.container,
            onClick = { expanded = true },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = current,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = KuaiMaoColors.textSec,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.TwoTone.KeyboardArrowRight,
                        contentDescription = null,
                        tint = KuaiMaoColors.chevron,
                        modifier = Modifier.size(16.dp),
                    )
                }
            },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            shape = RoundedCornerShape(20.dp),
            offset = DpOffset(x = 0.dp, y = 6.dp),
        ) {
            KuaiMaoMenuPanel {
                options.forEachIndexed { index, label ->
                    KuaiMaoMenuItem(
                        text = label,
                        selected = index == selectedIndex,
                        showCheck = true,
                        onClick = {
                            expanded = false
                            onSelect(index)
                        },
                    )
                }
            }
        }
    }
}

/** 外观 / 背景：语言、主题、DPI、模糊、底栏与自定义背景。 */
@Composable
private fun KuaiMaoAppearanceSection(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    pickImageLauncher: ManagedActivityResultLauncher<String, Uri?>,
    coroutineScope: CoroutineScope,
) {
    val themeConfig: ThemeConfig = koinInject()
    val backgroundManager: BackgroundManager = koinInject()
    Column {
        KuaiMaoGroupTitle(text = "外观", top = 0.dp)

        KuaiMaoCard {
            LanguageSetting(state = state, viewModel = viewModel)
            KuaiMaoRowDivider()

            KuaiMaoChoiceRow(
                icon = Icons.TwoTone.DarkMode,
                title = stringResource(R.string.theme_mode),
                options = state.themeOptions.map { stringResource(it) },
                selectedIndex = state.themeMode,
                tone = KuaiMaoTones.blue,
                onSelect = { index -> viewModel.dispatch(SettingsUiAction.SetThemeMode(index)) },
            )
            KuaiMaoRowDivider()

            // 主题色：动态取色已移除，固定使用这里的种子色
            KuaiMaoRow(
                icon = Icons.TwoTone.Palette,
                title = stringResource(R.string.theme_color),
                description = themeConfig.seedColor.toSeedColorHex(),
                onClick = {
                    viewModel.dispatch(SettingsUiAction.SetThemeColorDialogVisible(true))
                },
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(themeConfig.seedColor)),
                    )
                },
            )
            KuaiMaoRowDivider()

            KuaiMaoRow(
                icon = Icons.TwoTone.FormatSize,
                title = stringResource(R.string.app_dpi_title),
                description = stringResource(R.string.app_dpi_summary),
                trailing = {
                    Text(
                        text = stringResource(dpiFriendlyNameRes(state.tempDpi)),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = KuaiMaoColors.brand,
                    )
                },
            )

            KuaiMaoRowDivider()
            KuaiMaoChoiceRow(
                icon = Icons.TwoTone.Dock,
                title = "底栏样式",
                description = "传统栏贴底、胶囊栏浮起、侧边栏竖排在左侧",
                options = listOf("传统栏", "胶囊栏", "侧边栏"),
                selectedIndex = themeConfig.bottomBarStyle.ordinal,
                tone = KuaiMaoTones.violet,
                onSelect = { index ->
                    val style = BottomBarStyle.entries.getOrElse(index) {
                        BottomBarStyle.TRADITIONAL
                    }
                    backgroundManager.saveBottomBarStyle(style)
                },
            )
            KuaiMaoRowDivider()
            KuaiMaoSwitchRow(
                icon = Icons.TwoTone.BlurOn,
                title = stringResource(id = R.string.settings_config_enable_blur),
                description = "底栏、弹层对身后的内容做实时模糊；关闭后为不透明实色底",
                checked = themeConfig.isEnableBlur,
                tone = KuaiMaoTones.blue,
                onCheckedChange = { enabled ->
                    backgroundManager.saveEnableBlur(enabled)
                },
            )
            KuaiMaoRowDivider()
            KuaiMaoChoiceRow(
                icon = Icons.TwoTone.Layers,
                title = "胶囊质感",
                description = themeConfig.capsuleEffect.summary,
                options = CapsuleEffect.entries.map { it.label },
                selectedIndex = themeConfig.capsuleEffect.ordinal,
                tone = KuaiMaoTones.blue,
                onSelect = { index ->
                    val effect = CapsuleEffect.entries.getOrElse(index) {
                        CapsuleEffect.LIQUID_GLASS
                    }
                    backgroundManager.saveCapsuleEffect(effect)
                },
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        KuaiMaoCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
            DpiSliderControls(
                state = state,
                viewModel = viewModel,
                coroutineScope = coroutineScope,
            )
        }

        KuaiMaoGroupTitle(text = "背景特效")

        Text(
            text = "全部程序化绘制，颜色跟随主题色，透明度很低、不影响阅读",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = KuaiMaoColors.textTer,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
        )

        BackgroundEffectPicker(
            selected = themeConfig.backgroundEffect,
            onSelect = { backgroundManager.saveBackgroundEffect(it) },
        )

        Spacer(modifier = Modifier.height(18.dp))

        KuaiMaoGroupTitle(text = "自定义背景")

        KuaiMaoCard {
            KuaiMaoSwitchRow(
                icon = Icons.TwoTone.Wallpaper,
                title = stringResource(id = R.string.settings_custom_background),
                description = stringResource(id = R.string.settings_custom_background_summary),
                checked = state.isCustomBackgroundEnabled,
                tone = KuaiMaoTones.green,
                onCheckedChange = { isChecked ->
                    if (isChecked) {
                        pickImageLauncher.launch("image/*")
                    } else {
                        viewModel.dispatch(SettingsUiAction.RemoveCustomBackground)
                    }
                },
            )

            if (state.isCustomBackgroundEnabled) {
                KuaiMaoRowDivider()
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    KuaiMaoSliderRow(
                        title = stringResource(R.string.settings_card_alpha),
                        valueText = "${(state.cardAlpha * 100).roundToInt()}%",
                    ) {
                        KeyPointSlider(
                            value = state.cardAlpha,
                            onValueChange = { value ->
                                viewModel.dispatch(SettingsUiAction.SetCardAlpha(value))
                            },
                            onValueChangeFinished = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    viewModel.dispatch(SettingsUiAction.SaveCardConfig)
                                }
                            },
                            valueRange = 0f..1f,
                            keyPoints = listOf(0.25f, 0.5f, 0.75f),
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    // 背景模糊：整张壁纸的虚化强度。和原版一样用同一条可拖动的调节条，
                    // 拉到 0 是原图，拉满是一层底纹。
                    var blurSlider by remember { mutableFloatStateOf(themeConfig.backgroundBlur) }
                    KuaiMaoSliderRow(
                        title = "背景模糊",
                        valueText = "${(blurSlider * 100).roundToInt()}%",
                    ) {
                        KeyPointSlider(
                            value = blurSlider,
                            onValueChange = { value ->
                                blurSlider = value
                                themeConfig.backgroundBlur = value
                            },
                            onValueChangeFinished = {
                                backgroundManager.saveBackgroundBlur(blurSlider)
                            },
                            valueRange = 0f..1f,
                            keyPoints = listOf(0.25f, 0.5f, 0.75f),
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    KuaiMaoSliderRow(
                        title = stringResource(R.string.settings_background_dim),
                        valueText = "${(state.backgroundDim * 100).roundToInt()}%",
                    ) {
                        KeyPointSlider(
                            value = state.backgroundDim,
                            onValueChange = { value ->
                                viewModel.dispatch(SettingsUiAction.SetBackgroundDim(value))
                            },
                            onValueChangeFinished = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    viewModel.dispatch(SettingsUiAction.SaveCardConfig)
                                }
                            },
                            valueRange = 0f..1f,
                            keyPoints = listOf(0.25f, 0.5f, 0.75f),
                        )
                    }
                }

                if (state.useDynamicColor) {
                    KuaiMaoRowDivider()
                    KuaiMaoSwitchRow(
                        icon = Icons.TwoTone.FormatColorFill,
                        title = stringResource(id = R.string.settings_config_use_custom_background_seed_color),
                        description = stringResource(id = R.string.settings_config_use_custom_background_seed_color_summary),
                        checked = themeConfig.isUseBackgroundSeedColor,
                        tone = KuaiMaoTones.rose,
                        onCheckedChange = { isChecked ->
                            backgroundManager.saveUseBackgroundSeedColor(isChecked)
                        },
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    KuaiMaoRowDivider()
                    KuaiMaoSwitchRow(
                        icon = Icons.TwoTone.Contrast,
                        title = stringResource(id = R.string.settings_custom_enable_high_contrast),
                        description = stringResource(id = R.string.settings_custom_enable_high_contrast_summary),
                        checked = themeConfig.isHighContrastMode,
                        tone = KuaiMaoTones.slate,
                        onCheckedChange = { isChecked ->
                            backgroundManager.saveEnableHighContrastMode(isChecked)
                        },
                    )
                }
            }
        }
    }
}

/** 手势与交互：预测返回、滑动返回与翻页方式。 */
@Composable
private fun KuaiMaoGestureSection(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    Column {
        KuaiMaoGroupTitle(text = "手势与交互")

        KuaiMaoCard {
            KuaiMaoChoiceRow(
                icon = Icons.TwoTone.Animation,
                title = stringResource(R.string.predictive_back_animation),
                options = PredictiveBackAnimation.entries.map { stringResource(it.resId) },
                selectedIndex = state.predictiveBackAnimation.ordinal,
                tone = KuaiMaoTones.violet,
                onSelect = { index ->
                    PredictiveBackAnimation.entries.getOrNull(index)?.let { animation ->
                        viewModel.dispatch(SettingsUiAction.SetPredictiveBackAnimation(animation))
                    }
                },
            )

            if (state.predictiveBackAnimation == PredictiveBackAnimation.Scale) {
                KuaiMaoRowDivider()
                KuaiMaoChoiceRow(
                    icon = Icons.TwoTone.SwapHoriz,
                    title = stringResource(R.string.predictive_back_exit_direction),
                    options = PredictiveBackExitDirection.entries.map { stringResource(it.resId) },
                    selectedIndex = state.predictiveBackExitDirection.ordinal,
                    tone = KuaiMaoTones.blue,
                    onSelect = { index ->
                        PredictiveBackExitDirection.entries.getOrNull(index)?.let { direction ->
                            viewModel.dispatch(
                                SettingsUiAction.SetPredictiveBackExitDirection(direction),
                            )
                        }
                    },
                )
            }

            KuaiMaoRowDivider()
            KuaiMaoSwitchRow(
                icon = Icons.TwoTone.Swipe,
                title = stringResource(R.string.settings_enable_swipe_dismiss),
                description = stringResource(R.string.settings_enable_swipe_dismiss_summary),
                checked = state.enableSwipeDismiss,
                tone = KuaiMaoTones.green,
                onCheckedChange = { enabled ->
                    viewModel.dispatch(SettingsUiAction.SetSwipeDismiss(enabled))
                },
            )

            KuaiMaoRowDivider()
            KuaiMaoChoiceRow(
                icon = Icons.TwoTone.ViewCarousel,
                title = stringResource(R.string.settings_pager_gesture_mode),
                options = listOf(
                    stringResource(R.string.settings_pager_gesture_native),
                    stringResource(R.string.settings_pager_gesture_cross_axis),
                    stringResource(R.string.settings_pager_gesture_ios_like),
                ),
                selectedIndex = state.pagerInterceptionMode.coerceIn(0, 2),
                tone = KuaiMaoTones.amber,
                onSelect = { index ->
                    viewModel.dispatch(SettingsUiAction.SetPagerInterceptionMode(index))
                },
            )
        }
    }
}

/** 定制：只保留底栏角标显示。 */
@Composable
private fun KuaiMaoCustomizationSection(
    homeUiState: HomeUiState,
    homeViewModel: HomeViewModel,
) {
    Column {
        KuaiMaoGroupTitle(text = stringResource(R.string.custom_settings))

        KuaiMaoCard {
            KuaiMaoSwitchRow(
                icon = Icons.TwoTone.Pin,
                title = stringResource(R.string.navigation_bar_badge),
                description = stringResource(R.string.navigation_bar_badge_summary),
                checked = homeUiState.showNavigationBarBadge,
                tone = KuaiMaoTones.rose,
                onCheckedChange = { enabled ->
                    homeViewModel.dispatch(HomeUiAction.SetNavigationBarBadge(enabled))
                },
            )
        }
    }
}

/** 标题 + 数值 + 滑块的紧凑行。 */
@Composable
private fun KuaiMaoSliderRow(
    title: String,
    valueText: String,
    content: @Composable () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = KuaiMaoColors.textLabel,
            )
            Text(
                text = valueText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = KuaiMaoColors.brand,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        content()
    }
}

private fun PaletteStyle.displayName(): String = when (this) {
    PaletteStyle.TonalSpot -> "Tonal Spot"
    PaletteStyle.Neutral -> "Neutral"
    PaletteStyle.Vibrant -> "Vibrant"
    PaletteStyle.Expressive -> "Expressive"
    PaletteStyle.Rainbow -> "Rainbow"
    PaletteStyle.FruitSalad -> "Fruit Salad"
    PaletteStyle.Monochrome -> "Monochrome"
    PaletteStyle.Fidelity -> "Fidelity"
    PaletteStyle.Content -> "Content"
}

private fun ColorSpec.SpecVersion.displayName(): String = when (this) {
    ColorSpec.SpecVersion.SPEC_2021 -> "Spec 2021"
    ColorSpec.SpecVersion.SPEC_2025 -> "Spec 2025"
}

private fun Context.backgroundCropSize(): Pair<Int, Int> {
    val displayMetrics = resources.displayMetrics
    return displayMetrics.widthPixels to displayMetrics.heightPixels
}

private fun createBackgroundCropOutputUri(context: Context, prefix: String): Uri {
    val outputFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg").apply {
        parentFile?.mkdirs()
        delete()
        createNewFile()
        deleteOnExit()
    }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile,
    )
}

@Composable
fun PredictiveBackAnimationWidget(
    uiState: SettingsUiState,
    onSelect: (PredictiveBackAnimation) -> Unit,
) {
    SettingsChooseWidget(
        icon = Icons.TwoTone.Animation,
        title = stringResource(R.string.predictive_back_animation),
        items = PredictiveBackAnimation.entries.map { stringResource(it.resId) },
        selectedIndex = uiState.predictiveBackAnimation.ordinal,
        onSelectedIndexChange = { index ->
            PredictiveBackAnimation.entries.getOrNull(index)?.let(onSelect)
        },
    )
}

@Composable
fun PredictiveBackAnimationDirectionWidget(
    uiState: SettingsUiState,
    onSelect: (PredictiveBackExitDirection) -> Unit,
) {
    SettingsChooseWidget(
        icon = Icons.TwoTone.SwapHoriz,
        title = stringResource(R.string.predictive_back_exit_direction),
        items = PredictiveBackExitDirection.entries.map { stringResource(it.resId) },
        selectedIndex = uiState.predictiveBackExitDirection.ordinal,
        onSelectedIndexChange = { index ->
            PredictiveBackExitDirection.entries.getOrNull(index)?.let(onSelect)
        },
    )
}

private fun Int.toSeedColorHex(): String = "#%06X".format(this and 0x00FFFFFF)

@Composable
private fun DpiSliderControls(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    coroutineScope: CoroutineScope,
) {
    val confirmDialog = rememberConfirmDialog()
    val dpiConfirmTitle = stringResource(R.string.dpi_confirm_title)
    val dpiConfirmMessage =
        stringResource(R.string.dpi_confirm_message, state.currentDpi, state.tempDpi)
    val confirmText = stringResource(R.string.confirm)
    val cancelText = stringResource(R.string.cancel)

    val sliderValue by animateFloatAsState(
        targetValue = state.tempDpi.toFloat(),
        label = "DPI Slider Animation",
    )

    KeyPointSlider(
        value = sliderValue,
        onValueChange = { newValue ->
            viewModel.dispatch(SettingsUiAction.SetTempDpi(newValue.toInt()))
        },
        modifier = Modifier.fillMaxWidth(),
        valueRange = 160f..600f,
        keyPoints = state.dpiPresets.map { (_, dpi) -> dpi.toFloat() },
    )

    // DPI 预设按钮行
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        state.dpiPresets.forEach { (nameResource, dpi) ->
            val isSelected = state.tempDpi == dpi
            val buttonColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceBright
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(buttonColor)
                    .clickable {
                        viewModel.dispatch(SettingsUiAction.SetTempDpi(dpi))
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(nameResource),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    Text(
        text = if (state.isDpiCustom) {
            "${stringResource(R.string.dpi_size_custom)}: ${state.tempDpi}"
        } else {
            "${stringResource(dpiFriendlyNameRes(state.tempDpi))}: ${state.tempDpi}"
        },
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )

    Button(
        onClick = {
            coroutineScope.launch {
                val confirmResult = confirmDialog.awaitConfirm(
                    title = dpiConfirmTitle,
                    content = dpiConfirmMessage,
                    confirm = confirmText,
                    dismiss = cancelText,
                )

                if (confirmResult != ConfirmResult.Confirmed) return@launch

                viewModel.dispatch(SettingsUiAction.ApplyDpi)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        enabled = state.tempDpi != state.currentDpi,
    ) {
        Icon(
            Icons.TwoTone.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.dpi_apply_settings))
    }
}

@Composable
private fun CustomBackgroundSettings(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    pickImageLauncher: ManagedActivityResultLauncher<String, Uri?>,
) {
    // TODO Portrait/Landscape wallpaper split

    SettingsSwitchWidget(
        icon = Icons.TwoTone.Wallpaper,
        title = stringResource(id = R.string.settings_custom_background),
        description = stringResource(id = R.string.settings_custom_background_summary),
        checked = state.isCustomBackgroundEnabled,
        onCheckedChange = { isChecked ->
            if (isChecked) {
                pickImageLauncher.launch("image/*")
            } else {
                viewModel.dispatch(SettingsUiAction.RemoveCustomBackground)
            }
        },
    )
}

private fun SegmentedColumnScope.backgroundAdjustmentControls(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    coroutineScope: CoroutineScope,
    themeConfig: ThemeConfig,
    backgroundManager: BackgroundManager,
) {
    item(
        topPadding = 1.dp,
    ) {
        AlphaSlider(
            state = state,
            viewModel = viewModel,
            coroutineScope = coroutineScope,
        )
    }

    item(
        topPadding = 1.dp,
    ) {
        DimSlider(
            state = state,
            viewModel = viewModel,
            coroutineScope = coroutineScope,
        )
    }

    item(visible = themeConfig.isEnableBlur, topPadding = 1.dp) {
        SettingsSwitchWidget(
            icon = Icons.TwoTone.Draw,
            title = stringResource(id = R.string.settings_exp_draw_background_to_blur),
            description = stringResource(id = R.string.settings_exp_draw_background_to_blur_description),
            isError = true,
            checked = themeConfig.isEnableBlurExp,
            onCheckedChange = { isChecked ->
                backgroundManager.saveEnableBlurExp(isChecked)
            },
        )
    }

    item(
        visible = state.useDynamicColor,
        topPadding = 1.dp,
    ) {
        SettingsSwitchWidget(
            icon = Icons.TwoTone.FormatColorFill,
            title = stringResource(id = R.string.settings_config_use_custom_background_seed_color),
            description = stringResource(id = R.string.settings_config_use_custom_background_seed_color_summary),
            checked = themeConfig.isUseBackgroundSeedColor,
            onCheckedChange = { isChecked ->
                backgroundManager.saveUseBackgroundSeedColor(isChecked)
            },
        )
    }

    item(
        visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        topPadding = 1.dp,
    ) {
        SettingsSwitchWidget(
            icon = Icons.TwoTone.Contrast,
            title = stringResource(id = R.string.settings_custom_enable_high_contrast),
            description = stringResource(id = R.string.settings_custom_enable_high_contrast_summary),
            checked = themeConfig.isHighContrastMode,
            onCheckedChange = { isChecked ->
                backgroundManager.saveEnableHighContrastMode(isChecked)
            },
        )
    }
}

@Composable
private fun AlphaSlider(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    coroutineScope: CoroutineScope,
) {
    SettingsBaseWidget(
        icon = Icons.TwoTone.Opacity,
        title = stringResource(R.string.settings_card_alpha),
        descriptionColumnContent = {
            val alphaSliderValue by animateFloatAsState(
                targetValue = state.cardAlpha,
                label = "Alpha Slider Animation",
            )

            KeyPointSlider(
                value = alphaSliderValue,
                onValueChange = { newValue ->
                    viewModel.dispatch(SettingsUiAction.SetCardAlpha(newValue))
                },
                onValueChangeFinished = {
                    coroutineScope.launch(Dispatchers.IO) {
                        viewModel.dispatch(SettingsUiAction.SaveCardConfig)
                    }
                },
                valueRange = 0f..1f,
                keyPoints = listOf(0.25f, 0.5f, 0.75f),
            )
        },
    ) {
        Box(contentAlignment = Alignment.CenterEnd) {
            Text( // Some stupid way to solve measure problem
                text = "100%",
                style = MaterialTheme.typography.labelMediumEmphasized,
                modifier = Modifier.alpha(0f),
            )
            Text(
                text = "${(state.cardAlpha * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMediumEmphasized,
            )
        }
    }
}

@Composable
private fun DimSlider(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    coroutineScope: CoroutineScope,
) {
    SettingsBaseWidget(
        icon = Icons.TwoTone.LightMode,
        title = stringResource(R.string.settings_background_dim),
        descriptionColumnContent = {
            val dimSliderValue by animateFloatAsState(
                targetValue = state.backgroundDim,
                label = "Dim Slider Animation",
            )

            KeyPointSlider(
                value = dimSliderValue,
                onValueChange = { newValue ->
                    viewModel.dispatch(SettingsUiAction.SetBackgroundDim(newValue))
                },
                onValueChangeFinished = {
                    coroutineScope.launch(Dispatchers.IO) {
                        viewModel.dispatch(SettingsUiAction.SaveCardConfig)
                    }
                },
                valueRange = 0f..1f,
                keyPoints = listOf(0.25f, 0.5f, 0.75f),
            )
        },
    ) {
        Box(contentAlignment = Alignment.CenterEnd) {
            Text( // Some stupid way to solve measure problem
                text = "100%",
                style = MaterialTheme.typography.labelMediumEmphasized,
                modifier = Modifier.alpha(0f),
            )

            Text(
                text = "${(state.backgroundDim * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMediumEmphasized,
            )
        }
    }
}

@Composable
private fun LanguageSetting(state: SettingsUiState, viewModel: SettingsViewModel) {
    val language = stringResource(id = R.string.settings_language)
    val languageSystemDefault = stringResource(R.string.language_system_default)

    // Compute display name based on current app locale
    val currentLanguageDisplay = remember(state.currentAppLocale) {
        val locale = state.currentAppLocale
        if (locale != null) {
            locale.getDisplayName(locale)
        } else {
            languageSystemDefault
        }
    }

    KuaiMaoRow(
        icon = Icons.TwoTone.Translate,
        title = language,
        description = currentLanguageDisplay,
        onClick = {
            viewModel.dispatch(SettingsUiAction.SetLanguageDialogVisible(true))
        },
        trailing = {
            Icon(
                imageVector = Icons.TwoTone.KeyboardArrowRight,
                contentDescription = null,
                tint = KuaiMaoColors.chevron,
                modifier = Modifier.size(16.dp),
            )
        },
    )

    // Language Selection Dialog
    if (state.showLanguageDialog) {
        LanguageSelectionDialog(
            currentLocale = state.currentAppLocale?.let { locale ->
                if (locale.country.isEmpty()) locale.language else "${locale.language}_${locale.country}"
            } ?: "system",
            onLanguageSelected = { localeTag ->
                // Update local state immediately
                viewModel.dispatch(SettingsUiAction.SetLanguage(localeTag))
                // Apply locale change immediately for Android < 13
                viewModel.dispatch(SettingsUiAction.RestartActivity)
            },
            onDismiss = {
                viewModel.dispatch(SettingsUiAction.SetLanguageDialogVisible(false))
            },
        )
    }
}
