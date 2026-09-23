package com.shamala.dailylight

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue

/**
 * Draws the Lora lines into bitmaps.
 *
 * Why: a widget's layout is inflated inside the launcher's process, and some
 * launchers — Samsung's One UI Home among them — apply the phone's system
 * font to the text views regardless of the `@font/` the layout asks for. The
 * app's own screen keeps Lora because it inflates in our process; the widget
 * quietly loses it. Painting the text ourselves and handing over a bitmap is
 * the only way to guarantee the widget matches the preview.
 *
 * The weekday and year line are deliberately left as real text views — they
 * are set in the system sans face anyway, so there is nothing to protect.
 */
object CardRenderer {

    /** Widest bitmap we will produce, to keep RemoteViews memory sensible. */
    private const val MAX_WIDTH_PX = 1400

    /**
     * A RemoteViews payload crosses a Binder transaction, and the launcher
     * rejects a widget whose bitmaps exceed its budget — the widget then shows
     * as "Problem loading widget" with nothing we can catch. At 3x density a
     * 400dp card with five lines of Large text comfortably passes 2 MB, so the
     * render is scaled down to fit and the ImageView scales it back up. Only
     * the extreme sizes ever hit this, and softness beats a dead widget.
     */
    private const val MAX_BITMAP_BYTES = 1_500_000
    private const val BYTES_PER_PIXEL = 4

    /**
     * How far the words' type may step down to fit a short widget before the
     * ImageView's own scaling takes over. Below this the words stop being
     * comfortable to read at a glance, and a bigger widget is the answer.
     */
    private const val MIN_TYPE_FIT = 0.7f

    /** Factor to shrink a w x h render by so it lands inside the budget. */
    private fun fitScale(width: Int, height: Int): Float {
        val bytes = width.toLong() * height.toLong() * BYTES_PER_PIXEL
        if (bytes <= MAX_BITMAP_BYTES) return 1f
        return kotlin.math.sqrt(MAX_BITMAP_BYTES.toDouble() / bytes.toDouble())
            .toFloat()
            .coerceAtLeast(0.35f)
    }

    fun font(context: Context, resId: Int): Typeface? =
        runCatching { context.resources.getFont(resId) }.getOrNull()

