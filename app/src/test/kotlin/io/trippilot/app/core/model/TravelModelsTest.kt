package io.trippilot.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TravelModelsTest {
    @Test
    fun `trip dates reject inverse range and accept one day trip`() {
        val oneDay = TripInput("서울", "Seoul", "2026-10-01", "2026-10-01", "Asia/Seoul", TravelScope.DOMESTIC)
        assertEquals(ValidationResult.Valid, TravelValidators.trip(oneDay))

        val inverse = oneDay.copy(startDate = "2026-10-02", endDate = "2026-10-01")
        assertTrue(TravelValidators.trip(inverse) is ValidationResult.Invalid)
    }

    @Test
    fun `itinerary accepts boundaries and blocks date outside trip`() {
        assertEquals(ValidationResult.Valid, TravelValidators.itinerary("2026-10-01", 0, null, "2026-10-01", "2026-10-03", "출발"))
        assertEquals(ValidationResult.Valid, TravelValidators.itinerary("2026-10-03", 1439, null, "2026-10-01", "2026-10-03", "귀가"))
        assertTrue(TravelValidators.itinerary("2026-10-04", null, null, "2026-10-01", "2026-10-03", "범위 밖") is ValidationResult.Invalid)
    }

    @Test
    fun `scope templates do not infer a country from destination text`() {
        // AUTO required pack grew to 5 with EMERGENCY_CONTACT_COPY (gap doc §1).
        assertEquals(TravelScopeTemplates.items(TravelScope.AUTO).size, 5)
        assertTrue(TravelScopeTemplates.items(TravelScope.DOMESTIC).any { it.title == "신분증" })
        assertTrue(TravelScopeTemplates.items(TravelScope.INTERNATIONAL).any { it.title == "여권 유효기간 확인" })
    }

    @Test
    fun `only http and https urls are accepted`() {
        assertEquals(ValidationResult.Valid, TravelValidators.url("https://example.com/reservation"))
        assertTrue(TravelValidators.url("file:///private.txt") is ValidationResult.Invalid)
        assertTrue(TravelValidators.url("javascript:alert(1)") is ValidationResult.Invalid)
    }

    @Test
    fun `completion policy handles empty midway complete and skipped consistently`() {
        assertEquals(0, CompletionPolicy.preparationPercent(emptyList()))
        assertEquals(50, CompletionPolicy.preparationPercent(listOf(PreparationStatus.DONE, PreparationStatus.TODO)))
        assertEquals(100, CompletionPolicy.preparationPercent(listOf(PreparationStatus.DONE, PreparationStatus.SKIPPED)))
        assertEquals(0, CompletionPolicy.packingPercent(listOf(false, false)))
        assertEquals(100, CompletionPolicy.packingPercent(listOf(true, true)))
    }
}
