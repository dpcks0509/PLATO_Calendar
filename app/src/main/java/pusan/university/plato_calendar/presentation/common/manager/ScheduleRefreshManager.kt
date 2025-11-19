package pusan.university.plato_calendar.presentation.common.manager

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import pusan.university.plato_calendar.presentation.common.worker.ScheduleRefreshWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRefreshManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val workManager: WorkManager by lazy {
            WorkManager.getInstance(context)
        }

        fun setupPeriodicRefresh() {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val periodicWorkRequest =
                PeriodicWorkRequestBuilder<ScheduleRefreshWorker>(
                    1,
                    TimeUnit.HOURS,
                ).setConstraints(constraints)
                    .build()

            workManager.enqueueUniquePeriodicWork(
                SCHEDULE_REFRESH_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest,
            )
        }

        companion object {
            private const val SCHEDULE_REFRESH_WORK_NAME = "schedule_refresh_work"
        }
    }
