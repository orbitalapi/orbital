import {Component, Input} from '@angular/core';
import {RestfulQueryHistorySummary} from '../services/query.service';

@Component({
  selector: 'app-restful-record',
  template: `
    <div class="query-description">
      <div class="given-line clipped">
        Given (<span class="type-name "
                     *ngFor="let factName of factTypeNames; last as isLast">{{ factName }}<span
        *ngIf="!isLast">, </span> </span>)
      </div>
      <div class="discover-line clipped">
        <span class="verb">{{ historyRecord.query.queryMode.toLowerCase() }} </span>
        <span class="type-name">{{ expressionTypeName }}</span>
      </div>
    </div>
  `,
  styleUrls: ['./restful-record.component.scss']
})
export class RestfulRecordComponent {

  @Input()
  historyRecord: RestfulQueryHistorySummary;

  @Input()
  expressionTypeName: string;

  @Input()
  factTypeNames: string[];
}
