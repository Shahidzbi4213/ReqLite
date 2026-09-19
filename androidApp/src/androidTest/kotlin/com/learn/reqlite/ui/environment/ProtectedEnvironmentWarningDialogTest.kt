package com.learn.reqlite.ui.environment

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.learn.reqlite.ui.theme.ReqLiteTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProtectedEnvironmentWarningDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun protectedEnvironmentWarningDialog_displaysDetailsAndHandlesConfirm() {
        val prodEnv = EnvironmentUiModel(
            id = "env_prod",
            name = "Live Production",
            isProtected = true
        )

        var confirmed = false
        var dismissed = false

        composeTestRule.setContent {
            ReqLiteTheme {
                ProtectedEnvironmentWarningDialog(
                    environment = prodEnv,
                    method = "DELETE",
                    url = "https://api.prod.com/v1/users/42",
                    onConfirm = { confirmed = true },
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Protected Environment").assertIsDisplayed()
        composeTestRule.onNodeWithText("DELETE").assertIsDisplayed()
        composeTestRule.onNodeWithText("Live Production").assertIsDisplayed()
        composeTestRule.onNodeWithText("https://api.prod.com/v1/users/42").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm & Send").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()

        composeTestRule.onNodeWithText("Confirm & Send").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun protectedEnvironmentWarningDialog_handlesCancel() {
        val prodEnv = EnvironmentUiModel(
            id = "env_prod",
            name = "Live Production",
            isProtected = true
        )

        var dismissed = false

        composeTestRule.setContent {
            ReqLiteTheme {
                ProtectedEnvironmentWarningDialog(
                    environment = prodEnv,
                    method = "POST",
                    url = "https://api.prod.com/v1/data",
                    onConfirm = {},
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        assertTrue(dismissed)
    }
}
