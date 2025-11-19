package pusan.university.plato_calendar.data.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeletePersonalScheduleRequest(
    val index: Int = 0,
    @SerialName("methodname")
    val methodName: String = "core_calendar_delete_calendar_events",
    val args: DeletePersonalScheduleArgs,
)

@Serializable
data class DeletePersonalScheduleArgs(
    val events: List<DeletePersonalScheduleEvent>,
)

@Serializable
data class DeletePersonalScheduleEvent(
    @SerialName("eventid")
    val eventId: Long,
    val repeat: Boolean,
)
