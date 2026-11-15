package com.kuaimaosu.manager.ui.screen.main

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.FolderOff
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material.icons.twotone.Troubleshoot
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kuaimaosu.manager.BuildConfig
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.domain.usecase.GenerateBugreportUseCase
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoRow
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.component.rememberLoadingDialog
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.navigation.Route
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.ui.util.LocalSnackbarHost
import com.kuaimaosu.manager.ui.util.showReplacingSnackbar
import com.kuaimaosu.manager.ui.viewmodel.HomeViewModel
import com.kuaimaosu.manager.ui.viewmodel.SettingsUiAction
import com.kuaimaosu.manager.ui.viewmodel.SettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * 工具（设计稿 pages/tools）：日志 / 动态管理器 / Umount 路径 / 卸载镜像。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsToolsPage(bottomPadding: Dp = 0.dp) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost = LocalSnackbarHost.current
    val generateBugreport = koinInject<GenerateBugreportUseCase>()
    val loadingDialog = rememberLoadingDialog()
    val scope = rememberCoroutineScope()
    val logSaved = stringResource(R.string.log_saved)
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsViewModel.dispatch(SettingsUiAction.LoadFeatureSettings)
    }

    val exportBugreportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gzip"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            loadingDialog.show()
            context.contentResolver.openOutputStream(uri)?.use { output ->
                generateBugreport().inputStream().use { it.copyTo(output) }
            }
            loadingDialog.hide()
            snackBarHost.showReplacingSnackbar(logSaved)
        }
    }

    val lkmActive = homeState.systemStatus.lkmMode == true && !homeState.systemStatus.isLateLoadMode
    val toolCount = 3 + if (lkmActive) 1 else 0

    KuaiMaoSettingsScaffold(
        title = "工具",
        subtitle = "$toolCount 项 · 诊断与底层配置",
        bottomPadding = bottomPadding,
    ) {
        item {
            KuaiMaoCard {
                KuaiMaoRow(
                    icon = Icons.TwoTone.Troubleshoot,
                    title = stringResource(R.string.send_log),
                    description = "导出最近一次启动的完整日志",
                    iconTint = KuaiMaoTones.rose.content,
                    iconContainer = KuaiMaoTones.rose.container,
                    onClick = { showBottomSheet = true },
                    trailing = {
                        Icon(
                            imageVector = Icons.TwoTone.ChevronRight,
                            contentDescription = null,
                            tint = KuaiMaoColors.chevron,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )

                KuaiMaoRowDivider()
                KuaiMaoRow(
                    icon = Icons.TwoTone.Security,
                    title = stringResource(R.string.dynamic_manager_title),
                    description = stringResource(R.string.dynamic_manager_settings_summary),
                    iconTint = KuaiMaoTones.blue.content,
                    iconContainer = KuaiMaoTones.blue.container,
                    onClick = { navigator.push(Route.DynamicManager) },
                    trailing = {
                        Icon(
                            imageVector = Icons.TwoTone.ChevronRight,
                            contentDescription = null,
                            tint = KuaiMaoColors.chevron,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )

                KuaiMaoRowDivider()
                KuaiMaoRow(
                    icon = Icons.TwoTone.FolderOff,
                    title = stringResource(R.string.umount_path_manager),
                    description = if (uiState.isKernelUmountEnabled) {
                        stringResource(R.string.umount_path_manager_summary)
                    } else {
                        "需先在「通用与 Root」开启内核处理卸载模块"
                    },
                    iconTint = KuaiMaoTones.amber.content,
                    iconContainer = KuaiMaoTones.amber.container,
                    enabled = uiState.isKernelUmountEnabled,
                    onClick = { navigator.push(Route.UmountManager) },
                    trailing = {
                        Icon(
                            imageVector = Icons.TwoTone.ChevronRight,
                            contentDescription = null,
                            tint = KuaiMaoColors.chevron,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }

        if (lkmActive) {
            item {
                Text(
                    text = "镜像",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.5.sp,
                    color = KuaiMaoColors.text,
                    modifier = Modifier.padding(start = 2.dp, top = 20.dp, bottom = 10.dp),
                )
            }
            item {
                KuaiMaoCard {
                    UninstallItem {
                        loadingDialog.withLoading(it)
                    }
                }
            }
        }

        item {
            Text(
                text = "日志仅包含管理器与本机内核状态，不含个人数据",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = KuaiMaoColors.textMuted,
                modifier = Modifier.padding(start = 2.dp, top = 14.dp),
            )
        }
    }

    if (showBottomSheet) {
        val sendLog = stringResource(R.string.send_log)
        LogBottomSheet(
            onDismiss = { showBottomSheet = false },
            onSaveLog = {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                val current = LocalDateTime.now().format(formatter)
                exportBugreportLauncher.launch("KernelSU_bugreport_$current.tar.gz")
                showBottomSheet = false
            },
            onShareLog = {
                scope.launch {
                    val bugreport = loadingDialog.withLoading {
                        withContext(Dispatchers.IO) { generateBugreport() }
                    }
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                        bugreport,
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_STREAM, uri)
                        setDataAndType(uri, "application/gzip")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, sendLog))
                    showBottomSheet = false
                }
            },
        )
    }
}
