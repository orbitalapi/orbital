import { Component, Inject } from '@angular/core';
import { PartialSearchResult, SearchResult, SearchService } from '../../search/search.service';
import { Observable, zip } from 'rxjs';
import { map } from 'rxjs/operators';
import { MAT_LEGACY_DIALOG_DATA, MatLegacyDialogRef as MatDialogRef } from '@angular/material/legacy-dialog';
import { TypesService } from '../../services/types.service';
import { SearchResultDocs } from './type-search.component';
import { findType, Schema, Type } from '../../services/schema';
import { buildInheritable } from 'src/app/inheritence-graph/build.inheritable';
import { NewTypeSpec, qualifiedName } from 'src/app/type-editor/new-type-spec';
import { TypeSelectedEvent } from 'src/app/type-viewer/type-search/type-selected-event';

@Component({
  selector: 'app-type-search-container',
  template: `
    <!--// Just commenting the new type functionality out for now -->
    <!--<tui-tabs [(activeItemIndex)]="selectedTab">
      <button tuiTab>
        <img src="assets/img/tabler/search.svg" class="icon">
        Search
      </button>
      <button tuiTab>
        <img src="assets/img/tabler/plus.svg" class="icon">
        Create new
      </button>
    </tui-tabs>-->
    <h3>Select a replacement type</h3>
    <app-type-search
      (search)="triggerSearch($event)"
      [searchResults]="searchResults"
      [loading]="loading"
      [schema$]="schema$"
      [schema]="schema"
      [searchResultDocs]="searchResultDocs"
      (searchResultHighlighted)="loadDocs($event)"
      (searchResultSelected)="onResultSelected($event)"
    ></app-type-search>
    <!--<app-type-editor
      *ngIf="selectedTab === 1"
      [schema]="schema"
      (cancel)="close()"
      (create)="createNewType($event)"
    >
    </app-type-editor>-->
  `,
  styleUrls: ['./type-search-container.component.scss']
})
export class TypeSearchContainerComponent {
  searchResults: SearchResult[] | null = null;
  searchResultDocs: SearchResultDocs | null = null;
  schema$: Observable<Schema>;
  schema: Schema;
  loading: boolean = false;
  //selectedTab: number = 0;

  constructor(
    private dialogRef: MatDialogRef<TypeSearchContainerComponent>,
    private service: SearchService,
    private typeService: TypesService,
    @Inject(MAT_LEGACY_DIALOG_DATA) public data: {partialSchema: Schema, parentModel: Type}
  ) {
    this.schema$ = typeService.getTypes().pipe(map(schema => this.schema = schema));
  }

  triggerSearch($event: string) {
    this.loading = true;
    this.service.search($event)
      .pipe(
        map(searchResults => searchResults.filter(entry => entry.memberType === 'TYPE')),
      )
      .subscribe(results => {
        if (this.data?.partialSchema) {
          // we have a partialSchema to query on
          this.searchResults = results.concat(this.filterPartialSchema($event.toLowerCase()))
        } else {
          this.searchResults = results;
        }
        this.loading = false;
      })
  }

  loadDocs(searchResult: SearchResult | PartialSearchResult) {
    if ("isLocal" in searchResult) {
      // we don't do anything for now for schema's that haven't been committed
      this.searchResultDocs = null;
      return;
    }
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

  onResultSelected($event: SearchResult | PartialSearchResult) {
    let type;
    if ("isLocal" in $event) {
      type = this.data.partialSchema.types.find(type => type.memberQualifiedName.fullyQualifiedName === $event.qualifiedName.fullyQualifiedName)
    } else {
      type = findType(this.schema, $event.qualifiedName.parameterizedName);
    }
    this.dialogRef.close({
      type,
      source: 'schema'
    } as TypeSelectedEvent);
  }

  // Need a better solution than doing this on the client
  //  - Perhaps the new HTTP Query could be useful here, parsing the partialSchema
  //    up to the server with the search call and allowing the server to calculate
  //    the correct results with all the information available
  filterPartialSchema(term: string): PartialSearchResult[] {
    return this.data.partialSchema.types
      .filter(type => type.name.shortDisplayName.toLowerCase().includes(term) || type.typeDoc.toLowerCase().includes(term))
      .filter(type => {
        return type.memberQualifiedName.fullyQualifiedName !== this.data.parentModel?.memberQualifiedName.fullyQualifiedName
      })
      .map(filteredType => ({
        qualifiedName: filteredType.memberQualifiedName,
        typeDoc: filteredType.typeDoc,
        matches: [], // TODO
        memberType: 'TYPE',
        consumers: [], // TODO
        producers: [], // TODO
        metadata: filteredType.metadata,
        //matchedFieldName?: string,
        typeKind: filteredType.isScalar ? 'Model' : 'TYPE',
        //serviceKind?: ServiceKind,
        //operationKind?: OperationKind,
        primitiveType: filteredType.basePrimitiveTypeName,
        isLocal: true
      } as PartialSearchResult))
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

