package com.learn.reqlite.di

import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module

expect val platformModule: Module


fun initKoin() = initKoin(emptyList())

internal fun initKoin(extraModules: List<Module>) {
    startKoin {
        modules(
            dataModule,
            domainModule,
            platformModule,
            *extraModules.toTypedArray(),
        )
    }.printLogger(Level.ERROR)
}
