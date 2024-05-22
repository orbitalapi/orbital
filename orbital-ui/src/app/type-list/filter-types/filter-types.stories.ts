import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { TypeListModule } from "src/app/type-list/type-list.module";
import { RouterTestingModule } from "@angular/router/testing";

export default {
  title: "Type catalog filter",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [
        CommonModule,
        BrowserModule,
        BrowserAnimationsModule,
        TypeListModule,
        RouterTestingModule,
      ],
    }),
  ],
};

export const Default = () => {
  return {
    template: `<div style="padding: 40px">
<app-filter-types></app-filter-types>
    </div>`,
    props: {},
  };
};

Default.story = {
  name: "default",
};
