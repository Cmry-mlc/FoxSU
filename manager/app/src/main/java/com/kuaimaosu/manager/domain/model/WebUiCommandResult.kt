package com.kuaimaosu.manager.domain.model

data class WebUiCommandResult(
    val code: Int,
    val stdout: String,
    val stderr: String,
)
