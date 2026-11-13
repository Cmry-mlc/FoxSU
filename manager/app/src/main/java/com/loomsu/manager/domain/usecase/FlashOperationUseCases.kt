package com.loomsu.manager.domain.usecase

import com.loomsu.manager.data.flash.FlashRepository
import com.loomsu.manager.domain.model.FlashOperation

class ExecuteFlashOperationUseCase(private val repository: FlashRepository) {
    operator fun invoke(operation: FlashOperation) = repository.execute(operation)
}
