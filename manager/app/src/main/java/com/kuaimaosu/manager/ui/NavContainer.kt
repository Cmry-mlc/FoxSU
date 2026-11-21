package com.kuaimaosu.manager.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.domain.model.CatalogModule
import com.kuaimaosu.manager.domain.model.ModuleReleaseAsset
import com.kuaimaosu.manager.domain.usecase.ExtractModuleIdUseCase
import com.kuaimaosu.manager.ui.activity.PermissionRequestInterface
import com.kuaimaosu.manager.ui.animation.predictiveback.installerNavTransition
import com.kuaimaosu.manager.ui.component.LocalPagerInterceptionMode
import com.kuaimaosu.manager.ui.component.LocalRepositoryModuleInstall
import com.kuaimaosu.manager.ui.navigation.IntentDispatcher
import com.kuaimaosu.manager.ui.assistant.KuaiMaoAssistantRuntime
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.navigation.Navigator
import com.kuaimaosu.manager.ui.navigation.Route
import com.kuaimaosu.manager.ui.overscroll.StretchOverscrollCompensationState
import com.kuaimaosu.manager.ui.overscroll.rememberCustomOverscrollFactory
import com.kuaimaosu.manager.ui.screen.AppProfileScreen
import com.kuaimaosu.manager.ui.screen.AppProfileTemplateScreen
import com.kuaimaosu.manager.ui.screen.DynamicManagerScreen
import com.kuaimaosu.manager.ui.screen.ExecuteModuleActionScreen
import com.kuaimaosu.manager.ui.screen.FlashIt
import com.kuaimaosu.manager.ui.screen.FlashScreen
import com.kuaimaosu.manager.ui.screen.InstallScreen
import com.kuaimaosu.manager.ui.screen.ScriptRunScreen
import com.kuaimaosu.manager.ui.screen.SulogScreen
import com.kuaimaosu.manager.ui.screen.TemplateEditorScreen
import com.kuaimaosu.manager.ui.screen.UmountManagerScreen
import com.kuaimaosu.manager.ui.screen.about.AboutScreen
import com.kuaimaosu.manager.ui.screen.about.CreditsScreen
import com.kuaimaosu.manager.ui.screen.about.IconCreditsScreen
import com.kuaimaosu.manager.ui.screen.about.ModificationsScreen
import com.kuaimaosu.manager.ui.screen.about.OpenSourceLicenseScreen
import com.kuaimaosu.manager.ui.screen.kernelFlash.KernelFlashScreen
import com.kuaimaosu.manager.ui.screen.language.LanguageScreen
import com.kuaimaosu.manager.ui.screen.main.MainScreen
import com.kuaimaosu.manager.ui.screen.main.NotificationsPage
import com.kuaimaosu.manager.ui.screen.main.SettingsAiPage
import com.kuaimaosu.manager.ui.screen.main.SettingsProfilePage
import com.kuaimaosu.manager.ui.screen.main.SettingsRootPage
import com.kuaimaosu.manager.ui.screen.main.SettingsToolsPage
import com.kuaimaosu.manager.ui.screen.main.SettingsUpdatePage
import com.kuaimaosu.manager.ui.screen.main.SystemInfoPage
import com.kuaimaosu.manager.ui.screen.moduleRepo.AddRepositoryScreen
import com.kuaimaosu.manager.ui.screen.moduleRepo.ModuleRepoScreen
import com.kuaimaosu.manager.ui.screen.moduleRepo.OnlineModuleDetailScreen
import com.kuaimaosu.manager.ui.screen.mount.MountControllerScreen
import com.kuaimaosu.manager.ui.screen.susfs.SuSFSConfigScreen
import com.kuaimaosu.manager.ui.screen.susfs.SuSFSLkmScreen
import com.kuaimaosu.manager.ui.screen.acehide.AceHideScreen
import com.kuaimaosu.manager.ui.screen.themeSettings.ThemeSettingsScreen
import com.kuaimaosu.manager.ui.screen.zygisk.ZygiskScreen
import com.kuaimaosu.manager.ui.screen.kuaimao.KuaiMaoRootScreen
import com.kuaimaosu.manager.ui.theme.BackgroundRenderState
import com.kuaimaosu.manager.ui.theme.BackgroundManager
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.LocalBackgroundRenderState
import com.kuaimaosu.manager.ui.theme.BackgroundEffect
import com.kuaimaosu.manager.ui.theme.LocalDarkBackdrop
import com.kuaimaosu.manager.ui.theme.LocalOnWallpaper
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.drawKuaiMaoEffect
import com.kuaimaosu.manager.ui.theme.isInDarkTheme
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.util.LocalBackgroundBlurAnchor
import com.kuaimaosu.manager.ui.util.LocalBlurState
import com.kuaimaosu.manager.ui.util.LocalPermissionRequestInterface
import com.kuaimaosu.manager.ui.util.LocalPortraitState
import com.kuaimaosu.manager.ui.util.LocalSnackbarHost
import com.kuaimaosu.manager.ui.util.LocalStretchOverscrollCompensationState
import com.kuaimaosu.manager.ui.util.rememberDeviceCornerRadius
import com.kuaimaosu.manager.ui.viewmodel.HomeUiAction
import com.kuaimaosu.manager.ui.viewmodel.HomeViewModel
import com.kuaimaosu.manager.ui.viewmodel.PredictiveBackAnimation
import com.kuaimaosu.manager.ui.viewmodel.SettingsViewModel
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.nav.core.LocalNavTransitionScope
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode

