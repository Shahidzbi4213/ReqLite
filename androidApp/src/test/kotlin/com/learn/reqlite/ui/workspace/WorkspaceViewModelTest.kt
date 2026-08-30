package com.learn.reqlite.ui.workspace

import com.learn.reqlite.domain.engine.RequestExecutionEngine
import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.model.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var fakeEngine: FakeRequestExecutionEngine
    private lateinit var viewModel: WorkspaceViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeEngine = FakeRequestExecutionEngine()
        viewModel = WorkspaceViewModel(fakeEngine)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun executeRequest_updatesStateToLoadingThenSuccess() = runTest {
        assertEquals(ExecutionUiState.Idle, viewModel.executionState.value)

        viewModel.executeRequest("draft_123")
        
        val state = viewModel.executionState.value
        assertTrue(state is ExecutionUiState.Success)
        val successState = state as ExecutionUiState.Success
        assertEquals("draft_123", successState.historyEntry.requestId)
    }

    @Test
    fun executeRequest_updatesStateToErrorOnFailure() = runTest {
        fakeEngine.shouldFail = true

        viewModel.executeRequest("draft_error")
        
        advanceUntilIdle()
        
        val state = viewModel.executionState.value
        assertTrue(state is ExecutionUiState.Error)
        val errorState = state as ExecutionUiState.Error
        assertEquals("Engine failure", errorState.message)
    }

    class FakeRequestExecutionEngine : RequestExecutionEngine {
        var shouldFail = false

        override suspend fun execute(draftId: String, environmentId: String?): HistoryEntry {
            if (shouldFail) {
                throw Exception("Engine failure")
            }
            return HistoryEntry(
                id = "hist_1",
                requestId = draftId,
                requestMethod = HttpMethod.GET,
                requestUrl = "https://example.com",
                statusCode = 200,
                durationMs = 100,
                timestamp = 1000,
                responseArtifactId = "art_1"
            )
        }
    }
}
