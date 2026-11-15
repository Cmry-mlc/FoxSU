package com.kuaimaosu.manager

import android.app.Application
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import com.kuaimaosu.manager.di.appModules
import com.kuaimaosu.manager.domain.usecase.InitializeApplicationUseCase
import com.kuaimaosu.manager.util.CrashHandler
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KernelSUApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName()
            if (processName.endsWith("MagicaService")) {
                // avoid loading unnecessary thing when starting MagicaService
                return
            }
        }

        val koin = startKoin {
            androidLogger()
            androidContext(this@KernelSUApplication)
            modules(appModules)
        }.koin
        runBlocking(Dispatchers.IO) {
            koin.get<InitializeApplicationUseCase>()()
        }
    }
}
