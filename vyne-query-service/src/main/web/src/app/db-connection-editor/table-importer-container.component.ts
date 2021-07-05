import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {DbConnectionService, TableMetadata} from './db-importer.service';
import {mergeMap} from 'rxjs/operators';
import {Observable} from 'rxjs/index';

@Component({
  selector: 'app-table-importer-container',
  template: `
    <app-table-importer [tableMetadata]="tableMetadata | async"></app-table-importer>
  `,
  styleUrls: ['./table-importer-container.component.scss']
})
export class TableImporterContainerComponent {
  tableMetadata: Observable<TableMetadata>;

  constructor(private activeRoute: ActivatedRoute, private importerService: DbConnectionService) {
    this.tableMetadata = activeRoute.params.pipe(
      mergeMap(params => {
        return importerService.getColumns(
          params.connectionName,
          params.schemaName,
          params.tableName
        );
      })
    );
  }
}
