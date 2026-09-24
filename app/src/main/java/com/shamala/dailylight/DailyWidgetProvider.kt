package com.shamala.dailylight

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.Toast
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
            ACTION_PINNED -> {
                // The launcher has just placed it. Show them where it went,
                // and how to move it, while it's in front of them.
                refreshAll(context)
                Toast.makeText(context, R.string.pin_done, Toast.LENGTH_LONG).show()
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_HOME)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
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
        const val ACTION_PINNED = "com.shamala.dailylight.action.PINNED"

        /**
         * Ask the launcher to put the widget on the home screen. It shows its
         * own "Add to home screen?" box; [ACTION_PINNED] arrives if they say
         * yes. Returns false where the launcher can't do this, and the app
         * shows the steps to place it by hand instead.
         */
        fun requestPin(context: Context): Boolean {
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val callback = PendingIntent.getBroadcast(
                context, 3,
                Intent(context, DailyWidgetProvider::class.java).setAction(ACTION_PINNED),
                // Mutable: the launcher adds the new widget's id to it.
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            // Today's real card for the launcher's "Add to home screen?" box,
            // instead of the layout's placeholder text in the system font.
            val preview = Bundle().apply {
                putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, previewViews(context))
            }
            return manager.requestPinAppWidget(
                ComponentName(context, DailyWidgetProvider::class.java), preview, callback
            )
        }

        /** Whether at least one Daily Light widget is placed anywhere. */
        fun isPlaced(context: Context): Boolean =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, DailyWidgetProvider::class.java))
                .isNotEmpty()

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

        /**
         * Usable width and height in pixels inside this placed widget's
         * padding.
         *
         * The launcher reports a range, not one size: in portrait the card is
         * MIN_WIDTH x MAX_HEIGHT, in landscape MAX_WIDTH x MIN_HEIGHT. Reading
         * MIN_WIDTH alone painted a landscape card's words too narrow for
         * their view. Height is null when the launcher gives nothing, and the
         * words then paint at their natural size.
         */
        private fun contentSizePx(
            context: Context,
            manager: AppWidgetManager?,
            appWidgetId: Int
        ): Pair<Int, Int?> {
            val density = context.resources.displayMetrics.density
            val landscape = context.resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
            val options = manager?.getAppWidgetOptions(appWidgetId)
            fun option(key: String) = options?.getInt(key, 0)?.takeIf { it > 0 }

            val widthDp = (if (landscape) option(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) else null)
                ?: option(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                ?: 300
            val heightDp = (if (landscape) option(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) else null)
                ?: option(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

            val usableWidthDp = (widthDp - CardPainter.HORIZONTAL_PADDING_DP).coerceAtLeast(120)
            val usableHeightDp = heightDp?.let { it - CardPainter.VERTICAL_PADDING_DP }
            return (usableWidthDp * density).toInt() to usableHeightDp?.let { (it * density).toInt() }
        }

        /** The card at its default 4x3 size, for the launcher to preview. */
        private fun previewViews(context: Context): RemoteViews {
            val density = context.resources.displayMetrics.density
            return buildViews(
                context,
                size = ((PREVIEW_WIDTH_DP - CardPainter.HORIZONTAL_PADDING_DP) * density).toInt() to
                    ((PREVIEW_HEIGHT_DP - CardPainter.VERTICAL_PADDING_DP) * density).toInt()
            )
        }

        private const val PREVIEW_WIDTH_DP = 360
        private const val PREVIEW_HEIGHT_DP = 290

        fun buildViews(
            context: Context,
            manager: AppWidgetManager? = null,
            appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
            size: Pair<Int, Int?> = contentSizePx(context, manager, appWidgetId)
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_daily)
            val (widthPx, heightPx) = size
            CardPainter.paint(context, RemoteViewsSurface(views), widthPx, heightPx)

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
