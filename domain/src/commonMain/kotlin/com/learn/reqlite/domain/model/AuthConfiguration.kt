package com.learn.reqlite.domain.model

sealed interface AuthConfiguration {
    data object None : AuthConfiguration
    data class Bearer(val token: String) : AuthConfiguration
    data class Basic(val username: String, val password: String) : AuthConfiguration
}
