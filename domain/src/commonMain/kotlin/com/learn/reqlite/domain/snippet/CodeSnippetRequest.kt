package com.learn.reqlite.domain.snippet

import com.learn.reqlite.domain.model.AuthConfiguration
import com.learn.reqlite.domain.model.Draft
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.Request
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.domain.model.RequestField

data class CodeSnippetRequest(
    val method: HttpMethod = HttpMethod.GET,
    val url: String,
    val headers: List<RequestField> = emptyList(),
    val queryParams: List<RequestField> = emptyList(),
    val body: RequestBody = RequestBody.NoBody,
    val auth: AuthConfiguration = AuthConfiguration.None
) {
    companion object {
        fun fromDraft(draft: Draft, auth: AuthConfiguration = AuthConfiguration.None): CodeSnippetRequest {
            return CodeSnippetRequest(
                method = draft.method,
                url = draft.url,
                headers = draft.headers,
                queryParams = draft.queryParams,
                body = draft.body,
                auth = auth
            )
        }

        fun fromRequest(request: Request, auth: AuthConfiguration = AuthConfiguration.None): CodeSnippetRequest {
            return CodeSnippetRequest(
                method = request.method,
                url = request.url,
                headers = request.headers,
                queryParams = request.queryParams,
                body = request.body,
                auth = auth
            )
        }
    }
}
