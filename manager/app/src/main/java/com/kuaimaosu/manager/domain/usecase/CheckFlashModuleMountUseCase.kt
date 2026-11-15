package com.kuaimaosu.manager.domain.usecase

import com.kuaimaosu.manager.data.flash.FlashRepository

class CheckFlashModuleMountUseCase(private val repository: FlashRepository) {
    suspend operator fun invoke(uri: String) = repository.moduleNeedsMount(uri)
}
