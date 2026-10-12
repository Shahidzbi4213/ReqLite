package com.learn.reqlite.domain.script

import kotlin.test.*

class ScriptEngineTest {

    private val engine: ScriptEngine = ScriptInterpreter()

    @Test
    fun testPreRequestScriptSetsEnvironmentVariableAndHeader() {
        val preContext = PreRequestContext(
            url = "https://api.example.com/data",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer initial"),
            queryParams = emptyMap(),
            bodyContent = null,
            environmentVariables = mapOf("baseUrl" to "https://api.example.com")
        )

        val script = """
            let base = reqlite.environment.get("baseUrl");
            reqlite.environment.set("token", "secret_123");
            reqlite.request.headers.add("X-Calculated", "val_" + (10 + 20));
            console.log("Base URL is: " + base);
        """.trimIndent()

        val result = engine.executePreRequest(script, preContext)

        assertNull(result.executionError)
        assertEquals("secret_123", result.mutatedVariables["token"])
        assertEquals("val_30", result.mutatedHeaders["X-Calculated"])
        assertTrue(result.logs.any { it.contains("Base URL is: https://api.example.com") })
    }

    @Test
    fun testPreRequestScriptUtilities() {
        val preContext = PreRequestContext(
            url = "https://api.example.com",
            method = "POST",
            environmentVariables = emptyMap()
        )

        val script = """
            let generatedId = uuid();
            let b64 = btoa("hello world");
            let decoded = atob(b64);
            reqlite.environment.set("myUuid", generatedId);
            reqlite.environment.set("b64Encoded", b64);
            reqlite.environment.set("b64Decoded", decoded);
        """.trimIndent()

        val result = engine.executePreRequest(script, preContext)

        assertNull(result.executionError)
        assertTrue(result.mutatedVariables["myUuid"]!!.isNotBlank())
        assertEquals("aGVsbG8gd29ybGQ=", result.mutatedVariables["b64Encoded"])
        assertEquals("hello world", result.mutatedVariables["b64Decoded"])
    }

    @Test
    fun testPostRequestTestAssertionsPassing() {
        val postContext = PostRequestContext(
            statusCode = 200,
            statusText = "OK",
            responseTimeMs = 150L,
            responseHeaders = mapOf("Content-Type" to "application/json", "X-Server" to "Ktor"),
            responseBody = """{"user": {"id": 42, "name": "ReqLite User"}, "tags": ["mobile", "kmp"], "active": true}""",
            requestUrl = "https://api.example.com/users",
            requestMethod = "GET",
            environmentVariables = emptyMap()
        )

        val script = """
            reqlite.test("Status code is 200", function() {
                reqlite.expect(response.status).toBe(200);
            });

            reqlite.test("Response time is under 500ms", function() {
                reqlite.expect(response.responseTime).toBeLessThan(500);
            });

            reqlite.test("User data assertion", function() {
                let data = response.json();
                reqlite.expect(data.user.id).toBe(42);
                reqlite.expect(data.user.name).toBe("ReqLite User");
                reqlite.expect(data.active).toBe(true);
                reqlite.expect(data.tags).toContain("kmp");
            });

            reqlite.test("Header verification", function() {
                reqlite.expect(response.headers.get("X-Server")).toBe("Ktor");
                reqlite.expect(response.headers.has("Content-Type")).toBe(true);
            });
        """.trimIndent()

        val result = engine.executePostRequestTests(script, postContext)

        assertNull(result.executionError)
        assertEquals(4, result.testResults.size)
        assertTrue(result.testResults.all { it.passed })
    }

    @Test
    fun testPostRequestTestAssertionsFailing() {
        val postContext = PostRequestContext(
            statusCode = 500,
            statusText = "Internal Server Error",
            responseTimeMs = 850L,
            responseHeaders = emptyMap(),
            responseBody = """{"error": "Database error"}""",
            requestUrl = "https://api.example.com/error",
            requestMethod = "GET"
        )

        val script = """
            reqlite.test("Should be 200 OK", function() {
                reqlite.expect(response.status).toBe(200);
            });

            reqlite.test("Should be under 200ms", function() {
                reqlite.expect(response.responseTime).toBeLessThan(200);
            });
        """.trimIndent()

        val result = engine.executePostRequestTests(script, postContext)

        assertNull(result.executionError)
        assertEquals(2, result.testResults.size)
        assertFalse(result.testResults[0].passed)
        assertTrue(result.testResults[0].errorMessage!!.contains("Expected 500 to equal 200"))
        assertFalse(result.testResults[1].passed)
        assertTrue(result.testResults[1].errorMessage!!.contains("to be below 200"))
    }

    @Test
    fun testPostmanSyntaxCompatibility() {
        val postContext = PostRequestContext(
            statusCode = 201,
            statusText = "Created",
            responseTimeMs = 120L,
            responseHeaders = mapOf("Location" to "/users/101"),
            responseBody = """{"token": "jwt_token_sample", "id": 101}""",
            requestUrl = "https://api.example.com/users",
            requestMethod = "POST"
        )

        val script = """
            pm.test("Status code is 201", function () {
                pm.response.to.have.status(201);
            });

            pm.test("Response time check", () => {
                pm.expect(pm.response.responseTime).to.be.below(500);
            });

            pm.test("Save auth token to environment", function () {
                let json = pm.response.json();
                pm.expect(json.token).to.not.be.null;
                pm.environment.set("authToken", json.token);
            });
        """.trimIndent()

        val result = engine.executePostRequestTests(script, postContext)

        assertNull(result.executionError)
        assertEquals(3, result.testResults.size)
        assertTrue(result.testResults.all { it.passed })
        assertEquals("jwt_token_sample", result.mutatedVariables["authToken"])
    }
}
