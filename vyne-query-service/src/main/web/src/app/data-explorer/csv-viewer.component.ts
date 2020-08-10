import {Component, Input, OnInit} from '@angular/core';
import {ParsedCsvContent} from '../services/types.service';

interface ColumnDefs {
headerName: string;
field: string;
}

@Component({
  selector: 'app-csv-viewer',
  template: `
    <ag-grid-angular
      style="width: 100%; height: 50vh;"
      class="ag-theme-alpine"
      [rowData]="data"
      [columnDefs]="columnDefs"
    >
    </ag-grid-angular>
  `,
  styleUrls: ['./csv-viewer.component.scss']
})


export class CsvViewerComponent {
  private _firstRowAsHeaders = false;
  private _source: ParsedCsvContent;
  rowData: string[][] = [];
  headers: string[] = [];
  columnDefs: ColumnDefs[] = [];
  data: {}[] = [];

  @Input()
  get firstRowAsHeaders(): boolean {
    return this._firstRowAsHeaders;
  }

  set firstRowAsHeaders(value: boolean) {
    this._firstRowAsHeaders = value;
    this.updateRowData();
  }

  @Input()
  get source(): ParsedCsvContent {
    return this._source;
  }

  set source(value: ParsedCsvContent) {
    this._source = value;
    this.updateRowData();
  }

  private updateRowData() {
    if (!this.source) {
      this.rowData = [];
    } else {
      this.rowData = this.source.records;
      if (this.firstRowAsHeaders) {
        this.headers = this.source.headers;
      } else {
        if (this.source.records.length === 0) {
          this.headers = [];
        } else {
          const templateRow = this.source.records[0];
          this.headers = templateRow.map((value, index: number) => 'Column ' + index + 1);
        }
      }
      this.data = this.rowData.map((value: string[]) => {
        const row = {};
        value.map((item: string, index: number) => {
          row[this.headers[index]] = item;
        });
        return row;
      });
      this.columnDefs = this.headers.map(fieldName => {
        return {
          headerName: fieldName,
          field: fieldName,
        };
      });
    }
  }
}
