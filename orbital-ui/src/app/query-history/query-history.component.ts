import { ChangeDetectorRef, Component, OnDestroy, OnInit } from "@angular/core";
import { QueryHistorySummary, QueryProfileData, QueryService, StreamQueryErrorEvent } from "../services/query.service";
import { ActivatedRoute, Router } from "@angular/router";
import { DownloadClickedEvent } from "../object-view/object-view-container.component";
import { TypesService } from "../services/types.service";
import { BaseQueryResultDisplayComponent } from "../query-panel/BaseQueryResultDisplayComponent";
import { combineLatest, mergeMap, Observable, of, ReplaySubject } from "rxjs";
import { InstanceLike, tryFindType, Type } from "../services/schema";
import { catchError, flatMap, map, shareReplay, startWith, take, tap } from "rxjs/operators";
import {
  ActiveQueriesNotificationService,
  RunningQueryStatus
} from "../services/active-queries-notification-service";
import { ValueWithTypeName } from "../services/models";
import { Subscription } from "rxjs";
import { AppInfoService, AppConfig } from "../services/app-info.service";
import { QueryResultInstanceSelectedEvent } from "../query-panel/result-display/BaseQueryResultComponent";
import { ExportFormat, ResultsDownloadService } from "src/app/results-download/results-download.service";
import { isNullOrUndefined } from "../utils/utils";
import { HttpRequestState, httpRequestStates } from "ngx-http-request-state";
import { fromIterable } from "rxjs/internal/observable/innerFrom";

@Component({
  selector: "app-query-history",
  templateUrl: "./query-history.component.html",
  styleUrls: ["./query-history.component.scss"]
})
export class QueryHistoryComponent extends BaseQueryResultDisplayComponent implements OnInit, OnDestroy {
  history$: Observable<HttpRequestState<QueryHistorySummary[]>>;
  activeRecordResults$: Observable<InstanceLike>;
  activeRecordResultType: Type;
  activeQueryProfileData$: Observable<QueryProfileData>;
  isQueryLoading$: Observable<boolean>;
  activeRecordErrors$: Observable<StreamQueryErrorEvent>;
  errorCount: number =0;
  queryLoadingErrorMessage: string = null;

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

  profilerOperation: QueryProfileData;

  private selectedQueryId: string = null;

  ngOnInit() {
    this.loadQuerySummaries();
    this.activatedRoute.paramMap.subscribe(location => {
        if (location.has("queryResponseId")) {
          this.selectedQueryId = location.get("queryResponseId");
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
        console.log("Error thrown while unsubscribing : " + e.message);
      }
    });
  }

  loadQuerySummaries() {
    this.history$ = this.queryService.getHistory().pipe(
      // We don't want any queries that are still running, they're already stored in the activeQueries prop
      map(results => results.filter(result => result.responseStatus !== "RUNNING")),
      httpRequestStates()
    );
  }

  typeName(qualifiedTypeName: string) {
    // TODO : There's a correct parser for type names on the server
    // which can handle generics.
    // Consider using that, instead of this dirty hack

    if (qualifiedTypeName.startsWith("lang.taxi.Array<")) {
      const collectionMemberName = qualifiedTypeName.replace("lang.taxi.Array<", "").slice(0, -1);
      return this.typeName(collectionMemberName) + "[]";
    } else {
      const parts = qualifiedTypeName.split(".");
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
    this.queryLoadingErrorMessage = null;
    console.log(`Fetching query results for query ${selectedQueryId}`);

    this.activeRecordResults$ = this.queryService.getQueryResults(selectedQueryId, null)
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
        ),
        catchError((err, observable) => {
          this.queryLoadingErrorMessage = 'Query results are not available for this query';
          return of(err);
        })
      );
    this.activeQueryProfileData$ = this.queryService.getQueryProfile(selectedQueryId).pipe(
      shareReplay(1)
    );
    this.activeRecordErrors$ = this.activeQueryProfileData$.pipe(
      mergeMap(profileData => {
        const errorEvents = profileData.errors.map(error => {
          return {
            queryId: error.queryId,
            error: {
              timestamp: error.timestamp,
              message: error.message,
              payload: error.payload,
              typeName: error.typeName
            }
          } as StreamQueryErrorEvent;
        });
        this.errorCount = errorEvents.length;
        return fromIterable(errorEvents);
      })
    );

    // Subscribe to error records, as we need to get the error count.
    // This will trigger a subscription on profile data, which is
    // why it's shareReplay(), to avoid loading it mulitple times.
    this.activeRecordErrors$.subscribe()

    this.isQueryLoading$ = combineLatest([
      this.activeRecordResults$.pipe(map(val => false), startWith(true), catchError(() => of(false))),
      this.activeQueryProfileData$.pipe(map(val => false), startWith(true), catchError(() => of(false)))
    ]).pipe(
      map(([resultsLoaded, profileDataLoaded]) => resultsLoaded || profileDataLoaded)
    );
  }

  get queryId(): string {
    return this.selectedQueryId;
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

  protected readonly of = of;
}

