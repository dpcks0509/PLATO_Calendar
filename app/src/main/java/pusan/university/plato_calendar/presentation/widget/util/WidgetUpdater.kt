package pusan.university.plato_calendar.presentation.widget.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import dagger.hilt.android.qualifiers.ApplicationContext
import pusan.university.plato_calendar.presentation.calendar.model.ScheduleUiModel.PersonalScheduleUiModel
import pusan.university.plato_calendar.presentation.common.serializer.PersonalScheduleSerializer
import pusan.university.plato_calendar.presentation.widget.CalendarWidget
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdater
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        suspend fun updateAllWidgets(personalSchedules: List<PersonalScheduleUiModel>) {
            try {
                val glanceManager = GlanceAppWidgetManager(context)
                val glanceIds = glanceManager.getGlanceIds(CalendarWidget::class.java)

                if (glanceIds.isEmpty()) {
                    return
                }

                val schedulesJson =
                    PersonalScheduleSerializer.serializePersonalSchedules(personalSchedules)
                val today = LocalDate.now().toString()

                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[stringPreferencesKey("schedules_list")] = schedulesJson
                        prefs[stringPreferencesKey("today")] = today
                        prefs[stringPreferencesKey("selected_date")] = today
                        prefs[booleanPreferencesKey("is_loading")] = false
                    }

                    CalendarWidget.update(context, glanceId)
                }
            } catch (_: Exception) {
            }
        }

        suspend fun setWidgetsLoading() {
            try {
                val glanceManager = GlanceAppWidgetManager(context)
                val glanceIds = glanceManager.getGlanceIds(CalendarWidget::class.java)

                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[booleanPreferencesKey("is_loading")] = true
                    }
                    CalendarWidget.update(context, glanceId)
                }
            } catch (_: Exception) {
            }
        }
    }
