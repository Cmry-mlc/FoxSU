package com.kuaimaosu.manager.di

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
import com.kuaimaosu.manager.BuildConfig
import com.kuaimaosu.manager.data.AppSettingsRepository
import com.kuaimaosu.manager.data.application.ApplicationControlRepository
import com.kuaimaosu.manager.data.application.DynamicManagerRepository
import com.kuaimaosu.manager.data.count.CountRepository
import com.kuaimaosu.manager.data.download.DownloadRepository
import com.kuaimaosu.manager.data.file.ModuleFileRepository
import com.kuaimaosu.manager.data.flash.FlashRepository
import com.kuaimaosu.manager.data.kernel.KernelRepository
import com.kuaimaosu.manager.data.kernel.UmountRepository
import com.kuaimaosu.manager.data.logging.BugreportRepository
import com.kuaimaosu.manager.data.logging.SulogRepository
import com.kuaimaosu.manager.data.module.ModuleActionRepository
import com.kuaimaosu.manager.data.module.ModuleCatalogRepository
import com.kuaimaosu.manager.data.module.ModuleIconRepository
import com.kuaimaosu.manager.data.module.ModulePreferencesRepository
import com.kuaimaosu.manager.data.module.ModuleReleaseRepository
import com.kuaimaosu.manager.data.module.ModuleRepository
import com.kuaimaosu.manager.data.module.RepositorySourceStore
import com.kuaimaosu.manager.data.module.registry.KernelSuRegistryRepository
import com.kuaimaosu.manager.data.network.NetworkRequestRepository
import com.kuaimaosu.manager.data.network.NetworkStatusRepository
import com.kuaimaosu.manager.data.network.WebResourceRepository
import com.kuaimaosu.manager.data.packageinfo.AppIconDataSource
import com.kuaimaosu.manager.data.packageinfo.InstalledPackageCache
import com.kuaimaosu.manager.data.packageinfo.InstalledPackageRepository
import com.kuaimaosu.manager.data.packageinfo.RootServiceRepository
import com.kuaimaosu.manager.data.packageinfo.SuperUserRepository
import com.kuaimaosu.manager.data.profile.ProfileRepository
import com.kuaimaosu.manager.data.profile.ProfileTemplateRepository
import com.kuaimaosu.manager.data.script.ScriptRepository
import com.kuaimaosu.manager.data.settings.LocaleHelper
import com.kuaimaosu.manager.data.settings.LocaleRepository
import com.kuaimaosu.manager.data.settings.SettingsPlatformRepository
import com.kuaimaosu.manager.data.shell.KsuCliRepository
import com.kuaimaosu.manager.data.shell.ShortcutRepository
import com.kuaimaosu.manager.data.startup.ApplicationInitializationRepository
import com.kuaimaosu.manager.data.startup.StartupRepository
import com.kuaimaosu.manager.data.susfs.SuSFSConfigHelper
import com.kuaimaosu.manager.data.susfs.SuSFSRepository
import com.kuaimaosu.manager.data.system.HomeRuntimeRepository
import com.kuaimaosu.manager.data.system.HomeStateRepository
import com.kuaimaosu.manager.data.text.HanziToPinyin
import com.kuaimaosu.manager.data.theme.MonetCompatColorSource
import com.kuaimaosu.manager.data.theme.ThemeRepository
import com.kuaimaosu.manager.data.update.ManagerUpdateRepository
import com.kuaimaosu.manager.data.webui.WebUiRepository
import com.kuaimaosu.manager.domain.text.TextTransliterator
import com.kuaimaosu.manager.domain.usecase.AddModuleRepositoryUseCase
import com.kuaimaosu.manager.domain.usecase.AddUmountPathUseCase
import com.kuaimaosu.manager.domain.usecase.ApplyLanguageUseCase
import com.kuaimaosu.manager.domain.usecase.BackupAllowlistUseCase
import com.kuaimaosu.manager.domain.usecase.CalculateInstalledModuleSizeUseCase
import com.kuaimaosu.manager.domain.usecase.CheckFlashModuleMountUseCase
import com.kuaimaosu.manager.domain.usecase.CheckManagerUpdateUseCase
import com.kuaimaosu.manager.domain.usecase.CleanSulogUseCase
import com.kuaimaosu.manager.domain.usecase.ClearDynamicManagerUseCase
import com.kuaimaosu.manager.domain.usecase.ConfigureSuLogUseCase
import com.kuaimaosu.manager.domain.usecase.ControlAppUseCase
import com.kuaimaosu.manager.domain.usecase.DeleteProfileTemplateUseCase
import com.kuaimaosu.manager.domain.usecase.EnableSulogUseCase
import com.kuaimaosu.manager.domain.usecase.EnqueueDownloadUseCase
import com.kuaimaosu.manager.domain.usecase.EnqueueManagerUpdateUseCase
import com.kuaimaosu.manager.domain.usecase.EnsureManagerInstalledUseCase
import com.kuaimaosu.manager.domain.usecase.ExecuteFlashOperationUseCase
import com.kuaimaosu.manager.domain.usecase.ExecuteModuleActionUseCase
import com.kuaimaosu.manager.domain.usecase.ExportProfileTemplatesUseCase
import com.kuaimaosu.manager.domain.usecase.ExtractModuleIdUseCase
import com.kuaimaosu.manager.domain.usecase.ExtractModuleNameUseCase
import com.kuaimaosu.manager.domain.usecase.FetchRemoteTextUseCase
import com.kuaimaosu.manager.domain.usecase.GenerateBugreportUseCase
import com.kuaimaosu.manager.domain.usecase.GetAppProfileUseCase
import com.kuaimaosu.manager.domain.usecase.GetAppSepolicyUseCase
import com.kuaimaosu.manager.domain.usecase.GetBooleanPreferenceUseCase
import com.kuaimaosu.manager.domain.usecase.GetCatalogModuleUseCase
import com.kuaimaosu.manager.domain.usecase.GetDefaultUmountModulesUseCase
import com.kuaimaosu.manager.domain.usecase.GetHomeBasicInfoUseCase
import com.kuaimaosu.manager.domain.usecase.GetInstallEnvironmentUseCase
import com.kuaimaosu.manager.domain.usecase.GetKernelFeatureSettingsUseCase
import com.kuaimaosu.manager.domain.usecase.GetKernelStatusUseCase
import com.kuaimaosu.manager.domain.usecase.GetLastScriptRunUseCase
import com.kuaimaosu.manager.domain.usecase.GetManagerRuntimeInfoUseCase
import com.kuaimaosu.manager.domain.usecase.GetModuleReadmeUseCase
import com.kuaimaosu.manager.domain.usecase.GetModuleReleaseDetailsUseCase
import com.kuaimaosu.manager.domain.usecase.GetModuleUpdateChangelogUseCase
import com.kuaimaosu.manager.domain.usecase.GetPlatformFeatureStatusUseCase
import com.kuaimaosu.manager.domain.usecase.GetProfileTemplateUseCase
import com.kuaimaosu.manager.domain.usecase.GetStringPreferenceUseCase
import com.kuaimaosu.manager.domain.usecase.GetStringSetPreferenceUseCase
import com.kuaimaosu.manager.domain.usecase.GetSuSFSStatusUseCase
import com.kuaimaosu.manager.domain.usecase.GetSuperUserAppGroupUseCase
import com.kuaimaosu.manager.domain.usecase.ImportAllowlistUseCase
import com.kuaimaosu.manager.domain.usecase.ImportProfileTemplatesUseCase
import com.kuaimaosu.manager.domain.usecase.InitializeApplicationUseCase
import com.kuaimaosu.manager.domain.usecase.IsCatalogModuleCurrentUseCase
import com.kuaimaosu.manager.domain.usecase.IsLateLoadModeUseCase
import com.kuaimaosu.manager.domain.usecase.IsModuleUriAccessibleUseCase
import com.kuaimaosu.manager.domain.usecase.IsNetworkAvailableUseCase
import com.kuaimaosu.manager.domain.usecase.IsSoftRebootPreferredUseCase
import com.kuaimaosu.manager.domain.usecase.IsSystemLanguageSettingsUseCase
import com.kuaimaosu.manager.domain.usecase.LaunchSystemLanguageSettingsUseCase
import com.kuaimaosu.manager.domain.usecase.LoadModuleIconUseCase
import com.kuaimaosu.manager.domain.usecase.LoadSettingsPlatformUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveAssetDownloadUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveCatalogModulesUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveDownloadUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveDynamicManagerStateUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveInstalledModulesUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveKernelFlashUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveModuleCatalogRefreshingUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveProfileTemplateOfflineUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveProfileTemplateRefreshingUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveProfileTemplatesUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveRepositorySourcesUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveStartupStateUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveSulogStateUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveSuperUserStateUseCase
import com.kuaimaosu.manager.domain.usecase.ObserveUmountStateUseCase
import com.kuaimaosu.manager.domain.usecase.PeekOneshotScriptUseCase
import com.kuaimaosu.manager.domain.usecase.PrepareScriptUseCase
import com.kuaimaosu.manager.domain.usecase.RebootUseCase
import com.kuaimaosu.manager.domain.usecase.RefreshDynamicManagerUseCase
import com.kuaimaosu.manager.domain.usecase.RefreshInstalledModulesUseCase
import com.kuaimaosu.manager.domain.usecase.RefreshModuleCatalogUseCase
import com.kuaimaosu.manager.domain.usecase.RefreshProfileTemplatesUseCase
import com.kuaimaosu.manager.domain.usecase.RefreshSulogUseCase
import com.kuaimaosu.manager.domain.usecase.RefreshSuperUsersUseCase
import com.kuaimaosu.manager.domain.usecase.RefreshUmountPathsUseCase
import com.kuaimaosu.manager.domain.usecase.ReleaseScriptUseCase
import com.kuaimaosu.manager.domain.usecase.RemoveModuleRepositoryUseCase
import com.kuaimaosu.manager.domain.usecase.RemovePreferenceUseCase
import com.kuaimaosu.manager.domain.usecase.RemoveUmountPathUseCase
import com.kuaimaosu.manager.domain.usecase.RenameModuleRepositoryUseCase
import com.kuaimaosu.manager.domain.usecase.ResolveModuleUpdateUseCase
import com.kuaimaosu.manager.domain.usecase.RunScriptUseCase
import com.kuaimaosu.manager.domain.usecase.SaveModuleActionLogUseCase
import com.kuaimaosu.manager.domain.usecase.SaveProfileTemplateUseCase
import com.kuaimaosu.manager.domain.usecase.SelectDynamicManagerUseCase
import com.kuaimaosu.manager.domain.usecase.SetAppProfileUseCase
import com.kuaimaosu.manager.domain.usecase.SetAppSepolicyUseCase
import com.kuaimaosu.manager.domain.usecase.SetBooleanPreferenceUseCase
import com.kuaimaosu.manager.domain.usecase.SetDefaultUmountModulesUseCase
import com.kuaimaosu.manager.domain.usecase.SetKernelUmountEnabledUseCase
import com.kuaimaosu.manager.domain.usecase.SetManualDynamicManagerUseCase
import com.kuaimaosu.manager.domain.usecase.SetModuleEnabledUseCase
import com.kuaimaosu.manager.domain.usecase.SetModuleRemovedUseCase
import com.kuaimaosu.manager.domain.usecase.SetSelinuxHideEnabledUseCase
import com.kuaimaosu.manager.domain.usecase.SetShellSuAdbOnlyEnabledUseCase
import com.kuaimaosu.manager.domain.usecase.SetStringPreferenceUseCase
import com.kuaimaosu.manager.domain.usecase.SetStringSetPreferenceUseCase
import com.kuaimaosu.manager.domain.usecase.SetSuEnabledUseCase
import com.kuaimaosu.manager.domain.usecase.StartKernelFlashUseCase
import com.kuaimaosu.manager.domain.usecase.SuSFSConfigUseCase
import com.kuaimaosu.manager.domain.usecase.TakeModuleUriPermissionUseCase
import com.kuaimaosu.manager.domain.usecase.TransliterateTextUseCase
import com.kuaimaosu.manager.domain.usecase.UpdateAppearanceUseCase
import com.kuaimaosu.manager.domain.usecase.UpdateCachedModuleEnabledUseCase
import com.kuaimaosu.manager.domain.usecase.UpdatePlatformSettingUseCase
import com.kuaimaosu.manager.domain.usecase.ValidateSepolicyUseCase
import com.kuaimaosu.manager.ui.activity.util.ThemeUtils
import com.kuaimaosu.manager.ui.component.ZipFileDetector
import com.kuaimaosu.manager.ui.theme.BackgroundManager
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.util.Shortcut
import com.kuaimaosu.manager.ui.viewmodel.AppProfileViewModel
import com.kuaimaosu.manager.ui.viewmodel.DynamicManagerViewModel
import com.kuaimaosu.manager.ui.viewmodel.ExecuteModuleActionViewModel
import com.kuaimaosu.manager.ui.viewmodel.FlashViewModel
import com.kuaimaosu.manager.ui.viewmodel.HomeViewModel
import com.kuaimaosu.manager.ui.viewmodel.InstallViewModel
import com.kuaimaosu.manager.ui.viewmodel.KernelFlashViewModel
import com.kuaimaosu.manager.ui.viewmodel.MainIntentViewModel
import com.kuaimaosu.manager.ui.viewmodel.ModuleDetailViewModel
import com.kuaimaosu.manager.ui.viewmodel.ModuleRepoViewModel
import com.kuaimaosu.manager.ui.viewmodel.ModuleViewModel
import com.kuaimaosu.manager.ui.viewmodel.RepositorySourcesViewModel
import com.kuaimaosu.manager.ui.viewmodel.ScriptRunViewModel
import com.kuaimaosu.manager.ui.viewmodel.SettingsViewModel
import com.kuaimaosu.manager.ui.viewmodel.SuSFSViewModel
import com.kuaimaosu.manager.ui.viewmodel.SulogViewModel
import com.kuaimaosu.manager.ui.viewmodel.SuperUserViewModel
import com.kuaimaosu.manager.ui.viewmodel.TemplateEditorViewModel
import com.kuaimaosu.manager.ui.viewmodel.TemplateViewModel
import com.kuaimaosu.manager.ui.viewmodel.UmountManagerScreenViewModel
import com.kuaimaosu.manager.ui.webui.MonetColorsProvider
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
                        .header("User-Agent", "KuaiMaoSU/${BuildConfig.VERSION_CODE}")
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
