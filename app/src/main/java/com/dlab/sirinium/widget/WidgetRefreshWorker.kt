package com.dlab.sirinium.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Lightweight periodic worker that forces a Glance widget re-composition.
 *
 * Unlike [com.dlab.sirinium.sync.ScheduleSyncWorker] which fetches data from the network,
 * this worker only tells the existing widgets to re-read their local data and re-render.
 * This covers time-dependent state changes (lesson started/ended, countdown text)
 * that would otherwise only update on the next full sync or when the app is opened.
 */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "WidgetRefreshWorker"
        private const val UNIQUE_WORK_NAME = "PeriodicWidgetRefresh"

        /** Schedule periodic widget refreshes (minimum 15 minutes per WorkManager policy). */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Periodic widget refresh triggered")
            WidgetUpdateHelper.updateAllWidgets(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing widgets", e)
            Result.success() // Don't retry for widget refresh — next periodic run will handle it
        }
    }
}
