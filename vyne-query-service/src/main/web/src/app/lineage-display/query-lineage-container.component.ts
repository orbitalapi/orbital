import {Component, Input, OnInit} from '@angular/core';
import {QuerySankeyChartRow, QueryService} from '../services/query.service';
import {isNullOrUndefined} from 'util';
import {Observable} from 'rxjs/internal/Observable';

@Component({
  selector: 'app-query-lineage-container',
  template: `
    <p>
      query-lineage-container works!
    </p>
  `,
  styleUrls: ['./query-lineage-container.component.scss']
})
export class QueryLineageContainerComponent {
  sankeyChartRows: QuerySankeyChartRow[] = [];

  private _queryId: string;
  get queryId(): string {
    return this._queryId;
  }

  @Input()
  set queryId(value: string) {
    if (value === this._queryId) {
      return;
    }
    this._queryId = value;
    this.loadData();
  }

  private _clientQueryId: string;
  get clientQueryId(): string {
    return this._clientQueryId;
  }

  @Input()
  set clientQueryId(value: string) {
    if (value === this._clientQueryId) {
      return;
    }
    this._clientQueryId = value;
    this.loadData();
  }

  constructor(private queryService: QueryService) {
  }


  private loadData() {
    let observable: Observable<QuerySankeyChartRow[]>;
    if (!isNullOrUndefined(this.clientQueryId)) {
      observable = this.queryService.getQuerySankeyChartDataFromClientId(this.clientQueryId);
    } else if (!isNullOrUndefined(this.queryId)) {
      observable = this.queryService.getQuerySankeyChartData(this.queryId);
    }
    observable.subscribe(result => {
      this.sankeyChartRows = result;
    }, error => {
      console.error('Failed to load query sankey data: ' + JSON.stringify(error));
    });
  }
}
