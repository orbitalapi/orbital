import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {SchemaImporterModule} from './schema-importer.module';
import {importedSchema, schemaWithNestedTypes} from './schema-importer.data';
import {TuiRootModule} from '@taiga-ui/core';
import {RouterTestingModule} from '@angular/router/testing';

storiesOf('Schema importer', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, SchemaImporterModule, TuiRootModule, RouterTestingModule]
    })
  )
  .add('default', () => {
    return {
      template: `
<tui-root>
<div style="padding: 40px">
<app-schema-importer [importedSchema]="importedSchema"></app-schema-importer>
    </div>
</tui-root>`,
      props: {
        importedSchema
      }
    };
  })
  .add('schema source selector', () => {
    return {
      template: `
<tui-root>
      <div style="padding: 40px">

      <app-schema-source-panel></app-schema-source-panel>
      </div>
      </tui-root>
      `
    }
  })
  .add('schema explorer table', () => {
    return {
      template: `
      <tui-root>
      <div style="padding: 40px">
      <app-schema-explorer-table
        [schema]="schema"
      [partialSchema]="importedSchema"></app-schema-explorer-table>
      </div>

      `,
      props: {
        schema: schemaWithNestedTypes,
        importedSchema: importedSchema
      }
    }
  })
;
