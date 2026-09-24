package com.shamala.dailylight

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

/**
 * The sky at the top of the app screen. When the app opens it plays a
 * three-second sunrise in the morning voice, or a sunset in the evening one.
 *
 * Only ever runs while the screen is open, then stops: the widget itself never
 * animates. With animations turned off in the phone's settings it simply shows
 * the finished sky.
 *
 * Every gradient is built once per size and reused; a frame only changes
 * alphas and where the sun is, so the three seconds allocate nothing.
 */
class SkyHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Sunrise, or sunset. */
    var evening = false
        set(value) {
            field = value
            build(width, height)
            invalidate()
        }

    /** 0 = the start of the animation, 1 = where it comes to rest. */
    private var progress = 1f
    private var animator: ValueAnimator? = null

    private val density = resources.displayMetrics.density
    private val radius = 26f * density

    private val base = Paint()
    private val glow = Paint()
    private val night = Paint()
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG)
    private val disc = Paint(Paint.ANTI_ALIAS_FLAG)
    private val star = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ground = Paint()
    private val line = Paint()

    /** Its own rounded corners: clipToOutline only exists from Android 12. */
    private val corners = Path()

    /** The right half and the top strip only: the title sits on the left. */
    private val stars = listOf(
        0.58f to 0.07f, 0.67f to 0.30f, 0.78f to 0.12f, 0.88f to 0.38f,
        0.95f to 0.16f, 0.62f to 0.55f, 0.84f to 0.62f, 0.73f to 0.47f
    )

    /** Play from the beginning; [onWords] fires when the words should appear. */
    fun play(onWords: () -> Unit) {
        animator?.cancel()
        if (!ValueAnimator.areAnimatorsEnabled()) {
            progress = 1f
            invalidate()
            onWords()
            return
        }
        progress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DURATION_MS
            interpolator = if (evening) AccelerateInterpolator(1.3f) else DecelerateInterpolator(1.6f)
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
        postDelayed(onWords, WORDS_DELAY_MS)
    }

    /** Skip straight to the finished sky, as after a rotation. */
    fun showFinished() {
        animator?.cancel()
        progress = 1f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = build(w, h)

    private fun build(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        val horizon = h * HORIZON
        val sunX = w * SUN_X
        val warm = warmth()

        corners.reset()
        corners.addRoundRect(RectF(0f, 0f, w, h), CORNER_DP * density, CORNER_DP * density, Path.Direction.CW)

        base.shader = LinearGradient(
            0f, 0f, 0f, h,
            if (evening) intArrayOf(0xFF2A1E3E.toInt(), 0xFF6E4360.toInt(), 0xFFD98A7C.toInt())
            else intArrayOf(0xFF2B2650.toInt(), 0xFF5B4370.toInt(), 0xFFC98A7E.toInt()),
            floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP
        )
        glow.shader = RadialGradient(
            sunX, horizon, h * 0.95f,
            intArrayOf(argb(0.65f, warm), argb(0f, warm)), null, Shader.TileMode.CLAMP
        )
        night.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0E0F22.toInt(), 0xFF14162E.toInt(), 0xFF262445.toInt()),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        // Centred on the origin; drawn translated to wherever the sun is.
        halo.shader = RadialGradient(
            0f, 0f, radius * 2.6f,
            intArrayOf(argb(0.55f, warm), argb(0f, warm)), null, Shader.TileMode.CLAMP
        )
        disc.color = argb(1f, warm)
        star.color = Color.rgb(247, 241, 233)
        ground.shader = LinearGradient(
            0f, horizon, 0f, h,
            intArrayOf(argb(0.35f, 20, 18, 42), argb(0.8f, 20, 18, 42)), null, Shader.TileMode.CLAMP
        )
        line.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(argb(0f, 247, 221, 200), argb(0.6f, 247, 221, 200), argb(0f, 247, 221, 200)),
            null, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val horizon = h * HORIZON
        val sunX = w * SUN_X

        // How dark it is: sunrise goes from night to day, sunset the other way.
        val dark = if (evening) progress else 1f - progress

        canvas.save()
        canvas.clipPath(corners)

        canvas.drawRect(0f, 0f, w, h, base)
        glow.alpha = ((1f - dark * 0.8f) * 255).toInt()
        canvas.drawRect(0f, 0f, w, h, glow)
        night.alpha = (dark * 255).toInt()
        canvas.drawRect(0f, 0f, w, h, night)

        val starAlpha = (if (evening) (progress - 0.55f) / 0.45f else 1f - progress * 1.9f)
            .coerceIn(0f, 1f)
        if (starAlpha > 0f) {
            stars.forEachIndexed { i, (fx, fy) ->
                star.alpha = (starAlpha * (0.45f + 0.1f * (i % 4)) * 255).toInt()
                canvas.drawCircle(w * fx, h * fy, 1.2f * density, star)
            }
        }

        // The sun, right of centre so it never rises behind the title, and
        // cut off at the horizon.
        val low = horizon + radius * 1.2f
        val high = horizon - radius * 2.3f
        val sunY = if (evening) high + (low - high) * progress else low + (high - low) * progress
        canvas.save()
        canvas.clipRect(0f, 0f, w, horizon)
        canvas.translate(sunX, sunY)
        canvas.drawCircle(0f, 0f, radius * 2.6f, halo)
        canvas.drawCircle(0f, 0f, radius, disc)
        canvas.restore()

        // The ground below the horizon, darker so the words stay readable.
        canvas.drawRect(0f, horizon, w, h, ground)
        canvas.drawRect(0f, horizon, w, horizon + density, line)

        canvas.restore()
    }

    private fun warmth(): IntArray =
        if (evening) intArrayOf(244, 164, 143) else intArrayOf(247, 203, 166)

    private fun argb(alpha: Float, r: Int, g: Int, b: Int) =
        Color.argb((alpha.coerceIn(0f, 1f) * 255).toInt(), r, g, b)

    private fun argb(alpha: Float, rgb: IntArray) = argb(alpha, rgb[0], rgb[1], rgb[2])

    private companion object {
        const val DURATION_MS = 3000L
        const val WORDS_DELAY_MS = 1500L
        const val HORIZON = 0.8f
        const val SUN_X = 0.74f
        const val CORNER_DP = 28f
    }
}
