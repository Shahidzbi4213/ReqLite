package com.learn.reqlite.ui.response

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.learn.reqlite.ui.theme.ReqLiteTheme
import org.junit.Rule
import org.junit.Test

class PrettyAndRawViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun prettyJsonView_rendersFormattedJsonAndActions() {
        val json = """{"status":"ok","code":200}"""

        composeTestRule.setContent {
            ReqLiteTheme {
                PrettyJsonView(rawJson = json)
            }
        }

        composeTestRule.onNodeWithText("Copy JSON").assertIsDisplayed()
    }

    @Test
    fun rawTextView_rendersRawTextAndLineCount() {
        val text = "Line 1\nLine 2\nLine 3"

        composeTestRule.setContent {
            ReqLiteTheme {
                RawTextView(rawText = text)
            }
        }

        composeTestRule.onNodeWithText("Line 1\nLine 2\nLine 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("20 characters • 3 lines").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wrap").assertIsDisplayed()
        composeTestRule.onNodeWithText("Copy").assertIsDisplayed()
    }
}
