package com.shamala.dailylight

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.TypedValue
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * The sky behind the words: a sun that rises bottom left at 06:00, crosses
 * the top of the card and sets bottom right at 19:00, then a small moon and a
 * few stars until morning. The disc shows at sunrise, sunset and midday; the
 * rest of the day its glow carries the light across, behind the words.
 *
 * The card's colour still comes from the phase backgrounds; this only adds
 * the light. It is painted at a third of the card's size and scaled up by the
 * ImageView — soft glows lose nothing to that, and it keeps the bitmap near
 * 0.2 MB on top of what the widget already sends the launcher.
 *
 * The widget redraws every 30 minutes, so the sun moves a little between
 * glances; nothing here animates, which is what keeps it free on battery.
 */
object SkyPainter {

    /** Painted at 1/SCALE of the card, then stretched back up. */
    private const val SCALE = 3

    private const val SUNRISE_MINUTE = 6 * 60
    private const val SUNSET_MINUTE = 19 * 60

    /**
     * How far above the bottom edge the horizon sits. Inside the card's 20dp
     * bottom padding, so the line never runs through the words, however far
     * down a long thought reaches.
     */
    private const val HORIZON_FROM_BOTTOM_DP = 11f

    /** Matches the 28dp corners of the widget backgrounds. */
    private const val CORNER_DP = 28f

    /**
     * Only in the margins — the top band, the side padding and the strip
     * below the words — so none can read as a stray full stop in the text.
     */
    private val STARS = listOf(
        0.30f to 0.05f, 0.64f to 0.06f, 0.035f to 0.42f, 0.965f to 0.34f,
        0.97f to 0.74f, 0.03f to 0.82f, 0.36f to 0.955f, 0.68f to 0.95f
    )

