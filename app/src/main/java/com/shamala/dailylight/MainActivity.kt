package com.shamala.dailylight

import android.app.Activity
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/**
 * The settings screen and word editor. Shows the card exactly as the widget
 * draws it, so any change is visible before it reaches the home screen.
 *
 * Deliberately plain framework views — no AndroidX — so the project stays
 * dependency-free and builds without resolving anything beyond the plugin.
 */
class MainActivity : Activity() {

    /** Set while render() is writing control state, so listeners stay quiet. */
    private var binding = false

    private val firstEveningHour = 12

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        wireControls()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    // ------------------------------------------------------------------
    // Wiring
    // ------------------------------------------------------------------

    private fun wireControls() {
        findViewById<View>(R.id.btn_another).setOnClickListener {
            Prefs.bumpOffset(this)
            applied()
        }

        findViewById<View>(R.id.btn_keep).setOnClickListener {
            val day = DailyContent.now(this)
            Prefs.toggleFavourite(this, day.voice, day.affirmation)
            applied()
        }

        findViewById<View>(R.id.btn_add_word).setOnClickListener {
            val field = findViewById<EditText>(R.id.et_new_word)
            val text = field.text.toString()
            if (text.isBlank()) return@setOnClickListener
            Prefs.addCustomWord(this, text)
            field.setText("")
            field.clearFocus()
            applied()
        }

        findViewById<RadioGroup>(R.id.rg_year_line)
            .setOnCheckedChangeListener { _, checkedId ->
                if (binding) return@setOnCheckedChangeListener
                Prefs.setYearLine(
                    this,
                    when (checkedId) {
                        R.id.rb_days_lived -> YearLineStyle.DAYS_LIVED
                        R.id.rb_bar_only -> YearLineStyle.BAR_ONLY
                        R.id.rb_countdown -> YearLineStyle.COUNTDOWN
                        else -> YearLineStyle.DAY_OF_YEAR
                    }
                )
                applied()
            }

        findViewById<RadioGroup>(R.id.rg_text_size)
            .setOnCheckedChangeListener { _, checkedId ->
                if (binding) return@setOnCheckedChangeListener
                Prefs.setTextScale(
                    this,
                    when (checkedId) {
                        R.id.rb_small -> TextScale.SMALL
                        R.id.rb_large -> TextScale.LARGE
                        else -> TextScale.MEDIUM
                    }
                )
                applied()
            }

        findViewById<Switch>(R.id.sw_bar).setOnCheckedChangeListener { _, on ->
            if (binding) return@setOnCheckedChangeListener
            Prefs.setShowBar(this, on)
            applied()
        }

        findViewById<Switch>(R.id.sw_dark_mode).setOnCheckedChangeListener { _, on ->
            if (binding) return@setOnCheckedChangeListener
            Prefs.setDarkMode(this, on)
            applied()
        }

        findViewById<Switch>(R.id.sw_evening).setOnCheckedChangeListener { _, on ->
            if (binding) return@setOnCheckedChangeListener
            Prefs.setEveningEnabled(this, on)
            applied()
        }

        findViewById<Switch>(R.id.sw_shift).setOnCheckedChangeListener { _, on ->
            if (binding) return@setOnCheckedChangeListener
            Prefs.setShiftColours(this, on)
            applied()
        }

        findViewById<SeekBar>(R.id.sb_evening_hour)
            .setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                    if (binding || !fromUser) return
                    Prefs.setEveningHour(this@MainActivity, firstEveningHour + value)
                    applied()
                }

                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) = Unit
            })
    }

    /** A change was made: redraw here and push it to the home screen. */
    private fun applied() {
        render()
        DailyWidgetProvider.refreshAll(this)
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private fun render() {
        binding = true
        val day = DailyContent.now(this)
        val scale = Prefs.textScale(this)

        // --- the card, drawn the same way the widget draws it ---

        val isDark = Prefs.isDarkMode(this)

        val background =
            if (Prefs.shiftColours(this)) {
                DailyWidgetProvider.backgroundFor(day.phase, isDark)
            } else {
                if (isDark) R.drawable.widget_bg_dawn else R.drawable.widget_bg_dawn_light
            }
        findViewById<View>(R.id.widget_root).setBackgroundResource(background)

        val weekday = findViewById<TextView>(R.id.tv_weekday)
        weekday.text = day.weekday
        weekday.setTextColor(getColor(if (isDark) R.color.day_label else R.color.label_dark))
        weekday.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, DailyContent.weekdaySizeSp(scale)
        )

        val date = findViewById<TextView>(R.id.tv_date)
        date.text = day.date
        date.setTextColor(getColor(if (isDark) R.color.cream_dim else R.color.text_dark_dim))
        date.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, DailyContent.dateSizeSp(scale)
        )

        val affirmation = findViewById<TextView>(R.id.tv_affirmation)
        affirmation.text = day.affirmation
        affirmation.setTextColor(getColor(if (isDark) R.color.cream else R.color.text_dark))
        affirmation.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            DailyContent.affirmationSizeSp(day.affirmation, scale)
        )

        val thought = findViewById<TextView>(R.id.tv_thought)
        thought.text = day.thought
        thought.setTextColor(getColor(if (isDark) R.color.muted else R.color.text_muted_dark))
        thought.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            DailyContent.thoughtSizeSp(day.thought, scale)
        )

        val yearLine = findViewById<TextView>(R.id.tv_yearline)
        yearLine.setTextColor(getColor(if (isDark) R.color.muted else R.color.text_muted_dark))
        yearLine.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, DailyContent.yearLineSizeSp(scale)
        )

        val showText = day.yearLine.isNotEmpty()
        val showBar = Prefs.showBar(this)

        val llInline = findViewById<View>(R.id.ll_year_inline)
        val flFull = findViewById<View>(R.id.fl_year_full)
        val pbInlineDark = findViewById<ProgressBar>(R.id.pb_year_inline)
        val pbInlineLight = findViewById<ProgressBar>(R.id.pb_year_light_inline)
        val pbFullDark = findViewById<ProgressBar>(R.id.pb_year_full)
        val pbFullLight = findViewById<ProgressBar>(R.id.pb_year_light_full)

        llInline.visibility = View.GONE
        flFull.visibility = View.GONE

        if (showBar) {
            if (showText) {
                llInline.visibility = View.VISIBLE
                yearLine.text = DailyWidgetProvider.accented(this, day.yearLine, isDark)
                
                pbInlineDark.visibility = if (isDark) View.VISIBLE else View.GONE
                pbInlineLight.visibility = if (isDark) View.GONE else View.VISIBLE
                pbInlineDark.progress = day.yearProgress
                pbInlineLight.progress = day.yearProgress
                pbInlineDark.max = 1000
                pbInlineLight.max = 1000
            } else {
                flFull.visibility = View.VISIBLE
                pbFullDark.visibility = if (isDark) View.VISIBLE else View.GONE
                pbFullLight.visibility = if (isDark) View.GONE else View.VISIBLE
                pbFullDark.progress = day.yearProgress
                pbFullLight.progress = day.yearProgress
                pbFullDark.max = 1000
                pbFullLight.max = 1000
            }
        }

        // --- the chrome around it ---

        findViewById<TextView>(R.id.tv_voice_label).setText(
            if (day.voice == Voice.EVENING) R.string.tagline_evening else R.string.tagline
        )

        findViewById<TextView>(R.id.btn_keep).setText(
            if (day.isKept) R.string.kept_already else R.string.keep_this
        )

        // --- controls ---

        findViewById<RadioButton>(
            when (Prefs.yearLine(this)) {
                YearLineStyle.DAYS_LIVED -> R.id.rb_days_lived
                YearLineStyle.BAR_ONLY -> R.id.rb_bar_only
                YearLineStyle.COUNTDOWN -> R.id.rb_countdown
                YearLineStyle.DAY_OF_YEAR -> R.id.rb_day_of_year
            }
        ).isChecked = true

        findViewById<RadioButton>(
            when (scale) {
                TextScale.SMALL -> R.id.rb_small
                TextScale.LARGE -> R.id.rb_large
                TextScale.MEDIUM -> R.id.rb_medium
            }
        ).isChecked = true

        findViewById<Switch>(R.id.sw_dark_mode).isChecked = Prefs.isDarkMode(this)
        findViewById<Switch>(R.id.sw_bar).isChecked = Prefs.showBar(this)
        findViewById<Switch>(R.id.sw_evening).isChecked = Prefs.eveningEnabled(this)
        findViewById<Switch>(R.id.sw_shift).isChecked = Prefs.shiftColours(this)

        val hour = Prefs.eveningHour(this)
        findViewById<SeekBar>(R.id.sb_evening_hour).progress = hour - firstEveningHour
        findViewById<TextView>(R.id.tv_evening_hour).text =
            String.format("from %02d:00", hour)

        renderCustomWords()
        renderFavourites()

        binding = false
    }

    private fun renderCustomWords() {
        val container = findViewById<LinearLayout>(R.id.list_custom)
        container.removeAllViews()
        Prefs.customWords(this).forEach { word ->
            container.addView(row(container, word) {
                Prefs.removeCustomWord(this, word)
                applied()
            })
        }
    }

    private fun renderFavourites() {
        val container = findViewById<LinearLayout>(R.id.list_favourites)
        container.removeAllViews()

        var any = false
        Voice.entries.forEach { voice ->
            val kept = Prefs.favourites(this, voice)
            if (kept.isEmpty()) return@forEach
            any = true
            container.addView(subheading(if (voice == Voice.EVENING) "Evening" else "Morning"))
            kept.forEach { word ->
                container.addView(row(container, word) {
                    Prefs.removeFavourite(this, voice, word)
                    applied()
                })
            }
        }

        findViewById<TextView>(R.id.tv_kept_hint).setText(
            if (any) R.string.kept_hint else R.string.kept_empty
        )
    }

    private fun row(parent: ViewGroup, text: String, onRemove: () -> Unit): View {
        val view = LayoutInflater.from(this).inflate(R.layout.row_word, parent, false)
        view.findViewById<TextView>(R.id.row_text).text = text
        view.findViewById<TextView>(R.id.row_remove).setOnClickListener { onRemove() }
        return view
    }

    private fun subheading(text: String): TextView {
        val view = TextView(this)
        view.text = text.uppercase()
        view.setTextColor(getColor(R.color.day_label))
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        view.letterSpacing = 0.18f
        view.setPadding(0, dp(14), 0, 0)
        return view
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
