import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { service } from "./service-schema";
import { RouterTestingModule } from "@angular/router/testing";

export default {
  title: "Service view",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, RouterTestingModule],
    }),
  ],
};

export const ServiceView = () => {
  return {
    template: `<div style="padding: 40px; width: 100%; height: 100%" >
    <app-service-view [service]="service"></app-service-view>
    </div>`,
    props: {
      service: service,
    },
  };
};

ServiceView.story = {
  name: "Service view",
};
