import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {DbConnectionService, TableMetadata} from './db-importer.service';
import {mergeMap} from 'rxjs/operators';
import {Observable} from 'rxjs';
import {TypesService} from '../services/types.service';
import {Schema} from '../services/schema';

@Component({
  selector: 'app-table-importer-container',
  template: `
    <app-table-importer
      [schema]="schema"
      [tableMetadata]="tableMetadata | async"></app-table-importer>
  `,
  styleUrls: ['./table-importer-container.component.scss']
})
export class TableImporterContainerComponent {
  tableMetadata: Observable<TableMetadata>;
  schema: Schema;

  constructor(private activeRoute: ActivatedRoute,
              private importerService: DbConnectionService,
              private typeService: TypesService) {
    this.tableMetadata = activeRoute.params.pipe(
      mergeMap(params => {
        return importerService.getColumns(
          params.connectionName,
          params.schemaName,
          params.tableName
        );
      })
    );
    this.typeService.getTypes()
      .subscribe(schema => this.schema = schema);
  }
}
