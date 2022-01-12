import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {
  ColumnMapping,
  DbConnectionService,
  JdbcTable, NewOrExistingTypeName,
  TableMetadata, TableModelMapping,
  TableModelSubmissionRequest, TableTaxiGenerationRequest
} from './db-importer.service';
import {flatMap, map, mergeMap} from 'rxjs/operators';
import {Observable, of, Subject} from 'rxjs';
import {SchemaSubmissionResult, TypesService} from '../services/types.service';
import {findType, Schema, Type, VersionedSource} from '../services/schema';
import {isNullOrUndefined} from 'util';
import {HttpErrorResponse} from '@angular/common/http';
import {NewTypeSpec} from '../type-editor/type-editor.component';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-table-importer-container',
  template: `
    <app-table-importer
      (generateSchema)="generateSchema($event)"
      [schema]="schema"
      [newTypes]="newTypes"
      (save)="saveSchema($event)"
      (removeMapping)="removeMapping()"
      [errorMessage]="errorMessage"
      [table]="table"
      [schemaGenerationWorking]="schemaGenerationWorking"
      [saveSchemaWorking]="saveSchemaWorking"
      [tableMetadata$]="tableMetadata$"></app-table-importer>
  `,
  styleUrls: ['./table-importer-container.component.scss']
})
export class TableImporterContainerComponent {
  tableMetadata$ = new Subject<TableMetadata>();
  tableMetadata: TableMetadata;
  schema: Schema;

  connectionName: string;
  table: JdbcTable;

  schemaGenerationWorking = false;
  schemaGenerationResult: SchemaSubmissionResult;
  errorMessage: string | null = null;

  saveSchemaWorking = false;

  tableModel: Type;
  newTypes: Type[];

  constructor(private activeRoute: ActivatedRoute,
              private dbConnectionService: DbConnectionService,
              private typeService: TypesService,
              private snackbar: MatSnackBar,
              private router: Router
  ) {
    activeRoute.params.pipe(
      mergeMap(params => {
        this.connectionName = params.connectionName;
        this.table = {
          tableName: params.tableName,
          schemaName: params.schemaName
        };
        return dbConnectionService.getColumns(
          params.connectionName,
          params.schemaName,
          params.tableName
        );
      })
    ).subscribe(metadata => this.setMetdata(metadata));

    this.typeService.getTypes()
      .subscribe(schema => this.schema = schema);
  }

  private requestGeneratedSchema(newTypeSpec: NewTypeSpec): Observable<SchemaSubmissionResult> {
    const tableTypeName: NewOrExistingTypeName = {
      // Buggy using parameterizedName here .. needs investigation
      typeName: newTypeSpec.qualifiedName().fullyQualifiedName,
      exists: !newTypeSpec.isNewType
    };
    return this.dbConnectionService.generateTaxiForTable(
      this.connectionName,
      [{
        table: this.table,
        typeName: tableTypeName
      }]
    );
  }

  generateSchema(event: NewTypeSpec) {
    this.schemaGenerationWorking = true;
    this.errorMessage = null;
    this.requestGeneratedSchema(event)
      .subscribe(generatedSchema => {
          this.schemaGenerationWorking = false;
          this.handleGeneratedSchemaResult(generatedSchema);
        }, (errorResponse: HttpErrorResponse) => {
          this.errorMessage = errorResponse.error.message;
          this.schemaGenerationWorking = false;
        }
      );
  }

  private handleGeneratedSchemaResult(generatedSchema: SchemaSubmissionResult) {
    this.schemaGenerationResult = generatedSchema;
    this.newTypes = generatedSchema.types;
    const tableModel = generatedSchema.types.find(type => {
      const tableMetadata = type.metadata.find(m => {
        return m.name.fullyQualifiedName === 'io.vyne.jdbc.Table' &&
          m.params['table'] === this.table.tableName && m.params['schema'] === this.table.schemaName;
      });
      return !isNullOrUndefined(tableMetadata);
    });
    this.tableModel = tableModel;
    const metadata = this.tableMetadata;
    // Object.keys(this.tableModel.attributes).forEach(key => {
    //   const field = this.tableModel.attributes[key];
    //   const column = metadata.columns.find(c => c.name === key);
    //   if (column) {
    //     column.taxiType = field.type;
    //   }
    // });
    // this.setMetdata(metadata);
  }

  private setMetdata(metadata: TableMetadata) {
    this.tableMetadata = metadata;
    this.tableMetadata$.next(metadata);
  }

  saveSchema($event: TableModelMapping) {
    const tableMetadata = $event.tableMetadata;
    this.saveSchemaWorking = true;
    const generatedSchemas = (this.schemaGenerationResult) ? of(this.schemaGenerationResult) : this.requestGeneratedSchema($event.typeSpec);
    generatedSchemas
      .pipe(
        map(schema => schema.services.flatMap(service => service.sourceCode)),
        mergeMap((servicesSourceCode: VersionedSource[]) => {
          const request: TableModelSubmissionRequest = {
            model: {
              metadata: [],
              taxi: this.tableModel.sources[0],
              typeName: this.tableModel.name
            },
            columnMappings: tableMetadata.columns,
            serviceMappings: servicesSourceCode
          };
          return this.dbConnectionService.submitModel(
            this.connectionName,
            this.table.schemaName,
            this.table.tableName,
            request
          );
        })).subscribe(
      result => {
        this.saveSchemaWorking = false;
        this.snackbar.open('Table saved successfully', 'Dismiss', {duration: 3000});
        console.log(JSON.stringify(result));
      },
      (error: HttpErrorResponse) => {
        this.saveSchemaWorking = false;
        this.errorMessage = error.error.message;
      }
    );
  }

  removeMapping() {
    this.saveSchemaWorking = true;
    this.dbConnectionService.removeTypeMapping(this.connectionName,
      this.table.schemaName,
      this.table.tableName,
      this.tableMetadata.mappedType)
      .subscribe(result => {
        this.saveSchemaWorking = false;
        this.snackbar.open('Table mapping removed successfully', 'Dismiss', {duration: 3000});
        this.router.navigate(['..', '..'], {relativeTo: this.activeRoute});
      }, (error: HttpErrorResponse) => {
        this.saveSchemaWorking = false;
        this.errorMessage = error.error.message;
      });
  }
}
