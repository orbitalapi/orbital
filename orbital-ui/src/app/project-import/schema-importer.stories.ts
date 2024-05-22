import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import {
  importedSchema,
  schemaWithNestedTypes,
} from "../data-source-import/data-source-import.data";
import { TuiRootModule } from "@taiga-ui/core";
import { RouterTestingModule } from "@angular/router/testing";

export default {
  title: "Schema importer",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [
        CommonModule,
        BrowserModule,
        BrowserAnimationsModule,
        TuiRootModule,
        RouterTestingModule,
      ],
    }),
  ],
};

export const Default = () => {
  return {
    template: `
<tui-root>
<div style="padding: 40px">
<app-data-source-import [importedSchema]="importedSchema"></app-data-source-import>
    </div>
</tui-root>`,
    props: {
      importedSchema,
    },
  };
};

Default.story = {
  name: "default",
};

export const SchemaSourceSelector = () => {
  return {
    template: `
<tui-root>
      <div style="padding: 40px">

      <app-data-source-panel></app-data-source-panel>
      </div>
      </tui-root>
      `,
  };
};

SchemaSourceSelector.story = {
  name: "schema source selector",
};

export const SchemaExplorerTable = () => {
  return {
    template: `
      <tui-root>
      <div style="padding: 40px">
      <app-schema-member-type-explorer
        [schema]="schema"
      [partialSchema]="importedSchema"></app-schema-member-type-explorer>
      </div>

      `,
    props: {
      schema: schemaWithNestedTypes,
      importedSchema: importedSchema,
    },
  };
};

SchemaExplorerTable.story = {
  name: "schema explorer table",
};
