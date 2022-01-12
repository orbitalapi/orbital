import {Component} from '@angular/core';
import {SearchResult, SearchService} from '../../search/search.service';
import {CompletionObserver, Observable, zip} from 'rxjs';
import {TypesService} from '../../services/types.service';
import {map, tap} from 'rxjs/operators';
import {SearchResultDocs} from './type-search.component';
import {findType, Schema, Type} from '../../services/schema';
import {buildInheritable} from '../../inheritence-graph/inheritance-graph.component';
import {MatDialogRef} from '@angular/material/dialog';
import {NewTypeSpec, qualifiedName} from '../../type-editor/type-editor.component';

@Component({
  selector: 'app-type-search-container',
  template: `
    <tui-tabs [(activeItemIndex)]="selectedTab">
      <button tuiTab>
        <img src="assets/img/tabler/search.svg" class="icon">
        Search
      </button>
      <button tuiTab>
        <img src="assets/img/tabler/plus.svg" class="icon">
        Create new
      </button>
    </tui-tabs>
    <app-type-search
      *ngIf="selectedTab === 0"
      (search)="triggerSearch($event)"
      [searchResults]="searchResults"
      [working]="loading"
      [schema]="schema"
      [searchResultDocs]="searchResultDocs"
      (searchResultHighlighted)="loadDocs($event)"
      (searchResultSelected)="onResultSelected($event)"
    ></app-type-search>
    <app-type-editor
      *ngIf="selectedTab === 1"
      [schema]="schema"
      (cancel)="close()"
      (create)="createNewType($event)"
    >

    </app-type-editor>
  `,
  styleUrls: ['./type-search-container.component.scss']
})
export class TypeSearchContainerComponent {

  searchResults: SearchResult[] | null = null;
  searchResultDocs: SearchResultDocs | null = null;
  schema: Schema;
  loading: boolean = false;

  selectedTab: number = 0;

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

  close() {
    this.dialogRef.close();
  }

  onResultSelected($event: SearchResult) {
    const type = findType(this.schema, $event.qualifiedName.parameterizedName);
    this.dialogRef.close({
      type,
      source: 'schema'
    } as TypeSelectedEvent);
  }

  createNewType($event: NewTypeSpec) {
    const type: Type = {
      name: qualifiedName($event),
      fullyQualifiedName: qualifiedName($event).fullyQualifiedName,
      attributes: {},
      collectionType: null,
      modifiers: [],
      metadata: [],
      isScalar: true,
      format: null,
      hasFormat: false,
      aliasForType: null,
      basePrimitiveTypeName: $event.inheritsFrom,
      enumValues: [],
      isClosed: false,
      isCollection: false,
      isParameterType: false,
      typeParameters: [],
      inheritsFrom: ($event.inheritsFrom) ? [$event.inheritsFrom] : [],
      typeDoc: $event.typeDoc,
      sources: [],
      declaresFormat: false
    }
    this.dialogRef.close({
      type,
      source: 'new'
    } as TypeSelectedEvent);
  }
}

export interface TypeSelectedEvent {
  type: Type
  // TODO : add something for passing in newly created types that have been created during the import process, but dom't exist in the schema yet
  source: 'schema' | 'new';
}
