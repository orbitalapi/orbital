import {Component, Input, OnInit} from '@angular/core';
import {TableColumn, TableMetadata} from './db-importer.service';
import {AgGridColumnDefinitions} from '../data-explorer/csv-viewer.component';
import {ColDef, ValueGetterParams} from 'ag-grid-community';
import {QualifiedName, Schema} from '../services/schema';
import {ColumnApi} from 'ag-grid-community/dist/lib/columnController/columnApi';
import {TypeSelectorCellEditorComponent} from './type-selector-cell-editor.component';
import {CheckboxCellEditorComponent} from './checkbox-cell-editor.component';

@Component({
  selector: 'app-table-importer',
  template: `
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
  `,
  styleUrls: ['./table-importer.component.scss']
})
export class TableImporterComponent {

  @Input()
  tableMetadata: TableMetadata;

  @Input()
  schema: Schema;

  private self = this;

  columnDefs: ColDef[] = [
    {headerName: 'Column name', field: 'name', cellClass: 'read-only-cell'},
    {headerName: 'Database type', field: 'dbType', cellClass: 'read-only-cell'},
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
        return col.taxiType.shortDisplayName;
      },
      tooltipValueGetter: (params) => (params.data as TableColumn).taxiType.longDisplayName
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
  }
}
