package com.learn.reqlite.ui.workspace

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import com.learn.reqlite.ui.theme.ReqLiteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class WorkspaceHeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun workspaceHeader_displaysInitialState() {
        composeTestRule.setContent {
            ReqLiteTheme {
                WorkspaceHeader(
                    url = "https://example.com",
                    onUrlChange = {},
                    method = "GET",
                    onMethodChange = {},
                    onSend = {},
                    onCancel = {}
                )
            }
        }

        composeTestRule.onNodeWithText("GET").assertIsDisplayed()
        composeTestRule.onNodeWithText("https://example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Send").assertIsDisplayed()
        // Cancel should not be displayed when not loading
        composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
    }
    
    @Test
    fun workspaceHeader_displaysCancelWhenLoading() {
        composeTestRule.setContent {
            ReqLiteTheme {
                WorkspaceHeader(
                    url = "https://example.com",
                    onUrlChange = {},
                    method = "GET",
                    onMethodChange = {},
                    onSend = {},
                    onCancel = {},
                    isLoading = true
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        // Send button should still exist but display a CircularProgressIndicator instead of text.
        // The Send text will not be displayed.
        composeTestRule.onNodeWithText("Send").assertDoesNotExist()
    }
}
