# Daily Light

A home-screen and lock-screen widget for Android. The first thing you see when you unlock the phone:

- the weekday and full date
- how far along the year is — a day count and a quiet bar, not a countdown
- an affirmation for the day
- a short thought to sit with

After an hour you choose, it turns to a **closing voice** for the evening — setting down, noticing
what went well, permission to stop. Set in Lora, on a sky that follows the day: the sun rises bottom
left in the morning, crosses the top, and sets bottom right in rose, then a moon and a few stars
until morning. The app itself opens with a three-second sunrise, or a sunset in the evening.

---

## Build it and put it on the phone

You need **Android Studio** on a computer, once. After that the widget lives on the phone and needs
nothing — no account, no internet, no permissions beyond restarting after a reboot.

1. **Open the project.** Android Studio → *Open* → choose the `DailyLight` folder. Let it sync.
2. **Set the Gradle JDK to 21.** Settings → *Build, Execution, Deployment* → *Build Tools* → *Gradle*
   → **Gradle JDK** → *Download JDK…* → version **21**. (`gradle/gradle-daemon-jvm.properties`
   already asks for 21, so newer Android Studio versions may pick it up on their own.)
3. **Turn on developer mode.** Settings → *About phone* → *Software information* → tap **Build
   number** seven times. Then Settings → *Developer options* → **USB debugging** on (and **Install
   via USB** if you see it).
4. **Connect.** Either plug in a data cable, or Developer options → **Wireless debugging** → *Pair
   device with QR code*, and in Android Studio use the device dropdown → *Pair Devices Using Wi-Fi*.
5. **Press Run** (▶).

Then place it:

- **Home screen** — open the app and tap **Put it on my home screen**, then **Add**. The app shows
  this until a widget is placed. By hand: press and hold an empty spot → *Widgets* → search
  **Daily Light** → drag it on.
- **Lock screen** (One UI 8.5+) — press and hold the lock screen → pencil → *Widgets* → add it there
  too. On older One UI, Samsung allows only its own widgets on the lock screen; this one declares
  itself eligible and will appear once you update.

### Building from a terminal

```
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. The wrapper downloads Gradle 9.6 on first run and
checks it against its published SHA-256. For a Play upload, see `play/RELEASE.md`.

---

## Using it

Until you've tried them, a small line along the bottom of the widget says what the two taps do;
each half goes once you've used it.

**Tap the words** on the widget to draw a different pairing. That lasts the rest of the day only —
tomorrow returns to its own words rather than inheriting yesterday's fidgeting.

**Tap the date** to open the app, where you can:

- **Show this more often** — marked lines come up about one day in four, instead of waiting their
  turn in the full list. Morning and evening lines are kept separately, so an evening line comes
  back in the evening.
- **Add your own** — anything you write joins the pool and can come up on any day.
- **Show me another** — the same reshuffle as tapping the words on the widget.
- Choose how the year line reads, when the evening voice starts, whether the colours shift, and the
  text size.

---

## Making it yours in code

**The words.** `app/src/main/java/com/shamala/dailylight/Content.kt` — four plain lists:
`affirmations`, `thoughts`, `eveningAffirmations`, `eveningThoughts`. All original text. Add, delete
or rewrite freely; the rotation adapts to whatever length the lists are.

**The colours.** `app/src/main/res/values/colors.xml`, four sets of three stops — `dawn_*`,
`day_*`, `dusk_*`, `night_*`. Change a set and that time of day follows. The light on top — sun,
glow, moon and stars — is painted by `SkyPainter.kt`, which also sets sunrise (06:00) and sunset
(19:00).

**The type.** `app/src/main/res/font/` holds three static cuts of Lora. Drop in a different `.ttf`
(lowercase filename, letters and underscores only) and point both `CardRenderer.kt` (which paints
the widget's text) and `widget_daily.xml` (the in-app preview and fallback) at it.

**The layout.** `app/src/main/res/layout/widget_daily.xml`. Home-screen widgets can only use a
restricted set of views — `LinearLayout`, `FrameLayout`, `RelativeLayout`, `TextView`, `ImageView`,
`ProgressBar` and a few others. No `ConstraintLayout`, and no bare `View`.

---

## How it stays current

- an inexact daily alarm at 00:01, for the date rolling over
- `updatePeriodMillis` of 30 minutes, which is what moves the sun and catches the switch to the
  evening voice. Nothing on the widget animates, so it costs no battery.
- broadcasts for reboot, date change, clock change and timezone change

The affirmation and thought are a pure function of (date, voice, offset), so redraws never change
them mid-session.

If the words seem to freeze overnight, check Settings → *Battery* → *Background usage limits* and
make sure Daily Light isn't in *Sleeping apps*.

---

## Layout of the project

```
app/src/main/
├── java/com/shamala/dailylight/
│   ├── Content.kt              the four word pools
│   ├── Prefs.kt                settings, their own words, lines shown more often
│   ├── DailyContent.kt         date, year progress, which words this moment gets
│   ├── CardPainter.kt          decides how the card looks, for widget and preview alike
│   ├── CardSurface.kt          the widget (RemoteViews) and preview (views) it paints onto
│   ├── CardRenderer.kt         paints the date and words in Lora, as bitmaps
│   ├── SkyPainter.kt           the widget's sky: sun, glow, horizon, moon and stars
│   ├── SkyHeaderView.kt        the app screen's three-second sunrise or sunset
│   ├── DailyWidgetProvider.kt  draws the widget, schedules the daily refresh, one-tap placing
│   └── MainActivity.kt         settings screen and word editor
└── res/
    ├── layout/widget_daily.xml the widget face
    ├── layout/activity_main.xml the settings screen
    ├── drawable/widget_bg_*.xml the four times of day, dark and light
    ├── values/colors.xml       the palettes
    └── xml/widget_info.xml     size, resize limits, refresh interval, lock-screen eligibility
```

Minimum Android 8.0 (API 26). No third-party dependencies — only the platform SDK.

---

## Promo video

`promo-video/` holds a 24-second vertical promo, [`daily-light-promo.mp4`](promo-video/daily-light-promo.mp4),
made in code with Remotion. Its README explains how it was built and how to make one like it.

---

## Fonts

Lora, by Cyreal, under the SIL Open Font License 1.1. The three files in `res/font/` are static
instances cut from the variable font. If you ever distribute this app beyond your own phone, include
a copy of the OFL alongside them — it's at `https://fonts.google.com/specimen/Lora/license`.
