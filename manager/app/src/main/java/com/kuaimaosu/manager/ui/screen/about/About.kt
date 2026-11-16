package com.kuaimaosu.manager.ui.screen.about

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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.twotone.Difference
import androidx.compose.material.icons.twotone.Favorite
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
import com.kuaimaosu.manager.BuildConfig
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.domain.model.ManagerUpdateChannel
import com.kuaimaosu.manager.domain.usecase.CheckManagerUpdateUseCase
import com.kuaimaosu.manager.ui.component.KuaiMaoBackCapsule
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoPageTitle
import com.kuaimaosu.manager.ui.component.KuaiMaoPrimaryButton
import com.kuaimaosu.manager.ui.component.KuaiMaoRow
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.navigation.Navigator
import com.kuaimaosu.manager.ui.navigation.Route
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoType
import com.kuaimaosu.manager.ui.theme.blurSource
import com.kuaimaosu.manager.ui.util.LocalSnackbarHost
import com.kuaimaosu.manager.ui.util.adaptiveScaffoldWindowInsets
import com.kuaimaosu.manager.ui.util.showReplacingSnackbar
import com.kuaimaosu.manager.ui.viewmodel.HomeViewModel
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
        // 返回胶囊 + 标题固定在顶部，下面只有卡片滚动
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
                    title = "关于",
                    subtitle = "KuaiMaoSU · 快猫内核工作台",
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .blurSource(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            // 应用标识卡
            item {
                KuaiMaoCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            painter = rememberDrawablePainter(
                                drawable = ContextCompat.getDrawable(context, R.drawable.ic_brand_logo),
                            ),
                            contentDescription = stringResource(R.string.app_name),
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "KuaiMaoSU",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.2).sp,
                                color = KuaiMaoColors.text,
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "V${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = KuaiMaoColors.textMuted,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(
                                    if (status.ksuVersion != null) KuaiMaoColors.successSoft else KuaiMaoColors.warningSoft,
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = if (status.ksuVersion != null) "已激活" else "未激活",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (status.ksuVersion != null) KuaiMaoColors.success else KuaiMaoColors.warning,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    text = "应用",
                    style = KuaiMaoType.sectionTitle,
                    modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
                )
            }

            item {
                KuaiMaoCard {
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Code,
                        title = stringResource(R.string.get_source_code),
                        description = stringResource(R.string.get_source_code_detail),
                        onClick = { uriHandler.openUri("https://github.com/Cmry-mlc/FoxSu") },
                        trailing = { Arrow() },
                    )
                    KuaiMaoRowDivider()
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Group,
                        title = stringResource(R.string.join_telegram_group),
                        description = stringResource(R.string.join_telegram_group_detail),
                        onClick = { uriHandler.openUri("https://t.me/KuaiMaoSU") },
                        trailing = { Arrow() },
                    )
                    KuaiMaoRowDivider()
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Copyright,
                        title = stringResource(R.string.open_source_license),
                        description = "GPL-3.0 · 依赖组件清单",
                        onClick = { navigator.push(Route.OpenSourceLicense) },
                        trailing = { Arrow() },
                    )
                    KuaiMaoRowDivider()
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Favorite,
                        title = "开源致谢",
                        description = "上游项目 · 作者头像 · 仓库与协议",
                        onClick = { navigator.push(Route.Credits) },
                        trailing = { Arrow() },
                    )
                    KuaiMaoRowDivider()
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Difference,
                        title = "修改清单",
                        description = "相对 BakaSU 的全部改动，按模块列出",
                        onClick = { navigator.push(Route.Modifications) },
                        trailing = { Arrow() },
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                KuaiMaoCard {
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Info,
                        title = "图标与视觉",
                        description = "图标作者 尘梦如烟",
                    )
                    KuaiMaoRowDivider()
                    KuaiMaoRow(
                        icon = Icons.TwoTone.Update,
                        title = "检查更新",
                        description = if (checking) "正在检查…" else "当前版本 V${BuildConfig.VERSION_NAME}",
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
                KuaiMaoPrimaryButton(
                    text = if (checking) "检查中…" else "检查更新",
                    enabled = !checking,
                    onClick = {
                        if (checking) return@KuaiMaoPrimaryButton
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
                    text = "快猫 · KuaiMaoSU · 基于 BakaSU · GPL-3.0",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = KuaiMaoColors.textMuted,
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
        tint = KuaiMaoColors.chevron,
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