@Composable
fun NavContainer(
    settingsViewModel: SettingsViewModel,
    intentChannel: Channel<Intent>,
) {
    val themeConfig: ThemeConfig = koinInject()
    val backgroundRenderState = LocalBackgroundRenderState.current
    val extractModuleId = koinInject<ExtractModuleIdUseCase>()
    val activity = LocalActivity.current as MainActivity
    val context = LocalContext.current

    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val systemDensity = LocalDensity.current
    val stretchOverscrollCompensationState = remember {
        StretchOverscrollCompensationState()
    }
    val overscrollFactory = rememberCustomOverscrollFactory(
        compensationState = stretchOverscrollCompensationState,
    )

    val density = remember(systemDensity, settings.dpi) {
        if (settings.dpi <= 0f) {
            systemDensity
        } else {
            val targetDensity = settings.dpi / 160f
            Density(density = targetDensity, fontScale = systemDensity.fontScale)
        }
    }

    val backStack = rememberNavBackStack<Route>(Route.Main)
    val navigator = remember(backStack) { Navigator(backStack) }
    // 助手要能直接改底栏、开页面：把同一批全局对象交给它的执行端，
    // 不用另建一套状态，用户改完立刻在界面上生效。
    val assistantCardConfig: CardConfig = koinInject()
    val assistantBackgroundManager: BackgroundManager = koinInject()
    LaunchedEffect(navigator) {
        KuaiMaoAssistantRuntime.navigator = navigator
        KuaiMaoAssistantRuntime.themeConfig = themeConfig
        KuaiMaoAssistantRuntime.cardConfig = assistantCardConfig
        KuaiMaoAssistantRuntime.backgroundManager = assistantBackgroundManager
        KuaiMaoAssistantRuntime.appContext = context.applicationContext
    }

    // 从安装 / 刷写 / 越狱等二级页回到主界面时，重新读取真实内核状态，
    // 保证「安装镜像成功」后主页立即显示工作中（LKM / 内置）而不是旧的未工作。
    val homeViewModel: HomeViewModel = koinViewModel()
    var lastStackSize by remember { mutableStateOf(backStack.size) }
    LaunchedEffect(backStack.size) {
        val size = backStack.size
        if (size == 1 && lastStackSize > 1) {
            homeViewModel.dispatch(HomeUiAction.Refresh(showIndicator = false))
        }
        lastStackSize = size
    }
    val installRepositoryModule: (Uri, CatalogModule, ModuleReleaseAsset, () -> Boolean) -> Unit = { uri, module, asset, canInstall ->
        activity.lifecycleScope.launch(Dispatchers.IO) {
            if (!canInstall()) return@launch
            val moduleId = if (module.isScript) module.moduleId else runCatching { extractModuleId(uri.toString()) }.getOrNull()
            withContext(Dispatchers.Main) {
                if (!canInstall()) return@withContext
                if (module.isScript && asset.expected != null) {
                    navigator.push(Route.ScriptRun(uri.toString(), module.moduleId, asset.expected.size, asset.expected.sha256, displayName = module.moduleName))
                } else if (!moduleId.isNullOrBlank() && !module.isScript) {
                    navigator.push(Route.Flash.modules(listOf(uri.toString())))
                } else {
                    Toast.makeText(context, R.string.repo_error_module_archive, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val onBack = remember(navigator) {
        {
            when (val top = navigator.current()) {
                is Route.TemplateEditor -> {
                    if (!top.readOnly) {
                        navigator.setResult("template_edit", true)
                    } else {
                        navigator.pop()
                    }
                }

                else -> navigator.pop()
            }
        }
    }
    val useBlur = themeConfig.isEnableBlur

    lateinit var permissionRequestHandler: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>

    val permissionRequestInterface = remember(activity, context) {
        object : PermissionRequestInterface {
            private val mutex = Mutex()
            private var currentCallback: ((Map<String, @JvmSuppressWildcards Boolean>) -> Unit)? =
                null

            override fun requestPermission(
                permission: String,
                callback: (Boolean) -> Unit,
                requestDescription: String,
            ) {
                if (activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                    callback(true)
                    return
                }

                activity.lifecycleScope.launch {
                    mutex.withLock {
                        suspendCancellableCoroutine { continuation ->
                            currentCallback = { result ->
                                callback(result.any { it.value })
                                continuation.resume(Unit) { _, _, _ -> }
                            }

                            if (requestDescription.isNotBlank() && ActivityCompat.shouldShowRequestPermissionRationale(
                                    activity,
                                    permission,
                                )
                            ) {
                                Toast.makeText(
                                    context,
                                    requestDescription,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }

                            permissionRequestHandler.launch(arrayOf(permission))
                        }
                    }
                }
            }

            override fun requestPermissions(
                permissions: Array<String>,
                callback: (Map<String, @JvmSuppressWildcards Boolean>) -> Unit,
                requestDescription: Map<String, String>,
            ) {
                val permissionsToRequest = permissions.filter {
                    activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
                }.toTypedArray()

                if (permissionsToRequest.isEmpty()) {
                    callback(permissions.associateWith { true })
                    return
                }

                activity.lifecycleScope.launch {
                    mutex.withLock {
                        suspendCancellableCoroutine { continuation ->
                            currentCallback = { result ->
                                val finalResult = permissions.associateWith { perm ->
                                    result[perm] ?: true
                                }
                                callback(finalResult)
                                continuation.resume(Unit) { _, _, _ -> }
                            }

                            permissionsToRequest.forEach { perm ->
                                if (ActivityCompat.shouldShowRequestPermissionRationale(
                                        activity,
                                        perm,
                                    )
                                ) {
                                    val msg = requestDescription[perm]
                                    if (!msg.isNullOrBlank()) {
                                        Toast.makeText(
                                            activity,
                                            msg,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            }

                            permissionRequestHandler.launch(permissionsToRequest)
                        }
                    }
                }
            }

            fun onPermissionRequestCallback(result: Map<String, @JvmSuppressWildcards Boolean>) = currentCallback?.invoke(result)
        }
    }

    permissionRequestHandler = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = permissionRequestInterface::onPermissionRequestCallback,
    )

    CompositionLocalProvider(
        LocalOverscrollFactory provides overscrollFactory,
        LocalStretchOverscrollCompensationState provides stretchOverscrollCompensationState,
        LocalPermissionRequestInterface provides permissionRequestInterface,
        LocalNavigator provides navigator,
        LocalRepositoryModuleInstall provides installRepositoryModule,
        LocalDensity provides density,
        // 页面切换手势：主界面与所有内页分页共用同一个档位。
        LocalPagerInterceptionMode provides PagerInterceptionMode.entries.getOrElse(
            settings.pagerInterceptionMode,
        ) { PagerInterceptionMode.CrossAxisInterceptor },
    ) {
        IntentDispatcher(intentChannel)
        val navCornerRadius = rememberDeviceCornerRadius(defaultRadius = 0.dp)
        val roundAllCorners =
            settings.predictiveBackAnimation == PredictiveBackAnimation.AOSP ||
                settings.predictiveBackAnimation == PredictiveBackAnimation.Scale ||
                settings.predictiveBackAnimation == PredictiveBackAnimation.KernelSUClassic ||
                settings.predictiveBackAnimation == PredictiveBackAnimation.Depth ||
                settings.predictiveBackAnimation == PredictiveBackAnimation.SlideUp ||
                settings.predictiveBackAnimation == PredictiveBackAnimation.Cube
        val backdropColor = MaterialTheme.colorScheme.surfaceContainer
        val effects = remember(navCornerRadius, roundAllCorners, backdropColor) {
            NavDisplayEffects(
                enableCornerClip = true,
                cornerClipRadius = if (roundAllCorners && navCornerRadius <= 0.dp) 32.dp else navCornerRadius,
                cornerClipMode = if (roundAllCorners) NavCornerClipMode.All else NavCornerClipMode.Leading,
                dimAmount = 0.5f,
                backdropColor = backdropColor,
                blockInputDuringTransition = false,
            )
        }
        val transition = remember(
            settings.predictiveBackAnimation,
            settings.predictiveBackExitDirection,
        ) {
            installerNavTransition(
                animation = settings.predictiveBackAnimation,
                exitDirection = settings.predictiveBackExitDirection,
            )
        }
        val swipeBackDirection = if (settings.enableSwipeDismiss) {
            when (LocalLayoutDirection.current) {
                LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
                LayoutDirection.Ltr -> NavSwipeDirection.LeftToRight
            }
        } else {
            NavSwipeDirection.None
        }
        val interceptPredictiveBack =
            settings.predictiveBackAnimation == PredictiveBackAnimation.None && backStack.size > 1

        Box(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = onBack,
            transition = transition,
            effects = effects,
        ) {
            entry<Route.About>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    AboutScreen()
                }
            }
            entry<Route.IconCredits>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    IconCreditsScreen()
                }
            }
            entry<Route.Credits>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    CreditsScreen()
                }
            }
            entry<Route.Modifications>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    ModificationsScreen()
                }
            }
            entry<Route.OpenSourceLicense>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    OpenSourceLicenseScreen()
                }
            }
            entry<Route.Sulog>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    SulogScreen()
                }
            }
            entry<Route.Main>(swipeDismiss = NavSwipeDirection.None) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    MainScreen(pagerInterceptionMode = settings.pagerInterceptionMode)
                }
            }
            entry<Route.AppProfileTemplate>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    AppProfileTemplateScreen()
                }
            }
            entry<Route.TemplateEditor>(swipeDismiss = NavSwipeDirection.None) { key ->
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    TemplateEditorScreen(
                        templateId = key.templateId,
                        readOnly = key.readOnly,
                        isCreation = key.isCreation,
                    )
                }
            }
            entry<Route.AppProfile>(swipeDismiss = swipeBackDirection) { key ->
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    AppProfileScreen(key.uid, key.packageName)
                }
            }
            entry<Route.ModuleRepo>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    ModuleRepoScreen()
                }
            }
            entry<Route.ModuleRepoDetail>(swipeDismiss = swipeBackDirection) { key ->
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    OnlineModuleDetailScreen(key.moduleId, key.repositoryUrl)
                }
            }
            entry<Route.RepositoryModules>(swipeDismiss = swipeBackDirection) { key ->
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) { ModuleRepoScreen(key.repositoryUrl) }
            }
            entry<Route.AddRepository>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) { AddRepositoryScreen() }
            }
            entry<Route.Install>(swipeDismiss = NavSwipeDirection.None) { key ->
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    InstallScreen(key.preselectedKernelUri)
                }
            }
            entry<Route.Flash>(swipeDismiss = swipeBackDirection) { key ->
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    FlashScreen(key.toFlashIt())
                }
            }
            entry<Route.ScriptRun>(swipeDismiss = NavSwipeDirection.None) { key ->
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    ScriptRunScreen(key)
                }
            }
            entry<Route.ExecuteModuleAction>(swipeDismiss = swipeBackDirection) { key ->
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    ExecuteModuleActionScreen(key.moduleId, key.fromShortcut)
                }
            }
            entry<Route.Home>(swipeDismiss = NavSwipeDirection.None) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    MainScreen(pagerInterceptionMode = settings.pagerInterceptionMode)
                }
            }
            entry<Route.SuperUser>(swipeDismiss = NavSwipeDirection.None) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    MainScreen(pagerInterceptionMode = settings.pagerInterceptionMode)
                }
            }
            entry<Route.Module>(swipeDismiss = NavSwipeDirection.None) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    MainScreen(pagerInterceptionMode = settings.pagerInterceptionMode)
                }
            }
            entry<Route.Settings>(swipeDismiss = NavSwipeDirection.None) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    MainScreen(pagerInterceptionMode = settings.pagerInterceptionMode)
                }
            }
            entry<Route.SettingsRoot>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    SettingsRootPage()
                }
            }
            entry<Route.SettingsProfile>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    SettingsProfilePage()
                }
            }
            entry<Route.SettingsUpdate>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    SettingsUpdatePage()
                }
            }
            entry<Route.SettingsAi>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    SettingsAiPage()
                }
            }
            entry<Route.SettingsTools>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    SettingsToolsPage()
                }
            }
            entry<Route.Language>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    LanguageScreen()
                }
            }
            entry<Route.SystemInfo>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    SystemInfoPage(0.dp)
                }
            }
            entry<Route.Notifications>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    NotificationsPage()
                }
            }
            entry<Route.ThemeSettings>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    ThemeSettingsScreen(settingsViewModel = settingsViewModel)
                }
            }
            entry<Route.SuSFSConfig>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    SuSFSConfigScreen()
                }
            }
            entry<Route.SuSFSLkm>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    SuSFSLkmScreen()
                }
            }
            entry<Route.AceHide>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    AceHideScreen()
                }
            }
            entry<Route.MountController>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    MountControllerScreen()
                }
            }
            entry<Route.ZygiskEngine>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    ZygiskScreen()
                }
            }
            entry<Route.KuaiMaoRoot>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    KuaiMaoRootScreen(onBack = { navigator.pop() })
                }
            }
            entry<Route.UmountManager>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    UmountManagerScreen()
                }
            }
            entry<Route.DynamicManager>(swipeDismiss = swipeBackDirection) {
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    DynamicManagerScreen()
                }
            }
            entry<Route.KernelFlash>(swipeDismiss = NavSwipeDirection.None) { key ->
                ManagerNavEntry(
                    interceptPredictiveBack = interceptPredictiveBack,
                    onBack = onBack,
                    themeConfig = themeConfig,
                    backgroundRenderState = backgroundRenderState,
                    useBlur = useBlur,
                ) {
                    KernelFlashScreen(key.kernelUri, key.selectedSlot, key.skipKsud)
                }
            }
        }
        }
    }
}

