package io.trippilot.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class ReadinessAccessibilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun checklistRowAnnouncesGroupReasonAndCompletionState() {
        val title = "준비 접근성 ${System.currentTimeMillis()}"
        composeRule.onNodeWithText("새 여행 만들기").performClick()
        composeRule.onNodeWithTag("trip_title_input").performTextInput(title)
        composeRule.onNodeWithTag("trip_destination_input").performTextInput("Seoul")
        composeRule.onNodeWithTag("confirm_trip").performClick()
        val start = LocalDate.now()
        composeRule.onNodeWithContentDescription("$title, Seoul, ${start}부터 ${start.plusDays(2)}").performClick()
        composeRule.onNodeWithTag("trip_area_prepare").performClick()
        composeRule.onNodeWithTag("checklist_group_documents_entry").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "일정과 예약 확인, 서류 · 입국, 출발 전 시간·확인번호를 직접 대조합니다., 기본, 미완료",
        ).performScrollTo().assertIsDisplayed()
    }
}
