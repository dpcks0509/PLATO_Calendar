package pusan.university.plato_calendar.data.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalScheduleResponse(
    val error: Boolean,
    val data: PersonalScheduleData?,
)

@Serializable
data class PersonalScheduleData(
    @SerialName("event")
    val event: PersonalScheduleEvent,
)

@Serializable
data class PersonalScheduleEvent(
    val id: Long,
    val name: String,
    val description: String?,
    @SerialName("timestart")
    val timeStart: Long,
    @SerialName("timeduration")
    val timeDuration: Long,
)
