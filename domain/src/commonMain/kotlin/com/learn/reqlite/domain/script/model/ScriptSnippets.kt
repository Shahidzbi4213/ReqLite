package com.learn.reqlite.domain.script.model

import kotlinx.serialization.Serializable

@Serializable
data class ScriptSnippet(
    val id: String,
    val title: String,
    val description: String,
    val code: String,
    val isPreRequest: Boolean
)

object ScriptSnippets {
    val preRequestSnippets: List<ScriptSnippet> = listOf(
        ScriptSnippet(
            id = "pre_set_env",
            title = "Set environment variable",
            description = "Assign a value to an environment variable",
            code = "reqlite.environment.set(\"variable_key\", \"variable_value\");\n",
            isPreRequest = true
        ),
        ScriptSnippet(
            id = "pre_get_env",
            title = "Get environment variable",
            description = "Read an environment variable value",
            code = "let myVar = reqlite.environment.get(\"variable_key\");\nconsole.log(\"Resolved var: \" + myVar);\n",
            isPreRequest = true
        ),
        ScriptSnippet(
            id = "pre_timestamp",
            title = "Current timestamp",
            description = "Inject current epoch millisecond timestamp",
            code = "reqlite.environment.set(\"timestamp\", Date.now());\n",
            isPreRequest = true
        ),
        ScriptSnippet(
            id = "pre_uuid",
            title = "Generate UUID",
            description = "Generate and save a random UUID",
            code = "reqlite.environment.set(\"uuid\", uuid());\n",
            isPreRequest = true
        ),
        ScriptSnippet(
            id = "pre_add_header",
            title = "Add custom header",
            description = "Dynamically append a request header",
            code = "reqlite.request.headers.add(\"X-Request-Id\", uuid());\n",
            isPreRequest = true
        )
    )

    val testSnippets: List<ScriptSnippet> = listOf(
        ScriptSnippet(
            id = "test_status_200",
            title = "Status code: 200 OK",
            description = "Verify response status is 200",
            code = """
                reqlite.test("Status code is 200", function () {
                    reqlite.expect(response.status).toBe(200);
                });
            """.trimIndent() + "\n",
            isPreRequest = false
        ),
        ScriptSnippet(
            id = "test_status_2xx",
            title = "Status code: Successful (2xx)",
            description = "Verify response is between 200 and 299",
            code = """
                reqlite.test("Successful HTTP status code", function () {
                    reqlite.expect(response.status).toBeGreaterThanOrEqual(200);
                    reqlite.expect(response.status).toBeLessThan(300);
                });
            """.trimIndent() + "\n",
            isPreRequest = false
        ),
        ScriptSnippet(
            id = "test_response_time",
            title = "Response time < 500ms",
            description = "Ensure latency is within 500 milliseconds",
            code = """
                reqlite.test("Response time is under 500ms", function () {
                    reqlite.expect(response.responseTime).toBeLessThan(500);
                });
            """.trimIndent() + "\n",
            isPreRequest = false
        ),
        ScriptSnippet(
            id = "test_json_field",
            title = "JSON value check",
            description = "Parse response JSON and assert field value",
            code = """
                reqlite.test("Response JSON contains valid data", function () {
                    let jsonData = response.json();
                    reqlite.expect(jsonData).toBeDefined();
                });
            """.trimIndent() + "\n",
            isPreRequest = false
        ),
        ScriptSnippet(
            id = "test_save_token",
            title = "Extract & save auth token",
            description = "Extract token from JSON and set to environment",
            code = """
                reqlite.test("Extract and persist authentication token", function () {
                    let jsonData = response.json();
                    if (jsonData && jsonData.token) {
                        reqlite.environment.set("authToken", jsonData.token);
                    }
                });
            """.trimIndent() + "\n",
            isPreRequest = false
        ),
        ScriptSnippet(
            id = "test_postman_style",
            title = "Postman syntax style",
            description = "Test using standard Postman pm.* syntax",
            code = """
                pm.test("Status code is 200", function () {
                    pm.response.to.have.status(200);
                });
            """.trimIndent() + "\n",
            isPreRequest = false
        )
    )
}
