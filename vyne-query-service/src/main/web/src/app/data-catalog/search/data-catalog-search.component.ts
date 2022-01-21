import {Component, Input, OnInit, Output, EventEmitter} from '@angular/core';
import {ExpandableSearchResult, SearchEntryType, SearchResult} from '../../search/search.service';
import {FormControl} from '@angular/forms';
import {pipe, Subject} from 'rxjs';
import {distinctUntilChanged, debounceTime, filter} from 'rxjs/operators';
import {Router} from '@angular/router';
import {navigateForSearchResult} from '../../search/search-bar/search-bar.container.component';
import {Observable} from 'rxjs/internal/Observable';

@Component({
  selector: 'app-data-catalog-search',
  template: `
    <div class="page-content" xmlns="http://www.w3.org/1999/html">
      <div class="search-bar-container">
        <h2>Data Catalog</h2>
        <p class="help-text" *ngIf="!atLeastOneSearchCompleted">
          The data catalog contains all models, attributes, services and operations published to Vyne. You can search by
          name, or search for tags using # (eg: #MyTag)
        </p>
        <div class="input-container">
          <mat-form-field appearance="standard" class="text-input">
            <mat-label>Search for...</mat-label>
            <input matInput (input)="onSearchValueUpdated($event)" [value]="searchTerm" placeholder="Search"
                   name="search-input" id="search-input" type="text">
          </mat-form-field>
          <mat-form-field appearance="standard"
                          class="category-select"
                          *ngIf="showCategories"
          >
            <mat-label>Categories</mat-label>
            <mat-select multiple [formControl]="selectedCategories">
              <mat-select-trigger>
                <span *ngIf="selectedCategories.value?.length === searchCategories.length">Everything</span>
                <span *ngIf="selectedCategories.value?.length !== searchCategories.length">
                   {{selectedCategories.value ? selectedCategories.value[0].label : ''}}
                </span>
                <span
                  *ngIf="selectedCategories.value?.length > 1 && selectedCategories.value?.length < searchCategories.length"
                  class="select-label-hint">
        (+{{selectedCategories.value?.length - 1}} {{selectedCategories.value?.length === 2 ? 'other' : 'others'}})
      </span>
              </mat-select-trigger>
              <mat-option *ngFor="let category of searchCategories"
                          [value]="category">{{category.label}}</mat-option>
            </mat-select>
          </mat-form-field>

        </div>
        <a class="subtle" [routerLink]="['browse']">I'd rather browse</a>
      </div>

      <div class="search-results-table-container">
        <mat-table [dataSource]="searchResults" [trackBy]="searchResultTrackBy" class="mat-elevation-z1"
                   [class.hidden]="!atLeastOneSearchCompleted">

          <!-- Search result body -->
          <ng-container matColumnDef="result">
            <mat-header-cell *matHeaderCellDef>Results</mat-header-cell>
            <mat-cell *matCellDef="let element">
              <app-data-catalog-search-result-card [searchResult]="element"></app-data-catalog-search-result-card>
            </mat-cell>
          </ng-container>

          <!-- Consumers -->
          <ng-container matColumnDef="consumers">
            <mat-header-cell *matHeaderCellDef>Consumers</mat-header-cell>
            <mat-cell *matCellDef="let element">
              <div class="row">
                <app-operation-badge *ngFor="let consumer of consumerElements(element)"
                                     [qualifiedName]="consumer"></app-operation-badge>
                <a (click)="toggleConsumerExpand($event, element)" *ngIf="element.consumers.length > 3" class="subtle expand-collapse-link">

                </a>
              </div>
            </mat-cell>
          </ng-container>

          <!-- Publishers -->
          <ng-container matColumnDef="publishers">
            <mat-header-cell *matHeaderCellDef>Publishers</mat-header-cell>
            <mat-cell *matCellDef="let element">
              <div class="row">
                <app-operation-badge *ngFor="let producer of producerElements(element)"
                                     [qualifiedName]="producer">
                </app-operation-badge>
                <a (click)="toggleProducerExpand($event, element)" *ngIf="element.producers.length > 3" class="subtle expand-collapse-link">
                  {{element.producersExpanded ? 'Show less' : 'Plus ' + (element.producers.length - 3) + ' more'}}
                </a>
              </div>
            </mat-cell>
          </ng-container>

          <mat-header-row *matHeaderRowDef="selectedColumns.value"></mat-header-row>
          <mat-row *matRowDef="let row; columns: selectedColumns.value;"
                   (click)="navigateToElement(row)"></mat-row>
        </mat-table>
        <mat-select multiple [formControl]="selectedColumns" [class.hidden]="!atLeastOneSearchCompleted">
          <mat-select-trigger>
          </mat-select-trigger>
          <mat-option *ngFor="let optionalColumn of columns"
                      [value]="optionalColumn.value">{{optionalColumn.label}}</mat-option>
        </mat-select>
      </div>
    </div>
  `,
  styleUrls: ['./data-catalog-search.component.scss']
})
export class DataCatalogSearchComponent implements OnInit {
  showCategories = false;
  searchInput: FormControl;
  fixedColumns: string[] = ['result'];
  columns = [{label: 'Search result', value: 'result'},
    {label: 'Consumers', value: 'consumers'},
    {label: 'Publishers', value: 'publishers'}];

