import {Component, EventEmitter, Input, Output, ViewEncapsulation} from '@angular/core';
import {ParsedCsvContent} from '../services/types.service';
import {MatDialog, MatDialogConfig} from '@angular/material/dialog';
import {AssignTypeToColumnDialogComponent} from './assign-types-dialog/assign-type-to-column-dialog.component';
import {QualifiedName, Schema} from '../services/schema';
import {GridHeaderActionsComponent} from './custom-csv-table-header';
import {Subscription} from 'rxjs';
import {CustomCsvTableHeaderService} from '../services/custom-csv-table-header.service';

interface ColumnDefs {
  headerName: string;
  field: string;
}

export interface HeaderTypes {
  fieldName: string;
  typeName: string;
}

@Component({
  selector: 'app-csv-viewer',
  template: `
    <ag-grid-angular
      style="width: 100%; height: 60vh;"
      class="ag-theme-alpine"
      [rowData]="data"
      (gridReady)="onGridReady($event)"
      [gridOptions]="gridOptions"
      [columnDefs]="columnDefs">
    </ag-grid-angular>
  `,
  styleUrls: ['./csv-viewer.component.scss'],
  // Note: Ag-Grid component cannot retrieve Css classes unless encapsulation is set to None.
  encapsulation: ViewEncapsulation.None
})

export class CsvViewerComponent {
  constructor(private dialog: MatDialog, private customCsvTableHeaderService: CustomCsvTableHeaderService) {
    this.subscription = this.customCsvTableHeaderService.getFieldName().subscribe(fieldName => {
      this.handleTypeAssignment(fieldName)
    });
  }

  @Input()
  schema: Schema;
  private _firstRowAsHeaders = false;
  private _source: ParsedCsvContent;
  private _isTypeNamePanelVisible: boolean;
  private _isGenerateSchemaPanelVisible: boolean;
  gridOptions = {headerHeight: 50};
  rowData: string[][] = [];
  headers: string[] = [];
  columnDefs: ColumnDefs[] = [];
  data: {}[] = [];
  headersWithAssignedTypes: HeaderTypes[] = [];
  @Output() headerTypesChanged: EventEmitter<HeaderTypes[]> = new EventEmitter<HeaderTypes[]>();
  private gridApi;
  private gridColumnApi;
  subscription: Subscription;


  handleTypeAssignment(fieldName) {
    const selectedColumnName = fieldName;
    const dialogConfig = new MatDialogConfig();

    dialogConfig.disableClose = true;
    dialogConfig.autoFocus = true;
    dialogConfig.data = {};
    if (selectedColumnName) {
      const dialogRef = this.dialog.open(AssignTypeToColumnDialogComponent, {
        data: {schema: this.schema}
      });
      dialogRef.afterClosed().subscribe((result: QualifiedName) => {
        if (result) {
          dialogConfig.data = this.handleTypeAssignments(selectedColumnName, result.fullyQualifiedName);
          this.getColumnDefinitions();
        }
      });
    }
  }

  onGridReady(params) {
    this.gridApi = params.api;
    this.gridColumnApi = params.columnApi;
  }

  private handleTypeAssignments(selectedColumnName: string, changedType: string) {
    this.headersWithAssignedTypes = this.headersWithAssignedTypes.map((item) => {
      return {
        fieldName: item.fieldName,
        typeName: !item.typeName ? item.fieldName === selectedColumnName ? changedType : '' : item.typeName
      };
    });
    this.headerTypesChanged.emit(this.headersWithAssignedTypes);
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

  @Input()
  get isGenerateSchemaPanelVisible(): boolean {
    return this._isGenerateSchemaPanelVisible;
  }

  set isGenerateSchemaPanelVisible(value: boolean) {
    this._isGenerateSchemaPanelVisible = value;
    this.getColumnDefinitions();
  }

  private getColumnDefinitions() {
    this.columnDefs = this.headers.map((fieldName, index) => {
      const typeName = this.headersWithAssignedTypes[index].typeName;
      const shouldDisplayAddButtons = (this.isTypeNamePanelVisible && typeName.length === 0);
      const shouldDisplayBadges = (this.isTypeNamePanelVisible && typeName.length > 0);

      return {
        headerName: fieldName,
        field: fieldName,
        width: shouldDisplayBadges ? 300 : 200,
        headerComponentFramework: GridHeaderActionsComponent,
        headerComponentParams: {
          typeName: typeName,
          shouldDisplayAddButtons: shouldDisplayAddButtons,
          shouldDisplayBadges: shouldDisplayBadges,
          fieldName: fieldName
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
      this.headersWithAssignedTypes = this.headers.map((header) => {
        return {
          fieldName: header,
          typeName: ''
        };
      });
      this.headerTypesChanged.emit(this.headersWithAssignedTypes);
      this.getColumnDefinitions();
    }
  }
}
