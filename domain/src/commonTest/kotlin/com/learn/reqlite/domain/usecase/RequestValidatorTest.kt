package com.learn.reqlite.domain.usecase

import com.learn.reqlite.domain.model.Draft
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.domain.model.RequestField
import com.learn.reqlite.domain.model.Variable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequestValidatorTest {

    private val resolver = VariableResolver()
    private val validator = RequestValidator(resolver)

    private fun createDraft(
        url: String, 
        method: HttpMethod = HttpMethod.GET,
        headers: List<RequestField> = emptyList(),
        body: RequestBody = RequestBody.NoBody
    ) = Draft(
        id = "1",
        requestId = "r1",
        method = method,
        url = url,
        headers = headers,
        body = body,
        updatedAt = 0L
    )

    @Test
    fun testValidRequestNoIssues() {
        val draft = createDraft(url = "https://example.com/api")
        val issues = validator.validate(draft, emptyList())
        assertTrue(issues.isEmpty())
    }

    @Test
    fun testEmptyUrlError() {
        val draft = createDraft(url = "   ")
        val issues = validator.validate(draft, emptyList())
        
        assertEquals(1, issues.size)
        assertTrue(issues[0] is ValidationIssue.Error)
        assertEquals("URL cannot be empty", issues[0].message)
    }

    @Test
    fun testInvalidUrlSchemeError() {
        val draft = createDraft(url = "ftp://example.com")
        val issues = validator.validate(draft, emptyList())
        
        assertEquals(1, issues.size)
        assertTrue(issues[0] is ValidationIssue.Error)
        assertEquals("URL must start with http:// or https://", issues[0].message)
    }

    @Test
    fun testUnresolvedVariableInUrlError() {
        val draft = createDraft(url = "https://{{host}}/api")
        val issues = validator.validate(draft, emptyList()) // No vars provided
        
        assertEquals(1, issues.size)
        assertTrue(issues[0] is ValidationIssue.Error)
        assertEquals("Unresolved variables in URL", issues[0].message)
    }

    @Test
    fun testGetWithBodyWarning() {
        val draft = createDraft(
            url = "https://example.com",
            method = HttpMethod.GET,
            body = RequestBody.TextBody("test", "text/plain")
        )
        val issues = validator.validate(draft, emptyList())
        
        assertEquals(1, issues.size)
        assertTrue(issues[0] is ValidationIssue.Warning)
        assertEquals("GET request with a body is not recommended", issues[0].message)
    }

    @Test
    fun testDuplicateContentTypeWarning() {
        val headers = listOf(
            RequestField("1", "Content-Type", "application/json", true),
            RequestField("2", "content-type", "text/html", true)
        )
        val draft = createDraft(url = "https://example.com", headers = headers)
        val issues = validator.validate(draft, emptyList())
        
        assertEquals(1, issues.size)
        assertTrue(issues[0] is ValidationIssue.Warning)
        assertEquals("Duplicate Content-Type headers found", issues[0].message)
    }
}