    private fun sp(context: Context, value: Float): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, value, context.resources.displayMetrics
        )

    private fun dp(context: Context, value: Float): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics
        )

    private fun paint(
        context: Context,
        typeface: Typeface?,
        sizeSp: Float,
        colour: Int
    ) = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = sp(context, sizeSp)
        color = colour
        isSubpixelText = true
    }

    private fun layout(
        text: String,
        paint: TextPaint,
        widthPx: Int,
        maxLines: Int,
        extraPx: Float,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
            .setAlignment(alignment)
            .setLineSpacing(extraPx, 1f)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

    /** The date line, on its own. */
    fun date(context: Context, text: String, widthPx: Int, sizeSp: Float, isDark: Boolean = true): Bitmap? {
        if (text.isEmpty()) return null
        val width = widthPx.coerceIn(1, MAX_WIDTH_PX)
        val colour = context.getColor(if (isDark) R.color.cream_dim else R.color.text_dark_dim)

        fun render(factor: Float): StaticLayout {
            val p = paint(context, font(context, R.font.lora_medium), sizeSp * factor, colour)
            return layout(text, p, (width * factor).toInt().coerceAtLeast(1), 1, 0f)
        }

        var l = render(1f)
        val factor = fitScale(width, l.height)
        if (factor < 1f) l = render(factor)

        return draw((width * factor).toInt(), l.height) { canvas -> l.draw(canvas) }
    }

    /**
     * Affirmation and thought together in one bitmap — one image view, one
     * allocation, and the gap between them stays exactly where it was.
     *
     * @param maxHeightPx the room the words get on the card. Past it the type
     *   steps down and re-wraps at the full width, rather than leaving the
     *   ImageView to shrink the finished picture.
     */
    fun words(
        context: Context,
        affirmation: String,
        thought: String,
        widthPx: Int,
        scale: TextScale,
        isDark: Boolean = true,
        maxHeightPx: Int = Int.MAX_VALUE
    ): Bitmap? {
        val width = widthPx.coerceIn(1, MAX_WIDTH_PX)
        if (affirmation.isEmpty() && thought.isEmpty()) return null

        val affColour = context.getColor(if (isDark) R.color.cream else R.color.text_dark)
        val thoColour = context.getColor(if (isDark) R.color.muted else R.color.text_muted_dark)

        // One render pass. [type] shrinks only the type, re-wrapping it across
        // the full width; [factor] shrinks everything — width, type and gap —
        // together, so a memory-scaled render is the same picture at a smaller
        // pixel count, not a different layout.
        fun render(type: Float, factor: Float): Triple<StaticLayout, StaticLayout, Float> {
            val w = (width * factor).toInt().coerceAtLeast(1)
            val size = type * factor
            val affPaint = paint(
                context, font(context, R.font.lora_italic),
                DailyContent.affirmationSizeSp(affirmation, scale) * size, affColour
            )
            val thoPaint = paint(
                context, font(context, R.font.lora_regular),
                DailyContent.thoughtSizeSp(thought, scale) * size, thoColour
            )
            return Triple(
                layout(affirmation, affPaint, w, 5, dp(context, 5f) * size, Layout.Alignment.ALIGN_CENTER),
                layout(thought, thoPaint, w, 3, dp(context, 3f) * size, Layout.Alignment.ALIGN_CENTER),
                dp(context, 14f) * size
            )
        }

        fun Triple<StaticLayout, StaticLayout, Float>.height() =
            (first.height + third + second.height).toInt()

        // A short widget has less room than the words want. The ImageView would
        // shrink the whole bitmap to fit, which narrows the block into a column
        // in the middle of a full-width card. Stepping the type down instead
        // keeps the lines running the full width, as they do in the header.
        var type = 1f
        var rendered = render(type, 1f)
        while (rendered.height() > maxHeightPx && type > MIN_TYPE_FIT) {
            type = (type - 0.05f).coerceAtLeast(MIN_TYPE_FIT)
            rendered = render(type, 1f)
        }

        var (affLayout, thoLayout, gap) = rendered
        var factor = fitScale(width, rendered.height())
        if (factor < 1f) {
            val scaled = render(type, factor)
            affLayout = scaled.first
            thoLayout = scaled.second
            gap = scaled.third
        } else {
            factor = 1f
        }

        val height = (affLayout.height + gap + thoLayout.height).toInt()

        return draw((width * factor).toInt(), height) { canvas ->
            affLayout.draw(canvas)
            canvas.translate(0f, affLayout.height + gap)
            thoLayout.draw(canvas)
        }
    }

    /**
     * The year bar.
     *
     * Painted rather than left to a ProgressBar because a RemoteViews cannot
     * swap a progress drawable, and cannot tint one below API 31. The layout
     * used to carry four bars — light and dark, inline and full — switched by
     * visibility, which meant every colour decision existed in four places.
     * One ImageView per position, coloured here, is the same picture with the
     * duplication gone.
     *
     * [progress] is 0..1000, matching [DayContent.yearProgress].
     */
    fun bar(
        context: Context,
        progress: Int,
        widthPx: Int,
        heightPx: Int,
        isDark: Boolean
    ): Bitmap? {
        val width = widthPx.coerceIn(1, MAX_WIDTH_PX)
        val height = heightPx.coerceAtLeast(1)
        val radius = height / 2f

        val track = context.getColor(if (isDark) R.color.bar_track else R.color.bar_track_light)
        val fill = context.getColor(if (isDark) R.color.bar_fill else R.color.bar_fill_light)

        // A first-of-January bar should still read as a bar, not as a dot of
        // colour the eye cannot resolve, so the fill never falls below its own
        // corner radius.
        val fraction = progress.coerceIn(0, 1000) / 1000f
        val fillWidth = (width * fraction).coerceIn(
            if (progress > 0) height.toFloat() else 0f,
            width.toFloat()
        )

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        return draw(width, height) { canvas ->
            p.color = track
            canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), radius, radius, p)
            if (fillWidth > 0f) {
                p.color = fill
                canvas.drawRoundRect(RectF(0f, 0f, fillWidth, height.toFloat()), radius, radius, p)
            }
        }
    }

    private fun draw(width: Int, height: Int, block: (Canvas) -> Unit): Bitmap? {
        if (width <= 0 || height <= 0) return null
        return runCatching {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            block(Canvas(bitmap))
            bitmap
        }.getOrNull()
    }
}
