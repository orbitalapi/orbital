import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {CatalogExplorerPanelModule} from "./catalog-explorer-panel.module";
import {HttpClientModule} from "@angular/common/http";
import {SearchModule} from "../search/search.module";
import {Environment, ENVIRONMENT} from "../services/environment";
import {searchResults} from "./searchResults";

storiesOf('Catalog explorer panel', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      providers: [
        {
          provide: ENVIRONMENT, useValue: {
            serverUrl: 'http://localhost:9022',
            production: false
          } as Environment
        }
      ],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, CatalogExplorerPanelModule, HttpClientModule]
    })
  )
  .add('default', () => {
    return {
      template: `<div style="padding: 40px; width: 300px">
<app-catalog-explorer-panel></app-catalog-explorer-panel>
    </div>`,
      props: {}
    };
  }).add('search results', () => {
    return {
      template: `<div style="padding: 40px; width: 300px">
<app-catalog-panel-search-results [searchResults]="searchResults"></app-catalog-panel-search-results>
    </div>`,
      props: {
        searchResults: searchResults
      }
    };
  });
