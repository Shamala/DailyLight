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

## The ask (WhatsApp / SMS)

> I've made a small Android widget called Daily Light — it shows an affirmation, the
> date, and how far along the year is on your home screen. To put it on the Play
> Store I need 12 people to test it for two weeks.
>
> Would you mind? It's free, takes about two minutes, and you can delete it after.
>
> 1. Send me the Gmail address you use on your phone
> 2. I'll send a link — tap it, tap "Become a tester", then install from Play
> 3. Please leave it installed for two weeks
>
> Android only, sorry — it won't work on an iPhone.

## The ask (email / Slack)

> Subject: Two minutes of your time — testing an Android widget
>
> I've built a small Android widget called Daily Light. It sits on the home screen
> and shows one affirmation, a short thought, the date, and how far along the year
> is. No account, no ads, no tracking — it can't even reach the internet.
>
> To publish it on Google Play I need 12 people to be signed-up testers for 14 days.
> That's the whole commitment: install it, leave it there a fortnight, delete it
> afterwards if you like.
>
> If you're in, reply with the Gmail address you use on your Android phone and I'll
> send the link.

## Once they're in — send this on day one

> Thanks! Here's the link: <OPT-IN LINK>
>
> Tap "Become a tester", then "Download it on Google Play", then install.
>
> To add the widget: press and hold an empty spot on your home screen → Widgets →
> find Daily Light → drag it out. Long-press it afterwards to resize.
>
> If you get a moment, three things I'd love to know:
> 1. Does the text look right on your phone, or is anything cut off or blurry?
> 2. Try resizing it — does it still look good bigger and smaller?
> 3. After 6pm it changes to a different set of words. Does that land?
>
> One line on each is plenty.

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
