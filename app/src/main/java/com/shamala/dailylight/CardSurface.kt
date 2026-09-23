package com.shamala.dailylight

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView

/**
 * The handful of operations painting the card needs, so that [CardPainter] can
 * be written once against something it doesn't have to know the shape of.
 *
 * There are two places the card gets drawn — the widget, which can only be
 * addressed through [RemoteViews] across a Binder call, and the preview on the
 * app's own screen, which is an ordinary view tree. They used to be two
 * separate bodies of code, which meant the preview could tell you something
 * the home screen didn't do. This is the seam that keeps them honest.
 */
interface CardSurface {
    fun background(viewId: Int, drawableRes: Int)
    fun text(viewId: Int, value: CharSequence)
    fun textSizeSp(viewId: Int, sp: Float)
    fun textColour(viewId: Int, colour: Int)
    fun visible(viewId: Int, visible: Boolean)
    fun bitmap(viewId: Int, value: Bitmap)
    fun describe(viewId: Int, value: CharSequence)
}

/** The widget: every call is queued into the RemoteViews sent to the launcher. */
class RemoteViewsSurface(private val views: RemoteViews) : CardSurface {

    override fun background(viewId: Int, drawableRes: Int) =
        views.setInt(viewId, "setBackgroundResource", drawableRes)

    override fun text(viewId: Int, value: CharSequence) =
        views.setTextViewText(viewId, value)

    override fun textSizeSp(viewId: Int, sp: Float) =
        views.setTextViewTextSize(viewId, android.util.TypedValue.COMPLEX_UNIT_SP, sp)

    override fun textColour(viewId: Int, colour: Int) =
        views.setTextColor(viewId, colour)

    override fun visible(viewId: Int, visible: Boolean) =
        views.setViewVisibility(viewId, if (visible) View.VISIBLE else View.GONE)

    override fun bitmap(viewId: Int, value: Bitmap) =
        views.setImageViewBitmap(viewId, value)

    override fun describe(viewId: Int, value: CharSequence) =
        views.setContentDescription(viewId, value)
}

/** The preview on the settings screen: an ordinary inflated view tree. */
class ViewSurface(private val root: View) : CardSurface {

    override fun background(viewId: Int, drawableRes: Int) {
        root.findViewById<View>(viewId).setBackgroundResource(drawableRes)
    }

    override fun text(viewId: Int, value: CharSequence) {
        root.findViewById<TextView>(viewId).text = value
    }

    override fun textSizeSp(viewId: Int, sp: Float) {
        root.findViewById<TextView>(viewId)
            .setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sp)
    }

    override fun textColour(viewId: Int, colour: Int) {
        root.findViewById<TextView>(viewId).setTextColor(colour)
    }

    override fun visible(viewId: Int, visible: Boolean) {
        root.findViewById<View>(viewId).visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun bitmap(viewId: Int, value: Bitmap) {
        root.findViewById<ImageView>(viewId).setImageBitmap(value)
    }

    override fun describe(viewId: Int, value: CharSequence) {
        root.findViewById<View>(viewId).contentDescription = value
    }
}
