package me.weishu.kernelsu.ui.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.KsuIsValid
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.component.rebootlistpopup.RebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.getRebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.rememberRebootAction
import me.weishu.kernelsu.ui.uiModeLabelRes

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
    val currentUiMode = LocalUiMode.current

    val custom = CustomizationStore.current
    val cardColor = if (custom.cardColor == 0) {
        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = custom.cardAlpha)
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

    val navigationBarPadding = if (!Natives.isFullFeatured()) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }

    LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
            top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 8.dp,
                bottom = bottomInnerPadding + navigationBarPadding + 16.dp,
            ),
        ) {
            item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                FreshHeader()

                Spacer(Modifier.height(14.dp))

                if (issues.isNotEmpty()) {
                    FreshIssueList(
                        issues = issues,
                        onInstall = actions.onInstallClick,
                        canInstallKernel = state.canInstallKernelUpdate,
                    )
                    Spacer(Modifier.height(14.dp))
                }

                // 状态卡：运行模式 + 版本 + 刷入入口（原「运行模式 / 刷入镜像」合并在此）
                FreshStatusCard(
                    state = state,
                    onInstall = actions.onInstallClick,
                    onJailbreak = actions.onJailbreakClick,
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
                        containerColor = cardColor,
                        contentColor = cardContent,
                        accentColor = cardAccent,
                        onClick = null,
                    )
                    FreshKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.settings_ui_mode),
                        value = stringResource(uiModeLabelRes(currentUiMode)),
                        caption = stringResource(R.string.fresh_home_style_switch),
                        containerColor = cardColor,
                        contentColor = cardContent,
                        accentColor = cardAccent,
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
                        containerColor = cardColor,
                        contentColor = cardContent,
                        accentColor = cardAccent,
                        onClick = { onOpenTab(1) },
                    )
                    FreshKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Extension,
                        title = stringResource(R.string.module),
                        value = navigationBadge.moduleEnabledCount.toString(),
                        caption = stringResource(R.string.fresh_home_module_sub),
                        containerColor = cardColor,
                        contentColor = cardContent,
                        accentColor = cardAccent,
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
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
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
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
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
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
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

@Composable
private fun FreshStatusCard(
    state: HomeUiState,
    onInstall: () -> Unit,
    onJailbreak: () -> Unit,
) {
    val ksuActive = state.ksuVersion != null
    val notInstalled = !ksuActive && state.kernelVersion.isGKI()

    val containerColor = if (ksuActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (ksuActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = !state.isLateLoadMode) { onInstall() },
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
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
                        color = contentColor.copy(alpha = 0.78f),
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
                                color = contentColor.copy(alpha = 0.6f),
                            )
                        }
                    }
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
                tint = contentColor.copy(alpha = 0.30f),
                modifier = Modifier.size(44.dp),
            )
        }
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

@Composable
private fun FreshKpiCard(
    icon: ImageVector,
    title: String,
    value: String,
    caption: String,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .height(148.dp)
            .clip(RoundedCornerShape(22.dp))
            .then(clickModifier),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.14f),
                contentColor = accentColor,
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
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = caption,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = contentColor.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
        shadowElevation = 1.dp,
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
                color = LocalContentColor.current.copy(alpha = 0.65f),
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
                color = LocalContentColor.current.copy(alpha = 0.65f),
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

@Composable
private fun FreshIssueList(
    issues: List<FreshIssue>,
    onInstall: () -> Unit,
    canInstallKernel: Boolean,
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
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (actionable) Modifier.clickable(onClick = onInstall) else Modifier
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
        shadowElevation = 1.dp,
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
                    color = LocalContentColor.current.copy(alpha = 0.65f),
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
