import {Component, Input, OnInit} from '@angular/core';
import {MappedTable} from './db-importer.service';

@Component({
  selector: 'app-table-selector',
  template: `
    <table class="table-selector">
      <thead>
      <tr>
        <th>Schema</th>
        <th>Table name</th>
        <th>Mapped type</th>
      </tr>
      </thead>
      <tbody>
      <tr *ngFor="let table of tables">
        <td>{{ table.table.schemaName }}</td>
        <td>{{ table.table.tableName }}</td>
        <td>
          <div *ngIf="table.mappedTo">
            <span class="mono-badge">
              <a [routerLink]="['types',table.mappedTo.fullyQualifiedName]">{{ table.mappedTo.longDisplayName }}</a>
            </span>
            <button mat-stroked-button>Remove</button>
          </div>
          <div *ngIf="!table.mappedTo">
            <button mat-stroked-button>Add mapping</button>
          </div>
        </td>
      </tr>
      </tbody>
    </table>
  `,
  styleUrls: ['./table-selector.component.scss']
})
export class TableSelectorComponent {

  @Input()
  tables: MappedTable[];
}
