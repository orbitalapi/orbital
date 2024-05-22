import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { CodeEditorComponent } from "./code-editor.component";

export default {
  title: "CodeEditor",

  decorators: [
    moduleMetadata({
      declarations: [CodeEditorComponent],
      imports: [CommonModule, BrowserModule],
    }),
  ],
};

export const Default = () => {
  return {
    template: `<div style="height: 100vh"> <app-code-editor></app-code-editor> </div>`,
    props: {},
  };
};

Default.story = {
  name: "default",
};
