package com.loomsu.manager.di

import coil.ImageLoader
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import okhttp3.Cache
import okhttp3.OkHttpClient
import com.loomsu.manager.BuildConfig
import com.loomsu.manager.data.AppSettingsRepository
import com.loomsu.manager.data.application.ApplicationControlRepository
import com.loomsu.manager.data.application.DynamicManagerRepository
import com.loomsu.manager.data.count.CountRepository
import com.loomsu.manager.data.download.DownloadRepository
import com.loomsu.manager.data.file.ModuleFileRepository
import com.loomsu.manager.data.flash.FlashRepository
import com.loomsu.manager.data.kernel.KernelRepository
import com.loomsu.manager.data.kernel.UmountRepository
import com.loomsu.manager.data.logging.BugreportRepository
import com.loomsu.manager.data.logging.SulogRepository
import com.loomsu.manager.data.module.ModuleActionRepository
import com.loomsu.manager.data.module.ModuleCatalogRepository
import com.loomsu.manager.data.module.ModuleIconRepository
import com.loomsu.manager.data.module.ModulePreferencesRepository
import com.loomsu.manager.data.module.ModuleReleaseRepository
import com.loomsu.manager.data.module.ModuleRepository
import com.loomsu.manager.data.module.RepositorySourceStore
import com.loomsu.manager.data.module.registry.KernelSuRegistryRepository
import com.loomsu.manager.data.network.NetworkRequestRepository
import com.loomsu.manager.data.network.NetworkStatusRepository
import com.loomsu.manager.data.network.WebResourceRepository
import com.loomsu.manager.data.packageinfo.AppIconDataSource
import com.loomsu.manager.data.packageinfo.InstalledPackageCache
import com.loomsu.manager.data.packageinfo.InstalledPackageRepository
import com.loomsu.manager.data.packageinfo.RootServiceRepository
import com.loomsu.manager.data.packageinfo.SuperUserRepository
import com.loomsu.manager.data.profile.ProfileRepository
import com.loomsu.manager.data.profile.ProfileTemplateRepository
import com.loomsu.manager.data.script.ScriptRepository
import com.loomsu.manager.data.settings.LocaleHelper
import com.loomsu.manager.data.settings.LocaleRepository
import com.loomsu.manager.data.settings.SettingsPlatformRepository
import com.loomsu.manager.data.shell.KsuCliRepository
import com.loomsu.manager.data.shell.ShortcutRepository
import com.loomsu.manager.data.startup.ApplicationInitializationRepository
import com.loomsu.manager.data.startup.StartupRepository
import com.loomsu.manager.data.susfs.SuSFSConfigHelper
import com.loomsu.manager.data.susfs.SuSFSRepository
import com.loomsu.manager.data.system.HomeRuntimeRepository
import com.loomsu.manager.data.system.HomeStateRepository
import com.loomsu.manager.data.text.HanziToPinyin
import com.loomsu.manager.data.theme.MonetCompatColorSource
import com.loomsu.manager.data.theme.ThemeRepository
import com.loomsu.manager.data.update.ManagerUpdateRepository
import com.loomsu.manager.data.webui.WebUiRepository
import com.loomsu.manager.domain.text.TextTransliterator
import com.loomsu.manager.domain.usecase.AddModuleRepositoryUseCase
import com.loomsu.manager.domain.usecase.AddUmountPathUseCase
import com.loomsu.manager.domain.usecase.ApplyLanguageUseCase
import com.loomsu.manager.domain.usecase.BackupAllowlistUseCase
import com.loomsu.manager.domain.usecase.CalculateInstalledModuleSizeUseCase
import com.loomsu.manager.domain.usecase.CheckFlashModuleMountUseCase
import com.loomsu.manager.domain.usecase.CheckManagerUpdateUseCase
import com.loomsu.manager.domain.usecase.CleanSulogUseCase
import com.loomsu.manager.domain.usecase.ClearDynamicManagerUseCase
import com.loomsu.manager.domain.usecase.ConfigureSuLogUseCase
import com.loomsu.manager.domain.usecase.ControlAppUseCase
import com.loomsu.manager.domain.usecase.DeleteProfileTemplateUseCase
import com.loomsu.manager.domain.usecase.EnableSulogUseCase
import com.loomsu.manager.domain.usecase.EnqueueDownloadUseCase
import com.loomsu.manager.domain.usecase.EnqueueManagerUpdateUseCase
import com.loomsu.manager.domain.usecase.EnsureManagerInstalledUseCase
import com.loomsu.manager.domain.usecase.ExecuteFlashOperationUseCase
import com.loomsu.manager.domain.usecase.ExecuteModuleActionUseCase
import com.loomsu.manager.domain.usecase.ExportProfileTemplatesUseCase
import com.loomsu.manager.domain.usecase.ExtractModuleIdUseCase
import com.loomsu.manager.domain.usecase.ExtractModuleNameUseCase
import com.loomsu.manager.domain.usecase.FetchRemoteTextUseCase
import com.loomsu.manager.domain.usecase.GenerateBugreportUseCase
import com.loomsu.manager.domain.usecase.GetAppProfileUseCase
import com.loomsu.manager.domain.usecase.GetAppSepolicyUseCase
import com.loomsu.manager.domain.usecase.GetBooleanPreferenceUseCase
import com.loomsu.manager.domain.usecase.GetCatalogModuleUseCase
import com.loomsu.manager.domain.usecase.GetDefaultUmountModulesUseCase
import com.loomsu.manager.domain.usecase.GetHomeBasicInfoUseCase
import com.loomsu.manager.domain.usecase.GetInstallEnvironmentUseCase
import com.loomsu.manager.domain.usecase.GetKernelFeatureSettingsUseCase
import com.loomsu.manager.domain.usecase.GetKernelStatusUseCase
import com.loomsu.manager.domain.usecase.GetLastScriptRunUseCase
import com.loomsu.manager.domain.usecase.GetManagerRuntimeInfoUseCase
import com.loomsu.manager.domain.usecase.GetModuleReadmeUseCase
import com.loomsu.manager.domain.usecase.GetModuleReleaseDetailsUseCase
import com.loomsu.manager.domain.usecase.GetModuleUpdateChangelogUseCase
import com.loomsu.manager.domain.usecase.GetPlatformFeatureStatusUseCase
import com.loomsu.manager.domain.usecase.GetProfileTemplateUseCase
import com.loomsu.manager.domain.usecase.GetStringPreferenceUseCase
import com.loomsu.manager.domain.usecase.GetStringSetPreferenceUseCase
import com.loomsu.manager.domain.usecase.GetSuSFSStatusUseCase
import com.loomsu.manager.domain.usecase.GetSuperUserAppGroupUseCase
import com.loomsu.manager.domain.usecase.ImportAllowlistUseCase
import com.loomsu.manager.domain.usecase.ImportProfileTemplatesUseCase
import com.loomsu.manager.domain.usecase.InitializeApplicationUseCase
import com.loomsu.manager.domain.usecase.IsCatalogModuleCurrentUseCase
import com.loomsu.manager.domain.usecase.IsLateLoadModeUseCase
import com.loomsu.manager.domain.usecase.IsModuleUriAccessibleUseCase
import com.loomsu.manager.domain.usecase.IsNetworkAvailableUseCase
import com.loomsu.manager.domain.usecase.IsSoftRebootPreferredUseCase
import com.loomsu.manager.domain.usecase.IsSystemLanguageSettingsUseCase
import com.loomsu.manager.domain.usecase.LaunchSystemLanguageSettingsUseCase
import com.loomsu.manager.domain.usecase.LoadModuleIconUseCase
import com.loomsu.manager.domain.usecase.LoadSettingsPlatformUseCase
import com.loomsu.manager.domain.usecase.ObserveAssetDownloadUseCase
import com.loomsu.manager.domain.usecase.ObserveCatalogModulesUseCase
import com.loomsu.manager.domain.usecase.ObserveDownloadUseCase
import com.loomsu.manager.domain.usecase.ObserveDynamicManagerStateUseCase
import com.loomsu.manager.domain.usecase.ObserveInstalledModulesUseCase
import com.loomsu.manager.domain.usecase.ObserveKernelFlashUseCase
import com.loomsu.manager.domain.usecase.ObserveModuleCatalogRefreshingUseCase
import com.loomsu.manager.domain.usecase.ObserveProfileTemplateOfflineUseCase
import com.loomsu.manager.domain.usecase.ObserveProfileTemplateRefreshingUseCase
import com.loomsu.manager.domain.usecase.ObserveProfileTemplatesUseCase
import com.loomsu.manager.domain.usecase.ObserveRepositorySourcesUseCase
import com.loomsu.manager.domain.usecase.ObserveStartupStateUseCase
import com.loomsu.manager.domain.usecase.ObserveSulogStateUseCase
import com.loomsu.manager.domain.usecase.ObserveSuperUserStateUseCase
import com.loomsu.manager.domain.usecase.ObserveUmountStateUseCase
import com.loomsu.manager.domain.usecase.PeekOneshotScriptUseCase
import com.loomsu.manager.domain.usecase.PrepareScriptUseCase
import com.loomsu.manager.domain.usecase.RebootUseCase
import com.loomsu.manager.domain.usecase.RefreshDynamicManagerUseCase
import com.loomsu.manager.domain.usecase.RefreshInstalledModulesUseCase
import com.loomsu.manager.domain.usecase.RefreshModuleCatalogUseCase
import com.loomsu.manager.domain.usecase.RefreshProfileTemplatesUseCase
import com.loomsu.manager.domain.usecase.RefreshSulogUseCase
import com.loomsu.manager.domain.usecase.RefreshSuperUsersUseCase
import com.loomsu.manager.domain.usecase.RefreshUmountPathsUseCase
import com.loomsu.manager.domain.usecase.ReleaseScriptUseCase
import com.loomsu.manager.domain.usecase.RemoveModuleRepositoryUseCase
import com.loomsu.manager.domain.usecase.RemovePreferenceUseCase
import com.loomsu.manager.domain.usecase.RemoveUmountPathUseCase
import com.loomsu.manager.domain.usecase.RenameModuleRepositoryUseCase
import com.loomsu.manager.domain.usecase.ResolveModuleUpdateUseCase
import com.loomsu.manager.domain.usecase.RunScriptUseCase
import com.loomsu.manager.domain.usecase.SaveModuleActionLogUseCase
import com.loomsu.manager.domain.usecase.SaveProfileTemplateUseCase
import com.loomsu.manager.domain.usecase.SelectDynamicManagerUseCase
import com.loomsu.manager.domain.usecase.SetAppProfileUseCase
import com.loomsu.manager.domain.usecase.SetAppSepolicyUseCase
import com.loomsu.manager.domain.usecase.SetBooleanPreferenceUseCase
import com.loomsu.manager.domain.usecase.SetDefaultUmountModulesUseCase
import com.loomsu.manager.domain.usecase.SetKernelUmountEnabledUseCase
import com.loomsu.manager.domain.usecase.SetManualDynamicManagerUseCase
import com.loomsu.manager.domain.usecase.SetModuleEnabledUseCase
import com.loomsu.manager.domain.usecase.SetModuleRemovedUseCase
import com.loomsu.manager.domain.usecase.SetSelinuxHideEnabledUseCase
import com.loomsu.manager.domain.usecase.SetShellSuAdbOnlyEnabledUseCase
import com.loomsu.manager.domain.usecase.SetStringPreferenceUseCase
import com.loomsu.manager.domain.usecase.SetStringSetPreferenceUseCase
import com.loomsu.manager.domain.usecase.SetSuEnabledUseCase
import com.loomsu.manager.domain.usecase.StartKernelFlashUseCase
import com.loomsu.manager.domain.usecase.SuSFSConfigUseCase
import com.loomsu.manager.domain.usecase.TakeModuleUriPermissionUseCase
import com.loomsu.manager.domain.usecase.TransliterateTextUseCase
import com.loomsu.manager.domain.usecase.UpdateAppearanceUseCase
import com.loomsu.manager.domain.usecase.UpdateCachedModuleEnabledUseCase
import com.loomsu.manager.domain.usecase.UpdatePlatformSettingUseCase
import com.loomsu.manager.domain.usecase.ValidateSepolicyUseCase
import com.loomsu.manager.ui.activity.util.ThemeUtils
import com.loomsu.manager.ui.component.ZipFileDetector
import com.loomsu.manager.ui.theme.BackgroundManager
import com.loomsu.manager.ui.theme.CardConfig
import com.loomsu.manager.ui.theme.ThemeConfig
import com.loomsu.manager.ui.util.Shortcut
import com.loomsu.manager.ui.viewmodel.AppProfileViewModel
import com.loomsu.manager.ui.viewmodel.DynamicManagerViewModel
import com.loomsu.manager.ui.viewmodel.ExecuteModuleActionViewModel
import com.loomsu.manager.ui.viewmodel.FlashViewModel
import com.loomsu.manager.ui.viewmodel.HomeViewModel
import com.loomsu.manager.ui.viewmodel.InstallViewModel
import com.loomsu.manager.ui.viewmodel.KernelFlashViewModel
import com.loomsu.manager.ui.viewmodel.MainIntentViewModel
import com.loomsu.manager.ui.viewmodel.ModuleDetailViewModel
import com.loomsu.manager.ui.viewmodel.ModuleRepoViewModel
import com.loomsu.manager.ui.viewmodel.ModuleViewModel
import com.loomsu.manager.ui.viewmodel.RepositorySourcesViewModel
import com.loomsu.manager.ui.viewmodel.ScriptRunViewModel
import com.loomsu.manager.ui.viewmodel.SettingsViewModel
import com.loomsu.manager.ui.viewmodel.SuSFSViewModel
import com.loomsu.manager.ui.viewmodel.SulogViewModel
import com.loomsu.manager.ui.viewmodel.SuperUserViewModel
import com.loomsu.manager.ui.viewmodel.TemplateEditorViewModel
import com.loomsu.manager.ui.viewmodel.TemplateViewModel
import com.loomsu.manager.ui.viewmodel.UmountManagerScreenViewModel
import com.loomsu.manager.ui.webui.MonetColorsProvider
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val applicationScopeQualifier = named("applicationScope")

