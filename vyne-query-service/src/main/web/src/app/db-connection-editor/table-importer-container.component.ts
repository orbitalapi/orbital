import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {
  ColumnMapping,
  DbConnectionService,
  JdbcTable,
  TableMetadata,
  TableModelSubmissionRequest
} from './db-importer.service';
import {mergeMap} from 'rxjs/operators';
import {Observable, Subject} from 'rxjs';
import {TaxiSubmissionResult, TypesService} from '../services/types.service';
import {findType, Schema, Type, VersionedSource} from '../services/schema';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-table-importer-container',
  template: `
    <app-table-importer
      (generateSchema)="generateSchema()"
      [schema]="schema"
      [tableModel]="tableModel"
      [newTypes]="newTypes"
      (save)="saveSchema($event)"
      [schemaGenerationWorking]="schemaGenerationWorking"
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
  schemaGenerationResult: TaxiSubmissionResult;
  tableModel: Type;
  newTypes: Type[];

  constructor(private activeRoute: ActivatedRoute,
              private importerService: DbConnectionService,
              private typeService: TypesService) {
    activeRoute.params.pipe(
      mergeMap(params => {
        this.connectionName = params.connectionName;
        this.table = {
          tableName: params.tableName,
          schemaName: params.schemaName
        };
        return importerService.getColumns(
          params.connectionName,
          params.schemaName,
          params.tableName
        );
      })
    ).subscribe(metadata => this.setMetdata(metadata));

    this.typeService.getTypes()
      .subscribe(schema => this.schema = schema);
  }

  generateSchema() {
    this.schemaGenerationWorking = true;
    this.importerService.generateTaxiForTable(
      this.connectionName,
      [this.table],
      'io.vyne.fixme'
    ).subscribe(generatedSchema => {
      this.schemaGenerationWorking = false;
      this.schemaGenerationResult = generatedSchema;
      this.newTypes = generatedSchema.types;
      const tableModel = generatedSchema.types.find(type => {
        const tableMetadata = type.metadata.find(m => {
          return m.name.fullyQualifiedName === 'io.vyne.jdbc.Table' &&
            m.params['name'] === this.table.tableName && m.params['schema'] === this.table.schemaName;
        });
        return !isNullOrUndefined(tableMetadata);
      });
      this.tableModel = tableModel;
      const metadata = this.tableMetadata;
      Object.keys(this.tableModel.attributes).forEach(key => {
        const field = this.tableModel.attributes[key];
        const column = metadata.columns.find(c => c.columnName === key);
        if (column) {
          column.taxiType = field.type;
        }
      });
      this.setMetdata(metadata);
    });
  }

  private setMetdata(metadata: TableMetadata) {
    this.tableMetadata = metadata;
    this.tableMetadata$.next(metadata);
  }

  saveSchema($event: TableMetadata) {
    const columnMappings = $event.columns.map(column => {
      const columnType = findType(this.schema, column.taxiType.parameterizedName, this.newTypes);
      return {
        name: column.columnName,
        typeSpec: {
          metadata: [],
          taxi: columnType.sources[0],
          typeName: columnType.name.parameterizedName
        }
      } as ColumnMapping;
    });
    const services: VersionedSource[] = this.schemaGenerationResult.services
      .flatMap(s => s.sourceCode);
    const request: TableModelSubmissionRequest = {
      model: {
        metadata: [],
        taxi: this.tableModel.sources[0],
        typeName: this.tableModel.name.parameterizedName
      },
      columnMappings: columnMappings,
      serviceMappings: services
    };

    this.importerService.submitModel(
      this.connectionName,
      this.table.schemaName,
      this.table.tableName,
      request
    ).subscribe(
      result => {
        console.log(JSON.stringify(result));
      },
      error => {
        console.log(JSON.stringify(error));
      }
    );
  }
}
