import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { SimpleCodeViewerComponent } from "./simple-code-viewer.component";
import { sampleTaxi } from "./sample-taxi";
import { SimpleCodeViewerModule } from "./simple-code-viewer.module";

export default {
  title: "Simple code viewer",

  decorators: [
    moduleMetadata({
      imports: [CommonModule, BrowserModule, SimpleCodeViewerModule],
    }),
  ],
};

export const Default = () => {
  return {
    template: `<app-simple-code-viewer lang="taxi" [content]="source"></app-simple-code-viewer>`,
    props: {
      source: sampleTaxi,
    },
  };
};

Default.story = {
  name: "default",
};

export const NotExpandable = () => {
  return {
    template: `<app-simple-code-viewer [expandable]="false" lang="taxi" [content]="source"></app-simple-code-viewer>`,
    props: {
      source: sampleTaxi,
    },
  };
};

NotExpandable.story = {
  name: "not expandable",
};
