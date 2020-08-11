import {Component, HostListener, Input, ViewEncapsulation} from '@angular/core';
import {ParsedCsvContent} from '../services/types.service';
import {MatDialog, MatDialogConfig} from '@angular/material/dialog';
import {AssignTypeToColumnDialogComponent} from './assign-types-dialog/assign-type-to-column-dialog.component';
import {Schema, Type} from '../services/schema';

interface ColumnDefs {
  headerName: string;
  field: string;
}

interface HeaderTypes {
  headerName: string;
  index: number;
  type: string;
}

@Component({
  selector: 'app-csv-viewer',
  template: `
    <ag-grid-angular
      style="width: 100%; height: 60vh;"
      class="ag-theme-alpine"
      [rowData]="data"
      [columnDefs]="columnDefs">
    </ag-grid-angular>
  `,
  styleUrls: ['./csv-viewer.component.scss'],
  encapsulation: ViewEncapsulation.None
})

export class CsvViewerComponent {
  constructor(private dialog: MatDialog) {
  }

  private _firstRowAsHeaders = false;
  private _source: ParsedCsvContent;
  private _isTypeNamePanelVisible: boolean;
  rowData: string[][] = [];
  headers: string[] = [];
  columnDefs: ColumnDefs[] = [];
  data: {}[] = [];
  headersWithAssignedTypes: HeaderTypes[] = [];
  @Input()
  schema: Schema;
  addedType: Type;

  @HostListener('click', ['$event'])
  onClick(e) {
    const selectedColumnName = e.target.id;
    const dialogConfig = new MatDialogConfig();

    dialogConfig.disableClose = true;
    dialogConfig.autoFocus = true;
    dialogConfig.data = {};

    if (selectedColumnName) {
      const dialogRef = this.dialog.open(AssignTypeToColumnDialogComponent, {
        data: {schema: this.schema}
      });
      dialogRef.afterClosed().subscribe(result => {
        this.addedType = result;
        dialogConfig.data = this.handleTypeAssignments(selectedColumnName, result.fullyQualifiedName);
        this.getColumnDefinitions();
      });
    }
  }

  private handleTypeAssignments(selectedColumnName: string, changedType) {
    this.headersWithAssignedTypes = this.headersWithAssignedTypes.map((item, index) => {
      return {
        headerName: item.headerName,
        index: index,
        type: !item.type ? item.headerName === selectedColumnName ? changedType : '' : item.type
      };
    });
    return this.headersWithAssignedTypes;
  }

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

  @Input()
  get isTypeNamePanelVisible(): boolean {
    return this._isTypeNamePanelVisible;
  }

  set isTypeNamePanelVisible(value: boolean) {
    this._isTypeNamePanelVisible = value;
    this.getColumnDefinitions();
  }

  private getColumnDefinitions() {
    this.columnDefs = this.headers.map((fieldName, index) => {
      const shouldDisplayAddButtons = (this.isTypeNamePanelVisible && this.headersWithAssignedTypes[index].type.length === 0);
      const shouldDisplayBadges = (this.isTypeNamePanelVisible && this.headersWithAssignedTypes[index].type.length > 0);

      return {
        headerName: fieldName,
        field: fieldName,
        width: shouldDisplayBadges ? 320 : 200,
        headerComponentParams: {
          template:
            `<div class="ag-cell-label-container" role="presentation">
                <span ref="eMenu" class="ag-header-icon ag-header-cell-menu-button"></span>
                <div ref="eLabel" class="ag-header-cell-label" role="presentation" style="display:grid">
                    <span ref="eText" class="ag-header-cell-text" role="columnheader"></span>
                    <div style="display: ${shouldDisplayBadges ? 'flex' : 'none'}">
                    <span class="mono-badge">${this.headersWithAssignedTypes[index].type}</span>
                </div>
                <div style="display: ${shouldDisplayAddButtons ? 'flex' : 'none'}">
                    <span class="add-type-badge" id="${fieldName}">Add Type</span>
                </div>
                </div>
            </div>`,
        }
      };
    });
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
      this.headersWithAssignedTypes = this.headers.map((header, index) => {
        return {
          headerName: header,
          index: index,
          type: ''
        };
      });
      this.getColumnDefinitions();
    }
  }
}
