package io.trippilot.app.core.design

import java.time.LocalDate

enum class JourneyStageState(val label: String) {
    ACTION_REQUIRED("확인 필요"),
    PLANNED("일정 있음"),
    COMPLETE("완료"),
    EMPTY("일정 없음"),
    UPCOMING("예정"),
}

data class JourneyStage(
    val id: String,
    val label: String,
    val detail: String,
    val state: JourneyStageState,
)

/**
 * Small, deterministic presentation model for the Trip Briefing stage selector.
 * It is intentionally local-only: it reads only existing trip dates, checklist
 * counts and itinerary counts, and never invents completed itinerary days.
 */
object JourneyStageCalculator {
    fun defaultSelectedId(startDate: String, endDate: String, today: LocalDate = LocalDate.now()): String = runCatching {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        when {
            today.isBefore(start) -> PRE_STAGE_ID
            today.isAfter(end) -> POST_STAGE_ID
            else -> today.toString()
        }
    }.getOrDefault(PRE_STAGE_ID)

    fun calculate(
        startDate: String,
        endDate: String,
        itineraryCountByDate: Map<String, Int>,
        readinessTotal: Int,
        readinessPending: Int,
    ): List<JourneyStage> {
        val dates = datesInRange(startDate, endDate)
        val preState = when {
            readinessTotal == 0 -> JourneyStageState.EMPTY
            readinessPending == 0 -> JourneyStageState.COMPLETE
            else -> JourneyStageState.ACTION_REQUIRED
        }
        return buildList {
            add(
                JourneyStage(
                    id = PRE_STAGE_ID,
                    label = "출발 전",
                    detail = when (preState) {
                        JourneyStageState.EMPTY -> "준비 항목 없음"
                        JourneyStageState.COMPLETE -> "준비 완료"
                        else -> "준비 ${readinessPending.coerceAtLeast(0)}개"
                    },
                    state = preState,
                ),
            )
            dates.forEachIndexed { index, date ->
                val count = itineraryCountByDate[date.toString()].orEmptyCount()
                add(
                    JourneyStage(
                        id = date.toString(),
                        label = "DAY ${index + 1}",
                        detail = if (count == 0) "일정 없음" else "일정 ${count}개",
                        state = if (count == 0) JourneyStageState.EMPTY else JourneyStageState.PLANNED,
                    ),
                )
            }
            add(JourneyStage(POST_STAGE_ID, "귀국 후", "선택 팩", JourneyStageState.UPCOMING))
        }
    }

    fun summary(stages: List<JourneyStage>, selectedStageId: String): String {
        val stage = stages.firstOrNull { it.id == selectedStageId } ?: stages.firstOrNull()
        return if (stage == null) "여행 단계 정보가 없습니다." else "${stage.label} · ${stage.detail} · ${stage.state.label}"
    }

    private fun datesInRange(startDate: String, endDate: String): List<LocalDate> = runCatching {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        generateSequence(start) { current -> current.plusDays(1).takeIf { !it.isAfter(end) } }.toList()
    }.getOrDefault(emptyList())

    private fun Int?.orEmptyCount(): Int = (this ?: 0).coerceAtLeast(0)

    const val PRE_STAGE_ID = "pre"
    const val POST_STAGE_ID = "post"
}
