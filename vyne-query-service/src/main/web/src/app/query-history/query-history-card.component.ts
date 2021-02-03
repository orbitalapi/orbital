import {Component, Input, OnInit} from '@angular/core';
import {
  isRestQueryHistorySummaryRecord,
  isVyneQlQueryHistorySummaryRecord,
  QueryHistorySummary, VyneQlQueryHistorySummary
} from '../services/query.service';
import {Timespan} from '../query-panel/query-editor/counter-timer.component';
import {Router} from '@angular/router';

@Component({
  selector: 'app-query-history-card',
  template: `
    <div class="history-item">
      <div [ngSwitch]="recordType">
        <div *ngSwitchCase="'RestfulQuery'">
          <app-restful-record [expressionTypeName]="expressionTypeName(historyRecord)"
                              [historyRecord]="historyRecord"
                              [factTypeNames]="getFactTypeNames(historyRecord)"></app-restful-record>
        </div>
        <div *ngSwitchCase="'VyneQlQuery'">
          <app-vyneql-record [query]="vyneQl(historyRecord).query"></app-vyneql-record>
        </div>

      </div>
      <div class="record-stats">
        <div class="record-stat">
          <mat-icon class="clock-icon">schedule</mat-icon>
          <span>{{ duration }}</span>
        </div>
        <div class="record-stat" *ngIf="historyRecord.responseStatus !== 'ERROR'">
          <mat-icon class="clock-icon">done</mat-icon>
          <span>{{ historyRecord.recordSize }} records</span>
        </div>
        <div class="record-stat" *ngIf="historyRecord.responseStatus === 'ERROR'">
          <mat-icon class="clock-icon">error_outline</mat-icon>
          <span>A problem occurred</span>
        </div>
      </div>

      <div class="timestamp-row">
        <span>{{historyRecord.timestamp | amTimeAgo}}</span>
        <mat-icon class="clock-icon" (click)="queryAgain()" *ngIf="recordType === 'VyneQlQuery'">replay</mat-icon>
      </div>
    </div>
  `,
  styleUrls: ['./query-history-card.component.scss']
})
export class QueryHistoryCardComponent {
  duration: string;
  private _historyRecord: QueryHistorySummary;
  recordType: QueryType;

  // casting functions to get around template issues
  vyneQl(record: QueryHistorySummary): VyneQlQueryHistorySummary {
    return record as VyneQlQueryHistorySummary;
  }

  @Input()
  get historyRecord(): QueryHistorySummary {
    return this._historyRecord;
  }

  set historyRecord(value: QueryHistorySummary) {
    this._historyRecord = value;
    this.duration = Timespan.ofMillis(value.durationMs).duration;
    this.recordType = this.queryType(value);
  }


  constructor(private router: Router) {
  }


  getFactTypeNames(record: QueryHistorySummary): string[] {
    if (isRestQueryHistorySummaryRecord(record)) {
      return record.query.facts.map(fact => fact.qualifiedName.longDisplayName);
    } else {
      return [];
    }
  }

  expressionTypeName(historyRecord: QueryHistorySummary): string {
    if (isRestQueryHistorySummaryRecord(historyRecord)) {
      return historyRecord.query.expression.qualifiedTypeNames.map(t => t.longDisplayName).join(', ');
    } else {
      return '';
    }
  }


  queryType(historyRecord: QueryHistorySummary): QueryType {
    if (isVyneQlQueryHistorySummaryRecord(historyRecord)) {
      return 'VyneQlQuery';
    } else if (isRestQueryHistorySummaryRecord(historyRecord)) {
      return 'RestfulQuery';
    } else {
      throw new Error('Unknown type of query history record: ' + JSON.stringify(historyRecord));
    }
  }


  queryAgain() {
    if (this.historyRecord) {
      this.router.navigate(['/query-wizard'], {state: {query: this.historyRecord}});
    }
  }

}


type QueryType = 'VyneQlQuery' | 'RestfulQuery';

