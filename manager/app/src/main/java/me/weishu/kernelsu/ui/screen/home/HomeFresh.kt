package me.weishu.kernelsu.ui.screen.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.data.settings.Customization
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.data.settings.RainbowCards
import me.weishu.kernelsu.data.settings.HERO_SLOT
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import androidx.compose.ui.res.painterResource
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.CardAppearanceDialog
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.component.CardSurface
import me.weishu.kernelsu.ui.component.KsuIsValid
import me.weishu.kernelsu.ui.component.freshCardBorder
import me.weishu.kernelsu.ui.component.freshCardColor
import me.weishu.kernelsu.ui.component.isNightScheme
import me.weishu.kernelsu.ui.component.resolveCardSurface
import me.weishu.kernelsu.ui.component.supportingTextAlpha
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.component.rememberWallpaper
import me.weishu.kernelsu.ui.component.rebootlistpopup.RebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.getRebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.rememberRebootAction
import me.weishu.kernelsu.ui.uiModeLabelRes
import me.weishu.kernelsu.ui.component.ImageCropDialog
import me.weishu.kernelsu.ui.util.savePickedImage

/** Resolved colours for one card: global settings with optional per-card override. */
/** Things worth telling the user about, all derived from real state. */
private enum class FreshIssue {
    RootMissing,
    KernelOutdated,
    ManagerOutdated,
    SafeMode,
    NotGki,
    SelinuxPermissive,
    UpdateAvailable,
    LkmUpdate,
}

private fun HomeUiState.freshIssues(): List<FreshIssue> = buildList {
    if (ksuVersion != null && !isRootAvailable) add(FreshIssue.RootMissing)
    if (requiresNewKernel) add(FreshIssue.KernelOutdated)
    if (requiresNewManager) add(FreshIssue.ManagerOutdated)
    if (isSafeMode) add(FreshIssue.SafeMode)
    if (showGkiWarning) add(FreshIssue.NotGki)
    if (isSELinuxPermissive) add(FreshIssue.SelinuxPermissive)
    if (hasUpdate) add(FreshIssue.UpdateAvailable)
    if (showLkmUpdate) add(FreshIssue.LkmUpdate)
}

