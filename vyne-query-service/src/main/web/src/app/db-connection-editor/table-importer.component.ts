import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {TableColumn, TableMetadata} from './db-importer.service';
import {AgGridColumnDefinitions} from '../data-explorer/csv-viewer.component';
import {ColDef, ValueGetterParams} from 'ag-grid-community';
import {QualifiedName, Schema, Type} from '../services/schema';
import {ColumnApi} from 'ag-grid-community/dist/lib/columnController/columnApi';
import {TypeSelectorCellEditorComponent} from './type-selector-cell-editor.component';
import {CheckboxCellEditorComponent} from './checkbox-cell-editor.component';
import {Observable} from 'rxjs/internal/Observable';
import {Subscription} from 'rxjs';
import {GridApi} from 'ag-grid-community/dist/lib/gridApi';

@Component({
  selector: 'app-table-importer',
  template: `
    <div class="toolbar">
      <button mat-stroked-button [disabled]="schemaGenerationWorking" (click)="generateSchema.emit()">Auto generate
        schema
      </button>
      <span *ngIf="schemaGenerationWorking">Generating schema...</span>
      <mat-progress-bar mode="indeterminate" *ngIf="schemaGenerationWorking"></mat-progress-bar>
    </div>
    <div class="grid-wrapper" *ngIf="tableMetadata">
      <ag-grid-angular
        #agGrid
        style="width: 100%; height: 100%;"
        id="myGrid"
        class="ag-theme-alpine"
        tooltipShowDelay="250"
        tooltipMouseTrack="true"
        [rowData]="tableMetadata.columns"
        [columnDefs]="columnDefs"
        [singleClickEdit]="true"
        [defaultColDef]="defaultColDef"
        [frameworkComponents]="frameworkComponents"
        (gridReady)="onGridReady($event)"
      ></ag-grid-angular>
    </div>
    <div class="error-message-box" *ngIf="errorMessage">
      {{errorMessage}}
    </div>
    <div class="toolbar">
      <button mat-flat-button color="primary" (click)="save.emit(tableMetadata)">Save</button>
      <mat-progress-bar mode="indeterminate" *ngIf="saveSchemaWorking"></mat-progress-bar>
    </div>
  `,
  styleUrls: ['./table-importer.component.scss']
})
export class TableImporterComponent {

  @Input()
  schemaGenerationWorking = false;

  tableMetadata: TableMetadata;
  private metadataSubscription: Subscription;
  private _tableMetadata$: Observable<TableMetadata>;

  @Input()
  errorMessage: string;

  @Input()
  saveSchemaWorking = false;

  @Input()
  get tableMetadata$(): Observable<TableMetadata> {
    return this._tableMetadata$;
  }

  set tableMetadata$(value) {
    if (this.tableMetadata$ === value) {
      return;
    }
    if (this.metadataSubscription) {
      this.metadataSubscription.unsubscribe();
    }
    this._tableMetadata$ = value;
    this.metadataSubscription = this.tableMetadata$.subscribe(m => {
      this.tableMetadata = m;
      if (this.gridApi) {
        this.gridApi.setRowData(this.tableMetadata.columns);
      }
    });

  }

  @Input()
  schema: Schema;

  @Input()
  tableModel: Type;

  @Input()
  newTypes: Type[];

  @Output()
  generateSchema = new EventEmitter();

  @Output()
  save = new EventEmitter<TableMetadata>();

  gridApi: GridApi;

  columnDefs: ColDef[] = [
    {headerName: 'Column name', field: 'columnName', cellClass: 'read-only-cell'},
    {headerName: 'Database type', field: 'dataType', cellClass: 'read-only-cell'},
    {
      headerName: 'Optional', field: 'nullable', editable: false,
      cellEditor: 'checkboxEditor',
      cellRenderer: 'checkboxEditor'
      , cellClass: 'read-only-cell'
    },
    {
      cellClass: 'editable-cell',
      headerName: 'Taxonomy type',
      field: 'taxiType',
      editable: true,
      cellEditor: 'typePicker',
      cellEditorParams: {
        schema: () => {
          // Passing a value directly doesn't seem to work, but the closure does.
          // Must be more js 'this' madness
          return this.schema;
        }
      },
      valueGetter: (params: ValueGetterParams) => {
        const col = params.data as TableColumn;
        return col.taxiType ? col.taxiType.shortDisplayName : null;
      },
      tooltipValueGetter: (params) => {
        const column = (params.data as TableColumn);
        return column.taxiType ? column.taxiType.longDisplayName : 'No type mapped';
      }
    },

  ];

  defaultColDef: {
    flex: 1,
    resizable: true,
    editable: false,
    minWidth: 110
  };

  frameworkComponents = {
    'typePicker': TypeSelectorCellEditorComponent,
    checkboxEditor: CheckboxCellEditorComponent
  };

  onGridReady($event: any) {
    this.gridApi = $event.api;
  }
}
