package pusan.university.plato_calendar.presentation.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pusan.university.plato_calendar.R
import pusan.university.plato_calendar.domain.repository.CourseRepository
import pusan.university.plato_calendar.domain.repository.ScheduleRepository
import pusan.university.plato_calendar.presentation.common.manager.LoginManager
import pusan.university.plato_calendar.presentation.common.manager.ScheduleManager
import pusan.university.plato_calendar.presentation.common.manager.SettingsManager
import pusan.university.plato_calendar.presentation.common.notification.AlarmScheduler
import pusan.university.plato_calendar.presentation.common.serializer.PersonalScheduleSerializer.deserializePersonalSchedules
import pusan.university.plato_calendar.presentation.widget.callback.NavigateDateCallback
import pusan.university.plato_calendar.presentation.widget.callback.OpenNewScheduleCallback
import pusan.university.plato_calendar.presentation.widget.callback.RefreshSchedulesCallback
import pusan.university.plato_calendar.presentation.widget.component.ScheduleWidgetItem
import pusan.university.plato_calendar.presentation.widget.util.WidgetUpdater
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle.FULL
import java.time.format.TextStyle.SHORT
import java.util.Locale.KOREAN

@Suppress("RestrictedApi")
object CalendarWidget : GlanceAppWidget() {
    init {
        actionRunCallback<RefreshSchedulesCallback>()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun scheduleRepository(): ScheduleRepository

        fun courseRepository(): CourseRepository

        fun loginManager(): LoginManager

        fun scheduleManager(): ScheduleManager

        fun settingsManager(): SettingsManager

        fun alarmScheduler(): AlarmScheduler

        fun widgetUpdater(): WidgetUpdater
    }

    data class ScheduleWidgetUiModel(
        val title: String,
        val deadLine: String,
        val indicatorDrawable: Int,
    )

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            val prefs = currentState<Preferences>()
            val schedulesJson = prefs[stringPreferencesKey("schedules_list")] ?: ""
            val todayStr = prefs[stringPreferencesKey("today")] ?: LocalDate.now().toString()
            val selectedDateStr = prefs[stringPreferencesKey("selected_date")] ?: todayStr
            val isLoading = prefs[booleanPreferencesKey("is_loading")] ?: false

            val today = LocalDate.parse(todayStr)
            val selectedDate = LocalDate.parse(selectedDateStr)

            val weekDates = (0..6).map { today.plusDays(it.toLong()) }

            val schedules = deserializePersonalSchedules(schedulesJson)
            val schedulesMap = schedules.groupBy { schedule -> schedule.endAt.toLocalDate() }
            val selectedDateSchedules =
                schedulesMap[selectedDate]
                    ?.sortedBy { it.isCompleted }
                    ?: emptyList()

