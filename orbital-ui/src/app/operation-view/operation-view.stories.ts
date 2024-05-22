import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { service } from "../service-view/service-schema";
import { RouterTestingModule } from "@angular/router/testing";

export default {
  title: "Operation view",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, RouterTestingModule],
    }),
  ],
};

export const OperationView = () => {
  return {
    template: `<div style="padding: 40px; width: 100%; height: 100%" >
    <app-operation-view [operation]="operation"></app-operation-view>
    </div>`,
    props: {
      operation: service.operations[0],
    },
  };
};

OperationView.story = {
  name: "Operation view",
};
