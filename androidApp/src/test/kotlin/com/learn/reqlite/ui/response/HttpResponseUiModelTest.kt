package com.learn.reqlite.ui.response

import org.junit.Assert.*
import org.junit.Test

class HttpResponseUiModelTest {

    @Test
    fun httpStatusHelper_returnsCorrectDescriptions() {
        assertEquals("200 OK", HttpStatusHelper.getStatusDescription(200))
        assertEquals("201 Created", HttpStatusHelper.getStatusDescription(201))
        assertEquals("204 No Content", HttpStatusHelper.getStatusDescription(204))
        assertEquals("301 Moved Permanently", HttpStatusHelper.getStatusDescription(301))
        assertEquals("400 Bad Request", HttpStatusHelper.getStatusDescription(400))
        assertEquals("401 Unauthorized", HttpStatusHelper.getStatusDescription(401))
        assertEquals("403 Forbidden", HttpStatusHelper.getStatusDescription(403))
        assertEquals("404 Not Found", HttpStatusHelper.getStatusDescription(404))
        assertEquals("500 Internal Server Error", HttpStatusHelper.getStatusDescription(500))
        assertEquals("502 Bad Gateway", HttpStatusHelper.getStatusDescription(502))
        assertEquals("503 Service Unavailable", HttpStatusHelper.getStatusDescription(503))
        assertEquals("Unknown Status", HttpStatusHelper.getStatusDescription(null))
        assertEquals("200 Success Custom", HttpStatusHelper.getStatusDescription(200, "Success Custom"))
    }

    @Test
    fun httpStatusHelper_categorizesCorrectly() {
        assertEquals(HttpStatusCategory.SUCCESS, HttpStatusHelper.getCategory(200))
        assertEquals(HttpStatusCategory.SUCCESS, HttpStatusHelper.getCategory(204))
        assertEquals(HttpStatusCategory.REDIRECT, HttpStatusHelper.getCategory(302))
        assertEquals(HttpStatusCategory.CLIENT_ERROR, HttpStatusHelper.getCategory(404))
        assertEquals(HttpStatusCategory.CLIENT_ERROR, HttpStatusHelper.getCategory(422))
        assertEquals(HttpStatusCategory.SERVER_ERROR, HttpStatusHelper.getCategory(500))
        assertEquals(HttpStatusCategory.SERVER_ERROR, HttpStatusHelper.getCategory(503))
        assertEquals(HttpStatusCategory.UNKNOWN, HttpStatusHelper.getCategory(null))
    }

    @Test
    fun formatting_durationAndSize() {
        assertEquals("0 ms", HttpResponseUiModel.formatDuration(null))
        assertEquals("45 ms", HttpResponseUiModel.formatDuration(45))
        assertEquals("1.25 s", HttpResponseUiModel.formatDuration(1250))

        assertEquals("0 B", HttpResponseUiModel.formatSize(null))
        assertEquals("0 B", HttpResponseUiModel.formatSize(0))
        assertEquals("500 B", HttpResponseUiModel.formatSize(500))
        assertEquals("2.5 KB", HttpResponseUiModel.formatSize(2560))
        assertEquals("3.5 MB", HttpResponseUiModel.formatSize(3670016))
    }

    @Test
    fun isJsonContentType_detectsFromContentTypeOrBody() {
        val modelJsonHeader = HttpResponseUiModel(
            contentType = "application/json; charset=utf-8",
            body = "anything"
        )
        assertTrue(modelJsonHeader.isJsonContentType)

        val modelJsonObjectBody = HttpResponseUiModel(
            contentType = "text/plain",
            body = "{\"key\": \"val\"}"
        )
        assertTrue(modelJsonObjectBody.isJsonContentType)

        val modelJsonArrayBody = HttpResponseUiModel(
            contentType = null,
            body = "[1, 2, 3]"
        )
        assertTrue(modelJsonArrayBody.isJsonContentType)

        val modelPlainText = HttpResponseUiModel(
            contentType = "text/plain",
            body = "Hello World"
        )
        assertFalse(modelPlainText.isJsonContentType)
    }
}
