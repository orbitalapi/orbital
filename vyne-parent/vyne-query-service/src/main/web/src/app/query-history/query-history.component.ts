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

  loadData() {
    this.service.getHistory()
      .subscribe(history => this.history = history)
  }

  typeName(qualifiedTypeName: string) {
    // TODO : There's a correct parser for type names on the server
    // which can handle generics.
    // Consider usinng that, instead of this dirty hack

    if (qualifiedTypeName.startsWith("lang.taxi.Array<")) {
      const collectionMemberName = qualifiedTypeName.replace("lang.taxi.Array<", "").slice(0, -1)
      return this.typeName(collectionMemberName) + "[]";
    } else {
      let parts = qualifiedTypeName.split(".");
      return parts[parts.length - 1];
    }
  };

  getFactTypeNames(record: QueryHistoryRecord): string[] {
    return record.query.facts.map(fact => this.typeName(fact.typeName))
  }

  setActiveRecord(historyRecord: QueryHistoryRecord) {
    this.activeRecord = historyRecord
  }
}
