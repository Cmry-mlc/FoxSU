package com.kuaimaosu.manager.ui.screen

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AdminPanelSettings
import androidx.compose.material.icons.twotone.Extension
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.ui.screen.main.HomePage
import com.kuaimaosu.manager.ui.screen.main.ModulePage
import com.kuaimaosu.manager.ui.screen.main.SettingsPage
import com.kuaimaosu.manager.ui.screen.main.SuperUserPage

enum class BottomBarDestination(
    val direction: @Composable (bottomPadding: Dp) -> Unit,
    @param:StringRes val label: Int,
    val iconSelected: ImageVector,
    val iconNotSelected: ImageVector,
    val rootRequired: Boolean,
) {
    Home(
        { bottomPadding -> HomePage(bottomPadding) },
        R.string.home,
        Icons.TwoTone.Home,
        Icons.TwoTone.Home,
        false,
    ),
    SuperUser(
        { bottomPadding -> SuperUserPage(bottomPadding) },
        R.string.superuser,
        Icons.TwoTone.AdminPanelSettings,
        Icons.TwoTone.AdminPanelSettings,
        true,
    ),
    Module(
        { bottomPadding -> ModulePage(bottomPadding) },
        R.string.module,
        Icons.TwoTone.Extension,
        Icons.TwoTone.Extension,
        true,
    ),
    Settings(
        { bottomPadding -> SettingsPage(bottomPadding) },
        R.string.settings,
        Icons.TwoTone.Settings,
        Icons.TwoTone.Settings,
        false,
    ),
    ;

    companion object {
        /**
         * KuaiMaoSU 始终展示全部入口，不再因为内核未工作而隐藏超级用户 / 模块页。
         * 页面内部自行处理无 root 的空状态。
         */
        @Suppress("UNUSED_PARAMETER")
        fun getPages(isKsuValid: Boolean): List<BottomBarDestination> =
            BottomBarDestination.entries.toList()
    }
}
