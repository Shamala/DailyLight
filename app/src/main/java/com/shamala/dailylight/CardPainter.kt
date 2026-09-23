package com.shamala.dailylight

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
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

    /** Top and bottom padding in widget_daily.xml, together. */
    const val VERTICAL_PADDING_DP = 40

    /** words_zone's image sits this far below the rule. */
    private const val WORDS_MARGIN_TOP_DP = 15f

    /**
     * Slack for the header estimate below: a launcher that swaps in its own
     * sans face can set the weekday line a pixel or two taller than ours.
     */
    private const val HEADER_SLACK_DP = 4f

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
     * @param contentHeightPx the height inside the card's padding, or null
     *   where the card grows to fit its words, as the preview does.
     */
    fun paint(
        context: Context,
        surface: CardSurface,
        contentWidthPx: Int,
        contentHeightPx: Int? = null
    ) {
        val day = DailyContent.now(context)
        val scale = Prefs.textScale(context)
        val isDark = Prefs.isDarkMode(context)

        paintBackground(context, surface, day, isDark)
        val dateHeightPx = paintHeader(context, surface, day, scale, isDark, contentWidthPx)
        val wordsHeightPx = contentHeightPx?.let {
            it - headerHeightPx(context, day, scale, dateHeightPx) -
                px(context, WORDS_MARGIN_TOP_DP)
        } ?: Int.MAX_VALUE
        paintWords(context, surface, day, scale, isDark, contentWidthPx, wordsHeightPx)
        paintYear(context, surface, day, scale, isDark, contentWidthPx)
        describe(context, surface, day)
    }

    /**
     * How tall the header zone will lay out, so the words know what is left.
     * RemoteViews can't be measured from here, so this adds up the same
     * pieces widget_daily.xml stacks: the weekday row, the date, the
     * full-width bar when it shows, and the rule.
     */
    private fun headerHeightPx(
        context: Context, day: DayContent, scale: TextScale, dateHeightPx: Int
    ): Int {
        fun lineHeight(family: String, sizeSp: Float): Int {
            val paint = Paint().apply {
                typeface = Typeface.create(family, Typeface.NORMAL)
                textSize = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, sizeSp, context.resources.displayMetrics
                )
            }
            // A TextView keeps its font padding by default, so top to bottom.
            return paint.fontMetricsInt.let { it.bottom - it.top }
        }

        val showBar = Prefs.showBar(context)
        val inlineYear = showBar && day.yearLine.isNotEmpty()
        val fullBar = showBar && day.yearLine.isEmpty()

        var weekdayRow = lineHeight("sans-serif-light", DailyContent.weekdaySizeSp(scale))
        if (inlineYear) {
            weekdayRow = maxOf(
                weekdayRow,
                lineHeight("sans-serif-medium", DailyContent.yearLineSizeSp(scale)),
                px(context, INLINE_BAR_HEIGHT_DP)
            )
        }

        return weekdayRow +
            px(context, 4f) + dateHeightPx +
            (if (fullBar) px(context, 12f + FULL_BAR_HEIGHT_DP) else 0) +
            px(context, 18f + 1f) +
            px(context, HEADER_SLACK_DP)
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
    ): Int {
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
            return bitmap.height
        } else {
            surface.visible(R.id.img_date, false)
            surface.visible(R.id.tv_date, true)
            surface.text(R.id.tv_date, day.date)
            surface.textSizeSp(R.id.tv_date, DailyContent.dateSizeSp(scale))
            surface.textColour(
                R.id.tv_date,
                context.getColor(if (isDark) R.color.cream_dim else R.color.text_dark_dim)
            )
            return (TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, DailyContent.dateSizeSp(scale),
                context.resources.displayMetrics
            ) * 1.4f).toInt()
        }
    }

    private fun paintWords(
        context: Context,
        surface: CardSurface,
        day: DayContent,
        scale: TextScale,
        isDark: Boolean,
        widthPx: Int,
        maxHeightPx: Int
    ) {
        val bitmap = CardRenderer.words(
            context, day.affirmation, day.thought, widthPx, scale, isDark, maxHeightPx
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
