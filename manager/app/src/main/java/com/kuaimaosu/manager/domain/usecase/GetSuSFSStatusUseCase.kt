package com.kuaimaosu.manager.domain.usecase

import com.kuaimaosu.manager.data.susfs.SuSFSRepository

class GetSuSFSStatusUseCase(private val repository: SuSFSRepository) {
    suspend operator fun invoke() = repository.getStatus()
}
