package com.loomsu.manager.domain.usecase

import java.io.File
import com.loomsu.manager.data.logging.BugreportRepository

class GenerateBugreportUseCase(
    private val repository: BugreportRepository,
) {
    operator fun invoke(): File = repository.create()
}
