package io.trippilot.app.core.model

/** SKIPPED preparation items are excluded consistently from the completion denominator. */
object CompletionPolicy {
    fun preparationPercent(statuses: List<PreparationStatus>): Int {
        val applicable = statuses.filter { it != PreparationStatus.SKIPPED }
        return if (applicable.isEmpty()) 0 else applicable.count { it == PreparationStatus.DONE } * 100 / applicable.size
    }

    fun packingPercent(packed: List<Boolean>): Int =
        if (packed.isEmpty()) 0 else packed.count { it } * 100 / packed.size
}
