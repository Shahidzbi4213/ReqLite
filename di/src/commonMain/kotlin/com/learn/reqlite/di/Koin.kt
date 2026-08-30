package com.learn.reqlite.di

import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module

expect val platformModule: Module

fun initKoin(extraModules: List<Module> = emptyList()) {
    startKoin {
        modules(
            dataModule,
            domainModule,
            platformModule,
            *extraModules.toTypedArray(),
        )
    }.printLogger(Level.ERROR)
}
