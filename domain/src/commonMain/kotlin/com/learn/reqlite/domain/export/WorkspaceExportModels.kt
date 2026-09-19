package com.learn.reqlite.domain.export

import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceExportDto(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appName: String = APP_NAME,
    val exportedAt: Long,
    val collections: List<ExportCollectionDto> = emptyList(),
    val environments: List<ExportEnvironmentDto> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val APP_NAME = "ReqLite"
    }
}

@Serializable
data class ExportCollectionDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val folders: List<ExportFolderDto> = emptyList(),
    val requests: List<ExportRequestDto> = emptyList()
)

@Serializable
data class ExportFolderDto(
    val id: String,
    val collectionId: String,
    val parentFolderId: String? = null,
    val name: String,
    val description: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class ExportRequestDto(
    val id: String,
    val collectionId: String,
    val folderId: String? = null,
    val name: String,
    val method: String,
    val url: String,
    val headers: List<ExportFieldDto> = emptyList(),
    val queryParams: List<ExportFieldDto> = emptyList(),
    val body: ExportRequestBodyDto = ExportRequestBodyDto(type = "NO_BODY"),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class ExportFieldDto(
    val id: String,
    val key: String,
    val value: String,
    val isEnabled: Boolean = true,
    val description: String? = null
)

@Serializable
data class ExportRequestBodyDto(
    val type: String, // "NO_BODY", "TEXT", "FORM_DATA", "URL_ENCODED"
    val content: String? = null,
    val contentType: String? = null,
    val fields: List<ExportFieldDto> = emptyList()
)

@Serializable
data class ExportEnvironmentDto(
    val id: String,
    val name: String,
    val color: String? = null,
    val variables: List<ExportVariableDto> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class ExportVariableDto(
    val id: String,
    val key: String,
    val value: String,
    val isEnabled: Boolean = true,
    val isSecret: Boolean = false
)
