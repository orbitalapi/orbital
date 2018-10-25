import {Component, OnInit} from '@angular/core';
import {QueryHistoryRecord, QueryService} from "../services/query.service";

@Component({
  selector: 'app-query-history',
  templateUrl: './query-history.component.html',
  styleUrls: ['./query-history.component.scss']
})
export class QueryHistoryComponent implements OnInit {
  history: QueryHistoryRecord[];
  activeRecord: QueryHistoryRecord;

  constructor(private service: QueryService) {
  }


  ngOnInit() {
    this.loadData();
  }

  private loadData() {
    this.service.getHistory()
      .subscribe(history => this.history = history)
  }

  typeName(qualifiedTypeName: string) {
    let parts = qualifiedTypeName.split(".");
    return parts[parts.length - 1];
  };

  getFactTypeNames(record: QueryHistoryRecord): string[] {
    return Object.keys(record.query.facts).map(this.typeName)
  }

  setActiveRecord(historyRecord: QueryHistoryRecord) {
    this.activeRecord = historyRecord
  }
}