@Composable
private fun ManagerNavEntry(
    interceptPredictiveBack: Boolean,
    onBack: () -> Unit,
    themeConfig: ThemeConfig,
    backgroundRenderState: BackgroundRenderState,
    useBlur: Boolean,
    content: @Composable () -> Unit,
) {
    val transitionRunning = LocalNavTransitionScope.current.isRunning
    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = interceptPredictiveBack,
        onBackCompleted = onBack,
    )
    val snackBarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var backgroundBlurAnchorCoordinates by remember {
        mutableStateOf<LayoutCoordinates?>(null)
    }

    LaunchedEffect(backgroundRenderState.imagePainter) {
        if (backgroundRenderState.imagePainter == null) {
            backgroundBlurAnchorCoordinates = null
        }
    }

    // 每个路由页的根节点负责两件事：铺满不透明底色（转场时上下两页不互相透视），
    // 以及在这块底色上绘制动态背景特效。所有页面内部一律透明，
    // 所以设置、模块、超级用户这些界面看到的都是同一块底和同一套特效。
    val rootCanvasColor = KuaiMaoColors.canvasApp
    val bgEffect = themeConfig.backgroundEffect
    val bgEffectAccent = MaterialTheme.colorScheme.primary
    val bgEffectDark = isInDarkTheme(themeConfig.forceDarkMode)
    val bgEffectAmp = if (bgEffectDark) 1.00f else 1.20f
    val bgEffectClock = rememberInfiniteTransition(label = "bgEffectRoot")
    val bgEffectTime by bgEffectClock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bgEffectTime",
    )
    val bgEffectBeat by bgEffectClock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bgEffectBeat",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { transitionRunning }
            .pointerInput(transitionRunning) {
                if (transitionRunning) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
            // 没设自定义壁纸时铺实色底；设了壁纸就由下面的壁纸层负责铺满。
            // 两种情况都必须是不透明的，否则上一层路由会从这一页里透出来（整屏发灰、字重影）。
            // 动态特效不画在这里：它要盖在壁纸之上，见下面的特效层。
            .then(
                // 用「壁纸是否真的解码成功」来判断，而不是 painter 是否为 null：
                // 壁纸文件丢了的时候 painter 仍然存在，会把底色和特效一起跳掉，
                // 看起来就是「一片灰、特效全没」。
                if (backgroundRenderState.imageBitmap == null) {
                    Modifier.drawBehind { drawRect(rootCanvasColor) }
                } else {
                    Modifier
                },
            ),
    ) {
        val isPortrait = maxWidth < maxHeight || (maxHeight / maxWidth > 1.4f)
        val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer

        // 画在背景上的文字该用深色还是浅色：
        // 主题是深色 → 浅字；主题是浅色但壁纸本身很暗 → 也换成浅字，
        // 否则「设置」「系统动态」这些标题就是黑字压在暗花丛上。
        val cardConfigState: CardConfig = koinInject()
        val scrimLuminance = surfaceContainer.luminance()
        val wallpaperLuminance = if (backgroundRenderState.wallpaperReady) {
            backgroundRenderState.wallpaperLuminance
        } else {
            scrimLuminance
        }
        val dimFraction = themeConfig.backgroundDim.coerceIn(0f, 1f)
        val effectiveBackdropLuminance =
            wallpaperLuminance * (1f - dimFraction) + scrimLuminance * dimFraction
        val darkBackdrop = bgEffectDark ||
            (cardConfigState.isCustomBackgroundEnabled && effectiveBackdropLuminance < 0.46f)

        CompositionLocalProvider(
            LocalDarkBackdrop provides darkBackdrop,
            LocalOnWallpaper provides cardConfigState.isCustomBackgroundEnabled,
            LocalPortraitState provides isPortrait,
            LocalBlurState provides rememberMaterial3BlurBackdrop(
                enableBlur = useBlur,
            ),
            LocalSnackbarHost provides snackBarHostState,
            LocalBackgroundBlurAnchor provides backgroundBlurAnchorCoordinates,
        ) {
            backgroundRenderState.imagePainter?.let { painter ->
                val backgroundBitmap = backgroundRenderState.imageBitmap
                // Draw the decoded bitmap once available so the background does
                // not depend on the async painter's crossfade invalidations.
                val backgroundPainter = remember(backgroundBitmap, painter) {
                    backgroundBitmap?.let { BitmapPainter(it) } ?: painter
                }
                // 壁纸整体虚化：这是「自定义背景」最重要的一层。
                // 不糊的话整张图的花纹会直接顶到字上，卡片再怎么调透明度都看不清；
                // 糊掉之后它只当底纹用，卡片上的字才立得住。
                val wallpaperBlurRadius = (themeConfig.backgroundBlur.coerceIn(0f, 1f) * 28f).dp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            backgroundBlurAnchorCoordinates = coordinates.takeIf {
                                it.isAttached
                            }
                        }
                        .then(
                            if (themeConfig.backgroundBlur > 0.01f) {
                                Modifier.blur(
                                    radius = wallpaperBlurRadius,
                                    edgeTreatment = BlurredEdgeTreatment.Rectangle,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .paint(
                            painter = backgroundPainter,
                            contentScale = ContentScale.Crop,
                        )
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                color = surfaceContainer.copy(
                                    alpha = themeConfig.backgroundDim,
                                ),
                            )
                        },
                )
            }
            // 动态特效层：在壁纸之上、所有页面内容之下。
            // 以前只有在「没有壁纸」时才画特效，设了壁纸就整条不画了，
            // 所以用户看到的是「选了特效但什么都没有」。
            if (bgEffect != BackgroundEffect.NONE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawKuaiMaoEffect(
                                effect = bgEffect,
                                accent = bgEffectAccent,
                                time = bgEffectTime,
                                beat = bgEffectBeat,
                                amplitude = bgEffectAmp,
                                dark = bgEffectDark,
                            )
                        },
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                    ),
            ) {
                content()
            }
        }
    }
    BackHandler(enabled = transitionRunning) { }
}

