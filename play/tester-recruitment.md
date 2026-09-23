# Recruiting the 15

Google requires **12 testers opted in continuously for 14 days** before production
access unlocks. Recruit 15, because people drop out and dropping below 12 puts the
clock at risk.

Testers pay nothing and need no developer account — just a free Google account and
an Android phone.

---

## Before you send anything

Set the closed test up with a **Google Group**, not a pasted email list. With a group
you add and remove people without editing the release, which matters over two weeks.

1. Make a group at groups.google.com, e.g. `daily-light-testers@googlegroups.com`
2. Play Console → Testing → **Closed testing** → Testers → add the group address
3. Copy the **opt-in link** from "How testers join your test"

---

## The ask

```
Hey! I made a little Android widget — shows a daily
affirmation and the date on your home screen.

Need 12 people to test it for 2 weeks so I can put it
on the Play Store 🙏

Send me your Gmail and I'll share the link. Free,
takes 2 mins, delete it after. Android only!
```

## Once they send their Gmail

```
Here you go: <LINK>

Tap "Become a tester" → then install from Play Store.

To add it: long-press your home screen → Widgets →
Daily Light → drag it out.

Please keep it for 2 weeks 🙏
```

## A few days in

```
How's the widget looking? Two quick things —
does the text look right on your phone, and does
it still look ok if you resize it bigger?
```

---

## Why ask those three questions

The production access application asks what feedback you received and what you
changed. "Looks nice" is not an answer that gets approved. Those three map onto the
things most likely to actually break:

1. Font substitution — some launchers (Samsung's especially) override the app's
   typeface, which is why the card is painted as bitmaps rather than text
2. Resizing repaints those bitmaps at a new width, and the widest sizes are what
   exercise the bitmap memory cap
3. The evening voice is the feature most likely to feel wrong rather than be wrong

---

## Tracking

| # | Name | Gmail | Opted in | Day 14 |
|---|------|-------|----------|--------|
| 1 |  |  |  |  |
| 2 |  |  |  |  |
| 3 |  |  |  |  |
| 4 |  |  |  |  |
| 5 |  |  |  |  |
| 6 |  |  |  |  |
| 7 |  |  |  |  |
| 8 |  |  |  |  |
| 9 |  |  |  |  |
| 10 |  |  |  |  |
| 11 |  |  |  |  |
| 12 |  |  |  |  |
| 13 |  |  |  |  |
| 14 |  |  |  |  |
| 15 |  |  |  |  |

The 14 days counts **opted-in testers**, so the clock starts when the 12th person
accepts — not when you send the links. Chase the stragglers.
