# Releasing Daily Light to Google Play

Everything the build needs is already in the repo. What is left is a signing key,
two screenshots, and the Play Console forms.

---

## 1. Make the upload key (once, and never lose it)

Play uses **Play App Signing**: the key below signs the *upload*, and Google holds
the key that signs what users install. If you lose the upload key you can ask Google
to reset it, but do not rely on that — back the file up somewhere you will still
have in five years.

**Already done.** The key lives at `~/keys/dailylight-upload.p12` (PKCS12, RSA 4096,
alias `dailylight`, valid to Feb 2054) and `keystore.properties` in the project root
points at it. Neither is in the repo.

To recreate it from scratch on another machine:

```sh
keytool -genkeypair -v \
  -keystore ~/keys/dailylight-upload.p12 \
  -storetype PKCS12 \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -alias dailylight
```

Then create `keystore.properties` in the project root — it is gitignored, and the
build reads it automatically:

```properties
storeFile=/Users/you/keys/dailylight-upload.p12
storePassword=…
keyAlias=dailylight
keyPassword=…
```

The same four values can be supplied as `DAILYLIGHT_STORE_FILE`,
`DAILYLIGHT_STORE_PASSWORD`, `DAILYLIGHT_KEY_ALIAS` and `DAILYLIGHT_KEY_PASSWORD`
environment variables instead, which is what CI should do. Without either, the
release build still assembles — unsigned — so a machine with no key is never blocked.

## 2. Build the bundle

```sh
./gradlew clean :app:testDebugUnitTest :app:lintRelease :app:bundleRelease
```

The upload artifact is `app/build/outputs/bundle/release/app-release.aab`
(~300 KB). Play wants the **.aab**, not an APK.

Check it is signed with your key, not the debug key:

```sh
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab | head
```

## 3. Screenshots

Five are already in `play/graphics/screenshots/`, captured from a Pixel 8 emulator
with the widget genuinely placed on the home screen, and cropped to 1080×2160 so
they satisfy Play's rule that the long side is no more than twice the short side
(a raw 1080×2400 Pixel screenshot fails it):

| File | What it shows |
|---|---|
| `01-home-morning.png` | The widget on the home screen, dawn palette, morning voice |
| `02-home-evening.png` | The closing voice and the dusk palette |
| `03-home-light.png` | The light theme |
| `04-home-bar-only.png` | The year line set to bar-only |
| `05-settings.png` | The app's own screen with the live preview card |

Play takes at least 2 and at most 8, so upload all five in that order. Replace or
add to them from your own phone if you would rather the wallpaper and the app dock
be yours.

A 7-inch and 10-inch tablet screenshot is optional; without them Play marks the
listing "not optimised for tablets", which is only a note on the listing.

## 4. Store listing

`play/store-listing.md` holds the final text for every field, the content-rating
answers, and the Data safety answers. The graphics are already made:

| Asset | File | Required size |
|---|---|---|
| App icon | `play/graphics/icon-512.png` | 512×512, 32-bit PNG, no transparency ✓ |
| Feature graphic | `play/graphics/feature-graphic-1024x500.png` | 1024×500 ✓ |
| Phone screenshots | `play/graphics/screenshots/*.png` | ≥ 2, 1080×2160 ✓ |

The privacy policy is already live at
**https://shamala.github.io/DailyLight/privacy-policy.html**, served by GitHub Pages
from `docs/` on `main`. Paste that into the listing. **A privacy policy URL is
required for every app**, including one that collects nothing. Edit `docs/` and push
to change it.

## 5. Console checklist

- [ ] Create the app in Play Console — name **Daily Light**, English (US), free, app
      (not game).
- [ ] App access: **all functionality available without restrictions** (no login).
- [ ] Ads: **no ads**.
- [ ] Content rating questionnaire — all *no*; see `store-listing.md`.
- [ ] Target audience: 13+; not in Designed for Families.
- [ ] Data safety: **no data collected or shared**.
- [ ] Government apps: no. Financial features: none. Health: none.
- [ ] Upload the `.aab` to **Internal testing** first. Install from the test link on
      a real phone and confirm the widget places, resizes and redraws.
- [ ] Promote to Production, roll out at 100%.

First review typically takes a few days. New personal developer accounts may also
have to run a 14-day closed test with 12 testers before production is unlocked —
check whether your account is subject to that before promising anyone a date.

## 6. Shipping a change later

Raise `versionCode` by one in `app/build.gradle.kts` every upload — Play rejects a
repeat. Move `versionName` when the change is worth naming.

---

## Known gap, deliberately left

The widget picker preview uses `previewLayout`, which Android 12 and newer honour.
On Android 11 and below the picker falls back to `initialLayout`, which is the same
layout with its placeholder text — so it still looks right, just not live. Adding a
static `previewImage` PNG would cover those versions; it was left out rather than
ship a bitmap that goes stale every time the card design moves.
