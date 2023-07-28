import {ChangeDetectionStrategy, Component, Input, OnInit} from '@angular/core';
import {QueryHistorySummary} from "../services/query.service";
import {trimImportsFromQuery} from "../query-history/vyneql-record.component";

@Component({
  selector: 'app-query-history-entry',
  template: `
      <div class="row">
          <div class="query-code">{{ taxiQl  | truncate: 100  }}</div>
      </div>
      <div class="row stats">
        <span>{{ history.durationMs | number }} ms</span>
        <span>{{ history.recordCount | number }} records</span>
      </div>
  `,
  styleUrls: ['./query-history-entry.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QueryHistoryEntryComponent  {

  @Input()
  history: QueryHistorySummary

  get taxiQl():string {
    if (!this.history) return '';
    return trimImportsFromQuery(this.history.taxiQl)
  }

}
