package com.learn.reqlite.data.mapper

import com.learn.reqlite.data.local.entity.DraftEntity
import com.learn.reqlite.data.local.entity.RequestBodyEntity
import com.learn.reqlite.data.local.entity.RequestFieldEntity
import com.learn.reqlite.domain.model.Draft
import com.learn.reqlite.domain.model.HttpMethod

fun DraftEntity.toDomain(
    fields: List<RequestFieldEntity>,
    body: RequestBodyEntity?
): Draft {
    return Draft(
        id = this.id,
        requestId = this.requestId,
        method = HttpMethod.valueOf(this.method),
        url = this.url,
        headers = fields.filter { it.type == "HEADER" }.map { it.toDomain() },
        queryParams = fields.filter { it.type == "QUERY_PARAM" }.map { it.toDomain() },
        body = body?.toDomain(fields) ?: com.learn.reqlite.domain.model.RequestBody.NoBody,
        updatedAt = this.updatedAt
    )
}

fun Draft.toEntity(): DraftEntity {
    return DraftEntity(
        id = this.id,
        requestId = this.requestId,
        method = this.method.name,
        url = this.url,
        updatedAt = this.updatedAt
    )
}
