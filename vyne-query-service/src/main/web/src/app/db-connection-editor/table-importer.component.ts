import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {JdbcTable, JdbcColumn, TableMetadata, TableModelMapping, ColumnMapping} from './db-importer.service';
import {AgGridColumnDefinitions} from '../data-explorer/csv-viewer.component';
import {ColDef, ValueFormatterParams, ValueGetterParams, ValueSetterParams} from 'ag-grid-community';
import {findType, QualifiedName, Schema, Type} from '../services/schema';
import {ColumnApi} from 'ag-grid-community/dist/lib/columnController/columnApi';
import {TypeSelectorCellEditorComponent} from './type-selector-cell-editor.component';
import {CheckboxCellEditorComponent} from './checkbox-cell-editor.component';
import {Observable} from 'rxjs/internal/Observable';
import {Subscription} from 'rxjs';
import {GridApi} from 'ag-grid-community/dist/lib/gridApi';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {validNamespace, validTypeName} from '../services/validators';
import {NewTypeSpec} from '../type-editor/type-editor.component';
import {
  ConfirmationAction,
  ConfirmationDialogComponent,
  ConfirmationParams
} from '../confirmation-dialog/confirmation-dialog.component';
import {MatDialog} from '@angular/material/dialog';
import {isNullOrUndefined} from 'util';
import {capitalizeFirstLetter} from '../utils/strings';

@Component({
  selector: 'app-table-importer',
  template: `
    <h2>Create a model for table {{table?.tableName}}</h2>
    <div class="form-container">
      <div class="form-body" [formGroup]="tableSpecFormGroup">
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Model name</h3>
            <div class="help-text">
              <p>Define the name of your model. Must not contain spaces - the name of the table is often a good
                start.</p>
              <p>By convention, type names start with a capital letter for each word. For example:</p>
              <ul>
                <li>
                  <code>Customer</code>
                </li>
                <li>
                  <code>FirstName</code>
                </li>
              </ul>
            </div>

          </div>
          <mat-form-field appearance="outline">
            <mat-label>Model name</mat-label>
            <input matInput name="modelName" id="modelName" formControlName="typeName">
          </mat-form-field>
        </div>
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Model namespace</h3>
            <div class="help-text">
              <p>Provide a namespace for your model.</p>
              <p>Namespaces help keep things organised - it's like a category. Normally, this contains the business
                domain that the type relates to. If it's a specialised type for a
                specific application, you might want to include the application name. Often includes the company name
                too.</p>
              <ul>
                <li>
                  <code>com.acme.customers</code>
                </li>
                <li>
                  <code>com.acme.invoicing.invoicePlatform</code>
                </li>
              </ul>
            </div>

          </div>
          <mat-form-field appearance="outline">
            <mat-label>Model namespace</mat-label>
            <input matInput name="modelNamespace" id="modelNamespace" type="text" formControlName="namespace">
          </mat-form-field>
        </div>

        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Map columns</h3>
            <div class="help-text">
              <p>Assign types to each column.</p>
              <p>Each column should have a type that describes the content within the column. Try to use specific types
                that describe the meaning of the content.</p>
              <p>For example:</p>
              <ul>
                <li>Use <code>CustomerFirstName</code> instead of <code>String</code>
                </li>
                <li>Use <code>TransactionDate</code> instead of <code>Date</code>
                </li>
              </ul>
            </div>
          </div>
          <div class="grid-wrapper" *ngIf="tableMetadata">
            <div class="toolbar">
              <button mat-stroked-button [disabled]="tableSpecFormGroup.invalid || schemaGenerationWorking"
                      (click)="doGenerateSchema()">Auto generate schema
              </button>
              <span *ngIf="schemaGenerationWorking">Generating schema...</span>
              <mat-progress-bar mode="indeterminate" *ngIf="schemaGenerationWorking"></mat-progress-bar>
            </div>
            <ag-grid-angular
              #agGrid
              id="myGrid"
              class="ag-theme-alpine"
              tooltipShowDelay="250"
              tooltipMouseTrack="true"
              domLayout="autoHeight"
              [rowData]="tableMetadata.columns"
              [columnDefs]="columnDefs"
              [singleClickEdit]="true"
              [defaultColDef]="defaultColDef"
              [frameworkComponents]="frameworkComponents"
              (gridReady)="onGridReady($event)"
            ></ag-grid-angular>
          </div>
        </div>

      </div>
    </div>

    <div class="error-message-box" *ngIf="errorMessage">
      {{errorMessage}}
    </div>
    <div class="toolbar">
      <button *ngIf="tableMetadata && tableMetadata.mappedType " mat-flat-button color="warn"
              [disabled]="saveSchemaWorking"
              (click)="doRemoveMapping()">Remove mapping to {{ tableMetadata.mappedType.shortDisplayName }}</button>
      <button mat-flat-button color="primary" (click)="doSave()"
              [disabled]="tableSpecFormGroup.invalid || saveSchemaWorking">Save
      </button>
      <mat-progress-bar mode="indeterminate" *ngIf="saveSchemaWorking"></mat-progress-bar>
    </div>
  `,
  styleUrls: ['./table-importer.component.scss']
})
export class TableImporterComponent {

