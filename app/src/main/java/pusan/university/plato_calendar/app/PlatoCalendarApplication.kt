package pusan.university.plato_calendar.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import pusan.university.plato_calendar.BuildConfig
import pusan.university.plato_calendar.presentation.common.manager.NotificationSyncManager
import pusan.university.plato_calendar.presentation.common.manager.ScheduleRefreshManager
import pusan.university.plato_calendar.presentation.common.notification.NotificationHelper.Companion.CHANNEL_ID
import pusan.university.plato_calendar.presentation.common.notification.NotificationHelper.Companion.CHANNEL_NAME
import javax.inject.Inject

@HiltAndroidApp
class PlatoCalendarApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var notificationSyncManager: NotificationSyncManager

    @Inject
    lateinit var scheduleRefreshManager: ScheduleRefreshManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()

        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

        createNotificationChannel(applicationContext)
        notificationSyncManager.startSync(applicationScope)
        scheduleRefreshManager.setupPeriodicRefresh()
    }

    private fun createNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(
                NotificationManager::class.java,
            )

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

        notificationManager.createNotificationChannel(channel)
    }
}
