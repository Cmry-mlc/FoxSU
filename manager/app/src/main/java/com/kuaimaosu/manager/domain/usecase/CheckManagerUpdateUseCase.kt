package com.kuaimaosu.manager.domain.usecase

import com.kuaimaosu.manager.data.update.ManagerUpdateRepository
import com.kuaimaosu.manager.domain.model.ManagerUpdateChannel
import com.kuaimaosu.manager.domain.model.ManagerUpdateInfo

class CheckManagerUpdateUseCase(
    private val repository: ManagerUpdateRepository,
) {
    suspend operator fun invoke(channel: ManagerUpdateChannel): ManagerUpdateInfo? = when (channel) {
        ManagerUpdateChannel.STABLE -> repository.checkStableUpdate()
        ManagerUpdateChannel.BETA -> repository.checkBetaUpdate()
    }
}
