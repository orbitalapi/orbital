import {Component, Input, OnInit} from '@angular/core';
import {QueryHistoryRecord, VyneQlQueryHistoryRecord} from '../services/query.service';
import {Router} from "@angular/router";

@Component({
  selector: 'app-vyneql-record',
  template: `
    <code>{{ historyRecord.query }}</code>
    <div class="timestamp-row">
      <span>{{historyRecord.timestamp | amTimeAgo}}</span>
      <span style="float: right">
        <button mat-icon-button  color="primary" (click)="queryAgain()">
        <img src="assets/img/repeat.svg" class="repeatIcon" >
        </button>
      </span>
    </div>
  `,
  styleUrls: ['./vyneql-record.component.scss']
})
export class VyneqlRecordComponent {

  @Input()
  historyRecord: VyneQlQueryHistoryRecord;
  constructor(private router: Router) {
  }

  queryAgain() {
    this.historyRecord && this.router.navigate(['/query-wizard'], { state: { query: this.historyRecord}});
  }
}
