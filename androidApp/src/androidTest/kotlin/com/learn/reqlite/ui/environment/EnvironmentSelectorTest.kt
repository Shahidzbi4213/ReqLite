package com.learn.reqlite.ui.environment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.learn.reqlite.ui.theme.ReqLiteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class EnvironmentSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun environmentSelector_selectsEnvironmentFromDropdown() {
        val environments = listOf(
            EnvironmentUiModel(id = "env_dev", name = "Development", isProtected = false, variableCount = 2),
            EnvironmentUiModel(id = "env_prod", name = "Production", isProtected = true, variableCount = 5)
        )

        var selectedEnv: EnvironmentUiModel? = null

        composeTestRule.setContent {
            var active by remember { mutableStateOf<EnvironmentUiModel?>(null) }
            ReqLiteTheme {
                EnvironmentSelector(
                    activeEnvironment = active,
                    environments = environments,
                    onSelectEnvironment = {
                        active = it
                        selectedEnv = it
                    }
                )
            }
        }

        // Initially No Environment is shown
        composeTestRule.onNodeWithText("No Environment").assertIsDisplayed()

        // Click to open dropdown
        composeTestRule.onNodeWithText("No Environment").performClick()

        // Menu items should be visible
        composeTestRule.onNodeWithText("ENVIRONMENTS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Development").assertIsDisplayed()
        composeTestRule.onNodeWithText("Production").assertIsDisplayed()

        // Select Production
        composeTestRule.onNodeWithText("Production").performClick()

        // Verify selection updated
        assertEquals("env_prod", selectedEnv?.id)
        composeTestRule.onNodeWithText("Production").assertIsDisplayed()
        composeTestRule.onNodeWithText("PROTECTED").assertIsDisplayed()

        // Open menu again and select No Environment
        composeTestRule.onNodeWithText("Production").performClick()
        composeTestRule.onNodeWithText("No Environment").performClick()

        assertNull(selectedEnv)
    }
}
