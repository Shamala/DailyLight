package com.shamala.dailylight

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue

/**
 * How the card looks. The only place that decides it.
 *
 * Both the widget and the preview on the settings screen call [paint] against
 * their own [CardSurface], so a change to the design lands in both at once and
 * the preview cannot quietly disagree with the home screen.
 */
object CardPainter {

    /** Side padding in widget_daily.xml, both sides. */
    const val HORIZONTAL_PADDING_DP = 44

    /** Height of the small bar beside the year line, and of the full-width one. */
    private const val INLINE_BAR_WIDTH_DP = 28f
    private const val INLINE_BAR_HEIGHT_DP = 5f
    private const val FULL_BAR_HEIGHT_DP = 4f

    private fun px(context: Context, dp: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics
        ).toInt()

    /**
     * @param contentWidthPx the usable width inside the card's padding, which
     *   the Lora bitmaps are laid out against. The widget takes it from the
     *   launcher's reported size; the preview from its own measured width.
     */
    fun paint(context: Context, surface: CardSurface, contentWidthPx: Int) {
        val day = DailyContent.now(context)
        val scale = Prefs.textScale(context)
        val isDark = Prefs.isDarkMode(context)

        paintBackground(context, surface, day, isDark)
        paintHeader(context, surface, day, scale, isDark, contentWidthPx)
        paintWords(context, surface, day, scale, isDark, contentWidthPx)
        paintYear(context, surface, day, scale, isDark, contentWidthPx)
        describe(context, surface, day)
    }

    private fun paintBackground(
        context: Context, surface: CardSurface, day: DayContent, isDark: Boolean
    ) {
        val background =
            if (Prefs.shiftColours(context)) {
                backgroundFor(day.phase, isDark)
            } else {
                if (isDark) R.drawable.widget_bg_dawn else R.drawable.widget_bg_dawn_light
            }
        surface.background(R.id.widget_root, background)
    }

    private fun paintHeader(
        context: Context,
        surface: CardSurface,
        day: DayContent,
        scale: TextScale,
        isDark: Boolean,
        widthPx: Int
    ) {
        surface.text(R.id.tv_weekday, day.weekday)
        surface.textSizeSp(R.id.tv_weekday, DailyContent.weekdaySizeSp(scale))
        surface.textColour(
            R.id.tv_weekday,
            context.getColor(if (isDark) R.color.day_label else R.color.label_dark)
        )

        // The date is painted in Lora rather than set as text, because the
        // launcher inflates the widget in its own process and some launchers
        // substitute the system font for the one the layout asks for. The text
        // view stays in the layout as the fallback if a bitmap can't be made.
        val bitmap = CardRenderer.date(
            context, day.date, widthPx, DailyContent.dateSizeSp(scale), isDark
        )
        if (bitmap != null) {
            surface.bitmap(R.id.img_date, bitmap)
            surface.visible(R.id.img_date, true)
            surface.visible(R.id.tv_date, false)
        } else {
            surface.visible(R.id.img_date, false)
            surface.visible(R.id.tv_date, true)
            surface.text(R.id.tv_date, day.date)
            surface.textSizeSp(R.id.tv_date, DailyContent.dateSizeSp(scale))
            surface.textColour(
                R.id.tv_date,
                context.getColor(if (isDark) R.color.cream_dim else R.color.text_dark_dim)
            )
        }
    }

    private fun paintWords(
        context: Context,
        surface: CardSurface,
        day: DayContent,
        scale: TextScale,
        isDark: Boolean,
        widthPx: Int
    ) {
        val bitmap = CardRenderer.words(
            context, day.affirmation, day.thought, widthPx, scale, isDark
        )
        if (bitmap != null) {
            surface.bitmap(R.id.img_words, bitmap)
            surface.visible(R.id.img_words, true)
            surface.visible(R.id.tv_affirmation, false)
            surface.visible(R.id.tv_thought, false)
        } else {
            surface.visible(R.id.img_words, false)
            surface.visible(R.id.tv_affirmation, true)
            surface.visible(R.id.tv_thought, true)
            surface.text(R.id.tv_affirmation, day.affirmation)
            surface.text(R.id.tv_thought, day.thought)
            surface.textSizeSp(
                R.id.tv_affirmation, DailyContent.affirmationSizeSp(day.affirmation, scale)
            )
            surface.textSizeSp(
                R.id.tv_thought, DailyContent.thoughtSizeSp(day.thought, scale)
            )
            surface.textColour(
                R.id.tv_affirmation,
                context.getColor(if (isDark) R.color.cream else R.color.text_dark)
            )
            surface.textColour(
                R.id.tv_thought,
                context.getColor(if (isDark) R.color.muted else R.color.text_muted_dark)
            )
        }
    }

    /**
     * Two shapes, chosen by whether there is a year line to write: a small bar
     * beside the text up in the header, or a full-width rule under the date
     * carrying it alone.
     */
    private fun paintYear(
        context: Context,
        surface: CardSurface,
        day: DayContent,
        scale: TextScale,
        isDark: Boolean,
        widthPx: Int
    ) {
        val showText = day.yearLine.isNotEmpty()
        val showBar = Prefs.showBar(context)

        surface.visible(R.id.ll_year_inline, false)
        surface.visible(R.id.fl_year_full, false)
        if (!showBar) return

        if (showText) {
            surface.visible(R.id.ll_year_inline, true)
            surface.text(R.id.tv_yearline, accented(context, day.yearLine, isDark))
            surface.textSizeSp(R.id.tv_yearline, DailyContent.yearLineSizeSp(scale))
            surface.textColour(
                R.id.tv_yearline,
                context.getColor(if (isDark) R.color.muted else R.color.text_muted_dark)
            )
            CardRenderer.bar(
                context, day.yearProgress,
                px(context, INLINE_BAR_WIDTH_DP), px(context, INLINE_BAR_HEIGHT_DP), isDark
            )?.let { surface.bitmap(R.id.img_year_inline, it) }
        } else {
            surface.visible(R.id.fl_year_full, true)
            CardRenderer.bar(
                context, day.yearProgress,
                widthPx.coerceAtLeast(1), px(context, FULL_BAR_HEIGHT_DP), isDark
            )?.let { surface.bitmap(R.id.fl_year_full, it) }
        }
    }

    /**
     * The card is painted into bitmaps, which carry no text, so the words have
     * to be attached to the two tap zones by hand or a screen reader finds an
     * empty widget.
     */
    private fun describe(context: Context, surface: CardSurface, day: DayContent) {
        surface.describe(
            R.id.header_zone,
            buildString {
                append(day.weekday).append(", ").append(day.date)
                if (day.yearLine.isNotEmpty()) append(". ").append(day.yearLine)
                append(". ").append(context.getString(R.string.open_settings_action))
            }
        )
        surface.describe(
            R.id.words_zone,
            context.getString(
                R.string.card_description,
                day.affirmation, day.thought, context.getString(R.string.cycle_action)
            )
        )
    }

    fun backgroundFor(phase: Phase, isDark: Boolean): Int = if (isDark) {
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

    /** Warms the number, so the eye lands on it without it shouting. */
    fun accented(context: Context, text: String, isDark: Boolean): CharSequence {
        val span = SpannableString(text)
        if (text.isEmpty()) return span
        val colour = context.getColor(if (isDark) R.color.accent else R.color.accent_dark)
        val range = Regex("\\d+").find(text)?.range ?: text.indices
        span.setSpan(
            ForegroundColorSpan(colour),
            range.first, range.last + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return span
    }
}
