package com.learn.reqlite.ui.environment

import com.learn.reqlite.domain.model.Environment

data class EnvironmentUiModel(
    val id: String,
    val name: String,
    val color: String? = null,
    val isProtected: Boolean = false,
    val variableCount: Int = 0,
    val requireConfirmation: Boolean = isProtected
) {
    companion object {
        fun fromDomain(environment: Environment, customProtected: Boolean? = null): EnvironmentUiModel {
            val isProtected = customProtected ?: isProtectedName(environment.name)
            return EnvironmentUiModel(
                id = environment.id,
                name = environment.name,
                color = environment.color,
                isProtected = isProtected,
                variableCount = environment.variables.size,
                requireConfirmation = isProtected
            )
        }

        fun isProtectedName(name: String): Boolean {
            val lower = name.lowercase().trim()
            return lower.contains("prod") ||
                    lower.contains("production") ||
                    lower.contains("live") ||
                    lower.contains("main")
        }
    }
}
