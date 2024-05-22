import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { QueryHistoryModule } from "./query-history.module";
import { QueryHistorySummary, ResponseStatus } from "../services/query.service";
import { RouterTestingModule } from "@angular/router/testing";

export default {
  title: "Query History",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [
        CommonModule,
        BrowserModule,
        QueryHistoryModule,
        RouterTestingModule,
      ],
    }),
  ],
};

export const QueryCards = () => {
  return {
    template: `<div style="padding: 40px; width: 100%;">
<app-query-list [historyRecords]="historyRecords"></app-query-list>
</div>`,
    props: {
      historyRecords: [
        {
          durationMs: 20300,
          taxiQl: "find { foo.bar.baz }",
          queryId: "123",
          recordCount: 2300,
          responseStatus: ResponseStatus.COMPLETED,
          startTime: new Date(),
        },
        {
          durationMs: 200300,
          taxiQl: "find { foo.bar.baz }",
          queryId: "123",
          recordCount: 2300,
          responseStatus: ResponseStatus.COMPLETED,
          startTime: new Date(),
        },
        {
          durationMs: 2300,
          taxiQl: "find { foo.bar.baz }",
          queryId: "123",
          recordCount: 0,
          responseStatus: ResponseStatus.ERROR,
          startTime: new Date(),
        },
      ] as QueryHistorySummary[],
    },
  };
};

QueryCards.story = {
  name: "Query cards",
};
