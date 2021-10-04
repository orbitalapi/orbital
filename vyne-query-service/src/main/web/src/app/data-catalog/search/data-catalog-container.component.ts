import { Component, OnInit } from '@angular/core';
import {SearchResult, SearchService} from '../../search/search.service';

import {Observable, of} from 'rxjs';

@Component({
  selector: 'app-data-catalog-container',
  template: `
    <app-data-catalog-search [searchResults]="searchResults | async"
                             (searchValueUpdated)="search($event)"
    ></app-data-catalog-search>
  `,
  styleUrls: ['./data-catalog-container.component.scss']
})
export class DataCatalogContainerComponent implements OnInit {

  constructor(private service: SearchService) {
  }

  searchResults: Observable<SearchResult[]> = of([]);

  search(searchTerm: string) {
    this.searchResults = this.service.search(searchTerm);
  }

  ngOnInit() {
  }

}
