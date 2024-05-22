import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { HttpClientModule } from "@angular/common/http";
import { QueryHistoryPanelModule } from "./query-history-panel.module";
import { VyneServicesModule } from "../services/vyne-services.module";
import { Environment, ENVIRONMENT } from "../services/environment";

export default {
  title: "Query History Panel",

  decorators: [
    moduleMetadata({
      declarations: [],
      providers: [
        {
          provide: ENVIRONMENT,
          useValue: {
            serverUrl: "http://localhost:9022",
            production: false,
          } as Environment,
        },
      ],
      imports: [
        CommonModule,
        BrowserModule,
        BrowserAnimationsModule,
        HttpClientModule,
        QueryHistoryPanelModule,
        VyneServicesModule,
      ],
    }),
  ],
};

export const QueryHistoryPanel = () => {
  return {
    template: `<div style="padding: 40px; width: 400px; background-color: #f8fafc;">
<app-query-history-panel></app-query-history-panel>
    </div>`,
    props: {},
  };
};

QueryHistoryPanel.story = {
  name: "Query history panel",
};
