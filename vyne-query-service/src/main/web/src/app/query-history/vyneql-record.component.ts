import {Component, Input} from '@angular/core';
import {QueryHistorySummary} from '../services/query.service';

@Component({
  selector: 'app-vyneql-record',
  template: `
    <div class="history-item">
      <code>{{ historyRecord.taxiQl }}</code>
    </div>
  `,
  styleUrls: ['./vyneql-record.component.scss']
})
export class VyneqlRecordComponent {

  @Input()
  historyRecord: QueryHistorySummary;
}
