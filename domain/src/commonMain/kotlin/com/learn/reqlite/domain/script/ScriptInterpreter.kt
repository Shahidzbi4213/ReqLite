package com.learn.reqlite.domain.script

import com.learn.reqlite.domain.script.model.ScriptExecutionResult
import com.learn.reqlite.domain.script.model.TestAssertionResult
import com.learn.reqlite.utils.common.nowMs
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.*

class ScriptAssertionException(message: String) : Exception(message)

class ScriptInterpreter : ScriptEngine {

    @OptIn(ExperimentalUuidApi::class)
    override fun executePreRequest(script: String?, context: PreRequestContext): ScriptExecutionResult {
        if (script.isNullOrBlank()) {
            return ScriptExecutionResult()
        }

        val logs = mutableListOf<String>()
        val mutatedVars = mutableMapOf<String, String>()
        val mutatedHeaders = mutableMapOf<String, String>()
        var mutatedUrl: String? = null

        val globalScope = Scope()
        setupPreRequestEnvironment(
            scope = globalScope,
            context = context,
            logs = logs,
            mutatedVars = mutatedVars,
            mutatedHeaders = mutatedHeaders,
            onUrlChanged = { mutatedUrl = it }
        )

        val executionError = runScriptSafely(script, globalScope, logs)
        return ScriptExecutionResult(
            testResults = emptyList(),
            logs = logs,
            mutatedVariables = mutatedVars,
            mutatedHeaders = mutatedHeaders,
            mutatedUrl = mutatedUrl,
            executionError = executionError
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun executePostRequestTests(script: String?, context: PostRequestContext): ScriptExecutionResult {
        if (script.isNullOrBlank()) {
            return ScriptExecutionResult()
        }

        val logs = mutableListOf<String>()
        val testResults = mutableListOf<TestAssertionResult>()
        val mutatedVars = mutableMapOf<String, String>()

        val globalScope = Scope()
        setupPostRequestEnvironment(
            scope = globalScope,
            context = context,
            logs = logs,
            testResults = testResults,
            mutatedVars = mutatedVars
        )

        val executionError = runScriptSafely(script, globalScope, logs)
        return ScriptExecutionResult(
            testResults = testResults,
            logs = logs,
            mutatedVariables = mutatedVars,
            mutatedHeaders = emptyMap(),
            mutatedUrl = null,
            executionError = executionError
        )
    }

    private fun runScriptSafely(script: String, scope: Scope, logs: MutableList<String>): String? {
        return try {
            val tokens = Tokenizer(script).tokenize()
            val parser = Parser(tokens)
            val statements = parser.parseProgram()
            val evaluator = Evaluator()
            for (stmt in statements) {
                evaluator.execute(stmt, scope)
            }
            null
        } catch (e: ScriptAssertionException) {
            logs.add("Assertion Error: ${e.message}")
            e.message
        } catch (e: Exception) {
            logs.add("Script Error: ${e.message ?: "Unknown error"}")
            e.message ?: "Unknown script error"
        }
    }

    // =========================================================================
    // Scope Setup
    // =========================================================================

    @OptIn(ExperimentalUuidApi::class)
    private fun setupSharedGlobals(
        scope: Scope,
        logs: MutableList<String>,
        environmentVars: Map<String, String>,
        mutatedVars: MutableMap<String, String>
    ) {
        val mergedVars = environmentVars.toMutableMap()

        // console.log
        val consoleObj = ScriptValue.ObjectValue()
        consoleObj.set("log", ScriptValue.HostFunction("log") { args ->
            val msg = args.joinToString(" ") { it.toDisplayString() }
            logs.add(msg)
            ScriptValue.Undefined
        })
        consoleObj.set("warn", ScriptValue.HostFunction("warn") { args ->
            val msg = args.joinToString(" ") { it.toDisplayString() }
            logs.add("[WARN] $msg")
            ScriptValue.Undefined
        })
        consoleObj.set("error", ScriptValue.HostFunction("error") { args ->
            val msg = args.joinToString(" ") { it.toDisplayString() }
            logs.add("[ERROR] $msg")
            ScriptValue.Undefined
        })
        scope.define("console", consoleObj)

        // Utility functions
        scope.define("uuid", ScriptValue.HostFunction("uuid") {
            ScriptValue.StringVal(Uuid.random().toString())
        })
        scope.define("timestamp", ScriptValue.HostFunction("timestamp") {
            ScriptValue.NumberVal(nowMs().toDouble())
        })
        @OptIn(ExperimentalEncodingApi::class)
        scope.define("btoa", ScriptValue.HostFunction("btoa") { args ->
            val str = args.firstOrNull()?.toDisplayString() ?: ""
            ScriptValue.StringVal(Base64.Default.encode(str.encodeToByteArray()))
        })
        @OptIn(ExperimentalEncodingApi::class)
        scope.define("atob", ScriptValue.HostFunction("atob") { args ->
            val str = args.firstOrNull()?.toDisplayString() ?: ""
            val decoded = try { Base64.Default.decode(str).decodeToString() } catch (_: Exception) { "" }
            ScriptValue.StringVal(decoded)
        })
        scope.define("parseInt", ScriptValue.HostFunction("parseInt") { args ->
            val str = args.firstOrNull()?.toDisplayString() ?: ""
            val num = str.trim().toDoubleOrNull()?.toLong() ?: 0L
            ScriptValue.NumberVal(num.toDouble())
        })
        scope.define("parseFloat", ScriptValue.HostFunction("parseFloat") { args ->
            val str = args.firstOrNull()?.toDisplayString() ?: ""
            val num = str.trim().toDoubleOrNull() ?: 0.0
            ScriptValue.NumberVal(num)
        })
        scope.define("String", ScriptValue.HostFunction("String") { args ->
            ScriptValue.StringVal(args.firstOrNull()?.toDisplayString() ?: "")
        })
        scope.define("Number", ScriptValue.HostFunction("Number") { args ->
            val num = args.firstOrNull()?.toNumber() ?: 0.0
            ScriptValue.NumberVal(num)
        })
        scope.define("Boolean", ScriptValue.HostFunction("Boolean") { args ->
            ScriptValue.BooleanVal(args.firstOrNull()?.isTruthy() ?: false)
        })

        // Date.now()
        val dateObj = ScriptValue.ObjectValue()
        dateObj.set("now", ScriptValue.HostFunction("now") {
            ScriptValue.NumberVal(nowMs().toDouble())
        })
        scope.define("Date", dateObj)

        // JSON.parse & JSON.stringify
        val jsonObj = ScriptValue.ObjectValue()
        jsonObj.set("parse", ScriptValue.HostFunction("parse") { args ->
            val raw = args.firstOrNull()?.toDisplayString() ?: ""
            try {
                val element = Json.parseToJsonElement(raw)
                element.toScriptValue()
            } catch (e: Exception) {
                throw ScriptAssertionException("JSON.parse error: ${e.message}")
            }
        })
        jsonObj.set("stringify", ScriptValue.HostFunction("stringify") { args ->
            val element = args.firstOrNull()?.toJsonElement() ?: JsonNull
            ScriptValue.StringVal(element.toString())
        })
        scope.define("JSON", jsonObj)

        // Environment Object
        val envObj = ScriptValue.ObjectValue()
        envObj.set("get", ScriptValue.HostFunction("get") { args ->
            val key = args.firstOrNull()?.toDisplayString() ?: ""
            val value = mutatedVars[key] ?: mergedVars[key]
            if (value != null) ScriptValue.StringVal(value) else ScriptValue.Null
        })
        envObj.set("set", ScriptValue.HostFunction("set") { args ->
            val key = args.getOrNull(0)?.toDisplayString() ?: ""
            val value = args.getOrNull(1)?.toDisplayString() ?: ""
            if (key.isNotBlank()) {
                mergedVars[key] = value
                mutatedVars[key] = value
            }
            ScriptValue.Undefined
        })
        envObj.set("has", ScriptValue.HostFunction("has") { args ->
            val key = args.firstOrNull()?.toDisplayString() ?: ""
            val exists = mutatedVars.containsKey(key) || mergedVars.containsKey(key)
            ScriptValue.BooleanVal(exists)
        })
        envObj.set("unset", ScriptValue.HostFunction("unset") { args ->
            val key = args.firstOrNull()?.toDisplayString() ?: ""
            mergedVars.remove(key)
            mutatedVars.remove(key)
            ScriptValue.Undefined
        })
        envObj.set("toObject", ScriptValue.HostFunction("toObject") {
            val all = mergedVars + mutatedVars
            val result = ScriptValue.ObjectValue()
            all.forEach { (k, v) -> result.set(k, ScriptValue.StringVal(v)) }
            result
        })

        // Variables Object (alias to environment)
        val varsObj = ScriptValue.ObjectValue()
        varsObj.set("get", envObj.get("get"))
        varsObj.set("set", envObj.get("set"))

        scope.define("__envObj", envObj)
        scope.define("__varsObj", varsObj)
    }

    private fun setupPreRequestEnvironment(
        scope: Scope,
        context: PreRequestContext,
        logs: MutableList<String>,
        mutatedVars: MutableMap<String, String>,
        mutatedHeaders: MutableMap<String, String>,
        onUrlChanged: (String) -> Unit
    ) {
        setupSharedGlobals(scope, logs, context.environmentVariables, mutatedVars)

        val envObj = scope.resolve("__envObj") ?: ScriptValue.ObjectValue()
        val varsObj = scope.resolve("__varsObj") ?: ScriptValue.ObjectValue()

        // request object
        var currentUrl = context.url
        var currentMethod = context.method
        val headerMap = context.headers.toMutableMap()

        val reqObj = ScriptValue.ObjectValue()
        reqObj.set("url", ScriptValue.StringVal(currentUrl))
        reqObj.set("method", ScriptValue.StringVal(currentMethod))

        val headersObj = ScriptValue.ObjectValue()
        headersObj.set("add", ScriptValue.HostFunction("add") { args ->
            val key = args.getOrNull(0)?.toDisplayString() ?: ""
            val value = args.getOrNull(1)?.toDisplayString() ?: ""
            if (key.isNotBlank()) {
                headerMap[key] = value
                mutatedHeaders[key] = value
            }
            ScriptValue.Undefined
        })
        headersObj.set("set", headersObj.get("add"))
        headersObj.set("get", ScriptValue.HostFunction("get") { args ->
            val key = args.firstOrNull()?.toDisplayString() ?: ""
            val v = headerMap[key]
            if (v != null) ScriptValue.StringVal(v) else ScriptValue.Null
        })
        headersObj.set("remove", ScriptValue.HostFunction("remove") { args ->
            val key = args.firstOrNull()?.toDisplayString() ?: ""
            headerMap.remove(key)
            mutatedHeaders.remove(key)
            ScriptValue.Undefined
        })
        reqObj.set("headers", headersObj)

        val reqliteObj = ScriptValue.ObjectValue()
        reqliteObj.set("environment", envObj)
        reqliteObj.set("variables", varsObj)
        reqliteObj.set("request", reqObj)
        reqliteObj.set("uuid", scope.resolve("uuid") ?: ScriptValue.Null)
        reqliteObj.set("timestamp", scope.resolve("timestamp") ?: ScriptValue.Null)

        scope.define("reqlite", reqliteObj)
        scope.define("pm", reqliteObj)
        scope.define("request", reqObj)
    }

    private fun setupPostRequestEnvironment(
        scope: Scope,
        context: PostRequestContext,
        logs: MutableList<String>,
        testResults: MutableList<TestAssertionResult>,
        mutatedVars: MutableMap<String, String>
    ) {
        setupSharedGlobals(scope, logs, context.environmentVariables, mutatedVars)

        val envObj = scope.resolve("__envObj") ?: ScriptValue.ObjectValue()
        val varsObj = scope.resolve("__varsObj") ?: ScriptValue.ObjectValue()

        // Response Object
        val respObj = ScriptValue.ObjectValue()
        respObj.set("status", ScriptValue.NumberVal(context.statusCode.toDouble()))
        respObj.set("code", ScriptValue.NumberVal(context.statusCode.toDouble()))
        respObj.set("statusText", ScriptValue.StringVal(context.statusText))
        respObj.set("responseTime", ScriptValue.NumberVal(context.responseTimeMs.toDouble()))

        val respHeadersObj = ScriptValue.ObjectValue()
        respHeadersObj.set("get", ScriptValue.HostFunction("get") { args ->
            val name = args.firstOrNull()?.toDisplayString()?.lowercase() ?: ""
            val found = context.responseHeaders.entries.firstOrNull { it.key.lowercase() == name }?.value
            if (found != null) ScriptValue.StringVal(found) else ScriptValue.Null
        })
        respHeadersObj.set("has", ScriptValue.HostFunction("has") { args ->
            val name = args.firstOrNull()?.toDisplayString()?.lowercase() ?: ""
            val exists = context.responseHeaders.keys.any { it.lowercase() == name }
            ScriptValue.BooleanVal(exists)
        })
        respObj.set("headers", respHeadersObj)

        respObj.set("text", ScriptValue.HostFunction("text") {
            ScriptValue.StringVal(context.responseBody)
        })

        // Lazy json parser
        var cachedJson: ScriptValue? = null
        respObj.set("json", ScriptValue.HostFunction("json") {
            if (cachedJson == null) {
                cachedJson = try {
                    val elem = Json.parseToJsonElement(context.responseBody)
                    elem.toScriptValue()
                } catch (e: Exception) {
                    throw ScriptAssertionException("Invalid response JSON: ${e.message}")
                }
            }
            cachedJson ?: ScriptValue.Null
        })

        // Postman Chai style response chaining: pm.response.to.have.status(200)
        respObj.set("to", createResponseChaiAssertion(context))

        // Assertion Builder: expect(actual)
        val expectFunction = ScriptValue.HostFunction("expect") { args ->
            val actual = args.firstOrNull() ?: ScriptValue.Undefined
            ScriptValue.ExpectationValue(actual, isNegated = false)
        }

        // test runner: reqlite.test(name, fn) / pm.test(name, fn)
        val testFunction = ScriptValue.HostFunction("test") { args ->
            val testName = args.getOrNull(0)?.toDisplayString() ?: "Test"
            val fn = args.getOrNull(1)

            val startTime = nowMs()
            var passed = true
            var errorMsg: String? = null

            if (fn is ScriptValue.FunctionVal) {
                try {
                    val evaluator = Evaluator()
                    evaluator.callFunction(fn, emptyList())
                } catch (e: ScriptAssertionException) {
                    passed = false
                    errorMsg = e.message
                } catch (e: Exception) {
                    passed = false
                    errorMsg = e.message ?: "Test failed with unexpected error"
                }
            } else if (fn is ScriptValue.HostFunction) {
                try {
                    fn.fn(emptyList())
                } catch (e: ScriptAssertionException) {
                    passed = false
                    errorMsg = e.message
                } catch (e: Exception) {
                    passed = false
                    errorMsg = e.message ?: "Test failed"
                }
            } else {
                passed = false
                errorMsg = "Test callback function missing or invalid"
            }

            val durationMs = nowMs() - startTime
            testResults.add(
                TestAssertionResult(
                    name = testName,
                    passed = passed,
                    errorMessage = errorMsg,
                    durationMs = durationMs
                )
            )
            ScriptValue.Undefined
        }

        val reqliteObj = ScriptValue.ObjectValue()
        reqliteObj.set("environment", envObj)
        reqliteObj.set("variables", varsObj)
        reqliteObj.set("response", respObj)
        reqliteObj.set("test", testFunction)
        reqliteObj.set("expect", expectFunction)

        scope.define("reqlite", reqliteObj)
        scope.define("pm", reqliteObj)
        scope.define("response", respObj)
    }

    private fun createResponseChaiAssertion(context: PostRequestContext): ScriptValue.ObjectValue {
        val toObj = ScriptValue.ObjectValue()
        val haveObj = ScriptValue.ObjectValue()
        val beObj = ScriptValue.ObjectValue()

        haveObj.set("status", ScriptValue.HostFunction("status") { args ->
            val expectedCode = args.firstOrNull()?.toNumber()?.toInt() ?: 200
            if (context.statusCode != expectedCode) {
                throw ScriptAssertionException("Expected status $expectedCode but got ${context.statusCode}")
            }
            ScriptValue.Undefined
        })
        haveObj.set("header", ScriptValue.HostFunction("header") { args ->
            val name = args.firstOrNull()?.toDisplayString()?.lowercase() ?: ""
            val exists = context.responseHeaders.keys.any { it.lowercase() == name }
            if (!exists) {
                throw ScriptAssertionException("Expected response to have header '$name'")
            }
            ScriptValue.Undefined
        })

        beObj.set("success", ScriptValue.HostFunction("success") {
            if (context.statusCode !in 200..299) {
                throw ScriptAssertionException("Expected 2xx success status but got ${context.statusCode}")
            }
            ScriptValue.Undefined
        })
        beObj.set("clientError", ScriptValue.HostFunction("clientError") {
            if (context.statusCode !in 400..499) {
                throw ScriptAssertionException("Expected 4xx client error status but got ${context.statusCode}")
            }
            ScriptValue.Undefined
        })
        beObj.set("serverError", ScriptValue.HostFunction("serverError") {
            if (context.statusCode !in 500..599) {
                throw ScriptAssertionException("Expected 5xx server error status but got ${context.statusCode}")
            }
            ScriptValue.Undefined
        })

        toObj.set("have", haveObj)
        toObj.set("be", beObj)
        return toObj
    }
}

// =============================================================================
// AST & Values
// =============================================================================

sealed class ScriptValue {
    object Null : ScriptValue()
    object Undefined : ScriptValue()
    data class BooleanVal(val value: Boolean) : ScriptValue()
    data class NumberVal(val value: Double) : ScriptValue()
    data class StringVal(val value: String) : ScriptValue()
    data class ArrayVal(val elements: MutableList<ScriptValue> = mutableListOf()) : ScriptValue()
    data class ObjectValue(val properties: MutableMap<String, ScriptValue> = mutableMapOf()) : ScriptValue() {
        fun get(key: String): ScriptValue = properties[key] ?: Undefined
        fun set(key: String, value: ScriptValue) { properties[key] = value }
    }
    data class ExpectationValue(val actual: ScriptValue, val isNegated: Boolean = false) : ScriptValue() {
        fun check(condition: Boolean, message: String) {
            val passed = if (isNegated) !condition else condition
            if (!passed) {
                val fullMsg = if (isNegated) "Expected not ($message)" else message
                throw ScriptAssertionException(fullMsg)
            }
        }
    }
    data class FunctionVal(
        val params: List<String>,
        val body: AstStatement,
        val closureScope: Scope
    ) : ScriptValue()
    data class HostFunction(val name: String, val fn: (List<ScriptValue>) -> ScriptValue) : ScriptValue()

    fun isTruthy(): Boolean = when (this) {
        is Null, is Undefined -> false
        is BooleanVal -> value
        is NumberVal -> value != 0.0 && !value.isNaN()
        is StringVal -> value.isNotEmpty()
        is ArrayVal -> true
        is ObjectValue -> true
        is ExpectationValue -> true
        is FunctionVal, is HostFunction -> true
    }

    fun toNumber(): Double = when (this) {
        is NumberVal -> value
        is BooleanVal -> if (value) 1.0 else 0.0
        is StringVal -> value.trim().toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    fun toDisplayString(depth: Int = 0): String = when (this) {
        is Null -> "null"
        is Undefined -> "undefined"
        is BooleanVal -> value.toString()
        is NumberVal -> if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        is StringVal -> value
        is ArrayVal -> if (depth > 5) "[...]" else "[${elements.joinToString(", ") { it.toDisplayString(depth + 1) }}]"
        is ObjectValue -> if (depth > 5) "{...}" else "{${properties.entries.joinToString(", ") { "${it.key}: ${it.value.toDisplayString(depth + 1)}" }}}"
        is ExpectationValue -> "[Expectation]"
        is FunctionVal -> "[Function]"
        is HostFunction -> "[Function: $name]"
    }

    fun isEquivalentTo(other: ScriptValue): Boolean {
        if (this === other) return true
        if (this is NumberVal && other is NumberVal) return this.value == other.value
        if (this is StringVal && other is StringVal) return this.value == other.value
        if (this is BooleanVal && other is BooleanVal) return this.value == other.value
        if (this is Null && other is Null) return true
        if (this is Undefined && other is Undefined) return true
        // numeric string to number
        if (this is NumberVal && other is StringVal) return this.value == other.value.toDoubleOrNull()
        if (this is StringVal && other is NumberVal) return this.value.toDoubleOrNull() == other.value
        return false
    }

    fun toJsonElement(): JsonElement = when (this) {
        is Null, is Undefined, is ExpectationValue -> JsonNull
        is BooleanVal -> JsonPrimitive(value)
        is NumberVal -> if (value % 1.0 == 0.0) JsonPrimitive(value.toLong()) else JsonPrimitive(value)
        is StringVal -> JsonPrimitive(value)
        is ArrayVal -> JsonArray(elements.map { it.toJsonElement() })
        is ObjectValue -> JsonObject(properties.mapValues { it.value.toJsonElement() })
        is FunctionVal, is HostFunction -> JsonNull
    }
}

fun JsonElement.toScriptValue(): ScriptValue = when (this) {
    is JsonNull -> ScriptValue.Null
    is JsonPrimitive -> {
        if (isString) {
            ScriptValue.StringVal(content)
        } else {
            booleanOrNull?.let { ScriptValue.BooleanVal(it) }
                ?: doubleOrNull?.let { ScriptValue.NumberVal(it) }
                ?: ScriptValue.StringVal(content)
        }
    }
    is JsonArray -> ScriptValue.ArrayVal(this.map { it.toScriptValue() }.toMutableList())
    is JsonObject -> {
        val obj = ScriptValue.ObjectValue()
        this.forEach { (k, v) -> obj.set(k, v.toScriptValue()) }
        obj
    }
}

class Scope(val parent: Scope? = null) {
    private val variables = mutableMapOf<String, ScriptValue>()

    fun define(name: String, value: ScriptValue) {
        variables[name] = value
    }

    fun assign(name: String, value: ScriptValue): Boolean {
        if (variables.containsKey(name)) {
            variables[name] = value
            return true
        }
        if (parent?.assign(name, value) == true) {
            return true
        }
        variables[name] = value
        return true
    }

    fun resolve(name: String): ScriptValue? {
        return variables[name] ?: parent?.resolve(name)
    }
}

// =============================================================================
// AST Nodes
// =============================================================================

sealed interface AstNode

sealed interface AstStatement : AstNode {
    data class Block(val statements: List<AstStatement>) : AstStatement
    data class VarDecl(val name: String, val init: AstExpr?) : AstStatement
    data class ExprStmt(val expr: AstExpr) : AstStatement
    data class IfStmt(val condition: AstExpr, val thenBranch: AstStatement, val elseBranch: AstStatement?) : AstStatement
    data class ReturnStmt(val expr: AstExpr?) : AstStatement
}

sealed interface AstExpr : AstNode {
    data class Literal(val value: ScriptValue) : AstExpr
    data class Identifier(val name: String) : AstExpr
    data class ArrayLiteral(val elements: List<AstExpr>) : AstExpr
    data class ObjectLiteral(val entries: List<Pair<String, AstExpr>>) : AstExpr
    data class FunctionExpr(val params: List<String>, val body: AstStatement) : AstExpr
    data class MemberAccess(val target: AstExpr, val property: String) : AstExpr
    data class IndexAccess(val target: AstExpr, val index: AstExpr) : AstExpr
    data class Call(val target: AstExpr, val arguments: List<AstExpr>) : AstExpr
    data class Assignment(val target: AstExpr, val value: AstExpr) : AstExpr
    data class Binary(val left: AstExpr, val operator: String, val right: AstExpr) : AstExpr
    data class Unary(val operator: String, val expr: AstExpr) : AstExpr
}

// =============================================================================
// Tokenizer
// =============================================================================

enum class TokenType {
    IDENTIFIER, NUMBER, STRING,
    VAR, LET, CONST, FUNCTION, RETURN, IF, ELSE, TRUE, FALSE, NULL, UNDEFINED,
    PLUS, MINUS, STAR, SLASH, PERCENT,
    BANG, ASSIGN, EQUAL, NOT_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL,
    LOGICAL_AND, LOGICAL_OR, ARROW,
    LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET,
    DOT, COMMA, SEMICOLON, COLON,
    EOF
}

data class Token(val type: TokenType, val text: String)

class Tokenizer(private val input: String) {
    private var pos = 0

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (pos < input.length) {
            val c = input[pos]

            if (c.isWhitespace()) {
                pos++
                continue
            }

            // Comments
            if (c == '/' && peek() == '/') {
                pos += 2
                while (pos < input.length && input[pos] != '\n') pos++
                continue
            }
            if (c == '/' && peek() == '*') {
                pos += 2
                while (pos < input.length - 1 && !(input[pos] == '*' && input[pos + 1] == '/')) pos++
                pos += 2
                continue
            }

            // String literals: "...", '...', `...`
            if (c == '"' || c == '\'' || c == '`') {
                tokens.add(readString(c))
                continue
            }

            // Numbers
            if (c.isDigit()) {
                tokens.add(readNumber())
                continue
            }

            // Identifiers / Keywords
            if (c.isLetter() || c == '_' || c == '$') {
                tokens.add(readIdentifier())
                continue
            }

            // Two/Three-character punctuation / operators
            val threeChar = if (pos + 2 < input.length) input.substring(pos, pos + 3) else ""
            if (threeChar == "===") { tokens.add(Token(TokenType.EQUAL, "===")); pos += 3; continue }
            if (threeChar == "!==") { tokens.add(Token(TokenType.NOT_EQUAL, "!==")); pos += 3; continue }

            val twoChar = if (pos + 1 < input.length) input.substring(pos, pos + 2) else ""
            when (twoChar) {
                "==" -> { tokens.add(Token(TokenType.EQUAL, "==")); pos += 2; continue }
                "!=" -> { tokens.add(Token(TokenType.NOT_EQUAL, "!=")); pos += 2; continue }
                "<=" -> { tokens.add(Token(TokenType.LESS_EQUAL, "<=")); pos += 2; continue }
                ">=" -> { tokens.add(Token(TokenType.GREATER_EQUAL, ">=")); pos += 2; continue }
                "&&" -> { tokens.add(Token(TokenType.LOGICAL_AND, "&&")); pos += 2; continue }
                "||" -> { tokens.add(Token(TokenType.LOGICAL_OR, "||")); pos += 2; continue }
                "=>" -> { tokens.add(Token(TokenType.ARROW, "=>")); pos += 2; continue }
            }

            // Single-character punctuation
            val single = when (c) {
                '+' -> Token(TokenType.PLUS, "+")
                '-' -> Token(TokenType.MINUS, "-")
                '*' -> Token(TokenType.STAR, "*")
                '/' -> Token(TokenType.SLASH, "/")
                '%' -> Token(TokenType.PERCENT, "%")
                '!' -> Token(TokenType.BANG, "!")
                '=' -> Token(TokenType.ASSIGN, "=")
                '<' -> Token(TokenType.LESS, "<")
                '>' -> Token(TokenType.GREATER, ">")
                '(' -> Token(TokenType.LPAREN, "(")
                ')' -> Token(TokenType.RPAREN, ")")
                '{' -> Token(TokenType.LBRACE, "{")
                '}' -> Token(TokenType.RBRACE, "}")
                '[' -> Token(TokenType.LBRACKET, "[")
                ']' -> Token(TokenType.RBRACKET, "]")
                '.' -> Token(TokenType.DOT, ".")
                ',' -> Token(TokenType.COMMA, ",")
                ';' -> Token(TokenType.SEMICOLON, ";")
                ':' -> Token(TokenType.COLON, ":")
                else -> null
            }

            if (single != null) {
                tokens.add(single)
                pos++
            } else {
                pos++ // Skip unknown characters gracefully
            }
        }
        tokens.add(Token(TokenType.EOF, ""))
        return tokens
    }

    private fun peek(): Char = if (pos + 1 < input.length) input[pos + 1] else '\u0000'

    private fun readString(quote: Char): Token {
        pos++ // skip opening quote
        val sb = StringBuilder()
        while (pos < input.length && input[pos] != quote) {
            val c = input[pos]
            if (c == '\\' && pos + 1 < input.length) {
                pos++
                val next = input[pos]
                when (next) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    '\\', '"', '\'', '`' -> sb.append(next)
                    else -> sb.append(next)
                }
            } else {
                sb.append(c)
            }
            pos++
        }
        if (pos < input.length) pos++ // skip closing quote
        return Token(TokenType.STRING, sb.toString())
    }

    private fun readNumber(): Token {
        val start = pos
        while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
            pos++
        }
        return Token(TokenType.NUMBER, input.substring(start, pos))
    }