@Composable
fun HomePagerFresh(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
    navigationBadge: NavigationBadgeState,
    onOpenTab: (Int) -> Unit,
) {
    val issues = state.freshIssues()
    var showStylePicker by remember { mutableStateOf(false) }
    var showHeroMenu by remember { mutableStateOf(false) }
    var kpiMenuIndex by remember { mutableStateOf(-1) }
    var kpiPickIndex by remember { mutableStateOf(0) }
    var pendingCrop by remember { mutableStateOf<PendingCrop?>(null) }
    val currentUiMode = LocalUiMode.current

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val heroPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                savePickedImage(context, uri, "hero_raw.jpg", maxSide = 2000)
            }
            if (saved == null) {
                Toast.makeText(
                    context,
                    R.string.customization_wallpaper_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                pendingCrop = PendingCrop(saved, 2.6f, 0, 0)
            }
        }
    }

    val kpiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val index = kpiPickIndex
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                savePickedImage(context, uri, "kpi_raw_$index.jpg", maxSide = 2000)
            }
            if (saved == null) {
                Toast.makeText(
                    context,
                    R.string.customization_wallpaper_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                pendingCrop = PendingCrop(saved, 1.08f, 1, index)
            }
        }
    }

    val custom = CustomizationStore.current
    val night = isNightScheme()
    val cardColor = if (custom.cardColor == 0 || night) {
        freshCardColor().let { it.copy(alpha = it.alpha * custom.cardAlpha) }
    } else {
        Color(custom.cardColor).copy(alpha = custom.cardAlpha)
    }
    val cardContent = if (custom.cardColor == 0) {
        MaterialTheme.colorScheme.onSurface
    } else if (cardColor.luminance() > 0.55f) {
        Color(0xFF141A14)
    } else {
        Color.White
    }
    val cardAccent = if (custom.cardColor == 0) {
        MaterialTheme.colorScheme.primary
    } else {
        cardContent
    }

    // 顶部大卡 + 四张卡各自可覆盖卡面颜色与透明度
    val heroSurface = resolveCardSurface(HERO_SLOT)
    val surface1 = resolveCardSurface(HERO_SLOT + 1)
    val surface2 = resolveCardSurface(HERO_SLOT + 2)
    val surface3 = resolveCardSurface(HERO_SLOT + 3)
    val surface4 = resolveCardSurface(HERO_SLOT + 4)

    // 顶部大卡可以单独铺一张图片；有图时文字转为白色，靠暗化蒙层保证可读
    val heroCustom = if (custom.heroWallpaperUri != null) custom else null
    val heroBitmap = rememberWallpaper(custom.heroWallpaperUri)
    val heroOn = heroBitmap != null
    val heroContent = if (heroOn) Color.White else heroSurface.content

    val navigationBarPadding = if (!Natives.isFullFeatured()) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 标题栏保持不透明：全局壁纸只铺在它下方，避免图案压住 FoxSu 标题
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 8.dp,
                    bottom = 12.dp,
                ),
        ) {
            FreshHeader()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                bottom = bottomInnerPadding + navigationBarPadding + 16.dp,
            ),
        ) {
            item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                if (issues.isNotEmpty()) {
                    FreshIssueList(
                        issues = issues,
                        onInstall = actions.onInstallClick,
                        canInstallKernel = state.canInstallKernelUpdate,
                        containerColor = cardColor,
                        contentColor = cardContent,
                        latestVersionCode = state.latestVersionInfo.versionCode,
                        currentVersion = BuildConfig.VERSION_NAME,
                        onLongPress = { showHeroMenu = true },
                    )
                    Spacer(Modifier.height(14.dp))
                }

                // 状态卡：运行模式 + 版本 + 刷入入口（原「运行模式 / 刷入镜像」合并在此）
                FreshStatusCard(
                    state = state,
                    onInstall = actions.onInstallClick,
                    onJailbreak = actions.onJailbreakClick,
                    containerColor = if (heroOn) Color.Transparent else heroSurface.container,
                    contentColor = heroContent,
                    hero = heroCustom,
                    heroBitmap = heroBitmap,
                    cardBorder = heroSurface.border,
                    onLongPress = { showHeroMenu = true },
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FreshKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Smartphone,
                        title = stringResource(R.string.home_device_model),
                        value = state.systemInfo.deviceModel,
                        caption = stringResource(R.string.fresh_home_device_sub),
                        containerColor = surface1.container,
                        contentColor = surface1.content,
                        accentColor = surface1.accent,
                        cardBorder = surface1.border,
                        imageUri = custom.kpiImages.getOrNull(0),
                        imageAlpha = custom.kpiAlphas.getOrNull(0) ?: 0.7f,
                        onLongPress = { kpiMenuIndex = 0 },
                        onClick = null,
                    )
                    FreshKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.settings_ui_mode),
                        value = stringResource(uiModeLabelRes(currentUiMode)),
                        caption = stringResource(R.string.fresh_home_style_switch),
                        containerColor = surface2.container,
                        contentColor = surface2.content,
                        accentColor = surface2.accent,
                        cardBorder = surface2.border,
                        imageUri = custom.kpiImages.getOrNull(1),
                        imageAlpha = custom.kpiAlphas.getOrNull(1) ?: 0.7f,
                        onLongPress = { kpiMenuIndex = 1 },
                        onClick = { showStylePicker = true },
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FreshKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Shield,
                        title = stringResource(R.string.superuser),
                        value = navigationBadge.superuserCount.toString(),
                        caption = stringResource(R.string.fresh_home_superuser_sub),
                        containerColor = surface3.container,
                        contentColor = surface3.content,
                        accentColor = surface3.accent,
                        cardBorder = surface3.border,
                        imageUri = custom.kpiImages.getOrNull(2),
                        imageAlpha = custom.kpiAlphas.getOrNull(2) ?: 0.7f,
                        onLongPress = { kpiMenuIndex = 2 },
                        onClick = { onOpenTab(1) },
                    )
                    FreshKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Extension,
                        title = stringResource(R.string.module),
                        value = navigationBadge.moduleEnabledCount.toString(),
                        caption = stringResource(R.string.fresh_home_module_sub),
                        containerColor = surface4.container,
                        contentColor = surface4.content,
                        accentColor = surface4.accent,
                        cardBorder = surface4.border,
                        imageUri = custom.kpiImages.getOrNull(3),
                        imageAlpha = custom.kpiAlphas.getOrNull(3) ?: 0.7f,
                        onLongPress = { kpiMenuIndex = 3 },
                        onClick = { onOpenTab(2) },
                    )
                }

                Spacer(Modifier.height(14.dp))

                FreshSecurityGroup(
                    systemInfo = state.systemInfo,
                    containerColor = cardColor,
                    contentColor = cardContent,
                )

                Spacer(Modifier.height(14.dp))

                FreshSupportGroup(
                    onOpenUrl = actions.onOpenUrl,
                    containerColor = cardColor,
                    contentColor = cardContent,
                )

                Spacer(Modifier.height(14.dp))

                val (kc1, kt1) = rainbowPair(5)
                FreshKernelCard(
                    icon = Icons.Filled.DeveloperBoard,
                    title = stringResource(R.string.home_kernel),
                    value = state.systemInfo.kernelVersion,
                    containerColor = kc1,
                    contentColor = kt1,
                )

                Spacer(Modifier.height(10.dp))

                val (kc2, kt2) = rainbowPair(0)
                FreshKernelCard(
                    icon = Icons.Filled.Fingerprint,
                    title = stringResource(R.string.home_fingerprint),
                    value = state.systemInfo.fingerprint,
                    containerColor = kc2,
                    contentColor = kt2,
                )

            }
        }
        }
    }

    pendingCrop?.let { crop ->
        ImageCropDialog(
            sourcePath = crop.path,
            aspectRatio = crop.aspect,
            outputName = if (crop.kind == 0) "hero_card.jpg" else "kpi_card_${crop.index}.jpg",
            onConfirm = { cropped ->
                if (crop.kind == 0) {
                    CustomizationStore.setHeroWallpaper(cropped)
                } else {
                    CustomizationStore.setKpiImage(crop.index, cropped)
                }
                pendingCrop = null
            },
            onDismiss = { pendingCrop = null },
        )
    }

    if (showHeroMenu) {
        CardAppearanceDialog(
            slot = HERO_SLOT,
            title = stringResource(R.string.fresh_home_hero_title),
            description = stringResource(R.string.fresh_home_hero_desc),
            hasImage = heroOn,
            imageAlpha = custom.heroDim,
            imageAlphaRange = 0f..0.8f,
            imageAlphaLabel = stringResource(R.string.customization_dim),
            onImageAlphaChange = CustomizationStore::setHeroDim,
            onPickImage = {
                showHeroMenu = false
                heroPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemoveImage = {
                showHeroMenu = false
                CustomizationStore.setHeroWallpaper(null)
            },
            onDismiss = { showHeroMenu = false },
        )
    }

    val kpiIndex = kpiMenuIndex
    if (kpiIndex >= 0) {
        CardAppearanceDialog(
            slot = kpiIndex + 1,
            title = stringResource(R.string.fresh_home_kpi_bg_title),
            description = stringResource(R.string.fresh_home_kpi_bg_desc),
            hasImage = custom.kpiImages.getOrNull(kpiIndex) != null,
            imageAlpha = custom.kpiAlphas.getOrNull(kpiIndex) ?: 0.7f,
            imageAlphaRange = 0.2f..1f,
            imageAlphaLabel = stringResource(R.string.fresh_home_kpi_alpha),
            onImageAlphaChange = { CustomizationStore.setKpiAlpha(kpiIndex, it) },
            onPickImage = {
                kpiPickIndex = kpiIndex
                kpiMenuIndex = -1
                kpiPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemoveImage = {
                CustomizationStore.setKpiImage(kpiIndex, null)
                kpiMenuIndex = -1
            },
            onDismiss = { kpiMenuIndex = -1 },
        )
    }

    if (showStylePicker) {
        FreshStylePickerDialog(
            current = currentUiMode,
            onDismiss = { showStylePicker = false },
        )
    }
}

@Composable
private fun FreshStylePickerDialog(
    current: UiMode,
    onDismiss: () -> Unit,
) {
    val repo = remember { SettingsRepositoryImpl() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = freshCardColor(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.settings_ui_mode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))

                UiMode.entries.forEach { mode ->
                    val selected = mode == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                repo.uiMode = mode.value
                                onDismiss()
                            }
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(uiModeLabelRes(mode)),
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
                    Spacer(Modifier.height(4.dp))
                }

                Spacer(Modifier.height(8.dp))
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
private fun FreshHeader() {
    val navigator = LocalNavigator.current
    var showRebootDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val customTitle = CustomizationStore.current.homeTitle
        Text(
            text = customTitle.ifBlank { stringResource(R.string.app_name) },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        // 关于作者：和电源键同款的圆形描边按钮
        IconButton(
            onClick = { navigator.push(Route.Author) },
            modifier = Modifier
                .padding(end = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(freshCardColor()),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_author),
                contentDescription = stringResource(R.string.author_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
        }

        KsuIsValid {
            Surface(
                shape = CircleShape,
                color = freshCardColor(),
                border = freshCardBorder(),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { showRebootDialog = true },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_power),
                        contentDescription = stringResource(R.string.reboot),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    if (showRebootDialog) {
        FreshRebootDialog(onDismiss = { showRebootDialog = false })
    }
}

/** 3 x 3 grid of power options, replacing the old dropdown list. */
@Composable
private fun FreshRebootDialog(onDismiss: () -> Unit) {
    val options = getRebootListOption()
    val onReboot = rememberRebootAction()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = freshCardColor(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.reboot),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))

                options.chunked(3).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowOptions.forEach { option ->
                            RebootGridCell(
                                option = option,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onDismiss()
                                    onReboot(option.reason)
                                },
                            )
                        }
                        repeat(3 - rowOptions.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(2.dp))
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
private fun RebootGridCell(
    option: RebootListOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = rebootIcon(option.reason),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(option.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun rebootIcon(reason: String): ImageVector = when (reason) {
    "userspace" -> Icons.Filled.Autorenew
    "soft_reboot" -> Icons.Filled.Cached
    "recovery" -> Icons.Filled.SettingsBackupRestore
    "bootloader" -> Icons.Filled.DeveloperMode
    "download" -> Icons.Filled.Download
    "edl" -> Icons.Filled.Usb
    else -> Icons.Filled.RestartAlt
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FreshStatusCard(
    state: HomeUiState,
    onInstall: () -> Unit,
    onJailbreak: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    hero: Customization?,
    heroBitmap: ImageBitmap?,
    cardBorder: BorderStroke?,
    onLongPress: () -> Unit,
) {
    val ksuActive = state.ksuVersion != null
    val notInstalled = !ksuActive && state.kernelVersion.isGKI()

    val statusIcon = painterResource(
        when {
            ksuActive -> R.drawable.ic_home_status_ok
            notInstalled -> R.drawable.ic_home_status_warn
            else -> R.drawable.ic_home_status_fail
        }
    )
    val statusTitle = when {
        ksuActive -> stringResource(R.string.home_working)
        notInstalled -> stringResource(R.string.home_not_installed)
        else -> stringResource(R.string.home_unsupported)
    }
    val statusSummary = when {
        ksuActive -> stringResource(R.string.home_working_version, "${state.ksuVersion}-${state.kernelUAPIVersion}")
        notInstalled -> stringResource(R.string.home_click_to_install)
        else -> stringResource(R.string.home_unsupported_reason)
    }
    val workingMode = if (ksuActive) {
        when (state.lkmMode) {
            null -> ""
            true -> "LKM"
            else -> "GKI"
        }
    } else ""

    val canJailbreak = notInstalled && state.isSELinuxPermissive
    val hasHero = hero != null && heroBitmap != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                onClick = { if (!state.isLateLoadMode) onInstall() },
                onLongClick = onLongPress,
            ),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        contentColor = contentColor,
        border = if (hasHero) null else cardBorder,
        shadowElevation = 0.dp,
    ) {
        Box {
            if (hero != null && heroBitmap != null) {
                FreshHeroBackdrop(hero = hero, bitmap = heroBitmap)
            }
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor,
                        )
                        if (ksuActive && state.isLateLoadMode) {
                            Spacer(Modifier.width(8.dp))
                            FreshTag(
                                text = stringResource(R.string.jailbreak_mode),
                                container = MaterialTheme.colorScheme.errorContainer,
                                content = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                        if (ksuActive && state.isSafeMode) {
                            Spacer(Modifier.width(8.dp))
                            FreshTag(
                                text = stringResource(R.string.safe_mode),
                                container = MaterialTheme.colorScheme.errorContainer,
                                content = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = statusSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = if (supportingTextAlpha() > 0.7f) 0.88f else 0.78f),
                        )
                        if (state.showCustomLkmBadge) {
                            Spacer(Modifier.width(8.dp))
                            FreshTag(
                                text = stringResource(R.string.home_lkm_custom),
                                container = MaterialTheme.colorScheme.tertiaryContainer,
                                content = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                    if (workingMode.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FreshTag(
                                text = workingMode,
                                container = MaterialTheme.colorScheme.primary,
                                content = MaterialTheme.colorScheme.onPrimary,
                            )
                            if (!state.isLateLoadMode) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.fresh_home_flash_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor.copy(alpha = 0.72f),
                                )
                            }
                        }
                    }
                    if (!hasHero) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.fresh_home_hero_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.6f),
                        )
                    }
                    if (canJailbreak) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onJailbreak() },
                        ) {
                            Text(
                                text = stringResource(R.string.home_jailbreak),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
                // 状态徽章：右端垂直居中的圆形柔光底，视觉重心更稳
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (ksuActive) contentColor.copy(alpha = if (hasHero) 0.14f else 0.06f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = statusIcon,
                        contentDescription = null,
                        tint = if (ksuActive) {
                            contentColor.copy(alpha = if (hasHero) 0.9f else 0.5f)
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}

/** Paints the optional picture behind the hero card, with the same zoom / pan model as the wallpaper. */
@Composable
private fun BoxScope.FreshHeroBackdrop(hero: Customization, bitmap: ImageBitmap?) {
    if (bitmap == null) return
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                scaleX = hero.heroScale
                scaleY = hero.heroScale
                translationX = hero.heroOffsetX * size.width
                translationY = hero.heroOffsetY * size.height
            },
    )
    if (hero.heroDim > 0f) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = hero.heroDim)),
        )
    }
}

