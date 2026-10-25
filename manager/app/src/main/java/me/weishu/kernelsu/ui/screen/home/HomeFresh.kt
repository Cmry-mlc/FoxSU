package me.weishu.kernelsu.ui.screen.home

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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.KsuIsValid
import me.weishu.kernelsu.ui.component.rebootlistpopup.getRebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.rememberRebootAction
import me.weishu.kernelsu.ui.theme.FreshPalette

/**
 * Things worth telling the user about. Every entry is derived from real state, so the
 * banner list never shows made-up items.
 */
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
    onRefresh: () -> Unit,
    onOpenTab: (Int) -> Unit,
    onOpenModuleRepo: () -> Unit,
    onOpenLog: () -> Unit,
) {
    val issues = state.freshIssues()

    val navigationBarPadding = if (!Natives.isFullFeatured()) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(
            top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 8.dp,
            bottom = bottomInnerPadding + navigationBarPadding + 16.dp,
        ),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                FreshHeader(
                    onRefresh = onRefresh,
                    onOpenSettings = { onOpenTab(3) },
                )

                Spacer(Modifier.height(14.dp))

                if (issues.isNotEmpty()) {
                    FreshIssueList(
                        issues = issues,
                        onInstall = actions.onInstallClick,
                        canInstallKernel = state.canInstallKernelUpdate,
                    )
                    Spacer(Modifier.height(14.dp))
                }

                // 原「刷入镜像」与「运行模式」合并成这张状态卡：点一下就去刷入/更新内核
                FreshStatusCard(
                    state = state,
                    onInstall = actions.onInstallClick,
                    onJailbreak = actions.onJailbreakClick,
                )

                Spacer(Modifier.height(14.dp))

                FreshInfoGroup(systemInfo = state.systemInfo)

                Spacer(Modifier.height(14.dp))

                FreshSecurityGroup(systemInfo = state.systemInfo)

                Spacer(Modifier.height(14.dp))

                FreshSupportGroup(onOpenUrl = actions.onOpenUrl)

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.fresh_home_tools),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )

                FreshToolRow(
                    icon = Icons.Filled.Storefront,
                    tint = FreshPalette.Green,
                    title = stringResource(R.string.fresh_home_tool_repo),
                    description = stringResource(R.string.fresh_home_tool_repo_desc),
                    onClick = onOpenModuleRepo,
                )
                Spacer(Modifier.height(10.dp))
                FreshToolRow(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    tint = FreshPalette.Amber,
                    title = stringResource(R.string.fresh_home_tool_log),
                    description = stringResource(R.string.fresh_home_tool_log_desc),
                    onClick = onOpenLog,
                )
            }
        }
    }
}

@Composable
private fun FreshHeader(
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

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
        Box {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { menuExpanded = true },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = stringResource(R.string.reboot),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.fresh_home_menu_recheck)) },
                    leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onRefresh()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.fresh_home_menu_settings)) },
                    leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onOpenSettings()
                    },
                )

                HorizontalDivider()

                KsuIsValid {
                    val rebootOptions = getRebootListOption()
                    val onReboot = rememberRebootAction()
                    rebootOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes)) },
                            onClick = {
                                menuExpanded = false
                                onReboot(option.reason)
                            },
                        )
                    }
                }
            }
        }
    }
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
private fun FreshInfoGroup(systemInfo: SystemInfo) {
    FreshCardGroup {
        FreshInfoRow(
            icon = Icons.Filled.Tag,
            label = stringResource(R.string.home_manager_version),
            value = systemInfo.managerVersion,
        )
        FreshDivider()
        FreshInfoRow(
            icon = Icons.Filled.DeveloperBoard,
            label = stringResource(R.string.home_kernel),
            value = systemInfo.kernelVersion,
        )
        FreshDivider()
        FreshInfoRow(
            icon = Icons.Filled.Smartphone,
            label = stringResource(R.string.home_device_model),
            value = systemInfo.deviceModel,
        )
        FreshDivider()
        FreshInfoRow(
            icon = Icons.Filled.Fingerprint,
            label = stringResource(R.string.home_fingerprint),
            value = systemInfo.fingerprint,
        )
    }
}

@Composable
private fun FreshSecurityGroup(systemInfo: SystemInfo) {
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

    FreshCardGroup {
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
private fun FreshSupportGroup(onOpenUrl: (String) -> Unit) {
    val learnMoreUrl = stringResource(R.string.home_learn_kernelsu_url)
    FreshCardGroup {
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
private fun FreshCardGroup(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
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
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
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
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
