import { Audio } from "@remotion/media";
import { linearTiming, TransitionSeries } from "@remotion/transitions";
import { staticFile } from "remotion";
import { fade } from "@remotion/transitions/fade";
import { DayScene } from "./DayScene";
import { EndCard } from "./EndCard";
import { OneTapScene } from "./OneTapScene";
import { Sunrise } from "./Sunrise";
import { WidgetScene } from "./WidgetScene";

export const DailyLightPromo = () => (
  <>
    {/* "First Light": original music composed for this video (music/compose.py), CC0. */}
    <Audio name="Music" src={staticFile("first-light.mp3")} volume={0.8} />
    <TransitionSeries>
      <TransitionSeries.Sequence name="Sunrise" durationInFrames={150}>
        <Sunrise />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition
        presentation={fade()}
        timing={linearTiming({ durationInFrames: 15 })}
      />
      <TransitionSeries.Sequence name="Widget" durationInFrames={150}>
        <WidgetScene />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition
        presentation={fade()}
        timing={linearTiming({ durationInFrames: 15 })}
      />
      <TransitionSeries.Sequence name="Through the day" durationInFrames={210}>
        <DayScene />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition
        presentation={fade()}
        timing={linearTiming({ durationInFrames: 15 })}
      />
      <TransitionSeries.Sequence name="One tap" durationInFrames={135}>
        <OneTapScene />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition
        presentation={fade()}
        timing={linearTiming({ durationInFrames: 15 })}
      />
      <TransitionSeries.Sequence name="End card" durationInFrames={150}>
        <EndCard />
      </TransitionSeries.Sequence>
    </TransitionSeries>
  </>
);
