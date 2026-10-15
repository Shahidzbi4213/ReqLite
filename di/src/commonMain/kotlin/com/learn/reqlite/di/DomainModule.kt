package com.learn.reqlite.di

import com.learn.reqlite.domain.export.WorkspaceExportImportManager
import com.learn.reqlite.domain.export.WorkspaceExporter
import com.learn.reqlite.domain.export.WorkspaceImporter
import com.learn.reqlite.domain.parser.CurlParser
import com.learn.reqlite.domain.parser.CurlParserImpl
import com.learn.reqlite.domain.parser.InsomniaCollectionParser
import com.learn.reqlite.domain.parser.InsomniaCollectionParserImpl
import com.learn.reqlite.domain.parser.PostmanCollectionParser
import com.learn.reqlite.domain.parser.PostmanCollectionParserImpl
import com.learn.reqlite.domain.script.ScriptEngine
import com.learn.reqlite.domain.script.ScriptInterpreter
import com.learn.reqlite.domain.snippet.CodeSnippetGenerator
import com.learn.reqlite.domain.snippet.CodeSnippetGeneratorImpl
import org.koin.dsl.module

val domainModule = module {
    single<CurlParser> { CurlParserImpl() }
    single<CodeSnippetGenerator> { CodeSnippetGeneratorImpl() }
    single<PostmanCollectionParser> { PostmanCollectionParserImpl() }
    single<InsomniaCollectionParser> { InsomniaCollectionParserImpl() }
    single<ScriptEngine> { ScriptInterpreter() }
    single { WorkspaceExportImportManager(get(), get(), get(), getOrNull(), get(), get()) }
    single<WorkspaceExporter> { get<WorkspaceExportImportManager>() }
    single<WorkspaceImporter> { get<WorkspaceExportImportManager>() }
}