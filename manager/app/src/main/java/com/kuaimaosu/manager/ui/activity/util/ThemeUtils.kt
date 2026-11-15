package com.kuaimaosu.manager.ui.activity.util

import com.kuaimaosu.manager.data.theme.ThemeRepository
import com.kuaimaosu.manager.ui.theme.BackgroundManager
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.viewmodel.SettingsUiAction
import com.kuaimaosu.manager.ui.viewmodel.SettingsViewModel

class ThemeUtils(
    private val themeConfig: ThemeConfig,
    private val themeRepository: ThemeRepository,
    private val cardConfig: CardConfig,
    private val backgroundManager: BackgroundManager,
) {

    fun initializeThemeSettings(settingsViewModel: SettingsViewModel) {
        settingsViewModel.dispatch(SettingsUiAction.InitializeFirstRun)
        loadThemeSettings()
        settingsViewModel.dispatch(SettingsUiAction.Initialize)
    }

    fun onActivityPause() {
        cardConfig.save()
    }

    fun onActivityResume() {
        loadThemeSettings()
    }

    private fun loadThemeSettings() {
        themeConfig.forceDarkMode = themeRepository.loadThemeMode()
        themeConfig.seedColor = themeRepository.loadSeedColor()
        themeConfig.useDynamicColor = themeRepository.loadDynamicColorState()
        themeConfig.dynamicColorSpec = themeRepository.loadDynamicColorSpec()
        themeConfig.dynamicPaletteStyle = themeRepository.loadDynamicPaletteStyle(
            themeConfig.dynamicColorSpec,
        )
        themeConfig.useCustomThemeColor = themeRepository.loadUseCustomThemeColor()
        // 胶囊底栏是真正的折射玻璃，必须有可采样的模糊源；设置里的开关已移除，这里常开。
        themeConfig.isEnableBlur = true
        themeConfig.appFontId = themeRepository.loadAppFont()
        themeConfig.homeKicker = themeRepository.loadHomeKicker()
        themeConfig.kickerTicker = themeRepository.loadKickerTicker()
        themeConfig.homeTitle = themeRepository.loadHomeTitle()
        themeConfig.homeTitleSplit = themeRepository.loadHomeTitleSplit()
        themeConfig.homeTitleColorPrimary = themeRepository.loadHomeTitleColorPrimary()
        themeConfig.homeTitleColorAccent = themeRepository.loadHomeTitleColorAccent()
        cardConfig.load()
        backgroundManager.loadCustomBackground()
    }
}
