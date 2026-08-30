package com.learn.reqlite.di

import com.learn.reqlite.domain.export.WorkspaceExportImportManager
import com.learn.reqlite.domain.export.WorkspaceExporter
import com.learn.reqlite.domain.export.WorkspaceImporter
import com.learn.reqlite.domain.parser.CurlParser
import com.learn.reqlite.domain.parser.CurlParserImpl
import org.koin.dsl.module

val domainModule = module {
    single<CurlParser> { CurlParserImpl() }
    single { WorkspaceExportImportManager(get(), get(), get()) }
    single<WorkspaceExporter> { get<WorkspaceExportImportManager>() }
    single<WorkspaceImporter> { get<WorkspaceExportImportManager>() }
}