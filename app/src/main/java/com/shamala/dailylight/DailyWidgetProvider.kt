package com.shamala.dailylight

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
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
        val views = buildViews(context)
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
        scheduleNextMidnight(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_CYCLE -> {
                DailyContent.bumpOffset(context)
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

        /** Redraw every instance of the widget currently on a home screen. */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, DailyWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            val views = buildViews(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        fun buildViews(context: Context): RemoteViews {
            val day = DailyContent.forToday(context)
            val views = RemoteViews(context.packageName, R.layout.widget_daily)

            views.setTextViewText(R.id.tv_weekday, day.weekday)
            views.setTextViewText(R.id.tv_date, day.date)
            views.setTextViewText(R.id.tv_countdown, accentedCountdown(context, day))
            views.setTextViewText(R.id.tv_affirmation, day.affirmation)
            views.setTextViewText(R.id.tv_thought, day.thought)

            // Tapping anywhere on the widget draws a different pairing.
            val cycle = Intent(context, DailyWidgetProvider::class.java).apply {
                action = ACTION_CYCLE
            }
            val pending = PendingIntent.getBroadcast(
                context, 0, cycle,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)

            return views
        }

        /** Picks the number out of the countdown line and warms it up. */
        private fun accentedCountdown(context: Context, day: DayContent): CharSequence {
            val text = day.countdown
            val span = SpannableString(text)
            val accent = context.getColor(R.color.accent)
            val digits = Regex("\\d+").find(text)
            if (digits != null) {
                span.setSpan(
                    ForegroundColorSpan(accent),
                    digits.range.first,
                    digits.range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                // "the last day of 2026" — warm the whole phrase instead.
                span.setSpan(
                    ForegroundColorSpan(accent), 0, text.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return span
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
         * Inexact daily alarm at 00:01 so the date flips over on its own.
         * Inexact deliberately: it needs no special permission on Android 12+
         * and costs nothing in battery. The 30-minute `updatePeriodMillis` in
         * widget_info.xml is the backstop if the system defers it.
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

            alarms.setRepeating(
                AlarmManager.RTC,
                next,
                AlarmManager.INTERVAL_DAY,
                midnightIntent(context)
            )
        }
    }
}
