import { TuiTextfieldControllerModule, TuiInputModule, TuiMultiSelectModule, TuiSelectModule } from "@taiga-ui/legacy";
import { Component, EventEmitter, Inject, Injector, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs/internal/Observable';
import { TuiDialogService, TuiDataList, TuiIcon, TuiButton } from '@taiga-ui/core';
import { TuiDataListWrapper, TuiStringifyContentPipe, TuiFilterByInputPipe, TuiButtonLoading } from '@taiga-ui/kit';
import { PolymorpheusComponent } from '@taiga-ui/polymorpheus';
import { ConvertSchemaEvent, TableSchemaConverterOptions } from '../../data-source-import.models';
import { ConnectorSummary, MappedTable } from '../../../db-connection-editor/db-importer.service';
import {
  ConnectionEditorContext,
  DbConnectionEditorDialogComponent
} from '../../../db-connection-editor/db-connection-editor-dialog.component';
import { PackageIdentifier } from '../../../package-viewer/packages.service';
import { ConnectionFiltersModule } from '../../../utils/connections.pipe';
import { sanitiseNamespace } from '../../../utils/utils';

@Component({
  selector: 'app-database-table-config',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TuiDataList,
    TuiIcon,
    ConnectionFiltersModule,
    TuiDataListWrapper,
    TuiStringifyContentPipe,
    TuiFilterByInputPipe,
    TuiButton,
    TuiInputModule,
    TuiSelectModule,
    TuiMultiSelectModule,
    TuiTextfieldControllerModule,
    TuiButtonLoading
],
  template: `
    <div class="form-container">
      <form class="form-body" #databaseForm="ngForm">
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
              (ngModelChange)="selectedConnectionChanged($event)"
              name="connection"
              required
            >
              Connection name
              <tui-data-list *tuiDataList>
                <button
                  tuiOption
                  class="link"
                  (click)="createNewConnection()"
                >
                  <tui-icon icon="@tui.circle-plus" class="icon"></tui-icon>
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
            <h3>Table(s)</h3>
            <div class="help-text">
              Select the table(s) you wish to import
            </div>
          </div>
          <div class="form-element">
            <tui-multi-select
              [(ngModel)]="selectedTables"
              [stringify]="stringifyTableName"
              [tuiTextfieldCleaner]="true"
              [disabled]="selectedConnection == null"
              name="selectedTable"
              required
            >
              Table name(s)
              <tui-data-list-wrapper
                *tuiDataList
                tuiMultiSelectGroup
                [itemContent]="stringifyTableName | tuiStringifyContent"
                [items]="tables$  | async | tuiFilterByInput"
              ></tui-data-list-wrapper>
            </tui-multi-select>
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
            <tui-input [(ngModel)]="defaultNamespace" name="defaultNamespace" required>
              Default namespace
            </tui-input>
          </div>
        </div>
      </form>
    </div>

    <div class="form-button-bar">
      <button tuiButton [loading]="working" (click)="doCreate()" [size]="'m'" [disabled]="databaseForm.invalid">Configure
      </button>
    </div>
  `,
  styleUrls: ['./database-table-config.component.scss']
})
export class DatabaseTableConfigComponent {

  constructor(
    @Inject(Injector) private readonly injector: Injector,
    @Inject(TuiDialogService) private readonly dialogService: TuiDialogService
  ) {
  }

  selectedTables: MappedTable[];
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
    tableSchemaConverterOptions.tables = this.selectedTables.map(table => {
      return (
        {
          table: table.table,
          defaultNamespace: this.defaultNamespace
        }
      )
    })
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
