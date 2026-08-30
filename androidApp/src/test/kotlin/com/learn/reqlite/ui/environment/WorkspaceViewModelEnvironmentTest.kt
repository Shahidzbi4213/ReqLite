package com.learn.reqlite.ui.environment

import com.learn.reqlite.domain.engine.RequestExecutionEngine
import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.ui.workspace.ExecutionUiState
import com.learn.reqlite.ui.workspace.WorkspaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModelEnvironmentTest {

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
    fun selectEnvironment_updatesActiveEnvironment() {
        assertNull(viewModel.activeEnvironment.value)

        val devEnv = EnvironmentUiModel(id = "env_1", name = "Development", isProtected = false)
        viewModel.selectEnvironment(devEnv)

        assertEquals("env_1", viewModel.activeEnvironment.value?.id)
        assertEquals("Development", viewModel.activeEnvironment.value?.name)

        viewModel.selectEnvironment(null)
        assertNull(viewModel.activeEnvironment.value)
    }

    @Test
    fun onSendClicked_standardEnvironment_executesDirectly() = runTest {
        val devEnv = EnvironmentUiModel(id = "env_dev", name = "Development", isProtected = false)
        viewModel.selectEnvironment(devEnv)

        viewModel.onSendClicked(draftId = "draft_1", method = "GET", url = "https://dev.api.com")

        assertFalse(viewModel.isProtectedWarningVisible.value)
        assertNull(viewModel.pendingExecution.value)
        assertTrue(viewModel.executionState.value is ExecutionUiState.Success)
        assertEquals("env_dev", fakeEngine.lastEnvironmentId)
    }

    @Test
    fun onSendClicked_protectedEnvironment_triggersWarningDialog() = runTest {
        val prodEnv = EnvironmentUiModel(id = "env_prod", name = "Production", isProtected = true, requireConfirmation = true)
        viewModel.selectEnvironment(prodEnv)

        viewModel.onSendClicked(draftId = "draft_prod", method = "POST", url = "https://api.prod.com/v1/delete")

        // Should NOT execute yet
        assertTrue(viewModel.isProtectedWarningVisible.value)
        assertNotNull(viewModel.pendingExecution.value)
        assertEquals("draft_prod", viewModel.pendingExecution.value?.draftId)
        assertEquals("POST", viewModel.pendingExecution.value?.method)
        assertEquals("https://api.prod.com/v1/delete", viewModel.pendingExecution.value?.url)
        assertEquals(ExecutionUiState.Idle, viewModel.executionState.value)
    }

    @Test
    fun confirmProtectedExecution_executesPendingRequestAndClosesDialog() = runTest {
        val prodEnv = EnvironmentUiModel(id = "env_prod", name = "Production", isProtected = true, requireConfirmation = true)
        viewModel.selectEnvironment(prodEnv)

        viewModel.onSendClicked(draftId = "draft_prod", method = "POST", url = "https://api.prod.com/v1/data")
        assertTrue(viewModel.isProtectedWarningVisible.value)

        // Confirm
        viewModel.confirmProtectedExecution()

        assertFalse(viewModel.isProtectedWarningVisible.value)
        assertNull(viewModel.pendingExecution.value)
        assertTrue(viewModel.executionState.value is ExecutionUiState.Success)
        assertEquals("env_prod", fakeEngine.lastEnvironmentId)
    }

    @Test
    fun dismissProtectedWarning_cancelsPendingExecutionWithoutExecuting() = runTest {
        val prodEnv = EnvironmentUiModel(id = "env_prod", name = "Production", isProtected = true, requireConfirmation = true)
        viewModel.selectEnvironment(prodEnv)

        viewModel.onSendClicked(draftId = "draft_prod", method = "DELETE", url = "https://api.prod.com/v1/user")
        assertTrue(viewModel.isProtectedWarningVisible.value)

        // Dismiss
        viewModel.dismissProtectedWarning()

        assertFalse(viewModel.isProtectedWarningVisible.value)
        assertNull(viewModel.pendingExecution.value)
        assertEquals(ExecutionUiState.Idle, viewModel.executionState.value)
        assertNull(fakeEngine.lastEnvironmentId)
    }

    class FakeRequestExecutionEngine : RequestExecutionEngine {
        var lastEnvironmentId: String? = null

        override suspend fun execute(draftId: String, environmentId: String?): HistoryEntry {
            lastEnvironmentId = environmentId
            return HistoryEntry(
                id = "hist_1",
                requestId = draftId,
                requestMethod = HttpMethod.GET,
                requestUrl = "https://example.com",
                statusCode = 200,
                durationMs = 50,
                timestamp = 1000
            )
        }
    }
}
