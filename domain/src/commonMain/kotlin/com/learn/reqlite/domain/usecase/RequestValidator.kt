package com.learn.reqlite.domain.usecase

import com.learn.reqlite.domain.model.Draft
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.domain.model.Variable

sealed class ValidationIssue(open val message: String) {
    data class Error(override val message: String) : ValidationIssue(message)
    data class Warning(override val message: String) : ValidationIssue(message)
}

class RequestValidator(private val variableResolver: VariableResolver) {

    fun validate(draft: Draft, environmentVariables: List<Variable>): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        val resolvedUrl = variableResolver.resolve(draft.url, environmentVariables)

        // Errors
        if (resolvedUrl.isBlank()) {
            issues.add(ValidationIssue.Error("URL cannot be empty"))
        } else if (!resolvedUrl.startsWith("http://") && !resolvedUrl.startsWith("https://")) {
            issues.add(ValidationIssue.Error("URL must start with http:// or https://"))
        } else if (resolvedUrl.contains(" ")) {
            issues.add(ValidationIssue.Error("URL contains invalid characters"))
        }

        // Check unresolved variables left in URL (could be a missing variable)
        if (resolvedUrl.contains("{{") && resolvedUrl.contains("}}")) {
            // Depending on strictness, we might flag unresolved variables as errors or warnings
            // We'll treat them as errors for the URL as they usually break resolution
            issues.add(ValidationIssue.Error("Unresolved variables in URL"))
        }

        // Warnings
        if (draft.method == HttpMethod.GET && draft.body !is RequestBody.NoBody) {
            issues.add(ValidationIssue.Warning("GET request with a body is not recommended"))
        }

        val enabledHeaders = draft.headers.filter { it.isEnabled }
        val contentTypes = enabledHeaders.filter { 
            variableResolver.resolve(it.key, environmentVariables).equals("Content-Type", ignoreCase = true) 
        }
        
        if (contentTypes.size > 1) {
            issues.add(ValidationIssue.Warning("Duplicate Content-Type headers found"))
        }
        
        draft.queryParams.filter { it.isEnabled }.forEach { param ->
            val resolvedKey = variableResolver.resolve(param.key, environmentVariables)
            if (resolvedKey.isBlank()) {
                issues.add(ValidationIssue.Warning("Blank enabled query parameter key found"))
            }
        }
        
        draft.headers.filter { it.isEnabled }.forEach { header ->
            val resolvedKey = variableResolver.resolve(header.key, environmentVariables)
            if (resolvedKey.isBlank()) {
                issues.add(ValidationIssue.Warning("Blank enabled header key found"))
            }
        }

        return issues
    }
}
