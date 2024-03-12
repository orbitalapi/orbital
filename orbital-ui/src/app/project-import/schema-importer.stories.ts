import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {importedSchema, schemaWithNestedTypes} from '../data-source-import/data-source-import.data';
import {TuiRootModule} from '@taiga-ui/core';
import {RouterTestingModule} from '@angular/router/testing';

storiesOf('Schema importer', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, TuiRootModule, RouterTestingModule]
    })
  )
  .add('default', () => {
    return {
      template: `
<tui-root>
<div style="padding: 40px">
<app-data-source-import [importedSchema]="importedSchema"></app-data-source-import>
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

      <app-data-source-panel></app-data-source-panel>
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
      <app-schema-member-type-explorer
        [schema]="schema"
      [partialSchema]="importedSchema"></app-schema-member-type-explorer>
      </div>

      `,
      props: {
        schema: schemaWithNestedTypes,
        importedSchema: importedSchema
      }
    }
  })
;
