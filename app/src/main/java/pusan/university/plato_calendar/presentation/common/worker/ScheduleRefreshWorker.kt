package pusan.university.plato_calendar.presentation.common.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import pusan.university.plato_calendar.domain.entity.LoginStatus
import pusan.university.plato_calendar.domain.entity.Schedule.PersonalSchedule.CourseSchedule
import pusan.university.plato_calendar.domain.entity.Schedule.PersonalSchedule.CustomSchedule
import pusan.university.plato_calendar.domain.repository.CourseRepository
import pusan.university.plato_calendar.domain.repository.ScheduleRepository
import pusan.university.plato_calendar.presentation.calendar.model.ScheduleUiModel
import pusan.university.plato_calendar.presentation.calendar.model.ScheduleUiModel.AcademicScheduleUiModel
import pusan.university.plato_calendar.presentation.calendar.model.ScheduleUiModel.PersonalScheduleUiModel
import pusan.university.plato_calendar.presentation.calendar.model.ScheduleUiModel.PersonalScheduleUiModel.CourseScheduleUiModel
import pusan.university.plato_calendar.presentation.calendar.model.ScheduleUiModel.PersonalScheduleUiModel.CustomScheduleUiModel
import pusan.university.plato_calendar.presentation.common.manager.LoginManager
import pusan.university.plato_calendar.presentation.common.manager.ScheduleManager
import pusan.university.plato_calendar.presentation.common.manager.SettingsManager
import pusan.university.plato_calendar.presentation.common.notification.AlarmScheduler
import pusan.university.plato_calendar.presentation.widget.util.WidgetUpdater

@HiltWorker
class ScheduleRefreshWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val loginManager: LoginManager,
        private val scheduleRepository: ScheduleRepository,
        private val courseRepository: CourseRepository,
        private val scheduleManager: ScheduleManager,
        private val alarmScheduler: AlarmScheduler,
        private val settingsManager: SettingsManager,
        private val widgetUpdater: WidgetUpdater,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result =
            try {
                scheduleManager.updateToday()

                loginManager.autoLogin()

                when (val loginStatus = loginManager.loginStatus.value) {
                    is LoginStatus.Login -> {
                        val schedules =
                            coroutineScope {
                                val academicSchedules = async { getAcademicSchedules() }
                                val personalSchedules =
                                    async { getPersonalSchedules(loginStatus.loginSession.sessKey) }

                                academicSchedules.await() + personalSchedules.await()
                            }

                        if (schedules.isNotEmpty()) {
                            scheduleManager.updateSchedules(schedules)

                            val personalSchedules = schedules.filterIsInstance<PersonalScheduleUiModel>()

                            val settings = settingsManager.appSettings.first()
                            if (settings.notificationsEnabled) {
                                alarmScheduler.scheduleNotificationsForSchedule(
                                    personalSchedules = personalSchedules.filter { !it.isCompleted },
                                    firstReminderTime = settings.firstReminderTime,
                                    secondReminderTime = settings.secondReminderTime,
                                )
                            }

                            widgetUpdater.updateAllWidgets(personalSchedules)
                        }

                        Result.success()
                    }

                    LoginStatus.Logout -> {
                        val academicSchedules = getAcademicSchedules()
                        scheduleManager.updateSchedules(academicSchedules)

                        widgetUpdater.updateAllWidgets(emptyList())

                        Result.success()
                    }

                    else -> Result.success()
                }
            } catch (_: Exception) {
                Result.retry()
            }

        private suspend fun getAcademicSchedules(): List<AcademicScheduleUiModel> {
            scheduleRepository
                .getAcademicSchedules()
                .onSuccess {
                    return it.map(::AcademicScheduleUiModel)
                }

            return emptyList()
        }

        private suspend fun getPersonalSchedules(sessKey: String): List<ScheduleUiModel> {
            scheduleRepository
                .getPersonalSchedules(sessKey = sessKey)
                .onSuccess {
                    return it.map { domain ->
                        when (domain) {
                            is CourseSchedule -> {
                                val courseName =
                                    courseRepository.getCourseName(
                                        domain.courseCode,
                                    )

                                CourseScheduleUiModel(
                                    domain = domain,
                                    courseName = courseName,
                                )
                            }

                            is CustomSchedule -> CustomScheduleUiModel(domain)
                        }
                    }
                }

            return emptyList()
        }
    }
