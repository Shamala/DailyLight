package com.shamala.dailylight

import android.content.Context
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/** Where in the day we are. Drives the background only. */
enum class Phase { DAWN, DAY, DUSK, NIGHT }

/** Everything the card needs to draw itself for a given moment. */
data class DayContent(
    val weekday: String,
    val date: String,
    val dayOfYear: Int,
    val daysInYear: Int,
    /** 0..1000, for the progress bar. */
    val yearProgress: Int,
    /** Empty when the style is BAR_ONLY. */
    val yearLine: String,
    val affirmation: String,
    val thought: String,
    val voice: Voice,
    val phase: Phase,
    val isKept: Boolean
)

object DailyContent {

    /** The current moment, with the person's settings and words applied. */
    fun now(context: Context, at: LocalDateTime = LocalDateTime.now()): DayContent {
        val voice = voiceAt(
            hour = at.hour,
            eveningEnabled = Prefs.eveningEnabled(context),
            eveningHour = Prefs.eveningHour(context)
        )
        val content = build(
            at = at,
            voice = voice,
            style = Prefs.yearLine(context),
            offset = Prefs.offset(context),
            custom = Prefs.customWords(context),
            favourites = Prefs.favourites(context, voice)
        )
        return content.copy(
            isKept = Prefs.isFavourite(context, voice, content.affirmation)
        )
    }

    /**
     * Morning until the evening hour, then the closing voice through the small
     * hours — someone still up at 1am is ending a day, not starting one.
     */
    fun voiceAt(hour: Int, eveningEnabled: Boolean, eveningHour: Int): Voice = when {
        !eveningEnabled -> Voice.MORNING
        hour >= eveningHour -> Voice.EVENING
        hour < 4 -> Voice.EVENING
        else -> Voice.MORNING
    }

    fun phaseAt(hour: Int): Phase = when (hour) {
        in 5..9 -> Phase.DAWN
        in 10..16 -> Phase.DAY
        in 17..20 -> Phase.DUSK
        else -> Phase.NIGHT
    }

    /**
     * Pure: the same arguments always give the same card. That is what keeps
     * the words still all day however often the widget redraws.
     */
    fun build(
        at: LocalDateTime,
        voice: Voice,
        style: YearLineStyle = YearLineStyle.DAY_OF_YEAR,
        offset: Int = 0,
        custom: List<String> = emptyList(),
        favourites: List<String> = emptyList()
    ): DayContent {
        val locale = Locale.getDefault()
        val today: LocalDate = at.toLocalDate()

        val weekday = today.dayOfWeek.getDisplayName(TextStyle.FULL, locale).uppercase(locale)
        val month = today.month.getDisplayName(TextStyle.FULL, locale)
        val date = "${today.dayOfMonth} $month ${today.year}"

        val dayOfYear = today.dayOfYear
        val daysInYear = today.lengthOfYear()
        val progress = (dayOfYear * 1000) / daysInYear

        val yearLine = when (style) {
            YearLineStyle.DAY_OF_YEAR -> "Day $dayOfYear · ${today.year}"
            YearLineStyle.DAYS_LIVED ->
                if (dayOfYear == 1) "1 day lived this year"
                else "$dayOfYear days lived this year"
            YearLineStyle.BAR_ONLY -> ""
            YearLineStyle.COUNTDOWN -> {
                val left = daysInYear - dayOfYear
                when (left) {
                    0 -> "the last day of ${today.year}"
                    1 -> "1 day left in ${today.year}"
                    else -> "$left days left in ${today.year}"
                }
            }
        }

        // The evening pool is offset so a morning and an evening on the same
        // date don't land on the same index in their respective lists.
        val voiceSalt = if (voice == Voice.EVENING) 977 else 0
        val seed = today.year * 401 + dayOfYear + offset + voiceSalt

        val basePool =
            if (voice == Voice.EVENING) Words.eveningAffirmations else Words.affirmations
        val thoughtPool =
            if (voice == Voice.EVENING) Words.eveningThoughts else Words.thoughts

        val pool = basePool + custom

        // Roughly one day in four is drawn from what she chose to keep.
        val affirmation =
            if (favourites.isNotEmpty() && Math.floorMod(seed, 4) == 0) {
                favourites[Math.floorMod(seed / 4, favourites.size)]
            } else {
                pool[Math.floorMod(seed, pool.size)]
            }

        val thought = thoughtPool[Math.floorMod(seed * 7 + 29, thoughtPool.size)]

        return DayContent(
            weekday = weekday,
            date = date,
            dayOfYear = dayOfYear,
            daysInYear = daysInYear,
            yearProgress = progress,
            yearLine = yearLine,
            affirmation = affirmation,
            thought = thought,
            voice = voice,
            phase = phaseAt(at.hour),
            isKept = false
        )
    }

    // ------------------------------------------------------------------
    // Type sizes. Every line on the card scales, not just the affirmation —
    // a text-size setting that leaves the date and year line untouched isn't
    // a text-size setting. Bases below are the Small (1.0) values.
    // ------------------------------------------------------------------

    /** The letterspaced weekday label. */
    fun weekdaySizeSp(scale: TextScale): Float = 11f * scale.factor

    fun dateSizeSp(scale: TextScale): Float = 16f * scale.factor

    /** "Day 252 · 2026". Was 10sp and fixed, which was far too small. */
    fun yearLineSizeSp(scale: TextScale): Float = 12f * scale.factor

    /**
     * Long lines get a smaller face so nothing is clipped at 4x2, then the
     * person's own choice is applied on top.
     */
    fun affirmationSizeSp(text: String, scale: TextScale): Float {
        val base = when {
            text.length > 96 -> 15.5f
            text.length > 74 -> 17f
            text.length > 54 -> 18f
            else -> 19.5f
        }
        return base * scale.factor
    }

    fun thoughtSizeSp(text: String, scale: TextScale): Float {
        val base = if (text.length > 110) 11.5f else 12.5f
        return base * scale.factor
    }
}