/** Long-press menu offering to pick / remove the hero card picture. */
@Composable
private fun FreshDialogRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FreshTag(text: String, container: Color, content: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = container,
        contentColor = content,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FreshKpiCard(
    icon: ImageVector,
    title: String,
    value: String,
    caption: String,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
    cardBorder: BorderStroke?,
    imageUri: String?,
    imageAlpha: Float,
    onLongPress: () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberWallpaper(imageUri)
    val showImage = bitmap != null && imageAlpha > 0.02f
    // 图片足够不透明时文字转白，否则沿用卡面配色
    val onPhoto = showImage && imageAlpha > 0.45f
    val textColor = if (onPhoto) Color.White else contentColor
    val valueColor = if (onPhoto) Color.White else accentColor

    Surface(
        modifier = modifier
            .height(148.dp)
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = onLongPress,
            ),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        contentColor = textColor,
        border = cardBorder,
        shadowElevation = 0.dp,
    ) {
        Box {
            if (bitmap != null && imageAlpha > 0.02f) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = imageAlpha },
                )
                if (onPhoto) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.32f)),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = valueColor.copy(alpha = 0.16f),
                    contentColor = valueColor,
                    modifier = Modifier.size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 18.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = caption,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = textColor.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FreshSecurityGroup(
    systemInfo: SystemInfo,
    containerColor: Color,
    contentColor: Color,
) {
    val selinuxDisplay = when (systemInfo.selinuxStatus) {
        "Enforcing" -> stringResource(R.string.selinux_status_enforcing)
        "Permissive" -> stringResource(R.string.selinux_status_permissive)
        "Disabled" -> stringResource(R.string.selinux_status_disabled)
        else -> stringResource(R.string.selinux_status_unknown)
    }
    val seccompDisplay = when (systemInfo.seccompStatus) {
        -1 -> stringResource(R.string.seccomp_status_not_supported)
        0 -> stringResource(R.string.seccomp_status_disabled)
        1 -> stringResource(R.string.seccomp_status_strict)
        2 -> stringResource(R.string.seccomp_status_filter)
        else -> stringResource(R.string.seccomp_status_unknown)
    }

    val (c1, t1) = rainbowPair(1)
    val (c2, t2) = rainbowPair(2)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FreshPillTile(
            icon = Icons.Filled.Security,
            title = stringResource(R.string.home_selinux_status),
            caption = selinuxDisplay,
            containerColor = c1,
            contentColor = t1,
            modifier = Modifier.weight(1f),
        )
        FreshPillTile(
            icon = Icons.Filled.FilterList,
            title = stringResource(R.string.home_seccomp_status),
            caption = seccompDisplay,
            containerColor = c2,
            contentColor = t2,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FreshSupportGroup(
    onOpenUrl: (String) -> Unit,
    containerColor: Color,
    contentColor: Color,
) {
    val learnMoreUrl = stringResource(R.string.home_learn_kernelsu_url)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val (c3, t3) = rainbowPair(3)
        val (c4, t4) = rainbowPair(4)
        FreshPillTile(
            icon = Icons.Filled.VolunteerActivism,
            title = stringResource(R.string.home_support_title),
            caption = stringResource(R.string.home_support_content),
            accent = true,
            containerColor = c3,
            contentColor = t3,
            onClick = { onOpenUrl("https://patreon.com/weishu") },
            modifier = Modifier.weight(1f),
        )
        FreshPillTile(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = stringResource(R.string.home_learn_kernelsu),
            caption = stringResource(R.string.home_click_to_learn_kernelsu),
            accent = true,
            containerColor = c4,
            contentColor = t4,
            onClick = { onOpenUrl(learnMoreUrl) },
            modifier = Modifier.weight(1f),
        )
    }
}

/** 胶囊卡：左侧图标 + 标题/说明，对应参考图里的“常用工具”样式。 */
@Composable
private fun FreshPillTile(
    icon: ImageVector,
    title: String,
    caption: String,
    containerColor: Color,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(26.dp)
    val tint = if (accent) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.7f)
    val base = modifier
        .height(78.dp)
        .clip(shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Surface(
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = freshCardBorder(),
        shadowElevation = 0.dp,
        modifier = base,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = contentColor,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = caption,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = contentColor.copy(alpha = supportingTextAlpha()),
                )
            }
        }
    }
}


