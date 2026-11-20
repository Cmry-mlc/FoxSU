package com.kuaimaosu.manager.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.kuaimaosu.manager.domain.model.StartupState
import com.kuaimaosu.manager.domain.usecase.ApplyLanguageUseCase
import com.kuaimaosu.manager.domain.usecase.EnsureManagerInstalledUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveStartupStateUseCase
import com.kuaimaosu.manager.ui.activity.util.ThemeUtils
import com.kuaimaosu.manager.ui.screen.splash.KuaiMaoSplash
import com.kuaimaosu.manager.ui.theme.KernelSUTheme
import com.kuaimaosu.manager.ui.viewmodel.HomeUiAction
import com.kuaimaosu.manager.ui.viewmodel.HomeViewModel
import com.kuaimaosu.manager.ui.viewmodel.ModuleUiAction
import com.kuaimaosu.manager.ui.viewmodel.ModuleViewModel
import com.kuaimaosu.manager.ui.viewmodel.SettingsUiAction
import com.kuaimaosu.manager.ui.viewmodel.SettingsUiEvent
import com.kuaimaosu.manager.ui.viewmodel.SettingsViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

// 水墨扩散要 1.35s 才走完「墨散 → 图标 → 标题」，留 100ms 余量再淡出。
private const val SPLASH_MIN_DURATION_MS = 1450L
private const val SPLASH_FADE_MS = 420

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModel()
    private val moduleViewModel: ModuleViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()
    private val observeStartupState: ObserveStartupStateUseCase by inject()
    private val ensureManagerInstalled: EnsureManagerInstalledUseCase by inject()
    private val themeUtils: ThemeUtils by inject()
    private val applyLanguage: ApplyLanguageUseCase by inject()
    private val startupState by lazy { observeStartupState() }

    private var isInitialized = false

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let(applyLanguage::invoke))
    }

    private val intentChannel = Channel<Intent>(capacity = Channel.BUFFERED)

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // 只保留 Compose 动画首屏：系统启动窗口立即交棒，不再停留成第一屏。
            installSplashScreen()

            // Enable edge to edge
            enableEdgeToEdge()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            super.onCreate(savedInstanceState)

            // Keep the home state active even when an incoming intent opens another screen.
            lifecycleScope.launch {
                homeViewModel.uiState.first { it.isInitialDataLoaded }
            }

            lifecycleScope.launch { ensureManagerInstalled() }
            lifecycleScope.launch {
                settingsViewModel.events.collect { event ->
                    when (event) {
                        is SettingsUiEvent.Error -> if (event.message.isNotBlank()) {
                            Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG)
                                .show()
                        }

                        is SettingsUiEvent.Message -> {
                            val message = event.formatArg?.let {
                                getString(event.stringResource, it)
                            } ?: getString(event.stringResource)
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                        }

                        SettingsUiEvent.RestartActivity -> recreate()
                    }
                }
            }

            // Initialize app state once.
            if (!isInitialized) {
                initializeData()
                isInitialized = true
            }

            if (savedInstanceState == null) intent?.let { intentChannel.trySend(it) }

            setContent {
                KernelSUTheme {
                    when (val state = startupState.collectAsStateWithLifecycle().value) {
                        is StartupState.Failed -> StartupFailureContent(state.message)
                        else -> {
                            // 冷启动首屏：只在本进程首次进入时播放，重建（旋转/深色切换）不重播。
                            var showSplash by rememberSaveable {
                                mutableStateOf(savedInstanceState == null)
                            }

                            LaunchedEffect(Unit) {
                                if (!showSplash) return@LaunchedEffect
                                delay(SPLASH_MIN_DURATION_MS)
                                showSplash = false
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                NavContainer(settingsViewModel, intentChannel)

                                AnimatedVisibility(
                                    visible = showSplash,
                                    enter = EnterTransition.None,
                                    exit = fadeOut(animationSpec = tween(SPLASH_FADE_MS)),
                                ) {
                                    KuaiMaoSplash()
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentChannel.trySend(intent)
    }

    private fun initializeData() {
        lifecycleScope.launch {
            try {
                homeViewModel.dispatch(HomeUiAction.Refresh(showIndicator = false))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Initialize theme settings.
        themeUtils.initializeThemeSettings(settingsViewModel)
    }

    override fun onResume() {
        try {
            super.onResume()
            themeUtils.onActivityResume()
            synchronizeUiSettings()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun synchronizeUiSettings() {
        if (!isInitialized) return

        settingsViewModel.dispatch(SettingsUiAction.Initialize)
        moduleViewModel.dispatch(ModuleUiAction.ReloadSettings)
    }

    override fun onPause() {
        try {
            super.onPause()
            themeUtils.onActivityPause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@androidx.compose.runtime.Composable
private fun StartupFailureContent(reason: String) {
    Column(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = reason,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
