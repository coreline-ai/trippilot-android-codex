package io.trippilot.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun credentialFreeRuntimeScreenShowsAllUserSafetyBoundaries() {
        composeRule.onNodeWithTag("top_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("route_ribbon").assertIsDisplayed()
        composeRule.onNodeWithTag("runtime_status").assertIsDisplayed()
        composeRule.onNodeWithText("Codex에서 로그인 시작").performClick()
        composeRule.onNodeWithText("미리보기 로그인 완료 상태 보기").performClick()
        composeRule.onNodeWithText("연결됨").assertIsDisplayed()
        composeRule.onNodeWithText("미리보기 연결 해제").performClick()
        composeRule.onNodeWithText("Codex에서 로그인 시작").assertIsDisplayed()
    }
}
