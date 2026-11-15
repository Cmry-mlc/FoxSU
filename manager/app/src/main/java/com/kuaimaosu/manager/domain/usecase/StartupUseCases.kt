package com.kuaimaosu.manager.domain.usecase

import com.kuaimaosu.manager.data.startup.StartupRepository

class ObserveStartupStateUseCase(
    private val repository: StartupRepository,
) {
    operator fun invoke() = repository.state
}
