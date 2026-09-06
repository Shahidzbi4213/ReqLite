package com.learn.reqlite

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

import com.learn.reqlite.ui.home.HomeScreen
import com.learn.reqlite.ui.theme.ReqLiteTheme
import com.learn.reqlite.ui.workspace.WorkspaceScreen
import com.learn.reqlite.ui.response.ResponseScreen

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

@Serializable
object MainListDetailDestination

@Serializable
data class WorkspaceDestination(
    val initialUrl: String? = null,
    val initialMethod: String = "GET"
)

@Serializable
data class ResponseDestination(
    val historyId: String
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun App() {
    ReqLiteTheme {
        val navController = rememberNavController()
        
        NavHost(navController = navController, startDestination = MainListDetailDestination) {
            composable<MainListDetailDestination> {
                val navigator = rememberListDetailPaneScaffoldNavigator<WorkspaceDestination>()
                
                ListDetailPaneScaffold(
                    directive = navigator.scaffoldDirective,
                    value = navigator.scaffoldValue,
                    listPane = {
                        HomeScreen(
                            onNavigateToRequest = { url, method ->
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    WorkspaceDestination(
                                        initialUrl = url,
                                        initialMethod = method ?: "GET"
                                    )
                                )
                            }
                        )
                    },
                    detailPane = {
                        val destination = navigator.currentDestination?.content
                        WorkspaceScreen(
                            initialUrl = destination?.initialUrl,
                            initialMethod = destination?.initialMethod ?: "GET",
                            onNavigateBack = {
                                if (navigator.canNavigateBack()) {
                                    navigator.navigateBack()
                                }
                            },
                            onNavigateToResponse = { historyId ->
                                navController.navigate(ResponseDestination(historyId = historyId))
                            }
                        )
                    }
                )
            }
            
            composable<ResponseDestination> { backStackEntry ->
                val responseDest = backStackEntry.toRoute<ResponseDestination>()
                ResponseScreen(
                    historyId = responseDest.historyId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
