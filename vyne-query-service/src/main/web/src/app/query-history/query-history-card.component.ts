import {Component, Input} from '@angular/core';
import {QueryHistorySummary} from '../services/query.service';
import {Timespan} from '../query-panel/query-editor/counter-timer.component';
import {Router} from '@angular/router';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-query-history-card',
  template: `
    <div class="history-item">
      <app-vyneql-record [taxiQlQuery]="historyRecord.taxiQl">

      </app-vyneql-record>
      <div class="record-stats">
        <div class="record-stat">
          <img src="assets/img/tabler/clock.svg">
          <span>{{ duration }}</span>
        </div>
        <div class="record-stat" *ngIf="historyRecord.responseStatus !== 'ERROR' && historyRecord.responseStatus !== 'CANCELLED'">
          <img src="assets/img/tabler/check.svg">
          <span>{{ historyRecord.recordCount }} records</span>
        </div>
        <div class="record-stat" *ngIf="historyRecord.responseStatus === 'ERROR'">
          <img src="assets/img/tabler/exclamation-mark.svg">
          <span>A problem occurred</span>
        </div>
        <div class="record-stat" *ngIf="historyRecord.responseStatus === 'CANCELLED'">
          <img src="assets/img/tabler/circle-x.svg">
          <span>Cancelled - {{ historyRecord.recordCount }} records</span>
        </div>
      </div>

      <div class="timestamp-row">
        <span>{{historyRecord.startTime | amTimeAgo}}</span>
        <button  class="icon-button" mat-icon-button (click)="queryAgain($event)" *ngIf="recordType === 'VyneQlQuery'">
          <img src="assets/img/tabler/repeat.svg">
        </button>
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


  queryAgain(event:Event) {
    event.preventDefault();
    event.stopImmediatePropagation();
    if (this.historyRecord) {
      this.router.navigate(['/query/editor'], {state: {query: this.historyRecord}});
    }
  }

}


type QueryType = 'VyneQlQuery' | 'RestfulQuery';

