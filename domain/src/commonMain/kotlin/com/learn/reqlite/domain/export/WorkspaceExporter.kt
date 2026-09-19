package com.learn.reqlite.domain.export

import com.learn.reqlite.domain.model.Collection
import com.learn.reqlite.domain.model.Environment
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.Request
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.domain.model.RequestField
import com.learn.reqlite.domain.model.Variable
import com.learn.reqlite.domain.repository.CollectionRepository
import com.learn.reqlite.domain.repository.EnvironmentRepository
import com.learn.reqlite.domain.repository.RequestRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class ImportStrategy {
    CREATE_NEW,
    OVERWRITE_EXISTING,
    SKIP_EXISTING
}

sealed interface WorkspaceImportResult {
    data class Success(
        val collectionsImported: Int,
        val requestsImported: Int,
        val environmentsImported: Int,
        val warnings: List<String> = emptyList()
    ) : WorkspaceImportResult

    data class Error(val message: String) : WorkspaceImportResult
}

interface WorkspaceExporter {
    suspend fun exportWorkspace(
        collectionIds: List<String>? = null,
        environmentIds: List<String>? = null,
        includeSecrets: Boolean = false
    ): String

    suspend fun exportCollection(collectionId: String): String
    suspend fun exportEnvironment(environmentId: String, includeSecrets: Boolean = false): String
}

interface WorkspaceImporter {
    suspend fun importWorkspace(
        jsonContent: String,
        strategy: ImportStrategy = ImportStrategy.CREATE_NEW
    ): WorkspaceImportResult
}

