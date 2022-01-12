import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {of} from 'rxjs';
import {searchResults} from '../../search/search-result-list/search-result.stories';
import {schemaWithNestedTypes} from '../../schema-importer/schema-importer.data';
import {OperationQueryResult} from '../../services/types.service';
import {SearchResultDocs} from './type-search.component';
import {TypeViewerModule} from '../type-viewer.module';

storiesOf('Type search panel', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, TypeViewerModule]
    })
  )
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
        searchResults: of(searchResults),
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
  .add('in progress', () => {
    return {
      template: `
<div style="padding: 40px">
      <app-type-search
      [searchResults]="searchResults"
      [searchResultDocs]="searchResultsDocs"
        [working]="true"
       (searchResultHighlighted)="onSearchResultHighlighted"></app-type-search>
      </div>
`,
      props: {
        searchResults: of(searchResults),
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
