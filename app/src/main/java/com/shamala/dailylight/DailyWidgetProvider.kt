package com.shamala.dailylight

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DailyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context, appWidgetManager, id))
        }
        scheduleNextMidnight(context)
    }

    /** Resized: the Lora bitmaps have to be repainted at the new width. */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        appWidgetManager.updateAppWidget(
            appWidgetId, buildViews(context, appWidgetManager, appWidgetId)
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_CYCLE -> {
                Prefs.bumpOffset(context)
                refreshAll(context)
            }
            ACTION_MIDNIGHT,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> {
                refreshAll(context)
                scheduleNextMidnight(context)
            }
        }
    }

    override fun onEnabled(context: Context) = scheduleNextMidnight(context)

    override fun onDisabled(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.cancel(midnightIntent(context))
    }

    companion object {
        const val ACTION_CYCLE = "com.shamala.dailylight.action.CYCLE"
        const val ACTION_MIDNIGHT = "com.shamala.dailylight.action.MIDNIGHT"

        /** Redraw every instance, each at its own width. */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, DailyWidgetProvider::class.java)
            )
            ids.forEach { id ->
                manager.updateAppWidget(id, buildViews(context, manager, id))
            }
        }

        /** Usable text width in pixels for this particular placed widget. */
        private fun contentWidthPx(
            context: Context,
            manager: AppWidgetManager?,
            appWidgetId: Int
        ): Int {
            val density = context.resources.displayMetrics.density
            val widthDp = manager
                ?.getAppWidgetOptions(appWidgetId)
                ?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                ?.takeIf { it > 0 }
                ?: 300
            val usableDp = (widthDp - CardPainter.HORIZONTAL_PADDING_DP).coerceAtLeast(120)
            return (usableDp * density).toInt()
        }

        fun buildViews(
            context: Context,
            manager: AppWidgetManager? = null,
            appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_daily)
            CardPainter.paint(
                context,
                RemoteViewsSurface(views),
                contentWidthPx(context, manager, appWidgetId)
            )

            // --- the two tap zones -----------------------------------------

            val cycle = Intent(context, DailyWidgetProvider::class.java).apply {
                action = ACTION_CYCLE
            }
            views.setOnClickPendingIntent(
                R.id.words_zone,
                PendingIntent.getBroadcast(
                    context, 0, cycle,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            val open = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.header_zone,
                PendingIntent.getActivity(
                    context, 2, open,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            return views
        }

        private fun midnightIntent(context: Context): PendingIntent {
            val intent = Intent(context, DailyWidgetProvider::class.java).apply {
                action = ACTION_MIDNIGHT
            }
            return PendingIntent.getBroadcast(
                context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /**
         * Inexact one-shot alarm for the next 00:01 so the date flips over on
         * its own. Each firing schedules the following one.
         *
         * One-shot rather than setRepeating(INTERVAL_DAY): a fixed 24-hour
         * interval drifts off midnight the first time the clock crosses a DST
         * boundary and never recovers, and the same happens when the person
         * flies somewhere. Recomputing from the local calendar each time keeps
         * it at 00:01 wherever the phone is.
         *
         * Inexact deliberately: no special permission on Android 12+, and no
         * measurable battery cost. The 30-minute `updatePeriodMillis` in
         * widget_info.xml is what catches the morning-to-evening switch and
         * the background changing through the day, and it is also the safety
         * net if the alarm is dropped while the device is in Doze.
         */
        fun scheduleNextMidnight(context: Context) {
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val zone = ZoneId.systemDefault()
            val next = LocalDate.now(zone)
                .plusDays(1)
                .atTime(LocalTime.of(0, 1))
                .atZone(zone)
                .toInstant()
                .toEpochMilli()

            // A dead widget is not worth a crash: on the rare OEM that throttles
            // alarm registration, the periodic update still carries the date.
            runCatching {
                alarms.set(AlarmManager.RTC, next, midnightIntent(context))
            }
        }
    }
}
