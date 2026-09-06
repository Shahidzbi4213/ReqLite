package com.learn.reqlite.data.engine

import com.learn.reqlite.domain.engine.RequestExecutionEngine
import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.repository.EnvironmentRepository
import com.learn.reqlite.domain.repository.HistoryRepository
import com.learn.reqlite.domain.repository.RequestRepository
import com.learn.reqlite.domain.repository.SecureStorage
import com.learn.reqlite.domain.usecase.RequestValidator
import com.learn.reqlite.domain.usecase.VariableResolver
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpHeaders
import com.learn.reqlite.domain.model.ResponseArtifact
import com.learn.reqlite.domain.usecase.ValidationIssue
import com.learn.reqlite.utils.common.nowMs

class RequestExecutionEngineImpl(
    private val httpClient: HttpClient,
    private val requestRepository: RequestRepository,
    private val environmentRepository: EnvironmentRepository,
    private val secureStorage: SecureStorage,
    private val variableResolver: VariableResolver,
    private val requestValidator: RequestValidator,
    private val historyRepository: HistoryRepository
) : RequestExecutionEngine {
    
    override suspend fun execute(draftId: String, environmentId: String?): Pair<HistoryEntry, String> {
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
        
        val urlString = variableResolver.resolve(draft.url, resolvedVariables)
        
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
        
        try {
            val statement = httpClient.request(urlString) {
                method = HttpMethod.parse(draft.method.name)
                
                draft.headers.filter { it.isEnabled }.forEach { header ->
                    val resolvedKey = variableResolver.resolve(header.key, resolvedVariables)
                    val resolvedValue = variableResolver.resolve(header.value, resolvedVariables)
                    header(resolvedKey, resolvedValue)
                }
                
                draft.queryParams.filter { it.isEnabled }.forEach { param ->
                    val resolvedKey = variableResolver.resolve(param.key, resolvedVariables)
                    val resolvedValue = variableResolver.resolve(param.value, resolvedVariables)
                    url { parameters.append(resolvedKey, resolvedValue) }
                }
                
                when (val body = draft.body) {
                    is com.learn.reqlite.domain.model.RequestBody.NoBody -> {}
                    is com.learn.reqlite.domain.model.RequestBody.TextBody -> {
                        setBody(variableResolver.resolve(body.content, resolvedVariables))
                    }
                    is com.learn.reqlite.domain.model.RequestBody.FormDataBody -> {}
                    is com.learn.reqlite.domain.model.RequestBody.UrlEncodedBody -> {}
                }
            }
            
            responseBody = statement.bodyAsText()
            statusCode = statement.status.value
            durationMs = nowMs() - startTime
            
            val artifact = ResponseArtifact(
                id = "art_${nowMs()}",
                filePath = "memory", // TODO: proper file saving
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
            return Pair(failedEntry, "")
        }
        
        val finalEntry = pendingEntry.copy(
            statusCode = statusCode,
            durationMs = durationMs,
            responseArtifactId = responseArtifactId
        )
        historyRepository.insertHistoryEntry(finalEntry)
        return Pair(finalEntry, responseBody)
    }
}
