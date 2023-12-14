import {ChangeDetectorRef, Component, EventEmitter, Output} from '@angular/core';
import {SearchResult, SearchService} from "../search/search.service";
import {TypesService} from "../services/types.service";
import {QualifiedName} from "../services/schema";

@Component({
  selector: 'app-catalog-explorer-panel',
  template: `
      <div class="search-container">
          <tui-input
            icon="tuiIconSearch"
            [tuiTextfieldCleaner]="true"
            [tuiTextfieldLabelOutside]="true"
            [ngModel]="searchValue"
            (ngModelChange)="onSearchChanged($event)"
            tuiTextfieldSize="m">
            Search
            <input tuiTextfield/>

          </tui-input>
      </div>
      <mat-progress-bar mode="query" *ngIf="searchLoading"></mat-progress-bar>
      <app-catalog-tree *ngIf="!searchResults" (addToQueryClicked)="addToQueryClicked.emit($event)"
      ></app-catalog-tree>
      <app-catalog-panel-search-results *ngIf="searchResults"
                                        (addToQueryClicked)="addToQueryClicked.emit($event)"
                                        [searchResults]="searchResults"></app-catalog-panel-search-results>
  `,
  styleUrls: ['./catalog-explorer-panel.component.scss']
})
export class CatalogExplorerPanelComponent {

  constructor(private searchService: SearchService,
              private typeService: TypesService,
              private changeDetector: ChangeDetectorRef) {
  }

  @Output()
  addToQueryClicked = new EventEmitter<QualifiedName>();

  searchLoading: boolean;
  searchResults: SearchResult[];

  searchValue: string = '';


  onSearchChanged($event: any) {
    this.searchValue = $event;
    if (this.searchValue === '') {
      this.searchResults = null;
      this.changeDetector.markForCheck();
    }
    if (this.searchValue.length > 2) {
      this.searchLoading = true;
      this.searchService.search(this.searchValue)
        .subscribe(results => {
            this.searchResults = results;
            this.searchLoading = false;
            this.changeDetector.markForCheck();
          },
          error => {
            console.error(error);
            this.searchLoading = false;
            this.changeDetector.markForCheck();
          }
        )

    }
  }
}
