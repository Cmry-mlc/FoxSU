package com.kuaimaosu.manager.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Error
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.NewReleases
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material.icons.twotone.Update
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuaimaosu.manager.BuildConfig
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.data.notify.KuaiMaoNotificationStore
import com.kuaimaosu.manager.Natives.KernelPatchImplementation
import com.kuaimaosu.manager.domain.model.ManagerUpdateInfo
import com.kuaimaosu.manager.ui.component.KuaiMaoBackCapsule
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoPageTitle
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.navigation.Route
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTone
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.ui.theme.blurSource
import com.kuaimaosu.manager.ui.util.adaptiveScaffoldWindowInsets
import com.kuaimaosu.manager.ui.viewmodel.HomeUiState
import com.kuaimaosu.manager.ui.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

/** 通知中心里的一条消息。id 用于记录已读，[onClick] 为空表示只读提示。 */
data class KuaiMaoNotice(
    val id: String,
    val title: String,
    val desc: String,
    val tag: String,
    val icon: ImageVector,
    val tone: KuaiMaoTone,
    val onClick: (() -> Unit)? = null,
)

/**
 * 由真实运行状态推导通知列表：版本更新、管理器签名、内核可用性、Root 与 SELinux。
 *
 * 主页与通知页共用这一份数据，保证「角标数量 = 未读条数」。
 */
@Composable
fun kuaimaoNotices(
    uiState: HomeUiState,
    onOpenUpdate: () -> Unit = {},
    onOpenInstall: () -> Unit = {},
): List<KuaiMaoNotice> {
    val status = uiState.systemStatus
    val appName = stringResource(R.string.app_name)
    val list = mutableListOf<KuaiMaoNotice>()

    // 1. 版本更新：稳定版 + 测试版
    list += managerUpdateNotice(uiState.stableManagerUpdate, "稳定版", onOpenUpdate)
    list += managerUpdateNotice(uiState.betaManagerUpdate, "测试版", onOpenUpdate)

    // 2. Root 授权异常
    if (status.ksuVersion != null && !status.isRootAvailable) {
        list += KuaiMaoNotice(
            id = "root-failed",
            title = "Root 授权异常",
            desc = stringResource(R.string.grant_root_failed),
            tag = "安全",
            icon = Icons.TwoTone.Error,
            tone = KuaiMaoTones.danger,
        )
    }

    // 3. 管理器与内核能力不匹配
    if (status.isManager && !status.isFullFeatured) {
        val message = if ((status.kernelUAPIVersion ?: 1) > status.managerUAPIVersion) {
            stringResource(R.string.require_manager_version)
        } else if (status.lkmMode == true) {
            stringResource(R.string.require_kernel_version)
        } else {
            stringResource(R.string.require_kernel_version_gki)
        }
        list += KuaiMaoNotice(
            id = "manager-outdated",
            title = "组件需要更新",
            desc = message,
            tag = "更新",
            icon = Icons.TwoTone.Update,
            tone = KuaiMaoTones.amber,
            onClick = onOpenInstall,
        )
    }

    // 4. 非官方签名
    if (!status.isOfficialSignature) {
        list += KuaiMaoNotice(
            id = "unofficial-signature",
            title = "非官方管理器",
            desc = stringResource(R.string.unofficial_version_notice, appName),
            tag = "安全",
            icon = Icons.TwoTone.Security,
            tone = KuaiMaoTones.danger,
        )
    }

    // 5. PR 构建
    if (BuildConfig.IS_PR_BUILD || status.isPrBuild) {
        list += KuaiMaoNotice(
            id = "pr-build",
            title = "测试构建",
            desc = stringResource(R.string.home_pr_build_warning),
            tag = "版本",
            icon = Icons.TwoTone.Warning,
            tone = KuaiMaoTones.amber,
        )
    }

    // 6. 与 APatch 冲突
    if (status.kernelPatchImplementation == KernelPatchImplementation.OFFICIAL) {
        list += KuaiMaoNotice(
            id = "apatch-conflict",
            title = "检测到 APatch",
            desc = stringResource(R.string.conflict_with_apatch),
            tag = "安全",
            icon = Icons.TwoTone.Warning,
            tone = KuaiMaoTones.amber,
        )
    }

    // 7. SELinux 宽容模式
    if (status.ksuVersion != null && status.isSELinuxPermissive) {
        list += KuaiMaoNotice(
            id = "selinux-permissive",
            title = "SELinux 宽容模式",
            desc = "当前处于宽容模式，Root 隐藏能力下降，建议切回强制模式。",
            tag = "安全",
            icon = Icons.TwoTone.Info,
            tone = KuaiMaoTones.rose,
        )
    }

    return list
}

