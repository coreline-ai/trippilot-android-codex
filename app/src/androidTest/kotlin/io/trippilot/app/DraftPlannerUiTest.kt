package io.trippilot.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import io.trippilot.app.feature.drafts.DraftUiState
import io.trippilot.app.feature.drafts.TripDraftViewModel
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class DraftPlannerUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun fakeDraftCanBeReviewedEditedExcludedAndPartiallyApplied() {
        val title = "초안 테스트 ${System.currentTimeMillis()}"
        val start = LocalDate.now()
        composeRule.onNodeWithText("새 여행 만들기").performClick()
        composeRule.onNodeWithTag("trip_title_input").performTextInput(title)
        composeRule.onNodeWithTag("trip_destination_input").performTextInput("Seoul")
        composeRule.onNodeWithTag("confirm_trip").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("$title, Seoul, ${start}부터 ${start.plusDays(2)}").performClick()
        composeRule.onNodeWithTag("trip_area_help").performClick()
        composeRule.onNodeWithTag("trip_subpage_help_drafts").performClick()
        composeRule.onNodeWithTag("draft_planner_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("create_fake_draft").performScrollTo().performClick()
        composeRule.waitUntil(12_000) {
            var isReview = false
            composeRule.activityRule.scenario.onActivity { activity ->
                isReview = ViewModelProvider(activity)[TripDraftViewModel::class.java].state.value is DraftUiState.Review
            }
            isReview
        }
        composeRule.onNodeWithTag("draft_selection_day1-arrival").performScrollTo().performClick() // Exclude the suggested itinerary.
        composeRule.onNodeWithTag("edit_draft_reservation-stay").performScrollTo().performClick()
        composeRule.onNodeWithTag("draft_reservation_provider_reservation-stay").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("draft_reservation_provider_reservation-stay").performTextInput("Edited Hotel")
        composeRule.onNodeWithTag("apply_selected_draft").performScrollTo().performClick()
        composeRule.waitUntil(12_000) {
            var isApplied = false
            composeRule.activityRule.scenario.onActivity { activity ->
                isApplied = ViewModelProvider(activity)[TripDraftViewModel::class.java].state.value is DraftUiState.Applied
            }
            isApplied
        }
        composeRule.onNodeWithText("완료").performScrollTo().performClick()
        composeRule.onNodeWithTag("trip_area_storage").performClick()
        composeRule.onNodeWithTag("trip_subpage_storage_reservations").performClick()
        composeRule.onNodeWithText("Edited Hotel").assertIsDisplayed()
        composeRule.onAllNodesWithText("도착 후 동네 산책").assertCountEquals(0)
    }
}
