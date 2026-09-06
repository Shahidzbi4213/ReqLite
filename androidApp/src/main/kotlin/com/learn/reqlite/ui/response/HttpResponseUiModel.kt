package com.learn.reqlite.ui.response

data class HttpResponseUiModel(
    val statusCode: Int? = null,
    val statusText: String? = null,
    val durationMs: Long? = null,
    val sizeBytes: Long? = null,
    val contentType: String? = null,
    val headers: List<Pair<String, String>> = emptyList(),
    val body: String = "",
    val timestamp: Long = 0L,
    val url: String? = null,
    val method: String? = null,
    val artifactId: String? = null,
    val error: String? = null
) {
    val statusDescription: String
        get() = HttpStatusHelper.getStatusDescription(statusCode, statusText)

    val statusCategory: HttpStatusCategory
        get() = HttpStatusHelper.getCategory(statusCode)

    val formattedDuration: String
        get() = formatDuration(durationMs)

    val formattedSize: String
        get() = formatSize(sizeBytes)

    val isJsonContentType: Boolean
        get() = contentType?.contains("json", ignoreCase = true) == true ||
                (body.trimStart().startsWith("{") && body.trimEnd().endsWith("}")) ||
                (body.trimStart().startsWith("[") && body.trimEnd().endsWith("]"))

    companion object {
        fun formatDuration(durationMs: Long?): String {
            if (durationMs == null) return "0 ms"
            return if (durationMs >= 1000) {
                val seconds = durationMs / 1000.0
                "${(seconds * 100).toLong() / 100.0} s"
            } else {
                "$durationMs ms"
            }
        }

        fun formatSize(bytes: Long?): String {
            if (bytes == null || bytes <= 0) return "0 B"
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${((bytes / 1024.0) * 10).toLong() / 10.0} KB"
                else -> "${((bytes / (1024.0 * 1024.0)) * 10).toLong() / 10.0} MB"
            }
        }
    }
}

enum class HttpStatusCategory {
    SUCCESS,
    REDIRECT,
    CLIENT_ERROR,
    SERVER_ERROR,
    UNKNOWN
}

object HttpStatusHelper {
    fun getCategory(statusCode: Int?): HttpStatusCategory {
        return when (statusCode) {
            in 200..299 -> HttpStatusCategory.SUCCESS
            in 300..399 -> HttpStatusCategory.REDIRECT
            in 400..499 -> HttpStatusCategory.CLIENT_ERROR
            in 500..599 -> HttpStatusCategory.SERVER_ERROR
            else -> HttpStatusCategory.UNKNOWN
        }
    }

    fun getStatusDescription(statusCode: Int?, customText: String? = null): String {
        if (customText != null && customText.isNotBlank()) {
            return if (statusCode != null) "$statusCode $customText" else customText
        }
        if (statusCode == null) return "Unknown Status"

        val defaultReason = when (statusCode) {
            100 -> "Continue"
            101 -> "Switching Protocols"
            200 -> "OK"
            201 -> "Created"
            202 -> "Accepted"
            204 -> "No Content"
            206 -> "Partial Content"
            301 -> "Moved Permanently"
            302 -> "Found"
            304 -> "Not Modified"
            307 -> "Temporary Redirect"
            308 -> "Permanent Redirect"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            408 -> "Request Timeout"
            409 -> "Conflict"
            410 -> "Gone"
            415 -> "Unsupported Media Type"
            422 -> "Unprocessable Entity"
            429 -> "Too Many Requests"
            500 -> "Internal Server Error"
            501 -> "Not Implemented"
            502 -> "Bad Gateway"
            503 -> "Service Unavailable"
            504 -> "Gateway Timeout"
            else -> when (statusCode / 100) {
                2 -> "Success"
                3 -> "Redirection"
                4 -> "Client Error"
                5 -> "Server Error"
                else -> "Response"
            }
        }

        return "$statusCode $defaultReason"
    }
}
