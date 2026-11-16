// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 BakaSU contributors
package com.kuaimaosu.manager.ui.animation.predictiveback

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.navDirectionalTransition
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition

/**
 * 三种额外的手感：层叠、上浮、立方。
 *
 * 全部只改图形层（缩放 / 位移 / 透视），不改页面内容，
 * 所以进出手势、预测性返回和返回取消都还是同一套深度驱动，
 * 手势跟手时不会出现「动画和手指各走各的」。
 */

private val ExtraEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private val SoftMotion = NavMotion(
    commit = NavSettleSpec.Tween(durationMillis = 240, easing = FastOutSlowInEasing),
    cancel = NavSettleSpec.Spring(stiffness = 1500f),
    programmatic = NavSettleSpec.Tween(durationMillis = 300, easing = ExtraEasing),
)

private val RiseMotion = NavMotion(
    commit = NavSettleSpec.Tween(durationMillis = 280, easing = FastOutSlowInEasing),
    cancel = NavSettleSpec.Spring(stiffness = 1400f),
    programmatic = NavSettleSpec.Tween(durationMillis = 340, easing = ExtraEasing),
)

private val CubeMotion = NavMotion(
    commit = NavSettleSpec.Tween(durationMillis = 300, easing = FastOutSlowInEasing),
    cancel = NavSettleSpec.Spring(stiffness = 1300f),
    programmatic = NavSettleSpec.Tween(durationMillis = 380, easing = ExtraEasing),
)

/**
 * 层叠：新页从下方 92% 浮起，旧页缩到 94% 并轻微上移后退。
 * 位移量很小，主要是「卡片摞起来」的层次感。
 */
private val DepthStack: NavTransition = navGraphicsTransition(
    opaqueDepth = 1f,
    motion = SoftMotion,
    scrim = { scope -> coverProgress(scope.relativeDepth) * 0.55f },
) { scope ->
    val depth = scope.relativeDepth
    val widthPx = scope.layoutSize.width.toFloat()
    val heightPx = scope.layoutSize.height.toFloat()
    val liftPx = with(scope.density) { 26.dp.toPx() }
    if (depth <= 0f) {
        val progress = topProgress(depth)
        val scale = 0.92f + 0.08f * progress
        scaleX = snapScaleToPixelExtent(scale, widthPx)
        scaleY = scaleX
        translationY = snapTranslationToPixelEdge(
            translation = (1f - progress) * liftPx,
            scale = scaleY,
            extent = heightPx,
        )
        // 手势拖动期间保持不透明：否则拖到一半整页先淡掉，只剩下层在动。
        alpha = if (scope.gesture != null) 1f else (progress / 0.3f).coerceIn(0f, 1f)
    } else {
        val cover = coverProgress(depth)
        val scale = 1f - 0.06f * cover
        scaleX = snapScaleToPixelExtent(scale, widthPx)
        scaleY = scaleX
        translationY = snapTranslationToPixelEdge(
            translation = -cover * liftPx * 0.45f,
            scale = scaleY,
            extent = heightPx,
        )
    }
}

/**
 * 上浮：新页整屏从底部推上来，旧页原地缩一点；返回时新页往下退回去。
 * 幅度比层叠大，存在感更强，适合「从列表点进详情」。
 */
private val SlideUp: NavTransition = navGraphicsTransition(
    opaqueDepth = 1f,
    motion = RiseMotion,
    scrim = { scope -> coverProgress(scope.relativeDepth) * 0.6f },
) { scope ->
    val depth = scope.relativeDepth
    val widthPx = scope.layoutSize.width.toFloat()
    val heightPx = scope.layoutSize.height.toFloat()
    if (depth <= 0f) {
        val progress = topProgress(depth)
        // 只走到 92% 屏高：底边留一点，能看出它是「浮起来的一层」而不是整块位移。
        translationY = (1f - progress) * heightPx * 0.92f
        val scale = 0.94f + 0.06f * progress
        scaleX = snapScaleToPixelExtent(scale, widthPx)
        scaleY = scaleX
        alpha = if (scope.gesture != null) 1f else (progress / 0.22f).coerceIn(0f, 1f)
    } else {
        val cover = coverProgress(depth)
        val scale = 1f - 0.05f * cover
        scaleX = snapScaleToPixelExtent(scale, widthPx)
        scaleY = scaleX
        translationY = snapTranslationToPixelEdge(
            translation = -cover * with(scope.density) { 14.dp.toPx() },
            scale = scaleY,
            extent = heightPx,
        )
    }
}

/**
 * 立方：新页绕右边缘翻进来，旧页绕左边缘翻出去，像翻书。
 * 角度收在 78° 以内，避免贴到 90° 时整页被压成一条线、
 * 也避免文字透视到看不清；cameraDistance 放大让变形更平和。
 */
private val CubeFlip: NavTransition = navGraphicsTransition(
    opaqueDepth = 1f,
    motion = CubeMotion,
    scrim = { scope -> coverProgress(scope.relativeDepth) * 0.45f },
) { scope ->
    val depth = scope.relativeDepth
    cameraDistance = 22f * scope.density.density
    if (depth <= 0f) {
        val progress = topProgress(depth)
        rotationY = -(1f - progress) * 78f
        transformOrigin = TransformOrigin(pivotFractionX = 1f, pivotFractionY = 0.5f)
        alpha = if (scope.gesture != null) 1f else (progress / 0.35f).coerceIn(0f, 1f)
    } else {
        val cover = coverProgress(depth)
        rotationY = cover * 78f
        transformOrigin = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0.5f)
    }
}

internal val DepthStackNavTransition: NavTransition = navDirectionalTransition(
    push = DepthStack,
    pop = DepthStack,
    predictivePop = DepthStack,
)

internal val SlideUpNavTransition: NavTransition = navDirectionalTransition(
    push = SlideUp,
    pop = SlideUp,
    predictivePop = SlideUp,
)

internal val CubeNavTransition: NavTransition = navDirectionalTransition(
    push = CubeFlip,
    pop = CubeFlip,
    predictivePop = CubeFlip,
)
