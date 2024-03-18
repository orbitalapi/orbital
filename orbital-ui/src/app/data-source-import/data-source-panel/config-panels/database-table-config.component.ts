import { Component, EventEmitter, Inject, Injector, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs/internal/Observable';
import { MatLegacyDialog as MatDialog } from '@angular/material/legacy-dialog';
import { TuiButtonModule, TuiDataListModule, TuiDialogService, TuiSvgModule } from '@taiga-ui/core';
import {
  TuiDataListWrapperModule,
  TuiFilterByInputPipeModule, TuiInputModule,
  TuiSelectModule,
  TuiStringifyContentPipeModule
} from '@taiga-ui/kit';
import { PolymorpheusComponent } from '@tinkoff/ng-polymorpheus';
import { ConvertSchemaEvent, TableSchemaConverterOptions } from '../../data-source-import.models';
import { ConnectorSummary, MappedTable } from '../../../db-connection-editor/db-importer.service';
import {
  ConnectionEditorContext,
  DbConnectionEditorDialogComponent
} from '../../../db-connection-editor/db-connection-editor-dialog.component';
import { PackageIdentifier } from '../../../package-viewer/packages.service';
import { ConnectionFiltersModule } from '../../../utils/connections.pipe';
import { DbConnectionEditorModule } from '../../../db-connection-editor/db-connection-editor.module';
import { sanitiseNamespace } from '../../../utils/utils';

@Component({
  selector: 'app-database-table-config',
  standalone: true,
  imports: [
    CommonModule,
    TuiSelectModule,
    FormsModule,
    TuiDataListModule,
    TuiSvgModule,
    ConnectionFiltersModule,
    TuiDataListWrapperModule,
    TuiStringifyContentPipeModule,
    TuiFilterByInputPipeModule,
    TuiButtonModule,
    TuiInputModule,
    DbConnectionEditorModule
  ],
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
            <tui-select
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
                <button *ngFor="let connection of connections | databases" tuiOption
                        [value]="connection">{{ connection.connectionName }}
                </button>
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
            <tui-select [(ngModel)]="selectedTable"
                        [stringify]="stringifyTableName"
                        [disabled]="selectedConnection == null"
            >
              Table name
              <tui-data-list-wrapper
                *tuiDataList
                [itemContent]="stringifyTableName | tuiStringifyContent"
                [items]="tables$  | async | tuiFilterByInputWith : stringifyTableName"
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
            <tui-input [(ngModel)]="defaultNamespace">
              Default namespace
            </tui-input>
          </div>
        </div>
      </div>
    </div>

    <div class="form-button-bar">
      <button tuiButton [showLoader]="working" (click)="doCreate()" [size]="'m'">Configure
      </button>
    </div>
  `,
  styleUrls: ['./database-table-config.component.scss']
})
export class DatabaseTableConfigComponent {

  constructor(private dialog: MatDialog,
              @Inject(Injector) private readonly injector: Injector,
              @Inject(TuiDialogService) private readonly dialogService: TuiDialogService) {
  }

  selectedTable: MappedTable;
  selectedConnection: ConnectorSummary = null;

  @Input()
  connections: ConnectorSummary[] = [];
  @Input()
  tables$: Observable<MappedTable[]>;

  @Input()
  packageIdentifier: PackageIdentifier;

  @Output()
  connectionChanged = new EventEmitter<ConnectorSummary>();

  @Output()
  loadSchema = new EventEmitter<ConvertSchemaEvent>()

  @Input()
  working: boolean = false;

  defaultNamespace: string = null;


  doCreate() {
    const tableSchemaConverterOptions = new TableSchemaConverterOptions();
    tableSchemaConverterOptions.tables = [
      {
        table: this.selectedTable.table,
        defaultNamespace: this.defaultNamespace
      }
    ];
    tableSchemaConverterOptions.connectionName = this.selectedConnection.connectionName;
    this.loadSchema.next(new ConvertSchemaEvent('databaseTable', tableSchemaConverterOptions, this.packageIdentifier));
  }

  readonly stringifyConnection = (item: ConnectorSummary) => item.connectionName;
  readonly stringifyTableName = (item: MappedTable) => item.table.tableName;

  selectedConnectionChanged(selectedConnector: ConnectorSummary) {
    const {organisation, name} = this.packageIdentifier;
    this.defaultNamespace = sanitiseNamespace(`${organisation}.${name}.${selectedConnector.connectionName}`);
    this.connectionChanged.emit(selectedConnector);
  }

  createNewConnection() {
    this.dialogService.open<ConnectorSummary>(new PolymorpheusComponent(DbConnectionEditorDialogComponent, this.injector),
      {
        data: new ConnectionEditorContext(null, 'JDBC', 'edit', this.packageIdentifier),
      })
      .subscribe((result: ConnectorSummary) => {
        this.connections.push(result);
        this.selectedConnection = result;
        this.selectedConnectionChanged(result);
      })
  }
}