private fun Route.Flash.toFlashIt(): FlashIt = when (flashType) {
    Route.Flash.TYPE_BOOT -> FlashIt.FlashBoot(
        boot = bootUri,
        lkmUri = lkmUri,
        kmi = kmi,
        ota = ota,
        partition = partition,
        allowShell = allowShell,
        enableAdb = enableAdb,
        forceBackup = forceBackup,
    )

    Route.Flash.TYPE_MODULE -> FlashIt.FlashModule(uris.firstOrNull().orEmpty())

    Route.Flash.TYPE_MODULES -> FlashIt.FlashModules(uris, currentIndex)

    Route.Flash.TYPE_MODULE_UPDATE -> FlashIt.FlashModuleUpdate(uris.firstOrNull().orEmpty())

    Route.Flash.TYPE_RESTORE -> FlashIt.FlashRestore

    Route.Flash.TYPE_UNINSTALL -> FlashIt.FlashUninstall

    else -> FlashIt.FlashModule(uris.firstOrNull().orEmpty())
}

/**
 * Remember a LayerBackdrop for Material 3 with a surfaceContainer background
 * to prevent alpha-blending artifacts.
 *
 * @param enableBlur Whether the blur effect is globally enabled.
 * @return A LayerBackdrop instance if supported and enabled, null otherwise.
 */