@Composable
private fun managerUpdateNotice(
    info: ManagerUpdateInfo?,
    channelLabel: String,
    onOpenUpdate: () -> Unit,
): List<KuaiMaoNotice> {
    if (info == null) return emptyList()
    val changelog = info.changelog.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
    return listOf(
        KuaiMaoNotice(
            id = "update-${info.channel.name}-${info.versionCode}",
            title = "发现新版本 ${info.versionName}",
            desc = changelog.ifBlank { "$channelLabel · ${info.abi} · 点击查看更新详情" },
            tag = "更新",
            icon = Icons.TwoTone.NewReleases,
            tone = KuaiMaoTones.blue,
            onClick = onOpenUpdate,
        ),
    )
}

/** 通知中心：版本更新与安全提示集中在这里，铃铛角标 = 未读条数。 */
@Composable
fun NotificationsPage() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val viewModel = koinViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        KuaiMaoNotificationStore.load(context)
    }

    val notices = kuaimaoNotices(
        uiState = uiState,
        onOpenUpdate = { navigator.push(Route.SettingsUpdate) },
        onOpenInstall = { navigator.push(Route.Install(preselectedKernelUri = null)) },
    )
    val readIds = KuaiMaoNotificationStore.readIds

    // 离开通知页时把这批消息标记为已读，返回主页角标随即消失。
    val currentIds by rememberUpdatedState(notices.map { it.id })
    DisposableEffect(Unit) {
        onDispose { KuaiMaoNotificationStore.markAllRead(context, currentIds) }
    }

    Scaffold(
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        // 返回胶囊 + 标题固定，列表滚动时保持不动
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                KuaiMaoBackCapsule(text = "返回", onClick = { navigator.pop() })
                Spacer(modifier = Modifier.height(14.dp))
                KuaiMaoPageTitle(
                    title = "通知",
                    subtitle = if (notices.isEmpty()) {
                        "版本更新与安全提示"
                    } else {
                        "${notices.size} 条消息 · 版本更新与安全提示"
                    },
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
            if (notices.isEmpty()) {
                item {
                    KuaiMaoCard(contentPadding = PaddingValues(horizontal = 22.dp, vertical = 40.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(KuaiMaoTones.unified.container),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.TwoTone.NewReleases,
                                    contentDescription = null,
                                    tint = KuaiMaoTones.unified.content,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "暂无通知",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = KuaiMaoColors.textBody,
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "有新的版本更新或安全提示时会出现在这里",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = KuaiMaoColors.textMuted,
                            )
                        }
                    }
                }
            } else {
                items(notices, key = { it.id }) { notice ->
                    NoticeCard(
                        notice = notice,
                        unread = notice.id !in readIds,
                        onDismiss = { KuaiMaoNotificationStore.markRead(context, listOf(notice.id)) },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "更新检查可在「设置 · 更新」中调整",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = KuaiMaoColors.textMuted,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun NoticeCard(
    notice: KuaiMaoNotice,
    unread: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    KuaiMaoCard(
        modifier = if (notice.onClick != null) Modifier.clickable {
            onDismiss()
            notice.onClick.invoke()
        } else {
            Modifier
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(notice.tone.container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = notice.icon,
                    contentDescription = null,
                    tint = notice.tone.content,
                    modifier = Modifier.size(19.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notice.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        color = KuaiMaoColors.textBody,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (unread) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notice.desc,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp,
                    color = KuaiMaoColors.textSec,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(notice.tone.container)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = notice.tag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = notice.tone.content,
                    )
                }
            }

            if (notice.onClick != null) {
                Icon(
                    imageVector = Icons.TwoTone.ChevronRight,
                    contentDescription = null,
                    tint = KuaiMaoColors.chevron,
                    modifier = Modifier
                        .padding(start = 8.dp, top = 12.dp)
                        .size(16.dp),
                )
            }
        }
    }
}
