package com.learn.reqlite.domain.script.model

import kotlinx.serialization.Serializable

@Serializable
data class TestAssertionResult(
    val name: String,
    val passed: Boolean,
    val errorMessage: String? = null,
    val durationMs: Long = 0L
)
