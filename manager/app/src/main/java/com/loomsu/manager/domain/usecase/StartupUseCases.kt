package com.loomsu.manager.domain.usecase

import com.loomsu.manager.data.startup.StartupRepository

class ObserveStartupStateUseCase(
    private val repository: StartupRepository,
) {
    operator fun invoke() = repository.state
}
