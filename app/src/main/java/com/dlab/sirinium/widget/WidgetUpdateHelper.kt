package com.dlab.sirinium.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetUpdateHelper {
    private const val TAG = "WidgetUpdateHelper"

    /**
     * Updates all Glance widgets and sends explicit AppWidget broadcast to force immediate UI refresh.
     */
    suspend fun updateAllWidgets(context: Context) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext

        // 1. Trigger Glance internal composition update
        try {
            ScheduleGlanceWidget().updateAll(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating ScheduleGlanceWidget via Glance", e)
        }

        try {
            ScheduleListGlanceWidget().updateAll(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating ScheduleListGlanceWidget via Glance", e)
        }

        // 2. Trigger explicit AppWidgetManager broadcasts to notify launcher immediately
        try {
            val appWidgetManager = AppWidgetManager.getInstance(appContext)

            val compactComponent = ComponentName(appContext, ScheduleGlanceWidgetReceiver::class.java)
            val compactIds = appWidgetManager.getAppWidgetIds(compactComponent)
            if (compactIds != null && compactIds.isNotEmpty()) {
                val intent = Intent(appContext, ScheduleGlanceWidgetReceiver::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, compactIds)
                }
                appContext.sendBroadcast(intent)
            }

            val listComponent = ComponentName(appContext, ScheduleListGlanceWidgetReceiver::class.java)
            val listIds = appWidgetManager.getAppWidgetIds(listComponent)
            if (listIds != null && listIds.isNotEmpty()) {
                val intent = Intent(appContext, ScheduleListGlanceWidgetReceiver::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, listIds)
                }
                appContext.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting ACTION_APPWIDGET_UPDATE", e)
        }
    }
}
