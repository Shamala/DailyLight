import {
  AbsoluteFill,
  Easing,
  Img,
  Interactive,
  interpolate,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { fontFamily } from "./fonts";

// Close on the name, and what it doesn't ask of you.
export const EndCard = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  return (
    <AbsoluteFill
      name="End card"
      style={{
        background: "linear-gradient(180deg, #14122A 0%, #2A1E3E 60%, #6E4360 100%)",
      }}
    >
      <Img
        name="App icon"
        src={staticFile("icon.png")}
        style={{
          position: "absolute",
          left: 390,
          top: 520,
          width: 300,
          height: 300,
          borderRadius: 80,
          opacity: interpolate(frame, [0, 0.8 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
          scale: interpolate(frame, [0, 1 * fps], [0.85, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.spring({ damping: 200 }),
            output: "perceptual-scale",
          }),
        }}
      />
      <Interactive.Div
        name="Title"
        style={{
          position: "absolute",
          left: 80,
          right: 80,
          top: 900,
          textAlign: "center",
          fontFamily,
          fontWeight: 500,
          fontSize: 140,
          color: "#F7F1E9",
          opacity: interpolate(frame, [0.4 * fps, 1.2 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        Daily Light
      </Interactive.Div>
      <Interactive.Div
        name="Where"
        style={{
          position: "absolute",
          left: 80,
          right: 80,
          top: 1100,
          textAlign: "center",
          fontFamily,
          fontSize: 60,
          color: "#F0BC98",
          opacity: interpolate(frame, [1 * fps, 1.8 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        Free on Android
      </Interactive.Div>
      <Interactive.Div
        name="Promise"
        style={{
          position: "absolute",
          left: 80,
          right: 80,
          top: 1230,
          textAlign: "center",
          fontFamily,
          fontStyle: "italic",
          fontSize: 46,
          lineHeight: 1.45,
          color: "#E4DCEA",
          opacity: interpolate(frame, [1.6 * fps, 2.4 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        No ads. No account.
        <br />
        Nothing leaves your phone.
      </Interactive.Div>
    </AbsoluteFill>
  );
};