  constructor(private dialogService: MatDialog) {
  }

  @Input()
  schemaGenerationWorking = false;

  tableMetadata: TableMetadata;
  private metadataSubscription: Subscription;
  private _tableMetadata$: Observable<TableMetadata>;

  tableSpecFormGroup = new FormGroup({
    typeName: new FormControl(null, [Validators.required, validTypeName()]),
    namespace: new FormControl(null, [validNamespace()]),
  });

  @Input()
  errorMessage: string;

  @Input()
  saveSchemaWorking = false;

  @Input()
  table: JdbcTable;

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
      let typeName: string;
      let namespace: string;
      if (this.tableMetadata.mappedType) {
        typeName = this.tableMetadata.mappedType.name;
        namespace = this.tableMetadata.mappedType.namespace;
      } else {
        typeName = capitalizeFirstLetter(this.tableMetadata.tableName);
        namespace = [this.tableMetadata.connectionName, this.tableMetadata.schemaName]
          .filter(s => !isNullOrUndefined(s) && s.length > 0)
          .join('.');
      }
      this.tableSpecFormGroup.setValue({
        'typeName': typeName,
        'namespace': namespace
      });

      if (this.gridApi) {
        this.gridApi.setRowData(this.tableMetadata.columns);
      }
    });

  }

  @Input()
  schema: Schema;

  @Input()
  newTypes: Type[];

  @Output()
  generateSchema = new EventEmitter<NewTypeSpec>();

  @Output()
  save = new EventEmitter<TableModelMapping>();

  @Output()
  removeMapping = new EventEmitter<void>();

  gridApi: GridApi;

  columnDefs: ColDef[] = [
    {headerName: 'Column name', field: 'columnSpec.columnName', cellClass: 'read-only-cell'},
    {headerName: 'Database type', field: 'columnSpec.dataType', cellClass: 'read-only-cell'},
    {
      headerName: 'Optional', field: 'columnSpec.nullable', editable: false,
      cellEditor: 'checkboxEditor',
      cellRenderer: 'checkboxEditor'
      , cellClass: 'read-only-cell'
    },
    {
      cellClass: 'editable-cell',
      headerName: 'Taxonomy type',
      editable: true,
      cellEditor: 'typePicker',
      cellEditorParams: {
        schema: () => {
          // Passing a value directly doesn't seem to work, but the closure does.
          // Must be more js 'this' madness
          return this.schema;
        }
      },
      valueFormatter: (params: ValueFormatterParams) => {
        const col = params.data as ColumnMapping;
        return col.typeSpec ? col.typeSpec.typeName.shortDisplayName : null;
      },
      valueSetter: (params: ValueSetterParams) => {
        const col = params.data as ColumnMapping;
        // It seems that when we hit this line, the value is already updated.
        // Not sure how - perhaps in the cell editor?
        col.typeSpec = params.newValue.typeSpec;
        return true;
      },
      valueGetter: (params: ValueGetterParams) => {
        const col = params.data as ColumnMapping;
        return col.typeSpec ? col.typeSpec.typeName.shortDisplayName : null;
      },
      tooltipValueGetter: (params) => {
        const column = (params.data as ColumnMapping);
        return column.typeSpec ? column.typeSpec.typeName.longDisplayName : 'No type mapped';
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

  doSave() {
    const typeSpec = this.buildTypeSpec(this.tableSpecFormGroup.getRawValue());
    this.save.emit({
      typeSpec,
      tableMetadata: this.tableMetadata
    });
  }

  doGenerateSchema() {
    const typeSpec = this.buildTypeSpec(this.tableSpecFormGroup.getRawValue());
    this.generateSchema.emit(typeSpec);
  }

  doRemoveMapping() {
    this.dialogService.open(
      ConfirmationDialogComponent,
      {
        data: new ConfirmationParams(
          'Delete mapping?',
          // tslint:disable-next-line:max-line-length
          `This will remove the mapping between table ${this.tableMetadata.tableName} and type ${this.tableMetadata.mappedType.longDisplayName}.  The type is not removed, only the mapping between the type and table.`
        )
      }
    ).afterClosed().subscribe((result: ConfirmationAction) => {
      if (result === 'OK') {
        this.removeMapping.emit();
      }
    });
  }

  private buildTypeSpec(formData: any): NewTypeSpec {
    const rawValue = formData;
    const typeSpec = new NewTypeSpec();
    typeSpec.namespace = rawValue.namespace;
    typeSpec.typeName = rawValue.typeName;
    typeSpec.inheritsFrom = rawValue.inheritsFrom;
    typeSpec.typeDoc = rawValue.typeDoc;
    return typeSpec;
  }

}
