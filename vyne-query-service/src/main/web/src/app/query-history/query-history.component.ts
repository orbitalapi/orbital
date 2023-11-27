import {ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {QueryHistorySummary, QueryProfileData, QueryService} from '../services/query.service';
import {ActivatedRoute, Router} from '@angular/router';
import {DownloadClickedEvent} from '../object-view/object-view-container.component';
import {TypesService} from '../services/types.service';
import {BaseQueryResultDisplayComponent} from '../query-panel/BaseQueryResultDisplayComponent';
import {Observable, ReplaySubject} from 'rxjs';
import {InstanceLike, tryFindType, Type} from '../services/schema';
import {take, tap} from 'rxjs/operators';
import {ActiveQueriesNotificationService, RunningQueryStatus} from '../services/active-queries-notification-service';
import {ValueWithTypeName} from '../services/models';
import {Subscription} from 'rxjs';
import {AppInfoService, AppConfig} from '../services/app-info.service';
import {QueryResultInstanceSelectedEvent} from '../query-panel/result-display/BaseQueryResultComponent';
import {ExportFormat, ResultsDownloadService} from 'src/app/results-download/results-download.service';
import {isNullOrUndefined} from "../utils/utils";

@Component({
  selector: 'app-query-history',
  templateUrl: './query-history.component.html',
  styleUrls: ['./query-history.component.scss']
})
export class QueryHistoryComponent extends BaseQueryResultDisplayComponent implements OnInit, OnDestroy {
  history: QueryHistorySummary[];
  activeRecordResults$: Observable<InstanceLike>;
  activeRecordResultType: Type;
  activeQueryProfileData$: Observable<QueryProfileData>;

  instanceSelected$ = new ReplaySubject<QueryResultInstanceSelectedEvent>(1);
  sidePanelVisible: boolean = false;

  private subscriptions: Subscription[] = [];

  activeQueries: Map<string, RunningQueryStatus> = new Map<string, RunningQueryStatus>();
  config: AppConfig;

  constructor(appInfoService: AppInfoService,
              queryService: QueryService,
              typeService: TypesService,
              private router: Router,
              private activatedRoute: ActivatedRoute,
              private fileService: ResultsDownloadService,
              private activeQueryNotificationService: ActiveQueriesNotificationService,
              protected changeDetector: ChangeDetectorRef
  ) {
    super(queryService, typeService, changeDetector);
    this.subscriptions.push(this.activeQueryNotificationService.createActiveQueryNotificationSubscription()
      .subscribe(event => this.handleActiveQueryUpdate(event)));
    appInfoService.getConfig()
      .subscribe(next => this.config = next);
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

  ngOnDestroy(): void {
    this.subscriptions.forEach(subscription => {

      try {
        subscription.unsubscribe();
      } catch (e) {
        console.log('Error thrown while unsubscribing : ' + e.message);
      }
    });
  }

  loadQuerySummaries() {
    this.subscriptions.push(this.queryService.getHistory()
      .subscribe(history => this.history = history));
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

  downloadQueryHistory(event: DownloadClickedEvent) {
    const queryResponseId = this.selectedQueryId;
    if (event.format === ExportFormat.TEST_CASE) {
      this.fileService.promptToDownloadTestCase(queryResponseId);
    } else {
      this.fileService.downloadQueryHistory(queryResponseId, event.format);
    }
  }

  private loadQueryResults(selectedQueryId: string) {

    console.log(`Fetching query results for query ${selectedQueryId}`);

    this.activeRecordResults$ = this.queryService.getQueryResults(selectedQueryId)
      .pipe(
        tap((valueWithTypeName: ValueWithTypeName) => {
            if (!isNullOrUndefined(valueWithTypeName.typeName)) {
              // There's a race condition on startup if the user navigates to /history/{history-id}
              // where we can receive the history record before the schema, so we need to use
              // the schema from an observable, rather than the local instance/
              this.subscriptions.push(this.typeService.getTypes()
                .pipe(take(1))
                .subscribe(schema => {
                  // Make sure the activeRecordREsultType hasn't been set in between subscribing to the observable, and getting the result.
                  // if (isNullOrUndefined(this.activeRecordResultType) && !isNullOrUndefined(valueWithTypeName.typeName)) {
                  if (!isNullOrUndefined(valueWithTypeName.anonymousTypes) && valueWithTypeName.anonymousTypes.length > 0) {
                    this.activeRecordResultType = valueWithTypeName.anonymousTypes[0];
                  } else {
                    this.activeRecordResultType = tryFindType(schema, valueWithTypeName.typeName);
                  }
                  // }
                }));
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

  private handleActiveQueryUpdate(next: RunningQueryStatus) {
    if (next.running) {
      this.updateRunningQueryStatus(next);
    } else {
      this.activeQueries.delete(next.queryId);
      this.loadQuerySummaries();
    }

  }

  private updateRunningQueryStatus(next: RunningQueryStatus) {
    if (this.activeQueries.has(next.queryId)) {
      const currentStatus = this.activeQueries.get(next.queryId);
      if (next.completedProjections > currentStatus.completedProjections || !next.running) {
        // We receive updates out-of-order, so only update progress indicator
        // if this is a higher update than previously received.
        this.activeQueries.set(next.queryId, next);
      }
    } else {
      this.activeQueries.set(next.queryId, next);
    }
  }

  cancelActiveQuery($event: RunningQueryStatus) {
    this.queryService.cancelQuery($event.queryId)
      .subscribe(() => {
        console.log(`Query ${$event.queryId} cancelled`);
      });
  }
}

