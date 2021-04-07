import {Component, Input, OnInit} from '@angular/core';
import {ProfilerOperation, QueryHistorySummary, QueryService} from '../services/query.service';
import {Router} from '@angular/router';
import {ExportFileService} from '../services/export.file.service';
import {DownloadClickedEvent} from '../object-view/object-view-container.component';
import {TypesService} from '../services/types.service';
import {BaseQueryResultDisplayComponent} from '../query-panel/BaseQueryResultDisplayComponent';
import {DownloadFileType} from '../query-panel/result-display/result-container.component';
import {TestSpecFormComponent} from '../test-pack-module/test-spec-form.component';
import {MatDialog} from '@angular/material/dialog';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-query-history',
  templateUrl: './query-history.component.html',
  styleUrls: ['./query-history.component.scss']
})
export class QueryHistoryComponent extends BaseQueryResultDisplayComponent implements OnInit {
  history: QueryHistorySummary[];
  activeRecord: QueryHistorySummary;

  constructor(queryService: QueryService,
              typeService: TypesService,
              private router: Router,
              private fileService: ExportFileService,
              private dialogService: MatDialog) {
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
    return this.activeRecord.queryId;
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

  isVyneQlQuery(record: QueryHistorySummary): boolean {
    return !isNullOrUndefined(record.taxiQl);
  }

  isRestQuery(record: QueryHistorySummary): boolean {
    return !isNullOrUndefined(record.queryJson);
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
    this.router.navigate(['/query-history', this.activeRecord.queryId]);
  }

  onCloseTypedInstanceDrawer($event: boolean) {
    this.shouldTypedInstancePanelBeVisible = $event;
  }

  downloadQueryHistory(event: DownloadClickedEvent) {
    const queryResponseId = this.activeRecord.queryId;
    if (event.format === DownloadFileType.TEST_CASE) {
      const dialogRef = this.dialogService.open(TestSpecFormComponent, {
        width: '550px'
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result !== null) {
          // noinspection UnnecessaryLocalVariableJS
          const specName = result;
          this.fileService.downloadRegressionPackZipFile(queryResponseId, specName);
        }
      });
    } else {
      this.fileService.downloadQueryHistory(queryResponseId, event.format);
    }
  }
}

