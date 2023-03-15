import { Component } from '@angular/core';
import { SearchResult, SearchService } from '../search.service';
import { Observable, of } from 'rxjs';
import { Router } from '@angular/router';


@Component({
  selector: 'app-search-bar-container',
  template: `
    <app-search-bar (search)="triggerSearch($event)"
                    (select)="navigateToMember($event)"
                    [searchResults$]="searchResults"></app-search-bar>
  `
})
export class SearchBarContainerComponent {

  constructor(private service: SearchService, private router: Router) {
  }

  searchResults: Observable<SearchResult[]> = of([]);


  triggerSearch($event: string) {
    this.searchResults = this.service.search($event);
  }

  navigateToMember(searchResult: SearchResult) {
    navigateForSearchResult(this.router, searchResult);
  }
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
