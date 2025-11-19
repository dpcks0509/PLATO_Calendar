package pusan.university.plato_calendar.presentation.common.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pusan.university.plato_calendar.presentation.calendar.model.ScheduleUiModel.PersonalScheduleUiModel
import pusan.university.plato_calendar.presentation.common.manager.ScheduleManager
import pusan.university.plato_calendar.presentation.common.manager.SettingsManager
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var scheduleManager: ScheduleManager

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        scope.launch {
            try {
                val settings = settingsManager.appSettings.first()
                val schedules = scheduleManager.schedules.first()

                if (!settings.notificationsEnabled) {
                    return@launch
                }

                val personalSchedules =
                    schedules.filterIsInstance<PersonalScheduleUiModel>().filter { !it.isCompleted }

                alarmScheduler.scheduleNotificationsForSchedule(
                    personalSchedules = personalSchedules,
                    firstReminderTime = settings.firstReminderTime,
                    secondReminderTime = settings.secondReminderTime,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
