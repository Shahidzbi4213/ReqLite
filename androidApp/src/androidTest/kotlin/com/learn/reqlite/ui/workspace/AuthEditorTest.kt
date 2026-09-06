package com.learn.reqlite.ui.workspace

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.learn.reqlite.ui.theme.ReqLiteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AuthEditorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun authEditor_displaysInitialStateAndCanChangeType() {
        var authConfig: AuthConfiguration = AuthConfiguration.None

        composeTestRule.setContent {
            ReqLiteTheme {
                AuthEditor(
                    authConfiguration = authConfig,
                    onAuthChange = { authConfig = it }
                )
            }
        }

        // By default "None" should be selected (or available)
        composeTestRule.onNodeWithText("None").assertIsDisplayed()

        // Open Dropdown (Assuming there's a selector for Auth Type)
        // Click on the current type to expand
        composeTestRule.onNodeWithText("None").performClick()

        // Select Bearer
        composeTestRule.onNodeWithText("Bearer Token").performClick()

        // Verify state changed
        assert(authConfig is AuthConfiguration.Bearer)
    }

    @Test
    fun authEditor_showsTokenFieldForBearer() {
        composeTestRule.setContent {
            ReqLiteTheme {
                AuthEditor(
                    authConfiguration = AuthConfiguration.Bearer(""),
                    onAuthChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Token").assertIsDisplayed()
    }

    @Test
    fun authEditor_showsUsernamePasswordForBasic() {
        composeTestRule.setContent {
            ReqLiteTheme {
                AuthEditor(
                    authConfiguration = AuthConfiguration.Basic("", ""),
                    onAuthChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
    }
}
