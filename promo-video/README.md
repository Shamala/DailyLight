# Daily Light — promo video

A 24-second vertical video (1080×1920, 30 fps) for Reels, Shorts and WhatsApp Status, built in
code with [Remotion](https://www.remotion.dev).

**Watch it:** [`daily-light-promo.mp4`](daily-light-promo.mp4)

| Scene | Length | What's on screen |
|---|---|---|
| Sunrise | 5 s | Night lifts, the sun rises, "Daily Light · A quiet start to the day." |
| Widget | 5 s | The real widget on a real home screen |
| Through the day | 7 s | The same card at sunrise, midday, sunset and night |
| One tap | 4.5 s | The "Put it on my home screen" button being tapped |
| End card | 5 s | Icon, name, "No ads. No account. Nothing leaves your phone." |

---

## How a video like this is made with Remotion

Remotion turns React components into video. Each frame is a render of your components at a given
frame number; you animate by computing styles from that number instead of using CSS transitions.

### 1. Create the project

You need Node.js. Then:

```console
npx create-video@latest --yes --blank --no-tailwind promo-video
cd promo-video
npm i
npx remotion add @remotion/transitions    # fades between scenes
npx remotion add @remotion/google-fonts   # web fonts, loaded before each frame renders
```

### 2. Put your assets in `public/`

Screenshots, icons, music. Reference them with `staticFile("name.png")`. Here they are the app's
Play Store screenshots and its icon.

### 3. One component per scene

Each scene is a React component that reads the current frame and animates with `interpolate()`:

```tsx
import { AbsoluteFill, Easing, Interactive, interpolate, useCurrentFrame, useVideoConfig } from "remotion";

export const Title = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <AbsoluteFill style={{ backgroundColor: "#14122A", justifyContent: "center", alignItems: "center" }}>
      <Interactive.Div
        name="Title"
        style={{
          fontSize: 140,
          color: "#F7F1E9",
          // fade in over the first second
          opacity: interpolate(frame, [0, 1 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        Daily Light
      </Interactive.Div>
    </AbsoluteFill>
  );
};
```

Keep styles inline and use `Interactive.Div` with a `name`: the Studio can then let you click an
element in the preview and edit its text, position and keyframes, writing the change back to code.

### 4. Join the scenes

`src/DailyLightPromo.tsx` lines the scenes up with `<TransitionSeries>` and a 15-frame fade between
each. Every scene is also registered on its own in `src/Root.tsx`, so it can be previewed and edited
separately. The main composition's length is the sum of the scenes minus the fade overlaps
(810 − 4 × 15 = 735 frames).

### 5. Preview and render

```console
npx remotion studio                                   # live preview at http://localhost:3000
npx remotion render DailyLightPromo out/video.mp4     # write the MP4
npx remotion still DailyLightPromo --frame=100 f.png  # check a single frame
```

### Things that caught us out

- **CSS animations don't render.** Frames are rendered one at a time, so `transition` and
  `@keyframes` never play. Drive everything from `useCurrentFrame()`.
- **A bare `import "./fonts"` was silently dropped.** `package.json` marks only CSS files as having
  side effects, so the bundler removed the import and the text fell back to Times. Import something
  you use — here `import { fontFamily } from "./fonts"` — so the font loading actually runs.
- **Design for a phone screen.** Keep text at least 80px from the sides and 100px from top and
  bottom, headlines around 84px or larger, supporting text 44px or larger.

---

## Files

```
src/
├── Root.tsx             registers the full video and each scene
├── DailyLightPromo.tsx  the scenes in order, with fades
├── Sunrise.tsx          opening
├── WidgetScene.tsx      the widget on the home screen
├── DayScene.tsx         the card through the day
├── OneTapScene.tsx      one-tap setup
├── EndCard.tsx          closing card
└── fonts.ts             loads Lora, the widget's typeface
public/                  screenshots and icon from ../play/graphics
```

Remotion is free for individuals and teams of up to three; see
[remotion.pro/license](https://www.remotion.pro/license).
