package io.trippilot.app.core.data

import io.trippilot.app.core.data.db.TripEntity
import io.trippilot.app.core.model.TravelScope
import io.trippilot.app.core.model.TripInput
import io.trippilot.app.core.model.TravelValidators
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class TripBackupDocument(
    val schema: String = "trippilot.trip-backup",
    val version: Int = 1,
    val trips: List<TripBackupTrip>,
)

@Serializable
data class TripBackupTrip(
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val timezone: String,
    val scope: String,
    val notes: String,
    val itinerary: List<TripBackupItinerary> = emptyList(),
    val preparation: List<TripBackupPreparation> = emptyList(),
    val packing: List<TripBackupPacking> = emptyList(),
    val reservations: List<TripBackupReservation> = emptyList(),
    val sources: List<TripBackupSource> = emptyList(),
)

@Serializable
data class TripBackupItinerary(
    val date: String,
    val startMinute: Int? = null,
    val title: String,
    val location: String = "",
    val notes: String = "",
)

@Serializable
data class TripBackupPreparation(val title: String, val status: String, val origin: String)

@Serializable
data class TripBackupPacking(val title: String, val quantity: Int, val isPacked: Boolean, val origin: String)

@Serializable
data class TripBackupReservation(
    val type: String,
    val provider: String,
    val confirmationCode: String,
    val dateTime: String? = null,
    val location: String = "",
    val url: String? = null,
    val status: String,
    val notes: String = "",
)

/** ownerIndex is a position in the same backup trip's itinerary/reservations list, never a database ID. */
@Serializable
data class TripBackupSource(
    val ownerType: String,
    val ownerIndex: Int,
    val url: String,
    val title: String,
)

/**
 * Narrow, public-field-only compatibility reader. It intentionally contains no
 * database, account, chat, secret, or OAuth field and creates no link to
 * another application's storage. The accepted fields are documented with the
 * backup contract; restored trips are always new TripPilot records.
 */
@Serializable
private data class LegacyPublicTripBackupDocument(
    val schema: String,
    val version: Int,
    val trips: List<TripBackupTrip>,
)

