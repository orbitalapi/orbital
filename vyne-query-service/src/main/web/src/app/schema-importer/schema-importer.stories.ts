import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {SchemaImporterModule} from './schema-importer.module';
import {importedSchema, schemaWithNestedTypes} from './schema-importer.data';
import {TuiRootModule} from '@taiga-ui/core';
import {testSchema} from '../object-view/test-schema';
import {searchResults} from '../search/search-result-list/search-result.stories';
import {SearchResult} from '../search/search.service';
import {SearchResultDocs} from './type-search/type-search.component';
import {OperationQueryResult} from '../services/types.service';

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
  })
  .add('type search', () => {
    return {
      template: `
<div style="padding: 40px">
      <app-type-search
      [searchResults]="searchResults"
      [searchResultDocs]="searchResultsDocs"

       (searchResultHighlighted)="onSearchResultHighlighted"></app-type-search>
      </div>
`,
      props: {
        searchResults: searchResults,
        searchResultsDocs: {
          type: schemaWithNestedTypes.types.find(t => t.name.fullyQualifiedName === 'io.vyne.demo.Person'),
          typeUsages: {
            typeName: 'io.vyne.demo.Person',
            results: []
          } as OperationQueryResult
        } as SearchResultDocs
      }
    }
  })
  .add('model display', () => {
    return {
      template: `
        <app-model-display [model]="model" [schema]="schema" [editable]="true"></app-model-display>
      `,
      props: {
        schema: schemaWithNestedTypes,
        model: schemaWithNestedTypes.types.find(t => t.name.fullyQualifiedName === 'io.vyne.demo.Person')
      }
    }
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
;
