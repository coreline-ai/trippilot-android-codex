package io.trippilot.app

import androidx.compose.ui.test.assertIsDisplayed
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
        composeRule.onNodeWithTag("top_bar").assertIsDisplayed()
        composeRule.onNodeWithText("새 여행 만들기").performClick()
        composeRule.onNodeWithTag("trip_title_input").performTextInput(tripTitle)
        composeRule.onNodeWithTag("trip_destination_input").performTextInput("Seoul")
        composeRule.onNodeWithTag("confirm_trip").performClick()
        composeRule.onNodeWithText(tripTitle).assertIsDisplayed()
        val start = LocalDate.now()
        composeRule.onNodeWithContentDescription("$tripTitle, Seoul, ${start}부터 ${start.plusDays(2)}").performClick()
        composeRule.onNodeWithTag("route_ribbon").assertIsDisplayed()
        composeRule.onNodeWithTag("trip_section_itinerary").performClick()
        composeRule.onNodeWithTag("add_itinerary").performClick()
        composeRule.onNodeWithTag("itinerary_title_input").performTextInput("북촌 산책")
        composeRule.onNodeWithText("추가").performClick()
        composeRule.onNodeWithText("북촌 산책").assertIsDisplayed()
        composeRule.onNodeWithTag("trip_section_readiness").performClick()
        composeRule.onNodeWithTag("add_preparation").performClick()
        composeRule.onNodeWithTag("simple_text_input").performTextInput("여권 확인")
        composeRule.onNodeWithText("추가").performClick()
        composeRule.onNodeWithText("여권 확인").assertIsDisplayed()
        composeRule.onNodeWithTag("add_packing").performClick()
        composeRule.onNodeWithTag("packing_title_input").performTextInput("카메라")
        composeRule.onNodeWithText("추가").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("카메라").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodesWithText("카메라").assertCountEquals(1)
        composeRule.onNodeWithTag("trip_section_reservations").performClick()
        composeRule.onNodeWithTag("add_reservation").performClick()
        composeRule.onNodeWithTag("reservation_provider_input").performTextInput("Trip Hotel")
        composeRule.onNodeWithTag("reservation_code_input").performTextInput("TP-001")
        composeRule.onNodeWithText("저장").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("Trip Hotel").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodesWithText("Trip Hotel").assertCountEquals(1)
        composeRule.onNodeWithTag("trip_section_itinerary").performClick()
        composeRule.onNodeWithTag("add_source_itinerary").performClick()
        composeRule.onNodeWithTag("source_title_input").performTextInput("공식 안내")
        composeRule.onNodeWithTag("source_url_input").performTextInput("https://example.com/guide")
        composeRule.onNodeWithText("연결").performClick()
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("trip_detail_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("trip_section_itinerary").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("북촌 산책").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("북촌 산책").assertCountEquals(1)
        composeRule.onNodeWithTag("trip_section_summary").performClick()
        composeRule.onNodeWithTag("delete_trip").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("confirm_action").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("confirm_action").performClick()
        composeRule.onAllNodesWithText(tripTitle).assertCountEquals(0)
    }
}
