import {Component, OnInit} from '@angular/core';
import {
  isRestQueryHistoryRecord,
  isVyneQlQueryHistoryRecord,
  ProfilerOperation,
  QueryHistoryRecord,
  QueryService,
  RestfulQueryHistoryRecord
} from '../services/query.service';
import {isStyleUrlResolvable} from '@angular/compiler/src/style_url_resolver';

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

  profileLoading = false;
  profilerOperation: ProfilerOperation;

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.service.getHistory()
      .subscribe(history => this.history = history);
  }

  typeName(qualifiedTypeName: string) {
    // TODO : There's a correct parser for type names on the server
    // which can handle generics.
    // Consider usinng that, instead of this dirty hack

    if (qualifiedTypeName.startsWith('lang.taxi.Array<')) {
      const collectionMemberName = qualifiedTypeName.replace('lang.taxi.Array<', '').slice(0, -1);
      return this.typeName(collectionMemberName) + '[]';
    } else {
      const parts = qualifiedTypeName.split('.');
      return parts[parts.length - 1];
    }
  }

  isVyneQlQuery(record: QueryHistoryRecord): boolean {
    return isVyneQlQueryHistoryRecord(record);
  }

  isRestQuery(record: QueryHistoryRecord): boolean {
    return isRestQueryHistoryRecord(record);
  }

  getFactTypeNames(record: QueryHistoryRecord): string[] {
    if (isRestQueryHistoryRecord(record)) {
      return record.query.facts.map(fact => this.typeName(fact.typeName));
    } else {
      return [];
    }

  }

  setActiveRecord(historyRecord: QueryHistoryRecord) {
    this.activeRecord = historyRecord;
    this.profilerOperation = null;
    this.profileLoading = true;
    this.service.getQueryProfile(historyRecord.id).subscribe(
      result => {
        this.profileLoading = false;
        this.profilerOperation = result;
      }
    );
  }

  expressionTypeName(historyRecord: QueryHistoryRecord): string {
    if (isRestQueryHistoryRecord(historyRecord)) {
      return historyRecord.query.expression.map(t => this.typeName(t)).join(', ');
    } else {
      return '';
    }
  }


  queryType(historyRecord: QueryHistoryRecord): QueryType {
    if (isVyneQlQueryHistoryRecord(historyRecord)) {
      return 'VyneQlQuery';
    } else if (isRestQueryHistoryRecord(historyRecord)) {
      return 'RestfulQuery';
    } else {
      throw new Error('Unknown type of query history record: ' + JSON.stringify(historyRecord));
    }
  }
}

type QueryType = 'VyneQlQuery' | 'RestfulQuery';
