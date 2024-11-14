import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  Output
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {fromEventPattern, Observable} from 'rxjs';
import {Subscription} from 'rxjs';
import {bufferTime, filter, map} from 'rxjs/operators';
import * as moment from 'moment';
import {
  AgGridEvent,
  CellClickedEvent,
  ColDef,
  GridReadyEvent,
  ValueGetterParams,
  ColumnMovedEvent, ColumnResizedEvent, GridApi, FilterChangedEvent
} from 'ag-grid-community';
import {AppConfig, AppInfoService} from '../services/app-info.service';
import {QueryEditorStoreService} from '../services/query-editor-store.service';
import {
  InstanceLike,
  isTypedInstance,
  isTypedNull,
  isTypeNamedInstance,
  isTypeNamedNull,
  Type,
  UnknownType,
  UntypedInstance
} from '../services/schema';
import {BaseTypedInstanceViewer, unwrapValue} from '../object-view/BaseTypedInstanceViewer';
import {InstanceSelectedEvent} from '../query-panel/instance-selected-event';
import {ValueWithTypeName} from '../services/models';
import {isScalar} from "../object-view/object-view.component";
import {isNullOrUndefined} from '../utils/utils';

// Define a union of the event types we're interested in persisting,
// unfortunately ag-grid doesn't expose these in a type safe way
type AgGridEventTypes = 'sortChanged' | 'columnResized' | 'columnMoved' | 'filterChanged';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-results-table',
  template: `
    <ag-grid-angular
      class="ag-theme-alpine"
      [enableCellTextSelection]="true"
      [columnDefs]="columnDefs"
      [pagination]="true"
      [paginationPageSize]="paginationPageSize"
      [suppressDragLeaveHidesColumns]="true"
      (gridReady)="onGridReady($event)"
      (rowDataUpdated)="onRowDataUpdated()"
      (cellClicked)="onCellClicked($event)"
    >
    </ag-grid-angular>
  `,
  styleUrls: ['./results-table.component.scss']
})
export class ResultsTableComponent extends BaseTypedInstanceViewer {
  @Input()
  get instances$(): Observable<InstanceLike> {
    return this._instances$;
  }

