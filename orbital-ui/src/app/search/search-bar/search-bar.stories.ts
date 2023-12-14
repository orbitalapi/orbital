import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {SearchBarComponent} from './search-bar.component';
import {FormsModule} from '@angular/forms';
import {NgSelectModule} from '@ng-select/ng-select';
import {Subject} from 'rxjs';
import {SearchResult} from '../search.service';
import {searchResults} from '../search-result-list/search-result.stories';
import {SearchResultComponent} from '../seach-result/search-result.component';

const searchResults$ = new Subject<SearchResult[]>();
const doSearch = function (string) {
  console.log('Faking search results');
  searchResults$.next(searchResults);
};

storiesOf('Search bar', module)
  .addDecorator(
    moduleMetadata({
      declarations: [SearchBarComponent, SearchResultComponent],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, NgSelectModule, FormsModule]
    })
  ).add('search box', () => {
  return {
    template: `
<div style="background-color: #F5F7F9; height: 4rem;
      box-shadow: 0 2px 5px #dcdcdc; display: flex; flex-direction: row;
      align-items: center;
    padding-left: 100px;">
    <app-search-bar [searchResults$]="searchResults$" (search)="onSearch($event)"></app-search-bar>
</div>`
    ,
    props: {
      searchResults$,
      onSearch: doSearch
    }
  };
});