val coreModule = module {
    single<CoroutineScope>(applicationScopeQualifier) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
    single {
        OkHttpClient.Builder()
            .cache(Cache(File(androidApplication().cacheDir, "okhttp"), 10L * 1024L * 1024L))
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "LoomSU/${BuildConfig.VERSION_CODE}")
                        .header("Accept-Language", Locale.getDefault().toLanguageTag())
                        .build(),
                )
            }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }
    single {
        val application = androidApplication()
        val iconSize = application.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        ImageLoader.Builder(application)
            .components {
                add(AppIconKeyer())
                add(AppIconFetcher.Factory(iconSize, false, application))
            }
            .build()
    }
}

val repositoryModule = module {
    single { KsuCliRepository(androidApplication()) }
    singleOf(::CountRepository)
    singleOf(::InstalledPackageCache)
    singleOf(::AppIconDataSource)
    singleOf(::RootServiceRepository)
    singleOf(::InstalledPackageRepository)
    single {
        SuperUserRepository(
            application = get(),
            cache = get(),
            installedPackageRepository = get(),
            profileRepository = get(),
            applicationScope = get(applicationScopeQualifier),
        )
    }
    single {
        AppSettingsRepository(
            context = androidApplication(),
            applicationScope = get(applicationScopeQualifier),
        )
    }
    singleOf(::StartupRepository)
    single {
        ApplicationInitializationRepository(
            application = get(),
            imageLoader = get(),
            applicationScope = get(applicationScopeQualifier),
            flashRepository = get(),
            ksuCliRepository = get(),
        )
    }
    singleOf(::ManagerUpdateRepository)
    singleOf(::ApplicationControlRepository)
    singleOf(::DownloadRepository)
    single { FlashRepository(get(), get(applicationScopeQualifier), get(), get()) }
    singleOf(::KernelRepository)
    singleOf(::HomeRuntimeRepository)
    singleOf(::HomeStateRepository)
    singleOf(::NetworkStatusRepository)
    singleOf(::NetworkRequestRepository)
    singleOf(::DynamicManagerRepository)
    singleOf(::SulogRepository)
    singleOf(::BugreportRepository)
    singleOf(::UmountRepository)
    singleOf(::ModuleCatalogRepository)
    single { ModuleReleaseRepository(get(), get(applicationScopeQualifier)) }
    singleOf(::ModuleIconRepository)
    single { KernelSuRegistryRepository(get(), get(), get(), get()) }
    singleOf(::RepositorySourceStore)
    singleOf(::ModuleRepository)
    singleOf(::ModulePreferencesRepository)
    singleOf(::ModuleActionRepository)
    single { ScriptRepository(androidApplication(), get(), get(applicationScopeQualifier)) }
    singleOf(::WebResourceRepository)
    singleOf(::WebUiRepository)
    singleOf(::ModuleFileRepository)
    singleOf(::ProfileRepository)
    singleOf(::ProfileTemplateRepository)
    singleOf(::SuSFSConfigHelper)
    singleOf(::SuSFSRepository)
    singleOf(::MonetCompatColorSource)
    singleOf(::ThemeRepository)
    single {
        val themeRepository = get<ThemeRepository>()
        ThemeConfig(themeRepository::defaultSeedColor)
    }
    singleOf(::CardConfig)
    singleOf(::BackgroundManager)
    singleOf(::ThemeUtils)
    singleOf(::LocaleHelper)
    singleOf(::LocaleRepository)
    singleOf(::SettingsPlatformRepository)
    singleOf(::ShortcutRepository)
    singleOf(::Shortcut)
    singleOf(::MonetColorsProvider)
    singleOf(::ZipFileDetector)
    single { HanziToPinyin.create() } bind TextTransliterator::class
}

