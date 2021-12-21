import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {JsonSchemaConverterOptions, TableSchemaConverterOptions} from '../../schema-importer.models';
import {ConnectorSummary, DbConnectionService, MappedTable} from '../../../db-connection-editor/db-importer.service';
import {JsonSchemaVersionOption} from './jsonschema-config.component';
import {MatDialog} from '@angular/material/dialog';
import {DbConnectionWizardComponent} from '../../../db-connection-editor/db-connection-wizard.component';
import {DbConnectionEditorComponent} from '../../../db-connection-editor/db-connection-editor.component';

@Component({
  selector: 'app-database-table-config',
  template: `
    <div class="form-container">
      <div class="form-body">
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Connection</h3>
            <div class="help-text">
              Select the Database connection for the table you wish to import
            </div>
          </div>
          <div class="form-element">
            <tui-select [(ngModel)]="tableSchemaConverterOptions.connectionName"
                        (ngModelChange)="selectedConnectionChanged($event)">
              Table name
              <tui-data-list *tuiDataList>
                <button
                  tuiOption
                  class="link"
                  (click)="createNewConnection()"
                >
                  <tui-svg src="tuiIconPlusCircleLarge" class="icon"></tui-svg>
                  Add new connection...
                </button>
                <button *ngFor="let connection of connections" tuiOption
                        [value]="connection.connectionName">{{ connection.connectionName }}</button>
              </tui-data-list>
            </tui-select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Table</h3>
            <div class="help-text">
              Select the table you wish to import
            </div>
          </div>
          <div class="form-element">
            <tui-select [(ngModel)]="tableSchemaConverterOptions.tableName">
              Table name
              <tui-data-list-wrapper
                *tuiDataList
                [itemContent]="stringifyTableName | tuiStringifyContent"
                [items]="tables  | tuiFilterByInputWith : stringifyTableName"
              ></tui-data-list-wrapper>
            </tui-select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Default namespace</h3>
            <div class="help-text">
              Defines a namespace which any newly created types will be created in
            </div>
          </div>
          <div class="form-element">
            <tui-input [(ngModel)]="tableSchemaConverterOptions.defaultNamespace">
              Default namespace
            </tui-input>
          </div>
        </div>
      </div>
    </div>

    <div class="form-button-bar">
      <button mat-flat-button color="primary" (click)="doCreate()">Create
      </button>
    </div>
  `,
  styleUrls: ['./database-table-config.component.scss']
})
export class DatabaseTableConfigComponent {

  constructor(private dialog: MatDialog) {
  }

  tableSchemaConverterOptions = new TableSchemaConverterOptions();

  @Input()
  connections: ConnectorSummary[] = [];
  @Input()
  tables: MappedTable[] = [];

  @Output()
  connectionChanged = new EventEmitter<ConnectorSummary>();

  @Output()
  loadSchema = new EventEmitter<TableSchemaConverterOptions>()

  doCreate() {
    console.log(JSON.stringify(this.tableSchemaConverterOptions, null, 2));
    this.loadSchema.next(this.tableSchemaConverterOptions);
  }

  readonly stringifyConnection = (item: ConnectorSummary) => item.connectionName;
  readonly stringifyTableName = (item: MappedTable) => item.table.tableName;

  selectedConnectionChanged($event: any) {

  }

  createNewConnection() {
    const dialogRef = this.dialog.open(DbConnectionEditorComponent);
    dialogRef.afterClosed().subscribe(e => {
      console.log('Connection editor closed: ' + JSON.stringify(e));
    })

  }
}
