import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {JdbcTable, TableColumn, TableMetadata, TableModelMapping} from './db-importer.service';
import {AgGridColumnDefinitions} from '../data-explorer/csv-viewer.component';
import {ColDef, ValueGetterParams} from 'ag-grid-community';
import {QualifiedName, Schema, Type} from '../services/schema';
import {ColumnApi} from 'ag-grid-community/dist/lib/columnController/columnApi';
import {TypeSelectorCellEditorComponent} from './type-selector-cell-editor.component';
import {CheckboxCellEditorComponent} from './checkbox-cell-editor.component';
import {Observable} from 'rxjs/internal/Observable';
import {Subscription} from 'rxjs';
import {GridApi} from 'ag-grid-community/dist/lib/gridApi';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {validNamespace, validTypeName} from '../services/validators';
import {NewTypeSpec} from '../type-editor/type-editor.component';

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
                  <pre>Customer</pre>
                </li>
                <li>
                  <pre>FirstName</pre>
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
                  <pre>com.acme.customers</pre>
                </li>
              </ul>
              <ul>
                <li>
                  <pre>com.acme.invoicing.invoicePlatform</pre>
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
      <button mat-flat-button color="primary" (click)="doSave()"
              [disabled]="tableSpecFormGroup.invalid || saveSchemaWorking">Save
      </button>
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
  generateSchema = new EventEmitter<NewTypeSpec>();

  @Output()
  save = new EventEmitter<TableModelMapping>();

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
