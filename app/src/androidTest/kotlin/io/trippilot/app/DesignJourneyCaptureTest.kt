package io.trippilot.app

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.trippilot.app.feature.drafts.DraftUiState
import io.trippilot.app.feature.drafts.TripDraftViewModel
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.test.performTextClearance
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * A deterministic product-design audit, intentionally opt-in through the instrumentation argument.
 * It creates one local trip through the same Compose controls a user sees; no network, OAuth, or
 * external handoff is performed. The companion script exports the captured screens for review.
 */
class DesignJourneyCaptureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun capturesOneLocalTripAcrossEveryCurrentProductSurface() {
        assumeTrue(
            "Run only through scripts/run_design_journey_capture.sh",
            InstrumentationRegistry.getArguments().getString("captureDesignJourney") == "true" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
        )

        val tripTitle = "도쿄 가을 기록"
        val destination = "Tokyo"
        val start = "2026-11-01"
        val end = "2026-11-03"

        composeRule.onNodeWithText("새 여행 만들기").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("trip_title_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("trip_title_input").performTextInput(tripTitle)
        composeRule.onNodeWithTag("trip_destination_input").performTextInput(destination)
        composeRule.onNodeWithTag("trip_start_input").performTextClearance()
        composeRule.onNodeWithTag("trip_start_input").performTextInput(start)
        composeRule.onNodeWithTag("trip_end_input").performTextClearance()
        composeRule.onNodeWithTag("trip_end_input").performTextInput(end)
        composeRule.onNodeWithTag("confirm_trip").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithContentDescription("$tripTitle, $destination, ${start}부터 $end").fetchSemanticsNodes().isNotEmpty()
        }
        capture("01-list-featured.png", composeRule.onNodeWithTag("trip_list_screen"))

        composeRule.onNodeWithContentDescription("$tripTitle, $destination, ${start}부터 ${end}").performClick()
        capture("02-summary.png", composeRule.onNodeWithTag("trip_detail_screen"))

        composeRule.onNodeWithTag("home_next_itinerary").performClick()
        composeRule.onNodeWithTag("add_itinerary").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("itinerary_title_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("itinerary_title_input").performTextInput("아사쿠사 산책")
        composeRule.onNodeWithText("추가").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("아사쿠사 산책").fetchSemanticsNodes().isNotEmpty() }
        capture("03-itinerary.png", composeRule.onNodeWithTag("trip_detail_screen"))

        composeRule.onNodeWithTag("back_to_trips").performClick()
        composeRule.onNodeWithTag("home_readiness").performClick()
        composeRule.onNodeWithTag("add_preparation").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("simple_text_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("simple_text_input").performTextInput("여권 유효기간 확인")
        composeRule.onNodeWithText("추가").performClick()
        composeRule.onNodeWithTag("add_packing").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("packing_title_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("packing_title_input").performTextInput("보조 배터리")
        composeRule.onNodeWithText("추가").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("보조 배터리").fetchSemanticsNodes().isNotEmpty() }
        capture("04-readiness.png", composeRule.onNodeWithTag("trip_detail_screen"))

        composeRule.onNodeWithTag("back_to_trips").performClick()
        composeRule.onNodeWithTag("home_wallet").performClick()
        composeRule.onNodeWithTag("add_reservation").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("reservation_provider_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("reservation_provider_input").performTextInput("Field Hotel Tokyo")
        composeRule.onNodeWithTag("reservation_code_input").performTextInput("TP-TOKYO-01")
        composeRule.onNodeWithText("저장").performClick()
        capture("05-reservations.png", composeRule.onNodeWithTag("trip_detail_screen"))

        composeRule.onNodeWithTag("back_to_trips").performClick()
        composeRule.onNodeWithTag("home_next_itinerary").performClick()
        composeRule.onNodeWithTag("add_source_itinerary").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("source_title_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("source_title_input").performTextInput("공식 관광 안내")
        composeRule.onNodeWithTag("source_url_input").performTextInput("https://example.com/tokyo-guide")
        composeRule.onNodeWithText("연결").performClick()
        composeRule.onNodeWithTag("back_to_trips").performClick()
        composeRule.onNodeWithTag("home_wallet").performClick()
        capture("06-sources.png", composeRule.onNodeWithTag("trip_detail_screen"))

        composeRule.onNodeWithTag("back_to_trips").performClick()
        composeRule.onNodeWithTag("open_trip_tools").performClick()
        composeRule.onNodeWithTag("open_drafts").performClick()
        composeRule.onNodeWithTag("create_fake_draft").performScrollTo().performClick()
        composeRule.waitUntil(12_000) {
            var isReview = false
            composeRule.activityRule.scenario.onActivity { activity ->
                isReview = ViewModelProvider(activity)[TripDraftViewModel::class.java].state.value is DraftUiState.Review
            }
            isReview
        }
        // Keep the detail header and Help/AI context in the audit artifact. Capturing
        // only the inner review form made it impossible to judge screen hierarchy.
        capture("07-draft-review.png", composeRule.onNodeWithTag("trip_detail_screen"))

        composeRule.onNodeWithTag("back_to_trips").performClick()
        composeRule.onNodeWithTag("open_external_actions").performClick()
        composeRule.onNodeWithTag("backup_export_review").performScrollTo().performClick()
        capture("08-external-confirmation.png", composeRule.onNodeWithTag("approval_sheet"))
    }

    private fun capture(fileName: String, node: SemanticsNodeInteraction) {
        composeRule.waitForIdle()
        val directory = File(instrumentation.targetContext.filesDir, "design-journey")
        check(directory.mkdirs() || directory.isDirectory) { "Cannot create design capture directory" }
        FileOutputStream(File(directory, fileName)).use { output ->
            check(node.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Cannot write $fileName"
            }
        }
    }
}
