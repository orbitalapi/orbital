import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { QueryHistorySummary } from '../services/query.service';
import { trimPreludeFromQuery } from '../query-history/vyneql-record.component';
import { Timespan } from '../query-panel/query-editor/counter-timer.component';

@Component({
  selector: 'app-query-history-entry',
  template: `
    <div class="row">
      <div class="query-code">{{ taxiQl  | truncate: 100 }}</div>
    </div>
    <div class="row stats">
      <span>{{ duration(history.durationMs) }}</span>
      <span>{{ history.recordCount | number }} records</span>
    </div>
  `,
  styleUrls: ['./query-history-entry.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QueryHistoryEntryComponent {

  @Input()
  history: QueryHistorySummary

  get taxiQl(): string {
    if (!this.history) return '';
    return trimPreludeFromQuery(this.history.taxiQl)
  }

  duration(value: number) {
    return Timespan.ofMillis(value).duration;
  }
}
