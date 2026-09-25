import { loadFont } from "@remotion/google-fonts/Lora";

// Lora, the typeface the widget itself is set in.
export const { fontFamily } = loadFont("normal", {
  weights: ["400", "500"],
  subsets: ["latin"],
});
loadFont("italic", { weights: ["400"], subsets: ["latin"] });
