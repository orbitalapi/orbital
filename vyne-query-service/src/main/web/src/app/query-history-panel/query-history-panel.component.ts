import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {QueryHistorySummary, QueryService} from "../services/query.service";

@Component({
  selector: 'app-query-history-panel',
  styleUrls: ['./query-history-panel.component.scss'],
  template: `<div>
    <app-query-history-entry *ngFor="let summary of history"
                             (click)="queryHistoryElementClicked.emit(summary)"
        [history]="summary"
    ></app-query-history-entry>
  </div>`
})
export class QueryHistoryPanelComponent {
  history: QueryHistorySummary[];

  @Output()
  queryHistoryElementClicked = new EventEmitter<QueryHistorySummary>();

  constructor(private historyService: QueryService) {
    historyService.getHistory()
      .subscribe(result => {
        this.history = result;
      })
  }
}