    private fun readIdentifier(): Token {
        val start = pos
        while (pos < input.length && (input[pos].isLetterOrDigit() || input[pos] == '_' || input[pos] == '$')) {
            pos++
        }
        val text = input.substring(start, pos)
        val type = when (text) {
            "var" -> TokenType.VAR
            "let" -> TokenType.LET
            "const" -> TokenType.CONST
            "function" -> TokenType.FUNCTION
            "return" -> TokenType.RETURN
            "if" -> TokenType.IF
            "else" -> TokenType.ELSE
            "true" -> TokenType.TRUE
            "false" -> TokenType.FALSE
            "null" -> TokenType.NULL
            "undefined" -> TokenType.UNDEFINED
            else -> TokenType.IDENTIFIER
        }
        return Token(type, text)
    }
}

// =============================================================================
// Parser
// =============================================================================

class Parser(private val tokens: List<Token>) {
    private var cursor = 0

    fun parseProgram(): List<AstStatement> {
        val stmts = mutableListOf<AstStatement>()
        while (!isAtEnd()) {
            if (match(TokenType.SEMICOLON)) continue
            stmts.add(parseStatement())
        }
        return stmts
    }

    private fun parseStatement(): AstStatement {
        if (match(TokenType.VAR, TokenType.LET, TokenType.CONST)) {
            val name = consume(TokenType.IDENTIFIER, "Expected variable name").text
            val init = if (match(TokenType.ASSIGN)) parseExpression() else null
            match(TokenType.SEMICOLON)
            return AstStatement.VarDecl(name, init)
        }

        if (match(TokenType.IF)) {
            consume(TokenType.LPAREN, "Expected '(' after if")
            val cond = parseExpression()
            consume(TokenType.RPAREN, "Expected ')' after condition")
            val thenStmt = parseStatement()
            val elseStmt = if (match(TokenType.ELSE)) parseStatement() else null
            return AstStatement.IfStmt(cond, thenStmt, elseStmt)
        }

        if (match(TokenType.RETURN)) {
            val expr = if (!check(TokenType.SEMICOLON) && !check(TokenType.RBRACE) && !isAtEnd()) parseExpression() else null
            match(TokenType.SEMICOLON)
            return AstStatement.ReturnStmt(expr)
        }

        if (match(TokenType.LBRACE)) {
            return parseBlock()
        }

        val expr = parseExpression()
        match(TokenType.SEMICOLON)
        return AstStatement.ExprStmt(expr)
    }

