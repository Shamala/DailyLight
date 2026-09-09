package com.shamala.dailylight

import android.app.Activity
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.TextView

/**
 * A small home for the app icon: shows today's card and how to get the
 * widget onto the home screen. Not required for the widget to work.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<android.view.View>(R.id.widget_root).setOnClickListener {
            DailyContent.bumpOffset(this)
            render()
            DailyWidgetProvider.refreshAll(this)
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val day = DailyContent.forToday(this)

        findViewById<TextView>(R.id.tv_weekday).text = day.weekday
        findViewById<TextView>(R.id.tv_date).text = day.date
        findViewById<TextView>(R.id.tv_affirmation).text = day.affirmation
        findViewById<TextView>(R.id.tv_thought).text = day.thought

        val countdown = SpannableString(day.countdown)
        val digits = Regex("\\d+").find(day.countdown)
        val range = digits?.range ?: day.countdown.indices
        countdown.setSpan(
            ForegroundColorSpan(getColor(R.color.accent)),
            range.first,
            range.last + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        findViewById<TextView>(R.id.tv_countdown).text = countdown
    }
}
