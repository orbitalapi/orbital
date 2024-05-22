import { moduleMetadata } from "@storybook/angular";
import { ObjectViewComponent } from "../../object-view/object-view.component";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { PipelinesModule } from "../pipelines.module";
import { TypeAutocompleteModule } from "../../type-autocomplete/type-autocomplete.module";
import { testSchema } from "../../object-view/test-schema";
import { prepareSchema } from "../../services/types.service";
import { RouterTestingModule } from "@angular/router/testing";
import { ordersSchema } from "../../schema-display-table/orders-schema";

export default {
  title: "Pipeline Builder",

  decorators: [
    moduleMetadata({
      imports: [
        CommonModule,
        BrowserModule,
        PipelinesModule,
        TypeAutocompleteModule,
        RouterTestingModule,
      ],
    }),
  ],
};

export const Default = () => {
  return {
    template: `<div style="padding: 40px">
    <app-pipeline-builder [schema]="schema"></app-pipeline-builder>
    </div>`,
    props: {
      schema: prepareSchema(ordersSchema as any),
    },
  };
};

Default.story = {
  name: "default",
};
