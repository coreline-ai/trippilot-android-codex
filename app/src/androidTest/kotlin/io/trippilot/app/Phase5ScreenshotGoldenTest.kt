package io.trippilot.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * Pixel regression guard for the four stable Phase 5 journeys.
 *
 * Goldens are intentionally captured only with an explicit instrumentation argument, never at
 * normal test time. The companion script clears the emulator app data and fixes light mode/font
 * scale before both capture and comparison, so local user data and device preferences cannot
 * become an accidental visual baseline.
 */
class Phase5ScreenshotGoldenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val updateGoldens = InstrumentationRegistry.getArguments().getString("updateGoldens") == "true"

    @Test
    fun fourCoreScreensMatchApprovedGoldens() {
        // captureToImage cannot capture dialog content below API 28. The dedicated golden runner
        // refuses those devices; keeping the shared API 26 UI suite skipped avoids a false red.
        assumeTrue(
            "Phase 5 golden capture requires API 28+; use run_phase5_screenshot_golden.sh",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
        )
        captureOrAssert("01-trip-list-empty.png", composeRule.onNodeWithTag("trip_list_screen"))

        createStableTrip()
        captureOrAssert("02-trip-summary.png", composeRule.onNodeWithTag("trip_detail_screen"))

        composeRule.onNodeWithTag("trip_area_journey").performClick()
        composeRule.onNodeWithTag("trip_subpage_journey_itinerary").performClick()
        composeRule.onNodeWithTag("add_itinerary").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("itinerary_title_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("itinerary_title_input").performTextInput("서울역 도착")
        composeRule.onNodeWithText("추가").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("서울역 도착").fetchSemanticsNodes().isNotEmpty() }
        captureOrAssert("05-itinerary.png", composeRule.onNodeWithTag("trip_detail_screen"))

        composeRule.onNodeWithTag("trip_area_prepare").performClick()
        composeRule.onNodeWithTag("readiness_screen").assertIsDisplayed()
        captureOrAssert("06-readiness.png", composeRule.onNodeWithTag("readiness_screen"))

        composeRule.onNodeWithTag("trip_area_help").performClick()
        composeRule.onNodeWithTag("trip_subpage_help_drafts").performClick()
        composeRule.onNodeWithTag("draft_planner_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("create_fake_draft").performScrollTo().performClick()
        composeRule.waitUntil(12_000) {
            composeRule.onAllNodesWithTag("draft_review_screen").fetchSemanticsNodes().isNotEmpty()
        }
        captureOrAssert("03-draft-review.png", composeRule.onNodeWithTag("draft_planner_screen"))

        composeRule.onNodeWithTag("trip_area_help").performClick()
        composeRule.onNodeWithTag("trip_subpage_help_external").performClick()
        composeRule.onNodeWithTag("external_actions_section").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_export_review").performScrollTo().performClick()
        composeRule.onNodeWithText("로컬 백업 내보내기").assertIsDisplayed()
        captureOrAssert("04-external-confirmation.png", composeRule.onNodeWithTag("approval_sheet"))
    }

    private fun createStableTrip() {
        composeRule.onNodeWithText("새 여행 만들기").performClick()
        composeRule.onNodeWithTag("trip_title_input").performTextInput("Golden Seoul")
        composeRule.onNodeWithTag("trip_destination_input").performTextInput("Seoul")
        composeRule.onNodeWithTag("trip_start_input").performTextClearance()
        composeRule.onNodeWithTag("trip_start_input").performTextInput("2026-10-10")
        composeRule.onNodeWithTag("trip_end_input").performTextClearance()
        composeRule.onNodeWithTag("trip_end_input").performTextInput("2026-10-12")
        closeSoftKeyboard()
        composeRule.onNodeWithTag("confirm_trip").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithContentDescription("Golden Seoul, Seoul, 2026-10-10부터 2026-10-12").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Golden Seoul, Seoul, 2026-10-10부터 2026-10-12").performClick()
        composeRule.onNodeWithTag("trip_detail_screen").assertIsDisplayed()
    }

    private fun captureOrAssert(fileName: String, node: SemanticsNodeInteraction) {
        composeRule.waitForIdle()
        val actual = node.captureToImage().asAndroidBitmap()
        if (updateGoldens) {
            writeCandidate(fileName, actual)
            return
        }

        val expected = instrumentation.context.assets.open("screenshot-goldens/$fileName").use(BitmapFactory::decodeStream)
        requireNotNull(expected) { "Golden decode failed: $fileName" }
        assertEquals("Golden width changed: $fileName", expected.width, actual.width)
        assertEquals("Golden height changed: $fileName", expected.height, actual.height)

        val diff = pixelDifference(expected, actual)
        assertTrue(
            "Visual regression in $fileName: changed=${"%.3f".format(diff.changedPercent)}%, averageChannel=${"%.3f".format(diff.averageChannelDelta)}. " +
                "Run scripts/run_phase5_screenshot_golden.sh update only after design review.",
            diff.changedPercent <= MAX_CHANGED_PERCENT && diff.averageChannelDelta <= MAX_AVERAGE_CHANNEL_DELTA,
        )
    }

    private fun writeCandidate(fileName: String, bitmap: Bitmap) {
        val directory = File(instrumentation.targetContext.filesDir, "screenshot-goldens")
        check(directory.mkdirs() || directory.isDirectory) { "Cannot create candidate directory" }
        FileOutputStream(File(directory, fileName)).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Cannot write $fileName" }
        }
    }

    private fun pixelDifference(expected: Bitmap, actual: Bitmap): PixelDifference {
        val count = expected.width * expected.height
        val expectedPixels = IntArray(count)
        val actualPixels = IntArray(count)
        expected.getPixels(expectedPixels, 0, expected.width, 0, 0, expected.width, expected.height)
        actual.getPixels(actualPixels, 0, actual.width, 0, 0, actual.width, actual.height)
        var changed = 0
        var channelDelta = 0L
        expectedPixels.indices.forEach { index ->
            val before = expectedPixels[index]
            val after = actualPixels[index]
            val delta = kotlin.math.abs((before shr 16 and 0xff) - (after shr 16 and 0xff)) +
                kotlin.math.abs((before shr 8 and 0xff) - (after shr 8 and 0xff)) +
                kotlin.math.abs((before and 0xff) - (after and 0xff))
            if (delta > PIXEL_CHANGE_THRESHOLD) changed += 1
            channelDelta += delta
        }
        return PixelDifference(
            changedPercent = changed * 100.0 / count,
            averageChannelDelta = channelDelta.toDouble() / (count * 3),
        )
    }

    private data class PixelDifference(val changedPercent: Double, val averageChannelDelta: Double)

    private companion object {
        const val PIXEL_CHANGE_THRESHOLD = 12
        const val MAX_CHANGED_PERCENT = 0.35
        const val MAX_AVERAGE_CHANNEL_DELTA = 0.20
    }
}
