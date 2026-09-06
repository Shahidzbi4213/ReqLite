package com.learn.reqlite.ui.response

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.learn.reqlite.ui.theme.ReqLiteTheme
import org.junit.Rule
import org.junit.Test

class ResponseStatusSummaryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun responseStatusSummary_displaysStatusDurationAndSize() {
        val model = HttpResponseUiModel(
            statusCode = 200,
            durationMs = 245,
            sizeBytes = 1536,
            contentType = "application/json; charset=utf-8"
        )

        composeTestRule.setContent {
            ReqLiteTheme {
                ResponseStatusSummary(response = model)
            }
        }

        composeTestRule.onNodeWithText("200 OK").assertIsDisplayed()
        composeTestRule.onNodeWithText("245 ms").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.5 KB").assertIsDisplayed()
        composeTestRule.onNodeWithText("JSON").assertIsDisplayed()
    }

    @Test
    fun responseStatusSummary_displaysErrorStatus() {
        val model = HttpResponseUiModel(
            statusCode = 404,
            durationMs = 80,
            sizeBytes = 50,
            contentType = "text/plain"
        )

        composeTestRule.setContent {
            ReqLiteTheme {
                ResponseStatusSummary(response = model)
            }
        }

        composeTestRule.onNodeWithText("404 Not Found").assertIsDisplayed()
        composeTestRule.onNodeWithText("80 ms").assertIsDisplayed()
        composeTestRule.onNodeWithText("50 B").assertIsDisplayed()
        composeTestRule.onNodeWithText("TEXT").assertIsDisplayed()
    }
}
