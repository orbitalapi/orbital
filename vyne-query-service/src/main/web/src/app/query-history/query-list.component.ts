import {Component, EventEmitter, Input, Output} from '@angular/core';
import {QueryHistorySummary} from '../services/query.service';

@Component({
  selector: 'app-query-list',
  template: `
    <div class="list-container">
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

  @Output()
  recordSelected = new EventEmitter<QueryHistorySummary>();

}
