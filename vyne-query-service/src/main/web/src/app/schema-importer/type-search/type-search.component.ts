import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {SearchResult} from '../../search/search.service';
import {Schema, Type, VersionedSource} from '../../services/schema';
import {Inheritable} from '../../inheritence-graph/inheritance-graph.component';
import {OperationQueryResult} from '../../services/types.service';

@Component({
  selector: 'app-type-search',
  template: `
    <div class="search-bar">
      <tui-input
        appearance="no-border"
        class="hero-search-bar"
        icon="tuiIconSearchLarge"
        iconAlign="left"
        [tuiTextfieldCleaner]="true"
      >Search for a type
      </tui-input>
    </div>
    <div class="results-panel">
      <div class="results-list">
        <section>
          <div class="section-header">Matches</div>
          <div *ngFor="let searchResult of searchResults">
            <app-type-search-result [result]="searchResult"
                (mouseenter)="onMouseOver($event, searchResult)"
            ></app-type-search-result>
          </div>
        </section>
        <section>
          <div class="section-header">Suggestions</div>
        </section>
      </div>
      <div class="documentation-panel">
        <app-type-viewer
          *ngIf="searchResultDocs"
          [showContentsList]="false"
          [schema]="schema"
          [type]="searchResultDocs.type"
          [inheritanceView]="searchResultDocs.inheritanceView"
          [typeUsages]="searchResultDocs.typeUsages"></app-type-viewer>
      </div>
    </div>

  `,
  styleUrls: ['./type-search.component.scss']
})
export class TypeSearchComponent {

  @Input()
  searchResults: SearchResult[];

  @Input()
  searchResultDocs: SearchResultDocs

  @Input()
  schema: Schema;

  @Output()
  searchResultHighlighted = new EventEmitter<SearchResult>();

  onMouseOver($event: MouseEvent, searchResult: SearchResult) {
    this.searchResultHighlighted.emit(searchResult);
  }
}

export interface SearchResultDocs {
  type: Type;
  typeUsages: OperationQueryResult;
  inheritanceView: Inheritable;
}
