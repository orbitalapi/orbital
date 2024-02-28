import { Component, DestroyRef } from '@angular/core';
import { ActivatedRoute, NavigationEnd, NavigationSkipped, Router, Scroll } from '@angular/router';
import { Observable, of } from 'rxjs';
import { filter, map, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ExpandableSearchResult, SearchResult, SearchService } from '../../search/search.service';
import { TypesService } from 'src/app/services/types.service';
import { Schema } from 'src/app/services/schema';

@Component({
  selector: 'app-data-catalog-container',
  template: `
    <app-header-component-layout title='Catalog'
                                 description='The data catalog contains all models, attributes, services and operations published to Orbital. You can search by
          name, or search for tags using # (eg: #MyTag)' [padBottom]='false' [displayBody]='false'>
      <ng-container ngProjectAs='header-components'>
        <tui-tabs>
          <!--          <button tuiTab>Push from application</button>-->
          <!--          <button tuiTab>CI Pipeline</button>-->
          <button tuiTab routerLink="/catalog" routerLinkActive>Search</button>
          <button tuiTab routerLink="/catalog/diagram" routerLinkActive>Services diagram</button>
        </tui-tabs>
      </ng-container>

    </app-header-component-layout>
    <app-data-catalog-search [searchResults]='searchResults'
                             [initialSearchTerm]='lastSearchTerm'
                             [atLeastOneSearchCompleted]='searchPerformed'
                             (searchValueUpdated)='search($event)'
                             *ngIf='(activeTabIndex$ | async) === 0'
    >
      <progress
        max="100"
        tuiProgressBar
        size='xs'
        new
        *ngIf="isLoading"
      ></progress>
    </app-data-catalog-search>
    <app-schema-diagram *ngIf='(activeTabIndex$ | async) === 1' [schema$]='schema$'
                        displayedMembers='services'></app-schema-diagram>
  `,
  styleUrls: ['./data-catalog-container.component.scss']
})
export class DataCatalogContainerComponent {
  readonly schema$: Observable<Schema>;
  readonly activeTabIndex$: Observable<number>;
  isLoading = false;
  searchPerformed = false;
  searchResults: Observable<ExpandableSearchResult[]> = of([]);
  lastSearchTerm = '';

  constructor(
    private service: SearchService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private schemaService: TypesService,
    private destroyRef: DestroyRef
  ) {
    activatedRoute.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        if (params.search && this.lastSearchTerm !== params.search) {
          this.search(params.search);
        }
      });
    this.activeTabIndex$ = this.router.events.pipe(
      // NOTE: The Scroll event occurs here when the page first loads, not the NavigationEnd one
      filter((event) => {
        return event instanceof NavigationEnd ||
          event instanceof Scroll && (event.routerEvent instanceof NavigationEnd || event.routerEvent instanceof NavigationSkipped)
      }),
      map((event) => event instanceof Scroll ? event.routerEvent : event),
      map((event: NavigationEnd | NavigationSkipped) => this.getActiveTabIndex(event.url.split('?')[0]))
    );
    this.schema$ = schemaService.getTypes();
  }

  search(searchTerm: string) {
    this.isLoading = true;
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
        map((searchResults: SearchResult[]) =>
          searchResults.reduce((accumulator: ExpandableSearchResult[], searchResult: SearchResult) => {
            const expandableSearch = this.toExpandableSearch(searchResult);
            if (expandableSearch.memberType !== 'ANNOTATION' && expandableSearch.memberType !== 'UNKNOWN') {
              accumulator.push(expandableSearch);
            }
            return accumulator;
          }, [])
        ),
        tap({
          next: _ => {
            this.searchPerformed = true;
            this.isLoading = false;
          },
          error: (error) => {
            console.log('Search failed: ' + JSON.stringify(error));
            this.isLoading = false;
          }
        })
      );
  }

  private toExpandableSearch(searchResult: SearchResult): ExpandableSearchResult {
    return {
      ...searchResult,
      consumersExpanded: false,
      producersExpanded: false
    };
  }

  private getActiveTabIndex(url: string): number {
    switch (url) {
      case '/catalog':
        return 0;
      case '/catalog/diagram':
        return 1;
    }
  }
}
