# Daily Light — project context

A handoff note for anyone picking this up cold, including future me. The README says how to
build and use it; this says **why it is the way it is**, and which of its oddities are
deliberate.

Project: `github.com/Shamala/DailyLight` · package `com.shamala.dailylight` · Kotlin, no
third-party dependencies, minSdk 26.

---

## 1. What it is, and the one sentence that decides everything

An Android home-screen and lock-screen widget that shows an affirmation, a short thought,
the date, and how far along the year is.

The brief: **something you read in the first few seconds after unlocking the phone in the
morning, that is soothing and encourages a fresh start.**

That sentence has settled every argument so far. It is why there is no streak counter, no
notification, no "you haven't opened this in 3 days". The widget asks nothing of the person
looking at it. Anything that turns it into a thing you can fail at is out of scope, however
easy it would be to build.

---

## 2. Decisions, and why

**Native Android widget, not a PWA.** Asked and answered explicitly. A PWA on Android can
put an icon on the home screen but cannot draw a tile you passively see; Android widgets
must come from an installed app, and the widget host can't render a WebView. The Chromium
request for PWA widgets has been open for years and hasn't shipped. A wrapper app that
renders HTML to a bitmap was considered and rejected — it saves nothing on setup, since you
still build in Android Studio either way.

**Day counter, not a countdown.** v1 said "114 days left in 2026". That reads as pressure
on a heavy morning. v2 replaced it with **"Day 252 · 2026"** plus a hairline bar that fills
as the year goes — nothing depletes, nothing is running out. The old countdown survives as
a setting, along with "252 days lived this year" and bar-only. If this is ever revisited:
the principle is accumulation over depletion.

**A separate evening voice, not the morning words restated.** After an hour you set
(default 18:00, holding until 04:00) the card switches to a different pool: setting down,
noticing what went well, permission to stop. 56 affirmations and 56 thoughts of their own.
Someone up at 1am is ending a day, not starting one — hence the 4am boundary rather than
midnight.

**Original text throughout.** All four word pools were written from scratch rather than
collected as quotations. No attribution obligations, nothing to license, and every line can
be rewritten freely. If the pools grow, keep them original.

**Lora, on a dawn gradient.** A warm book serif — italic for the affirmation, medium for the
date. The palette moves through the day: indigo at dawn, cooler at midday, rose at dusk,
deepest at night. Text tones stay constant across all four so legibility is predictable.
v1's palette was near-black and was lightened once, on request; the current stops are
documented at the top of `colors.xml` along with a darker and a lighter set.

**A sky that moves, but never animates on the home screen.** The card should feel like
watching a sunrise in the morning and a sunset in the evening. On the widget that is a
painted sky (`SkyPainter`): the sun rises bottom left at 06:00, crosses the top and sets
bottom right at 19:00, then a moon and a few stars. It is redrawn with every 30-minute
update, so the change is between glances, not while you look. Real animation on a widget
means redrawing every few seconds, which costs battery and which Android throttles; the one
genuine animation is the three-second sunrise or sunset on the app's own screen
(`SkyHeaderView`), which stops when it's done. The sky is painted at a third of the card's
size and stretched: soft light survives that, and it keeps the bitmap near 0.2 MB. Stars and
the moon stay in the margins so none reads as punctuation in the words, and the sun's disc
only shows where there's room for it — as a half-disc on the horizon just after sunrise and
before sunset, small enough to sit in the card's bottom padding under the last line of words,
and in the gap at the top around midday. The rest of the day just its glow crosses
the card, so nothing solid ever sits behind a line of text.

**One tap to put it on the home screen.** Testers installed the app and never found the
widget — older testers didn't know what a widget was. Until one is placed, the app offers a
single large button that asks the launcher to add it (`requestPinAppWidget`, with today's
real card as the preview), then goes to the home screen with a hint about moving and
resizing. Where the launcher can't do that, the same space shows three written steps.

