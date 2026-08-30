package com.learn.reqlite.di

import com.learn.reqlite.ui.home.HomeViewModel
import com.learn.reqlite.ui.workspace.WorkspaceViewModel
import org.koin.dsl.module

val appModule = module {
    factory {
        HomeViewModel(
            historyRepository = get(),
            requestRepository = get()
        )
    }
    factory { 
        WorkspaceViewModel(
            executionEngine = getOrNull(),
            environmentRepository = getOrNull(),
            historyRepository = getOrNull(),
            requestRepository = getOrNull(),
            httpClient = getOrNull(),
            secureStorage = getOrNull(),
            variableResolver = get()
        ) 
    }
}
