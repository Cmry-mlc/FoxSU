package com.kuaimaosu.manager.ui.component

import androidx.compose.runtime.Composable
import com.kuaimaosu.manager.domain.model.KernelStatus

@Composable
inline fun KsuIsValid(
    status: KernelStatus,
    content: @Composable () -> Unit,
) {
    if (status.isFullFeatured) {
        content()
    }
}
