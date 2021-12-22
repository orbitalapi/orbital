import {Component, OnInit} from '@angular/core';
import {SearchResult, SearchService} from '../../search/search.service';
import {forkJoin, Observable, of, zip} from 'rxjs';
import {OperationQueryResult, TypesService} from '../../services/types.service';
import {filter, flatMap, map, mergeMap} from 'rxjs/operators';
import {SearchResultDocs} from './type-search.component';
import {findType, Schema, Type} from '../../services/schema';
import {buildInheritable} from '../../inheritence-graph/inheritance-graph.component';
import {MatDialogRef} from '@angular/material/dialog';

@Component({
  selector: 'app-type-search-container',
  template: `
    <app-type-search
      (search)="triggerSearch($event)"
      [searchResultDocs]="searchResultDocs"
      (searchResultHighlighted)="loadDocs($event)"
      (searchResultSelected)="onResultSelected($event)"
    ></app-type-search>
  `,
  styleUrls: ['./type-search-container.component.scss']
})
export class TypeSearchContainerComponent {

  searchResults: Observable<SearchResult[]> = of([]);
  searchResultDocs: SearchResultDocs | null = null;
  schema: Schema;

  constructor(private dialogRef: MatDialogRef<TypeSearchContainerComponent>, private service: SearchService, private typeService: TypesService) {
    typeService.getTypes()
      .subscribe(schema => this.schema = schema);
  }


  triggerSearch($event: string) {
    this.searchResults = this.service.search($event);
  }

  loadDocs(searchResult: SearchResult) {
    this.searchResultDocs = null;
    const type$ = this.typeService.getType(searchResult.qualifiedName.parameterizedName);
    const usages$ = this.typeService.getTypeUsages(searchResult.qualifiedName.parameterizedName);
    zip([type$, usages$])
      .pipe(
        map(([type, usages]) => {
          return {
            type,
            inheritanceView: buildInheritable(<Type>type, this.schema),
            typeUsages: <OperationQueryResult>usages
          } as SearchResultDocs
        })
      )
      .subscribe((docs) => {
        this.searchResultDocs = docs;
      })

  }

  onResultSelected($event: SearchResult) {
    const type = findType(this.schema, $event.qualifiedName.parameterizedName);
    this.dialogRef.close(type);
  }
}
