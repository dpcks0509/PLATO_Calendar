package pusan.university.plato_calendar.data.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatePersonalScheduleRequest(
    val index: Int = 0,
    @SerialName("methodname")
    val methodName: String = "core_calendar_submit_create_update_form",
    val args: CreatePersonalScheduleArgs,
)

@Serializable
data class CreatePersonalScheduleArgs(
    @SerialName("formdata")
    val formData: String,
)
