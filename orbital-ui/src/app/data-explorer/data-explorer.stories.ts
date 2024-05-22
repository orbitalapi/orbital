import { moduleMetadata } from "@storybook/angular";
import { DataExplorerModule } from "./data-explorer.module";
import { RouterTestingModule } from "@angular/router/testing";
import { CsvOptions } from "../services/types.service";
import { NgxFileDropEntry } from "ngx-file-drop";

export default {
  title: "Data Explorer",

  decorators: [
    moduleMetadata({
      imports: [DataExplorerModule, RouterTestingModule],
    }),
  ],
};

export const DataSourceToolbar = () => ({
  template: `<div style="margin: 20px"><app-data-source-toolbar></app-data-source-toolbar></div>`,
  props: {},
});

DataSourceToolbar.story = {
  name: "data source toolbar",
};

export const CsvSelected = () => ({
  template: `<div style="margin: 20px"><app-data-source-toolbar [fileDataSource]="dataSource"></app-data-source-toolbar></div>`,
  props: {
    dataSource: {
      relativePath: "some-content.csv",
      fileEntry: {},
    } as NgxFileDropEntry,
  },
});

CsvSelected.story = {
  name: "csv selected",
};

export const CsvViewer = () => ({
  template: `<div style="margin: 20px"><app-csv-viewer [source]="data" [firstRowAsHeaders]="true"></app-csv-viewer></div>`,
  props: {
    data: {
      records: [
        ["The quick", "brown fox", "jumps over"],
        ["the lazy", "but very cute", "pupppppppy!"],
      ],
      headers: ["Col 1", "Col 2", "Col 3"],
    },
  },
});

CsvViewer.story = {
  name: "csv viewer",
};

export const FileIcon = () => ({
  template: `<div style="margin: 20px"><app-file-extension-icon extension="json"></app-file-extension-icon> </div>`,
  props: {},
});

FileIcon.story = {
  name: "file icon",
};

export const CaskPanel = () => ({
  template: `<div style="margin: 20px">
       <app-cask-panel format="json" targetTypeName="demo.Customer"></app-cask-panel>
       <app-cask-panel format="csv" targetTypeName="demo.Customer" [csvOptions]="csvOptions"
       [xmlIngestionParameters]="xmlIngestionParameters"></app-cask-panel>
    </div>`,
  props: {
    csvOptions: new CsvOptions(true, ";", "NULL"),
  },
});

CaskPanel.story = {
  name: "cask panel",
};
