// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.kuaimaosu.manager.ui.component

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode

/**
 * 全局「页面切换手势」档位。
 *
 * 以前这个档位只塞进了主界面的 pager，安装页 / 模块详情 / SUSFS 这些内页
 * 还各写各的；改成 CompositionLocal 之后，所有横向分页都读同一个值，
 * 在设置里改一次就全部生效。
 */
val LocalPagerInterceptionMode = compositionLocalOf { PagerInterceptionMode.CrossAxisInterceptor }

private sealed interface SwipeEvent {
    /** 手指位移（已换算成 pager 的滚动方向）。 */
    data class Delta(val value: Float) : SwipeEvent

    /** 抬手：带着目标页收尾。 */
    data class Release(val target: Int) : SwipeEvent
}

/**
 * 一次拖拽只开一个 [PagerState.scroll] 事务：
 *
 * 多个事务互相抢占会丢帧（新事务把上一个里的残留位移一起取消掉），
 * 手指就会觉得「跟不上手」。这里用一个 Channel 把位移喂给同一个事务，
 * 抬手后再用同一个 pager 的动画补到目标页。
 */
private class PagerSwipeController(
    private val scope: CoroutineScope,
) {
    private var job: Job? = null
    private var events: Channel<SwipeEvent>? = null

    fun begin(state: PagerState): Channel<SwipeEvent> {
        cancel()
        val channel = Channel<SwipeEvent>(Channel.UNLIMITED)
        events = channel
        job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            var target = -1
            try {
                state.scroll(MutatePriority.UserInput) {
                    for (event in channel) {
                        when (event) {
                            is SwipeEvent.Delta -> scrollBy(event.value)
                            is SwipeEvent.Release -> {
                                target = event.target
                                break
                            }
                        }
                    }
                }
            } finally {
                channel.cancel()
                if (events === channel) events = null
            }
            if (target >= 0 && state.pageCount > 0) {
                // 事务已经结束，再单独跑翻页动画：新手指按下时会被 UserInput 抢占，
                // 所以「翻页到一半再滑」也能立刻接管。
                state.animateScrollToPage(target.coerceIn(0, state.pageCount - 1))
            }
        }
        return channel
    }

    fun cancel() {
        events?.close()
        events = null
        job?.cancel()
        job = null
    }
}

/**
 * 跨轴拦截式翻页：不依赖 pager 自带的拖拽（它会被子级竖向滚动抢走），
 * 自己在 Initial 阶段读事件，判定为横向后接管并消费。
 *
 * 用 [Modifier.pointerInput] 而不是自定义 ModifierNode：
 * 自定义节点挂在 pager 上时收不到事件（真机上表现为「怎么滑都不翻页」），
 * 这套标准 API 在本项目所有 Compose 版本上都可靠。
 */
@Composable
fun Modifier.crossAxisPagerSwipe(
    pagerState: PagerState,
    enabled: Boolean = true,
): Modifier {
    val scope = rememberCoroutineScope()
    val controller = remember(pagerState) { PagerSwipeController(scope) }
    val layoutDirection = LocalLayoutDirection.current

    return this.pointerInput(pagerState, enabled, layoutDirection) {
        if (!enabled) return@pointerInput

        val touchSlop = viewConfiguration.touchSlop
        val minimumVelocity = 400.dp.toPx()
        val scrollSign = if (
            (layoutDirection == LayoutDirection.Rtl) xor pagerState.layoutInfo.reverseLayout
        ) {
            1f
        } else {
            -1f
        }
        val tracker = VelocityTracker()

        fun targetPage(velocity: Float): Int {
            if (pagerState.pageCount == 0) return 0
            val offset = pagerState.currentPageOffsetFraction
            val position = pagerState.currentPage + if (offset.isNaN()) 0f else offset
            val page = when {
                velocity > minimumVelocity -> ceil(position).toInt()
                velocity < -minimumVelocity -> floor(position).toInt()
                else -> position.roundToInt()
            }
            return page.coerceIn(0, pagerState.pageCount - 1)
        }

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            // 新的一次按下立刻接管：正在回弹的翻页动画会被这次 UserInput 取消。
            val channel = controller.begin(pagerState)
            tracker.resetTracking()
            tracker.addPointerInputChange(down)

            var pointerId = down.id
            var accumulated = Offset.Zero
            var dragging = false
            var released = false

            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                    if (change.isConsumed && !dragging) break

                    if (!change.pressed) {
                        // 换手指：接着算，不打断这一轮手势。
                        val replacement = event.changes.firstOrNull { it.pressed }
                        if (replacement != null) {
                            pointerId = replacement.id
                            tracker.resetTracking()
                            tracker.addPointerInputChange(replacement)
                            continue
                        }
                        tracker.addPointerInputChange(change)
                        val velocity = if (dragging) {
                            change.consume()
                            tracker.calculateVelocity(
                                Velocity(
                                    viewConfiguration.maximumFlingVelocity,
                                    viewConfiguration.maximumFlingVelocity,
                                ),
                            ).x * scrollSign
                        } else {
                            0f
                        }
                        channel.trySend(SwipeEvent.Release(targetPage(velocity)))
                        released = true
                        break
                    }

                    tracker.addPointerInputChange(change)
                    val delta = change.position - change.previousPosition
                    accumulated += delta

                    if (!dragging) {
                        val dx = abs(accumulated.x)
                        val dy = abs(accumulated.y)
                        if (dx > touchSlop && dx > dy) {
                            dragging = true
                            change.consume()
                            // 把越过 slop 的那一段补上，否则起步会「少一格」。
                            val overSlop = accumulated.x - sign(accumulated.x) * touchSlop
                            channel.trySend(SwipeEvent.Delta(overSlop * scrollSign))
                        } else if (dy > touchSlop) {
                            // 竖向手势，交给页面内容。
                            break
                        }
                    } else {
                        change.consume()
                        channel.trySend(SwipeEvent.Delta(delta.x * scrollSign))
                    }
                }
            } finally {
                if (!released) {
                    // 手势被打断（比如被上层取消）：原地回弹，不留下半页。
                    channel.trySend(SwipeEvent.Release(targetPage(0f)))
                }
                channel.close()
            }
        }
    }
}
