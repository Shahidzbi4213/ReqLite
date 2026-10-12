package com.learn.reqlite.domain.script.model

data class ScriptExecutionResult(
    val testResults: List<TestAssertionResult> = emptyList(),
    val logs: List<String> = emptyList(),
    val mutatedVariables: Map<String, String> = emptyMap(),
    val mutatedHeaders: Map<String, String> = emptyMap(),
    val mutatedUrl: String? = null,
    val executionError: String? = null
)
