import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {SearchResultComponent} from '../seach-result/search-result.component';
import {SearchResult} from '../search.service';
import {QualifiedName} from '../../services/schema';
import {SearchResultListComponent} from './search-result-list.component';

const typeDocSearchResult: SearchResult = {
  qualifiedName: QualifiedName.from('taxi.demo.Person'),
  typeDoc: 'A human being, who knows and understands the value of the earlier seasons of community',
  matches: [
    {field: 'TYPEDOC', highlightedMatch: 'This is person, a <span class="matchedText">human</span> being'}
  ]
};
const typeNameSearchResult: SearchResult = {
  qualifiedName: QualifiedName.from('taxi.demo.Person'),
  typeDoc: 'A human being, who knows and understands the value of the earlier seasons of community',
  matches: [
    {field: 'NAME', highlightedMatch: '<span class="matchedText">Per</span>son'}
  ]
};
const typeQualifiedNameSearchResult: SearchResult = {
  qualifiedName: QualifiedName.from('taxi.demo.Person'),
  typeDoc: 'A human being, who knows and understands the value of the earlier seasons of community',
  matches: [
    {field: 'QUALIFIED_NAME', highlightedMatch: 'taxi.demo.<span class="matchedText">Per</span>son'}
  ]
};

export const searchResults: SearchResult[] = [typeDocSearchResult, typeNameSearchResult, typeQualifiedNameSearchResult];

storiesOf('SearchResults', module)
  .addDecorator(
    moduleMetadata({
      declarations: [SearchResultComponent, SearchResultListComponent],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule]
    })
  ).add('searchResult', () => {
  return {
    template: `
<div style="max-width: 450px">
    <app-search-result-list [searchResults]="searchResults"></app-search-result-list>
</div>`
    ,
    props: {
      searchResults: [
        typeDocSearchResult,
        typeNameSearchResult,
        typeQualifiedNameSearchResult
      ]
    }
  };
});

