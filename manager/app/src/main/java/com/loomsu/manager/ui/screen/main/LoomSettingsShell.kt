package com.loomsu.manager.ui.screen.main

import androidx.compose.ui.graphics.vector.ImageVector
import com.loomsu.manager.ui.component.LoomRow
import com.loomsu.manager.ui.component.LoomSwitch
import com.loomsu.manager.ui.theme.LoomTone
import com.loomsu.manager.ui.theme.LoomTones
import androidx.compose.foundation.layout.PaddingValues
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
import com.loomsu.manager.ui.theme.blurSource
import com.loomsu.manager.ui.util.adaptiveScaffoldWindowInsets

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

    Scaffold(
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .blurSource(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = innerPadding.calculateBottomPadding() + bottomPadding + 28.dp,
            ),
        ) {
            item {
                Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding() + 6.dp))
            }
            item {
                LoomBackCapsule(text = "设置", onClick = { navigator.pop() })
            }
            item {
                Spacer(modifier = Modifier.height(14.dp))
                LoomPageTitle(title = title, subtitle = subtitle)
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
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
