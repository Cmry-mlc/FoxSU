package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.util.shouldShowSplitPane
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.utils.springAnimateToPage

class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
    private val animatePageChanges: Boolean,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return

        navJob?.cancel()

        selectedPage = targetIndex
        isNavigating = true

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                if (animatePageChanges) {
                    pagerState.springAnimateToPage(targetIndex)
                } else {
                    pagerState.scrollToPage(targetIndex)
                }
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    animatePageChanges: Boolean = true,
): MainPagerState {
    return remember(pagerState, coroutineScope, animatePageChanges) {
        MainPagerState(pagerState, coroutineScope, animatePageChanges)
    }
}

@Immutable
data class NavigationBadgeState(
    val superuserCount: Int = 0,
    val moduleEnabledCount: Int = 0,
    val moduleUpdatableCount: Int = 0,
)

internal enum class BadgeTone { Alert, Accent }

@Immutable
internal data class NavBadge(val count: Int, val tone: BadgeTone)

internal fun badgeFor(index: Int, state: NavigationBadgeState): NavBadge? = when (index) {
    BottomBarDestination.SuperUser.ordinal ->
        state.superuserCount.takeIf { it > 0 }?.let { NavBadge(it, BadgeTone.Accent) }

    BottomBarDestination.Module.ordinal -> when {
        state.moduleUpdatableCount > 0 -> NavBadge(state.moduleUpdatableCount, BadgeTone.Alert)
        state.moduleEnabledCount > 0 -> NavBadge(state.moduleEnabledCount, BadgeTone.Accent)
        else -> null
    }

    else -> null
}

@Composable
fun useNavigationRail(enableFloatingBottomBar: Boolean): Boolean {
    return shouldShowSplitPane() && !(LocalUiMode.current == UiMode.Miuix && enableFloatingBottomBar)
}

@Composable
fun BottomBar(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop,
    navigationBadge: NavigationBadgeState,
    modifier: Modifier = Modifier,
) {
    when (LocalUiMode.current) {
        // 晨曦玻璃 Redesign：紫→品红品牌渐变选中
        UiMode.Miuix -> ThemeDockBar(
            theme = DockTheme(
                bg = Color(0xC7FFFFFF), border = Color(0x99FFFFFF), shadow = Color(0x2E32286E),
                selected = Color(0xFF6C5CE7), unselected = Color(0xFF66647A),
                selectedBg = Color(0x1A6C5CE7), badge = Color(0xFFEC4899), glass = true,
            ),
            navigationBadge = navigationBadge,
            modifier = modifier,
        )
        // Pages 极简：纯白胶囊 + 蓝色强调
        UiMode.Fresh -> ThemeDockBar(
            theme = DockTheme(
                bg = Color(0xF2FFFFFF), border = Color(0x14111111), shadow = Color(0x1A111111),
                selected = Color(0xFF2D6BFF), unselected = Color(0xFF9AA0B0),
                selectedBg = Color(0x142D6BFF), badge = Color(0xFF2D6BFF),
            ),
            navigationBadge = navigationBadge,
            modifier = modifier,
        )
        // Editorial 纸面：纸色胶囊 + 墨描边 + 信号橙
        UiMode.Material -> ThemeDockBar(
            theme = DockTheme(
                bg = Color(0xFFFFFCF6), border = Color(0x331D1C22), shadow = Color(0x33282418),
                selected = Color(0xFFE8502E), unselected = Color(0xFF9C968B),
                selectedBg = Color(0x1AE8502E), badge = Color(0xFFE8502E),
            ),
            navigationBadge = navigationBadge,
            modifier = modifier,
        )
        // Daylight 玻璃：白玻璃 + 紫/薄荷
        UiMode.Glass -> ThemeDockBar(
            theme = DockTheme(
                bg = Color(0xCCFFFFFF), border = Color(0x33FFFFFF), shadow = Color(0x2E32286E),
                selected = Color(0xFF6A57F5), unselected = Color(0xFF9AA0B0),
                selectedBg = Color(0x1A6A57F5), badge = Color(0xFF0FA98B), glass = true,
            ),
            navigationBadge = navigationBadge,
            modifier = modifier,
        )
        // Nocturne 暗夜：暗玻璃 + 紫罗兰
        UiMode.Nocturne -> ThemeDockBar(
            theme = DockTheme(
                bg = Color(0xE6101218), border = Color(0x26FFFFFF), shadow = Color(0x66000000),
                selected = Color(0xFF8B7BFF), unselected = Color(0xFF5D6376),
                selectedBg = Color(0x2E8B7BFF), badge = Color(0xFF4DE2C5), glass = true,
            ),
            navigationBadge = navigationBadge,
            modifier = modifier,
        )
    }
}

@Composable
fun SideRail(
    navigationBadge: NavigationBadgeState,
    modifier: Modifier = Modifier,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> NavigationRailMiuix(navigationBadge, modifier)
        UiMode.Material -> NavigationRailMaterial(navigationBadge, modifier)
        UiMode.Fresh -> NavigationRailMaterial(navigationBadge, modifier)
        UiMode.Glass -> NavigationRailMaterial(navigationBadge, modifier)
        UiMode.Nocturne -> NavigationRailMaterial(navigationBadge, modifier)
    }
}
