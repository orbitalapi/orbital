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

  constructor(private typesService: TypesService, private service: SearchService, private router: Router) {
    this.typesService.getTypes()
      .subscribe(next => this.schema = next);
  }

  searchResults: Observable<SearchResult[]> = of([]);
  schema: Schema;


  triggerSearch($event: string) {
    this.searchResults = this.service.search($event);
  }

  navigateToMember(memberName: QualifiedName) {
    // In case of a change in types, type we were receiving from service was undefined for the changed type.
    // By refreshing types when encountering 'undefined', we resolve this problem.
    if (!this.schema.types.find(type => type.name === memberName)) {
      this.typesService.getTypes(true);
    }
    this.router.navigate(['/types', memberName.fullyQualifiedName]);

  }
}
