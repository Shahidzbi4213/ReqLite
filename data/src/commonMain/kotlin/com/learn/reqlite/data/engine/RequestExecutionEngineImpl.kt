package com.learn.reqlite.data.engine

import com.learn.reqlite.domain.engine.RequestExecutionEngine
import com.learn.reqlite.domain.engine.RequestExecutionResult
import com.learn.reqlite.domain.model.Environment
import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.model.ResponseArtifact
import com.learn.reqlite.domain.model.Variable
import com.learn.reqlite.domain.repository.EnvironmentRepository
import com.learn.reqlite.domain.repository.HistoryRepository
import com.learn.reqlite.domain.repository.RequestRepository
import com.learn.reqlite.domain.repository.SecureStorage
import com.learn.reqlite.domain.script.PreRequestContext
import com.learn.reqlite.domain.script.PostRequestContext
import com.learn.reqlite.domain.script.ScriptEngine
import com.learn.reqlite.domain.script.ScriptInterpreter
import com.learn.reqlite.domain.script.model.TestAssertionResult
import com.learn.reqlite.domain.usecase.RequestValidator
import com.learn.reqlite.domain.usecase.ValidationIssue
import com.learn.reqlite.domain.usecase.VariableResolver
import com.learn.reqlite.utils.common.nowMs
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.*

