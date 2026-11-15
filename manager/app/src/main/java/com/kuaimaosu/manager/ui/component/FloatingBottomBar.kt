// Adapted from compose-miuix-ui example (IosLiquidGlassNavigationBar) — Apache 2.0.

package com.kuaimaosu.manager.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.launch
import com.kuaimaosu.manager.ui.component.miuix.animation.DampedDragAnimation
import com.kuaimaosu.manager.ui.component.miuix.animation.InteractiveHighlight
import com.kuaimaosu.manager.ui.theme.CapsuleEffect
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.theme.backdropInk
import com.kuaimaosu.manager.ui.theme.barSurfaceColor
import com.kuaimaosu.manager.ui.theme.isInDarkTheme
import com.kuaimaosu.manager.ui.theme.renderBackgroundBlur
import com.kuaimaosu.manager.ui.theme.LocalDarkBackdrop
import org.koin.compose.koinInject

val LocalFloatingBottomBarTabScale = staticCompositionLocalOf { { 1f } }

@Composable
fun RowScope.FloatingBottomBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scale = LocalFloatingBottomBarTabScale.current
    Column(
        modifier
            // 点击判定统一放在 FloatingBottomBar 的整行上（见 onTabTap）：
            // 子项一旦自己注册指针输入，就会和父级的 DampedDragAnimation 抢事件，
            // 拖动整条胶囊的手势会直接失效。这里只保留语义与键盘操作。
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.Tab
                onClick {
                    onClick()
                    true
                }
            }
            .onKeyEvent { event ->
                val activationKey = event.key == Key.Enter ||
                    event.key == Key.NumPadEnter || event.key == Key.Spacebar
                if (activationKey) {
                    if (event.type == KeyEventType.KeyUp) onClick()
                    true
                } else {
                    false
                }
            }
            // 不要 focusable：点击时它会请求焦点，Material 的焦点指示会在这一格
            // 画出一块直角色块（就是「按下去出现四方框黑影」）。
            // 键盘操作本来就靠 NavigationBar 的语义，这里去掉不影响点击与拖动。
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val scale = scale()
                scaleX = scale
                scaleY = scale
            },
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
fun FloatingBottomBar(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onSelected: (index: Int) -> Unit,
    tabsCount: Int,
    isBlurEnabled: Boolean = true,
    /** 有些格子只是操作入口（例如中间的电源键），不参与选中：拖到它上面会回弹。 */
    isTabSelectable: (index: Int) -> Boolean = { true },
    /**
     * 整条胶囊的点击回调。
     *
     * 点击判定做在父级而不是每个 tab 上：子项注册 pointerInput 会和这里的
     * [DampedDragAnimation] 抢 down 事件，导致「按住胶囊左右拖」彻底失灵。
     * 父级这一层只观察、不消费，拖动仍然完整交给 DampedDragAnimation。
     */
    onTabTap: (index: Int) -> Unit = {},
    content: @Composable RowScope.((Int) -> Unit) -> Unit,
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val isInDark = isInDarkTheme(themeConfig.forceDarkMode)
    val pillShape = remember { CircleShape }
    // 底栏浮在内容之上：亮底深字、暗底浅字。壁纸偏暗时 KuaiMaoColors.text 仍是深色，
    // 压在深膜上就看不见了，所以这里跟页面标题一样走 backdropInk。
    val tabContentColor = backdropInk()
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val effect = themeConfig.capsuleEffect
    val wallpaper = cardConfig.isCustomBackgroundEnabled
    // 膜是深是浅：亮底白膜、暗底深膜（壁纸偏暗时 barSurfaceColor 已经切成深膜）
    val barTint = barSurfaceColor()
    // 三档只差「膜多厚」，模糊交给下面同一条管线：
    //   · 液态玻璃：最薄，背景轮廓要透得上来；
    //   · 磨砂    ：最厚，只留一层柔光；
    //   · 半透明  ：中间，且不加额外模糊。
    // 有壁纸时必须明显更薄：0.9 的白膜压在壁纸上就是一块白板，深膜就是一块黑板
    // ——「底栏变成白色 / 黑色」就是这么来的。
    val membraneAlpha = when {
        // 关掉「模糊」总开关：不透明，按开关文案走，不被质感选项覆盖。
        !isBlurEnabled -> 1f
        effect == CapsuleEffect.FROSTED -> if (wallpaper) 0.62f else 0.84f
        effect == CapsuleEffect.TRANSLUCENT -> if (wallpaper) 0.22f else 0.34f
        else -> if (wallpaper) 0.34f else 0.55f
    }
    // 膜和选中块按「这块底色是深是浅」取反：壁纸偏暗时虽然主题是亮的，
    // 底栏已经是深膜，选中块就得用白块，不能再用黑块。
    val darkBar = isInDark || LocalDarkBackdrop.current
    // 胶囊描边：亮底下一圈极淡的墨线，暗底下一圈极淡的白线。
    // 只为了让「白膜压白卡」时不至于整条消失，不参与材质。
    val pillEdge = if (darkBar) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val onSelectedUpdated by rememberUpdatedState(onSelected)

    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { 4.dp.toPx() }
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx)
            .toInt()
            .coerceIn(0, tabsCount - 1)
    }

    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { offset ->
                offset.x in 0f..totalWidthPx
            },
            onDragStarted = { position ->
                val idx = indexAt(position.x)
                updateValue((if (isTabSelectable(idx)) idx else currentIndex).toFloat())
            },
            onDragStopped = {
                val rawTarget = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                // 落在不可选中的格子上（电源键）就回弹到原来的页
                val targetIndex = if (isTabSelectable(rawTarget)) rawTarget else currentIndex
                if (currentIndex != targetIndex) {
                    currentIndex = targetIndex
                    onSelectedUpdated(targetIndex)
                }
                updateValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDragCancelled = {
                updateValue(currentIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f && dragAmount.x != 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            },
        )
    }

    LaunchedEffect(selectedIndex) {
        if (currentIndex != selectedIndex) {
            currentIndex = selectedIndex
            dampedDragAnimation.animateToValue(selectedIndex.toFloat())
        }
    }

    fun activateTab(index: Int) {
        if (index !in 0 until tabsCount) return
        if (currentIndex != index) {
            currentIndex = index
            onSelectedUpdated(index)
        }
        dampedDragAnimation.animateToValue(index.toFloat())
    }

    val interactiveHighlight = remember(animationScope, tabWidthPx, dampedDragAnimation) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { size, _ ->
                Offset(
                    if (isLtr) {
                        (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                    } else {
                        size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                    },
                    size.height / 2f,
                )
            },
        )
    }

    // 整行的点击判定：只观察、不消费。手指位移超过 touchSlop 或事件被拖动层消费时，
    // 就不算点击，保证「按住拖」优先于「点一下」。
    val onTabTapUpdated by rememberUpdatedState(onTabTap)
    val tapModifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            if (totalWidthPx <= 0f) return@awaitEachGesture
            val down = awaitFirstDown(requireUnconsumed = false)
            var moved = false
            var consumed = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed) consumed = true
                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                    moved = true
                }
                if (!change.pressed) break
            }
            if (!moved && !consumed) {
                onTabTapUpdated(indexAt(down.position.x))
            }
        }
    }

    Box(
        modifier = modifier.width(IntrinsicSize.Min),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier
                .onGloballyPositioned { coords ->
                    totalWidthPx = coords.size.width.toFloat()
                    val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                    tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                }
                .selectableGroup()
                .graphicsLayer { translationX = panelOffset }
                // 不要外投影：底栏是浮在内容上的一层，投影会在页面滚动时
                // 压在列表底部形成一条阴影带，滚到顶才「消失」。
                .then(
                    if (!isBlurEnabled) {
                        // 模糊总开关关掉：不透明膜，图标对比优先。
                        Modifier.background(surfaceContainer, pillShape)
                    } else {
                        // 三档共用同一条材质管线（和卡片、顶栏完全一致）：
                        //   1. 先铺一层半透明膜 —— 亮底白、暗底深，壁纸偏暗时自动换深膜；
                        //   2. 有壁纸 / 背景特效时再叠一层「真实模糊过的背景」。
                        // 不再用 drawBackdrop：它在没有壁纸的纯色页上采不到东西，
                        // 会把 40% 的白膜合成到图层底色上 —— 整条就是一块灰黑（#666），
                        // 也就是「亮色液态玻璃底栏变黑」。它在形状外扩采样还会在胶囊
                        // 上下留下横条，那正是「磨砂底栏一直有阴影」。
                        Modifier
                            .clip(pillShape)
                            .background(barTint.copy(alpha = membraneAlpha), pillShape)
                            .renderBackgroundBlur(tintColor = barTint, tintAlpha = membraneAlpha)
                            .border(1.dp, pillEdge, pillShape)
                    },
                )
                .then(
                    // 按压高光三档都有：它只在按下时画一层很淡的柔光，不参与材质。
                    if (isBlurEnabled) {
                        interactiveHighlight.modifier.then(interactiveHighlight.gestureModifier)
                    } else {
                        Modifier
                    },
                )
                .then(dampedDragAnimation.modifier)
                .then(tapModifier)
                .height(64.dp)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides tabContentColor) {
                content(::activateTab)
            }
        }

        if (tabWidthPx > 0f) {
            val tabWidthDp = with(density) { tabWidthPx.toDp() }
            // 选中块只有一种画法：一层很淡的反色块，跟着拖拽值移动。
            // 不再采样 backdrop、也不再复制一层图标 —— 副本会跟着动画偏移，
            // 和真图标错位叠在一起就是「切换时重影」；采样则会在磨砂档
            // 把图标糊掉，只剩一块空白。
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        val progressOffset = dampedDragAnimation.value * tabWidthPx
                        translationX =
                            if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                    }
                    .clip(pillShape)
                    // 底栏是导航不是按钮：选中态只要看得出位置就够了。
                    .background(
                        if (darkBar) Color.White.copy(alpha = 0.12f)
                        else Color.Black.copy(alpha = 0.06f),
                        pillShape,
                    )
                    .height(56.dp)
                    .width(tabWidthDp),
            )
        }
    }
}
