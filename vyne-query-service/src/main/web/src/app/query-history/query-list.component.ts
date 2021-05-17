import {Component, EventEmitter, Input, Output} from '@angular/core';
import {QueryHistorySummary} from '../services/query.service';
import {RunningQueryStatus} from '../services/active-queries-notification-service';

@Component({
  selector: 'app-query-list',
  template: `
    <div class="list-container">
      <div *ngFor="let record of activeQueries | keyvalue; trackBy: queryId"
           (click)="activeQuerySelected.emit(record.value)"
           class="history-item">
        <app-active-query-card [queryStatus]="record.value" (cancel)="cancelActiveQuery.emit(record.value)"></app-active-query-card>
      </div>
      <div *ngFor="let historyRecord of historyRecords" (click)="recordSelected.emit(historyRecord)"
           class="history-item">
        <app-query-history-card [historyRecord]="historyRecord"></app-query-history-card>
      </div>
    </div>
  `,
  styleUrls: ['./query-list.component.scss']
})
export class QueryListComponent {

  @Input()
  historyRecords: QueryHistorySummary[];

  @Input()
  activeQueries: Map<string, RunningQueryStatus>;

  @Output()
  recordSelected = new EventEmitter<QueryHistorySummary>();

  @Output()
  activeQuerySelected = new EventEmitter<RunningQueryStatus>();

  @Output()
  cancelActiveQuery = new EventEmitter<RunningQueryStatus>();

  queryId(index: number, item: RunningQueryStatus): string {
    return item.queryId;
  }

}
