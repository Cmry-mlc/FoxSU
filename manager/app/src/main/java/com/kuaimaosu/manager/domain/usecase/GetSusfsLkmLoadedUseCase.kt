package com.kuaimaosu.manager.domain.usecase

import com.kuaimaosu.manager.data.susfs.SusfsLkmRepository

/**
 * 主页大卡只需要「SUSFS 守卫模块是否已加载」这一个布尔量。
 * 内核本身内建 SUSFS 时这里返回 false，那种情况由 SUSFS 版本号另行展示。
 */
class GetSusfsLkmLoadedUseCase(private val repository: SusfsLkmRepository) {
    suspend operator fun invoke(): Boolean = runCatching { repository.status().loaded }
        .getOrDefault(false)
}
