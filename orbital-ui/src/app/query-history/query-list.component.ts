import {Component, EventEmitter, Input, Output} from '@angular/core';
import {QueryHistorySummary} from '../services/query.service';
import {RunningQueryStatus} from '../services/active-queries-notification-service';
import {HttpRequestState} from 'ngx-http-request-state';

@Component({
  selector: 'app-query-list',
  template: `
    <div class="list-container">
      <app-active-query-card
        *ngFor="let record of activeQueries | keyvalue; trackBy: queryId"
        [queryStatus]="record.value"
        [routerLink]="'/query-history/' + record.key"
        routerLinkActive="selected-history-list-item"
        (cancel)="cancelActiveQuery.emit(record.value)"
      ></app-active-query-card>
      <ng-container *ngIf="historyRecords">
        <!-- Show a spinner if state is loading -->
        <progress
          max="100"
          tuiProgressBar
          size='xs'
          new
          *ngIf='historyRecords.isLoading'
        ></progress>
        <!-- Show the data if state is loaded -->
        <div *ngIf="historyRecords.value?.length !== 0" class="inner-list-container">
          <app-query-history-card
            *ngFor="let historyRecord of historyRecords.value"
            [routerLink]="'/query-history/' + historyRecord.queryId"
            routerLinkActive="selected-history-list-item"
            [historyRecord]="historyRecord">
          </app-query-history-card>
        </div>
        <tui-notification *ngIf='historyRecords.value?.length === 0'>
          No queries have been run yet
        </tui-notification>
        <!-- Show an error message if state is error -->
        <tui-notification *ngIf='historyRecords.error' status='error'>
          {{historyRecords.error.message}}
        </tui-notification>
      </ng-container>
    </div>
  `,
  styleUrls: ['./query-list.component.scss']
})
export class QueryListComponent {

  @Input()
  historyRecords: HttpRequestState<QueryHistorySummary[]>;

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

  protected readonly JSON = JSON;
}
