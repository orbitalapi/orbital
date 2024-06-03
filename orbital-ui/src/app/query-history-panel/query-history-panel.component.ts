import {Component, EventEmitter, Output} from '@angular/core';
import {QueryHistorySummary} from "../services/query.service";
import {QueryHistoryStoreService} from '../services/query-history-store.service';

@Component({
  selector: 'app-query-history-panel',
  styleUrls: ['./query-history-panel.component.scss'],
  template: `<div>
    <ng-container *ngIf="historyStoreService.history$ | async as history">
      <!-- Show a spinner if state is loading -->
      <progress
        max="100"
        tuiProgressBar
        size='xs'
        new
        *ngIf='history.isLoading'
      ></progress>
      <!-- Show the data if state is loaded -->
      <div *ngIf="history.value?.length !== 0">
        <app-query-history-entry *ngFor="let summary of history.value"
                                 (click)="queryHistoryElementClicked.emit(summary)"
                                 [history]="summary">
        </app-query-history-entry>
      </div>
      <tui-notification *ngIf='history.value?.length === 0'>
        No queries have finished running yet
      </tui-notification>
      <!-- Show an error message if state is error -->
      <tui-notification *ngIf='history.error' status='error'>
        {{history.error.message}}
      </tui-notification>
    </ng-container>
  </div>`
})
export class QueryHistoryPanelComponent {
  @Output()
  queryHistoryElementClicked = new EventEmitter<QueryHistorySummary>();

  constructor(public historyStoreService: QueryHistoryStoreService) {
    historyStoreService.getHistory();
  }
}
