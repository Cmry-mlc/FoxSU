package com.kuaimaosu.manager.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AutoFixHigh
import androidx.compose.material.icons.twotone.ExpandMore
import androidx.compose.material.icons.twotone.FileOpen
import androidx.compose.material.icons.twotone.FileUpload
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.twotone.Adb
import androidx.compose.material.icons.twotone.Backup
import androidx.compose.material.icons.twotone.Bolt
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material.icons.twotone.Science
import androidx.compose.material.icons.twotone.SwapVert
import androidx.compose.material.icons.twotone.Terminal
import com.kuaimaosu.manager.ui.component.KuaiMaoBackCapsule
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoCheckMark
import com.kuaimaosu.manager.ui.component.KuaiMaoPageTitle
import com.kuaimaosu.manager.ui.component.KuaiMaoPrimaryButton
import com.kuaimaosu.manager.ui.component.KuaiMaoRow
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.component.KuaiMaoSegmentedTabs
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoType
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.domain.model.LkmSelection
import com.kuaimaosu.manager.ui.component.DialogHandle
import com.kuaimaosu.manager.ui.component.HorizontalPagerWithInteraction
import com.kuaimaosu.manager.ui.component.rememberConfirmDialog
import com.kuaimaosu.manager.ui.component.rememberCustomDialog
import com.kuaimaosu.manager.ui.component.settings.AppBackButton
import com.kuaimaosu.manager.ui.component.settings.SegmentedColumn
import com.kuaimaosu.manager.ui.component.settings.SettingsBaseWidget
import com.kuaimaosu.manager.ui.component.settings.SettingsChooseDialog
import com.kuaimaosu.manager.ui.component.settings.SettingsChooseWidget
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.navigation.Route
import com.kuaimaosu.manager.ui.rememberMaterial3BlurBackdrop
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.theme.blurEffect
import com.kuaimaosu.manager.ui.theme.blurSource
import com.kuaimaosu.manager.ui.util.LocalBlurState
import com.kuaimaosu.manager.ui.util.adaptiveScaffoldWindowInsets
import com.kuaimaosu.manager.ui.viewmodel.InstallUiEvent
import com.kuaimaosu.manager.ui.viewmodel.InstallViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstallScreen(
    preselectedKernelUri: String? = null,
) {
    val viewModel = koinViewModel<InstallViewModel>()
    val installState by viewModel.state.collectAsStateWithLifecycle()
    val environment = installState.environment
    val themeConfig: ThemeConfig = koinInject()
    val context = LocalContext.current

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val isGKI = environment.isGki

    LaunchedEffect(installState.loading, isGKI, preselectedKernelUri) {
        if (installState.loading) return@LaunchedEffect

        when {
            preselectedKernelUri != null -> pagerState.scrollToPage(1)
            !isGKI -> pagerState.scrollToPage(0)
        }
    }

    val failedReboot = stringResource(R.string.failed_reboot)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is InstallUiEvent.Error -> {
                    val message = event.message.ifBlank { failedReboot }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val lkmTitle = stringResource(R.string.Lkm_install_methods)
    val ak3Title = stringResource(R.string.GKI_install_methods)

    Scaffold(
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        topBar = {},
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blurSource()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(6.dp))
                KuaiMaoBackCapsule(onClick = { navigator.pop() })
                Spacer(modifier = Modifier.height(14.dp))
                KuaiMaoPageTitle(
                    title = "安装镜像",
                    subtitle = if (isGKI) {
                        "GKI 内核 · 选择 LKM 或 AnyKernel3 镜像写入"
                    } else {
                        "非 GKI 内核 · 使用 AnyKernel3 镜像写入"
                    },
                )
                Spacer(modifier = Modifier.height(14.dp))
                KuaiMaoSegmentedTabs(
                    options = listOf(lkmTitle, ak3Title),
                    selectedIndex = pagerState.currentPage,
                    onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPagerWithInteraction(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                CompositionLocalProvider(
                    LocalBlurState provides rememberMaterial3BlurBackdrop(
                        enableBlur = themeConfig.isEnableBlur,
                        pagerState = pagerState,
                        pagerPage = page,
                    ),
                ) {
                    if (installState.loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding()),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingIndicator()
                        }
                    } else {
                        val pageBottom = innerPadding.calculateBottomPadding() + 28.dp
                        when (page) {
                            0 -> LKMInstallPage(
                                modifier = Modifier.fillMaxSize(),
                                topPadding = 4.dp,
                                bottomPadding = pageBottom,
                                isGKI = environment.isGki,
                                rootAvailable = environment.rootAvailable,
                                isAbDevice = environment.isAbDevice,
                                currentKmi = environment.currentKmi,
                                supportedKmis = environment.supportedKmis,
                                activeSlotSuffix = environment.activeSlotSuffix,
                                inactiveSlotSuffix = environment.inactiveSlotSuffix,
                                availablePartitions = environment.availablePartitions,
                                defaultPartition = environment.defaultPartition,
                            )

                            1 -> Anykernel3InstallPage(
                                modifier = Modifier.fillMaxSize(),
                                topPadding = 4.dp,
                                bottomPadding = pageBottom,
                                rootAvailable = environment.rootAvailable,
                                isAbDevice = environment.isAbDevice,
                                activeSlotSuffix = environment.activeSlotSuffix,
                                preselectedKernelUri = preselectedKernelUri,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LKMInstallPage(
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    isGKI: Boolean,
    rootAvailable: Boolean,
    isAbDevice: Boolean,
    currentKmi: String,
    supportedKmis: List<String>,
    activeSlotSuffix: String,
    inactiveSlotSuffix: String,
    availablePartitions: List<String>,
    defaultPartition: String,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val selectFileTip = stringResource(
        id = R.string.select_file_tip,
        defaultPartition,
    )
    val installOnlySupportKoFile = stringResource(R.string.install_only_support_ko_file)
    val dialogTitle = stringResource(id = android.R.string.dialog_alert_title)
    val dialogContent = stringResource(id = R.string.install_inactive_slot_warning)

    var lkmInstallMethod by remember { mutableStateOf<InstallMethod?>(null) }
    var lkmSelection by remember { mutableStateOf<LkmSelection>(LkmSelection.KmiNone) }
    var lkmFileName by remember { mutableStateOf<String?>(null) }
    var allowShell by remember { mutableStateOf(false) }
    var enableAdb by remember { mutableStateOf(false) }
    var forceBackup by remember { mutableStateOf(false) }
    var partitionSelectionIndex by remember { mutableIntStateOf(0) }
    var hasCustomSelected by remember { mutableStateOf(false) }
    var advancedOptionsShown by remember { mutableStateOf(false) }

    val advRotation by animateFloatAsState(
        targetValue = if (advancedOptionsShown) 180f else 0f,
        label = "AdvRotation",
    )

    val lkmMethods = remember(rootAvailable, isAbDevice, isGKI, selectFileTip) {
        buildList {
            add(InstallMethod.SelectFile(summary = selectFileTip))
            if (isGKI && rootAvailable) {
                add(InstallMethod.DirectInstall)
                if (isAbDevice) {
                    add(InstallMethod.DirectInstallToInactiveSlot)
                }
            }
        }
    }

    val confirmDialog = rememberConfirmDialog(
        onConfirm = {
            lkmInstallMethod = InstallMethod.DirectInstallToInactiveSlot
        },
        onDismiss = null,
    )

    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                lkmInstallMethod = InstallMethod.SelectFile(
                    uri,
                    summary = selectFileTip,
                )
            }
        }
    }

    val selectLkmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                val isKo = isKoFile(context, uri)
                if (isKo) {
                    lkmSelection = LkmSelection.LkmUri(uri.toString())
                    lkmFileName = uri.toString()
                } else {
                    lkmSelection = LkmSelection.KmiNone
                    lkmFileName = null
                    Toast.makeText(
                        context,
                        installOnlySupportKoFile,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    val onMethodClick = { option: InstallMethod ->
        when (option) {
            is InstallMethod.SelectFile -> {
                selectImageLauncher.launch(
                    Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "application/*"
                        putExtra(
                            Intent.EXTRA_MIME_TYPES,
                            arrayOf("application/octet-stream", "application/zip"),
                        )
                    },
                )
            }

            is InstallMethod.DirectInstall -> {
                lkmInstallMethod = option
            }

            is InstallMethod.DirectInstallToInactiveSlot -> {
                confirmDialog.showConfirm(dialogTitle, dialogContent)
            }

            is InstallMethod.HorizonKernel -> Unit
        }
    }

    val onLkmInstall = {
        lkmInstallMethod?.let { method ->
            when (method) {
                is InstallMethod.SelectFile,
                is InstallMethod.DirectInstall,
                is InstallMethod.DirectInstallToInactiveSlot,
                -> {
                    navigator.push(
                        Route.Flash.boot(
                            bootUri = if (method is InstallMethod.SelectFile) {
                                method.uri?.toString()
                            } else {
                                null
                            },
                            lkmUri = (lkmSelection as? LkmSelection.LkmUri)?.uri,
                            kmi = (lkmSelection as? LkmSelection.KmiString)?.value,
                            ota = method is InstallMethod.DirectInstallToInactiveSlot,
                            partition = availablePartitions.getOrNull(partitionSelectionIndex),
                            allowShell = allowShell,
                            enableAdb = enableAdb,
                            forceBackup = if (method is InstallMethod.SelectFile) forceBackup else false,
                        ),
                    )
                }

                is InstallMethod.HorizonKernel -> Unit
            }
        }
        Unit
    }

    val selectKmiDialog = rememberSelectKmiDialog(supportedKmis) { kmi ->
        kmi?.let {
            lkmSelection = LkmSelection.KmiString(it)
            onLkmInstall()
        }
    }

    val onClickNext = {
        if (isGKI && lkmSelection == LkmSelection.KmiNone && currentKmi.isBlank()) {
            selectKmiDialog.show()
        } else {
            onLkmInstall()
        }
    }

    val isOta = lkmInstallMethod is InstallMethod.DirectInstallToInactiveSlot
    val suffix = if (isOta) inactiveSlotSuffix else activeSlotSuffix
    val displayPartitions = availablePartitions.map { name ->
        if (defaultPartition == name) "$name (default)" else name
    }
    val defaultIndex =
        availablePartitions.indexOf(defaultPartition).takeIf { it >= 0 } ?: 0

    if (!hasCustomSelected && partitionSelectionIndex != defaultIndex) {
        partitionSelectionIndex = defaultIndex
    }

    val canSelectPartition =
        lkmInstallMethod is InstallMethod.DirectInstall ||
            lkmInstallMethod is InstallMethod.DirectInstallToInactiveSlot

    var showPartitionDialog by remember { mutableStateOf(false) }

    if (showPartitionDialog) {
        SettingsChooseDialog(
            show = true,
            title = "${stringResource(R.string.install_select_partition)} ($suffix)",
            items = displayPartitions,
            selectedIndex = partitionSelectionIndex,
            onDismiss = { showPartitionDialog = false },
            onSelectedIndexChange = {
                hasCustomSelected = true
                partitionSelectionIndex = it
                showPartitionDialog = false
            },
        )
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp),
    ) {
        item { Spacer(modifier = Modifier.height(topPadding)) }

        item {
            Text(
                text = "安装方式",
                style = KuaiMaoType.sectionTitle,
                modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 10.dp),
            )
        }

        item {
            KuaiMaoCard {
                lkmMethods.forEachIndexed { index, method ->
                    if (index > 0) KuaiMaoRowDivider()
                    val selected = method.javaClass == lkmInstallMethod?.javaClass
                    KuaiMaoRow(
                        title = stringResource(id = method.label),
                        description = method.summary,
                        icon = when (method) {
                            is InstallMethod.SelectFile -> Icons.TwoTone.FileOpen
                            is InstallMethod.DirectInstall -> Icons.TwoTone.Bolt
                            else -> Icons.TwoTone.SwapVert
                        },
                        iconTint = if (selected) KuaiMaoColors.brand else KuaiMaoColors.textSec,
                        iconContainer = if (selected) KuaiMaoColors.brandSoft else KuaiMaoColors.canvasSubtle,
                        selected = selected,
                        onClick = { onMethodClick(method) },
                    )
                }
            }
        }

        item {
            Text(
                text = "高级选项",
                style = KuaiMaoType.sectionTitle,
                modifier = Modifier.padding(start = 2.dp, top = 20.dp, bottom = 10.dp),
            )
        }

        item {
            KuaiMaoCard {
                KuaiMaoRow(
                    icon = Icons.TwoTone.Settings,
                    title = stringResource(R.string.advanced_options),
                    onClick = { advancedOptionsShown = !advancedOptionsShown },
                    trailing = {
                        Icon(
                            imageVector = Icons.TwoTone.ExpandMore,
                            contentDescription = null,
                            tint = KuaiMaoColors.chevron,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = advRotation },
                        )
                    },
                )

                AnimatedVisibility(visible = advancedOptionsShown) {
                    Column {
                        if (canSelectPartition && displayPartitions.isNotEmpty()) {
                            KuaiMaoRowDivider()
                            KuaiMaoRow(
                                icon = Icons.TwoTone.AutoFixHigh,
                                title = "${stringResource(R.string.install_select_partition)} ($suffix)",
                                description = displayPartitions.getOrNull(partitionSelectionIndex),
                                onClick = { showPartitionDialog = true },
                                trailing = {
                                    Icon(
                                        imageVector = Icons.TwoTone.ChevronRight,
                                        contentDescription = null,
                                        tint = KuaiMaoColors.chevron,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                        }

                        val hasLkmUri = lkmSelection is LkmSelection.LkmUri
                        KuaiMaoRowDivider()
                        KuaiMaoRow(
                            icon = Icons.TwoTone.FileOpen,
                            title = stringResource(R.string.install_upload_lkm_file),
                            description = if (hasLkmUri) {
                                lkmFileName.orEmpty()
                            } else {
                                stringResource(R.string.install_upload_lkm_file_summary)
                            },
                            selected = hasLkmUri,
                            onClick = {
                                if (hasLkmUri) {
                                    lkmSelection = LkmSelection.KmiNone
                                    lkmFileName = null
                                } else {
                                    selectLkmLauncher.launch(
                                        Intent(Intent.ACTION_GET_CONTENT).apply {
                                            type = "application/octet-stream"
                                        },
                                    )
                                }
                            },
                        )

                        KuaiMaoRowDivider()
                        KuaiMaoRow(
                            icon = Icons.TwoTone.Terminal,
                            title = stringResource(R.string.allow_shell),
                            description = stringResource(R.string.allow_shell_summary),
                            onClick = { allowShell = !allowShell },
                            trailing = { KuaiMaoCheckMark(checked = allowShell) },
                        )

                        KuaiMaoRowDivider()
                        KuaiMaoRow(
                            icon = Icons.TwoTone.Adb,
                            title = stringResource(R.string.enable_adb),
                            description = stringResource(R.string.enable_adb_summary),
                            onClick = { enableAdb = !enableAdb },
                            trailing = { KuaiMaoCheckMark(checked = enableAdb) },
                        )

                        if (lkmInstallMethod is InstallMethod.SelectFile) {
                            KuaiMaoRowDivider()
                            KuaiMaoRow(
                                icon = Icons.TwoTone.Backup,
                                title = stringResource(R.string.install_force_backup),
                                description = stringResource(R.string.install_force_backup_summary),
                                onClick = { forceBackup = !forceBackup },
                                trailing = { KuaiMaoCheckMark(checked = forceBackup) },
                            )
                        }
                    }
                }
            }
        }

        item {
            KuaiMaoPrimaryButton(
                text = stringResource(R.string.install_next),
                enabled = lkmInstallMethod != null,
                onClick = onClickNext,
                modifier = Modifier.padding(top = 20.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}

@Composable
private fun Anykernel3InstallPage(
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    rootAvailable: Boolean,
    isAbDevice: Boolean,
    activeSlotSuffix: String,
    preselectedKernelUri: String?,
) {
    val navigator = LocalNavigator.current
    val summary = stringResource(R.string.horizon_kernel_summary)
    var ak3InstallMethod by remember { mutableStateOf<InstallMethod?>(null) }
    var skipKsud by remember { mutableStateOf(false) }
    var showSlotSelectionDialog by remember { mutableStateOf(false) }
    var advancedOptionsShown by remember { mutableStateOf(false) }

    val ak3AdvRotation by animateFloatAsState(
        targetValue = if (advancedOptionsShown) 180f else 0f,
        label = "Ak3AdvRotation",
    )

    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                ak3InstallMethod = InstallMethod.HorizonKernel(uri = uri, summary = summary)
            }
        }
    }

    LaunchedEffect(preselectedKernelUri, isAbDevice) {
        preselectedKernelUri?.let { uriString ->
            try {
                val preselectedUri = uriString.toUri()
                ak3InstallMethod = InstallMethod.HorizonKernel(
                    uri = preselectedUri,
                    summary = summary,
                )
            } catch (_: Exception) {
            }
        }
    }

    val onClickNext = {
        (ak3InstallMethod as? InstallMethod.HorizonKernel)?.let { method ->
            method.uri?.let { uri ->
                if (isAbDevice) {
                    showSlotSelectionDialog = true
                } else {
                    navigator.push(
                        Route.KernelFlash(
                            kernelUri = uri.toString(),
                            selectedSlot = null,
                            skipKsud = skipKsud,
                        ),
                    )
                }
            }
        }
    }

    SettingsChooseDialog(
        show = showSlotSelectionDialog && isAbDevice,
        title = stringResource(R.string.select_slot_title),
        items = listOf(stringResource(R.string.slot_a), stringResource(R.string.slot_b)),
        selectedIndex = when (activeSlotSuffix.removePrefix("_")) {
            "b" -> 1
            else -> 0
        },
        onDismiss = { showSlotSelectionDialog = false },
        onSelectedIndexChange = { index ->
            val method = ak3InstallMethod as? InstallMethod.HorizonKernel
            method?.uri?.let { uri ->
                navigator.push(
                    Route.KernelFlash(
                        kernelUri = uri.toString(),
                        selectedSlot = if (index == 1) "b" else "a",
                        skipKsud = skipKsud,
                    ),
                )
            }
        },
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp),
    ) {
        item { Spacer(modifier = Modifier.height(topPadding)) }

        item {
            Text(
                text = "镜像来源",
                style = KuaiMaoType.sectionTitle,
                modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 10.dp),
            )
        }

        item {
            KuaiMaoCard {
                if (rootAvailable) {
                    val horizonSelected = ak3InstallMethod is InstallMethod.HorizonKernel
                    KuaiMaoRow(
                        icon = Icons.TwoTone.FileUpload,
                        title = stringResource(R.string.GKI_install_methods),
                        description = stringResource(R.string.ak3_select_zip),
                        iconTint = if (horizonSelected) KuaiMaoColors.brand else KuaiMaoColors.textSec,
                        iconContainer = if (horizonSelected) KuaiMaoColors.brandSoft else KuaiMaoColors.canvasSubtle,
                        selected = horizonSelected,
                        onClick = {
                            selectImageLauncher.launch(
                                Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "application/*"
                                    putExtra(
                                        Intent.EXTRA_MIME_TYPES,
                                        arrayOf("application/octet-stream", "application/zip"),
                                    )
                                },
                            )
                        },
                    )
                } else {
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Lock,
                        title = "需要 Root 权限",
                        description = "AnyKernel3 需要 root 才能写入分区，请先完成安装或越狱。",
                        iconTint = KuaiMaoColors.warning,
                        iconContainer = KuaiMaoColors.warningSoft,
                        enabled = false,
                    )
                }
            }
        }

        item {
            Text(
                text = "高级选项",
                style = KuaiMaoType.sectionTitle,
                modifier = Modifier.padding(start = 2.dp, top = 20.dp, bottom = 10.dp),
            )
        }

        item {
            KuaiMaoCard {
                KuaiMaoRow(
                    icon = Icons.TwoTone.Settings,
                    title = stringResource(R.string.advanced_options),
                    onClick = { advancedOptionsShown = !advancedOptionsShown },
                    trailing = {
                        Icon(
                            imageVector = Icons.TwoTone.ExpandMore,
                            contentDescription = null,
                            tint = KuaiMaoColors.chevron,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = ak3AdvRotation },
                        )
                    },
                )

                AnimatedVisibility(visible = advancedOptionsShown) {
                    Column {
                        KuaiMaoRowDivider()
                        KuaiMaoRow(
                            icon = Icons.TwoTone.Science,
                            title = stringResource(id = R.string.skip_ksud),
                            description = stringResource(id = R.string.skip_ksud_summary),
                            onClick = { skipKsud = !skipKsud },
                            trailing = { KuaiMaoCheckMark(checked = skipKsud) },
                        )
                    }
                }
            }
        }

        item {
            KuaiMaoPrimaryButton(
                text = stringResource(R.string.install_next),
                enabled = ak3InstallMethod != null,
                onClick = { onClickNext() },
                modifier = Modifier.padding(top = 20.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberSelectKmiDialog(
    supportedKmi: List<String>,
    onSelected: (String?) -> Unit,
): DialogHandle = rememberCustomDialog { dismiss ->
    SettingsChooseDialog(
        show = true,
        title = stringResource(R.string.select_kmi),
        items = supportedKmi,
        selectedIndex = -1,
        onDismiss = dismiss,
        onSelectedIndexChange = { index ->
            onSelected(supportedKmi.getOrNull(index))
        },
    )
}

private fun isKoFile(context: Context, uri: Uri): Boolean {
    val seg = uri.lastPathSegment ?: ""
    if (seg.endsWith(".ko", ignoreCase = true)) return true

    return try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx != -1 && cursor.moveToFirst()) {
                val name = cursor.getString(idx)
                name?.endsWith(".ko", ignoreCase = true) == true
            } else {
                false
            }
        } ?: false
    } catch (_: Throwable) {
        false
    }
}

@Preview
@Composable
private fun SelectInstallPreview() {
    InstallScreen()
}

sealed class InstallMethod {
    data class SelectFile(
        val uri: Uri? = null,
        @param:StringRes override val label: Int = R.string.select_file,
        override val summary: String?,
    ) : InstallMethod()

    data object DirectInstall : InstallMethod() {
        override val label: Int
            get() = R.string.direct_install
    }

    data object DirectInstallToInactiveSlot : InstallMethod() {
        override val label: Int
            get() = R.string.install_inactive_slot
    }

    data class HorizonKernel(
        val uri: Uri? = null,
        val slot: String? = null,
        @param:StringRes override val label: Int = R.string.horizon_kernel,
        override val summary: String? = null,
    ) : InstallMethod()

    abstract val label: Int
    open val summary: String? = null
}
