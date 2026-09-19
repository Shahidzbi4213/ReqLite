package com.learn.reqlite

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class AppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun app_displaysHomeScreen() {
        composeTestRule.setContent {
            App()
        }

        composeTestRule.onNodeWithText("ReqLite").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quick Request").assertIsDisplayed()
        composeTestRule.onNodeWithText("Import from cURL").assertIsDisplayed()
    }
}
