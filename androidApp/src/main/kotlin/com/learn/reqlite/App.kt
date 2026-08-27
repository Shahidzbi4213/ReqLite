package com.learn.reqlite

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable

import com.learn.reqlite.ui.home.HomeScreen
import com.learn.reqlite.ui.theme.ReqLiteTheme

@Serializable
object HomeDestination

@Composable
fun App() {
    ReqLiteTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController, 
            startDestination = HomeDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            composable<HomeDestination> {
                HomeScreen(
                    onNavigateToRequest = { url ->
                        // TODO: Navigate to request details
                    }
                )
            }
        }
    }
}
