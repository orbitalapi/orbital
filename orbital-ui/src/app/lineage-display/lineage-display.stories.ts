import { moduleMetadata } from "@storybook/angular";
import { ObjectViewComponent } from "../object-view/object-view.component";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { LineageDisplayModule } from "./lineage-display.module";
import {
  LINEAGE_GRAPH,
  LINEAGE_GRAPH_WITH_COMPLEX_EXPRESSION,
  LINEAGE_GRAPH_WITH_EVALUATED_EXPRESSION,
  LINEAGE_GRAPH_WITH_FAILED_EXPRESSION,
} from "./lineage-data";
import { QueryService } from "../services/query.service";
import {
  lineageSankeyChartData,
  lineageSankeyChartDataFromKafkaTopic,
} from "./lineage-sankey-chart.data";

class MockQueryService implements Partial<QueryService> {}

export default {
  title: "Lineage display",

  decorators: [
    moduleMetadata({
      declarations: [],
      providers: [{ provide: QueryService, useClass: MockQueryService }],
      imports: [CommonModule, BrowserModule, LineageDisplayModule],
    }),
  ],
};

export const Default = () => {
  return {
    template: `<div style="padding: 40px">
    <app-lineage-display
    [dataSource]="lineageGraph"
    [instance]="typeNamedInstance"
    ></app-lineage-display>
    </div>`,
    props: {
      lineageGraph: LINEAGE_GRAPH,
      typeNamedInstance: {
        typeName: "demo.RewardsBalance",
        value: 2300,
      },
    },
  };
};

Default.story = {
  name: "default",
};

export const EvaluatedExpression = () => {
  return {
    template: `<div style="padding: 40px">
    <app-lineage-display
    [dataSource]="lineageGraph"
    [instance]="typeNamedInstance"
    ></app-lineage-display>
    </div>`,
    props: {
      lineageGraph: LINEAGE_GRAPH_WITH_EVALUATED_EXPRESSION,
      typeNamedInstance: {
        typeName: "demo.RewardsBalance",
        value: 2300,
      },
    },
  };
};

EvaluatedExpression.story = {
  name: "evaluated expression",
};

export const FailedExpression = () => {
  return {
    template: `<div style="padding: 40px">
    <app-lineage-display
    [dataSource]="lineageGraph"
    [instance]="typeNamedInstance"
    ></app-lineage-display>
    </div>`,
    props: {
      lineageGraph: LINEAGE_GRAPH_WITH_FAILED_EXPRESSION,
      typeNamedInstance: {
        typeName: "demo.RewardsBalance",
        value: 2300,
      },
    },
  };
};

FailedExpression.story = {
  name: "failed expression",
};

export const ComplexExpression = () => {
  return {
    template: `<div style="padding: 40px">
    <app-lineage-display
    [dataSource]="lineageGraph"
    [instance]="typeNamedInstance"
    ></app-lineage-display>
    </div>`,
    props: {
      lineageGraph: JSON.parse(
        JSON.stringify(LINEAGE_GRAPH_WITH_COMPLEX_EXPRESSION),
      ),
      typeNamedInstance: {
        typeName: "demo.RewardsBalance",
        value: 2300,
      },
    },
  };
};

ComplexExpression.story = {
  name: "complex expression",
};

export const QueryLineageChart = () => {
  return {
    template: `<div style="padding: 40px">
<app-query-lineage [rows]="chartData"></app-query-lineage>
    </div>`,
    props: {
      chartData: lineageSankeyChartData,
    },
  };
};

QueryLineageChart.story = {
  name: "query lineage chart",
};

export const QueryLineageChartFromKafka = () => {
  return {
    template: `<div style="padding: 40px">
<app-query-lineage [rows]="chartData"></app-query-lineage>
    </div>`,
    props: {
      chartData: lineageSankeyChartDataFromKafkaTopic,
    },
  };
};

QueryLineageChartFromKafka.story = {
  name: "query lineage chart from Kafka",
};