class WorkspaceExportImportManager(
    private val collectionRepository: CollectionRepository,
    private val requestRepository: RequestRepository,
    private val environmentRepository: EnvironmentRepository,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : WorkspaceExporter, WorkspaceImporter {

    override suspend fun exportWorkspace(
        collectionIds: List<String>?,
        environmentIds: List<String>?,
        includeSecrets: Boolean
    ): String {
        val allCollections = collectionRepository.getAllCollections().first()
        val allRequests = requestRepository.getAllRequests().first()
        val allEnvironments = environmentRepository.getAllEnvironments().first()

        val filteredCollections = if (collectionIds != null) {
            allCollections.filter { it.id in collectionIds }
        } else {
            allCollections
        }

        val filteredEnvironments = if (environmentIds != null) {
            allEnvironments.filter { it.id in environmentIds }
        } else {
            allEnvironments
        }

        val exportCollections = filteredCollections.map { col ->
            val colRequests = allRequests.filter { it.collectionId == col.id }
            col.toExportDto(requests = colRequests)
        }

        val exportEnvironments = filteredEnvironments.map { env ->
            env.toExportDto(includeSecrets = includeSecrets)
        }

        val exportDto = WorkspaceExportDto(
            exportedAt = 1000L,
            collections = exportCollections,
            environments = exportEnvironments
        )

        return json.encodeToString(exportDto)
    }

    override suspend fun exportCollection(collectionId: String): String {
        return exportWorkspace(
            collectionIds = listOf(collectionId),
            environmentIds = emptyList(),
            includeSecrets = false
        )
    }

    override suspend fun exportEnvironment(environmentId: String, includeSecrets: Boolean): String {
        return exportWorkspace(
            collectionIds = emptyList(),
            environmentIds = listOf(environmentId),
            includeSecrets = includeSecrets
        )
    }

    override suspend fun importWorkspace(
        jsonContent: String,
        strategy: ImportStrategy
    ): WorkspaceImportResult {
        if (jsonContent.isBlank()) {
            return WorkspaceImportResult.Error("Import content cannot be empty")
        }

        val exportDto = try {
            json.decodeFromString<WorkspaceExportDto>(jsonContent)
        } catch (e: Exception) {
            return WorkspaceImportResult.Error("Invalid JSON structure: ${e.message}")
        }

        val warnings = mutableListOf<String>()
        if (exportDto.schemaVersion > WorkspaceExportDto.CURRENT_SCHEMA_VERSION) {
            warnings.add("Export schema version ${exportDto.schemaVersion} is newer than current ${WorkspaceExportDto.CURRENT_SCHEMA_VERSION}")
        }

        var collectionsCount = 0
        var requestsCount = 0
        var environmentsCount = 0

        val existingCollections = collectionRepository.getAllCollections().first().associateBy { it.id }
        val existingRequests = requestRepository.getAllRequests().first().associateBy { it.id }
        val existingEnvironments = environmentRepository.getAllEnvironments().first().associateBy { it.id }

        // Import Collections and their Requests
        for (colDto in exportDto.collections) {
            val targetCollectionId: String
            val shouldInsertCollection: Boolean

            when (strategy) {
                ImportStrategy.CREATE_NEW -> {
                    targetCollectionId = if (existingCollections.containsKey(colDto.id)) "col_imp_${colDto.id}_${colDto.createdAt}" else colDto.id
                    shouldInsertCollection = true
                }
                ImportStrategy.OVERWRITE_EXISTING -> {
                    targetCollectionId = colDto.id
                    shouldInsertCollection = true
                }
                ImportStrategy.SKIP_EXISTING -> {
                    if (existingCollections.containsKey(colDto.id)) {
                        targetCollectionId = colDto.id
                        shouldInsertCollection = false
                    } else {
                        targetCollectionId = colDto.id
                        shouldInsertCollection = true
                    }
                }
            }

            if (shouldInsertCollection) {
                val collection = Collection(
                    id = targetCollectionId,
                    name = colDto.name,
                    description = colDto.description,
                    createdAt = if (colDto.createdAt > 0) colDto.createdAt else 1000L,
                    updatedAt = if (colDto.updatedAt > 0) colDto.updatedAt else 1000L
                )
                if (existingCollections.containsKey(targetCollectionId)) {
                    collectionRepository.updateCollection(collection)
                } else {
                    collectionRepository.insertCollection(collection)
                }
                collectionsCount++
            }

            // Import Requests in this collection
            for (reqDto in colDto.requests) {
                val targetRequestId: String
                val shouldInsertRequest: Boolean

                when (strategy) {
                    ImportStrategy.CREATE_NEW -> {
                        targetRequestId = if (existingRequests.containsKey(reqDto.id)) "req_imp_${reqDto.id}_${reqDto.createdAt}" else reqDto.id
                        shouldInsertRequest = true
                    }
                    ImportStrategy.OVERWRITE_EXISTING -> {
                        targetRequestId = reqDto.id
                        shouldInsertRequest = true
                    }
                    ImportStrategy.SKIP_EXISTING -> {
                        if (existingRequests.containsKey(reqDto.id)) {
                            targetRequestId = reqDto.id
                            shouldInsertRequest = false
                        } else {
                            targetRequestId = reqDto.id
                            shouldInsertRequest = true
                        }
                    }
                }

                if (shouldInsertRequest) {
                    val method = try {
                        HttpMethod.valueOf(reqDto.method.uppercase())
                    } catch (e: Exception) {
                        HttpMethod.GET
                    }

                    val request = Request(
                        id = targetRequestId,
                        collectionId = targetCollectionId,
                        folderId = reqDto.folderId,
                        name = reqDto.name,
                        method = method,
                        url = reqDto.url,
                        headers = reqDto.headers.map { it.toDomain() },
                        queryParams = reqDto.queryParams.map { it.toDomain() },
                        body = reqDto.body.toDomain(),
                        createdAt = if (reqDto.createdAt > 0) reqDto.createdAt else 1000L,
                        updatedAt = if (reqDto.updatedAt > 0) reqDto.updatedAt else 1000L
                    )
                    requestRepository.insertRequest(request)
                    requestsCount++
                }
            }
        }

        // Import Environments
        for (envDto in exportDto.environments) {
            val targetEnvId: String
            val shouldInsertEnv: Boolean

            when (strategy) {
                ImportStrategy.CREATE_NEW -> {
                    targetEnvId = if (existingEnvironments.containsKey(envDto.id)) "env_imp_${envDto.id}_${envDto.createdAt}" else envDto.id
                    shouldInsertEnv = true
                }
                ImportStrategy.OVERWRITE_EXISTING -> {
                    targetEnvId = envDto.id
                    shouldInsertEnv = true
                }
                ImportStrategy.SKIP_EXISTING -> {
                    if (existingEnvironments.containsKey(envDto.id)) {
                        targetEnvId = envDto.id
                        shouldInsertEnv = false
                    } else {
                        targetEnvId = envDto.id
                        shouldInsertEnv = true
                    }
                }
            }

            if (shouldInsertEnv) {
                val environment = Environment(
                    id = targetEnvId,
                    name = envDto.name,
                    color = envDto.color,
                    variables = envDto.variables.map { it.toDomain() },
                    createdAt = if (envDto.createdAt > 0) envDto.createdAt else 1000L,
                    updatedAt = if (envDto.updatedAt > 0) envDto.updatedAt else 1000L
                )
                if (existingEnvironments.containsKey(targetEnvId)) {
                    environmentRepository.updateEnvironment(environment)
                } else {
                    environmentRepository.insertEnvironment(environment)
                }
                environmentsCount++
            }
        }

        return WorkspaceImportResult.Success(
            collectionsImported = collectionsCount,
            requestsImported = requestsCount,
            environmentsImported = environmentsCount,
            warnings = warnings
        )
    }

    private fun Collection.toExportDto(requests: List<Request>): ExportCollectionDto {
        return ExportCollectionDto(
            id = id,
            name = name,
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt,
            folders = emptyList(),
            requests = requests.map { it.toExportDto() }
        )
    }

    private fun Request.toExportDto(): ExportRequestDto {
        return ExportRequestDto(
            id = id,
            collectionId = collectionId,
            folderId = folderId,
            name = name,
            method = method.name,
            url = url,
            headers = headers.map { it.toExportDto() },
            queryParams = queryParams.map { it.toExportDto() },
            body = body.toExportDto(),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun RequestField.toExportDto(): ExportFieldDto {
        return ExportFieldDto(
            id = id,
            key = key,
            value = value,
            isEnabled = isEnabled,
            description = description
        )
    }

    private fun ExportFieldDto.toDomain(): RequestField {
        return RequestField(
            id = id,
            key = key,
            value = value,
            isEnabled = isEnabled,
            description = description
        )
    }

    private fun RequestBody.toExportDto(): ExportRequestBodyDto {
        return when (this) {
            is RequestBody.NoBody -> ExportRequestBodyDto(type = "NO_BODY")
            is RequestBody.TextBody -> ExportRequestBodyDto(
                type = "TEXT",
                content = content,
                contentType = contentType
            )
            is RequestBody.FormDataBody -> ExportRequestBodyDto(
                type = "FORM_DATA",
                fields = parts.map { it.toExportDto() }
            )
            is RequestBody.UrlEncodedBody -> ExportRequestBodyDto(
                type = "URL_ENCODED",
                fields = fields.map { it.toExportDto() }
            )
        }
    }

    private fun ExportRequestBodyDto.toDomain(): RequestBody {
        return when (type.uppercase()) {
            "TEXT" -> RequestBody.TextBody(
                content = content ?: "",
                contentType = contentType ?: "text/plain"
            )
            "FORM_DATA" -> RequestBody.FormDataBody(
                parts = fields.map { it.toDomain() }
            )
            "URL_ENCODED" -> RequestBody.UrlEncodedBody(
                fields = fields.map { it.toDomain() }
            )
            else -> RequestBody.NoBody
        }
    }

    private fun Environment.toExportDto(includeSecrets: Boolean): ExportEnvironmentDto {
        return ExportEnvironmentDto(
            id = id,
            name = name,
            color = color,
            variables = variables.map { v ->
                ExportVariableDto(
                    id = v.id,
                    key = v.key,
                    value = if (v.isSecret && !includeSecrets) "" else v.value,
                    isEnabled = v.isEnabled,
                    isSecret = v.isSecret
                )
            },
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun ExportVariableDto.toDomain(): Variable {
        return Variable(
            id = id,
            key = key,
            value = value,
            isEnabled = isEnabled,
            isSecret = isSecret
        )
    }
}
