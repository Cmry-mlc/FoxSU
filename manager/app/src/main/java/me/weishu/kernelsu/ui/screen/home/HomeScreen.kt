package me.weishu.kernelsu.ui.screen.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.magica.MagicaService
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.dialog.rememberLoadingDialog
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.screen.flash.FlashIt
import me.weishu.kernelsu.ui.util.LkmSelection
import me.weishu.kernelsu.ui.util.checkNewVersion
import me.weishu.kernelsu.ui.viewmodel.HomeViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun HomePager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
    navigationBadge: NavigationBadgeState = NavigationBadgeState(),
) {
    val viewModel = viewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val loadingDialog = rememberLoadingDialog()
    val scope = rememberCoroutineScope()
    var pendingUpdateUrl by remember { mutableStateOf("") }
    val updateDialog = rememberConfirmDialog(
        onConfirm = {
            val url = pendingUpdateUrl
            if (url.isNotEmpty()) uriHandler.openUri(url)
        },
    )
    val latestIsCurrentPage by rememberUpdatedState(isCurrentPage)
    val initialResumeHandled = rememberSaveable { mutableStateOf(false) }

    var hasActivated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage && !hasActivated) {
            hasActivated = true
            viewModel.refresh()
        }
    }

    LifecycleResumeEffect(Unit) {
        if (initialResumeHandled.value && latestIsCurrentPage) {
            viewModel.refresh()
        }
        initialResumeHandled.value = true
        onPauseOrDispose { }
    }

    val actions = HomeActions(
        onInstallClick = { navigator.push(Route.Install) },
        onOpenUrl = uriHandler::openUri,
        onJailbreakClick = {
            loadingDialog.showLoading()
            context.startService(Intent(context, MagicaService::class.java))
            // Manager will be force-stopped and restarted by late-load on success.
            // If that doesn't happen within timeout, jailbreak likely failed.
            scope.launch(Dispatchers.IO) {
                delay(30_000.milliseconds)
                withContext(Dispatchers.Main) {
                    loadingDialog.hide()
                    Toast.makeText(context, R.string.jailbreak_timeout, Toast.LENGTH_LONG).show()
                }
            }
        },
        onCheckUpdate = {
            scope.launch {
                loadingDialog.showLoading()
                val latest = withContext(Dispatchers.IO) { checkNewVersion() }
                loadingDialog.hide()
                val current = uiState.currentManagerVersionCode
                if (latest.versionCode > current && latest.downloadUrl.isNotEmpty()) {
                    pendingUpdateUrl = latest.downloadUrl
                    updateDialog.showConfirm(
                        title = context.getString(R.string.module_changelog),
                        content = latest.changelog.ifEmpty {
                            context.getString(R.string.new_version_available, latest.versionCode)
                        },
                        markdown = true,
                        confirm = context.getString(R.string.module_update),
                    )
                } else {
                    Toast.makeText(
                        context,
                        R.string.home_no_update_found,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        },
        // 刷新镜像：真实执行 ksud boot-patch -f（走标准刷写页，可看实时日志）
        onRefreshImage = {
            navigator.push(
                Route.Flash(
                    FlashIt.FlashBoot(
                        boot = null,
                        lkm = LkmSelection.KmiNone,
                        ota = false,
                    )
                )
            )
        },
    )

    val mainPagerState = LocalMainPagerState.current

    when (LocalUiMode.current) {
        UiMode.Miuix -> me.weishu.kernelsu.ui.screen.ag.AgHome(
            state = uiState,
            actions = actions,
            onOpenTab = { page -> mainPagerState.animateToPage(page) },
            onOpenAuthor = { navigator.push(Route.Author) },
        )

        UiMode.Material -> me.weishu.kernelsu.ui.screen.editorial.EditorialHome(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
            navigationBadge = navigationBadge,
            onOpenTab = { page -> mainPagerState.animateToPage(page) },
            onOpenAuthor = { navigator.push(Route.Author) },
        )

        UiMode.Fresh -> HomePagerFresh(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
            navigationBadge = navigationBadge,
            onOpenTab = { page -> mainPagerState.animateToPage(page) },
        )

        UiMode.Glass -> me.weishu.kernelsu.ui.screen.glass.GlassHome(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
            navigationBadge = navigationBadge,
            onOpenTab = { page -> mainPagerState.animateToPage(page) },
            onOpenAuthor = { navigator.push(Route.Author) },
        )

        UiMode.Nocturne -> me.weishu.kernelsu.ui.screen.nocturne.NocturneHome(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
            navigationBadge = navigationBadge,
            onOpenTab = { page -> mainPagerState.animateToPage(page) },
            onOpenAuthor = { navigator.push(Route.Author) },
        )
    }
}
