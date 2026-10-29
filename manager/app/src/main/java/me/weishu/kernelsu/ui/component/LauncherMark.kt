package me.weishu.kernelsu.ui.component

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.util.LauncherIconManager

/**
 * 桌面启动器图标（自适应图标）的等价渲染。
 *
 * 应用内的品牌位（作者页头像、Pages / Glass 的界面图标）统一改用这个组件，
 * 而不是再拿一张 `ic_fox_global` 线稿：桌面看到的是什么，界面里就是什么。
 *
 * 实现上与 `mipmap-anydpi/ic_launcher*.xml` 完全一致 —— 一层底 + 一层前景，
 * 叠起来裁剪成圆角方形（自适应图标在桌面上被系统裁成的形状）。
 *
 * 注意：这里**不能**用 `painterResource`。前几版用它直接崩溃在启动首帧：
 * `painterResource` 只接受 VectorDrawable 与 PNG/JPG/WEBP，
 * 而 `ic_launcher_fox_foreground`（默认形象的狐狸前景）是 `<layer-list>` + `<bitmap>`，
 * Compose 会抛
 * 「Only VectorDrawables and rasterized asset types are supported ex. PNG, JPG, WEBP」，
 * 于是点开图标 → 闪屏 → 立刻退回桌面。
 * 现在改成取原始 [Drawable] 自己画：layer-list / vector / bitmap / shape 全都能画。
 */
@Composable
fun LauncherMark(
    modifier: Modifier = Modifier,
    style: Int = CustomizationStore.current.launcherIcon,
    cornerPercent: Int = 26,
) {
    val bgId: Int
    val fgId: Int
    when (style) {
        LauncherIconManager.STYLE_KERNELSU -> {
            bgId = R.drawable.ic_launcher_background
            fgId = R.drawable.ic_launcher_kernelsu_foreground
        }
        LauncherIconManager.STYLE_FOX2 -> {
            bgId = R.drawable.ic_launcher_background
            fgId = R.drawable.ic_launcher_fox2_foreground
        }
        LauncherIconManager.STYLE_FOX3 -> {
            bgId = R.drawable.ic_launcher_bg_ink
            fgId = R.drawable.ic_launcher_fox3_foreground
        }
        LauncherIconManager.STYLE_FOX4 -> {
            bgId = R.drawable.ic_launcher_bg_ink
            fgId = R.drawable.ic_launcher_fox4_foreground
        }
        else -> {
            bgId = R.drawable.ic_launcher_background
            fgId = R.drawable.ic_launcher_fox_foreground
        }
    }

    val bg = rememberDrawable(bgId)
    val fg = rememberDrawable(fgId)

    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = cornerPercent))
            .background(Color.White)
            .drawBehind {
                if (size.width <= 0f || size.height <= 0f) return@drawBehind
                // 自适应图标的标准画布是 108dp，前景里的 <bitmap> 是按 78dp 这种
                // 绝对 dp 摆位的（layer-list 不会随 bounds 缩放子项）。
                // 所以先把画布按 108dp → 控件尺寸整体缩放，再在 108dp 坐标系里画，
                // 否则 36dp 的界面图标会把 78dp 的狐狸裁掉大半。
                val unit = 108.dp.toPx()
                val k = minOf(size.width, size.height) / unit
                drawIntoCanvas { canvas ->
                    val nc = canvas.nativeCanvas
                    val save = nc.save()
                    nc.scale(k, k)
                    for (layer in arrayOf(bg, fg)) {
                        layer ?: continue
                        layer.setBounds(0, 0, unit.toInt(), unit.toInt())
                        layer.draw(nc)
                    }
                    nc.restoreToCount(save)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // 图标内容全部由 drawBehind 绘制
    }
}

/**
 * 取一份独立的 Drawable 实例。
 *
 * - `constantState.newDrawable()` 让每个使用点拿到互不共享 bounds 的副本
 *   （同一个图标可能同时出现在头图和列表里，共享实例会互相改 bounds）。
 * - `mutate()` 断开与资源缓存的状态共享，避免状态残留。
 */
@Composable
private fun rememberDrawable(resId: Int): Drawable? {
    val context = LocalContext.current
    return remember(resId, context) {
        ContextCompat.getDrawable(context, resId)
            ?.constantState
            ?.newDrawable(context.resources)
            ?.mutate()
            ?: ContextCompat.getDrawable(context, resId)
    }
}
