import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { LineageGraphModule } from "src/app/type-viewer/lineage-graph/lineage-graph.module";
import { Observable } from "rxjs/internal/Observable";
import { LineageNodeDiagramComponent } from "src/app/type-viewer/lineage-graph/lineage-node-diagram/lineage-node-diagram.component";
import {
  LINEAGE_DEMO_DATA,
  LINEAGE_DEMO_SCHEMA,
} from "src/app/type-viewer/lineage-graph/lineage-node-diagram/lineage-graph-data";
import { of } from "rxjs";

export default {
  title: "Service Lineage Graph",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [
        CommonModule,
        BrowserModule,
        BrowserAnimationsModule,
        LineageGraphModule,
      ],
    }),
  ],
};

export const InboundDependencies = () => {
  const data = of(LINEAGE_DEMO_DATA);
  const schema = of(LINEAGE_DEMO_SCHEMA);
  return {
    template: `<div style="padding: 40px">
<app-lineage-node-diagram [schemaGraph$]="data" [schema$]="schema" style="width: 1500px; height: 1200px;" [initialServices]="displayedMembers"></app-lineage-node-diagram>
    </div>`,
    props: {
      data,
      schema,
      displayedMembers: ["demo.orderFeeds.dataWarehouse.DataWarehouse"],
    },
  };
};

InboundDependencies.story = {
  name: "inbound dependencies",
};

export const OutboundDependencies = () => {
  const data = of(LINEAGE_DEMO_DATA);
  const schema = of(LINEAGE_DEMO_SCHEMA);
  return {
    template: `<div style="padding: 40px">
<app-lineage-node-diagram [schemaGraph$]="data" [schema$]="schema" style="width: 1500px; height: 1200px;" [initialServices]="displayedMembers"></app-lineage-node-diagram>
    </div>`,
    props: {
      data,
      schema,
      displayedMembers: ["demo.orderFeeds.icap.IcapOrderFeedService"],
    },
  };
};

OutboundDependencies.story = {
  name: "outbound dependencies",
};
