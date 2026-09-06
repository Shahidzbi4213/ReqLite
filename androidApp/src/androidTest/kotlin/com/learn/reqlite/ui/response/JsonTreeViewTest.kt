package com.learn.reqlite.ui.response

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.learn.reqlite.ui.theme.ReqLiteTheme
import org.junit.Rule
import org.junit.Test

class JsonTreeViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun jsonTreeView_rendersJsonObjectStructure() {
        val json = """
            {
                "appName": "ReqLite",
                "version": 1,
                "isReleased": false
            }
        """.trimIndent()

        composeTestRule.setContent {
            ReqLiteTheme {
                JsonTreeView(rawJson = json)
            }
        }

        composeTestRule.onNodeWithText("appName: ").assertIsDisplayed()
        composeTestRule.onNodeWithText("\"ReqLite\"").assertIsDisplayed()
        composeTestRule.onNodeWithText("version: ").assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("isReleased: ").assertIsDisplayed()
        composeTestRule.onNodeWithText("false").assertIsDisplayed()
    }

    @Test
    fun jsonTreeView_expandAndCollapseNestedNodes() {
        val json = """
            {
                "user": {
                    "id": "100",
                    "username": "tester"
                }
            }
        """.trimIndent()

        composeTestRule.setContent {
            ReqLiteTheme {
                JsonTreeView(rawJson = json)
            }
        }

        // Root is expanded by default; user node summary is visible
        composeTestRule.onNodeWithText("user: ").assertIsDisplayed()
        composeTestRule.onNodeWithText("{ 2 keys }").assertIsDisplayed()

        // Expand all button
        composeTestRule.onNodeWithText("Expand All").performClick()

        // Now nested children should be visible
        composeTestRule.onNodeWithText("id: ").assertIsDisplayed()
        composeTestRule.onNodeWithText("\"100\"").assertIsDisplayed()
        composeTestRule.onNodeWithText("username: ").assertIsDisplayed()
        composeTestRule.onNodeWithText("\"tester\"").assertIsDisplayed()
    }

    @Test
    fun jsonTreeView_invalidJson_displaysErrorNotice() {
        composeTestRule.setContent {
            ReqLiteTheme {
                JsonTreeView(rawJson = "not a valid json")
            }
        }

        composeTestRule.onNodeWithText("Cannot parse JSON tree").assertIsDisplayed()
    }
}
