package com.loomsu.manager.data.count

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import com.loomsu.manager.data.shell.KsuCliRepository

data class CountState(
    val superuserCount: Int = 0,
    val moduleCount: Int = 0,
)

class CountRepository(
    private val ksuCliRepository: KsuCliRepository,
) {
    private val mutableState = MutableStateFlow(CountState())
    val state: StateFlow<CountState> = mutableState.asStateFlow()

    suspend fun refresh() {
        val (superuserCount, moduleCount) = withContext(Dispatchers.IO) {
            // ksud 未就绪或设备未越狱时保持 0，不让异常冒到主线程。
            val users = runCatching { ksuCliRepository.getSuperuserCount() }.getOrDefault(0)
            val modules = runCatching { ksuCliRepository.getModuleCount() }.getOrDefault(0)
            users to modules
        }
        mutableState.update {
            it.copy(superuserCount = superuserCount, moduleCount = moduleCount)
        }
    }
}
