import {Component, OnInit} from '@angular/core';
import {SearchResult, SearchService} from '../search.service';
import {Observable, of} from 'rxjs';
import {Router} from '@angular/router';
import {QualifiedName, SchemaMember} from '../../services/schema';


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

  navigateToMember(memberName: QualifiedName) {
    this.router.navigate(['/types', memberName.fullyQualifiedName]);
  }
}
