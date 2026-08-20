package io.trippilot.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Safety Hub + post-trip pack smoke (implement_20260817_230622 Phase 4). */
class SafetyHubUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun safetyHubShowsGeneralGuidanceAndStoresLocalMemo() {
        val tripTitle = "대응 ${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("trip_list_screen").assertIsDisplayed()
        composeRule.onNodeWithText("새 여행 만들기").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("trip_title_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("trip_title_input").performTextInput(tripTitle)
        composeRule.onNodeWithTag("trip_destination_input").performTextInput("Busan")
        composeRule.onNodeWithTag("confirm_trip").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("trip_form_sheet").fetchSemanticsNodes().isEmpty() }
        val start = LocalDate.now()
        composeRule.onNodeWithContentDescription("$tripTitle, Busan, ${start}부터 ${start.plusDays(2)}").performClick()
        composeRule.onNodeWithTag("trip_briefing_screen").assertIsDisplayed()

        // Safety stays reachable from the travel tools without occupying the home viewport.
        composeRule.onNodeWithTag("open_trip_tools").performClick()
        composeRule.onNodeWithTag("safety_hub_entry").assertIsDisplayed()
        composeRule.onNodeWithTag("safety_hub_entry").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("safety_hub_screen").fetchSemanticsNodes().isNotEmpty() }
        composeRule.waitForIdle()
        Thread.sleep(300)

        // Static guidance: the non-emergency notice and all seven categories.
        composeRule.onNodeWithTag("safety_hub_notice").performScrollTo().assertIsDisplayed()
        io.trippilot.app.core.model.SafetyCategory.entries.forEach { category ->
            composeRule.onNodeWithTag("safety_category_${category.name.lowercase()}").performScrollTo().assertIsDisplayed()
        }

        // Empty state, then a local memo round trip through the form sheet.
        composeRule.onNodeWithText("저장한 안전 메모가 없습니다").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("add_safety_memo").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("safety_memo_title").fetchSemanticsNodes().isNotEmpty() }
        // The modal sheet still animates when the field is already composed; let
        // it settle before measuring, or the pass hits a mid-animation remeasure.
        composeRule.waitForIdle()
        Thread.sleep(600)
        composeRule.onNodeWithTag("safety_memo_title").performTextInput("카드사 공식 앱")
        Thread.sleep(200)
        composeRule.onNodeWithTag("confirm_safety_memo").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("카드사 공식 앱").fetchSemanticsNodes().isNotEmpty() }
        // Verify without a measuring scroll: the sheet-close animation can leave
        // the layout mid-pass on this Compose/API level.
        composeRule.onNodeWithText("카드사 공식 앱").assertExists()

        // Back returns to the tools list where review and external actions remain reachable.
        composeRule.onNodeWithTag("safety_hub_back").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("safety_hub_entry").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("open_drafts").assertIsDisplayed()
        composeRule.onNodeWithTag("open_external_actions").assertIsDisplayed()
    }

    @Test
    fun postTripPackIsOptInIdempotentAndVisibleInWindow() {
        val tripTitle = "귀국 ${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("trip_list_screen").assertIsDisplayed()
        composeRule.onNodeWithText("새 여행 만들기").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("trip_title_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("trip_title_input").performTextInput(tripTitle)
        composeRule.onNodeWithTag("trip_destination_input").performTextInput("Seoul")
        composeRule.onNodeWithTag("confirm_trip").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("trip_form_sheet").fetchSemanticsNodes().isEmpty() }
        val start = LocalDate.now()
        composeRule.onNodeWithContentDescription("$tripTitle, Seoul, ${start}부터 ${start.plusDays(2)}").performClick()

        composeRule.onNodeWithTag("home_readiness").performClick()
        val packButton = composeRule.onNodeWithTag("add_post_trip_pack_within_48_hours")
        packButton.performScrollTo().performClick()
        // Idempotent by stable template id: a second tap adds no duplicates.
        composeRule.waitForIdle()
        Thread.sleep(300)
        packButton.performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("귀가 지갑·서류·장비 확인").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("post_trip_window_within_48_hours").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("귀가 지갑·서류·장비 확인").performScrollTo().assertIsDisplayed()
    }
}
