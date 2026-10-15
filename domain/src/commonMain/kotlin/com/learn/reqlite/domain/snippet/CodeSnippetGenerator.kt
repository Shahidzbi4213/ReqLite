package com.learn.reqlite.domain.snippet

interface CodeSnippetGenerator {
    fun generateSnippet(request: CodeSnippetRequest, language: CodeSnippetLanguage): String
    fun getSupportedLanguages(): List<CodeSnippetLanguage> = CodeSnippetLanguage.entries
}
