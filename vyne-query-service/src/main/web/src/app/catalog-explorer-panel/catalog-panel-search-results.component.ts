import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {SearchResult} from "../search/search.service";

@Component({
  selector: 'app-catalog-panel-search-results',
  template: `
      <div *ngFor="let searchResult of searchResults" class="search-result">
          <app-catalog-entry-line
                  [qualifiedName]="searchResult.qualifiedName"
                  [primitiveType]="searchResult.primitiveType"
                  [fieldName]="searchResult.matchedFieldName"
                  [serviceOrTypeKind]="searchResult.typeKind || searchResult.serviceKind"
          ></app-catalog-entry-line>
          <div class="row">
              <ng-container *ngIf="searchResult.typeDoc">
                  <span class="docs">{{searchResult.typeDoc}}</span>
              </ng-container>
              <ng-container *ngIf="!searchResult.typeDoc">
                  <span class="no-docs">No documentation present</span>
              </ng-container>

            <tui-svg src="tuiIconPlus"></tui-svg>
          </div>
      </div>
  `,
  styleUrls: ['./catalog-panel-search-results.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CatalogPanelSearchResults {

  @Input()
  searchResults: SearchResult[];


}