**Words hold still.** The affirmation and thought are a pure function of (date, voice,
offset). The widget redraws every 30 minutes and never changes the words underneath you —
what you read at 6am is still there at lunch.

---

## 3. Wording principles

A label should say what the control does. Two were rewritten after they proved opaque in
use:

| was | now |
|---|---|
| Keep this one / Kept | **Show this more often** / ✓ Showing more often |
| Your own words | **Add your own** |
| Add to the pool | Add |
| Another | Show me another |

Each section also states the actual rule rather than leaving it to be inferred — "these come
up about one day in four, instead of waiting their turn in the full list". Apply the same
test to anything added: if the person has to guess the mechanism, the label is wrong.

---

## 4. The code

```
app/src/main/java/com/shamala/dailylight/
├── Content.kt              four word pools (morning + evening × affirmations + thoughts)
├── Prefs.kt                settings, their own words, "shown more often" lines; SharedPreferences
├── DailyContent.kt         date, year progress, phase, voice, which words this moment gets
├── CardRenderer.kt         paints the Lora lines into bitmaps (see §5)
├── SkyPainter.kt           the widget's sky: sun, glow, horizon, moon and stars, at 1/3 size
├── SkyHeaderView.kt        the app screen's three-second sunrise or sunset
├── DailyWidgetProvider.kt  assembles the RemoteViews, schedules the daily refresh
└── MainActivity.kt         settings screen and word editor
```

`DailyContent.build(...)` is deliberately pure — no Context, everything passed in — so the
selection logic can be checked on its own. (One exception: it reads `Locale.getDefault()` to
spell the weekday and month. The words chosen don't depend on it, but pin the locale if you
ever test the date strings on another machine.) It has been verified against a separate implementation for day-of-year, leap years, the year-line wording at all
four styles, voice and phase boundaries through a full day, and ~2,900 cards across four
years with no index errors. The "shown more often" weighting measured 26% over a simulated
year against a target of one day in four.

**Selection:** `seed = year*401 + dayOfYear + offset + (977 if evening)`. Two different
strides for affirmation and thought so they don't march in lockstep. Favourites are drawn
when `seed % 4 == 0`.

---

## 5. Things that will bite you

These are all learned the hard way. None are obvious from the docs.

**Widget layouts can only use certain views.** `LinearLayout`, `FrameLayout`,
`RelativeLayout`, `GridLayout`, `TextView`, `ImageView`, `ProgressBar` and a few more. No
`ConstraintLayout`. **No bare `View`** — that's why the rule under the date was an
`ImageView` before the progress bar replaced it.

**Custom fonts silently don't work in widgets.** The layout is inflated in the *launcher's*
process, and One UI Home substitutes the system font for whatever `@font/` the layout asks
for. The in-app preview keeps Lora because it inflates in our process, which makes the bug
look like a settings problem. There is no remote "set typeface" call in RemoteViews, and
`TypefaceSpan` loses its Typeface when parcelled. The only reliable fix is to paint the text
yourself — `CardRenderer` draws the date, affirmation and thought with `StaticLayout` and
sends bitmaps. Consequence: those lines are pixels, so the system font-size slider doesn't
affect them; the app's own size setting does.

**A widget can't resize its own footprint.** It occupies the cells it was dropped into, full
stop. When the text got smaller the slack pooled at the bottom; the fix was `wrap_content` +
`layout_weight="1"` + `gravity="center_vertical"` on the words zone, so leftover space
splits above and below and nothing is squeezed when there is none. Actual resizing is the
person's job, by long-pressing the widget.

**Bitmaps must be repainted on resize.** Hence `onAppWidgetOptionsChanged`, and hence
`buildViews` taking an `appWidgetId` so it can read that instance's width.

**Text scale must apply to every line.** v2 shipped a Small/Medium/Large setting that only
touched the affirmation and thought — the date and year line were hardcoded, so the setting
appeared broken. Now `Small = 1.0` (the original size), Medium 1.13, Large 1.28, applied to
all five lines. The scale only ever grows; nobody has wanted this smaller.

