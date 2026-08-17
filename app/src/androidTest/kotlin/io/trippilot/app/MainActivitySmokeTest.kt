package io.trippilot.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun offlineUserCanCreateTripAndAddItineraryWithoutLogin() {
        val tripTitle = "서울 ${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("trip_list_screen").assertIsDisplayed()
        composeRule.onNodeWithText("새 여행 만들기").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("trip_title_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("trip_title_input").performTextInput(tripTitle)
        composeRule.onNodeWithTag("trip_destination_input").performTextInput("Seoul")
        composeRule.onNodeWithTag("confirm_trip").assertIsEnabled()
        composeRule.onNodeWithTag("confirm_trip").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("trip_form_sheet").fetchSemanticsNodes().isEmpty() }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText(tripTitle).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(tripTitle).assertIsDisplayed()
        val start = LocalDate.now()
        composeRule.onNodeWithContentDescription("$tripTitle, Seoul, ${start}부터 ${start.plusDays(2)}").performClick()
        composeRule.onNodeWithTag("journey_stage_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("trip_area_journey").performClick()
        composeRule.onNodeWithTag("trip_subpage_journey_itinerary").performClick()
        composeRule.onNodeWithTag("add_itinerary").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("itinerary_title_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("itinerary_title_input").performTextInput("북촌 산책")
        composeRule.onNodeWithText("추가").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("북촌 산책").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("북촌 산책").assertIsDisplayed()
        composeRule.onNodeWithTag("trip_area_prepare").performClick()
        composeRule.onNodeWithTag("add_preparation").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("simple_text_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("simple_text_input").performTextInput("여권 확인")
        composeRule.onNodeWithText("추가").performClick()
        // `준비` 화면은 preparation과 packing을 같은 scroll surface에 쌓는다.
        // 360dp API 26 기기에서는 새 항목이 fold 아래에 생길 수 있으므로
        // 표시 전 해당 행까지 명시적으로 스크롤한다.
        composeRule.onNodeWithText("여권 확인").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("add_packing").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("packing_title_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("packing_title_input").performTextInput("카메라")
        composeRule.onNodeWithText("추가").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("카메라").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodesWithText("카메라").assertCountEquals(1)
        composeRule.onNodeWithTag("trip_area_storage").performClick()
        composeRule.onNodeWithTag("trip_subpage_storage_reservations").performClick()
        composeRule.onNodeWithTag("add_reservation").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("reservation_provider_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("reservation_provider_input").performTextInput("Trip Hotel")
        composeRule.onNodeWithTag("reservation_code_input").performTextInput("TP-001")
        composeRule.onNodeWithText("저장").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("Trip Hotel").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodesWithText("Trip Hotel").assertCountEquals(1)
        composeRule.onNodeWithTag("trip_area_journey").performClick()
        composeRule.onNodeWithTag("trip_subpage_journey_itinerary").performClick()
        composeRule.onNodeWithTag("add_source_itinerary").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("source_title_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("source_title_input").performTextInput("공식 안내")
        composeRule.onNodeWithTag("source_url_input").performTextInput("https://example.com/guide")
        composeRule.onNodeWithText("연결").performClick()
        composeRule.onNodeWithTag("trip_subpage_journey_summary").performClick()
        composeRule.onNodeWithTag("delete_trip").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("confirm_action").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("confirm_action").performClick()
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("trip_list_screen").assertIsDisplayed()
        composeRule.onAllNodesWithText(tripTitle).assertCountEquals(0)
    }
}
