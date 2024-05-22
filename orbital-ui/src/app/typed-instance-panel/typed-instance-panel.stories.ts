import { moduleMetadata } from "@storybook/angular";
import { RouterTestingModule } from "@angular/router/testing";
import { TypedInstancePanelModule } from "./typed-instance-panel.module";
import { sampleOrderEventType } from "../data-explorer/sample-type";

export default {
  title: "Typed instance panel",

  decorators: [
    moduleMetadata({
      imports: [TypedInstancePanelModule, RouterTestingModule],
    }),
  ],
};

export const TypedInstancePanel = () => {
  return {
    template: `<div style="margin: 20px">
        <app-typed-instance-panel [instance]="value" [type]="testType"></app-typed-instance-panel>
    </div>`,
    props: {
      testType: sampleOrderEventType,
      value: {
        typeName: "bank.orders.OrderEventType",
        value: "Open",
      },
    },
  };
};

TypedInstancePanel.story = {
  name: "typed instance panel",
};
