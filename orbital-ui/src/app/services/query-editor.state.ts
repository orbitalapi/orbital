import {inject, WritableSignal} from '@angular/core';
import {BehaviorSubject, merge, Observable, ReplaySubject, Subject, takeUntil} from 'rxjs';
import {
  bufferToggle,
  distinctUntilChanged,
  filter,
  map,
  mergeMap,
  retry,
  startWith,
  tap,
  windowToggle
} from 'rxjs/operators';
import {QueryLanguage, QueryState} from '../query-panel/query-editor/query-editor-toolbar.component';
import {QueryResultInstanceSelectedEvent} from '../query-panel/result-display/BaseQueryResultComponent';
import {isNullOrUndefined} from '../utils/utils';
import {ActiveQueriesNotificationService, RunningQueryStatus} from './active-queries-notification-service';
import {AppConfig} from './app-info.service';
import {FailedSearchResponse, isFailedSearchResponse, isValueWithTypeName, StreamingQueryMessage} from './models';
import {QueryHistoryStoreService} from './query-history-store.service';
import {
  QueryProfileData,
  QueryResult,
  QueryService,
  randomId,
  ResultMode,
  StreamQueryErrorEvent
} from './query.service';
import {findType, InstanceLike, Schema, Type} from './schema';
import {SavedQueryWithSource} from "../project-import/schema-importer.service";

export type QueryEditorPayload = {
  queryLanguage: WritableSignal<QueryLanguage>
  query: WritableSignal<string>
  chatQuery: WritableSignal<string>
  savedQueryWithSource: WritableSignal<SavedQueryWithSource>
  currentState: WritableSignal<QueryState>
  queryClientId: WritableSignal<string>
  lastQueryResult: WritableSignal<QueryResult | FailedSearchResponse>
  queryReturnedResults: WritableSignal<boolean | null>
  queryStartTime: WritableSignal<Date | null>
  resultType: WritableSignal<Type>
  anonymousTypes: WritableSignal<Type[]>
  latestQueryStatus: WritableSignal<RunningQueryStatus>
  lastErrorMessage: WritableSignal<string>
  valuePanelVisible: WritableSignal<boolean>
  errorCount: WritableSignal<number>
  isErrorMessageSubscriptionSetup: WritableSignal<boolean>
  isQueryPaused: WritableSignal<boolean>
  // Observabley stuff...
  results: WritableSignal<ReplaySubject<InstanceLike>>
  potentiallyPausedResults: WritableSignal<Observable<InstanceLike>>
  errors: WritableSignal<ReplaySubject<StreamQueryErrorEvent>>
  queryProfileData: WritableSignal<Observable<QueryProfileData>>
  isProfileDataLoading: WritableSignal<Observable<boolean>>
  queryMetadata: WritableSignal<Observable<RunningQueryStatus>>
  // Use a replay subject, as sometimes the UI hasn't rendered at the time
  // when the event is emitted, but will subscribe shortly after
  instanceSelected: WritableSignal<ReplaySubject<QueryResultInstanceSelectedEvent>>
}

export class QueryEditorState {
  private activeQueryNotificationService = inject(ActiveQueriesNotificationService)
  private queryService = inject(QueryService)
  private queryHistoryStoreService = inject(QueryHistoryStoreService)

  private schema: Schema
  private config: AppConfig
  private readonly MAX_QUERY_RECORD_COUNT_DEFAULT: number = 5000;

  // Pause stream related stuff
  private pauseSubj$ = new BehaviorSubject(false);
  private pause$ = this.pauseSubj$.pipe(
    distinctUntilChanged(),
  );
  private on$ = this.pause$.pipe(filter(v=>!v));
  private off$ = this.pause$.pipe(filter(v=>!!v));

  private destroySubject: Subject<void> = new Subject();

  constructor(
    // TODO: rename to state
    public payload: QueryEditorPayload,
  ) {
  }

