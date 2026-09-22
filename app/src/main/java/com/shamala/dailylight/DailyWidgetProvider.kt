package com.shamala.dailylight

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
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

        /** Side padding in widget_daily.xml, both sides. */
        private const val HORIZONTAL_PADDING_DP = 44

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

        fun backgroundFor(phase: Phase, isDark: Boolean): Int {
            return if (isDark) {
                when (phase) {
                    Phase.DAWN -> R.drawable.widget_bg_dawn
                    Phase.DAY -> R.drawable.widget_bg_day
                    Phase.DUSK -> R.drawable.widget_bg_dusk
                    Phase.NIGHT -> R.drawable.widget_bg_night
                }
            } else {
                when (phase) {
                    Phase.DAWN -> R.drawable.widget_bg_dawn_light
                    Phase.DAY -> R.drawable.widget_bg_day_light
                    Phase.DUSK -> R.drawable.widget_bg_dusk_light
                    Phase.NIGHT -> R.drawable.widget_bg_night_light
                }
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
            val usableDp = (widthDp - HORIZONTAL_PADDING_DP).coerceAtLeast(120)
            return (usableDp * density).toInt()
        }

        fun buildViews(
            context: Context,
            manager: AppWidgetManager? = null,
            appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
        ): RemoteViews {
            val day = DailyContent.now(context)
            val scale = Prefs.textScale(context)
            val isDark = Prefs.isDarkMode(context)
            val views = RemoteViews(context.packageName, R.layout.widget_daily)

            // Adjust text colors based on dark mode preference
            if (isDark) {
                views.setTextColor(R.id.tv_weekday, context.getColor(R.color.day_label))
                views.setTextColor(R.id.tv_date, context.getColor(R.color.cream_dim))
                views.setTextColor(R.id.tv_affirmation, context.getColor(R.color.cream))
                views.setTextColor(R.id.tv_thought, context.getColor(R.color.muted))
                views.setTextColor(R.id.tv_yearline, context.getColor(R.color.muted))
            } else {
                views.setTextColor(R.id.tv_weekday, context.getColor(R.color.label_dark))
                views.setTextColor(R.id.tv_date, context.getColor(R.color.text_dark_dim))
                views.setTextColor(R.id.tv_affirmation, context.getColor(R.color.text_dark))
                views.setTextColor(R.id.tv_thought, context.getColor(R.color.text_muted_dark))
                views.setTextColor(R.id.tv_yearline, context.getColor(R.color.text_muted_dark))
            }

            val background =
                if (Prefs.shiftColours(context)) {
                    backgroundFor(day.phase, isDark)
                } else {
                    if (isDark) R.drawable.widget_bg_dawn else R.drawable.widget_bg_dawn_light
                }
            views.setInt(R.id.widget_root, "setBackgroundResource", background)

            views.setTextViewText(R.id.tv_weekday, day.weekday)
            views.setTextViewTextSize(
                R.id.tv_weekday, TypedValue.COMPLEX_UNIT_SP,
                DailyContent.weekdaySizeSp(scale)
            )

            // --- the Lora lines -------------------------------------------
            // Painted here and sent as bitmaps, because the launcher inflates
            // this layout in its own process and can substitute the system
            // font for the one the layout asks for. Text views stay in the
            // layout as a fallback if a bitmap can't be made.

            val widthPx = contentWidthPx(context, manager, appWidgetId)

            val dateBitmap = CardRenderer.date(
                context, day.date, widthPx, DailyContent.dateSizeSp(scale), isDark
            )
            if (dateBitmap != null) {
                views.setImageViewBitmap(R.id.img_date, dateBitmap)
                views.setViewVisibility(R.id.img_date, View.VISIBLE)
                views.setViewVisibility(R.id.tv_date, View.GONE)
            } else {
                views.setViewVisibility(R.id.img_date, View.GONE)
                views.setViewVisibility(R.id.tv_date, View.VISIBLE)
                views.setTextViewText(R.id.tv_date, day.date)
                views.setTextViewTextSize(
                    R.id.tv_date, TypedValue.COMPLEX_UNIT_SP,
                    DailyContent.dateSizeSp(scale)
                )
            }

            val wordsBitmap = CardRenderer.words(
                context, day.affirmation, day.thought, widthPx, scale, isDark
            )
            if (wordsBitmap != null) {
                views.setImageViewBitmap(R.id.img_words, wordsBitmap)
                views.setViewVisibility(R.id.img_words, View.VISIBLE)
                views.setViewVisibility(R.id.tv_affirmation, View.GONE)
                views.setViewVisibility(R.id.tv_thought, View.GONE)
            } else {
                views.setViewVisibility(R.id.img_words, View.GONE)
                views.setViewVisibility(R.id.tv_affirmation, View.VISIBLE)
                views.setViewVisibility(R.id.tv_thought, View.VISIBLE)
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
            }

            // --- the year line and its bar ---------------------------------

            views.setTextViewTextSize(
                R.id.tv_yearline, TypedValue.COMPLEX_UNIT_SP,
                DailyContent.yearLineSizeSp(scale)
            )

            val showText = day.yearLine.isNotEmpty()
            val showBar = Prefs.showBar(context)

            // Hide both containers initially
            views.setViewVisibility(R.id.fl_year_container, View.GONE)
            views.setViewVisibility(R.id.ll_year_inline, View.GONE)
            views.setViewVisibility(R.id.fl_year_full, View.GONE)

            if (showBar) {
                views.setViewVisibility(R.id.fl_year_container, View.VISIBLE)
                if (showText) {
                    // Inline mode
                    views.setViewVisibility(R.id.ll_year_inline, View.VISIBLE)
                    views.setTextViewText(R.id.tv_yearline, accented(context, day.yearLine, isDark))
                    
                    views.setViewVisibility(R.id.pb_year_inline, if (isDark) View.VISIBLE else View.GONE)
                    views.setViewVisibility(R.id.pb_year_light_inline, if (isDark) View.GONE else View.VISIBLE)
                    views.setProgressBar(R.id.pb_year_inline, 1000, day.yearProgress, false)
                    views.setProgressBar(R.id.pb_year_light_inline, 1000, day.yearProgress, false)
                } else {
                    // Full width bar only mode
                    views.setViewVisibility(R.id.fl_year_full, View.VISIBLE)
                    views.setViewVisibility(R.id.pb_year_full, if (isDark) View.VISIBLE else View.GONE)
                    views.setViewVisibility(R.id.pb_year_light_full, if (isDark) View.GONE else View.VISIBLE)
                    views.setProgressBar(R.id.pb_year_full, 1000, day.yearProgress, false)
                    views.setProgressBar(R.id.pb_year_light_full, 1000, day.yearProgress, false)
                }
            }

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

        /** Warms the number, so the eye lands on it without it shouting. */
        fun accented(context: Context, text: String, isDark: Boolean = true): CharSequence {
            val span = SpannableString(text)
            if (text.isEmpty()) return span
            val colour = context.getColor(if (isDark) R.color.accent else R.color.accent_dark)
            val digits = Regex("\\d+").find(text)
            val range = digits?.range ?: text.indices
            span.setSpan(
                ForegroundColorSpan(colour),
                range.first,
                range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
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
