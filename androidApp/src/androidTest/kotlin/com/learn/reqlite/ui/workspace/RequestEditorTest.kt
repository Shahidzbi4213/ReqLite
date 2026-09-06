package com.learn.reqlite.ui.workspace

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import com.learn.reqlite.domain.model.RequestBody
import com.learn.reqlite.domain.model.RequestField
import com.learn.reqlite.ui.theme.ReqLiteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RequestEditorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun requestEditor_showsTabsAndEmptyState() {
        composeTestRule.setContent {
            ReqLiteTheme {
                RequestEditor(
                    queryParams = emptyList(),
                    headers = emptyList(),
                    body = RequestBody.NoBody,
                    authConfiguration = AuthConfiguration.None,
                    onQueryParamsChange = {},
                    onHeadersChange = {},
                    onBodyChange = {},
                    onAuthChange = {}
                )
            }
        }

        // Verify tabs are present
        composeTestRule.onNodeWithText("Params").assertIsDisplayed()
        composeTestRule.onNodeWithText("Headers").assertIsDisplayed()

        // Verify add button is present for Params (default selected)
        composeTestRule.onNodeWithText("Add Param").assertIsDisplayed()
    }

    @Test
    fun requestEditor_canAddAndEditFields() {
        var params = emptyList<RequestField>()
        
        composeTestRule.setContent {
            ReqLiteTheme {
                RequestEditor(
                    queryParams = params,
                    headers = emptyList(),
                    body = RequestBody.NoBody,
                    authConfiguration = AuthConfiguration.None,
                    onQueryParamsChange = { params = it },
                    onHeadersChange = {},
                    onBodyChange = {},
                    onAuthChange = {}
                )
            }
        }

        // Click Add
        composeTestRule.onNodeWithText("Add Param").performClick()
        
        // Key and Value placeholders should appear
        composeTestRule.onNodeWithText("Key").assertIsDisplayed()
        composeTestRule.onNodeWithText("Value").assertIsDisplayed()
        
        // We should have 1 item now
        assertEquals(1, params.size)
    }

    @Test
    fun requestEditor_canSwitchTabsAndAddHeader() {
        var headers = emptyList<RequestField>()
        
        composeTestRule.setContent {
            ReqLiteTheme {
                RequestEditor(
                    queryParams = emptyList(),
                    headers = headers,
                    body = RequestBody.NoBody,
                    authConfiguration = AuthConfiguration.None,
                    onQueryParamsChange = {},
                    onHeadersChange = { headers = it },
                    onBodyChange = {},
                    onAuthChange = {}
                )
            }
        }

        // Switch to Headers tab
        composeTestRule.onNodeWithText("Headers").performClick()
        
        // Click Add Header
        composeTestRule.onNodeWithText("Add Header").performClick()
        
        // We should have 1 header now
        assertEquals(1, headers.size)
    }
}
