package com.shamala.dailylight

import android.content.Context

/** How the year line reads. Set in the app; the widget just renders it. */
enum class YearLineStyle {
    /** "Day 252 · 2026" — factual, no urgency. */
    DAY_OF_YEAR,

    /** "252 days lived this year" — accumulation rather than depletion. */
    DAYS_LIVED,

    /** Nothing written at all; the bar carries it. */
    BAR_ONLY,

    /** "114 days left in 2026" — the v1 wording, kept as an option. */
    COUNTDOWN;

    companion object {
        fun from(name: String?): YearLineStyle =
            entries.firstOrNull { it.name == name } ?: DAY_OF_YEAR
    }
}

/**
 * Small is the size the card has always been. Medium and Large go up from
 * there — the scale only ever grows, because nobody has ever wanted this
 * smaller. Applied to every line on the card, the date and year line
 * included, not just the affirmation.
 */
enum class TextScale(val factor: Float) {
    SMALL(1.0f), MEDIUM(1.13f), LARGE(1.28f);

    companion object {
        fun from(name: String?): TextScale =
            entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}

/** Which pool the card is drawing from right now. */
enum class Voice { MORNING, EVENING }

/**
 * Everything the person has chosen, plus the words they've written or kept.
 * Plain SharedPreferences — small, synchronous, and readable from the widget
 * without any of the machinery a database would need.
 */
object Prefs {

    private const val FILE = "daily_light_prefs"
    private const val KEY_DARK_MODE = "dark_mode"

    fun isDarkMode(context: Context): Boolean =
        p(context).getBoolean(KEY_DARK_MODE, true)

    fun setDarkMode(context: Context, darkMode: Boolean) {
        p(context).edit().putBoolean(KEY_DARK_MODE, darkMode).apply()
    }

    private const val K_OFFSET = "shuffle_offset"
    private const val K_OFFSET_DATE = "shuffle_offset_date"
    private const val K_YEAR_LINE = "year_line"
    private const val K_SHOW_BAR = "show_bar"
    private const val K_EVENING_ON = "evening_enabled"
    private const val K_EVENING_HOUR = "evening_hour"
    private const val K_SHIFT_COLOURS = "shift_colours"
    private const val K_TEXT_SCALE = "text_scale"
    private const val K_CUSTOM = "custom_words"
    private const val K_FAV_MORNING = "fav_morning"
    private const val K_FAV_EVENING = "fav_evening"

    private fun p(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // --- the tap offset -------------------------------------------------
    // Reshuffling is deliberately a one-day thing: the offset is stamped with
    // the date it was made, so tomorrow returns to the day's own words rather
    // than inheriting yesterday's fidgeting.

    private fun today(): String = java.time.LocalDate.now().toString()

    fun offset(c: Context): Int =
        if (p(c).getString(K_OFFSET_DATE, null) == today()) p(c).getInt(K_OFFSET, 0) else 0

    fun bumpOffset(c: Context) {
        p(c).edit()
            .putInt(K_OFFSET, offset(c) + 1)
            .putString(K_OFFSET_DATE, today())
            .apply()
    }

    // --- appearance and behaviour ---------------------------------------

    fun yearLine(c: Context): YearLineStyle =
        YearLineStyle.from(p(c).getString(K_YEAR_LINE, null))

    fun setYearLine(c: Context, v: YearLineStyle) {
        p(c).edit().putString(K_YEAR_LINE, v.name).apply()
    }

    fun showBar(c: Context): Boolean = p(c).getBoolean(K_SHOW_BAR, true)
    fun setShowBar(c: Context, v: Boolean) { p(c).edit().putBoolean(K_SHOW_BAR, v).apply() }

    fun eveningEnabled(c: Context): Boolean = p(c).getBoolean(K_EVENING_ON, true)
    fun setEveningEnabled(c: Context, v: Boolean) {
        p(c).edit().putBoolean(K_EVENING_ON, v).apply()
    }

    /** Hour of day (0-23) the card turns to the closing voice. */
    fun eveningHour(c: Context): Int = p(c).getInt(K_EVENING_HOUR, 18)
    fun setEveningHour(c: Context, v: Int) {
        p(c).edit().putInt(K_EVENING_HOUR, v.coerceIn(12, 23)).apply()
    }

    fun shiftColours(c: Context): Boolean = p(c).getBoolean(K_SHIFT_COLOURS, true)
    fun setShiftColours(c: Context, v: Boolean) {
        p(c).edit().putBoolean(K_SHIFT_COLOURS, v).apply()
    }

    fun textScale(c: Context): TextScale = TextScale.from(p(c).getString(K_TEXT_SCALE, null))
    fun setTextScale(c: Context, v: TextScale) {
        p(c).edit().putString(K_TEXT_SCALE, v.name).apply()
    }

    // --- their own words -------------------------------------------------
    // Stored newline-separated so order is stable and the list is easy to
    // read back. Shown in both the morning and evening pools.

    fun customWords(c: Context): List<String> =
        p(c).getString(K_CUSTOM, "")
            .orEmpty()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun addCustomWord(c: Context, text: String) {
        val clean = text.trim().replace("\n", " ")
        if (clean.isEmpty()) return
        val all = customWords(c).toMutableList()
        if (all.any { it.equals(clean, ignoreCase = true) }) return
        all.add(clean)
        p(c).edit().putString(K_CUSTOM, all.joinToString("\n")).apply()
    }

    fun removeCustomWord(c: Context, text: String) {
        val all = customWords(c).filterNot { it == text }
        p(c).edit().putString(K_CUSTOM, all.joinToString("\n")).apply()
    }

    // --- kept favourites -------------------------------------------------
    // Tagged by voice so an evening line resurfaces in the evening.

    private fun favKey(voice: Voice) =
        if (voice == Voice.EVENING) K_FAV_EVENING else K_FAV_MORNING

    /** Sorted so selection from this list is deterministic across redraws. */
    fun favourites(c: Context, voice: Voice): List<String> =
        p(c).getStringSet(favKey(voice), emptySet()).orEmpty().sorted()

    fun isFavourite(c: Context, voice: Voice, text: String): Boolean =
        p(c).getStringSet(favKey(voice), emptySet()).orEmpty().contains(text)

    /** Returns true if the line is a favourite after the toggle. */
    fun toggleFavourite(c: Context, voice: Voice, text: String): Boolean {
        val key = favKey(voice)
        val current = p(c).getStringSet(key, emptySet()).orEmpty().toMutableSet()
        val nowKept = if (current.contains(text)) {
            current.remove(text); false
        } else {
            current.add(text); true
        }
        // A fresh set instance — SharedPreferences does not copy on write.
        p(c).edit().putStringSet(key, HashSet(current)).apply()
        return nowKept
    }

    fun removeFavourite(c: Context, voice: Voice, text: String) {
        val key = favKey(voice)
        val current = p(c).getStringSet(key, emptySet()).orEmpty().toMutableSet()
        current.remove(text)
        p(c).edit().putStringSet(key, HashSet(current)).apply()
    }
}
