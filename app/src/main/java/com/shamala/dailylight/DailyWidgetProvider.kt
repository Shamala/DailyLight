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
import android.util.TypedValue
import android.view.View
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

        /** Redraw every instance of the widget, wherever it is placed. */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, DailyWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            val views = buildViews(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        fun backgroundFor(phase: Phase): Int = when (phase) {
            Phase.DAWN -> R.drawable.widget_bg_dawn
            Phase.DAY -> R.drawable.widget_bg_day
            Phase.DUSK -> R.drawable.widget_bg_dusk
            Phase.NIGHT -> R.drawable.widget_bg_night
        }

        fun buildViews(context: Context): RemoteViews {
            val day = DailyContent.now(context)
            val scale = Prefs.textScale(context)
            val views = RemoteViews(context.packageName, R.layout.widget_daily)

            val background =
                if (Prefs.shiftColours(context)) backgroundFor(day.phase)
                else R.drawable.widget_bg_dawn
            views.setInt(R.id.widget_root, "setBackgroundResource", background)

            views.setTextViewText(R.id.tv_weekday, day.weekday)
            views.setTextViewText(R.id.tv_date, day.date)
            views.setTextViewText(R.id.tv_affirmation, day.affirmation)
            views.setTextViewText(R.id.tv_thought, day.thought)

            views.setTextViewTextSize(
                R.id.tv_affirmation, TypedValue.COMPLEX_UNIT_SP,
                DailyContent.affirmationSizeSp(day.affirmation, scale)
            )
            views.setTextViewTextSize(
                R.id.tv_thought, TypedValue.COMPLEX_UNIT_SP,
                DailyContent.thoughtSizeSp(day.thought, scale)
            )

            // The year line disappears entirely on the bar-only setting.
            if (day.yearLine.isEmpty()) {
                views.setViewVisibility(R.id.tv_yearline, View.GONE)
            } else {
                views.setViewVisibility(R.id.tv_yearline, View.VISIBLE)
                views.setTextViewText(R.id.tv_yearline, accented(context, day.yearLine))
            }

            if (Prefs.showBar(context)) {
                views.setViewVisibility(R.id.pb_year, View.VISIBLE)
                views.setProgressBar(R.id.pb_year, 1000, day.yearProgress, false)
            } else {
                views.setViewVisibility(R.id.pb_year, View.GONE)
            }

            // Tap the words to draw a different pairing…
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

            // …tap the date to open the app, where you can keep it or edit.
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

        /** Warms the number, so the eye lands on it without it shouting. */
        fun accented(context: Context, text: String): CharSequence {
            val span = SpannableString(text)
            val colour = context.getColor(R.color.accent)
            val digits = Regex("\\d+").find(text)
            val range = digits?.range ?: text.indices
            if (!text.isEmpty()) {
                span.setSpan(
                    ForegroundColorSpan(colour),
                    range.first,
                    range.last + 1,
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
         * Inexact deliberately: no special permission on Android 12+, and no
         * measurable battery cost. The 30-minute `updatePeriodMillis` in
         * widget_info.xml is what catches the morning-to-evening switch and
         * the background changing through the day.
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
