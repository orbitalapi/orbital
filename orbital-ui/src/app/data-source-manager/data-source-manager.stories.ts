import { moduleMetadata } from "@storybook/angular";
import { RouterTestingModule } from "@angular/router/testing";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { HttpClientTestingModule } from "@angular/common/http/testing";

export default {
  title: "Connection manager",

  decorators: [
    moduleMetadata({
      imports: [
        BrowserModule,
        BrowserAnimationsModule,
        HttpClientTestingModule,
        RouterTestingModule,
      ],
    }),
  ],
};

export const ConnectionManager = () => {
  return {
    template: `
        <div style="margin: 20px">
          <app-data-source-manager></app-data-source-manager>
        </div>
      `,
  };
};

ConnectionManager.story = {
  name: "connection manager",
};
