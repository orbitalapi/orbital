import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { DataWorkbookModule } from "./data-workbook.module";
import { ParsedCsvContent } from "../services/types.service";
import { RouterTestingModule } from "@angular/router/testing";
import { testSchema } from "../object-view/test-schema";

const csvContents = `symbol,quantity
GBPNZD,15000
AUDNZD,20000`;

const parsedCsv: ParsedCsvContent = {
  headers: ["symbol", "quantity"],
  records: [
    ["GBPNZD", "15000"],
    ["AUDNZD", "25000"],
  ],
};

export default {
  title: "Data Workbook editor",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [
        CommonModule,
        BrowserModule,
        BrowserAnimationsModule,
        DataWorkbookModule,
        RouterTestingModule,
      ],
    }),
  ],
};

export const SourceModelSelector = () => {
  return {
    template: `<div style="padding: 40px; background-color: #f5f6fa" >
<app-source-mode-selector></app-source-mode-selector>
    </div>`,
    props: {},
  };
};

SourceModelSelector.story = {
  name: "source model selector",
};

export const DataSourcePanel = () => {
  return {
    template: `<div style="padding: 40px; background-color: #f5f6fa" >
<app-data-source-panel></app-data-source-panel>
    </div>`,
    props: {},
  };
};

DataSourcePanel.story = {
  name: "data source panel",
};

export const DataSourcePanelWithFileSelected = () => {
  return {
    template: `<div style="padding: 40px; background-color: #f5f6fa" >
<app-data-source-panel
    [fileDataSource]="file"
    [fileContents]="fileContents"
    [parsedCsvContent]="parsedCsvContent"
></app-data-source-panel>
    </div>`,
    props: {
      fileContents: csvContents,
      parsedCsvContent: parsedCsv,
      file: {
        relativePath: "/src/foo/bar.csv",
        fileEntry: null,
      },
    },
  };
};

DataSourcePanelWithFileSelected.story = {
  name: "data source panel with file selected",
};

export const SchemaSelector = () => {
  return {
    template: `<div style="padding: 40px; background-color: #f5f6fa" >
<app-workbook-schema-selector [schema]="schema"></app-workbook-schema-selector>
    </div>`,
    props: {
      fileContents: csvContents,
      parsedCsvContent: parsedCsv,
      schema: testSchema,
      file: {
        relativePath: "/src/foo/bar.csv",
        fileEntry: null,
      },
    },
  };
};

SchemaSelector.story = {
  name: "schema selector",
};
