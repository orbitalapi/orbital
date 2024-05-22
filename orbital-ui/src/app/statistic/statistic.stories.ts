import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { PipelinesModule } from "../pipelines/pipelines.module";
import { TypeAutocompleteModule } from "../type-autocomplete/type-autocomplete.module";
import { prepareSchema } from "../services/types.service";
import { testSchema } from "../object-view/test-schema";
import { StatisticModule } from "./statistic.module";

export default {
  title: "Statistic",

  decorators: [
    moduleMetadata({
      imports: [CommonModule, BrowserModule, StatisticModule],
    }),
  ],
};

export const Default = () => {
  return {
    template: `<div style="padding: 40px; display: flex;">
    <app-statistic label="Started" value="Today, 13:00:00"></app-statistic>
    <app-statistic label="Status" value="Active"></app-statistic>
    <app-statistic label="Ingested" value="23"></app-statistic>
    <app-statistic label="Emitted" value="120023"></app-statistic>
    <app-statistic label="Queued" value="0"></app-statistic>
    </div>`,
    props: {
      schema: prepareSchema(testSchema as any),
    },
  };
};

Default.story = {
  name: "default",
};
