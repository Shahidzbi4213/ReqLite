package com.learn.reqlite.di

import com.learn.reqlite.domain.engine.RequestExecutionEngine
import com.learn.reqlite.domain.export.WorkspaceExporter
import com.learn.reqlite.domain.export.WorkspaceImporter
import com.learn.reqlite.domain.parser.CurlParser
import com.learn.reqlite.domain.repository.EnvironmentRepository
import com.learn.reqlite.domain.repository.HistoryRepository
import com.learn.reqlite.domain.repository.RequestRepository
import com.learn.reqlite.domain.repository.SecureStorage
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object DiHelper : KoinComponent {
    fun initKoin() {
        com.learn.reqlite.di.initKoin()
    }

    fun getRequestExecutionEngine(): RequestExecutionEngine = get()
    fun getEnvironmentRepository(): EnvironmentRepository = get()
    fun getRequestRepository(): RequestRepository = get()
    fun getHistoryRepository(): HistoryRepository = get()
    fun getSecureStorage(): SecureStorage = get()
    fun getCurlParser(): CurlParser = get()
    fun getWorkspaceExporter(): WorkspaceExporter = get()
    fun getWorkspaceImporter(): WorkspaceImporter = get()
}
