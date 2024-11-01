import {
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  HostListener,
  Input, OnInit,
  Output,
  QueryList,
  ViewChildren
} from '@angular/core';
import { SearchResult } from '../../search/search.service';
import { Schema, Type } from '../../services/schema';
import { OperationQueryResult } from '../../services/types.service';
import { ActiveDescendantKeyManager, ListKeyManager } from '@angular/cdk/a11y';
import { TypeSearchResultComponent } from './type-search-result.component';
import { DOWN_ARROW, ENTER, UP_ARROW } from '@angular/cdk/keycodes';
import { Inheritable } from 'src/app/inheritence-graph/build.inheritable';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-type-search',
  template: `
    <div class="search-bar">
      <tui-input
        appearance="no-border"
        class="hero-search-bar"
        icon="@tui.search"
        iconAlign="left"
        [(ngModel)]="searchTerm"
        (ngModelChange)="searchTermChanged.next($event)"
        [tuiTextfieldCleaner]="true"
        [ngClass]="{ 'working' : loading }"
      >Search for a type
      </tui-input>
      <progress
        *ngIf='loading'
        max="100"
        tuiProgressBar
        size='xs'
        new
        class="text-search-loader"
      ></progress>
    </div>
    <div class="results-panel" *ngIf="searchResults">
      <div class="results-list">
        <section>
          <ng-container *ngIf="searchResults.length > 0">
            <app-type-search-result
              *ngFor="let searchResult of searchResults; let idx = index"
              [result]="searchResult"
              (click)="selectResult(searchResult)"
              (mouseenter)="onMouseOver(searchResult, idx)"
            ></app-type-search-result>
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
        <progress
          *ngIf='loadingDocs'
          max="100"
          tuiProgressBar
          size='xs'
          new
          class="docs-loader"
        ></progress>
        <app-type-viewer
          *ngIf="searchResultDocs"
          [showContentsList]="false"
          [schema]="schema"
          [type]="searchResultDocs.type"
          [inheritanceView]="searchResultDocs.inheritanceView"
          [typeUsages]="searchResultDocs.typeUsages"
          [schemaMemberNavigable]="false"
        ></app-type-viewer>
      </div>
    </div>

  `,
  styleUrls: ['./type-search.component.scss']
})
export class TypeSearchComponent implements OnInit {
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
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          debounceTime(250)
        )
        .subscribe(activeIndex => {
          this.searchResultHighlighted.emit(this.keyboardEventsManager.activeItem.result);
        });
    }
  }

  private _items: QueryList<TypeSearchResultComponent>;

  private keyboardEventsManager: ListKeyManager<TypeSearchResultComponent>;

  searchTerm: string;
  searchTermChanged: Subject<string> = new Subject<string>();

  @Input()
  loading: boolean = false;

  @Input()
  loadingDocs: boolean = false;

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

  constructor(private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.searchTermChanged
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        debounceTime(350),
        distinctUntilChanged(),
      )
      .subscribe(value => this.search.emit(value));
  }

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