  submitQuery(schema: Schema, config: AppConfig) {
    this.schema = schema;
    this.payload.valuePanelVisible.set(false);
    switch (this.payload.queryLanguage()) {
      case 'Text':
        this.submitTextQuery();
        break;
      case 'TaxiQL':
        this.submitTaxiQlQuery();
        break;
    }
  }

  private prepareToSubmitQuery() {
    this.payload.currentState.set('Running');
    this.payload.queryStartTime.set(new Date());
    this.payload.lastQueryResult.set(null);
    this.payload.lastErrorMessage.set(null);
    this.payload.errorCount.set(0);
    this.payload.queryReturnedResults.set(false);
    this.payload.queryClientId.set(randomId());
    this.payload.resultType.set(null);
    this.payload.latestQueryStatus.set(null);
    this.payload.queryMetadata.set(null);
    this.payload.queryProfileData.set(null);
    // Note: using the MAX_QUERY_RECORD_COUNT_DEFAULT in case the server doesn't return a maxQueryRecordCount prop
    const bufferSize = this.config?.maxQueryRecordCount || this.MAX_QUERY_RECORD_COUNT_DEFAULT
    // Use a replay subject here, so that when users switch between
    // Query Results and Profiler tabs (and the query-editor tabs),
    // the results are still made available
    this.payload.results.set(new ReplaySubject<InstanceLike>(bufferSize))
    this.payload.errors.set(new ReplaySubject<StreamQueryErrorEvent>(bufferSize))

    // This bundle of Rx joy allows streaming queries to be paused
    // (the bufferToggle is in effect) and when un-paused, the windowToggle
    // allows the observable to return streaming results as normal
    this.payload.potentiallyPausedResults.set(merge(
      this.payload.results().pipe(
        bufferToggle(
          this.off$,
          () => this.on$
        ),
        mergeMap(x => x)
      ),
      this.payload.results().pipe(
        windowToggle(
          this.on$,
          () => this.off$
        ),
        mergeMap(x=> x)
      )
    ))
    this.toggleStreamPauseState(false);
  }

  private submitTaxiQlQuery() {
    this.prepareToSubmitQuery();

    const queryCompleteHandler = () => {
      this.handleQueryFinished();
    };

    const queryErrorHandler = (error: FailedSearchResponse) => {
      if (error instanceof CloseEvent) {
        console.warn("websocket closed", error)
        queryCompleteHandler();
        return;
      }

      this.payload.lastQueryResult.set(error);
      this.payload.isErrorMessageSubscriptionSetup.set(false);
      console.error('Search failed: ' + JSON.stringify(error));
      this.payload.currentState.set('Error');
      this.payload.lastErrorMessage.set(this.formatErrorMessage((this.payload.lastQueryResult() as FailedSearchResponse).message));
    };

    const queryMessageHandler = (message: StreamingQueryMessage) => {
      if (isFailedSearchResponse(message)) {
        queryErrorHandler(message);
      } else if (isValueWithTypeName(message)) {
        if (this.payload.queryMetadata() === null) {
          this.subscribeForQueryStatusUpdates(message.queryId);
        }
        this.payload.queryReturnedResults.set(true);
        if (!isNullOrUndefined(message.typeName)) {
          this.payload.anonymousTypes.set(message.anonymousTypes);
          this.payload.resultType.set(findType(this.schema, message.typeName, message.anonymousTypes));
        }
        this.payload.results().next(message);
      } else {
        console.error('Received an unexpected type of message from a query event stream: ' + JSON.stringify(message));
      }

    };

    this.queryService.websocketQuery(this.payload.query(), this.payload.queryClientId(), ResultMode.SIMPLE)
      .pipe(
        tap(_ => !this.payload.isErrorMessageSubscriptionSetup() ? this.setupErrorMessageSubscription() : null),
        takeUntil(this.destroySubject)
      )
      .subscribe({
        next: queryMessageHandler,
        error: queryErrorHandler,
        complete: queryCompleteHandler
      });
  }

