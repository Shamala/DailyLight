import {
  AbsoluteFill,
  Img,
  Interactive,
  interpolate,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { fontFamily } from "./fonts";

// The same card through the day, from the real screenshots: sunrise, midday,
// sunset with the evening voice, then the moon.
export const DayScene = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  return (
    <AbsoluteFill name="Through the day" style={{ backgroundColor: "#14122A" }}>
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
        }}
      >
        A sky that follows your day
      </Interactive.Div>

      <Interactive.Div
        name="Time: sunrise"
        style={{
          position: "absolute",
          left: 90,
          right: 90,
          top: 360,
          textAlign: "center",
          fontFamily,
          fontStyle: "italic",
          fontSize: 52,
          color: "#F0BC98",
          opacity: interpolate(frame, [0, 0.4 * fps, 1.5 * fps, 1.8 * fps], [0, 1, 1, 0], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        Sunrise
      </Interactive.Div>
      <Interactive.Div
        name="Time: midday"
        style={{
          position: "absolute",
          left: 90,
          right: 90,
          top: 360,
          textAlign: "center",
          fontFamily,
          fontStyle: "italic",
          fontSize: 52,
          color: "#F0BC98",
          opacity: interpolate(frame, [1.8 * fps, 2.1 * fps, 3.2 * fps, 3.5 * fps], [0, 1, 1, 0], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        Midday
      </Interactive.Div>
      <Interactive.Div
        name="Time: sunset"
        style={{
          position: "absolute",
          left: 90,
          right: 90,
          top: 360,
          textAlign: "center",
          fontFamily,
          fontStyle: "italic",
          fontSize: 52,
          color: "#F0BC98",
          opacity: interpolate(frame, [3.5 * fps, 3.8 * fps, 4.9 * fps, 5.2 * fps], [0, 1, 1, 0], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        Sunset, and a closing thought
      </Interactive.Div>
      <Interactive.Div
        name="Time: night"
        style={{
          position: "absolute",
          left: 90,
          right: 90,
          top: 360,
          textAlign: "center",
          fontFamily,
          fontStyle: "italic",
          fontSize: 52,
          color: "#F0BC98",
          opacity: interpolate(frame, [5.2 * fps, 5.5 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        Night
      </Interactive.Div>

      <Interactive.Div
        name="Phone"
        style={{
          position: "absolute",
          left: 220,
          top: 500,
          width: 640,
          height: 1280,
          borderRadius: 64,
          overflow: "hidden",
          border: "10px solid #2E2750",
          boxShadow: "0 40px 120px rgba(0,0,0,0.55)",
        }}
      >
        <Img
          name="Sunrise screen"
          src={staticFile("01-home-morning.png")}
          style={{ position: "absolute", width: 620, height: 1260, objectFit: "cover" }}
        />
        <Img
          name="Midday screen"
          src={staticFile("03-home-light.png")}
          style={{
            position: "absolute",
            width: 620,
            height: 1260,
            objectFit: "cover",
            opacity: interpolate(frame, [1.6 * fps, 2.1 * fps], [0, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
            }),
          }}
        />
        <Img
          name="Sunset screen"
          src={staticFile("02-home-evening.png")}
          style={{
            position: "absolute",
            width: 620,
            height: 1260,
            objectFit: "cover",
            opacity: interpolate(frame, [3.3 * fps, 3.8 * fps], [0, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
            }),
          }}
        />
        <Img
          name="Night screen"
          src={staticFile("04-home-bar-only.png")}
          style={{
            position: "absolute",
            width: 620,
            height: 1260,
            objectFit: "cover",
            opacity: interpolate(frame, [5 * fps, 5.5 * fps], [0, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
            }),
          }}
        />
      </Interactive.Div>
    </AbsoluteFill>
  );
};
