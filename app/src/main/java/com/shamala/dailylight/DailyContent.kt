package com.shamala.dailylight

import android.content.Context
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Everything the widget needs to draw itself for a given day.
 *
 * The pairing of affirmation + thought is deterministic for a date, so the
 * words stay put all day no matter how often the widget redraws. The tap
 * gesture bumps a stored offset, which shuffles to a different pairing
 * without changing the date logic.
 */
data class DayContent(
    val weekday: String,
    val date: String,
    val daysLeft: Int,
    val countdown: String,
    val affirmation: String,
    val thought: String
)

object DailyContent {

    private const val PREFS = "daily_light_prefs"
    private const val KEY_OFFSET = "shuffle_offset"

    fun offset(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_OFFSET, 0)

    fun bumpOffset(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_OFFSET, prefs.getInt(KEY_OFFSET, 0) + 1).apply()
    }

    fun forToday(context: Context): DayContent =
        forDate(LocalDate.now(), offset(context))

    fun forDate(today: LocalDate, offset: Int = 0): DayContent {
        val locale = Locale.getDefault()

        val weekday = today.dayOfWeek
            .getDisplayName(TextStyle.FULL, locale)
            .uppercase(locale)

        val month = today.month.getDisplayName(TextStyle.FULL, locale)
        val date = "${today.dayOfMonth} $month ${today.year}"

        val endOfYear = LocalDate.of(today.year, 12, 31)
        val daysLeft = ChronoUnit.DAYS.between(today, endOfYear).toInt()

        val countdown = when (daysLeft) {
            0 -> "the last day of ${today.year}"
            1 -> "1 day left in ${today.year}"
            else -> "$daysLeft days left in ${today.year}"
        }

        // Two different strides so the affirmation and the thought don't
        // march in lockstep — the same affirmation meets a different thought
        // in a different year.
        val seed = today.year * 401 + today.dayOfYear + offset
        val affirmation = Words.affirmations[
            Math.floorMod(seed, Words.affirmations.size)
        ]
        val thought = Words.thoughts[
            Math.floorMod(seed * 7 + 29, Words.thoughts.size)
        ]

        return DayContent(weekday, date, daysLeft, countdown, affirmation, thought)
    }
}
