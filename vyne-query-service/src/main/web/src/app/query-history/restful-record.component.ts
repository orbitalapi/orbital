import {Component, Input, OnInit} from '@angular/core';
import {RestfulQueryHistoryRecord, RestfulQueryHistorySummary} from '../services/query.service';

@Component({
  selector: 'app-restful-record',
  template: `
    <div class="query-description">
      <code>
        <div class="code-line">
          given (<span class="type-name">{{factTypeNameList}}</span>)
        </div>
        <div class="code-line">
          <span class="verb">{{ historyRecord.query.queryMode.toLowerCase() }} </span>
          <span class="type-name">{{ expressionTypeName }}</span>
        </div>
      </code>
    </div>
  `,
  styleUrls: ['./restful-record.component.scss']
})
export class RestfulRecordComponent {


  factTypeNameList: string;
  private _factTypeNames: string[];

  @Input()
  get factTypeNames(): string[] {
    return this._factTypeNames;
  }

  set factTypeNames(value: string[]) {
    if (this._factTypeNames === value) {
      return;
    }
    this._factTypeNames = value;
    this.factTypeNameList = this.factTypeNames.join(', ');
  }

  @Input()
  historyRecord: RestfulQueryHistorySummary;

  @Input()
  expressionTypeName: string;

}
