package com.learn.reqlite.domain.snippet

enum class CodeSnippetLanguage(
    val id: String,
    val displayName: String,
    val category: String,
    val fileExtension: String,
    val syntaxHighlightLang: String
) {
    CURL("curl", "cURL", "Shell", "sh", "bash"),
    KOTLIN_KTOR("kotlin_ktor", "Kotlin (Ktor)", "Kotlin", "kt", "kotlin"),
    KOTLIN_OKHTTP("kotlin_okhttp", "Kotlin (OkHttp)", "Kotlin", "kt", "kotlin"),
    SWIFT_URLSESSION("swift_urlsession", "Swift (URLSession)", "Swift", "swift", "swift"),
    SWIFT_ALAMOFIRE("swift_alamofire", "Swift (Alamofire)", "Swift", "swift", "swift"),
    JAVASCRIPT_FETCH("js_fetch", "JavaScript (Fetch)", "JavaScript", "js", "javascript"),
    JAVASCRIPT_AXIOS("js_axios", "JavaScript (Axios)", "JavaScript", "js", "javascript"),
    PYTHON_REQUESTS("python_requests", "Python (Requests)", "Python", "py", "python"),
    PYTHON_HTTPX("python_httpx", "Python (HTTPX)", "Python", "py", "python"),
    GO_NETHTTP("go_nethttp", "Go (net/http)", "Go", "go", "go"),
    DART_HTTP("dart_http", "Dart (http)", "Dart", "dart", "dart")
}
