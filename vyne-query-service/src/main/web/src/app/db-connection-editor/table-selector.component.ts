import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
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
              <a [routerLink]="['/types',table.mappedTo.fullyQualifiedName]">{{ table.mappedTo.longDisplayName }}</a>
            </span>
            <button mat-stroked-button [routerLink]="[table.table.schemaName, table.table.tableName]">Manage</button>
          </div>
          <div *ngIf="!table.mappedTo">
            <button mat-stroked-button [mat-menu-trigger-for]="mappingTypeMenu">Add mapping</button>
            <mat-menu #mappingTypeMenu="matMenu">
              <button mat-menu-item (click)="createMappingToExistingType(table)">To existing model...</button>
              <button [routerLink]="[table.table.schemaName, table.table.tableName]"  mat-menu-item>To new model...</button>
            </mat-menu>
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

  @Output()
  mapToNewModel = new EventEmitter<MappedTable>();

  createMappingToExistingType(table: MappedTable) {

  }
}
