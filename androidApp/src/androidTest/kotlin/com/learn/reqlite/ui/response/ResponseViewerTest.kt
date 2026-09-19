package com.learn.reqlite.ui.response

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.learn.reqlite.ui.theme.ReqLiteTheme
import org.junit.Rule
import org.junit.Test

class ResponseViewerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleResponse = HttpResponseUiModel(
        statusCode = 200,
        statusText = "OK",
        durationMs = 150,
        sizeBytes = 2048,
        contentType = "application/json",
        headers = listOf(
            "Content-Type" to "application/json",
            "Server" to "Ktor"
        ),
        body = """{"message": "success", "count": 42}""",
        url = "https://api.example.com/v1/data",
        method = "GET"
    )

    @Test
    fun responseViewer_displaysStatusAndTabs() {
        composeTestRule.setContent {
            ReqLiteTheme {
                ResponseViewer(response = sampleResponse)
            }
        }

        composeTestRule.onNodeWithText("200 OK").assertIsDisplayed()
        composeTestRule.onNodeWithText("150 ms").assertIsDisplayed()
        composeTestRule.onNodeWithText("2.0 KB").assertIsDisplayed()
        composeTestRule.onNodeWithText("Body").assertIsDisplayed()
        composeTestRule.onNodeWithText("Headers (2)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Summary").assertIsDisplayed()
    }

    @Test
    fun responseViewer_switchesTabsAndModes() {
        composeTestRule.setContent {
            ReqLiteTheme {
                ResponseViewer(response = sampleResponse)
            }
        }

        // Switch to Headers tab
        composeTestRule.onNodeWithText("Headers (2)").performClick()
        composeTestRule.onNodeWithText("Content-Type").assertIsDisplayed()
        composeTestRule.onNodeWithText("Server").assertIsDisplayed()

        // Switch to Summary tab
        composeTestRule.onNodeWithText("Summary").performClick()
        composeTestRule.onNodeWithText("Response Overview").assertIsDisplayed()
        composeTestRule.onNodeWithText("Request Target").assertIsDisplayed()
        composeTestRule.onNodeWithText("GET https://api.example.com/v1/data").assertIsDisplayed()

        // Switch back to Body tab
        composeTestRule.onNodeWithText("Body").performClick()
        composeTestRule.onNodeWithText("JSON Tree").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pretty JSON").assertIsDisplayed()
        composeTestRule.onNodeWithText("Raw Text").assertIsDisplayed()

        // Switch to Pretty JSON sub-mode
        composeTestRule.onNodeWithText("Pretty JSON").performClick()
        composeTestRule.onNodeWithText("Copy JSON").assertIsDisplayed()

        // Switch to Raw Text sub-mode
        composeTestRule.onNodeWithText("Raw Text").performClick()
        composeTestRule.onNodeWithText("Wrap").assertIsDisplayed()
    }
}
