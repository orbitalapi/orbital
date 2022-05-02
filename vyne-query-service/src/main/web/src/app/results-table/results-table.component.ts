import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output} from '@angular/core';
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
import {BaseTypedInstanceViewer, unwrapValue} from '../object-view/BaseTypedInstanceViewer';
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
import {GridApi} from 'ag-grid-community/dist/lib/gridApi';
import {Observable} from 'rxjs';
import {Subscription} from 'rxjs';
import {ValueWithTypeName} from '../services/models';
import * as moment from 'moment';
import {buffer, bufferTime} from 'rxjs/operators';
import {isScalar} from "../object-view/object-view.component";

@Component({

  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-results-table',
  template: `
    <ag-grid-angular
      class="ag-theme-alpine"
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

  constructor(private service: CaskService, private changeDetector: ChangeDetectorRef) {
    super();
  }

  private gridApi: GridApi;

  private _instances$: Observable<InstanceLike>;
  private _instanceSubscription: Subscription;

  columnDefs = [];

  @Output()
  instanceClicked = new EventEmitter<InstanceSelectedEvent>();

  @Input()
    // eslint-disable-next-line @typescript-eslint/no-inferrable-types
  selectable: boolean = true;

  // Need a reference to the rowData as well as the subscripton.
  // rowData provides a persistent copy of the rows we've received.
  // It's maintained by the parent container.  This component doesn't modify it.
  // We need the subscription as ag grid expects changes made after rowDAta is set
  // to be done by calling a method.
  @Input()
  rowData: ReadonlyArray<InstanceLike> = [];

  remeasure() {
    if (this.gridApi) {
      console.log('Resize ag Grid columns to fit');
      this.gridApi.sizeColumnsToFit();
    } else {
      console.warn('Called remeasure, but gridApi not available yet');
    }

  }

  downloadAsCsvFromGrid() {
    if (this.gridApi) {
      const exportFileName = moment(new Date()).format('YYYY-MM-DD-HH_mm_SSS');
      const csvParams = {
        skipHeader: false,
        skipFooters: true,
        skipGroups: true,
        fileName: `query-${exportFileName}.csv`
      };
      this.gridApi.exportDataAsCsv(csvParams);
    }
  }

  @Input()
  get instances$(): Observable<InstanceLike> {
    return this._instances$;
  }

  set instances$(value: Observable<InstanceLike>) {
    if (value === this._instances$) {
      return;
    }
    this._instances$ = value;
    this.resetGrid();
    this.subscribeForData();

  }

  private subscribeForData() {
    if (!this.gridApi || !this._instances$) {
      // Don't subscribe until the grid is ready to receive data, and we have an observable
      return;
    }
    this.unsubscribeAllNow();
    this.unsubscribeOnClose(this.instances$
      .pipe(
        bufferTime(500)
      )
      .subscribe((next) => {
        if (this.columnDefs.length === 0) {
          if (next.length > 0) {
            this.rebuildGridData(next[0]);
          }

        }

        if (this.gridApi) {

          this.gridApi.applyTransaction({
            add: next
          });
        } else {
          console.error('Received an instance before the grid was ready - this record batch will get dropped!');
        }
      }));
  }

  @Input()
  get type(): Type {
    return this._type;
  }

  set type(value: Type) {
    if (value === this._type) {
      return;
    }
    this._type = value;
  }

  protected onSchemaChanged() {
    super.onSchemaChanged();
  }

  private rebuildGridData(value: InstanceLike) {
    this.buildColumnDefinitions(value);
    this.changeDetector.markForCheck();
  }

  /**
   * Builds columns from a value.  The attributes
   * present will be used to determine column names.
   */
  private buildColumnDefinitions(value: InstanceLike) {
    const instanceValue = unwrapValue(value);
    const scalar = isScalar(instanceValue);
    if (scalar) {
      this.columnDefs = [{
        headerName: 'Result',
        flex: 1,
        // headerComponentFramework: TypeInfoHeaderComponent,
        // headerComponentParams: {
        //   fieldName: this.type.name.shortDisplayName,
        //   typeName: this.type.name
        // },
        // This was commented out.  It broke display of scalar values when running from the query builder.
        valueGetter: (params: ValueGetterParams) => {
          return params.data.value;
        }
      }];
    } else {
      const attributeNames = Object.keys(instanceValue);
      const columnDefinitions = attributeNames.map((fieldName, index) => {
        const cellRenderer = (fieldName === 'caskMessageId') ? this.downloadLinkRender() : null;
        return {
          resizable: true,
          headerName: fieldName,
          field: fieldName,
          valueGetter: (params: ValueGetterParams) => {
            return this.unwrap(params.data, fieldName);
          },
          cellRenderer: cellRenderer
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
    } else if (Array.isArray(instance)) {
      return 'View collections in tree mode';
    } else if (typeof instance === 'object' && instance != null) {
      return 'View nested structures in tree mode';
    } else {
      return instance;
    }
  }

  onCellClicked($event: CellClickedEvent) {
    const rowInstance: ValueWithTypeName = $event.data;
    const cellInstance = this.unwrap(rowInstance, $event.colDef.field);
    const untypedCellInstance: UntypedInstance = {
      value: cellInstance,
      type: UnknownType.UnknownType,
      nearestType: this.getTypeForAttribute($event.colDef.field)
    };
    this.instanceClicked.emit(new InstanceSelectedEvent(
      untypedCellInstance, null, rowInstance.valueId, $event.colDef.field, rowInstance.queryId));
  }

  onGridReady(event: GridReadyEvent) {
    this.gridApi = event.api;
    this.resetGrid();
    this.subscribeForData();
  }

  onFirstDataRendered(params: FirstDataRenderedEvent) {
    const colIds = params.columnApi.getAllColumns().map(c => c.getId());
    params.columnApi.autoSizeColumns(colIds);
  }

  private resetGrid() {
    if (this.gridApi) {
      this.columnDefs = [];
      this.gridApi.setColumnDefs([]);
      this.gridApi.setRowData([]);
    }
  }
}
