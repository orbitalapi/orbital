import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {SchemaImporterModule} from './schema-importer.module';
import {importedSchema} from './schema-importer.data';
import {TuiRootModule} from '@taiga-ui/core';

storiesOf('Schema importer', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, SchemaImporterModule, TuiRootModule]
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
  });
