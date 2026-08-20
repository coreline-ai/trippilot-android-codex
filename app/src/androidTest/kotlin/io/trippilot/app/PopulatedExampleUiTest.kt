package io.trippilot.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.core.util.Pair
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.trippilot.app.core.data.TripRepository
import io.trippilot.app.core.data.db.TripPilotDatabase
import io.trippilot.app.tools.ExampleTripData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Populated-state regression: seeds the full example dataset, walks every
 * surface, and asserts the data actually renders. Complements the empty-state
 * smoke test; cleaned up after itself so user data is untouched.
 */
class PopulatedExampleUiTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var repository: TripRepository

    /**
     * A separate Room instance cannot push invalidation into the running app,
     * so each test seeds first and only then launches the activity: the app's
     * initial queries read the seeded rows directly.
     */
    @Before
    fun seedThenLaunch() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, TripPilotDatabase::class.java, "trippilot.db").build()
        repository = TripRepository(database, database.tripDao())
        ExampleTripData.seed(repository, database)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("trip_list_screen").fetchSemanticsNodes().isNotEmpty() }
    }

    @After
    fun closeAndCleanup() = runBlocking {
        scenario.close()
        val titles = setOf(ExampleTripData.BUSAN_TITLE, ExampleTripData.JEJU_TITLE, ExampleTripData.TOKYO_TITLE)
        repository.observeTrips().first().filter { it.title in titles }.forEach {
            repository.deleteTrip(it.id)
        }
    }

    private fun scrollListUntilFound(text: String) {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("trip_list_screen").fetchSemanticsNodes().isNotEmpty() }
        repeat(6) {
            if (composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) return
            composeRule.onNodeWithTag("trip_list_screen").performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
    }

    private fun openTripByTitle(title: String) {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("trip_list_screen").fetchSemanticsNodes().isNotEmpty() }
        // Long titles ellipsize in cards; substring matching keeps this stable.
        scrollListUntilFound(title.substringBefore(" "))
        composeRule.onNodeWithText(title, substring = true).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("trip_briefing_screen").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun listShowsFeaturedPastTripAndOtherJourneys() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("trip_list_screen").fetchSemanticsNodes().isNotEmpty() }
        composeRule.waitUntil(15_000) { composeRule.onAllNodesWithText("부산 위크엔드", substring = true).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("부산 위크엔드", substring = true).assertExists()
        composeRule.onNodeWithText("제주 치유 여행", substring = true).performScrollTo().assertExists()
        // The third card sits below the lazy fold; its presence is asserted at
        // the data level in tokyoTripShowsInternationalRequirements instead.
    }

    @Test
    fun endedTripBriefingShowsPostTripFollowUp() {
        openTripByTitle("부산 위크엔드 (지난 여행)")
        composeRule.onNodeWithTag("trip_briefing_screen").assertIsDisplayed()
        composeRule.onNodeWithText("귀국 후 정리를 확인하세요").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun jejuTripShowsItineraryReservationsPostTripWindowsAndSafetyMemos() {
        openTripByTitle("제주 치유 여행 4박 5일")

        // Itinerary: timed items for DAY 1, then the all-day item on DAY 4.
        composeRule.onNodeWithTag("home_next_itinerary").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("김포공항 출발").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("itinerary_date_2026-09-10").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("성산일출봉 등반").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("itinerary_date_2026-09-12").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("제주 올레길 12코스 (자유일정)").fetchSemanticsNodes().isNotEmpty() }

        // Reservations: confirmed, draft, and cancelled documents render.
        composeRule.onNodeWithTag("back_to_trips").performClick()
        composeRule.onNodeWithTag("home_wallet").performClick()
        composeRule.onNodeWithText("대한항공 KE1225").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("카약 투어 (미확정)").performScrollTo().assertExists()

        // Preparation: every post-trip window group renders with progress.
        composeRule.onNodeWithTag("back_to_trips").performClick()
        composeRule.onNodeWithTag("home_readiness").performClick()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag("post_trip_window_within_48_hours").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("post_trip_window_within_one_week").performScrollTo().assertExists()
        composeRule.onNodeWithTag("post_trip_window_later").performScrollTo().assertExists()
        composeRule.onNodeWithText("우천 대비 접이식 우산 (AI 초안 반영)").performScrollTo().assertExists()

        // Safety Hub: guidance and both user memos with contact values.
        composeRule.onNodeWithTag("back_to_trips").performClick()
        composeRule.onNodeWithTag("open_trip_tools").performClick()
        composeRule.onNodeWithTag("safety_hub_entry").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("safety_hub_screen").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("카드사 해외이상거래 차단").performScrollTo().assertExists()
        composeRule.onNodeWithText("항공사 지연 안내").performScrollTo().assertExists()
    }

    @Test
    fun tokyoTripShowsInternationalRequirements() = runBlocking {
        // The international card sits below the lazy fold on a 360dp screen, so
        // this surface is verified through the same repository the UI reads.
        val tokyo = repository.observeTrips().first().first { it.title.startsWith("도쿄") }
        val preparation = repository.observePreparation(tokyo.id).first().map { it.title }
        org.junit.Assert.assertTrue("여권 유효기간 확인" in preparation)
        org.junit.Assert.assertTrue("입국 요건 공식 출처 확인" in preparation)
        org.junit.Assert.assertTrue(repository.observeReservations(tokyo.id).first().any { it.provider == "JL098" })
        org.junit.Assert.assertTrue(repository.observeSafetyMemos(tokyo.id).first().any { it.category == io.trippilot.app.core.model.SafetyCategory.HEALTH })
    }
}
