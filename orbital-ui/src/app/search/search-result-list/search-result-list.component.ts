import {Component, Input, OnInit} from '@angular/core';
import {SearchResult} from '../search.service';

@Component({
  selector: 'app-search-result-list',
  styleUrls: ['./search-result-list.component.scss'],
  template: `
    <div>
      <div class="search-result" *ngFor="let result of searchResults">
        <app-search-result [result]="result"></app-search-result>
      </div>
    </div>
  `
})
export class SearchResultListComponent {

  @Input()
  searchResults: SearchResult[];

}
