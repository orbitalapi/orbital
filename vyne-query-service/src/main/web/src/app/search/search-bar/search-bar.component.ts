import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Observable} from 'rxjs';
import {SearchResult} from '../search.service';
import {searchResults} from '../search-result-list/search-result.stories';
import {QualifiedName} from '../../services/schema';

@Component({
  selector: 'app-search-bar',
  styleUrls: ['./search-bar.component.scss'],
  template: `
    <img class="search-icon" src="assets/img/search.svg">
    <ng-select
      [items]="searchResults$ | async"
      placeholder="Search..."
      [trackByFn]="trackByFn"
      [searchFn]="searchFn"
      [multiple]="false"
      [minTermLength] = 3
      [typeahead]="search"
      closeOnSelect="true"
      bindLabel="qualifiedName.fullyQualifiedName"
      (change)="onSelect($event)"
      (search)="onSearch($event)"
    >
      <ng-template let-item="item" ng-option-tmp>
        <app-search-result [result]="item"></app-search-result>
      </ng-template>
    </ng-select>
    <!--    <img class="clear-icon" src="assets/img/clear-cross.svg">-->
  `
})
export class SearchBarComponent {

  @Output()
  search = new EventEmitter<string>();

  @Input()
  searchResults$: Observable<SearchResult[]>;

  @Output()
  select = new EventEmitter<QualifiedName>();


  searchFn() {
    // Because we're searching on the server, always match true
    return true;
  }

  trackByFn(item: SearchResult) {
    return item.qualifiedName.fullyQualifiedName;
  }

  onSearch($event: {
    term: string;
    items: any[];
  }) {
    if ($event.term.length >= 3) {
      this.search.emit($event.term);
    }

  }

  onSelect($event: any | null) {
    if ($event) {
      this.select.emit($event.qualifiedName);
    }

  }
}
