package com.shamala.dailylight

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
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
        noteOpenedFromWidget(intent)
        applyEdgeToEdgeInsets()
        wireControls()
        openSky(firstOpen = savedInstanceState == null)

        // The preview grows and shrinks with its words; its sky has to be
        // repainted to the new size or the sun would stretch.
        findViewById<View>(R.id.widget_root)
            .addOnLayoutChangeListener { card, _, _, _, _, _, _, _, _ -> paintPreviewSky(card) }
    }

    /**
     * Sunrise when the app opens in the morning voice, sunset in the evening
     * one, with the words fading in as the light settles. Only on a fresh
     * open — a rotation shows the finished sky.
     */
    private fun openSky(firstOpen: Boolean) {
        val evening = DailyContent.now(this).voice == Voice.EVENING
        val sky = findViewById<SkyHeaderView>(R.id.sky_view)
        val words = findViewById<View>(R.id.sky_words)
        sky.evening = evening
        findViewById<TextView>(R.id.tv_greeting).setText(
            if (evening) R.string.greeting_evening else R.string.greeting_morning
        )
        if (!firstOpen) {
            sky.showFinished()
            return
        }
        words.alpha = 0f
        sky.play { words.animate().alpha(1f).setDuration(1500L).start() }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        noteOpenedFromWidget(intent)
    }

    /**
     * Opened by tapping the date on the widget: they know that part now, so
     * the widget's hint can drop it.
     */
    private fun noteOpenedFromWidget(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_FROM_WIDGET, false) != true) return
        if (Prefs.usedOpen(this)) return
        Prefs.setUsedOpen(this)
        DailyWidgetProvider.refreshAll(this)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    // ------------------------------------------------------------------
    // Wiring
    // ------------------------------------------------------------------

    private fun wireControls() {
        val pin = View.OnClickListener {
            if (!DailyWidgetProvider.requestPin(this)) goHome()
        }
        findViewById<View>(R.id.btn_pin).setOnClickListener(pin)
        findViewById<View>(R.id.btn_pin_again).setOnClickListener(pin)

        findViewById<View>(R.id.btn_pin_later).setOnClickListener {
            Prefs.setPinDismissed(this, true)
            renderAddPanel()
        }

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
        val content = findViewById<View>(R.id.card_content)
        val contentWidth = content.width - content.paddingStart - content.paddingEnd
        if (contentWidth <= 0) {
            card.post { paintPreviewCard() }
            return
        }
        CardPainter.paint(this, ViewSurface(card), contentWidth, showHint = false)
        // Forced: the size may be the same, but the time or theme may not be.
        paintPreviewSky(card, force = true)
    }

    private var skySize = 0 to 0

    /** Only when the size has actually changed, or the time has moved on. */
    private fun paintPreviewSky(card: View, force: Boolean = false) {
        if (card.width <= 0 || card.height <= 0) return
        val size = card.width to card.height
        if (!force && size == skySize) return
        skySize = size
        CardPainter.paintSky(this, ViewSurface(card), card.width, card.height)
    }

    /**
     * Until the widget is on the home screen, the panel under the preview
     * offers to put it there — one tap where the launcher allows it, three
     * written steps where it doesn't.
     */
    private fun renderAddPanel() {
        val placed = DailyWidgetProvider.isPlaced(this)
        val canPin = AppWidgetManager.getInstance(this).isRequestPinAppWidgetSupported

        findViewById<View>(R.id.add_panel).visibility =
            if (!placed && !Prefs.pinDismissed(this)) View.VISIBLE else View.GONE
        findViewById<View>(R.id.btn_pin_again).visibility =
            if (!placed && canPin) View.VISIBLE else View.GONE

        findViewById<TextView>(R.id.tv_add_title)
            .setText(if (canPin) R.string.pin_title else R.string.steps_title)
        findViewById<TextView>(R.id.tv_add_body)
            .setText(if (canPin) R.string.pin_body else R.string.steps_body)
        findViewById<View>(R.id.steps_group).visibility =
            if (canPin) View.GONE else View.VISIBLE
        findViewById<TextView>(R.id.btn_pin)
            .setText(if (canPin) R.string.pin_button else R.string.steps_button)
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
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
        renderAddPanel()

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

    companion object {
        /** On the intent the widget's date opens us with. */
        const val EXTRA_FROM_WIDGET = "com.shamala.dailylight.extra.FROM_WIDGET"
    }
}
