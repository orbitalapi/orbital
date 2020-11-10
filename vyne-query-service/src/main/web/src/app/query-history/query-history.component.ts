import {Component, Input, OnInit} from '@angular/core';
import {
  isRestQueryHistoryRecord,
  isVyneQlQueryHistoryRecord,
  ProfilerOperation,
  QueryHistoryRecord,
  QueryHistorySummary,
  QueryResult,
  QueryService,
} from '../services/query.service';
import {Router} from '@angular/router';
import {ExportFileService} from '../services/export.file.service';
import {DownloadClickedEvent} from '../object-view/object-view-container.component';
import {TypesService} from '../services/types.service';
import {BaseQueryResultDisplayComponent} from '../query-panel/BaseQueryResultDisplayComponent';


@Component({
  selector: 'app-query-history',
  templateUrl: './query-history.component.html',
  styleUrls: ['./query-history.component.scss']
})
export class QueryHistoryComponent extends BaseQueryResultDisplayComponent implements OnInit {
  history: QueryHistorySummary[];
  activeRecord: QueryHistoryRecord;

  constructor(queryService: QueryService,
              typeService: TypesService,
              private router: Router,
              private fileService: ExportFileService) {
    super(queryService, typeService);
  }

  profileLoading = false;
  profilerOperation: ProfilerOperation;
  private _queryResponseId: string;

  @Input()
  get queryResponseId(): string {
    return this._queryResponseId;
  }

  set queryResponseId(value: string) {
    this._queryResponseId = value;
  }

  get queryId(): string {
    return this.activeRecord.id;
  }

  ngOnInit() {
    this.loadData();
    if (this._queryResponseId && this._queryResponseId.length > 0) {
      this.setActiveRecordFromRoute();
    }
  }

  loadData() {
    this.queryService.getHistory()
      .subscribe(history => this.history = history);
  }

  typeName(qualifiedTypeName: string) {
    // TODO : There's a correct parser for type names on the server
    // which can handle generics.
    // Consider using that, instead of this dirty hack

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

  setActiveRecord(historyRecord: QueryHistorySummary) {
    this.profilerOperation = null;
    this.profileLoading = true;
    this.queryService.getHistoryRecord(historyRecord.queryId).subscribe(
      result => {
        this.activeRecord = result;
      }
    );
    // Profiles on large objects are causing problems.
    // Disabling for now.
    // this.service.getQueryProfile(historyRecord.queryId).subscribe(
    //   result => {
    //     this.profileLoading = false;
    //     this.profilerOperation = result;
    //   }
    // );
    this.setRouteFromActiveRecord();
  }

  setActiveRecordFromRoute() {
    this.queryService.getHistoryRecord(this._queryResponseId)
      .subscribe(record => {
        this.activeRecord = record;
      });
  }

  setRouteFromActiveRecord() {
    this.router.navigate(['/query-history', this.activeRecord.id]);
  }

  onCloseTypedInstanceDrawer($event: boolean) {
    this.shouldTypedInstancePanelBeVisible = $event;
  }

  downloadQueryHistory(event: DownloadClickedEvent) {
    const queryResponseId = (<QueryResult>this.activeRecord.response).queryResponseId;
    this.fileService.downloadQueryHistory(queryResponseId, event.format);
  }
}

