import {Component, Input, OnInit, Output} from '@angular/core';
import {QueryHistoryRecord, VyneQlQueryHistoryRecord, VyneQlQueryHistorySummary} from '../services/query.service';
import {Router} from '@angular/router';
import {Timespan} from '../query-panel/query-editor/counter-timer.component';

@Component({
  selector: 'app-vyneql-record',
  template: `
    <div class="history-item">
      <code>{{ historyRecord.query }}</code>
    </div>
  `,
  styleUrls: ['./vyneql-record.component.scss']
})
export class VyneqlRecordComponent {

  @Input()
  historyRecord: VyneQlQueryHistorySummary;
}