/** 次级卡片的配色：七彩开启时取对应色，玻璃卡面优先。 */
@Composable
private fun rainbowPair(index: Int): Pair<Color, Color> {
    val custom = CustomizationStore.current
    if (!custom.cardGlass && !isNightScheme() && custom.rainbowCards) {
        val (bg, fg) = RainbowCards[index % RainbowCards.size]
        return Color(bg) to Color(fg)
    }
    return freshCardColor() to MaterialTheme.colorScheme.onSurface
}

/** 内核信息卡：左侧圆角图标块 + 标题/详情，对应参考图里的单行工具卡。 */
@Composable
private fun FreshKernelCard(
    icon: ImageVector,
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
        border = freshCardBorder(),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.68f),
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = value,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = contentColor.copy(alpha = supportingTextAlpha()),
                )
            }
        }
    }
}

@Composable
private fun FreshCardGroup(
    containerColor: Color,
    contentColor: Color,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        contentColor = contentColor,
        border = freshCardBorder(),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column { content() }
    }
}

@Composable
private fun FreshDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun FreshInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = LocalContentColor.current,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = supportingTextAlpha()),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FreshNavRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = LocalContentColor.current,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = supportingTextAlpha()),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FreshIssueList(
    issues: List<FreshIssue>,
    onInstall: () -> Unit,
    canInstallKernel: Boolean,
    containerColor: Color,
    contentColor: Color,
    latestVersionCode: Int,
    currentVersion: String,
    onLongPress: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        issues.forEach { issue ->
            val text = when (issue) {
                FreshIssue.RootMissing -> stringResource(R.string.fresh_issue_root)
                FreshIssue.KernelOutdated -> stringResource(R.string.fresh_issue_kernel_old)
                FreshIssue.ManagerOutdated -> stringResource(R.string.fresh_issue_manager_old)
                FreshIssue.SafeMode -> stringResource(R.string.fresh_issue_safe_mode)
                FreshIssue.NotGki -> stringResource(R.string.fresh_issue_not_gki)
                FreshIssue.SelinuxPermissive -> stringResource(R.string.fresh_issue_selinux)
                FreshIssue.UpdateAvailable -> stringResource(
                    R.string.fresh_issue_update,
                    latestVersionCode,
                    currentVersion,
                )
                FreshIssue.LkmUpdate -> stringResource(R.string.fresh_issue_lkm_update)
            }
            val actionable = canInstallKernel && (
                issue == FreshIssue.KernelOutdated ||
                    issue == FreshIssue.ManagerOutdated ||
                    issue == FreshIssue.UpdateAvailable ||
                    issue == FreshIssue.LkmUpdate
                )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                contentColor = contentColor,
                border = freshCardBorder(),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(
                        onClick = { if (actionable) onInstall() },
                        onLongClick = onLongPress,
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        modifier = Modifier.weight(1f),
                    )
                    if (actionable) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/** 待裁剪的卡面图片：kind 0 = 顶部大卡，1 = KPI 卡（带索引）。 */
private data class PendingCrop(
    val path: String,
    val aspect: Float,
    val kind: Int,
    val index: Int,
)
