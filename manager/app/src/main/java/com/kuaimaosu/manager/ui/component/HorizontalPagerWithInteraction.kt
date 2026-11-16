package com.kuaimaosu.manager.ui.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import com.kuaimaosu.manager.ui.theme.LocalBackgroundRenderState
import com.kuaimaosu.manager.ui.util.LocalPagerPage
import com.kuaimaosu.manager.ui.util.LocalPagerState
import top.yukonga.miuix.kmp.nav.core.LocalNavTransitionScope
import top.yukonga.miuix.kmp.utils.PagerFlingTrackerConnection
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode
import top.yukonga.miuix.kmp.utils.iosStyleMomentumHalt

@Composable
fun HorizontalPagerWithInteraction(
    state: PagerState,
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true,
    pageNestedScrollConnection: NestedScrollConnection? = null,
    flingBehavior: TargetedFlingBehavior? = null,
    enableGestureOverride: Boolean = true,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    val backgroundRenderState = LocalBackgroundRenderState.current
    val scrollOwner = remember { Any() }
    LaunchedEffect(state, backgroundRenderState) {
        try {
            snapshotFlow { state.isScrollInProgress }.collect {
                backgroundRenderState.updatePagerScroll(scrollOwner, it)
            }
        } finally {
            backgroundRenderState.updatePagerScroll(scrollOwner, false)
        }
    }
    val transitionRunning = LocalNavTransitionScope.current.isRunning
    var contentReady by remember { mutableStateOf(false) }
    LaunchedEffect(transitionRunning) {
        if (!transitionRunning && !contentReady) {
            withFrameNanos { }
            contentReady = true
        }
    }
    // 兜底：万一转场状态没回到「结束」，页面会一直不预组合，
    // 左右滑动时就会翻出一张白页。给个上限，保证邻页一定会被组合出来。
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(700)
        contentReady = true
    }
    val nestedConnection = pageNestedScrollConnection ?: PagerDefaults.pageNestedScrollConnection(
        state = state,
        orientation = Orientation.Horizontal,
    )

    // 页面切换手势：三个档位在这里统一分发，主界面和内页走同一套。
    // 跨轴拦截：pager 自带拖拽关掉，改用我们自己的手势层（能抢在竖向滚动之前接管）。
    // iOS 风格：保留自带拖拽，只加「按下即刹住子级惯性」。
    val pagerMode = LocalPagerInterceptionMode.current
    val crossAxisSwipe = enableGestureOverride && pagerMode == PagerInterceptionMode.CrossAxisInterceptor
    val haltMomentum = enableGestureOverride && pagerMode == PagerInterceptionMode.TapToHalt
    val flingTracker = remember { PagerFlingTrackerConnection() }

    HorizontalPager(
        state = state,
        modifier = when {
            crossAxisSwipe -> modifier.crossAxisPagerSwipe(state, enabled = userScrollEnabled)
            haltMomentum -> modifier.iosStyleMomentumHalt(
                flingTracker = flingTracker,
                enabled = userScrollEnabled,
            )
            else -> modifier
        },
        userScrollEnabled = if (crossAxisSwipe) false else userScrollEnabled,
        beyondViewportPageCount = if (contentReady) state.pageCount else 0,
        pageNestedScrollConnection = if (crossAxisSwipe) {
            pageNestedScrollConnection ?: PagerGestureNestedScrollConnection
        } else {
            nestedConnection
        },
        flingBehavior = flingBehavior ?: PagerDefaults.flingBehavior(state),
        pageContent = { pageIndex ->
            if (!contentReady && state.currentPage != pageIndex) return@HorizontalPager

            CompositionLocalProvider(
                LocalPagerState provides state,
                LocalPagerPage provides pageIndex,
            ) {
                pageContent(pageIndex)
            }
        },
    )
}
