import {Component, OnInit} from '@angular/core';
import {DbConnectionService, MappedTable} from './db-importer.service';
import {ActivatedRoute, Router} from '@angular/router';
import {mergeMap} from 'rxjs/operators';
import {Observable} from 'rxjs/index';

@Component({
  selector: 'app-table-selector-container',
  template: `
    <app-table-selector [tables]="tables | async" (mapToNewModel)="importNewTable($event)"></app-table-selector>
  `,
  styleUrls: ['./table-selector-container.component.scss']
})
export class TableSelectorContainerComponent {

  tables: Observable<MappedTable[]>;

  constructor(private service: DbConnectionService, private route: ActivatedRoute, private router: Router) {
    this.tables = route.params
      .pipe(
        mergeMap(params => service.getMappedTablesForConnection(params.connectionName))
      );
  }

  importNewTable(table: MappedTable) {
    this.router.navigate([table.table.schemaName, table.table.tableName], {relativeTo: this.route});
  }
}
