package com.learn.reqlite.ui.environment

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.learn.reqlite.ui.theme.ReqLiteTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EnvironmentBadgeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun environmentBadge_displaysNoEnvironment() {
        var clicked = false
        composeTestRule.setContent {
            ReqLiteTheme {
                EnvironmentBadge(
                    activeEnvironment = null,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("No Environment").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Environment").performClick()
        assertTrue(clicked)
    }

    @Test
    fun environmentBadge_displaysStandardEnvironment() {
        val devEnv = EnvironmentUiModel(
            id = "env_1",
            name = "Staging",
            isProtected = false,
            variableCount = 4
        )

        composeTestRule.setContent {
            ReqLiteTheme {
                EnvironmentBadge(
                    activeEnvironment = devEnv,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Staging").assertIsDisplayed()
        composeTestRule.onNodeWithText("(4)").assertIsDisplayed()
    }

    @Test
    fun environmentBadge_displaysProtectedEnvironment() {
        val prodEnv = EnvironmentUiModel(
            id = "env_prod",
            name = "Production",
            isProtected = true,
            variableCount = 5
        )

        composeTestRule.setContent {
            ReqLiteTheme {
                EnvironmentBadge(
                    activeEnvironment = prodEnv,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Production").assertIsDisplayed()
        composeTestRule.onNodeWithText("PROTECTED").assertIsDisplayed()
    }
}