@Composable
fun rememberMaterial3BlurBackdrop(
    enableBlur: Boolean,
    pagerState: PagerState? = null,
    pagerPage: Int? = null,
): LayerBackdrop? {
    val themeConfig: ThemeConfig = koinInject()
    val backgroundRenderState = LocalBackgroundRenderState.current
    if (!enableBlur || !isRenderEffectSupported()) return null

    val backgroundColor =
        MaterialTheme.colorScheme.surfaceContainer
    val layoutDirection = LocalLayoutDirection.current
    val backgroundAnchor = LocalBackgroundBlurAnchor.current

    return rememberLayerBackdrop {
        if (themeConfig.isEnableBlurExp) {
            val wallpaperPainter = backgroundRenderState.imagePainter
            if (wallpaperPainter == null) {
                // 没设壁纸时必须先铺一层不透明底色：否则 backdrop 里只有
                // 页面内容（页面根是透明的），胶囊玻璃采样到透明就是一块黑。
                drawRect(backgroundColor)
            }
            wallpaperPainter?.let { painter ->
                val backgroundViewportSize = backgroundAnchor
                    ?.takeIf { it.isAttached && it.size.width > 0 && it.size.height > 0 }
                    ?.size
                    ?: backgroundRenderState.blurViewportSize
                val backgroundWidth = backgroundViewportSize.width
                    .takeIf { it > 0 }
                    ?.toFloat()
                    ?: size.width
                val backgroundHeight = backgroundViewportSize.height
                    .takeIf { it > 0 }
                    ?.toFloat()
                    ?: size.height
                val pagerViewportWidth = pagerState
                    ?.layoutInfo
                    ?.viewportSize
                    ?.width
                    ?.takeIf { it > 0 }
                    ?.toFloat()
                    ?: size.width
                val leadingNavigationWidth =
                    (backgroundWidth - pagerViewportWidth).coerceAtLeast(0f)
                val pagerViewportLeft = if (layoutDirection == LayoutDirection.Ltr) {
                    leadingNavigationWidth
                } else {
                    0f
                }
                val pageOffset = if (
                    pagerState != null &&
                    pagerPage != null &&
                    pagerPage in 0 until pagerState.pageCount
                ) {
                    pagerState.getOffsetDistanceInPages(pagerPage)
                } else {
                    0f
                }
                val physicalPageOffset = pageOffset * pagerViewportWidth *
                    if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
                val backgroundOffset = pagerViewportLeft + physicalPageOffset
                val backgroundBitmap = backgroundRenderState.imageBitmap

                if (
                    backgroundBitmap != null &&
                    backgroundBitmap.width > 0 &&
                    backgroundBitmap.height > 0
                ) {
                    val backgroundScale = maxOf(
                        backgroundWidth / backgroundBitmap.width,
                        backgroundHeight / backgroundBitmap.height,
                    )
                    val renderedLeft =
                        (backgroundWidth - backgroundBitmap.width * backgroundScale) / 2f
                    val renderedTop =
                        (backgroundHeight - backgroundBitmap.height * backgroundScale) / 2f

                    translate(
                        left = -backgroundOffset + renderedLeft,
                        top = renderedTop,
                    ) {
                        scale(
                            scaleX = backgroundScale,
                            scaleY = backgroundScale,
                            pivot = Offset.Zero,
                        ) {
                            drawImage(
                                image = backgroundBitmap,
                                filterQuality = FilterQuality.Low,
                            )
                        }
                    }
                } else {
                    translate(left = -backgroundOffset) {
                        with(painter) {
                            draw(size = Size(backgroundWidth, backgroundHeight))
                        }
                    }
                }
            }
        } else {
            drawRect(backgroundColor)
        }

        drawRect(
            color = backgroundColor.copy(alpha = themeConfig.backgroundDim),
        )

        drawContent()
    }
}
