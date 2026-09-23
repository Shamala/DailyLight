package com.shamala.dailylight

import android.app.Activity
import android.graphics.Insets
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.EditText
import android.widget.LinearLayout
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
        applyEdgeToEdgeInsets()
        wireControls()
    }

    /**
     * Android 15 draws every app edge to edge once it targets API 35+, so the
     * status and navigation bars sit on top of the content unless we inset it
     * ourselves. The layout keeps its own breathing room; the system bars are
     * added on top of it rather than replacing it.
     */
    private fun applyEdgeToEdgeInsets() {
        val scroll = findViewById<View>(R.id.scroll_root)
        val content = findViewById<View>(R.id.content_root)
        val basePaddingTop = content.paddingTop
        val basePaddingBottom = content.paddingBottom
        val basePaddingStart = content.paddingStart
        val basePaddingEnd = content.paddingEnd

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        scroll.setOnApplyWindowInsetsListener { _, insets ->
            val bars: Insets = insets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            content.setPaddingRelative(
                basePaddingStart + bars.left,
                basePaddingTop + bars.top,
                basePaddingEnd + bars.right,
                basePaddingBottom + bars.bottom
            )
            insets
        }
        scroll.requestApplyInsets()
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

    /**
     * The preview card, painted by exactly the code the widget runs.
     *
     * The Lora bitmaps are laid out against a pixel width, so the card has to
     * have been measured before this means anything. On the very first pass it
     * has not been, so the paint is deferred one frame.
     */
    private fun paintPreviewCard() {
        val card = findViewById<View>(R.id.widget_root)
        val contentWidth = card.width - card.paddingStart - card.paddingEnd
        if (contentWidth <= 0) {
            card.post { paintPreviewCard() }
            return
        }
        CardPainter.paint(this, ViewSurface(card), contentWidth)
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

        paintPreviewCard()

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
            getString(R.string.evening_from_hour, hour)

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
            container.addView(
                subheading(
                    getString(
                        if (voice == Voice.EVENING) R.string.voice_evening
                        else R.string.voice_morning
                    )
                )
            )
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
        view.text = text.uppercase(java.util.Locale.getDefault())
        view.setTextColor(getColor(R.color.day_label))
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        view.letterSpacing = 0.18f
        view.setPadding(0, dp(14), 0, 0)
        return view
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
