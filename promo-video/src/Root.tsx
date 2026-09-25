import "./index.css";
import { Composition, Folder } from "remotion";
import { DailyLightPromo } from "./DailyLightPromo";
import { DayScene } from "./DayScene";
import { EndCard } from "./EndCard";
import { OneTapScene } from "./OneTapScene";
import { Sunrise } from "./Sunrise";
import { WidgetScene } from "./WidgetScene";

export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Folder name="DailyLightPromo-Scenes">
        <Composition id="Sunrise" component={Sunrise} width={1080} height={1920} fps={30} durationInFrames={150} />
        <Composition id="Widget" component={WidgetScene} width={1080} height={1920} fps={30} durationInFrames={150} />
        <Composition id="ThroughTheDay" component={DayScene} width={1080} height={1920} fps={30} durationInFrames={210} />
        <Composition id="OneTap" component={OneTapScene} width={1080} height={1920} fps={30} durationInFrames={135} />
        <Composition id="EndCard" component={EndCard} width={1080} height={1920} fps={30} durationInFrames={150} />
      </Folder>
      <Composition
        id="DailyLightPromo"
        component={DailyLightPromo}
        width={1080}
        height={1920}
        fps={30}
        durationInFrames={735}
      />
    </>
  );
};
