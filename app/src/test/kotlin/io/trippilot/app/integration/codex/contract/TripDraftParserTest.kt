package io.trippilot.app.integration.codex.contract

import org.junit.Assert.assertTrue
import org.junit.Test

class TripDraftParserTest {
    private val start = "2026-10-01"
    private val end = "2026-10-03"

    @Test
    fun `valid plan and weather contracts parse`() {
        val plan = """{"schema":"trippilot.trip-plan-draft","version":1,"kind":"TRIP_PLAN","title":"도쿄 여행","destination":"도쿄","startDate":"$start","endDate":"$end","days":[{"date":"$start","items":[{"id":"walk","title":"산책","startMinute":600,"location":"시부야","notes":""}]}],"reservations":[{"id":"hotel","type":"HOTEL","provider":"예시 호텔","confirmationCode":"ABC-1","dateTime":"2026-10-01T15:00","location":"시부야","sourceUrl":"https://example.com/hotel"}],"packingSuggestions":[{"id":"adapter","title":"어댑터","quantity":1,"reason":"충전"}],"preparationSuggestions":[{"id":"hours","title":"운영 시간 확인","reason":"변경 가능"}],"sources":[{"id":"walk-source","title":"공식 안내","url":"https://example.com/walk","relatedItemId":"walk"}],"assumptions":["직접 재확인 필요"]}"""
        val weather = """{"schema":"trippilot.weather-advisory-draft","version":1,"kind":"WEATHER_ADVISORY","destination":"도쿄","dates":["$start"],"summary":"참고용","advisories":["우산 확인"],"assumptions":["실제 예보 아님"]}"""

        assertTrue(TripDraftParser.parseTripPlan(plan, start, end) is ContractResult.Valid)
        assertTrue(TripDraftParser.parseWeatherAdvisory(weather, start, end) is ContractResult.Valid)
    }

    @Test
    fun `unknown required range url and item limits are rejected`() {
        val base = """{"schema":"trippilot.trip-plan-draft","version":1,"kind":"TRIP_PLAN","title":"도쿄 여행","destination":"도쿄","startDate":"$start","endDate":"$end","days":[],"reservations":[],"packingSuggestions":[],"preparationSuggestions":[],"sources":[],"assumptions":[]}"""
        assertTrue(TripDraftParser.parseTripPlan(base.dropLast(1) + ",\"unknown\":true}", start, end) is ContractResult.Invalid)
        assertTrue(TripDraftParser.parseTripPlan(base.replace("\"days\":[]", "\"days\":[{\"date\":\"2026-09-30\",\"items\":[]}]"), start, end) is ContractResult.Invalid)
        assertTrue(TripDraftParser.parseTripPlan(base.replace("\"sources\":[]", "\"sources\":[{\"id\":\"s\",\"title\":\"x\",\"url\":\"ftp://bad\",\"relatedItemId\":\"missing\"}]"), start, end) is ContractResult.Invalid)
        val tooMany = (1..61).joinToString(",") { "{\"id\":\"p$it\",\"title\":\"준비$it\",\"reason\":\"\"}" }
        assertTrue(TripDraftParser.parseTripPlan(base.replace("\"preparationSuggestions\":[]", "\"preparationSuggestions\":[$tooMany]"), start, end) is ContractResult.Invalid)
        assertTrue(TripDraftParser.parseTripPlan(base.replace("\"title\":\"도쿄 여행\",", ""), start, end) is ContractResult.Invalid)
    }

    @Test
    fun `approved selection after edit is validated before repository boundary`() {
        val selection = ApprovedDraftSelection(
            itinerary = listOf(ApprovedItineraryItem("walk", start, "산책", 600, "시부야", "")),
            reservations = emptyList(), preparation = emptyList(), packing = emptyList(), sources = emptyList(),
        )
        assertTrue(TripDraftParser.validateApprovedSelection(selection, start, end) is ContractResult.Valid)
        assertTrue(TripDraftParser.validateApprovedSelection(selection.copy(itinerary = listOf(selection.itinerary.single().copy(date = "2026-09-30"))), start, end) is ContractResult.Invalid)
    }
}
