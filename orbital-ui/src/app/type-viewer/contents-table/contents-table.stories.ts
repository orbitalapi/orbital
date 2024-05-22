import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { Contents } from "../toc-host.directive";
import { ContentsTableComponent } from "./contents-table.component";

export const contents: Contents = {
  items: [
    { name: "What is Vyne?", slug: "what-is-vyne" },
    { name: "What is Life?", slug: "what-is-life" },
    { name: "Getting started", slug: "getting-started" },
  ],
};

export default {
  title: "ContentsTable",

  decorators: [
    moduleMetadata({
      declarations: [ContentsTableComponent],
      imports: [CommonModule, BrowserModule],
    }),
  ],

  excludeStories: ["contents"],
};

export const Default = () => {
  return {
    template: `<app-contents-table [contents]="contents"></app-contents-table>`,
    props: {
      contents,
    },
  };
};

Default.story = {
  name: "default",
};
