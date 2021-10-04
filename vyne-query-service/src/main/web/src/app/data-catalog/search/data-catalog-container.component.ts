import {Component, OnInit} from '@angular/core';
import {SearchResult, SearchService} from '../../search/search.service';

import {Observable, of} from 'rxjs';
import {tap} from 'rxjs/operators';
import {ActivatedRoute, Router} from '@angular/router';

@Component({
  selector: 'app-data-catalog-container',
  template: `
    <mat-progress-bar mode="query" [class.loading]="loading"></mat-progress-bar>
    <app-data-catalog-search [searchResults]="searchResults"
                             [initialSearchTerm]="lastSearchTerm"
                             [atLeastOneSearchCompleted]="searchPerformed"
                             (searchValueUpdated)="search($event)"
    ></app-data-catalog-search>
  `,
  styleUrls: ['./data-catalog-container.component.scss']
})
export class DataCatalogContainerComponent {

  constructor(private service: SearchService, private router: Router, private activatedRoute: ActivatedRoute) {
    activatedRoute.queryParams.subscribe(params => {
      if (params.search && this.lastSearchTerm !== params.search) {
        this.search(params.search);
      }
    });
  }

  loading = false;
  searchPerformed = false;
  searchResults: Observable<SearchResult[]> = of([]);
  lastSearchTerm = '';

  search(searchTerm: string) {
    this.loading = true;
    this.lastSearchTerm = searchTerm;
    this.router.navigate([],
      {
        relativeTo: this.activatedRoute,
        queryParams: {search: searchTerm},
        queryParamsHandling: 'merge',
        replaceUrl: true
      });
    this.searchResults = this.service.search(searchTerm)
      .pipe(tap(next => {
        this.searchPerformed = true;
        this.loading = false;
      }, error => {
        console.log('Search failed: ' + JSON.stringify(error));
        this.loading = false;
      }));
  }
}
