package com.loomsu.manager.ui.screen.main

import androidx.compose.ui.graphics.vector.ImageVector
import com.loomsu.manager.ui.component.LoomRow
import com.loomsu.manager.ui.component.LoomSwitch
import com.loomsu.manager.ui.theme.LoomTone
import com.loomsu.manager.ui.theme.LoomTones
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loomsu.manager.ui.component.LoomBackCapsule
import com.loomsu.manager.ui.component.LoomPageTitle
import com.loomsu.manager.ui.navigation.LocalNavigator
import com.loomsu.manager.ui.theme.ThemeConfig
import com.loomsu.manager.ui.theme.barSurfaceColor
import com.loomsu.manager.ui.theme.blurEffect
import com.loomsu.manager.ui.theme.blurSource
import com.loomsu.manager.ui.util.adaptiveScaffoldWindowInsets
import org.koin.compose.koinInject

/**
 * 二级设置页外壳（对应设计稿 .bkrow + .ptitle + .psub 结构）。
 *
 * 返回胶囊 → 24sp 标题 → 11sp 副标题 → 内容，左右 20dp 边距。
 */
@Composable
fun LoomSettingsScaffold(
    title: String,
    subtitle: String? = null,
    bottomPadding: Dp = 0.dp,
    content: LazyListScope.() -> Unit,
) {
    val navigator = LocalNavigator.current
    val themeConfig: ThemeConfig = koinInject()

    Scaffold(
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        // 返回胶囊与标题固定在顶部，只有内容列表滚动。
        // 顶栏和底栏共用一套「胶囊质感」：磨砂 / 半透明都要在这里真的改变膜厚与模糊。
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 与首页一致：顶栏不再铺色块 / 膜层，所有模式下都直接透出页面背景。
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                LoomBackCapsule(text = "设置", onClick = { navigator.pop() })
                Spacer(modifier = Modifier.height(14.dp))
                LoomPageTitle(title = title, subtitle = subtitle)
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
                bottom = innerPadding.calculateBottomPadding() + bottomPadding + 28.dp,
            ),
        ) {
            content()
        }
    }
}

/** 标准开关行：左侧彩色图标块 + 标题/说明 + 右侧 Loom 开关。 */
@Composable
internal fun LoomSwitchRow(
    icon: ImageVector,
    title: String,
    description: String?,
    checked: Boolean,
    enabled: Boolean = true,
    tone: LoomTone = LoomTones.violet,
    onCheckedChange: (Boolean) -> Unit,
) {
    LoomRow(
        icon = icon,
        title = title,
        description = description,
        iconTint = tone.content,
        iconContainer = tone.container,
        enabled = enabled,
        onClick = { if (enabled) onCheckedChange(!checked) },
        trailing = {
            LoomSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}
