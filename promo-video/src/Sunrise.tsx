import {
  AbsoluteFill,
  Easing,
  Interactive,
  interpolate,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { fontFamily } from "./fonts";

// Opening: night lifts, the sun rises over the horizon, the name fades in.
export const Sunrise = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  return (
    <AbsoluteFill name="Sunrise" style={{ backgroundColor: "#14122A" }}>
      <Interactive.Div
        name="Dawn sky"
        style={{
          position: "absolute",
          inset: 0,
          background:
            "linear-gradient(180deg, #2B2650 0%, #5B4370 55%, #C98A7E 100%)",
          opacity: interpolate(frame, [0, 3 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.33, 0, 0.2, 1),
          }),
        }}
      />
      <Interactive.Div
        name="Glow"
        style={{
          position: "absolute",
          left: 90,
          top: 740,
          width: 900,
          height: 900,
          borderRadius: "50%",
          background:
            "radial-gradient(circle, rgba(247,203,166,0.65) 0%, rgba(247,203,166,0) 65%)",
          opacity: interpolate(frame, [0.5 * fps, 3.5 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      />
      <Interactive.Div
        name="Sky above horizon"
        style={{
          position: "absolute",
          left: 0,
          top: 0,
          width: 1080,
          height: 1190,
          overflow: "hidden",
        }}
      >
        <Interactive.Div
          name="Sun"
          style={{
            position: "absolute",
            left: 440,
            top: 1190,
            width: 200,
            height: 200,
            borderRadius: "50%",
            backgroundColor: "#F7CBA6",
            boxShadow: "0 0 160px 70px rgba(240,176,141,0.55)",
            translate: interpolate(frame, [0.3 * fps, 3.8 * fps], ["0px 20px", "0px -260px"], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
          }}
        />
      </Interactive.Div>
      <Interactive.Div
        name="Horizon"
        style={{
          position: "absolute",
          left: 0,
          top: 1190,
          width: 1080,
          height: 730,
          background: "linear-gradient(180deg, rgba(20,18,42,0.55), #14122A 70%)",
          borderTop: "2px solid rgba(247,221,200,0.55)",
        }}
      />
      <Interactive.Div
        name="Title"
        style={{
          position: "absolute",
          left: 80,
          right: 80,
          top: 1330,
          textAlign: "center",
          fontFamily,
          fontWeight: 500,
          fontSize: 150,
          color: "#F7F1E9",
          opacity: interpolate(frame, [1.8 * fps, 3 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
          translate: interpolate(frame, [1.8 * fps, 3 * fps], ["0px 30px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        Daily Light
      </Interactive.Div>
      <Interactive.Div
        name="Tagline"
        style={{
          position: "absolute",
          left: 80,
          right: 80,
          top: 1530,
          textAlign: "center",
          fontFamily,
          fontStyle: "italic",
          fontSize: 60,
          color: "#E4DCEA",
          opacity: interpolate(frame, [2.6 * fps, 3.6 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        A quiet start to the day.
      </Interactive.Div>
    </AbsoluteFill>
  );
};
