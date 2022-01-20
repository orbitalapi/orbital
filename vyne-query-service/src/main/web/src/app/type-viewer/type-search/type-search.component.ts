import {Component, EventEmitter, forwardRef, HostListener, Input, Output, QueryList, ViewChildren} from '@angular/core';
import {SearchResult} from '../../search/search.service';
import {Schema, Type} from '../../services/schema';
import {Inheritable} from '../../inheritence-graph/inheritance-graph.component';
import {OperationQueryResult} from '../../services/types.service';
import {Observable} from 'rxjs/internal/Observable';
import {ActiveDescendantKeyManager, ListKeyManager} from '@angular/cdk/a11y';
import {TypeSearchResultComponent} from './type-search-result.component';
import {DOWN_ARROW, ENTER, UP_ARROW} from '@angular/cdk/keycodes';

@Component({
  selector: 'app-type-search',
  template: `
    <div class="search-bar">
      <tui-input
        appearance="no-border"
        class="hero-search-bar"
        icon="tuiIconSearchLarge"
        iconAlign="left"
        [(ngModel)]="searchTerm"
        (ngModelChange)="search.emit($event)"
        [tuiTextfieldCleaner]="true"
        [ngClass]="{ 'working' : working }"
      >Search for a type
      </tui-input>
      <mat-progress-spinner mode="indeterminate" *ngIf="working" [diameter]="24" [strokeWidth]="2"></mat-progress-spinner>
    </div>
    <div class="results-panel" *ngIf="searchResults">
      <div class="results-list">
        <section>
          <div class="section-header">Matches</div>
          <ng-container *ngIf="searchResults.length > 0">
            <div *ngFor="let searchResult of searchResults; let idx = index">
              <app-type-search-result [result]="searchResult"
                                      (click)="selectResult(searchResult)"
                                      (mouseenter)="onMouseOver(searchResult, idx)"
              ></app-type-search-result>
            </div>
          </ng-container>
          <ng-container *ngIf="searchResults.length === 0">
            <div class="no-results">No results</div>
          </ng-container>
        </section>
        <!--        <section>-->
        <!--          <div class="section-header">Suggestions</div>-->
        <!--        </section>-->
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
  @ViewChildren(forwardRef(() => TypeSearchResultComponent))
  get items(): QueryList<TypeSearchResultComponent> {
    return this._items;
  }

  set items(value: QueryList<TypeSearchResultComponent>) {
    this._items = value;
    if (this.items) {
      this.keyboardEventsManager = new ActiveDescendantKeyManager(this.items)
        .withWrap();
      this.keyboardEventsManager.change
        .subscribe(activeIndex => {
          this.searchResultHighlighted.emit(this.keyboardEventsManager.activeItem.result);
        });
    }
  }

  private _items: QueryList<TypeSearchResultComponent>;

  private keyboardEventsManager: ListKeyManager<TypeSearchResultComponent>;

  searchTerm: string;

  @Input()
  working: boolean = false;

  @Input()
  searchResults: SearchResult[] | null = null;

  @Input()
  searchResultDocs: SearchResultDocs

  @Input()
  schema: Schema;

  @Output()
  search = new EventEmitter<string>();

  @Output()
  searchResultHighlighted = new EventEmitter<SearchResult>();

  @Output()
  searchResultSelected = new EventEmitter<SearchResult>();

  onMouseOver(searchResult: SearchResult, index: number) {
    if (this.keyboardEventsManager) {
      this.keyboardEventsManager.setActiveItem(index)
    }
  }

  @HostListener('keydown', ['$event'])
  onKeydown(event: KeyboardEvent) {
    if (this.keyboardEventsManager) {
      if (event.keyCode === DOWN_ARROW || event.keyCode === UP_ARROW) {
        // passing the event to key manager so we get a change fired
        this.keyboardEventsManager.onKeydown(event);
        event.stopImmediatePropagation();
      } else if (event.keyCode === ENTER) {
        // when we hit enter, the keyboardManager should call the selectItem method of the `ListItemComponent`
        // this.keyboardEventsManager.activeItem.selectItem();
        this.selectResult(this.keyboardEventsManager.activeItem.result)
      }
    }
  }

  selectResult(result: SearchResult) {
    this.searchResultSelected.emit(result);
  }
}

export interface SearchResultDocs {
  type: Type;
  typeUsages: OperationQueryResult;
  inheritanceView: Inheritable;
}
