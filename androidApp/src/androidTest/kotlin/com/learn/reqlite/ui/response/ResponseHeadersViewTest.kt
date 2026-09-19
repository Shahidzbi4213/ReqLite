package com.learn.reqlite.ui.response

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.learn.reqlite.ui.theme.ReqLiteTheme
import org.junit.Rule
import org.junit.Test

class ResponseHeadersViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun responseHeadersView_displaysHeadersList() {
        val headers = listOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer secret_token",
            "X-Request-ID" to "req-12345"
        )

        composeTestRule.setContent {
            ReqLiteTheme {
                ResponseHeadersView(headers = headers)
            }
        }

        composeTestRule.onNodeWithText("Content-Type").assertIsDisplayed()
        composeTestRule.onNodeWithText("application/json").assertIsDisplayed()
        composeTestRule.onNodeWithText("Authorization").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bearer secret_token").assertIsDisplayed()
        composeTestRule.onNodeWithText("X-Request-ID").assertIsDisplayed()
        composeTestRule.onNodeWithText("req-12345").assertIsDisplayed()
        composeTestRule.onNodeWithText("Copy All").assertIsDisplayed()
    }

    @Test
    fun responseHeadersView_filtersHeaders() {
        val headers = listOf(
            "Content-Type" to "application/json",
            "Cache-Control" to "no-cache",
            "Server" to "Ktor"
        )

        composeTestRule.setContent {
            ReqLiteTheme {
                ResponseHeadersView(headers = headers)
            }
        }

        composeTestRule.onNodeWithText("Filter headers...").performTextInput("Cache")

        composeTestRule.onNodeWithText("Cache-Control").assertIsDisplayed()
        composeTestRule.onNodeWithText("no-cache").assertIsDisplayed()
    }

    @Test
    fun responseHeadersView_emptyHeaders_displaysEmptyMessage() {
        composeTestRule.setContent {
            ReqLiteTheme {
                ResponseHeadersView(headers = emptyList())
            }
        }

        composeTestRule.onNodeWithText("No headers received").assertIsDisplayed()
    }
}
