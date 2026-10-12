package com.learn.reqlite.domain.script

import com.learn.reqlite.domain.script.model.ScriptExecutionResult

interface ScriptEngine {
    fun executePreRequest(script: String?, context: PreRequestContext): ScriptExecutionResult
    fun executePostRequestTests(script: String?, context: PostRequestContext): ScriptExecutionResult
}
