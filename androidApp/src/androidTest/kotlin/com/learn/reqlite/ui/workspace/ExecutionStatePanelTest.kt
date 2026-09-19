package com.learn.reqlite.ui.workspace

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.learn.reqlite.domain.model.HistoryEntry
import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.ui.theme.ReqLiteTheme
import org.junit.Rule
import org.junit.Test

class ExecutionStatePanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun executionStatePanel_displaysIdleState() {
        composeTestRule.setContent {
            ReqLiteTheme {
                ExecutionStatePanel(state = ExecutionUiState.Idle)
            }
        }
        composeTestRule.onNodeWithText("Enter a URL and hit Send").assertIsDisplayed()
    }

    @Test
    fun executionStatePanel_displaysLoadingState() {
        composeTestRule.setContent {
            ReqLiteTheme {
                ExecutionStatePanel(state = ExecutionUiState.Loading(progress = null))
            }
        }
        composeTestRule.onNodeWithText("Sending request...").assertIsDisplayed()
    }

    @Test
    fun executionStatePanel_displaysProgressState() {
        composeTestRule.setContent {
            ReqLiteTheme {
                ExecutionStatePanel(state = ExecutionUiState.Loading(progress = 0.5f))
            }
        }
        composeTestRule.onNodeWithText("Loading... 50%").assertIsDisplayed()
    }

    @Test
    fun executionStatePanel_displaysStreamingState() {
        composeTestRule.setContent {
            ReqLiteTheme {
                ExecutionStatePanel(state = ExecutionUiState.Streaming("Partial data..."))
            }
        }
        composeTestRule.onNodeWithText("Streaming Response...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Partial data...").assertIsDisplayed()
    }

    @Test
    fun executionStatePanel_displaysSuccessState() {
        val entry = HistoryEntry(
            id = "1",
            requestId = "req_1",
            requestMethod = HttpMethod.GET,
            requestUrl = "https://example.com",
            statusCode = 200,
            durationMs = 123,
            timestamp = 1000,
            responseArtifactId = "art_1"
        )
        composeTestRule.setContent {
            ReqLiteTheme {
                ExecutionStatePanel(state = ExecutionUiState.Success(entry))
            }
        }
        composeTestRule.onNodeWithText("200 OK").assertIsDisplayed()
        composeTestRule.onNodeWithText("123 ms").assertIsDisplayed()
    }

    @Test
    fun executionStatePanel_displaysErrorState() {
        composeTestRule.setContent {
            ReqLiteTheme {
                ExecutionStatePanel(state = ExecutionUiState.Error("Connection failed"))
            }
        }
        composeTestRule.onNodeWithText("Error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connection failed").assertIsDisplayed()
    }
}
