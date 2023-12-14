import {Component} from '@angular/core';
import {ExpandableSearchResult, SearchResult, SearchService} from '../../search/search.service';

import {Observable, of} from 'rxjs';
import {map, tap} from 'rxjs/operators';
import {ActivatedRoute, Router} from '@angular/router';
import {TypesService} from 'src/app/services/types.service';
import {Schema} from 'src/app/services/schema';

@Component({
  selector: 'app-data-catalog-container',
  template: `
    <app-header-component-layout title='Catalog'
                                 description='The data catalog contains all models, attributes, services and operations published to Orbital. You can search by
          name, or search for tags using # (eg: #MyTag)' [padBottom]='false' [displayBody]='false'>
      <ng-container ngProjectAs='header-components'>
        <tui-tabs [(activeItemIndex)]='activeTabIndex'>
          <!--          <button tuiTab>Push from application</button>-->
          <!--          <button tuiTab>CI Pipeline</button>-->
          <button tuiTab>Search</button>
          <button tuiTab>Services diagram</button>
        </tui-tabs>
      </ng-container>

    </app-header-component-layout>
    <mat-progress-bar mode='query' [class.loading]='loading'></mat-progress-bar>
    <app-data-catalog-search [searchResults]='searchResults'
                             [initialSearchTerm]='lastSearchTerm'
                             [atLeastOneSearchCompleted]='searchPerformed'
                             (searchValueUpdated)='search($event)'
                             *ngIf='activeTabIndex===0'
    ></app-data-catalog-search>
    <app-schema-diagram *ngIf='activeTabIndex===1' [schema$]='schema$' displayedMembers='services'></app-schema-diagram>
  `,
  styleUrls: ['./data-catalog-container.component.scss']
})
export class DataCatalogContainerComponent {
  schema$: Observable<Schema>;
  activeTabIndex: number = 0;

  constructor(private service: SearchService, private router: Router, private activatedRoute: ActivatedRoute,
              private schemaService: TypesService) {
    activatedRoute.queryParams.subscribe(params => {
      if (params.search && this.lastSearchTerm !== params.search) {
        this.search(params.search);
      }
    });
    this.schema$ = schemaService.getTypes();
  }

  loading = false;
  searchPerformed = false;
  searchResults: Observable<ExpandableSearchResult[]> = of([]);
  lastSearchTerm = '';

  search(searchTerm: string) {
    this.loading = true;
    this.lastSearchTerm = searchTerm;
    this.router.navigate([],
      {
        relativeTo: this.activatedRoute,
        queryParams: { search: searchTerm },
        queryParamsHandling: 'merge',
        replaceUrl: true
      });
    this.searchResults = this.service.search(searchTerm)
      .pipe(
        map((searchResults: SearchResult[]) => searchResults.map(searchResult => this.toExpandableSearch(searchResult))),
        tap(_ => {
          this.searchPerformed = true;
          this.loading = false;
        }, error => {
          console.log('Search failed: ' + JSON.stringify(error));
          this.loading = false;
        }));
  }

  toExpandableSearch(searchResult: SearchResult) {
    return {
      ...searchResult,
      consumersExpanded: false,
      producersExpanded: false
    };
  }
}
