package com.kuaimaosu.manager.domain.model

data class WebUiModuleInfo(
    val name: String,
    val enabled: Boolean,
    val remove: Boolean,
    val hasWebUi: Boolean,
)
