import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {AgGridEvent, ICellRendererFunc, IDatasource, IGetRowsParams} from 'ag-grid-community';
import {CaskService} from '../../../services/cask.service';
import {GridApi} from 'ag-grid-community/dist/lib/gridApi';
import {ColumnApi} from 'ag-grid-community/dist/lib/columnController/columnApi';
import {SearchInput} from './search-input';

@Component({
  selector: 'app-cask-ingestion-errors-grid',
  template: `
    <ag-grid-angular
      #agGrid
      style="width: 100%; height: 100vh;"
      id="caskIngestionErrorsGrid"
      class="ag-theme-alpine"
      [components]="components"
      [columnDefs]="columnDefs"
      [defaultColDef]="defaultColDef"
      [rowBuffer]="rowBuffer"
      [rowSelection]="rowSelection"
      [rowModelType]="rowModelType"
      [paginationPageSize]="paginationPageSize"
      [cacheOverflowSize]="cacheOverflowSize"
      [maxConcurrentDatasourceRequests]="maxConcurrentDatasourceRequests"
      [infiniteInitialRowCount]="infiniteInitialRowCount"
      [maxBlocksInCache]="maxBlocksInCache"
      [rowData]="rowData"
      [blockLoadDebounceMillis]="500"
      (gridReady)="onGridReady($event)"
    ></ag-grid-angular>`,
  styleUrls: ['./cask-ingestion-errors-grid.component.scss']
})
export class CaskIngestionErrorsGridComponent implements OnChanges {
  private gridApi: GridApi;
  private gridColumnApi: ColumnApi;
  columnDefs: any;
  defaultColDef: any;
  rowBuffer: any;
  rowSelection: any;
  rowModelType: any;
  paginationPageSize;
  cacheOverflowSize: number;
  maxConcurrentDatasourceRequests: number;
  infiniteInitialRowCount: number;
  maxBlocksInCache: number;
  rowData: [];
  components: any;

  @Input() searchInput: SearchInput;

  constructor(private service: CaskService) {
    this.components = {
      loadingRenderer: function (params) {
        if (params.value !== undefined) {
          return params.value;
        } else {
          return '<img src="assets/img/loading.gif">';
        }
      },
    };
    this.paginationPageSize = 100;
    this.columnDefs = [
      // this row shows the row index, doesn't use any data from the row
      {
        headerName: 'Id', width: 50,
        // it is important to have node.id here, so that when the id changes (which happens
        // when the row is loaded) then the cell is refreshed.
        valueGetter: 'node.id',
        cellRenderer: 'loadingRenderer'
      },
      {field: 'createdAt', headerName: 'Error Timestamp'},
      {field: 'error', headerName: 'Error'},
      {field: 'fqn', headerName: 'Type'},
      {
        field: 'caskMessageId',
        headerName: 'Cask Message Id',
        cellRenderer: this.downloadLinkRender()
      }
    ];
    this.defaultColDef = {
      flex: 1,
      minWidth: 50,
      resizable: true,
    };
    this.rowModelType = 'infinite';
    this.rowBuffer = 0;
    this.rowSelection = 'multiple';
    this.rowModelType = 'infinite';
    this.cacheOverflowSize = 2;
    this.maxConcurrentDatasourceRequests = 1;
    this.infiniteInitialRowCount = 1000;
    this.maxBlocksInCache = 10;
  }

  private downloadLinkRender(): ICellRendererFunc {
    const urlFunc = this.service.downloadIngestedMessageUrl;
    return function (params) {
      const keyData = params && params.data && params.data.caskMessageId ? params.data.caskMessageId : '';
      const newLink = `<a href="${urlFunc(keyData)}" target="_blank">${keyData}</a>`;
      return newLink;
    };
  }

  private readonly serverSideDataSource = (): IDatasource => {
    const searchStart = new Date(this.searchInput.searchStart);
    searchStart.setUTCHours(0, 0, 0, 0);
    const searchEnd = new Date(this.searchInput.searchEnd);
    searchEnd.setUTCHours(23, 59, 59, 999);
    const tableName = this.searchInput.tableName;
    const caskRestService = this.service;
    const pageSize = this.paginationPageSize;
    return {
      rowCount: null,
      getRows(params: IGetRowsParams) {
        console.log(
          'asking for ' + params.startRow + ' to ' + params.endRow
        );
        const pageNumber = params.startRow / pageSize;
        caskRestService.fetchCaskIngestionErrors(tableName,
          {
            pageNumber: pageNumber,
            pageSize: pageSize,
            searchStart: searchStart.toISOString(),
            searchEnd: searchEnd.toISOString(),
          }).toPromise()
          .then(result => {
            // Under normal operation, you will return null
            // or undefined for lastRow for every time getRows() sis called with the exception of when you get to the last block.
            // For example, if block size is 100 and you have 250 rows, when getRows() is called for the third time,
            // you will return back 50 rows in the result and set rowCount to 250.
            // This will then get the grid to set the scrollbar to fit exactly 250 rows
            // and will not ask for any more blocks.
            const lastRow = (): number => {
              if (result.totalPages <= 1) {
                return result.totalItem;
              } else if (pageNumber < result.totalPages - 1) {
                return null;
              } else {
                return result.totalItem;
              }
            };
            console.log(`total pages ${result.totalPages}, total items ${result.totalItem}`);
            params.successCallback(result.items, lastRow());
          }).catch(error => {
          console.log(`failed to fetch Cask ingestion errors ${error}`);
          params.failCallback();
        });
      }
    };
  }

  onGridReady(params: AgGridEvent) {
    this.gridApi = params.api;
    this.gridColumnApi = params.columnApi;
    this.loadIngestionErrors();
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.loadIngestionErrors();
  }

  loadIngestionErrors() {
    if (this.searchInput && this.gridApi && this.searchInput.searchEnd && this.searchInput.searchStart && this.searchInput.tableName) {
      console.log(`loading ingestion errors between ${this.searchInput.searchStart} and ${this.searchInput.searchEnd}`);
      this.gridApi.setDatasource(this.serverSideDataSource());
    }
  }

  downloadIngestionMessageUrl(caskMessageId: string) {
    return this.service.downloadIngestedMessageUrl(caskMessageId);
  }
}
