package com.loomsu.manager.ui.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.KeyboardArrowLeft
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Code
import androidx.compose.material.icons.twotone.Copyright
import androidx.compose.material.icons.twotone.Group
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.loomsu.manager.BuildConfig
import com.loomsu.manager.R
import com.loomsu.manager.domain.model.ManagerUpdateChannel
import com.loomsu.manager.domain.usecase.CheckManagerUpdateUseCase
import com.loomsu.manager.ui.component.LoomCard
import com.loomsu.manager.ui.component.LoomPageTitle
import com.loomsu.manager.ui.component.LoomPrimaryButton
import com.loomsu.manager.ui.component.LoomRow
import com.loomsu.manager.ui.component.LoomRowDivider
import com.loomsu.manager.ui.navigation.LocalNavigator
import com.loomsu.manager.ui.navigation.Navigator
import com.loomsu.manager.ui.navigation.Route
import com.loomsu.manager.ui.theme.LoomColors
import com.loomsu.manager.ui.theme.LoomType
import com.loomsu.manager.ui.theme.blurSource
import com.loomsu.manager.ui.util.LocalSnackbarHost
import com.loomsu.manager.ui.util.adaptiveScaffoldWindowInsets
import com.loomsu.manager.ui.util.showReplacingSnackbar
import com.loomsu.manager.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * 关于页（设计稿 pages/about 重做）。
 *
 * 结构：圆形返回 → 应用标识卡 → 关于应用分组 → 社区与许可分组 → 检查更新 → 版权注脚。
 * 版本号、链接、开源许可、检查更新全部保留真实行为。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackBarHost = LocalSnackbarHost.current
    val homeViewModel = koinViewModel<HomeViewModel>()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val checkManagerUpdate = koinInject<CheckManagerUpdateUseCase>()
    var checking by remember { mutableStateOf(false) }
    val status = homeState.systemStatus

    Scaffold(
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .blurSource(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            item {
                Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding() + 6.dp))
            }

            item {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(6.dp, CircleShape, clip = false, ambientColor = Color(0x1418263C), spotColor = Color(0x1418263C))
                        .clip(CircleShape)
                        .background(LoomColors.canvasCard)
                        .clickable(onClick = { navigator.pop() }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.TwoTone.KeyboardArrowLeft,
                        contentDescription = "返回",
                        tint = LoomColors.textBody,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
                LoomPageTitle(
                    title = "关于",
                    subtitle = "Loom SU · 织光内核工作台",
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 应用标识卡
            item {
                LoomCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            painter = rememberDrawablePainter(
                                drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher),
                            ),
                            contentDescription = stringResource(R.string.app_name),
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Loom SU",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.2).sp,
                                color = LoomColors.text,
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "V${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = LoomColors.textMuted,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(
                                    if (status.ksuVersion != null) LoomColors.successSoft else LoomColors.warningSoft,
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = if (status.ksuVersion != null) "已激活" else "未激活",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (status.ksuVersion != null) LoomColors.success else LoomColors.warning,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    text = "应用",
                    style = LoomType.sectionTitle,
                    modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
                )
            }

            item {
                LoomCard {
                    LoomRow(
                        icon = Icons.TwoTone.Code,
                        title = stringResource(R.string.get_source_code),
                        description = stringResource(R.string.get_source_code_detail),
                        iconTint = LoomColors.brandDeep,
                        iconContainer = LoomColors.brandSoft,
                        onClick = { uriHandler.openUri("https://github.com/Cmry-mlc/FoxSu") },
                        trailing = { Arrow() },
                    )
                    LoomRowDivider()
                    LoomRow(
                        icon = Icons.TwoTone.Group,
                        title = stringResource(R.string.join_telegram_group),
                        description = stringResource(R.string.join_telegram_group_detail),
                        iconTint = LoomColors.info,
                        iconContainer = LoomColors.infoSoft,
                        onClick = { uriHandler.openUri("https://t.me/LoomSU") },
                        trailing = { Arrow() },
                    )
                    LoomRowDivider()
                    LoomRow(
                        icon = Icons.TwoTone.Copyright,
                        title = stringResource(R.string.open_source_license),
                        description = "GPL-3.0 · 依赖组件清单",
                        iconTint = LoomColors.success,
                        iconContainer = LoomColors.successSoft,
                        onClick = { navigator.push(Route.OpenSourceLicense) },
                        trailing = { Arrow() },
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                LoomCard {
                    LoomRow(
                        icon = Icons.TwoTone.Info,
                        title = "图标与视觉",
                        description = "图标作者 OukaroMF · 点击查看主页",
                        iconTint = LoomColors.amber,
                        iconContainer = LoomColors.amberSoft,
                        onClick = { uriHandler.openUri("https://github.com/OukaroMF") },
                        trailing = { Arrow() },
                    )
                    LoomRowDivider()
                    LoomRow(
                        icon = Icons.TwoTone.Update,
                        title = "检查更新",
                        description = if (checking) "正在检查…" else "当前版本 V${BuildConfig.VERSION_NAME}",
                        iconTint = LoomColors.textSec,
                        iconContainer = LoomColors.canvasSubtle,
                        enabled = !checking,
                        onClick = {
                            checking = true
                            scope.launch {
                                val info = runCatching {
                                    checkManagerUpdate(ManagerUpdateChannel.STABLE)
                                }.getOrNull()
                                checking = false
                                snackBarHost.showReplacingSnackbar(
                                    if (info == null) {
                                        "已是最新版本 V${BuildConfig.VERSION_NAME}"
                                    } else {
                                        "发现新版本 ${info.versionName}"
                                    },
                                )
                            }
                        },
                        trailing = { Arrow() },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
                LoomPrimaryButton(
                    text = if (checking) "检查中…" else "检查更新",
                    enabled = !checking,
                    onClick = {
                        if (checking) return@LoomPrimaryButton
                        checking = true
                        scope.launch {
                            val info = runCatching {
                                checkManagerUpdate(ManagerUpdateChannel.STABLE)
                            }.getOrNull()
                            checking = false
                            snackBarHost.showReplacingSnackbar(
                                if (info == null) {
                                    "已是最新版本 V${BuildConfig.VERSION_NAME}"
                                } else {
                                    "发现新版本 ${info.versionName}"
                                },
                            )
                        }
                    },
                )
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "织光 · Loom SU · 基于 KernelSU · GPL-3.0",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = LoomColors.textMuted,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun Arrow() {
    Icon(
        imageVector = Icons.TwoTone.ChevronRight,
        contentDescription = null,
        tint = LoomColors.chevron,
        modifier = Modifier.size(16.dp),
    )
}

@Preview
@Composable
private fun AboutScreenPreview() {
    CompositionLocalProvider(
        LocalNavigator provides Navigator(Route.About),
    ) {
        AboutScreen()
    }
}
