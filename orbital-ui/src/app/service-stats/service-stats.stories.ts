import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import {
  RemoteOperationPerformanceStats,
  ResponseCodeGroup,
} from "../services/query.service";
import { RouterTestingModule } from "@angular/router/testing";
import { ServiceStatsModule } from "./service-stats.module";

const operationStats: RemoteOperationPerformanceStats[] = [
  {
    operationQualifiedName: "",
    serviceName: "Service",
    operationName: "Get all Customers",
    callsInitiated: 34,
    averageTimeToFirstResponse: 7,
    totalWaitTime: 204,
    responseCodes: {
      HTTP_2XX: 24,
      HTTP_3XX: 0,
      HTTP_4XX: 5,
      HTTP_5XX: 5,
      SUCCESS: 1,
      FAIL: 5,
      UNKNOWN: 0,
    },
  },
  {
    operationQualifiedName: "",
    serviceName: "Service",
    operationName: "Find customer name",
    callsInitiated: 82,
    averageTimeToFirstResponse: 2,
    totalWaitTime: 503,
    responseCodes: {
      HTTP_2XX: 45,
      HTTP_3XX: 0,
      HTTP_4XX: 78,
      HTTP_5XX: 3,
      SUCCESS: 1,
      FAIL: 3,
      UNKNOWN: 0,
    },
  },
];

export default {
  title: "Service stats",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [
        CommonModule,
        BrowserModule,
        ServiceStatsModule,
        RouterTestingModule,
      ],
    }),
  ],
};

export const Default = () => {
  return {
    template: `<div style="padding: 40px; width: 800px">
<app-service-stats [operationStats]="operationStats"></app-service-stats>
</div>`,
    props: {
      operationStats,
    },
  };
};

Default.story = {
  name: "default",
};
