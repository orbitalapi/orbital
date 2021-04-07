import {Component, Input} from '@angular/core';
import {QueryHistorySummary} from '../services/query.service';
import {Timespan} from '../query-panel/query-editor/counter-timer.component';
import {Router} from '@angular/router';
import {isNullOrUndefined} from 'util';

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
          <app-vyneql-record [historyRecord]="historyRecord">

          </app-vyneql-record>
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
        <span>{{historyRecord.startTime | amTimeAgo}}</span>
        <img src="assets/img/repeat.svg" class="repeatIcon" (click)="queryAgain()" *ngIf="recordType === 'VyneQlQuery'">
      </div>
    </div>
  `,
  styleUrls: ['./query-history-card.component.scss']
})
export class QueryHistoryCardComponent {
  duration: string;
  private _historyRecord: QueryHistorySummary;
  recordType: QueryType;

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

    if (!isNullOrUndefined(record.queryJson)) {
      return record.queryJson.facts.map(fact => fact.qualifiedName.longDisplayName);
    } else {
      return [];
    }
  }

  expressionTypeName(historyRecord: QueryHistorySummary): string {
    if (!isNullOrUndefined(historyRecord.queryJson)) {
      return historyRecord.queryJson.expression.qualifiedTypeNames.map(t => t.longDisplayName).join(', ');
    } else {
      return '';
    }
  }


  queryType(historyRecord: QueryHistorySummary): QueryType {
    if (!isNullOrUndefined(historyRecord.taxiQl)) {
      return 'VyneQlQuery';
    } else if (!isNullOrUndefined(historyRecord.queryJson)) {
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

