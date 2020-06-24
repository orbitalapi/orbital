import {Component, Input, OnInit} from '@angular/core';
import {ParsedCsvContent} from '../services/types.service';

@Component({
  selector: 'app-csv-viewer',
  template: `
    <table mat-table [dataSource]="rowData">
      <ng-container [matColumnDef]="'column-' + i" *ngFor="let columnValue of headers; index as i">
        <th mat-header-cell *matHeaderCellDef>{{ columnValue }}</th>
        <td mat-cell *matCellDef="let element"> {{element[i]}} </td>
      </ng-container>


      <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
      <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
    </table>
  `,
  styleUrls: ['./csv-viewer.component.scss']
})
export class CsvViewerComponent {
  private _firstRowAsHeaders = false;
  private _source: ParsedCsvContent;
  rowData: string[][] = [];
  headers: string[] = [];

  displayedColumns: string[];


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
        this.displayedColumns = this.source.headers.map((columnValue: string, index: number) => 'column-' + index);
      } else {
        if (this.source.records.length === 0) {
          this.headers = [];
          this.displayedColumns = [];
        } else {
          const templateRow = this.source.records[0];
          this.headers = templateRow.map((value, index: number) => 'Column ' + index + 1);
          this.displayedColumns = templateRow.map((columnValue: string, index: number) => 'column-' + index);
        }

      }
    }
  }
}