object TripBackupCodec {
    const val MAX_BYTES = 2 * 1024 * 1024
    const val MAX_TRIPS = 100
    const val MAX_ITEMS_PER_TRIP = 500
    const val SCHEMA = "trippilot.trip-backup"
    private const val LEGACY_SCHEMA = "openminis.trip-backup"
    private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true }

    fun encode(trips: List<TripEntity>): String = encode(
        TripBackupDocument(
            trips = trips.map {
                TripBackupTrip(
                    title = it.title, destination = it.destination, startDate = it.startDate,
                    endDate = it.endDate, timezone = it.timezone, scope = it.scope.name, notes = it.notes,
                )
            },
        ),
    )

    fun encode(document: TripBackupDocument): String {
        validate(document)
        return json.encodeToString(document)
    }

    fun decode(payload: String): Result<TripBackupDocument> = runCatching {
        require(payload.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "백업 파일이 2MB 제한을 초과합니다." }
        val document = json.decodeFromString<TripBackupDocument>(payload)
        validate(document)
    }

    /** The one explicitly documented legacy public schema is mapped only to v1 trip fields. */
    fun decodeForRestore(payload: String): Result<TripBackupDocument> = runCatching {
        require(payload.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "백업 파일이 2MB 제한을 초과합니다." }
        val schema = json.parseToJsonElement(payload).jsonObject["schema"]?.jsonPrimitive?.content
        val document = if (schema == LEGACY_SCHEMA) {
            val legacy = json.decodeFromString<LegacyPublicTripBackupDocument>(payload)
            require(legacy.version == 1) { "지원하지 않는 legacy 백업 버전입니다." }
            TripBackupDocument(trips = legacy.trips)
        } else {
            json.decodeFromString<TripBackupDocument>(payload)
        }
        validate(document)
    }

    /** IDs are deliberately absent: callers must create independent records, never overwrite. */
    fun restoreInputs(document: TripBackupDocument): Result<List<TripInput>> = runCatching {
        validate(document)
        document.trips.map { trip ->
            val scope = runCatching { TravelScope.valueOf(trip.scope) }.getOrElse {
                throw IllegalArgumentException("지원하지 않는 여행 범위입니다.")
            }
            TripInput(trip.title, trip.destination, trip.startDate, trip.endDate, trip.timezone, scope, trip.notes)
                .also { input -> require(TravelValidators.trip(input).isValid()) { "백업 여행 값이 올바르지 않습니다." } }
        }
    }

    private fun validate(document: TripBackupDocument): TripBackupDocument {
        require(document.schema == SCHEMA) { "지원하지 않는 백업 schema입니다." }
        require(document.version == 1) { "지원하지 않는 백업 버전입니다." }
        require(document.trips.size <= MAX_TRIPS) { "여행 수 제한을 초과합니다." }
        document.trips.forEach {
            require(it.title.isNotBlank() && it.destination.isNotBlank()) { "백업 여행 제목과 목적지가 필요합니다." }
            require(it.startDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && it.endDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                "백업 날짜 형식이 올바르지 않습니다."
            }
            require(it.startDate <= it.endDate) { "백업 여행 기간이 올바르지 않습니다." }
            require(it.timezone.isNotBlank()) { "백업 시간대가 필요합니다." }
            require(it.scope in TravelScope.entries.map { scope -> scope.name }) { "백업 여행 범위가 올바르지 않습니다." }
            require(it.itinerary.size <= MAX_ITEMS_PER_TRIP && it.preparation.size <= MAX_ITEMS_PER_TRIP && it.packing.size <= MAX_ITEMS_PER_TRIP && it.reservations.size <= MAX_ITEMS_PER_TRIP && it.sources.size <= MAX_ITEMS_PER_TRIP) {
                "백업 항목 수 제한을 초과합니다."
            }
            it.itinerary.forEach { item ->
                require(TravelValidators.itinerary(item.date, item.startMinute, null, it.startDate, it.endDate, item.title).isValid()) { "백업 일정 값이 올바르지 않습니다." }
            }
            it.preparation.forEach { item ->
                require(item.title.isNotBlank() && item.status in io.trippilot.app.core.model.PreparationStatus.entries.map { status -> status.name } && item.origin in io.trippilot.app.core.model.ItemOrigin.entries.map { origin -> origin.name }) { "백업 준비 항목 값이 올바르지 않습니다." }
            }
            it.packing.forEach { item ->
                require(item.title.isNotBlank() && item.quantity >= 1 && item.origin in io.trippilot.app.core.model.ItemOrigin.entries.map { origin -> origin.name }) { "백업 짐 항목 값이 올바르지 않습니다." }
            }
            it.reservations.forEach { item ->
                require(item.provider.isNotBlank() && item.confirmationCode.isNotBlank() && item.status in io.trippilot.app.core.model.ReservationStatus.entries.map { status -> status.name }) { "백업 예약 값이 올바르지 않습니다." }
                require(item.url.isNullOrBlank() || TravelValidators.url(item.url).isValid()) { "백업 예약 URL이 올바르지 않습니다." }
            }
            it.sources.forEach { item ->
                val ownerLimit = if (item.ownerType == "ITINERARY") it.itinerary.size else if (item.ownerType == "RESERVATION") it.reservations.size else -1
                require(item.ownerIndex in 0 until ownerLimit && item.title.isNotBlank() && TravelValidators.url(item.url).isValid()) { "백업 출처 값이 올바르지 않습니다." }
            }
        }
        return document
    }
}

private fun io.trippilot.app.core.model.ValidationResult.isValid(): Boolean =
    this is io.trippilot.app.core.model.ValidationResult.Valid
