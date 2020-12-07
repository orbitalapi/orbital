import {Component, OnInit} from '@angular/core';
import {SearchResult, SearchService} from '../search.service';
import {Observable, of} from 'rxjs';
import {Router} from '@angular/router';
import {QualifiedName, Schema, SchemaMember} from '../../services/schema';
import {TypesService} from "../../services/types.service";


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
  schema: Schema;


  triggerSearch($event: string) {
    this.searchResults = this.service.search($event);
  }

  navigateToMember(memberName: QualifiedName) {
    this.router.navigate(['/types', memberName.fullyQualifiedName]);
  }
}