    private fun parseBlock(): AstStatement.Block {
        val list = mutableListOf<AstStatement>()
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            if (match(TokenType.SEMICOLON)) continue
            list.add(parseStatement())
        }
        consume(TokenType.RBRACE, "Expected '}'")
        return AstStatement.Block(list)
    }

    private fun parseExpression(): AstExpr {
        return parseAssignment()
    }

    private fun parseAssignment(): AstExpr {
        val expr = parseLogicalOr()
        if (match(TokenType.ASSIGN)) {
            val value = parseAssignment()
            return AstExpr.Assignment(expr, value)
        }
        return expr
    }

    private fun parseLogicalOr(): AstExpr {
        var expr = parseLogicalAnd()
        while (match(TokenType.LOGICAL_OR)) {
            val right = parseLogicalAnd()
            expr = AstExpr.Binary(expr, "||", right)
        }
        return expr
    }

    private fun parseLogicalAnd(): AstExpr {
        var expr = parseEquality()
        while (match(TokenType.LOGICAL_AND)) {
            val right = parseEquality()
            expr = AstExpr.Binary(expr, "&&", right)
        }
        return expr
    }

    private fun parseEquality(): AstExpr {
        var expr = parseRelational()
        while (match(TokenType.EQUAL, TokenType.NOT_EQUAL)) {
            val op = previous().text
            val right = parseRelational()
            expr = AstExpr.Binary(expr, op, right)
        }
        return expr
    }

    private fun parseRelational(): AstExpr {
        var expr = parseAdditive()
        while (match(TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            val op = previous().text
            val right = parseAdditive()
            expr = AstExpr.Binary(expr, op, right)
        }
        return expr
    }

    private fun parseAdditive(): AstExpr {
        var expr = parseMultiplicative()
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val op = previous().text
            val right = parseMultiplicative()
            expr = AstExpr.Binary(expr, op, right)
        }
        return expr
    }

    private fun parseMultiplicative(): AstExpr {
        var expr = parseUnary()
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            val op = previous().text
            val right = parseUnary()
            expr = AstExpr.Binary(expr, op, right)
        }
        return expr
    }

    private fun parseUnary(): AstExpr {
        if (match(TokenType.BANG, TokenType.MINUS, TokenType.PLUS)) {
            val op = previous().text
            val right = parseUnary()
            return AstExpr.Unary(op, right)
        }
        return parseCallAndAccess()
    }

    private fun parseCallAndAccess(): AstExpr {
        var expr = parsePrimary()
        while (true) {
            if (match(TokenType.DOT)) {
                val prop = if (check(TokenType.IDENTIFIER) || check(TokenType.NULL) || check(TokenType.TRUE) || check(TokenType.FALSE) || check(TokenType.UNDEFINED) || check(TokenType.FUNCTION)) {
                    advance().text
                } else {
                    consume(TokenType.IDENTIFIER, "Expected property name after '.'").text
                }
                expr = AstExpr.MemberAccess(expr, prop)
            } else if (match(TokenType.LBRACKET)) {
                val index = parseExpression()
                consume(TokenType.RBRACKET, "Expected ']'")
                expr = AstExpr.IndexAccess(expr, index)
            } else if (match(TokenType.LPAREN)) {
                val args = mutableListOf<AstExpr>()
                if (!check(TokenType.RPAREN)) {
                    do {
                        args.add(parseExpression())
                    } while (match(TokenType.COMMA))
                }
                consume(TokenType.RPAREN, "Expected ')' after arguments")
                expr = AstExpr.Call(expr, args)
            } else {
                break
            }
        }
        return expr
    }

    private fun parsePrimary(): AstExpr {
        if (match(TokenType.TRUE)) return AstExpr.Literal(ScriptValue.BooleanVal(true))
        if (match(TokenType.FALSE)) return AstExpr.Literal(ScriptValue.BooleanVal(false))
        if (match(TokenType.NULL)) return AstExpr.Literal(ScriptValue.Null)
        if (match(TokenType.UNDEFINED)) return AstExpr.Literal(ScriptValue.Undefined)

        if (match(TokenType.NUMBER)) {
            val num = previous().text.toDoubleOrNull() ?: 0.0
            return AstExpr.Literal(ScriptValue.NumberVal(num))
        }

        if (match(TokenType.STRING)) {
            return AstExpr.Literal(ScriptValue.StringVal(previous().text))
        }

        if (match(TokenType.FUNCTION)) {
            // function name?(param, ...) { ... }
            if (check(TokenType.IDENTIFIER)) advance()
            consume(TokenType.LPAREN, "Expected '(' after function")
            val params = mutableListOf<String>()
            if (!check(TokenType.RPAREN)) {
                do {
                    params.add(consume(TokenType.IDENTIFIER, "Expected parameter name").text)
                } while (match(TokenType.COMMA))
            }
            consume(TokenType.RPAREN, "Expected ')'")
            consume(TokenType.LBRACE, "Expected '{'")
            val body = parseBlock()
            return AstExpr.FunctionExpr(params, body)
        }

        // Array literal: [e1, e2]
        if (match(TokenType.LBRACKET)) {
            val elements = mutableListOf<AstExpr>()
            if (!check(TokenType.RBRACKET)) {
                do {
                    elements.add(parseExpression())
                } while (match(TokenType.COMMA))
            }
            consume(TokenType.RBRACKET, "Expected ']'")
            return AstExpr.ArrayLiteral(elements)
        }

        // Object literal: { key: val, "key": val }
        if (match(TokenType.LBRACE)) {
            val entries = mutableListOf<Pair<String, AstExpr>>()
            if (!check(TokenType.RBRACE)) {
                do {
                    val key = when {
                        match(TokenType.IDENTIFIER) -> previous().text
                        match(TokenType.STRING) -> previous().text
                        else -> throw ScriptAssertionException("Expected key in object literal")
                    }
                    consume(TokenType.COLON, "Expected ':' after key")
                    val value = parseExpression()
                    entries.add(key to value)
                } while (match(TokenType.COMMA))
            }
            consume(TokenType.RBRACE, "Expected '}'")
            return AstExpr.ObjectLiteral(entries)
        }

        // Grouping or Arrow function
        if (match(TokenType.LPAREN)) {
            val params = mutableListOf<String>()
            var isArrowCandidate = true

            if (check(TokenType.RPAREN)) {
                advance()
                if (match(TokenType.ARROW)) {
                    val body = if (match(TokenType.LBRACE)) parseBlock() else AstStatement.ReturnStmt(parseExpression())
                    return AstExpr.FunctionExpr(emptyList(), body)
                }
                return AstExpr.Literal(ScriptValue.Undefined)
            }

            val firstExpr = parseExpression()
            if (firstExpr is AstExpr.Identifier) params.add(firstExpr.name) else isArrowCandidate = false

            while (match(TokenType.COMMA)) {
                val next = parseExpression()
                if (next is AstExpr.Identifier) params.add(next.name) else isArrowCandidate = false
            }
            consume(TokenType.RPAREN, "Expected ')'")

            if (isArrowCandidate && match(TokenType.ARROW)) {
                val body = if (match(TokenType.LBRACE)) parseBlock() else AstStatement.ReturnStmt(parseExpression())
                return AstExpr.FunctionExpr(params, body)
            }
            return firstExpr
        }

        if (match(TokenType.IDENTIFIER)) {
            val name = previous().text
            if (match(TokenType.ARROW)) {
                val body = if (match(TokenType.LBRACE)) parseBlock() else AstStatement.ReturnStmt(parseExpression())
                return AstExpr.FunctionExpr(listOf(name), body)
            }
            return AstExpr.Identifier(name)
        }

        throw ScriptAssertionException("Unexpected token '${peek().text}'")
    }

    private fun match(vararg types: TokenType): Boolean {
        for (t in types) {
            if (check(t)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: TokenType): Boolean = if (isAtEnd()) type == TokenType.EOF else peek().type == type

    private fun advance(): Token {
        if (!isAtEnd()) cursor++
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF

    private fun peek(): Token = tokens[cursor]

    private fun previous(): Token = tokens[cursor - 1]

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw ScriptAssertionException(message)
    }
}

// =============================================================================
// Evaluator
// =============================================================================

class Evaluator {
    class ReturnControl(val value: ScriptValue) : Exception()

    fun execute(stmt: AstStatement, scope: Scope) {
        when (stmt) {
            is AstStatement.Block -> {
                val blockScope = Scope(scope)
                for (s in stmt.statements) {
                    execute(s, blockScope)
                }
            }
            is AstStatement.VarDecl -> {
                val value = stmt.init?.let { evaluate(it, scope) } ?: ScriptValue.Undefined
                scope.define(stmt.name, value)
            }
            is AstStatement.ExprStmt -> {
                evaluate(stmt.expr, scope)
            }
            is AstStatement.IfStmt -> {
                val cond = evaluate(stmt.condition, scope)
                if (cond.isTruthy()) {
                    execute(stmt.thenBranch, scope)
                } else if (stmt.elseBranch != null) {
                    execute(stmt.elseBranch, scope)
                }
            }
            is AstStatement.ReturnStmt -> {
                val v = stmt.expr?.let { evaluate(it, scope) } ?: ScriptValue.Undefined
                throw ReturnControl(v)
            }
        }
    }

    fun evaluate(expr: AstExpr, scope: Scope): ScriptValue {
        return when (expr) {
            is AstExpr.Literal -> expr.value
            is AstExpr.Identifier -> scope.resolve(expr.name) ?: ScriptValue.Undefined
            is AstExpr.ArrayLiteral -> {
                val list = expr.elements.map { evaluate(it, scope) }.toMutableList()
                ScriptValue.ArrayVal(list)
            }
            is AstExpr.ObjectLiteral -> {
                val obj = ScriptValue.ObjectValue()
                for ((k, v) in expr.entries) {
                    obj.set(k, evaluate(v, scope))
                }
                obj
            }
            is AstExpr.FunctionExpr -> {
                ScriptValue.FunctionVal(expr.params, expr.body, scope)
            }
            is AstExpr.MemberAccess -> {
                val target = evaluate(expr.target, scope)
                when (target) {
                    is ScriptValue.ExpectationValue -> {
                        when (expr.property) {
                            "to", "be", "have", "and" -> target
                            "not" -> target.copy(isNegated = !target.isNegated)
                            "equal", "eql", "toBe", "toEqual" -> ScriptValue.HostFunction(expr.property) { args ->
                                val expected = args.firstOrNull() ?: ScriptValue.Undefined
                                target.check(target.actual.isEquivalentTo(expected), "Expected ${target.actual.toDisplayString()} to equal ${expected.toDisplayString()}")
                                target
                            }
                            "below", "toBeLessThan" -> ScriptValue.HostFunction(expr.property) { args ->
                                val expected = args.firstOrNull()?.toNumber() ?: 0.0
                                val act = target.actual.toNumber()
                                target.check(act < expected, "Expected $act to be below $expected")
                                target
                            }
                            "above", "toBeGreaterThan" -> ScriptValue.HostFunction(expr.property) { args ->
                                val expected = args.firstOrNull()?.toNumber() ?: 0.0
                                val act = target.actual.toNumber()
                                target.check(act > expected, "Expected $act to be above $expected")
                                target
                            }
                            "least", "toBeGreaterThanOrEqual" -> ScriptValue.HostFunction(expr.property) { args ->
                                val expected = args.firstOrNull()?.toNumber() ?: 0.0
                                val act = target.actual.toNumber()
                                target.check(act >= expected, "Expected $act to be at least $expected")
                                target
                            }
                            "most", "toBeLessThanOrEqual" -> ScriptValue.HostFunction(expr.property) { args ->
                                val expected = args.firstOrNull()?.toNumber() ?: 0.0
                                val act = target.actual.toNumber()
                                target.check(act <= expected, "Expected $act to be at most $expected")
                                target
                            }
                            "include", "toContain" -> ScriptValue.HostFunction(expr.property) { args ->
                                val targetStr = args.firstOrNull()?.toDisplayString() ?: ""
                                val contains = when (val a = target.actual) {
                                    is ScriptValue.StringVal -> a.value.contains(targetStr)
                                    is ScriptValue.ArrayVal -> a.elements.any { it.toDisplayString() == targetStr }
                                    is ScriptValue.ObjectValue -> a.properties.containsKey(targetStr)
                                    else -> false
                                }
                                target.check(contains, "Expected ${target.actual.toDisplayString()} to include '$targetStr'")
                                target
                            }
                            "null" -> {
                                target.check(target.actual is ScriptValue.Null, "Expected ${target.actual.toDisplayString()} to be null")
                                target
                            }
                            "toBeNull" -> ScriptValue.HostFunction("toBeNull") {
                                target.check(target.actual is ScriptValue.Null, "Expected ${target.actual.toDisplayString()} to be null")
                                target
                            }
                            "undefined" -> {
                                target.check(target.actual is ScriptValue.Undefined, "Expected ${target.actual.toDisplayString()} to be undefined")
                                target
                            }
                            "toBeUndefined" -> ScriptValue.HostFunction("toBeUndefined") {
                                target.check(target.actual is ScriptValue.Undefined, "Expected ${target.actual.toDisplayString()} to be undefined")
                                target
                            }
                            "defined" -> {
                                target.check(target.actual !is ScriptValue.Undefined && target.actual !is ScriptValue.Null, "Expected value to be defined")
                                target
                            }
                            "toBeDefined" -> ScriptValue.HostFunction("toBeDefined") {
                                target.check(target.actual !is ScriptValue.Undefined && target.actual !is ScriptValue.Null, "Expected value to be defined")
                                target
                            }
                            "true" -> {
                                target.check(target.actual.isTruthy(), "Expected ${target.actual.toDisplayString()} to be true")
                                target
                            }
                            "toBeTruthy" -> ScriptValue.HostFunction("toBeTruthy") {
                                target.check(target.actual.isTruthy(), "Expected ${target.actual.toDisplayString()} to be truthy")
                                target
                            }
                            "false" -> {
                                target.check(!target.actual.isTruthy(), "Expected ${target.actual.toDisplayString()} to be false")
                                target
                            }
                            "toBeFalsy" -> ScriptValue.HostFunction("toBeFalsy") {
                                target.check(!target.actual.isTruthy(), "Expected ${target.actual.toDisplayString()} to be false")
                                target
                            }
                            "lengthOf", "toHaveLength" -> ScriptValue.HostFunction(expr.property) { args ->
                                val expected = args.firstOrNull()?.toNumber()?.toInt() ?: 0
                                val len = when (val a = target.actual) {
                                    is ScriptValue.StringVal -> a.value.length
                                    is ScriptValue.ArrayVal -> a.elements.size
                                    is ScriptValue.ObjectValue -> a.properties.size
                                    else -> -1
                                }
                                target.check(len == expected, "Expected length to be $expected, but got $len")
                                target
                            }
                            "oneOf", "toBeOneOf" -> ScriptValue.HostFunction(expr.property) { args ->
                                val list = (args.firstOrNull() as? ScriptValue.ArrayVal)?.elements ?: emptyList()
                                val match = list.any { it.isEquivalentTo(target.actual) }
                                target.check(match, "Expected ${target.actual.toDisplayString()} to be one of ${list.map { it.toDisplayString() }}")
                                target
                            }
                            "a", "an", "toBeA" -> ScriptValue.HostFunction(expr.property) { args ->
                                val typeStr = args.firstOrNull()?.toDisplayString()?.lowercase() ?: ""
                                val matches = when (typeStr) {
                                    "string" -> target.actual is ScriptValue.StringVal
                                    "number" -> target.actual is ScriptValue.NumberVal
                                    "boolean" -> target.actual is ScriptValue.BooleanVal
                                    "object" -> target.actual is ScriptValue.ObjectValue
                                    "array" -> target.actual is ScriptValue.ArrayVal
                                    else -> false
                                }
                                target.check(matches, "Expected ${target.actual.toDisplayString()} to be a $typeStr")
                                target
                            }
                            else -> ScriptValue.Undefined
                        }
                    }
                    is ScriptValue.ObjectValue -> target.get(expr.property)
                    is ScriptValue.ArrayVal -> when (expr.property) {
                        "length" -> ScriptValue.NumberVal(target.elements.size.toDouble())
                        else -> ScriptValue.Undefined
                    }
                    is ScriptValue.StringVal -> when (expr.property) {
                        "length" -> ScriptValue.NumberVal(target.value.length.toDouble())
                        else -> ScriptValue.Undefined
                    }
                    else -> ScriptValue.Undefined
                }
            }
            is AstExpr.IndexAccess -> {
                val target = evaluate(expr.target, scope)
                val indexVal = evaluate(expr.index, scope)
                when (target) {
                    is ScriptValue.ArrayVal -> {
                        val idx = indexVal.toNumber().toInt()
                        target.elements.getOrNull(idx) ?: ScriptValue.Undefined
                    }
                    is ScriptValue.ObjectValue -> {
                        val key = indexVal.toDisplayString()
                        target.get(key)
                    }
                    is ScriptValue.StringVal -> {
                        val idx = indexVal.toNumber().toInt()
                        if (idx in 0 until target.value.length) ScriptValue.StringVal(target.value[idx].toString()) else ScriptValue.Undefined
                    }
                    else -> ScriptValue.Undefined
                }
            }
            is AstExpr.Assignment -> {
                val right = evaluate(expr.value, scope)
                when (val left = expr.target) {
                    is AstExpr.Identifier -> {
                        scope.assign(left.name, right)
                    }
                    is AstExpr.MemberAccess -> {
                        val target = evaluate(left.target, scope)
                        if (target is ScriptValue.ObjectValue) {
                            target.set(left.property, right)
                        }
                    }
                    is AstExpr.IndexAccess -> {
                        val target = evaluate(left.target, scope)
                        val idx = evaluate(left.index, scope)
                        if (target is ScriptValue.ObjectValue) {
                            target.set(idx.toDisplayString(), right)
                        } else if (target is ScriptValue.ArrayVal) {
                            val i = idx.toNumber().toInt()
                            while (target.elements.size <= i) target.elements.add(ScriptValue.Undefined)
                            target.elements[i] = right
                        }
                    }
                    else -> throw ScriptAssertionException("Invalid assignment target")
                }
                right
            }
            is AstExpr.Call -> {
                val callee = evaluate(expr.target, scope)
                val evaluatedArgs = expr.arguments.map { evaluate(it, scope) }
                when (callee) {
                    is ScriptValue.HostFunction -> callee.fn(evaluatedArgs)
                    is ScriptValue.FunctionVal -> callFunction(callee, evaluatedArgs)
                    else -> throw ScriptAssertionException("Cannot call non-function ${callee.toDisplayString()}")
                }
            }
            is AstExpr.Unary -> {
                val right = evaluate(expr.expr, scope)
                when (expr.operator) {
                    "!" -> ScriptValue.BooleanVal(!right.isTruthy())
                    "-" -> ScriptValue.NumberVal(-right.toNumber())
                    "+" -> ScriptValue.NumberVal(right.toNumber())
                    else -> ScriptValue.Undefined
                }
            }
            is AstExpr.Binary -> {
                val left = evaluate(expr.left, scope)
                // Short-circuit logical operators
                if (expr.operator == "&&") {
                    return if (!left.isTruthy()) left else evaluate(expr.right, scope)
                }
                if (expr.operator == "||") {
                    return if (left.isTruthy()) left else evaluate(expr.right, scope)
                }

                val right = evaluate(expr.right, scope)
                when (expr.operator) {
                    "+" -> {
                        if (left is ScriptValue.StringVal || right is ScriptValue.StringVal) {
                            ScriptValue.StringVal(left.toDisplayString() + right.toDisplayString())
                        } else {
                            ScriptValue.NumberVal(left.toNumber() + right.toNumber())
                        }
                    }
                    "-" -> ScriptValue.NumberVal(left.toNumber() - right.toNumber())
                    "*" -> ScriptValue.NumberVal(left.toNumber() * right.toNumber())
                    "/" -> ScriptValue.NumberVal(left.toNumber() / right.toNumber())
                    "%" -> ScriptValue.NumberVal(left.toNumber() % right.toNumber())
                    "==", "===" -> ScriptValue.BooleanVal(left.isEquivalentTo(right))
                    "!=", "!==" -> ScriptValue.BooleanVal(!left.isEquivalentTo(right))
                    "<" -> ScriptValue.BooleanVal(left.toNumber() < right.toNumber())
                    "<=" -> ScriptValue.BooleanVal(left.toNumber() <= right.toNumber())
                    ">" -> ScriptValue.BooleanVal(left.toNumber() > right.toNumber())
                    ">=" -> ScriptValue.BooleanVal(left.toNumber() >= right.toNumber())
                    else -> ScriptValue.Undefined
                }
            }
        }
    }

    fun callFunction(fn: ScriptValue.FunctionVal, args: List<ScriptValue>): ScriptValue {
        val fnScope = Scope(fn.closureScope)
        for (i in fn.params.indices) {
            val paramName = fn.params[i]
            val argVal = args.getOrNull(i) ?: ScriptValue.Undefined
            fnScope.define(paramName, argVal)
        }
        return try {
            execute(fn.body, fnScope)
            ScriptValue.Undefined
        } catch (r: ReturnControl) {
            r.value
        }
    }
}
