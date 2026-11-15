package com.kuaimaosu.manager.domain.usecase

import com.kuaimaosu.manager.data.flash.FlashRepository
import com.kuaimaosu.manager.domain.model.FlashOperation

class ExecuteFlashOperationUseCase(private val repository: FlashRepository) {
    operator fun invoke(operation: FlashOperation) = repository.execute(operation)
}