val useCaseModule = module {
    factoryOf(::InitializeApplicationUseCase)
    factoryOf(::GetHomeBasicInfoUseCase)
    factoryOf(::IsNetworkAvailableUseCase)
    factoryOf(::LoadSettingsPlatformUseCase)
    factoryOf(::UpdateAppearanceUseCase)
    factoryOf(::UpdatePlatformSettingUseCase)
    factoryOf(::GetPlatformFeatureStatusUseCase)
    factoryOf(::IsSoftRebootPreferredUseCase)
    factoryOf(::CheckManagerUpdateUseCase)
    factoryOf(::EnsureManagerInstalledUseCase)
    factoryOf(::RebootUseCase)
    factoryOf(::EnqueueDownloadUseCase)
    factoryOf(::EnqueueManagerUpdateUseCase)
    factoryOf(::ObserveDownloadUseCase)
    factoryOf(::ObserveAssetDownloadUseCase)
    factoryOf(::GetKernelStatusUseCase)
    factoryOf(::GetInstallEnvironmentUseCase)
    factoryOf(::ExecuteFlashOperationUseCase)
    factoryOf(::CheckFlashModuleMountUseCase)
    factoryOf(::GetManagerRuntimeInfoUseCase)
    factoryOf(::GetKernelFeatureSettingsUseCase)
    factoryOf(::SetSuEnabledUseCase)
    factoryOf(::SetKernelUmountEnabledUseCase)
    factoryOf(::SetShellSuAdbOnlyEnabledUseCase)
    factoryOf(::ConfigureSuLogUseCase)
    factoryOf(::SetSelinuxHideEnabledUseCase)
    factoryOf(::SetDefaultUmountModulesUseCase)
    factoryOf(::IsLateLoadModeUseCase)
    factoryOf(::GetAppProfileUseCase)
    factoryOf(::SetAppProfileUseCase)
    factoryOf(::GetAppSepolicyUseCase)
    factoryOf(::SetAppSepolicyUseCase)
    factoryOf(::ControlAppUseCase)
    factoryOf(::ValidateSepolicyUseCase)
    factoryOf(::GetDefaultUmountModulesUseCase)
    factoryOf(::GetSuSFSStatusUseCase)
    factoryOf(::SuSFSConfigUseCase)
    factoryOf(::ApplyLanguageUseCase)
    factoryOf(::IsSystemLanguageSettingsUseCase)
    factoryOf(::LaunchSystemLanguageSettingsUseCase)
    factoryOf(::GenerateBugreportUseCase)
    factoryOf(::ObserveStartupStateUseCase)
    factoryOf(::GetSuperUserAppGroupUseCase)
    factoryOf(::ObserveCatalogModulesUseCase)
    factoryOf(::ObserveModuleCatalogRefreshingUseCase)
    factoryOf(::RefreshModuleCatalogUseCase)
    factoryOf(::GetCatalogModuleUseCase)
    factoryOf(::IsCatalogModuleCurrentUseCase)
    factoryOf(::GetModuleReadmeUseCase)
    factoryOf(::GetModuleReleaseDetailsUseCase)
    factoryOf(::LoadModuleIconUseCase)
    factoryOf(::ResolveModuleUpdateUseCase)
    factoryOf(::GetModuleUpdateChangelogUseCase)
    factoryOf(::ObserveRepositorySourcesUseCase)
    factoryOf(::AddModuleRepositoryUseCase)
    factoryOf(::RenameModuleRepositoryUseCase)
    factoryOf(::RemoveModuleRepositoryUseCase)
    factoryOf(::ObserveProfileTemplatesUseCase)
    factoryOf(::ObserveProfileTemplateRefreshingUseCase)
    factoryOf(::ObserveProfileTemplateOfflineUseCase)
    factoryOf(::RefreshProfileTemplatesUseCase)
    factoryOf(::GetProfileTemplateUseCase)
    factoryOf(::SaveProfileTemplateUseCase)
    factoryOf(::DeleteProfileTemplateUseCase)
    factoryOf(::ImportProfileTemplatesUseCase)
    factoryOf(::ExportProfileTemplatesUseCase)
    factoryOf(::GetBooleanPreferenceUseCase)
    factoryOf(::SetBooleanPreferenceUseCase)
    factoryOf(::GetStringPreferenceUseCase)
    factoryOf(::SetStringPreferenceUseCase)
    factoryOf(::GetStringSetPreferenceUseCase)
    factoryOf(::SetStringSetPreferenceUseCase)
    factoryOf(::ObserveDynamicManagerStateUseCase)
    factoryOf(::RefreshDynamicManagerUseCase)
    factoryOf(::SelectDynamicManagerUseCase)
    factoryOf(::SetManualDynamicManagerUseCase)
    factoryOf(::ClearDynamicManagerUseCase)
    factoryOf(::ObserveSulogStateUseCase)
    factoryOf(::RefreshSulogUseCase)
    factoryOf(::EnableSulogUseCase)
    factoryOf(::CleanSulogUseCase)
    factoryOf(::ObserveUmountStateUseCase)
    factoryOf(::RefreshUmountPathsUseCase)
    factoryOf(::AddUmountPathUseCase)
    factoryOf(::RemoveUmountPathUseCase)
    factoryOf(::ObserveKernelFlashUseCase)
    factoryOf(::StartKernelFlashUseCase)
    factoryOf(::RemovePreferenceUseCase)
    factoryOf(::ObserveSuperUserStateUseCase)
    factoryOf(::RefreshSuperUsersUseCase)
    factoryOf(::BackupAllowlistUseCase)
    factoryOf(::ImportAllowlistUseCase)
    factoryOf(::FetchRemoteTextUseCase)
    factoryOf(::IsModuleUriAccessibleUseCase)
    factoryOf(::TakeModuleUriPermissionUseCase)
    factoryOf(::ExtractModuleNameUseCase)
    factoryOf(::ExtractModuleIdUseCase)
    factoryOf(::ObserveInstalledModulesUseCase)
    factoryOf(::RefreshInstalledModulesUseCase)
    factoryOf(::CalculateInstalledModuleSizeUseCase)
    factoryOf(::UpdateCachedModuleEnabledUseCase)
    factoryOf(::ExecuteModuleActionUseCase)
    factoryOf(::PeekOneshotScriptUseCase)
    factoryOf(::PrepareScriptUseCase)
    factoryOf(::ReleaseScriptUseCase)
    factoryOf(::RunScriptUseCase)
    factoryOf(::GetLastScriptRunUseCase)
    factoryOf(::SaveModuleActionLogUseCase)
    factoryOf(::SetModuleEnabledUseCase)
    factoryOf(::SetModuleRemovedUseCase)
    factoryOf(::TransliterateTextUseCase)
}

