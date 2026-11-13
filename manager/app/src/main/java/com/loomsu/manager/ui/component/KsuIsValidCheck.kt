package com.loomsu.manager.ui.component

import androidx.compose.runtime.Composable
import com.loomsu.manager.domain.model.KernelStatus

@Composable
inline fun KsuIsValid(
    status: KernelStatus,
    content: @Composable () -> Unit,
) {
    if (status.isFullFeatured) {
        content()
    }
}
