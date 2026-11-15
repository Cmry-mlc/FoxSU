package com.kuaimaosu.manager.domain.usecase

import com.kuaimaosu.manager.data.application.ApplicationControlRepository

class RebootUseCase(
    private val repository: ApplicationControlRepository,
) {
    suspend operator fun invoke(reason: String = "") = repository.reboot(reason)
}
