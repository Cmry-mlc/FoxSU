package com.kuaimaosu.manager.domain.usecase

import com.kuaimaosu.manager.data.flash.FlashRepository
import com.kuaimaosu.manager.domain.model.InstallEnvironment

class GetInstallEnvironmentUseCase(
    private val repository: FlashRepository,
) {
    fun cached(): InstallEnvironment? = repository.installEnvironment.value

    suspend operator fun invoke(forceRefresh: Boolean = false) = repository.getInstallEnvironment(forceRefresh)
}