    /**
     * @param widthPx the whole card, padding included
     * @param heightPx the whole card, padding included
     */
    fun paint(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        isDark: Boolean,
        at: LocalTime = LocalTime.now()
    ): Bitmap? {
        val w = widthPx / SCALE
        val h = heightPx / SCALE
        if (w <= 0 || h <= 0) return null

        val bitmap = runCatching {
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        }.getOrNull() ?: return null
        val canvas = Canvas(bitmap)
        val density = context.resources.displayMetrics.density / SCALE
        fun dp(v: Float) = v * density

        // Stay inside the card's rounded corners: the ImageView sits on top
        // of the background and would otherwise square them off.
        val corner = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, CORNER_DP, context.resources.displayMetrics
        ) / SCALE
        canvas.clipPath(Path().apply {
            addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), corner, corner, Path.Direction.CW)
        })

        val horizonY = h - dp(HORIZON_FROM_BOTTOM_DP)
        val minute = at.hour * 60 + at.minute
        // 0 at sunrise, 1 at sunset; outside that the sun is below the horizon.
        val t = (minute - SUNRISE_MINUTE).toFloat() / (SUNSET_MINUTE - SUNRISE_MINUTE)

        val night = when {
            t < 0f -> ((-t - 0.04f) / 0.06f).coerceIn(0f, 1f)
            t > 1f -> ((t - 1.04f) / 0.06f).coerceIn(0f, 1f)
            else -> 0f
        }
        if (night > 0f) drawNight(canvas, w, h, night, isDark, ::dp)

        // Twilight: the sun has gone but its glow still sits on the horizon.
        val twilight = when {
            t < 0f -> (1f - -t / 0.1f).coerceIn(0f, 1f)
            t > 1f -> (1f - (t - 1f) / 0.1f).coerceIn(0f, 1f)
            else -> 1f
        }
        if (twilight > 0f) drawSun(canvas, w, h, horizonY, t, twilight, isDark, ::dp)

        drawHorizon(canvas, w, horizonY, isDark, ::dp)
        return bitmap
    }

    private fun drawSun(
        canvas: Canvas, w: Int, h: Int, horizonY: Float,
        t: Float, strength: Float, isDark: Boolean, dp: (Float) -> Float
    ) {
        val along = t.coerceIn(0f, 1f)
        // How high it is: 0 on the horizon, 1 overhead.
        val elevation = sin(PI * along).toFloat()
        // Low sun is warm and bright; high sun is paler and quieter. Morning
        // leans peach, evening leans rose.
        val warm = 1f - elevation
        val radius = dp(lerpF(11f, 13f, warm))

        val x = w * (0.14f + 0.72f * along)
        // Overhead it sits in the gap between the weekday and the year line,
        // whole, not cut off by the card's top edge.
        val topY = max(h * 0.07f, radius + dp(6f))
        val y = if (t in 0f..1f) {
            horizonY - elevation * (horizonY - topY)
        } else {
            horizonY + dp(10f) // just under the horizon at twilight
        }

        val low = if (t < 0.5f) intArrayOf(240, 176, 141) else intArrayOf(240, 150, 130)
        val high = intArrayOf(250, 232, 196)
        val glow = IntArray(3) { lerp(high[it], low[it], warm) }
        val glowAlpha = (if (isDark) lerpF(0.2f, 0.55f, warm) else lerpF(0.30f, 0.50f, warm)) * strength

        val glowRadius = h * lerpF(0.55f, 0.75f, warm)
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                x, y, glowRadius,
                intArrayOf(argb(glowAlpha, glow), argb(0f, glow)),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(x, y, glowRadius, glowPaint)

        if (t !in 0f..1f) return

        // The disc only where the card has room for it: within a sun's width
        // of the horizon at sunrise and sunset — measured in distance, so a
        // short card doesn't lift it into the words — and in the gap at the
        // top around midday. In between, only the glow travels across.
        val onHorizon = 1f - smoothstep(radius * 1.0f, radius * 2.4f, horizonY - y)
        val overhead = smoothstep(0.94f, 0.985f, elevation)
        val shown = max(onHorizon, overhead)
        if (shown <= 0f) return

        // The disc itself, cut off at the horizon so it rises and sets.
        val disc = if (isDark) {
            IntArray(3) { lerp(intArrayOf(252, 236, 205)[it], if (t < 0.5f) intArrayOf(247, 203, 166)[it] else intArrayOf(244, 164, 143)[it], warm) }
        } else {
            intArrayOf(250, 215, 170)
        }
        val discAlpha = lerpF(0.8f, 1f, warm) * shown
        canvas.save()
        canvas.clipRect(0f, 0f, w.toFloat(), horizonY)
        canvas.drawCircle(x, y, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                x, y, radius,
                intArrayOf(argb(discAlpha, disc), argb(discAlpha, disc), argb(0f, disc)),
                floatArrayOf(0f, 0.72f, 1f), Shader.TileMode.CLAMP
            )
        })
        canvas.restore()
    }

    private fun drawNight(
        canvas: Canvas, w: Int, h: Int, strength: Float, isDark: Boolean, dp: (Float) -> Float
    ) {
        val light = if (isDark) intArrayOf(232, 228, 242) else intArrayOf(86, 80, 129)

        // A crescent: one circle with a second, offset one taken out of it.
        // Top centre, in the gap between the weekday and the year line.
        val cx = w * 0.5f
        val cy = dp(26f)
        val r = dp(9f)
        val moon = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
        val bite = Path().apply { addCircle(cx - r * 0.55f, cy - r * 0.3f, r * 0.9f, Path.Direction.CW) }
        moon.op(bite, Path.Op.DIFFERENCE)
        canvas.drawPath(moon, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = argb(0.9f * strength, light)
        })

        val star = Paint(Paint.ANTI_ALIAS_FLAG)
        STARS.forEachIndexed { i, (fx, fy) ->
            star.color = argb((0.35f + 0.07f * (i % 5)) * strength, light)
            canvas.drawCircle(w * fx, h * fy, max(dp(1.1f), 0.8f), star)
        }
    }

    private fun drawHorizon(canvas: Canvas, w: Int, y: Float, isDark: Boolean, dp: (Float) -> Float) {
        val line = if (isDark) intArrayOf(247, 241, 233) else intArrayOf(31, 28, 60)
        val alpha = if (isDark) 0.22f else 0.14f
        val inset = dp(22f)
        canvas.drawRect(inset, y, w - inset, y + max(dp(1f), 1f), Paint().apply {
            shader = LinearGradient(
                inset, 0f, w - inset, 0f,
                intArrayOf(argb(0f, line), argb(alpha, line), argb(0f, line)),
                null, Shader.TileMode.CLAMP
            )
        })
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val f = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return f * f * (3f - 2f * f)
    }

    private fun lerp(a: Int, b: Int, f: Float): Int = (a + (b - a) * f).toInt()
    private fun lerpF(a: Float, b: Float, f: Float): Float = a + (b - a) * f
    private fun argb(alpha: Float, rgb: IntArray): Int =
        Color.argb((alpha.coerceIn(0f, 1f) * 255).toInt(), rgb[0], rgb[1], rgb[2])
}
