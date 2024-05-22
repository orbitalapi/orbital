import { moduleMetadata } from "@storybook/angular";
import { InlineQueryRunnerModule } from "./inline-query-runner.module";
import { VyneServicesModule } from "../services/vyne-services.module";

export default {
  title: "Inline Query Runner",

  decorators: [
    moduleMetadata({
      imports: [InlineQueryRunnerModule, VyneServicesModule],
    }),
  ],
};

export const DataSourceToolbar = () => {
  return {
    template: `<div style="margin: 20px; width: 300px">
<app-inline-query-runner targetType="CustomerName"></app-inline-query-runner></div>`,
    props: {},
  };
};

DataSourceToolbar.story = {
  name: "data source toolbar",
};
