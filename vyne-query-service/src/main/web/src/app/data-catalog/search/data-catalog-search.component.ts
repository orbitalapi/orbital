import {Component, Input, OnInit} from '@angular/core';
import {SearchEntryType, SearchResult} from '../../search/search.service';
import {FormControl} from '@angular/forms';

@Component({
  selector: 'app-data-catalog-search',
  template: `
    <div class="page-content">
      <div class="search-bar-container">
        <h3>Search</h3>
        <div class="input-container">
          <mat-form-field appearance="standard" class="text-input">
            <mat-label>Search for...</mat-label>
            <input matInput placeholder="Search">
          </mat-form-field>
          <mat-form-field appearance="standard"
                          class="category-select"
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
      </div>
      <div class="search-results-table-container">
        <mat-table [dataSource]="searchResults" class="mat-elevation-z1">

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
              <div class="operations-container">
                <app-operation-badge *ngFor="let consumer of element.consumers"
                                     [qualifiedName]="consumer"></app-operation-badge>
              </div>
            </mat-cell>
          </ng-container>

          <!-- Publishers -->
          <ng-container matColumnDef="publishers">
            <mat-header-cell *matHeaderCellDef>Publishers</mat-header-cell>
            <mat-cell *matCellDef="let element">
              <div class="operations-container">
                <app-operation-badge *ngFor="let producer of element.producers"
                                     [qualifiedName]="producer"></app-operation-badge>
              </div>
            </mat-cell>
          </ng-container>

          <mat-header-row *matHeaderRowDef="selectedColumns.value"></mat-header-row>
          <mat-row *matRowDef="let row; columns: selectedColumns.value;"></mat-row>
        </mat-table>
        <mat-select multiple [formControl]="selectedColumns">
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
export class DataCatalogSearchComponent {
  fixedColumns: string[] = ['result'];
  columns = [{label: 'Search result', value: 'result'},
    {label: 'Consumers', value: 'consumers'},
    {label: 'Publishers', value: 'publishers'}];

  selectedColumns = new FormControl(['result']);

  @Input()
  searchResults: SearchResult[] = [];

  searchCategories: { label: string, value: SearchEntryType }[] = [
    {label: 'Dataset', value: 'TYPE'},
    {label: 'Attribute', value: 'ATTRIBUTE'},
    {label: 'Operation', value: 'OPERATION'},
    {label: 'Data source', value: 'SERVICE'},
  ];

  selectedCategories: FormControl = new FormControl(this.searchCategories);


}
