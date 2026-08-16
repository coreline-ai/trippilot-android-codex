package io.trippilot.app.core.data.db

import androidx.room.TypeConverter
import io.trippilot.app.core.model.CalendarActionStatus
import io.trippilot.app.core.model.ItemOrigin
import io.trippilot.app.core.model.PreparationStatus
import io.trippilot.app.core.model.RecheckResult
import io.trippilot.app.core.model.ReservationStatus
import io.trippilot.app.core.model.SourceOwnerType
import io.trippilot.app.core.model.TravelScope
import io.trippilot.app.core.model.TripStatus

class TripConverters {
    @TypeConverter fun travelScopeToString(value: TravelScope): String = value.name
    @TypeConverter fun stringToTravelScope(value: String): TravelScope = TravelScope.valueOf(value)
    @TypeConverter fun tripStatusToString(value: TripStatus): String = value.name
    @TypeConverter fun stringToTripStatus(value: String): TripStatus = TripStatus.valueOf(value)
    @TypeConverter fun preparationStatusToString(value: PreparationStatus): String = value.name
    @TypeConverter fun stringToPreparationStatus(value: String): PreparationStatus = PreparationStatus.valueOf(value)
    @TypeConverter fun originToString(value: ItemOrigin): String = value.name
    @TypeConverter fun stringToOrigin(value: String): ItemOrigin = ItemOrigin.valueOf(value)
    @TypeConverter fun reservationStatusToString(value: ReservationStatus): String = value.name
    @TypeConverter fun stringToReservationStatus(value: String): ReservationStatus = ReservationStatus.valueOf(value)
    @TypeConverter fun ownerTypeToString(value: SourceOwnerType): String = value.name
    @TypeConverter fun stringToOwnerType(value: String): SourceOwnerType = SourceOwnerType.valueOf(value)
    @TypeConverter fun recheckResultToString(value: RecheckResult): String = value.name
    @TypeConverter fun stringToRecheckResult(value: String): RecheckResult = RecheckResult.valueOf(value)
    @TypeConverter fun calendarStatusToString(value: CalendarActionStatus): String = value.name
    @TypeConverter fun stringToCalendarStatus(value: String): CalendarActionStatus = CalendarActionStatus.valueOf(value)
}
