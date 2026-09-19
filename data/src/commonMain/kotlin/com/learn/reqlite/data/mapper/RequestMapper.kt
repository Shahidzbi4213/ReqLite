package com.learn.reqlite.data.mapper

import com.learn.reqlite.data.local.entity.RequestBodyEntity
import com.learn.reqlite.data.local.entity.RequestEntity
import com.learn.reqlite.data.local.entity.RequestFieldEntity
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.Request
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.domain.model.RequestField

fun RequestEntity.toDomain(
    fields: List<RequestFieldEntity>,
    bodyEntity: RequestBodyEntity?
): Request {
    return Request(
        id = id,
        collectionId = collectionId,
        folderId = folderId,
        name = name,
        method = HttpMethod.valueOf(method),
        url = url,
        headers = fields.filter { it.type == "HEADER" }.map { it.toDomain() },
        queryParams = fields.filter { it.type == "QUERY_PARAM" }.map { it.toDomain() },
        body = bodyEntity?.toDomain(fields) ?: RequestBody.NoBody,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Request.toEntity(): RequestEntity {
    return RequestEntity(
        id = id,
        collectionId = collectionId,
        folderId = folderId,
        name = name,
        method = method.name,
        url = url,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun RequestFieldEntity.toDomain(): RequestField {
    return RequestField(
        id = id,
        key = key,
        value = value,
        isEnabled = isEnabled,
        description = description
    )
}

fun RequestField.toEntity(requestId: String?, draftId: String?, type: String): RequestFieldEntity {
    return RequestFieldEntity(
        id = id,
        requestId = requestId,
        draftId = draftId,
        type = type,
        key = key,
        value = value,
        isEnabled = isEnabled,
        description = description
    )
}

fun RequestBodyEntity.toDomain(fields: List<RequestFieldEntity>): RequestBody {
    return when (type) {
        "TEXT" -> RequestBody.TextBody(content ?: "", contentType ?: "text/plain")
        "FORM_DATA" -> RequestBody.FormDataBody(fields.filter { it.type == "FORM_DATA" }.map { it.toDomain() })
        "URL_ENCODED" -> RequestBody.UrlEncodedBody(fields.filter { it.type == "URL_ENCODED" }.map { it.toDomain() })
        else -> RequestBody.NoBody
    }
}

fun RequestBody.toEntity(requestId: String?, draftId: String?): RequestBodyEntity {
    return when (this) {
        is RequestBody.NoBody -> RequestBodyEntity(
            requestId = requestId,
            draftId = draftId,
            type = "NO_BODY",
            content = null,
            contentType = null
        )
        is RequestBody.TextBody -> RequestBodyEntity(
            requestId = requestId,
            draftId = draftId,
            type = "TEXT",
            content = content,
            contentType = contentType
        )
        is RequestBody.FormDataBody -> RequestBodyEntity(
            requestId = requestId,
            draftId = draftId,
            type = "FORM_DATA",
            content = null,
            contentType = null
        )
        is RequestBody.UrlEncodedBody -> RequestBodyEntity(
            requestId = requestId,
            draftId = draftId,
            type = "URL_ENCODED",
            content = null,
            contentType = null
        )
    }
}
