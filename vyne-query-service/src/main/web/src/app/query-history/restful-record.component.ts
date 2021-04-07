import {Component, Input, OnInit} from '@angular/core';
import {QueryHistorySummary} from '../services/query.service';

@Component({
  selector: 'app-restful-record',
  template: `
    <div class="query-description">
      <div class="given-line">
        Given (<span class="type-name"
                     *ngFor="let factName of factTypeNames; last as isLast">{{ factName }}<span
        *ngIf="!isLast">, </span> </span>)
      </div>
      <div class="discover-line">
        <span class="verb">{{ historyRecord.queryJson.queryMode.toLowerCase() }} </span>
        <span class="type-name">{{ expressionTypeName }}</span>
      </div>
    </div>
  `,
  styleUrls: ['./restful-record.component.scss']
})
export class RestfulRecordComponent {

  @Input()
  historyRecord: QueryHistorySummary;

  @Input()
  expressionTypeName: string;

  @Input()
  factTypeNames: string[];
}
