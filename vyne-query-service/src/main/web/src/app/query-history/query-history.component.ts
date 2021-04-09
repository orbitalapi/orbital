import {Component, Input, OnInit} from '@angular/core';
import {QueryProfileData, QueryHistorySummary, QueryService, ValueWithTypeName} from '../services/query.service';
import {ActivatedRoute, Router} from '@angular/router';
import {ExportFormat, ExportFileService} from '../services/export.file.service';
import {DownloadClickedEvent} from '../object-view/object-view-container.component';
import {TypesService} from '../services/types.service';
import {BaseQueryResultDisplayComponent} from '../query-panel/BaseQueryResultDisplayComponent';
import {TestSpecFormComponent} from '../test-pack-module/test-spec-form.component';
import {MatDialog} from '@angular/material/dialog';
import {isNullOrUndefined} from 'util';
import {Observable} from 'rxjs/index';
import {findType, InstanceLike, Type} from '../services/schema';
import {take, tap} from 'rxjs/operators';

@Component({
  selector: 'app-query-history',
  templateUrl: './query-history.component.html',
  styleUrls: ['./query-history.component.scss']
})
export class QueryHistoryComponent extends BaseQueryResultDisplayComponent implements OnInit {
  history: QueryHistorySummary[];
  activeRecordResults$: Observable<InstanceLike>;
  activeRecordResultType: Type;

  activeQueryProfileData$: Observable<QueryProfileData>;

  constructor(queryService: QueryService,
              typeService: TypesService,
              private router: Router,
              private activatedRoute: ActivatedRoute,
              private fileService: ExportFileService
  ) {
    super(queryService, typeService);
  }

  profileLoading = false;
  profilerOperation: QueryProfileData;

  private selectedQueryId: string = null;

  ngOnInit() {
    this.loadQuerySummaries();
    this.activatedRoute.paramMap.subscribe(location => {
        if (location.has('queryResponseId')) {
          this.selectedQueryId = location.get('queryResponseId');
          this.loadQueryResults(this.selectedQueryId);
        }
      }
    );
  }

  loadQuerySummaries() {
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

  onCloseTypedInstanceDrawer($event: boolean) {
    this.shouldTypedInstancePanelBeVisible = $event;
  }

  downloadQueryHistory(event: DownloadClickedEvent) {
    const queryResponseId = this.selectedQueryId;
    if (event.format === ExportFormat.TEST_CASE) {
      this.fileService.promptToDownloadTestCase(queryResponseId);
    } else {
      this.fileService.downloadQueryHistory(queryResponseId, event.format);
    }
  }

  private loadQueryResults(selectedQueryId: string) {
    this.activeRecordResults$ = this.queryService.getQueryResults(selectedQueryId)
      .pipe(
        tap((valueWithTypeName: ValueWithTypeName) => {
            if (isNullOrUndefined(this.activeRecordResultType) && !isNullOrUndefined(valueWithTypeName.typeName)) {
              // There's a race condition on startup if the user navigates to /history/{history-id}
              // where we can receive the history record before the schema, so we need to use
              // the schema from an observable, rather than the local instance/
              this.typeService.getTypes()
                .pipe(take(1))
                .subscribe(schema => {
                  this.activeRecordResultType = findType(schema, valueWithTypeName.typeName);
                });
            }
          }
        )
      );
    // Don't subscribe here.  We'll only fetch these results if the user opens the profile data
    this.activeQueryProfileData$ = this.queryService.getQueryProfile(selectedQueryId);
  }

  get queryId(): string {
    return this.selectedQueryId;
  }

  setActiveRecord($event: QueryHistorySummary) {
    this.router.navigate(['/query-history', $event.queryId]);
  }
}