class RequestExecutionEngineImpl(
    private val httpClient: HttpClient,
    private val requestRepository: RequestRepository,
    private val environmentRepository: EnvironmentRepository,
    private val secureStorage: SecureStorage,
    private val variableResolver: VariableResolver,
    private val requestValidator: RequestValidator,
    private val historyRepository: HistoryRepository,
    private val scriptEngine: ScriptEngine = ScriptInterpreter()
) : RequestExecutionEngine {
    
    override suspend fun execute(draftId: String, environmentId: String?): Pair<HistoryEntry, String> {
        val result = executeWithHeaders(draftId, environmentId)
        return Pair(result.historyEntry, result.responseBody)
    }

    override suspend fun executeWithHeaders(draftId: String, environmentId: String?): RequestExecutionResult {
        val draft = requestRepository.getDraftById(draftId) ?: throw IllegalArgumentException("Draft not found")
        
        val env = environmentId?.let { environmentRepository.getEnvironmentById(it) }
        val variables = env?.variables ?: emptyList()
        
        val issues = requestValidator.validate(draft, variables)
        if (issues.any { it is ValidationIssue.Error }) {
            throw IllegalArgumentException("Validation failed")
        }
        
        val resolvedVariables = variables.map { variable ->
            if (variable.isSecret) {
                val secretValue = secureStorage.getSecret(variable.value)
                variable.copy(value = secretValue ?: "")
            } else {
                variable
            }
        }

        val mutableVariablesMap = resolvedVariables.associate { it.key to it.value }.toMutableMap()
        var effectiveUrl = draft.url
        val dynamicHeaders = mutableMapOf<String, String>()
        val scriptLogs = mutableListOf<String>()

        // 1. Pre-request Script Execution
        if (!draft.preRequestScript.isNullOrBlank()) {
            val preContext = PreRequestContext(
                url = draft.url,
                method = draft.method.name,
                headers = draft.headers.filter { it.isEnabled }.associate { it.key to it.value },
                queryParams = draft.queryParams.filter { it.isEnabled }.associate { it.key to it.value },
                bodyContent = when (val b = draft.body) {
                    is com.learn.reqlite.domain.model.RequestBody.TextBody -> b.content
                    else -> null
                },
                environmentVariables = mutableVariablesMap
            )
            val preResult = scriptEngine.executePreRequest(draft.preRequestScript, preContext)
            scriptLogs.addAll(preResult.logs)
            if (preResult.mutatedVariables.isNotEmpty()) {
                mutableVariablesMap.putAll(preResult.mutatedVariables)
                if (env != null) {
                    persistMutatedVariables(env, preResult.mutatedVariables)
                }
            }
            if (preResult.mutatedHeaders.isNotEmpty()) {
                dynamicHeaders.putAll(preResult.mutatedHeaders)
            }
            if (!preResult.mutatedUrl.isNullOrBlank()) {
                effectiveUrl = preResult.mutatedUrl!!
            }
        }

        // Re-construct active variables with mutations for VariableResolver
        val effectiveResolvedVariables = resolvedVariables.map { v ->
            val updated = mutableVariablesMap[v.key] ?: v.value
            v.copy(value = updated)
        } + mutableVariablesMap.filterKeys { k -> resolvedVariables.none { it.key == k } }.map { (k, v) ->
            Variable(
                id = "var_${k}",
                key = k,
                value = v
            )
        }
        
        val urlString = variableResolver.resolve(effectiveUrl, effectiveResolvedVariables)
        
        val pendingHistoryId = "hist_${nowMs()}"
        val pendingEntry = HistoryEntry(
            id = pendingHistoryId,
            requestId = draft.requestId,
            requestMethod = draft.method,
            requestUrl = urlString,
            timestamp = nowMs()
        )
        historyRepository.insertHistoryEntry(pendingEntry)
        
        val startTime = nowMs()
        var statusCode: Int? = null
        var responseArtifactId: String? = null
        var responseBody: String = ""
        var durationMs: Long? = null
        var capturedHeaders: Map<String, String> = emptyMap()
        var statusDescription: String = "OK"
        
        try {
            val statement = httpClient.request(urlString) {
                method = HttpMethod.parse(draft.method.name)
                
                draft.headers.filter { it.isEnabled }.forEach { header ->
                    val resolvedKey = variableResolver.resolve(header.key, effectiveResolvedVariables)
                    val resolvedValue = variableResolver.resolve(header.value, effectiveResolvedVariables)
                    header(resolvedKey, resolvedValue)
                }

                // Add dynamic headers computed by pre-request script
                dynamicHeaders.forEach { (k, v) ->
                    val resolvedKey = variableResolver.resolve(k, effectiveResolvedVariables)
                    val resolvedValue = variableResolver.resolve(v, effectiveResolvedVariables)
                    header(resolvedKey, resolvedValue)
                }
                
                draft.queryParams.filter { it.isEnabled }.forEach { param ->
                    val resolvedKey = variableResolver.resolve(param.key, effectiveResolvedVariables)
                    val resolvedValue = variableResolver.resolve(param.value, effectiveResolvedVariables)
                    url { parameters.append(resolvedKey, resolvedValue) }
                }
                
                when (val body = draft.body) {
                    is com.learn.reqlite.domain.model.RequestBody.NoBody -> {}
                    is com.learn.reqlite.domain.model.RequestBody.TextBody -> {
                        setBody(variableResolver.resolve(body.content, effectiveResolvedVariables))
                    }
                    is com.learn.reqlite.domain.model.RequestBody.FormDataBody -> {}
                    is com.learn.reqlite.domain.model.RequestBody.UrlEncodedBody -> {}
                    is com.learn.reqlite.domain.model.RequestBody.GraphQLBody -> {
                        val resolvedQuery = variableResolver.resolve(body.query, effectiveResolvedVariables)
                        val resolvedVariablesJson = variableResolver.resolve(body.variables, effectiveResolvedVariables)
                        val payload = buildJsonObject {
                            put("query", resolvedQuery)
                            if (resolvedVariablesJson.isNotBlank()) {
                                val parsedVars = try {
                                    Json.parseToJsonElement(resolvedVariablesJson)
                                } catch (_: Exception) {
                                    null
                                }
                                if (parsedVars != null) {
                                    put("variables", parsedVars)
                                }
                            }
                            if (!body.operationName.isNullOrBlank()) {
                                put("operationName", body.operationName)
                            }
                        }.toString()
                        header(HttpHeaders.ContentType, "application/json")
                        setBody(payload)
                    }
                }
            }
            
            responseBody = statement.bodyAsText()
            statusCode = statement.status.value
            statusDescription = statement.status.description
            durationMs = nowMs() - startTime
            capturedHeaders = statement.headers.entries().associate { it.key to it.value.joinToString(", ") }
            
            val artifact = ResponseArtifact(
                id = "art_${nowMs()}",
                filePath = "memory",
                contentType = statement.headers[HttpHeaders.ContentType] ?: "text/plain",
                sizeBytes = responseBody.encodeToByteArray().size.toLong(),
                timestamp = nowMs()
            )
            historyRepository.insertResponseArtifact(artifact)
            responseArtifactId = artifact.id
            
        } catch (e: Exception) {
            durationMs = nowMs() - startTime
            val mapped = com.learn.reqlite.data.remote.mapThrowableToNetworkError(e)
            val failedEntry = pendingEntry.copy(
                durationMs = durationMs,
                errorCode = mapped.type.name,
                errorMessage = mapped.message
            )
            historyRepository.insertHistoryEntry(failedEntry)
            return RequestExecutionResult(
                historyEntry = failedEntry,
                responseBody = "",
                responseHeaders = emptyMap(),
                testResults = emptyList(),
                scriptLogs = scriptLogs
            )
        }
        
        val finalEntry = pendingEntry.copy(
            statusCode = statusCode,
            durationMs = durationMs,
            responseArtifactId = responseArtifactId
        )
        historyRepository.insertHistoryEntry(finalEntry)

        // 2. Post-request Test Script Execution
        var testResults = emptyList<TestAssertionResult>()
        if (!draft.testScript.isNullOrBlank()) {
            val postContext = PostRequestContext(
                statusCode = statusCode ?: 0,
                statusText = statusDescription,
                responseTimeMs = durationMs ?: 0L,
                responseHeaders = capturedHeaders,
                responseBody = responseBody,
                requestUrl = urlString,
                requestMethod = draft.method.name,
                environmentVariables = mutableVariablesMap
            )
            val testExecResult = scriptEngine.executePostRequestTests(draft.testScript, postContext)
            testResults = testExecResult.testResults
            scriptLogs.addAll(testExecResult.logs)
            if (testExecResult.mutatedVariables.isNotEmpty()) {
                mutableVariablesMap.putAll(testExecResult.mutatedVariables)
                if (env != null) {
                    persistMutatedVariables(env, testExecResult.mutatedVariables)
                }
            }
        }

        return RequestExecutionResult(
            historyEntry = finalEntry,
            responseBody = responseBody,
            responseHeaders = capturedHeaders,
            testResults = testResults,
            scriptLogs = scriptLogs
        )
    }

    private suspend fun persistMutatedVariables(env: Environment, mutated: Map<String, String>) {
        val updatedList = env.variables.toMutableList()
        mutated.forEach { (key, value) ->
            val existingIndex = updatedList.indexOfFirst { it.key == key }
            if (existingIndex >= 0) {
                updatedList[existingIndex] = updatedList[existingIndex].copy(value = value)
            } else {
                updatedList.add(
                    Variable(
                        id = "var_${nowMs()}_${(100..999).random()}",
                        key = key,
                        value = value
                    )
                )
            }
        }
        environmentRepository.updateEnvironment(env.copy(variables = updatedList, updatedAt = nowMs()))
    }
}