  set instances$(value: Observable<InstanceLike>) {
    if (value === this._instances$) {
      return;
    }
    this._instances$ = value;
    this.hasFirstData = false;
    this.resetGrid();
    this.subscribeForData();
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

  @Input()
  selectable: boolean = true;

  // Need a reference to the rowData as well as the subscripton.
  // rowData provides a persistent copy of the rows we've received.
  // It's maintained by the parent container.  This component doesn't modify it.
  // We need the subscription as ag grid expects changes made after rowDAta is set
  // to be done by calling a method.
  // TODO: don't think we need this anymore...? Double check with Marty, I must be missing something!!??
  @Input()
  rowData: ReadonlyArray<InstanceLike> = [];

  @Input()
  isStreamingQuery: boolean;

  @Output()
  instanceClicked = new EventEmitter<InstanceSelectedEvent>();

  config: AppConfig;
  private gridApi: GridApi;

  private _instances$: Observable<InstanceLike>;
  private _instanceSubscription: Subscription;

  columnDefs: ColDef[] = [];
  paginationPageSize = 100;
  private hasFirstData: boolean;

  constructor(
    private appInfoService: AppInfoService,
    private queryEditorStore: QueryEditorStoreService,
    private changeDetector: ChangeDetectorRef,
    private destroyRef: DestroyRef
  ) {
    super();
    appInfoService.getConfig()
      .subscribe(next => this.config = next);
  }

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

  private subscribeForData() {
    if (!this.gridApi || !this._instances$) {
      // Don't subscribe until the grid is ready to receive data, and we have an observable
      return;
    }
    this.unsubscribeAllNow();
    this.unsubscribeOnClose(this.instances$
      // TODO: might need this approach in the other components as well...
      .pipe(
        bufferTime(500),
        filter(arr => arr.length > 0),
        map(buffer => this.isStreamingQuery ? buffer.reverse() : buffer)
      )
      .subscribe((next) => {
        if (this.columnDefs.length === 0) {
          if (next.length > 0) {
            this.rebuildGridData(next[0]);
          }
        }

        if (this.gridApi) {
          this.gridApi.applyTransactionAsync({
            add: next,
            addIndex: this.isStreamingQuery ? 0 : null
          });
        } else {
          console.error('Received an instance before the grid was ready - this record batch will get dropped!');
        }
      }));
  }

  protected onSchemaChanged() {
    super.onSchemaChanged();
  }

  private rebuildGridData(value: InstanceLike) {
    this.gridApi.setGridOption('suppressColumnMoveAnimation', true)
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
        valueGetter: (params: ValueGetterParams) => {
          return params.data.value;
        }
      }];
    } else {
      const attributeNames = Object.keys(instanceValue);
      const columnDefinitions: ColDef[] = attributeNames.map((fieldName, index) => {
        const filter = typeof instanceValue[fieldName] === 'number' ?
          'agNumberColumnFilter' :
          this.isValidDate(instanceValue[fieldName]) ?
            'agDateColumnFilter' :
            'agTextColumnFilter';
        return {
          resizable: true,
          headerName: fieldName,
          field: fieldName,
          valueGetter: (params: ValueGetterParams) => {
            return this.unwrap(params.data, fieldName);
          },
          filter,
          sortable: true
        };
      });
      this.columnDefs = columnDefinitions;
    }
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
    } else if (typeof instance === 'object' && instance !== null) {
      return 'View nested structures in tree mode';
    } else {
      return typeof instance !== 'number' && this.isValidDate(instance) ? new Date(instance) : instance
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

  // NOTE: using this approach instead of firstDataRendered event as when the user re-runs
  //       a query from the same tab, the firstDataRendered event doesn't trigger.
  onRowDataUpdated() {
    if (!this.hasFirstData && this.gridApi.getColumnDefs().length) {
      this.hasFirstData = true;
      this.gridApi.autoSizeAllColumns();
      this.reinstateColumnState();
      this.registerColumnEventHandlers()
    }
  }

  private registerColumnEventHandlers() {
    const eventObservable = fromEventPattern<[AgGridEventTypes, AgGridEvent]>(
      // Register event listener
      (handler) => this.gridApi.addGlobalListener(handler),
      // Deregister event listener
      (handler) => !this.gridApi.isDestroyed() ? this.gridApi.removeGlobalListener(handler) : null
    );
    eventObservable.pipe(
      takeUntilDestroyed(this.destroyRef),
      // Filter for relevant event types
      filter(([eventType, event]) =>
        eventType === 'sortChanged' ||
        eventType === 'columnResized' && (event as ColumnResizedEvent).finished && (event as ColumnResizedEvent).source !== 'autosizeColumns' ||
        eventType === 'columnMoved' && (event as ColumnMovedEvent).finished ||
        eventType === 'filterChanged' && (event as FilterChangedEvent).source !== 'api'
      ),
    ).subscribe(([eventType, event]) => {
      console.log('persist state!!', eventType)
      const filterModel = this.gridApi.getFilterModel();
      const columnState = this.gridApi.getColumnState()
      // doing this instead of prop drilling through 5 levels of components...
      this.queryEditorStore.updateAgGridColumnState({columnState, filterModel})
    });
  }

  private resetGrid() {
    if (this.gridApi) {
      this.columnDefs = [];
      this.gridApi.setGridOption("columnDefs", []);
      this.gridApi.setGridOption("rowData", []);
    }
  }

  private isValidDate(dateString) {
    if (!dateString) return false;
    const formats = [
      "YYYY-MM-DD",
      "YYYY/MM/DD",
      "DD-MM-YYYY",
      "DD/MM/YYYY",
      "MM-DD-YYYY",
      "MM/DD/YYYY",
      "YYYY-MM-DDTHH:mm:ss.sssZ", // ISO 8601 with time (including ms) and timezone
      "YYYY-MM-DDTHH:mm:ss.sss", // ISO 8601 with time (including ms)
      "YYYY-MM-DDTHH:mm:ssZ", // ISO 8601 with time and timezone
      "YYYY-MM-DDTHH:mm:ss",  // ISO 8601 with time
    ];
    const isDate = moment(dateString, formats, true).isValid();
    return isDate;
  }

  private reinstateColumnState() {
    // reinstate column filters/state if available
    const {filterModel, columnState} = this.queryEditorStore.activeQueryEditorState().payload.agGridColumnState() ?? {}
    if (!isNullOrUndefined(filterModel)) {
      this.gridApi.setFilterModel(filterModel)
    }
    if (!isNullOrUndefined(columnState)) {
      this.gridApi.applyColumnState({ state: columnState, applyOrder: true })
    }
    this.gridApi.setGridOption('suppressColumnMoveAnimation', false)
  }

}
