package com.learn.reqlite.ui.workspace

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.ui.theme.ReqLiteTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RequestBodyEditorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun requestBodyEditor_displaysInitialStateAndCanChangeType() {
        var body: RequestBody = RequestBody.NoBody

        composeTestRule.setContent {
            ReqLiteTheme {
                RequestBodyEditor(
                    body = body,
                    onBodyChange = { body = it }
                )
            }
        }

        // None should be selected
        composeTestRule.onNodeWithText("None").assertIsDisplayed()

        // Open Dropdown
        composeTestRule.onNodeWithText("None").performClick()

        // Select Text
        composeTestRule.onNodeWithText("Text").performClick()

        assertTrue(body is RequestBody.TextBody)
    }

    @Test
    fun requestBodyEditor_showsTextFieldForTextBody() {
        composeTestRule.setContent {
            ReqLiteTheme {
                RequestBodyEditor(
                    body = RequestBody.TextBody("", "text/plain"),
                    onBodyChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Content").assertIsDisplayed()
        composeTestRule.onNodeWithText("Content Type").assertIsDisplayed()
    }
}