  // Intentionally removing the consumers and publishers columns.  These should be 'opt-in' by the user,
  // since it's only relevant in some situations.
  selectedColumns = new FormControl(['result', /*'consumers', 'publishers' */]);

  @Input()
  searchResults: Observable<ExpandableSearchResult[]>;

  @Input()
  atLeastOneSearchCompleted = false;

  private _initialSearchTerm: string;
  @Input()
  get initialSearchTerm(): string {
    return this._initialSearchTerm;
  }

  set initialSearchTerm(value: string) {
    this._initialSearchTerm = value;
    // We want to update the search box when first landing
    // here, so that the queryParam (ie., from navigating back)
    // updates correctly.  After handling the first update though,
    // we don't want to be continually updating, as the debounce
    // makes these events out-of-date.
    if (!this.searchTerm) {
      this.searchTerm = value;
    }
  }

  // The searchTerm bound in the input box.
  searchTerm: string;


  @Output()
  searchValueUpdated: EventEmitter<string> = new EventEmitter();
  debouncer: Subject<string> = new Subject<string>();

  constructor(private router: Router) {
    this.debouncer
      .pipe(
        filter(term => term.length >= 2),
        debounceTime(300),
        distinctUntilChanged())
      .subscribe(value => this.searchValueUpdated.emit(value));
  }

  searchCategories: { label: string, value: SearchEntryType }[] = [
    {label: 'Dataset', value: 'TYPE'},
    {label: 'Attribute', value: 'ATTRIBUTE'},
    {label: 'Operation', value: 'OPERATION'},
    {label: 'Data source', value: 'SERVICE'},
  ];

  selectedCategories: FormControl = new FormControl(this.searchCategories);

  onSearchValueUpdated($event) {
    this.searchTerm = $event.target.value;
    const value = $event.target.value;
    this.debouncer.next(value);
  }

  searchResultTrackBy(index, searchResult: SearchResult) {
    return searchResult.qualifiedName.fullyQualifiedName;
  }


  navigateToElement(row: SearchResult) {
    navigateForSearchResult(this.router, row);
  }

  producerElements(result: ExpandableSearchResult) {
    if (result.producersExpanded) {
      return result.producers;
    } else {
      return result.producers.slice(0, 3);
    }
  }

  consumerElements(result: ExpandableSearchResult) {
    if (result.consumersExpanded) {
      return result.consumers;
    } else {
      return result.consumers.slice(0, 3);
    }
  }

  toggleProducerExpand($event: MouseEvent, element: ExpandableSearchResult) {
    $event.stopPropagation();
    element.producersExpanded = !element.producersExpanded;
  }

  toggleConsumerExpand($event: MouseEvent, element: ExpandableSearchResult) {
    $event.stopPropagation();
    element.consumersExpanded = !element.consumersExpanded;
  }

  ngOnInit(): void {
  }
}
