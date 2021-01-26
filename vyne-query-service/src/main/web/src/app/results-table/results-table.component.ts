import {Component, EventEmitter, Input, Output} from '@angular/core';
import {
  InstanceLike,
  InstanceLikeOrCollection,
  isTypedInstance,
  isTypedNull,
  isTypeNamedInstance,
  isTypeNamedNull,
  Type,
  UnknownType,
  UntypedInstance
} from '../services/schema';
import {BaseTypedInstanceViewer} from '../object-view/BaseTypedInstanceViewer';
import {
  CellClickedEvent,
  FirstDataRenderedEvent,
  GridReadyEvent,
  ICellRendererFunc,
  ValueGetterParams
} from 'ag-grid-community';
import {TypeInfoHeaderComponent} from './type-info-header.component';
import {InstanceSelectedEvent} from '../query-panel/instance-selected-event';
import {isNullOrUndefined} from 'util';
import {CaskService} from '../services/cask.service';

@Component({
  selector: 'app-results-table',
  template: `
    <ag-grid-angular
      class="ag-theme-alpine"
      headerHeight="65"
      [enableCellTextSelection]="true"
      [rowData]="rowData"
      [columnDefs]="columnDefs"
      (gridReady)="onGridReady($event)"
      (firstDataRendered)="onFirstDataRendered($event)"
      (cellClicked)="onCellClicked($event)"
    >
    </ag-grid-angular>
  `,
  styleUrls: ['./results-table.component.scss']
})
export class ResultsTableComponent extends BaseTypedInstanceViewer {

  constructor(private service: CaskService) {
    super();
  }

  @Output()
  instanceClicked = new EventEmitter<InstanceSelectedEvent>();

  @Input()
    // tslint:disable-next-line:no-inferrable-types
  selectable: boolean = true;

  @Input()
  get instance(): InstanceLikeOrCollection {
    return this._instance;
  }

  set instance(value: InstanceLikeOrCollection) {
    if (value === this._instance) {
      return;
    }
    this._instance = value;
    this.rebuildGridData();
  }


  @Input()
  get type(): Type {
    return super['type'];
  }

  set type(value: Type) {
    // Comparing against the private field, as calling
    // the getter can trigger us to derive the type, which we
    // don't want to do right now.
    if (value === this._type) {
      return;
    }
    this._type = value;
    this.rebuildGridData();
  }

  columnDefs = [];

  rowData = [];

  protected onSchemaChanged() {
    super.onSchemaChanged();
    this.rebuildGridData();
  }

  private rebuildGridData() {
    if (!this.type || !this.instance) {
      this.columnDefs = [];
      this.rowData = [];
      return;
    }

    this.buildColumnDefinitions();

    const collection = (this.isArray) ? this.instance as InstanceLike[] : [this.instance];
    if (collection.length === 0) {
      this.rowData = [];
    } else {
      this.rowData = collection;
    }
  }

  private buildColumnDefinitions() {
    if (this.type.isScalar) {
      this.columnDefs = [{
        headerName: this.type.name.shortDisplayName,
        flex: 1,
        headerComponentFramework: TypeInfoHeaderComponent,
        headerComponentParams: {
          fieldName: this.type.name.shortDisplayName,
          typeName: this.type.name
        },
        valueGetter: (params: ValueGetterParams) => {
          return this.unwrap(this.instance, null);
        }
      }];
    } else {
      const attributeNames = this.getAttributes(this.type);
      const caskMessageIdFieldName = 'caskMessageId';
      const columnDefinitions = attributeNames.map((fieldName, index) => {
        const lastColumn = index === attributeNames.length - 1;
        return fieldName !== caskMessageIdFieldName ? {
            resizable: true,
            headerName: fieldName,
            field: fieldName,
            // flex: (lastColumn) ? 1 : null,
            headerComponentFramework: TypeInfoHeaderComponent,
            headerComponentParams: {
              fieldName: fieldName,
              typeName: this.getTypeForAttribute(fieldName).name
            },
            valueGetter: (params: ValueGetterParams) => {
              return this.unwrap(params.data, fieldName);
            }
          } :
          {
            resizable: true,
            headerName: fieldName,
            field: fieldName,
            // flex: (lastColumn) ? 1 : null,
            headerComponentFramework: TypeInfoHeaderComponent,
            headerComponentParams: {
              fieldName: fieldName,
              typeName: this.getTypeForAttribute(fieldName).name
            },
            valueGetter: (params: ValueGetterParams) => {
              return this.unwrap(params.data, fieldName);
            },
            cellRenderer: this.downloadLinkRender()
          };
      });
      this.columnDefs = columnDefinitions;
    }
  }

  private downloadLinkRender(): ICellRendererFunc {
    const urlFunc = this.service.downloadIngestedMessageUrl;
    return function (params) {
      const keyData = params && params.data && params.data.caskMessageId ? params.data.caskMessageId : '';
      const newLink = `<a href="${urlFunc(keyData)}" target="_blank">${keyData}</a>`;
      return newLink;
    };
  }

  private getAttributes(type: Type): string[] {
    const itemType = (type.collectionType !== null
      && type.collectionType !== undefined) ?
      type.collectionType
      : type;
    return Object.keys(itemType.attributes);
  }

  private unwrap(instance: any, fieldName: string | null): any {
    if (isTypedInstance(instance) || isTypeNamedInstance(instance)) {
      const object = instance.value;
      if (fieldName === null) {
        return object;
      } else {
        return this.unwrap(object[fieldName], null);
      }
    } else if (isTypedNull(instance) || isTypeNamedNull(instance)) {
      return null;
    } else if (fieldName !== null) {
      const fieldValue = instance[fieldName];
      if (isNullOrUndefined(fieldValue)) {
        return null;
      } else {
        // In the operation explorer, we can end up with typed instances
        // at the field level, even if the top-level object isn't
        // a typed instance.  We should fix that, but for now, just unwrap
        return this.unwrap(instance[fieldName], null);
      }
    } else {
      return instance;
    }
  }

  onCellClicked($event: CellClickedEvent) {
    const rowInstance: InstanceLike = $event.data;
    const nodeId = this.isArray ? `[${$event.rowIndex}].${$event.colDef.field}` : $event.colDef.field;
    const cellInstance = this.unwrap(rowInstance, $event.colDef.field);
    const untypedCellInstance: UntypedInstance = {
      value: cellInstance,
      type: UnknownType.UnknownType,
      nearestType: this.getTypeForAttribute($event.colDef.field)
    };
    this.instanceClicked.emit(new InstanceSelectedEvent(untypedCellInstance, null, nodeId));
  }

  onGridReady(event: GridReadyEvent) {
   // event.api.sizeColumnsToFit();
  }

  onFirstDataRendered(params: FirstDataRenderedEvent) {
    const colIds = params.columnApi.getAllColumns().map(c => c.getId());
    params.columnApi.autoSizeColumns(colIds);
  }
}
