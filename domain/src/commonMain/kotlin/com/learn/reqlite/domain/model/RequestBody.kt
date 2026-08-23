package com.learn.reqlite.domain.model

sealed interface RequestBody {
    data object NoBody : RequestBody
    data class TextBody(val content: String, val contentType: String) : RequestBody
    data class FormDataBody(val parts: List<RequestField>) : RequestBody
    data class UrlEncodedBody(val fields: List<RequestField>) : RequestBody
}
