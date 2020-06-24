import {Component, Input, OnInit} from '@angular/core';
import {VyneQlQueryHistoryRecord} from '../services/query.service';

@Component({
  selector: 'app-vyneql-record',
  template: `
    <code>{{ historyRecord.query }}</code>
    <div class="timestamp-row">
      <span>{{historyRecord.timestamp | amTimeAgo}}</span>
    </div>
  `,
  styleUrls: ['./vyneql-record.component.scss']
})
export class VyneqlRecordComponent {

  @Input()
  historyRecord: VyneQlQueryHistoryRecord;
}
