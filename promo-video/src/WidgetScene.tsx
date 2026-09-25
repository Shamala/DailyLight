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

// The real widget on a real home screen, the first thing you see.
export const WidgetScene = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  return (
    <AbsoluteFill name="Widget" style={{ backgroundColor: "#14122A" }}>
      <Interactive.Div
        name="Caption"
        style={{
          position: "absolute",
          left: 90,
          right: 90,
          top: 130,
          textAlign: "center",
          fontFamily,
          fontSize: 84,
          lineHeight: 1.2,
          color: "#F7F1E9",
          opacity: interpolate(frame, [0, 0.8 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        The first thing you see each morning
      </Interactive.Div>
      <Interactive.Div
        name="Phone"
        style={{
          position: "absolute",
          left: 220,
          top: 470,
          width: 640,
          height: 1280,
          borderRadius: 64,
          overflow: "hidden",
          border: "10px solid #2E2750",
          boxShadow: "0 40px 120px rgba(0,0,0,0.55)",
          opacity: interpolate(frame, [0.3 * fps, 1.2 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
          translate: interpolate(frame, [0.3 * fps, 1.4 * fps], ["0px 140px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.spring({ damping: 200 }),
          }),
        }}
      >
        <Img
          name="Home screen"
          src={staticFile("01-home-morning.png")}
          style={{ width: 620, height: 1260, objectFit: "cover" }}
        />
      </Interactive.Div>
    </AbsoluteFill>
  );
};
