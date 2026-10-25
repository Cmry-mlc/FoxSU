package me.weishu.kernelsu.ui.screen.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.theme.FreshPalette

/**
 * Health checks that feed the ring gauge. Every item is derived from real state, so the
 * score is meaningful instead of decorative.
 */
private enum class FreshIssue(val penalty: Int) {
    RootMissing(35),
    KernelOutdated(25),
    ManagerOutdated(12),
    SafeMode(15),
    NotGki(10),
    SelinuxPermissive(8),
    UpdateAvailable(5),
    LkmUpdate(5),
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

private fun List<FreshIssue>.freshScore(): Int =
    (100 - sumOf { it.penalty }).coerceIn(20, 100)

@Composable
fun HomePagerFresh(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
    navigationBadge: NavigationBadgeState,
    onRefresh: () -> Unit,
    onOpenTab: (Int) -> Unit,
    onOpenModuleRepo: () -> Unit,
    onOpenLog: () -> Unit,
) {
    val issues = state.freshIssues()
    val score = issues.freshScore()
    val needsAction = issues.any {
        it == FreshIssue.KernelOutdated || it == FreshIssue.ManagerOutdated ||
            it == FreshIssue.UpdateAvailable || it == FreshIssue.LkmUpdate
    }
    val primaryAction = if (needsAction) actions.onInstallClick else onRefresh

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

                Spacer(Modifier.height(8.dp))

                FreshScoreRing(score = score)

                Spacer(Modifier.height(18.dp))

                Text(
                    text = freshStatusText(score = score, issueCount = issues.size),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(18.dp))

                FreshPrimaryButton(
                    label = stringResource(
                        if (needsAction) R.string.fresh_home_optimize else R.string.fresh_home_recheck
                    ),
                    onClick = primaryAction,
                )

                if (issues.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    FreshIssueList(
                        issues = issues,
                        onInstall = actions.onInstallClick,
                        canInstallKernel = state.canInstallKernelUpdate,
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FreshInfoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Memory,
                        title = stringResource(R.string.fresh_home_kernel),
                        value = state.kernelVersion.toString(),
                        subtitle = stringResource(
                            if (state.kernelVersion.isGKI()) R.string.fresh_home_kernel_gki
                            else R.string.fresh_home_kernel_nongki
                        ),
                        onClick = actions.onInstallClick,
                    )
                    FreshInfoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Bolt,
                        title = stringResource(R.string.fresh_home_mode),
                        value = freshModeValue(state),
                        subtitle = stringResource(R.string.fresh_home_mode_sub),
                        onClick = actions.onInstallClick,
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FreshInfoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Extension,
                        title = stringResource(R.string.fresh_home_modules),
                        value = navigationBadge.moduleEnabledCount.toString(),
                        subtitle = if (navigationBadge.moduleUpdatableCount > 0) {
                            stringResource(
                                R.string.fresh_home_modules_update,
                                navigationBadge.moduleUpdatableCount
                            )
                        } else {
                            stringResource(R.string.fresh_home_modules_sub)
                        },
                        onClick = { onOpenTab(2) },
                    )
                    FreshInfoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Shield,
                        title = stringResource(R.string.fresh_home_superuser),
                        value = navigationBadge.superuserCount.toString(),
                        subtitle = stringResource(R.string.fresh_home_superuser_sub),
                        onClick = { onOpenTab(1) },
                    )
                }

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
                    icon = Icons.Filled.SystemUpdate,
                    tint = FreshPalette.GreenDeep,
                    title = stringResource(R.string.fresh_home_tool_flash),
                    description = stringResource(R.string.fresh_home_tool_flash_desc),
                    onClick = actions.onInstallClick,
                )
                Spacer(Modifier.height(10.dp))
                FreshToolRow(
                    icon = Icons.Filled.ReceiptLong,
                    tint = FreshPalette.Amber,
                    title = stringResource(R.string.fresh_home_tool_log),
                    description = stringResource(R.string.fresh_home_tool_log_desc),
                    onClick = onOpenLog,
                )
            }
        }
    }
}

private fun freshModeValue(state: HomeUiState): String = when {
    state.lkmMode == true -> "LKM"
    state.isLateLoadMode -> "Late"
    else -> "Kernel"
}

@Composable
private fun freshStatusText(score: Int, issueCount: Int): String = when {
    score >= 95 -> stringResource(R.string.fresh_home_status_good)
    score >= 80 -> stringResource(R.string.fresh_home_status_pending, issueCount)
    else -> stringResource(R.string.fresh_home_status_bad, issueCount)
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
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null,
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
            }
        }
    }
}

@Composable
private fun FreshScoreRing(score: Int) {
    val progress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = spring(),
        label = "fresh_score_progress",
    )
    val ringColor = when {
        score >= 80 -> FreshPalette.Green
        score >= 60 -> FreshPalette.Amber
        else -> FreshPalette.Red
    }
    val trackColor = MaterialTheme.colorScheme.primaryContainer
    val trackWidth = 22.dp
    val ringSize = 208.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val stroke = trackWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(ringColor.copy(alpha = 0.55f), ringColor),
                    center = center,
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.fresh_home_score_unit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FreshPrimaryButton(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = FreshPalette.Green,
        contentColor = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
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
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
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
private fun FreshInfoCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
