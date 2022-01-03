import {Component} from '@angular/core';
import {SearchResult, SearchService} from '../../search/search.service';
import {CompletionObserver, Observable, zip} from 'rxjs';
import {TypesService} from '../../services/types.service';
import {map, tap} from 'rxjs/operators';
import {SearchResultDocs} from './type-search.component';
import {findType, Schema} from '../../services/schema';
import {buildInheritable} from '../../inheritence-graph/inheritance-graph.component';
import {MatDialogRef} from '@angular/material/dialog';

@Component({
  selector: 'app-type-search-container',
  template: `
    <app-type-search
      (search)="triggerSearch($event)"
      [searchResults]="searchResults"
      [working]="loading"
      [schema]="schema"
      [searchResultDocs]="searchResultDocs"
      (searchResultHighlighted)="loadDocs($event)"
      (searchResultSelected)="onResultSelected($event)"
    ></app-type-search>
  `,
  styleUrls: ['./type-search-container.component.scss']
})
export class TypeSearchContainerComponent {

  searchResults: SearchResult[] | null = null;
  searchResultDocs: SearchResultDocs | null = null;
  schema: Schema;
  loading: boolean = false;

  constructor(private dialogRef: MatDialogRef<TypeSearchContainerComponent>, private service: SearchService, private typeService: TypesService) {
    typeService.getTypes()
      .subscribe(schema => this.schema = schema);
  }


  triggerSearch($event: string) {
    if ($event.length < 3) {
      return;
    }
    this.loading = true;
    this.service.search($event)
      .pipe(
        map(searchResults => searchResults.filter(entry => entry.memberType === 'TYPE')),
      )
      .subscribe(results => {
        this.searchResults = results;
        this.loading = false;
      })
  }

  loadDocs(searchResult: SearchResult) {
    this.searchResultDocs = null;
    const type$ = this.typeService.getType(searchResult.qualifiedName.parameterizedName);
    const usages$ = this.typeService.getTypeUsages(searchResult.qualifiedName.parameterizedName);
    zip(type$, usages$)
      .pipe(
        map(([type, usages]) => {
          return {
            type,
            inheritanceView: buildInheritable(type, this.schema),
            typeUsages: usages
          } as SearchResultDocs
        })
      )
      .subscribe((docs) => {
        this.searchResultDocs = docs;
      }, error => {
        console.log(JSON.stringify(error));
      })


  }

  onResultSelected($event: SearchResult) {
    const type = findType(this.schema, $event.qualifiedName.parameterizedName);
    this.dialogRef.close(type);
  }
}
