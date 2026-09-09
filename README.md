# Daily Light

A home-screen widget for Android. The first thing you see when you unlock the phone:

- the weekday and full date
- how many days are left in the year
- an affirmation for the day
- a short thought to sit with

Set in Lora, on a dawn gradient — night at the bottom-left, first light at the top-right.

---

## Build it and put it on the phone

You need **Android Studio** (free, any recent version) on a computer, once. After that
the widget lives on the phone and needs nothing.

1. **Open the project.** Android Studio → *Open* → choose the `DailyLight` folder.
   Let it sync (it downloads the Android Gradle Plugin and Kotlin the first time —
   a few minutes, needs internet).

2. **Turn on developer mode on the Samsung.**
   Settings → *About phone* → *Software information* → tap **Build number** seven times.
   Then Settings → *Developer options* → turn on **USB debugging**.

3. **Plug the phone in** with a USB cable. Tap *Allow* on the "Allow USB debugging?" prompt.
   The phone should appear in the device dropdown at the top of Android Studio.

4. **Press Run** (the green ▶). It installs and opens the app, which shows today's card
   and the instructions below.

5. **Add the widget.** On the phone: press and hold an empty spot on the home screen →
   **Widgets** → search **Daily Light** → drag it onto the screen you land on when you unlock.
   Resize it by long-pressing and dragging the handles.

You can unplug the cable now. Nothing else is needed — no account, no internet, no permissions
beyond restarting after a reboot.

### If you'd rather build from a terminal

`gradlew`/`gradlew.bat` are not included (they couldn't be generated here). Run `gradle wrapper`
once in the project folder to create them, or just use Android Studio, which reads
`gradle/wrapper/gradle-wrapper.properties` directly and doesn't need them.

---

## Making it yours

**The words.** `app/src/main/java/com/shamala/dailylight/Content.kt` — two plain lists,
87 affirmations and 84 thoughts, all original text. Add, delete or rewrite freely; the rotation
adapts to whatever length the lists are. Rebuild and re-run to push the change to the phone.

**The colours.** `app/src/main/res/values/colors.xml`. `bg_start` / `bg_center` / `bg_end` are the
diagonal gradient; `glow` is the warm light in the top-right corner; `accent` is the colour of the
countdown number.

**The type.** `app/src/main/res/font/` holds three static cuts of Lora. Drop in a different `.ttf`
(lowercase filename, letters and underscores only) and point `widget_daily.xml` at it.

**The layout.** `app/src/main/res/layout/widget_daily.xml`. Note that home-screen widgets can only
use a restricted set of views — `LinearLayout`, `FrameLayout`, `RelativeLayout`, `TextView`,
`ImageView` and a few others. No `ConstraintLayout`, and no bare `View` (that's why the little rule
under the date is an `ImageView`).

---

## How it stays current

Three overlapping mechanisms, so the date is never stale:

- an inexact daily alarm at 00:01 (`AlarmManager.setRepeating`, no special permission needed)
- `updatePeriodMillis` of 30 minutes as a backstop, in `res/xml/widget_info.xml`
- broadcasts for reboot, date change, clock change and timezone change

The affirmation and thought are a pure function of the date, so redraws never change them
mid-day. Tapping the card bumps a stored offset to draw a different pairing; the next day
returns to the date's own words.

---

## Layout of the project

```
app/src/main/
├── java/com/shamala/dailylight/
│   ├── Content.kt              the affirmations and thoughts
│   ├── DailyContent.kt         date formatting, day counter, which words today gets
│   ├── DailyWidgetProvider.kt  draws the widget, schedules the daily refresh
│   └── MainActivity.kt         the small screen behind the app icon
└── res/
    ├── layout/widget_daily.xml the widget face
    ├── drawable/widget_bg.xml  the dawn gradient
    ├── values/colors.xml       the palette
    └── xml/widget_info.xml     size, resize limits, refresh interval
```

Minimum Android 8.0 (API 26). No third-party dependencies — the widget uses only the platform SDK.

---

## Fonts

Lora, by Cyreal, under the SIL Open Font License 1.1. The three files in `res/font/` are static
instances cut from the variable font. If you ever distribute this app beyond your own phone,
include a copy of the OFL alongside them — it's at
`https://fonts.google.com/specimen/Lora/license`.
