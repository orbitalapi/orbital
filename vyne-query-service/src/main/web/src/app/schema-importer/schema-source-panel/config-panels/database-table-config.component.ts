import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ConvertSchemaEvent, TableSchemaConverterOptions} from '../../schema-importer.models';
import {ConnectorSummary, MappedTable} from '../../../db-connection-editor/db-importer.service';
import {MatDialog} from '@angular/material/dialog';
import {DbConnectionEditorComponent} from '../../../db-connection-editor/db-connection-editor.component';
import {Observable} from 'rxjs/internal/Observable';

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
            <tui-combo-box
              [stringify]="stringifyConnection"
              [(ngModel)]="selectedConnection"
              (ngModelChange)="selectedConnectionChanged($event)">
              Connection name
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
                        [value]="connection">{{ connection.connectionName }}</button>
              </tui-data-list>
            </tui-combo-box>
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
            <tui-combo-box [(ngModel)]="selectedTable"
                           [stringify]="stringifyTableName"
                           [disabled]="selectedConnection == null"
            >
              Table name
              <tui-data-list-wrapper
                *tuiDataList
                [itemContent]="stringifyTableName | tuiStringifyContent"
                [items]="tables$  | async | tuiFilterByInputWith : stringifyTableName"
              ></tui-data-list-wrapper>
            </tui-combo-box>
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
            <tui-input>
              Default namespace
            </tui-input>
          </div>
        </div>
      </div>
    </div>

    <div class="form-button-bar">
      <button tuiButton [showLoader]="working" (click)="doCreate()" [size]="'m'">Create
      </button>
    </div>
  `,
  styleUrls: ['./database-table-config.component.scss']
})
export class DatabaseTableConfigComponent {

  constructor(private dialog: MatDialog) {
  }

  selectedTable: MappedTable;
  selectedConnection: ConnectorSummary = null;

  @Input()
  connections: ConnectorSummary[] = [];
  @Input()
  tables$: Observable<MappedTable[]>;

  @Output()
  connectionChanged = new EventEmitter<ConnectorSummary>();

  @Output()
  loadSchema = new EventEmitter<ConvertSchemaEvent>()

  @Input()
  working: boolean = false;


  doCreate() {
    const tableSchemaConverterOptions = new TableSchemaConverterOptions();
    tableSchemaConverterOptions.tables = [{
      table: this.selectedTable.table
    }];
    tableSchemaConverterOptions.connectionName = this.selectedConnection.connectionName;
    console.log(JSON.stringify(tableSchemaConverterOptions, null, 2));
    this.loadSchema.next(new ConvertSchemaEvent('databaseTable', tableSchemaConverterOptions));
  }

  readonly stringifyConnection = (item: ConnectorSummary) => item.connectionName;
  readonly stringifyTableName = (item: MappedTable) => item.table.tableName;

  selectedConnectionChanged(selectedConnector: ConnectorSummary) {
    this.connectionChanged.emit(selectedConnector);
  }

  createNewConnection() {
    const dialogRef = this.dialog.open(DbConnectionEditorComponent);
    dialogRef.afterClosed().subscribe(e => {
      console.log('Connection editor closed: ' + JSON.stringify(e));
    })

  }
}
