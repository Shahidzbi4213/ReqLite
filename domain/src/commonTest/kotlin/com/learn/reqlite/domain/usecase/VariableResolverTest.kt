package com.learn.reqlite.domain.usecase

import com.learn.reqlite.domain.model.Variable
import kotlin.test.Test
import kotlin.test.assertEquals

class VariableResolverTest {

    private val resolver = VariableResolver()

    @Test
    fun testResolveSingleVariable() {
        val input = "https://{{host}}/api"
        val vars = listOf(Variable("1", "host", "example.com"))
        
        val result = resolver.resolve(input, vars)
        assertEquals("https://example.com/api", result)
    }

    @Test
    fun testResolveMultipleVariables() {
        val input = "{{protocol}}://{{host}}:{{port}}/api"
        val vars = listOf(
            Variable("1", "protocol", "http"),
            Variable("2", "host", "localhost"),
            Variable("3", "port", "8080")
        )
        
        val result = resolver.resolve(input, vars)
        assertEquals("http://localhost:8080/api", result)
    }

    @Test
    fun testResolveIgnoresDisabledVariables() {
        val input = "https://{{host}}/api"
        val vars = listOf(Variable("1", "host", "example.com", isEnabled = false))
        
        val result = resolver.resolve(input, vars)
        assertEquals("https://{{host}}/api", result) // Unresolved
    }

    @Test
    fun testResolveUnmatchedVariablesRemain() {
        val input = "https://{{host}}/{{path}}"
        val vars = listOf(Variable("1", "host", "example.com"))
        
        val result = resolver.resolve(input, vars)
        assertEquals("https://example.com/{{path}}", result) // path unresolved
    }
}
