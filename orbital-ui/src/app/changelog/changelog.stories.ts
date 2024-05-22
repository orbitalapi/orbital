import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { ChangelogModule } from "./changelog.module";
import { CHANGELOG_DATA } from "./changelog-data";

export default {
  title: "Notebook editor",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [
        CommonModule,
        BrowserModule,
        BrowserAnimationsModule,
        ChangelogModule,
      ],
    }),
  ],
};

export const Default = () => {
  return {
    template: `<div style="padding: 40px">
<app-changelog-list [changeLogEntries]="changeLogEntries"></app-changelog-list>
    </div>`,
    props: {
      changeLogEntries: CHANGELOG_DATA,
    },
  };
};

Default.story = {
  name: "default",
};
