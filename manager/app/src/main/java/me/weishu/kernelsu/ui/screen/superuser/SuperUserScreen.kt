package me.weishu.kernelsu.ui.screen.superuser

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.ui.component.SearchStatus
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.viewmodel.SuperUserViewModel

@Composable
fun SuperUserPager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val viewModel = viewModel<SuperUserViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestIsCurrentPage by rememberUpdatedState(isCurrentPage)
    val initialResumeHandled = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            val state = viewModel.uiState.value
            if (!state.hasLoaded && !state.isRefreshing) {
                viewModel.initializePreferences()
                viewModel.loadAppList()
            }
        } else if (!uiState.searchStatus.isCollapsed()) {
            viewModel.updateSearchStatus(uiState.searchStatus.copy(searchText = "", current = SearchStatus.Status.COLLAPSED))
        }
    }

    LifecycleResumeEffect(Unit) {
        if (initialResumeHandled.value && latestIsCurrentPage) {
            val state = viewModel.uiState.value
            if (!state.isRefreshing) {
                if (!state.hasLoaded) {
                    viewModel.initializePreferences()
                    viewModel.loadAppList()
                } else if (viewModel.isNeedRefresh) {
                    viewModel.loadAppList(resort = false)
                }
            }
        }
        initialResumeHandled.value = true
        onPauseOrDispose {}
    }

    val onSearchTextChange: (String) -> Unit = viewModel::updateSearchText
    val onToggleShowSystemApps: () -> Unit = {
        viewModel.toggleShowSystemApps()
    }
    val onToggleShowOnlyPrimaryUserApps: () -> Unit = {
        viewModel.toggleShowOnlyPrimaryUserApps()
    }
    val onOpenProfile: (GroupedApps) -> Unit = { group ->
        navigator.push(Route.AppProfile(group.uid))
        viewModel.markNeedRefresh()
    }
    val actions = SuperUserActions(
        onRefresh = { viewModel.loadAppList(force = true) },
        onOpenSulog = { navigator.push(Route.Sulog) },
        onSearchTextChange = onSearchTextChange,
        onSearchStatusChange = viewModel::updateSearchStatus,
        onClearSearch = { onSearchTextChange("") },
        onToggleShowSystemApps = onToggleShowSystemApps,
        onToggleShowOnlyPrimaryUserApps = onToggleShowOnlyPrimaryUserApps,
        onUpdateSortConfig = { viewModel.updateSortConfig(it) },
        onOpenProfile = onOpenProfile,
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> SuperUserPagerMiuix(
            uiState = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )

        UiMode.Material, UiMode.Fresh, UiMode.Glass -> SuperUserPagerMaterial(
            uiState = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )

        UiMode.Glass -> SuperUserPagerGlass(
            uiState = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )
    }
}

@Composable
private fun SuperUserPagerGlass(
    uiState: SuperUserUiState,
    actions: SuperUserActions,
    bottomInnerPadding: Dp,
) {
    val granted = uiState.groupedApps.count { it.anyAllowSu }
    val apps = if (uiState.searchStatus.searchText.isBlank()) uiState.groupedApps else uiState.searchResults
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 13.dp, end = 13.dp, top = 8.dp, bottom = bottomInnerPadding + 90.dp
        ),
    ) {
        item {
            me.weishu.kernelsu.ui.screen.glass.GlassSuperUserHeader(
                grantedCount = granted,
                searchValue = uiState.searchStatus.searchText,
                onSearch = actions.onSearchTextChange,
            )
        }
        items(apps.size) { index ->
            val app = apps[index]
            me.weishu.kernelsu.ui.screen.glass.GlassSuperUserRow(
                app = app,
                onClick = { actions.onOpenProfile(app) },
            )
        }
    }
}
