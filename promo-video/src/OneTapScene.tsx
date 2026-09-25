import {
  AbsoluteFill,
  Easing,
  Interactive,
  interpolate,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { fontFamily } from "./fonts";

// Setting it up: one button, one tap, and it is on the home screen.
export const OneTapScene = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  return (
    <AbsoluteFill name="One tap" style={{ backgroundColor: "#14122A" }}>
      <Interactive.Div
        name="Caption"
        style={{
          position: "absolute",
          left: 90,
          right: 90,
          top: 360,
          textAlign: "center",
          fontFamily,
          fontSize: 96,
          lineHeight: 1.2,
          color: "#F7F1E9",
          opacity: interpolate(frame, [0, 0.6 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        Set up in one tap
      </Interactive.Div>
      <Interactive.Div
        name="Button"
        style={{
          position: "absolute",
          left: 110,
          top: 820,
          width: 860,
          height: 170,
          borderRadius: 44,
          backgroundColor: "#F0BC98",
          color: "#221F3E",
          fontFamily,
          fontWeight: 500,
          fontSize: 56,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          opacity: interpolate(frame, [0.4 * fps, 1 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
          scale: interpolate(frame, [1.6 * fps, 1.75 * fps, 2 * fps], [1, 0.94, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            output: "perceptual-scale",
          }),
        }}
      >
        Put it on my home screen
      </Interactive.Div>
      <Interactive.Div
        name="Tap ripple"
        style={{
          position: "absolute",
          left: 450,
          top: 815,
          width: 180,
          height: 180,
          borderRadius: "50%",
          border: "6px solid #F7F1E9",
          opacity: interpolate(frame, [1.6 * fps, 1.7 * fps, 2.3 * fps], [0, 0.9, 0], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
          scale: interpolate(frame, [1.6 * fps, 2.3 * fps], [0.4, 1.6], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
            output: "perceptual-scale",
          }),
        }}
      />
      <Interactive.Div
        name="Confirmation"
        style={{
          position: "absolute",
          left: 90,
          right: 90,
          top: 1120,
          textAlign: "center",
          fontFamily,
          fontStyle: "italic",
          fontSize: 60,
          lineHeight: 1.35,
          color: "#E4DCEA",
          opacity: interpolate(frame, [2.3 * fps, 3 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        It's on your home screen. No settings to learn.
      </Interactive.Div>
    </AbsoluteFill>
  );
};