val viewModelModule = module {
    viewModel { parameters -> ScriptRunViewModel(parameters[0], get(), get(), get(), get(), get()) }
    viewModel { parameters ->
        AppProfileViewModel(
            uid = parameters[0],
            packageName = parameters[1],
            getAppGroup = get(),
            getProfile = get(),
            getDefaultUmountModules = get(),
            setProfile = get(),
            getSepolicy = get(),
            setSepolicy = get(),
            controlApp = get(),
            validateSepolicy = get(),
        )
    }
    viewModelOf(::HomeViewModel)
    viewModelOf(::InstallViewModel)
    viewModelOf(::MainIntentViewModel)
    viewModelOf(::KernelFlashViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ModuleViewModel)
    viewModelOf(::SuperUserViewModel)
    viewModelOf(::SuSFSViewModel)
    viewModel { parameters -> ModuleRepoViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), parameters.getOrNull<String>()) }
    viewModel { parameters -> ModuleDetailViewModel(parameters[0], parameters[1], get(), get(), get(), get(), get(), get(), get()) }
    viewModelOf(::RepositorySourcesViewModel)
    viewModelOf(::TemplateViewModel)
    viewModel { parameters ->
        TemplateEditorViewModel(
            templateId = parameters[0],
            readOnly = parameters[1],
            isCreation = parameters[2],
            getTemplate = get(),
            saveTemplate = get(),
            deleteTemplate = get(),
        )
    }
    viewModelOf(::SulogViewModel)
    viewModelOf(::DynamicManagerViewModel)
    viewModelOf(::FlashViewModel)
    viewModelOf(::UmountManagerScreenViewModel)
    viewModel { parameters ->
        ExecuteModuleActionViewModel(
            moduleId = parameters[0],
            executeModuleAction = get(),
            saveModuleActionLog = get(),
        )
    }
}

val appModules = listOf(coreModule, repositoryModule, useCaseModule, viewModelModule)
