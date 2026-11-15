package com.kuaimaosu.manager.domain.usecase

import java.io.File
import com.kuaimaosu.manager.data.logging.BugreportRepository

class GenerateBugreportUseCase(
    private val repository: BugreportRepository,
) {
    operator fun invoke(): File = repository.create()
}
