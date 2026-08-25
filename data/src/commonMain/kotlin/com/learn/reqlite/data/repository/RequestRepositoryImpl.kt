package com.learn.reqlite.data.repository

import com.learn.reqlite.data.local.dao.RequestDao
import com.learn.reqlite.data.mapper.toDomain
import com.learn.reqlite.data.mapper.toEntity
import com.learn.reqlite.domain.model.Request
import com.learn.reqlite.domain.repository.RequestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RequestRepositoryImpl(
    private val requestDao: RequestDao
) : RequestRepository {

    override suspend fun insertRequest(request: Request) {
        requestDao.insertRequest(request.toEntity())
        
        val fields = request.headers.map { it.toEntity(request.id, null, "HEADER") } +
                request.queryParams.map { it.toEntity(request.id, null, "QUERY_PARAM") }
        
        if (fields.isNotEmpty()) {
            requestDao.insertRequestFields(fields)
        }
        
        val bodyEntity = request.body.toEntity(request.id, null)
        requestDao.insertRequestBody(bodyEntity)
    }

    override suspend fun getRequestById(id: String): Request? {
        val entity = requestDao.getRequestById(id) ?: return null
        val fields = requestDao.getFieldsForRequest(id)
        val body = requestDao.getBodyForRequest(id)
        return entity.toDomain(fields, body)
    }

    override fun getAllRequests(): Flow<List<Request>> {
        return requestDao.getAllRequests().map { entities ->
            entities.map { entity ->
                val fields = requestDao.getFieldsForRequest(entity.id)
                val body = requestDao.getBodyForRequest(entity.id)
                entity.toDomain(fields, body)
            }
        }
    }

    override suspend fun deleteRequest(id: String) {
        requestDao.deleteRequest(id)
    }

    override suspend fun getDraftById(id: String): com.learn.reqlite.domain.model.Draft? {
        val entity = requestDao.getDraftById(id) ?: return null
        val fields = requestDao.getFieldsForDraft(id)
        val body = requestDao.getBodyForDraft(id)
        return entity.toDomain(fields, body)
    }
}
