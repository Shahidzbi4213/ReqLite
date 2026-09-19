package com.learn.reqlite.domain.usecase

import com.learn.reqlite.domain.model.Variable

class VariableResolver {
    fun resolve(input: String, variables: List<Variable>): String {
        val enabledVars = variables.filter { it.isEnabled }.associateBy { it.key }
        val regex = Regex("\\{\\{([^}]+)\\}\\}")
        
        return regex.replace(input) { matchResult ->
            val key = matchResult.groupValues[1]
            enabledVars[key]?.value ?: matchResult.value
        }
    }
}
