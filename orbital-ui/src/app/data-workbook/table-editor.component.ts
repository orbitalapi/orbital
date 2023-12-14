import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {GridApi} from 'ag-grid-community/dist/lib/gridApi';
import {ShouldRowBeSkippedParams} from 'ag-grid-community';

@Component({
  selector: 'app-table-editor',
  template: `
    <ag-grid-angular
      class="ag-theme-alpine"
      [rowData]="rowData"
      [columnDefs]="columnDefs"
      [singleClickEdit]="true"
      (gridReady)="onGridReady($event)"
      (cellEditingStopped)="onCellEditFinished($event)"
    ></ag-grid-angular>
  `,
  styleUrls: ['./table-editor.component.scss']
})
export class TableEditorComponent {

  private gridApi: GridApi;
  columnDefs = ['A', 'B', 'C', 'D', 'E', 'F', 'G'].map(field => {
    return {
      field,
      editable: true
    };
  });

  @Output()
  csvDataUpdated = new EventEmitter<string>();

  rowData = Array.from({length: 10}, (x, i) => {
    const row: any = {};
    // @ts-ignore
    this.columnDefs.forEach(def => row[def.field] = '');
    return row;
  });


  onGridReady($event: any) {
    this.gridApi = $event.api;
  }

  onCellEditFinished($event: any) {
    const shouldRowBeSkpped = (params: ShouldRowBeSkippedParams) => {
      const data = params.node.data;
      const hasDataPopulated = this.columnDefs.some(column => {
        return data[column.field] !== '';
      });
      return !hasDataPopulated;
    };
    const dataAsCsv = this.gridApi.getDataAsCsv({
      shouldRowBeSkipped: shouldRowBeSkpped
    });
    console.log(dataAsCsv);
    this.csvDataUpdated.emit(dataAsCsv);
  }
}
