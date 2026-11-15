package com.kuaimaosu.manager.data.application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kuaimaosu.manager.Natives
import com.kuaimaosu.manager.data.shell.KsuCliRepository

class ApplicationControlRepository(
    private val ksuCliRepository: KsuCliRepository,
) {
    suspend fun ensureManagerInstalled(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (Natives.isFullFeatured() && ksuCliRepository.rootAvailable()) {
                ksuCliRepository.install()
            }
        }
    }

    suspend fun reboot(reason: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { ksuCliRepository.reboot(reason) }
    }
}
