import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Field, Schema, Type, TypedInstance} from '../services/schema';
import {InstanceLike, InstanceLikeOrCollection} from '../object-view/object-view.component';
import {BaseTypedInstanceViewer} from '../object-view/BaseTypedInstanceViewer';
import {isTypedInstance, isTypeNamedInstance, TypeNamedInstance} from '../services/query.service';
import {CellClickedEvent, ValueGetterParams} from 'ag-grid-community';

@Component({
  selector: 'app-results-table',
  templateUrl: './results-table.component.html',
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
    return super.instance;
  }

  set instance(value: InstanceLikeOrCollection) {
    if (value === super._instance) {
      return;
    }
    super.instance = value;
    this.rebuildGridData();
  }

  @Input()
  get type(): Type {
    return super.type;
  }

  set type(value: Type) {
    // Comparing against the private field, as calling
    // the getter can trigger us to derive the type, which we
    // don't want to do right now.
    if (value === this._type) {
      return;
    }
    super.type = value;
    this.rebuildGridData();
  }

  columnDefs = [
    // {headerName: 'Make', field: 'make'},
    // {headerName: 'Model', field: 'model'},
    // {headerName: 'Price', field: 'price'}
  ];

  rowData = [
    // {make: 'Toyota', model: 'Celica', price: 35000},
    // {make: 'Ford', model: 'Mondeo', price: 32000},
    // {make: 'Porsche', model: 'Boxter', price: 72000}
  ];


  private rebuildGridData() {
    if (!this.type || !this.instance) {
      this.columnDefs = [];
      this.rowData = [];
      return;
    }

    const attributeNames = this.getAttributes(this.type);
    this.columnDefs = attributeNames.map(fieldName => {
      return {
        headerName: fieldName,
        field: fieldName,
        valueGetter: (params: ValueGetterParams) => {
          return this.unwrap(params.data[fieldName]);
        }
      };
    });

    const collection = (this.isArray) ? this.instance as InstanceLike[] : [this.instance];
    if (collection.length === 0) {
      this.rowData = [];
    } else {
      if (isTypeNamedInstance(collection[0])) {
        this.rowData = collection.map((instance: TypeNamedInstance) => instance.value);
      } else if (isTypedInstance(collection[0])) {
        this.rowData = collection.map((instance: TypedInstance) => instance.value);
      } else {
        this.rowData = collection;
      }
    }
  }

  private getAttributes(type: Type): string[] {
    const itemType = (type.collectionType !== null
      && type.collectionType !== undefined) ?
      type.collectionType
      : type;
    return Object.keys(itemType.attributes);
  }

  private unwrap(instance: any): any {
    if (isTypedInstance(instance)) {
      return instance.value;
    } else if (isTypeNamedInstance(instance)) {
      return instance.value;
    } else {
      return instance;
    }
  }

  onCellClicked($event: CellClickedEvent) {
    const instance = $event.data[$event.colDef.field];
    if (isTypeNamedInstance(instance)) {
      this.instanceClicked.emit(instance);
    } else if (isTypedInstance(instance)) {
      this.instanceClicked.emit(instance);
    } else {
      console.log('Item clicked didn\'t have type data, so not emitting event');
    }
  }
}
