import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CsvOptions, ParsedCsvContent} from '../services/types.service';
import {MatDialog, MatDialogConfig} from '@angular/material/dialog';
import {
  AssignedTypeData,
  AssignTypeToColumnDialogComponent
} from './assign-types-dialog/assign-type-to-column-dialog.component';
import {Schema} from '../services/schema';
import {GridHeaderActionsComponent} from './custom-csv-table-header';
import {Subscription} from 'rxjs';
import {CustomCsvTableHeaderService} from '../services/custom-csv-table-header.service';

export interface AgGridColumnDefinitions {
  headerName: string;
  field: string;
}

export interface HeaderTypes {
  fieldName: string;
  typeName: string;
  format?: string;
}

@Component({
  selector: 'app-csv-viewer',
  template: `
    <ag-grid-angular
      class="ag-theme-alpine"
      [enableCellTextSelection]="true"
      [rowData]="data"
      [columnDefs]="columnDefs">
    </ag-grid-angular>
  `,
  styleUrls: ['./csv-viewer.component.scss'],
})

export class CsvViewerComponent {
  constructor(private dialog: MatDialog, private customCsvTableHeaderService: CustomCsvTableHeaderService) {
    this.subscription = this.customCsvTableHeaderService.getFieldName().subscribe(fieldName => {
      this.renderAddTypePopup(fieldName);
    });
    this.subscription = this.customCsvTableHeaderService.getTypeToRemove().subscribe(fieldName => {
      this.onRemoveType(fieldName);
    });
  }

  @Input()
  schema: Schema;
  @Input()
  csvOptions: CsvOptions;
  private _firstRowAsHeaders = false;
  private _source: ParsedCsvContent;
  private _isTypeNamePanelVisible: boolean;
  private _isGenerateSchemaPanelVisible: boolean;
  rowData: string[][] = [];
  headers: string[] = [];
  columnDefs: AgGridColumnDefinitions[] = [];
  data: {}[] = [];
  headersWithAssignedTypes: HeaderTypes[] = [];
  @Output() headerTypesChanged: EventEmitter<HeaderTypes[]> = new EventEmitter<HeaderTypes[]>();
  subscription: Subscription;

  renderAddTypePopup(selectedColumnName: string) {
    const dialogConfig = new MatDialogConfig();

    dialogConfig.disableClose = true;
    dialogConfig.autoFocus = true;
    dialogConfig.data = {};
    const dialogRef = this.dialog.open(AssignTypeToColumnDialogComponent, {
      data: {schema: this.schema},
      panelClass: 'add-type-panel-container'
    });
    dialogRef.afterClosed().subscribe((result: AssignedTypeData) => {
      if (result) {
        this.handleTypeAssignments(selectedColumnName, result.targetType.fullyQualifiedName, result.format);
        this.getColumnDefinitions();
      }
    });
  }

  private handleTypeAssignments(selectedColumnName: string, changedTypeName: string, format?: string) {
    this.headersWithAssignedTypes = this.headersWithAssignedTypes.map((item) => {
      return {
        fieldName: item.fieldName,
        typeName: !item.typeName ? item.fieldName === selectedColumnName ? changedTypeName : '' : item.typeName,
        format: !item.format ? (item.fieldName === selectedColumnName ? (format || '') : '') : item.format
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

  private calculateOptimalColumnWidth(fieldName: string, typeName: string) {
    const pixelByChar = 8;
    const defaultColumnWidth = 200;
    const doesDefaultWidthFits = fieldName.length < 20;
    return typeName && typeName.length > fieldName.length
      ? typeName.length * pixelByChar
      : doesDefaultWidthFits ? defaultColumnWidth : (fieldName.length * pixelByChar);
  }

  private getColumnDefinitions() {
    this.columnDefs = this.headers.map((fieldName, index) => {
      const typeName = this.headersWithAssignedTypes[index].typeName;
      const shouldDisplayAddButtons = (this.isTypeNamePanelVisible && typeName.length === 0);
      const shouldDisplayBadges = (this.isTypeNamePanelVisible && typeName.length > 0);
      const columnWidth = this.calculateOptimalColumnWidth(fieldName, typeName);
      return {
        headerName: fieldName,
        field: fieldName,
        width: columnWidth,
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
      if (this.csvOptions && this.csvOptions.firstRecordAsHeader) {
        this.headers = this.source.headers;
      } else {
        if (this.source.records.length === 0) {
          this.headers = [];
        } else {
          const templateRow = this.source.records[0];
          this.headers = templateRow.map((value, index: number) => 'Column ' + (index + 1));
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

  private onRemoveType(fieldName: string) {
    const match = this.headersWithAssignedTypes.filter(item => item.fieldName === fieldName);
    this.headersWithAssignedTypes[this.headersWithAssignedTypes.indexOf(match[0])].typeName = '';
    this.headerTypesChanged.emit(this.headersWithAssignedTypes);
    this.getColumnDefinitions();
  }
}