  private subscribeForQueryStatusUpdates(queryId: string) {
    this.payload.queryMetadata.set(this.activeQueryNotificationService.getQueryStatusStreamForQueryId(
      queryId
    ).pipe(
      tap(message => {
        if (isNullOrUndefined(this.payload.latestQueryStatus())) {
          this.payload.latestQueryStatus.set(message);
        } else if (this.payload.latestQueryStatus().completedProjections < message.completedProjections || !message.running) {
          // We can receive messages out-of-order, because of how everything
          // executes in parallel.  Therefore, only update if this update moves us forward.
          this.payload.latestQueryStatus.set(message);
        }
      })
    ));
  }

  private submitTextQuery() {
    this.prepareToSubmitQuery();
    this.payload.currentState.set('Generating');
    this.queryService.textToQuery(this.payload.chatQuery())
      .subscribe(result => {
        this.payload.query.set(result.taxi);
        // Submit the taxiQL query.  Make sure parsingQuery = true, so we don't come
        // through this branch again,
        this.submitTaxiQlQuery();
      }, error => {
        console.log('Failed to parse ChatGPT query');
        console.log(error);
        this.payload.lastErrorMessage.set(`A problem occurred generating a query: ${error.error.message}`);
        this.payload.currentState.set('Error');
      });
  }

  cancelQuery() {
    const previousState = this.payload.currentState();
    this.payload.currentState.set('Cancelling');
    let cancelOperation$: Observable<void>;

    if (this.payload.latestQueryStatus()) {
      cancelOperation$ = this.queryService.cancelQuery(this.payload.latestQueryStatus().queryId);
    } else {
      cancelOperation$ = this.queryService.cancelQueryByClientQueryId(this.payload.queryClientId());
    }

    cancelOperation$.subscribe({
      next: () => {
        if (previousState === 'Running') {
          this.payload.currentState.set('Editing');
        } else {
          this.payload.currentState.set('Result');
        }
      },
      error: (error) => {
        console.log('Error occurred trying to cancel query: ' + JSON.stringify(error));
        this.payload.currentState.set('Editing');
      },
    });
  }

  private handleQueryFinished() {
    const currentState = this.payload.currentState();
    // If we're already in an error state, then don't change the state.
    if (currentState === 'Running' || currentState === 'Cancelling') {
      this.payload.currentState.set('Result');
      if (!this.payload.queryReturnedResults()) {
        this.payload.currentState.set('Error');
        this.payload.lastErrorMessage.set('No results matched your query');
      }
    }
    this.payload.isErrorMessageSubscriptionSetup.set(false);
    this.payload.queryProfileData.set(null);
    this.loadProfileData();
    this.queryHistoryStoreService.getHistory();
  }

  loadProfileData() {
    const currentState = this.payload.currentState();
    const isFinished = (currentState === 'Result' || currentState === 'Error');
    if (isFinished && !isNullOrUndefined(this.payload.queryProfileData())) {
      // We've already loaded the query profile data.  It won't be different, as
      // the query is finished, so no point in loading it again.
      return;
    }

    this.payload.queryProfileData.set(this.queryService.getQueryProfileFromClientId(this.payload.queryClientId()));
    this.payload.isProfileDataLoading.set(this.payload.queryProfileData().pipe(map(val => false), startWith(true)))
  }

  private setupErrorMessageSubscription() {
    this.payload.isErrorMessageSubscriptionSetup.set(true);
    this.queryService.getQueryErrors(this.payload.queryClientId())
      .pipe(
        retry({
            count: 3,
            delay: 250
          }
        ),
        takeUntil(this.destroySubject)
      )
      .subscribe(message => {
          this.payload.errorCount.update(count => count + 1);
          this.payload.errors().next(message)
        }
      )
  }

  private formatErrorMessage(val: string): string {
    return val?.replace("[Error]", "\n[Error]");
  }

  toggleStreamPauseState($event: boolean) {
    this.pauseSubj$.next($event);
  }

  destroy() {
    this.destroySubject.next();
  }
}
