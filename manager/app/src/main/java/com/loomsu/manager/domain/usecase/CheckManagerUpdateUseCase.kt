package com.loomsu.manager.domain.usecase

import com.loomsu.manager.data.update.ManagerUpdateRepository
import com.loomsu.manager.domain.model.ManagerUpdateChannel
import com.loomsu.manager.domain.model.ManagerUpdateInfo

class CheckManagerUpdateUseCase(
    private val repository: ManagerUpdateRepository,
) {
    suspend operator fun invoke(channel: ManagerUpdateChannel): ManagerUpdateInfo? = when (channel) {
        ManagerUpdateChannel.STABLE -> repository.checkStableUpdate()
        ManagerUpdateChannel.BETA -> repository.checkBetaUpdate()
    }
}