**Reshuffling is day-scoped.** The tap offset is stamped with the date it was made, so
tomorrow returns to its own words instead of inheriting yesterday's fidgeting. v1 persisted
the offset forever, which was a bug dressed as a feature.

**Samsung will put the app to sleep.** Settings → Battery → Background usage limits. If the
words freeze overnight, that's why.

**Lock screen needs One UI 8.5+.** `widgetCategory="home_screen|keyguard"` is all the app
has to do — modern Android allows every widget on the lock screen unless it opts out with
`not_keyguard`. Before 8.5 Samsung permitted only its own widgets there.

**After a layout change, remove and re-add the widget.** A placed widget can hold a stale
layout; new view ids won't exist in it.

---

## 6. Build environment

Notes specific to this machine, all of which cost time once.

- **Gradle JDK must be 21.** Current Android Studio bundles JetBrains Runtime 25, and Gradle
  8.13 refuses anything above 23. Settings → Build Tools → Gradle → Gradle JDK →
  *Download JDK…* → 21. "Use the bundled runtime" is the wrong advice here.
  `gradle/gradle-daemon-jvm.properties` now pins the daemon to JetBrains 21 as well, with
  download links, and `settings.gradle.kts` applies the foojay resolver so toolchains can be
  fetched.
- **Versions, as of 18 Sep 2026:** Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin 2.0.21,
  compile/target SDK 35. Moved up from Gradle 8.11.1 / AGP 8.7.3.
- **Gradle distribution downloads can time out.** `services.gradle.org` is the host. Retry
  first; if it persists, download the zip in a browser, unzip it, and point *Use Gradle from
  → Specified location* at it.
- **`gradlew` / `gradlew.bat` are in the repo.** `./gradlew assembleDebug` builds from a
  terminal; Android Studio doesn't need them.
- **Installed over wifi**, via Developer options → Wireless debugging → *Pair device with QR
  code*. No cable involved.
- **Deleting a file needs doing by hand on upgrade.** v2 removed `drawable/widget_bg.xml`;
  unzipping over the top left it behind, and it referenced colours that no longer existed —
  the build failed on resource linking. Check for orphans when files are removed.

---

## 7. Where it stands

| | |
|---|---|
| **v1** · 8 Sep 2026 | Date, countdown, affirmation, thought. Lora, one dark gradient. Tap to reshuffle. |
| | Background lightened on request (9 Sep). |
| **v2** · 9 Sep 2026 | Evening voice; day counter + year bar; colours follow the time of day; add your own words; "show this more often"; settings screen; lock-screen eligible; split tap zones; day-scoped reshuffle. |
| **fixes** · 17–18 Sep 2026 | Text scale applied to every line and rebased (Small = old size). Words centred in leftover space. Lora painted to bitmaps so the widget matches the preview. Labels rewritten to say what they do. |

Installed and in daily use on a Samsung running One UI 8.5+, home screen and lock screen.

---

## 8. Ideas raised and not built

Kept here so they don't have to be rediscovered.

- **Tag the pools seasonally** — January leans fresh-start, December reflective, Monday
  differs from Saturday. Same words, better timing.
- **A word for the year** — one intention set in January, sitting above everything else.
- **Your own countdowns** — a trip, a birthday; the year line rotates through dates you care
  about.
- **Auto-sizing text that fills the box** — real `autoSizeTextType` rather than the current
  length-based steps. Awkward in RemoteViews (no remote autosize setter; would need one
  layout per size), but moot now that the text is painted — `CardRenderer` could measure to
  fit the available height directly.
- **Export / import your own words**, so they survive a reinstall. Currently
  SharedPreferences only.

Explicitly rejected, and should stay rejected unless the brief changes: **streaks, scores,
and reminders.** See §1.
