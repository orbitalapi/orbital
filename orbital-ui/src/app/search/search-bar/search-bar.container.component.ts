import {Component} from '@angular/core';
import {SearchResult, SearchService} from '../search.service';
import {Observable, of, Subject} from 'rxjs';
import {Router} from '@angular/router';
import {filter, startWith, switchMap} from "rxjs/operators";
import {isNullOrUndefined} from "util";


@Component({
  selector: 'app-search-bar-container',
  styleUrls: ['./search-bar.component.scss'],
  template: `

      <tui-combo-box *tuiLet="searchResults$ | async as items" tuiTextfieldSize="m" [ngModel]="selectedSearchItem"
                     (ngModelChange)="valueChange($event)"
                     (searchChange)="searchInputValueChanged($event)" [tuiTextfieldCleaner]="true"
                     [tuiTextfieldLabelOutside]="true"
                     [stringify]="emptyStringify"
                     tuiTextfieldIconLeft="tuiIconSearch"
      >
          Search
          <input placeholder="Search" tuiTextfield>
          <tui-data-list-wrapper
                  *tuiDataList
                  [itemContent]="content"
                  [items]="items"
          ></tui-data-list-wrapper>
      </tui-combo-box>
      <ng-template
              #content
              let-data
      >
          <app-search-result [result]="data" (click)="navigateToMember(data)"></app-search-result>
      </ng-template>
  `
})
export class SearchBarContainerComponent {

  /**
   * Bit of a hack, but once a search is completed (ie., the combobox has emitted
   * the value the user clicks), we don't want to show anything, just an empty search box
   * ready for the next search. So, always return empty string
   */
  readonly emptyStringify = () => "";

  get selectedSearchItem(): SearchResult | null {
    return null;
  }

  constructor(private service: SearchService, private router: Router) {
  }

  valueChange(searchResult: SearchResult | null) {
    if (!isNullOrUndefined(searchResult)) {
      navigateForSearchResult(this.router, searchResult);
    }
  }

  readonly search$ = new Subject<string>()
  readonly searchResults$: Observable<SearchResult[]> =
    this.search$.pipe(
      switchMap(search => {
        if (search == null || search.length <= 1) {
          return of([])
        } else {
          return this.service.search(search)
            .pipe(startWith(null))
        }
      }),
      startWith([])
    )


  searchInputValueChanged(newValue: string | null) {
    this.search$.next(newValue);
  }

  navigateToMember(searchResult: SearchResult | null) {

  }

  protected readonly navigateForSearchResult = navigateForSearchResult;
}

export function navigateForSearchResult(router: Router, searchResult: SearchResult) {
  const qualifiedName = searchResult.qualifiedName.fullyQualifiedName;
  switch (searchResult.memberType) {
    case 'SERVICE':
      router.navigate(['/services', qualifiedName]);
      break;
    case 'OPERATION':
      const parts = qualifiedName.split('@@');
      const serviceName = parts[0];
      const operationName = parts[1];
      router.navigate(['/services', serviceName, operationName]);
      break;
    default:
    case 'TYPE':
      router.navigate(['/catalog', qualifiedName]);
      break;
  }
}
