import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Field, Schema, Type, TypedInstance} from '../services/schema';
import {InstanceLike, InstanceLikeOrCollection} from '../object-view/object-view.component';
import {BaseTypedInstanceViewer} from '../object-view/BaseTypedInstanceViewer';
import {
  isTypedInstance,
  isTypedNull,
  isTypeNamedInstance,
  isTypeNamedNull,
  TypeNamedInstance
} from '../services/query.service';
import {CellClickedEvent, GridReadyEvent, ValueGetterParams} from 'ag-grid-community';
import {AgGridColumn} from 'ag-grid-angular';
import {TypeInfoHeaderComponent} from './type-info-header.component';

@Component({
  selector: 'app-results-table',
  template: `
    <ag-grid-angular
      class="ag-theme-alpine"
      headerHeight="65"
      [rowData]="rowData"
      [columnDefs]="columnDefs"
      (gridReady)="onGridReady($event)"
      (cellClicked)="onCellClicked($event)"
    >
    </ag-grid-angular>
  `,
  styleUrls: ['./results-table.component.scss']
})
export class ResultsTableComponent extends BaseTypedInstanceViewer {

  @Output()
  instanceClicked = new EventEmitter<InstanceLike>();

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

  private rebuildGridData() {
    if (!this.type || !this.instance) {
      this.columnDefs = [];
      this.rowData = [];
      return;
    }

    const attributeNames = this.getAttributes(this.type);
    this.columnDefs = attributeNames.map((fieldName, index) => {
      const lastColumn = index === attributeNames.length - 1;

      return {
        headerName: fieldName,
        field: fieldName,
        flex: (lastColumn) ? 1 : null,
        headerComponentFramework: TypeInfoHeaderComponent,
        headerComponentParams: {
          fieldName: fieldName,
          typeName: this.type.attributes[fieldName].type
        },
        valueGetter: (params: ValueGetterParams) => {
          return this.unwrap(params.data, fieldName);
        }
      };
    });

    const collection = (this.isArray) ? this.instance as InstanceLike[] : [this.instance];
    if (collection.length === 0) {
      this.rowData = [];
    } else {
      this.rowData = collection;
    }
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
    } else {
      return instance;
    }
  }

  onCellClicked($event: CellClickedEvent) {
    const rowInstance: InstanceLike = $event.data;
    if (isTypeNamedInstance(rowInstance) || isTypeNamedInstance(rowInstance)) {
      const cellInstance = rowInstance.value[$event.colDef.field];
      this.instanceClicked.emit(cellInstance);
    } else if (isTypeNamedNull(rowInstance) || isTypedNull(rowInstance)) {
      // TODO  :Suspect we may have to handle typed-nulls differently
      const cellInstance = rowInstance.value[$event.colDef.field];
      this.instanceClicked.emit(cellInstance);
    } else {
      console.log('Item clicked didn\'t have type data, so not emitting event');
    }
  }

  onGridReady(event: GridReadyEvent) {
    event.api.setGridAutoHeight(true);
  }
}