            Column(
                modifier =
                    GlanceModifier
                        .fillMaxSize()
                        .background(ImageProvider(R.drawable.widget_background))
                        .padding(16.dp),
                verticalAlignment = Alignment.Vertical.Top,
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.Start,
                ) {
                    Row(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                        horizontalAlignment = Alignment.Horizontal.Start,
                    ) {
                        Text(
                            text = selectedDate.dayOfMonth.toString(),
                            style =
                                TextStyle(
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorProvider(Color.Black),
                                    textAlign = TextAlign.Center,
                                ),
                            modifier = GlanceModifier.width(52.dp),
                        )

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        Column(
                            verticalAlignment = Alignment.Vertical.CenterVertically,
                        ) {
                            Text(
                                text = "${selectedDate.monthValue}월, ${selectedDate.year}",
                                style =
                                    TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ColorProvider(Color.Black),
                                    ),
                            )
                            Text(
                                text =
                                    if (selectedDate == today) {
                                        "${selectedDate.dayOfWeek.getDisplayName(FULL, KOREAN)}, 오늘"
                                    } else {
                                        selectedDate.dayOfWeek.getDisplayName(FULL, KOREAN)
                                    },
                                style =
                                    TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = ColorProvider(Color.Gray),
                                    ),
                            )
                        }
                    }

                    Box(
                        modifier =
                            GlanceModifier
                                .width(36.dp)
                                .height(36.dp)
                                .background(ImageProvider(R.drawable.widget_circle_blue))
                                .clickable(
                                    actionRunCallback<RefreshSchedulesCallback>(),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_refresh),
                            contentDescription = null,
                            modifier = GlanceModifier.width(20.dp).height(20.dp),
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    Box(
                        modifier =
                            GlanceModifier
                                .width(36.dp)
                                .height(36.dp)
                                .background(ImageProvider(R.drawable.widget_circle_blue))
                                .clickable(
                                    actionStartActivity(
                                        Intent().apply {
                                            setClassName(
                                                "pusan.university.plato_calendar",
                                                "pusan.university.plato_calendar.presentation.PlatoCalendarActivity",
                                            )
                                            action = OpenNewScheduleCallback.ACTION_OPEN_NEW_SCHEDULE
                                            flags =
                                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                        },
                                    ),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = GlanceModifier.width(20.dp).height(20.dp),
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(12.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Horizontal.Start,
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    weekDates.forEach { date ->
                        val isToday = date == today

                        Box(
                            modifier = GlanceModifier.defaultWeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = date.dayOfWeek.getDisplayName(SHORT, KOREAN).uppercase(),
                                style =
                                    TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color =
                                            ColorProvider(
                                                when {
                                                    isToday -> Color(0xFF3B6EC7)
                                                    date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY ->
                                                        Color.Red

                                                    else -> Color.Black
                                                },
                                            ),
                                    ),
                            )
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Horizontal.Start,
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    weekDates.forEach { date ->
                        val isToday = date == today
                        val isSelected = date == selectedDate

                        Box(
                            modifier = GlanceModifier.defaultWeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier =
                                    GlanceModifier
                                        .width(32.dp)
                                        .height(32.dp)
                                        .then(
                                            when {
                                                isSelected ->
                                                    GlanceModifier.background(
                                                        ImageProvider(R.drawable.widget_selected_date_background),
                                                    )

                                                else -> GlanceModifier
                                            },
                                        ).clickable(
                                            actionRunCallback<NavigateDateCallback>(
                                                actionParametersOf(
                                                    NavigateDateCallback.currentDateKey to selectedDate.toString(),
                                                    NavigateDateCallback.targetDateKey to date.toString(),
                                                ),
                                            ),
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style =
                                        TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color =
                                                ColorProvider(
                                                    when {
                                                        isToday -> Color(0xFF3B6EC7)
                                                        date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY ->
                                                            Color.Red

                                                        else -> Color.Black
                                                    },
                                                ),
                                        ),
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    Spacer(
                        modifier =
                            GlanceModifier
                                .fillMaxWidth()
                                .height(8.dp),
                    )

                    LinearProgressIndicator(
                        modifier =
                            GlanceModifier
                                .fillMaxWidth()
                                .height(9.dp),
                    )

                    Spacer(
                        modifier =
                            GlanceModifier
                                .fillMaxWidth()
                                .height(4.dp),
                    )
                } else {
                    Spacer(
                        modifier =
                            GlanceModifier
                                .fillMaxWidth()
                                .height(12.dp),
                    )

                    Spacer(
                        modifier =
                            GlanceModifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFCCCCCC)),
                    )

                    Spacer(
                        modifier =
                            GlanceModifier
                                .fillMaxWidth()
                                .height(8.dp),
                    )
                }

                if (selectedDateSchedules.isEmpty()) {
                    Box(
                        modifier =
                            GlanceModifier
                                .fillMaxWidth()
                                .defaultWeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "일정 없음",
                            style =
                                TextStyle(
                                    fontSize = 14.sp,
                                    color = ColorProvider(Color.Gray),
                                ),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier =
                            GlanceModifier
                                .fillMaxWidth()
                                .defaultWeight(),
                    ) {
                        items(selectedDateSchedules) { schedule ->
                            ScheduleWidgetItem(schedule)
                            Spacer(modifier = GlanceModifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
