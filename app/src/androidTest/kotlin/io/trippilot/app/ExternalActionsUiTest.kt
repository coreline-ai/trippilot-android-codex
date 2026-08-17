package io.trippilot.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class ExternalActionsUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun externalActionsStayInConfirmationUiUntilTheUserApproves() {
        val title = "외부 실행 ${System.currentTimeMillis()}"
        composeRule.onNodeWithText("새 여행 만들기").performClick()
        composeRule.onNodeWithTag("trip_title_input").performTextInput(title)
        composeRule.onNodeWithTag("trip_destination_input").performTextInput("Seoul")
        composeRule.onNodeWithTag("confirm_trip").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(title).performClick()
        composeRule.onNodeWithTag("trip_area_help").performClick()
        composeRule.onNodeWithTag("trip_subpage_help_external").performClick()
        composeRule.onNodeWithTag("external_actions_section").assertIsDisplayed()

        composeRule.onNodeWithTag("backup_export_review").performScrollTo().performClick()
        composeRule.onNodeWithText("로컬 백업 내보내기").assertIsDisplayed()
        composeRule.onNodeWithText("취소").performClick()
        composeRule.onNodeWithTag("reminder_review").performScrollTo().performClick()
        composeRule.onNodeWithText("여행 준비 알림 켜기").assertIsDisplayed()
        composeRule.onNodeWithText("취소").performClick()
    }
}
