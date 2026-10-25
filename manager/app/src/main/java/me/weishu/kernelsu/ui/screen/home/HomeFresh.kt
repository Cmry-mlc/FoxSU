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
import androidx.compose.material.icons.filled.CheckCircle
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
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.data.settings.Customization
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.data.settings.HERO_SLOT
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.KsuIsValid
import me.weishu.kernelsu.ui.component.freshCardBorder
import me.weishu.kernelsu.ui.component.freshCardColor
import me.weishu.kernelsu.ui.component.supportingTextAlpha
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.component.rememberWallpaper
import me.weishu.kernelsu.ui.component.rebootlistpopup.RebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.getRebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.rememberRebootAction
import me.weishu.kernelsu.ui.uiModeLabelRes
import me.weishu.kernelsu.ui.util.savePickedImage

/** Resolved colours for one card: global settings with optional per-card override. */
private data class CardSurface(
    val container: Color,
    val content: Color,
    val accent: Color,
    val border: BorderStroke?,
)

/**
 * Slot 0 is the hero card, 1..4 are the KPI cards. A card either follows the global card
 * colour / opacity or overrides it from the long-press sheet.
 */
@Composable
private fun resolveCardSurface(slot: Int): CardSurface {
    val custom = CustomizationStore.current
    val overrideColor = custom.surfaceColors.getOrNull(slot) ?: 0
    val effectiveColor = if (overrideColor != 0) overrideColor else custom.cardColor
    val alphaOverride = custom.surfaceAlphas.getOrNull(slot) ?: -1f
    val alpha = if (alphaOverride >= 0f) alphaOverride else custom.cardAlpha

    val base = if (effectiveColor == 0) {
        freshCardColor()
    } else {
        Color(effectiveColor)
    }
    val content = when {
        effectiveColor == 0 -> MaterialTheme.colorScheme.onSurface
        base.luminance() > 0.55f -> Color(0xFF141A14)
        else -> Color.White
    }
    val accent = if (effectiveColor == 0) MaterialTheme.colorScheme.primary else content
    val border = if (effectiveColor == 0) freshCardBorder() else null
    return CardSurface(base.copy(alpha = alpha), content, accent, border)
}

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
    onOpenLog: () -> Unit,
) {
    val issues = state.freshIssues()
    var showStylePicker by remember { mutableStateOf(false) }
    var showHeroMenu by remember { mutableStateOf(false) }
    var kpiMenuIndex by remember { mutableStateOf(-1) }
    var kpiPickIndex by remember { mutableStateOf(0) }
    val currentUiMode = LocalUiMode.current

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val heroPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                savePickedImage(context, uri, "hero_card.jpg", maxSide = 1400)
            }
            if (saved == null) {
                Toast.makeText(
                    context,
                    R.string.customization_wallpaper_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                CustomizationStore.setHeroWallpaper(saved)
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
                savePickedImage(context, uri, "kpi_card_$index.jpg", maxSide = 1200)
            }
            if (saved == null) {
                Toast.makeText(
                    context,
                    R.string.customization_wallpaper_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                CustomizationStore.setKpiImage(index, saved)
            }
        }
    }

    val custom = CustomizationStore.current
    val cardColor = if (custom.cardColor == 0) {
        freshCardColor().copy(alpha = custom.cardAlpha)
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

                FreshCardGroup(
                    containerColor = cardColor,
                    contentColor = cardContent,
                ) {
                    FreshInfoRow(
                        icon = Icons.Filled.DeveloperBoard,
                        label = stringResource(R.string.home_kernel),
                        value = state.systemInfo.kernelVersion,
                    )
                    FreshDivider()
                    FreshInfoRow(
                        icon = Icons.Filled.Fingerprint,
                        label = stringResource(R.string.home_fingerprint),
                        value = state.systemInfo.fingerprint,
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

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.fresh_home_tools),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )

                FreshToolRow(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    tint = MaterialTheme.colorScheme.tertiary,
                    title = stringResource(R.string.fresh_home_tool_log),
                    description = stringResource(R.string.fresh_home_tool_log_desc),
                    containerColor = cardColor,
                    contentColor = cardContent,
                    onClick = onOpenLog,
                )
            }
        }
        }
    }

    if (showHeroMenu) {
        FreshBackdropDialog(
            title = stringResource(R.string.fresh_home_hero_title),
            description = stringResource(R.string.fresh_home_hero_desc),
            surfaceColor = custom.surfaceColors.getOrNull(HERO_SLOT) ?: 0,
            surfaceAlpha = (custom.surfaceAlphas.getOrNull(HERO_SLOT) ?: -1f)
                .let { if (it >= 0f) it else custom.cardAlpha },
            onSurfaceColorChange = { CustomizationStore.setSurfaceColor(HERO_SLOT, it) },
            onSurfaceAlphaChange = { CustomizationStore.setSurfaceAlpha(HERO_SLOT, it) },
            hasImage = heroOn,
            alpha = custom.heroDim,
            alphaRange = 0f..0.8f,
            alphaLabel = stringResource(R.string.customization_dim),
            onAlphaChange = CustomizationStore::setHeroDim,
            onPick = {
                showHeroMenu = false
                heroPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemove = {
                showHeroMenu = false
                CustomizationStore.setHeroWallpaper(null)
            },
            onDismiss = { showHeroMenu = false },
        )
    }

    val kpiIndex = kpiMenuIndex
    if (kpiIndex >= 0) {
        FreshBackdropDialog(
            title = stringResource(R.string.fresh_home_kpi_bg_title),
            description = stringResource(R.string.fresh_home_kpi_bg_desc),
            surfaceColor = custom.surfaceColors.getOrNull(kpiIndex + 1) ?: 0,
            surfaceAlpha = (custom.surfaceAlphas.getOrNull(kpiIndex + 1) ?: -1f)
                .let { if (it >= 0f) it else custom.cardAlpha },
            onSurfaceColorChange = { CustomizationStore.setSurfaceColor(kpiIndex + 1, it) },
            onSurfaceAlphaChange = { CustomizationStore.setSurfaceAlpha(kpiIndex + 1, it) },
            hasImage = custom.kpiImages.getOrNull(kpiIndex) != null,
            alpha = custom.kpiAlphas.getOrNull(kpiIndex) ?: 0.7f,
            alphaRange = 0.2f..1f,
            alphaLabel = stringResource(R.string.fresh_home_kpi_alpha),
            onAlphaChange = { CustomizationStore.setKpiAlpha(kpiIndex, it) },
            onPick = {
                kpiPickIndex = kpiIndex
                kpiMenuIndex = -1
                kpiPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemove = {
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
    var showRebootDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        KsuIsValid {
            Surface(
                shape = CircleShape,
                color = freshCardColor(),
                border = freshCardBorder(),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { showRebootDialog = true },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = stringResource(R.string.reboot),
                        tint = MaterialTheme.colorScheme.onSurface,
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

    val statusIcon = when {
        ksuActive -> Icons.Filled.CheckCircle
        notInstalled -> Icons.Filled.Warning
        else -> Icons.Filled.Block
    }
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
        shadowElevation = if (hasHero || cardBorder != null) 0.dp else 1.dp,
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
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = if (ksuActive) {
                        contentColor.copy(alpha = if (hasHero) 0.85f else 0.30f)
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(44.dp),
                )
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
private fun FreshBackdropDialog(
    title: String,
    description: String,
    surfaceColor: Int,
    surfaceAlpha: Float,
    onSurfaceColorChange: (Int) -> Unit,
    onSurfaceAlphaChange: (Float) -> Unit,
    hasImage: Boolean,
    alpha: Float,
    alphaRange: ClosedFloatingPointRange<Float>,
    alphaLabel: String,
    onAlphaChange: (Float) -> Unit,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
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
                FreshCardColorSection(selected = surfaceColor, onSelect = onSurfaceColorChange)

                DialogSliderRow(
                    label = stringResource(R.string.fresh_home_card_alpha),
                    value = surfaceAlpha,
                    range = 0.2f..1f,
                    onChange = onSurfaceAlphaChange,
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.customization_wallpaper),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FreshPillButton(
                        text = stringResource(R.string.customization_pick_image),
                        icon = Icons.Filled.Image,
                        filled = true,
                        onClick = onPick,
                    )
                    if (hasImage) {
                        FreshPillButton(
                            text = stringResource(R.string.customization_clear),
                            icon = Icons.Filled.Delete,
                            filled = false,
                            onClick = onRemove,
                        )
                    }
                }
                if (hasImage) {
                    DialogSliderRow(
                        label = alphaLabel,
                        value = alpha,
                        range = alphaRange,
                        onChange = onAlphaChange,
                    )
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
private fun FreshCardColorSection(selected: Int, onSelect: (Int) -> Unit) {
    val presets = CustomizationStore.cardColorOptions
    val isPreset = selected == 0 || presets.contains(selected)
    var customOpen by remember { mutableStateOf(!isPreset) }
    val themeSeed = MaterialTheme.colorScheme.primary.toArgb()
    var hsv by remember {
        mutableStateOf(argbToHsv(if (selected == 0) themeSeed else selected))
    }

    FreshColorSwatches(
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
private fun FreshColorSwatches(
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

        // 自定义颜色
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
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
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

private fun argbToHsv(argb: Int): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    return hsv
}

private fun hsvToArgb(hsv: FloatArray): Int =
    android.graphics.Color.HSVToColor(
        floatArrayOf(hsv[0], hsv[1].coerceIn(0f, 1f), hsv[2].coerceIn(0.05f, 1f))
    )

@Composable
private fun FreshPillButton(
    text: String,
    icon: ImageVector,
    filled: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
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
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

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
        shadowElevation = if (cardBorder != null) 0.dp else 1.dp,
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

    FreshCardGroup(containerColor = containerColor, contentColor = contentColor) {
        FreshInfoRow(
            icon = Icons.Filled.Security,
            label = stringResource(R.string.home_selinux_status),
            value = selinuxDisplay,
        )
        FreshDivider()
        FreshInfoRow(
            icon = Icons.Filled.FilterList,
            label = stringResource(R.string.home_seccomp_status),
            value = seccompDisplay,
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
    FreshCardGroup(containerColor = containerColor, contentColor = contentColor) {
        FreshNavRow(
            icon = Icons.Filled.VolunteerActivism,
            title = stringResource(R.string.home_support_title),
            description = stringResource(R.string.home_support_content),
            onClick = { onOpenUrl("https://patreon.com/weishu") },
        )
        FreshDivider()
        FreshNavRow(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = stringResource(R.string.home_learn_kernelsu),
            description = stringResource(R.string.home_click_to_learn_kernelsu),
            onClick = { onOpenUrl(learnMoreUrl) },
        )
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
        shadowElevation = if (freshCardBorder() != null) 0.dp else 1.dp,
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
                FreshIssue.UpdateAvailable -> stringResource(R.string.fresh_issue_update)
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
                shadowElevation = if (freshCardBorder() != null) 0.dp else 1.dp,
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

@Composable
private fun FreshToolRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor,
        border = freshCardBorder(),
        shadowElevation = if (freshCardBorder() != null) 0.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = tint.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalContentColor.current,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = supportingTextAlpha()),
                    maxLines = 1,
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
}
