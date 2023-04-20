import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {SearchResult} from "../search/search.service";
import {QualifiedName} from "../services/schema";

@Component({
  selector: 'app-catalog-panel-search-results',
  template: `
    <div *ngFor="let searchResult of searchResults" class="search-result"
         (click)="itemClicked.emit(searchResult.qualifiedName)">
      <app-catalog-entry-line
        [qualifiedName]="searchResult.qualifiedName"
        [primitiveType]="searchResult.primitiveType"
        [fieldName]="searchResult.matchedFieldName"
        [allowAddToQuery]="false"
        [serviceOrTypeKind]="searchResult.typeKind || searchResult.serviceKind"
      ></app-catalog-entry-line>
      <div class="row">
        <ng-container *ngIf="searchResult.typeDoc">
          <span class="docs flex-grow">{{searchResult.typeDoc}}</span>
        </ng-container>
        <ng-container *ngIf="!searchResult.typeDoc">
          <span class="no-docs flex-grow">No documentation present</span>
        </ng-container>
        <button *ngIf="showAddToQueryButton(searchResult)"
                class="add-to-query-button"
                tuiIconButton
                type="button"
                size="xs"
                appearance="icon"
                icon="tuiIconPlusCircle"
                (click)="addToQueryClicked.emit(searchResult.qualifiedName)"
                [tuiHint]="'Add to query'"
        ></button>
      </div>
    </div>
  `,
  styleUrls: ['./catalog-panel-search-results.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CatalogPanelSearchResults {

  @Output()
  itemClicked = new EventEmitter<QualifiedName>();

  @Output()
  addToQueryClicked = new EventEmitter<QualifiedName>();

  @Input()
  searchResults: SearchResult[];

  showAddToQueryButton(result: SearchResult) {
    return result.typeKind || result.memberType === "OPERATION";
  }

  protected readonly alert = alert;
}
