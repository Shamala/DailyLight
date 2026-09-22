package com.shamala.dailylight

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
        val width = widthPx.coerceIn(1, MAX_WIDTH_PX)
        val p = paint(
            context, font(context, R.font.lora_medium), sizeSp,
            context.getColor(if (isDark) R.color.cream_dim else R.color.text_dark_dim)
        )
        val l = layout(text, p, width, 1, 0f)
        return draw(width, l.height) { canvas -> l.draw(canvas) }
    }

    /**
     * Affirmation and thought together in one bitmap — one image view, one
     * allocation, and the gap between them stays exactly where it was.
     */
    fun words(
        context: Context,
        affirmation: String,
        thought: String,
        widthPx: Int,
        scale: TextScale,
        isDark: Boolean = true
    ): Bitmap? {
        val width = widthPx.coerceIn(1, MAX_WIDTH_PX)

        val affPaint = paint(
            context, font(context, R.font.lora_italic),
            DailyContent.affirmationSizeSp(affirmation, scale),
            context.getColor(if (isDark) R.color.cream else R.color.text_dark)
        )
        val thoPaint = paint(
            context, font(context, R.font.lora_regular),
            DailyContent.thoughtSizeSp(thought, scale),
            context.getColor(if (isDark) R.color.muted else R.color.text_muted_dark)
        )

        val affLayout = layout(affirmation, affPaint, width, 5, dp(context, 5f), Layout.Alignment.ALIGN_CENTER)
        val thoLayout = layout(thought, thoPaint, width, 3, dp(context, 3f), Layout.Alignment.ALIGN_CENTER)

        val gap = dp(context, 14f)
        val height = (affLayout.height + gap + thoLayout.height).toInt()

        return draw(width, height) { canvas ->
            affLayout.draw(canvas)
            canvas.translate(0f, affLayout.height + gap)
            thoLayout.draw(canvas)
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
