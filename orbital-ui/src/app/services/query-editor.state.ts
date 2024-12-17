import {inject, Signal, WritableSignal} from '@angular/core';
import {ColumnState, FilterModel} from 'ag-grid-community';
import {IPosition} from 'monaco-editor';
import {BehaviorSubject, merge, Observable, of, ReplaySubject, Subject, takeUntil} from 'rxjs';
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
import {QueryState} from '../query-panel/query-editor/query-editor-toolbar/query-editor-toolbar.component';
import {QueryResultInstanceSelectedEvent} from '../query-panel/result-display/BaseQueryResultComponent';
import {isNullOrUndefined} from '../utils/utils';
import {ActiveQueriesNotificationService, RunningQueryStatus} from './active-queries-notification-service';
import {AppConfig} from './app-info.service';
import {FailedSearchResponse, isFailedSearchResponse, isValueWithTypeName, StreamingQueryMessage} from './models';
import {QueryHistoryStoreService} from './query-history-store.service';
import {
  ConversationMessage,
  QueryPlan,
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
  query: WritableSignal<string>
  lastChatGptText: WritableSignal<string>
  conversationMessages: WritableSignal<ConversationMessage[]>
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
  showMaxRecordCountWarning: WritableSignal<boolean>
  isQuerySaveable: WritableSignal<boolean>
  lastCursorPosition: WritableSignal<IPosition>
  agGridColumnState: WritableSignal<{columnState: ColumnState[], filterModel: FilterModel}>
  // Observabley stuff...
  results: WritableSignal<ReplaySubject<InstanceLike>>
  potentiallyPausedResults: WritableSignal<Observable<InstanceLike>>
  errors: WritableSignal<ReplaySubject<StreamQueryErrorEvent>>
  queryProfileData: WritableSignal<Observable<QueryProfileData>>
  queryPlanData: WritableSignal<Observable<QueryPlan>>
  isProfileDataLoading: WritableSignal<Observable<boolean>>
  queryMetadata: WritableSignal<Observable<RunningQueryStatus>>
  // Use a replay subject, as sometimes the UI hasn't rendered at the time
  // when the event is emitted, but will subscribe shortly after
  instanceSelected: WritableSignal<ReplaySubject<QueryResultInstanceSelectedEvent>>,
  // config
  config: Signal<AppConfig>
}

export type QueryLanguage = 'TaxiQL' | 'Text';

export class QueryEditorState {
  private activeQueryNotificationService = inject(ActiveQueriesNotificationService)
  private queryService = inject(QueryService)
  private queryHistoryStoreService = inject(QueryHistoryStoreService)

  private schema: Schema
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

  submitQuery(queryLanguage: QueryLanguage, schema: Schema) {
    this.schema = schema;
    this.payload.valuePanelVisible.set(false);
    switch (queryLanguage) {
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
    // Use a replay subject here, so that when users switch between
    // Query Results and Profiler tabs (and the query-editor tabs),
    // the results are still made available
    this.payload.results.set(new ReplaySubject<InstanceLike>())
    this.payload.errors.set(new ReplaySubject<StreamQueryErrorEvent>())

    // Use a ReplaySubject to allow for the user switching between the tabs in the tabbed-results UI
    const replayableResults = new ReplaySubject<any>();
    // This bundle of Rx joy allows streaming queries to be paused
    // (the bufferToggle is in effect) and when un-paused, the windowToggle
    // allows the observable to return streaming results as normal
    const pausableObservable = merge(
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
        mergeMap(x => x)
      )
    );
    // Subscribe to the merged observable and push values to the ReplaySubject
    pausableObservable.subscribe(value => replayableResults.next(value));
    this.payload.potentiallyPausedResults.set(replayableResults);
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
      this.payload.lastErrorMessage.set(
        this.formatErrorMessage((this.payload.lastQueryResult() as FailedSearchResponse).message)?.trim()
      );
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
          try {
            this.payload.resultType.set(findType(this.schema, message.typeName, message.anonymousTypes));
          } catch (e) {
            // TODO : A bug exists where union / intersection types aren't returned as anonymous types.
            // causing an error here.
            // Fix this as part of ORB-850
          }

        }
        this.payload.results().next(message);
      } else {
        console.error('Received an unexpected type of message from a query event stream: ' + JSON.stringify(message));
      }

    };

    let resultCount = 0;
    let maxResultExceeded = false;
    this.queryService.websocketQuery(this.payload.query(), this.payload.queryClientId(), ResultMode.SIMPLE)
      .pipe(
        tap(()=> {
          resultCount++;
          if (resultCount > (this.payload.config()?.analytics.maxQueryRecordCount || this.MAX_QUERY_RECORD_COUNT_DEFAULT) && !maxResultExceeded) {
            maxResultExceeded = true;
            console.log("results exceeded config.analytics.maxQueryRecordCount, pausing")
            this.toggleStreamPauseState(true)
            this.payload.showMaxRecordCountWarning.set(true)
          }
        }),
        takeUntil(this.destroySubject),
      )
      .subscribe({
        next: queryMessageHandler,
        error: queryErrorHandler,
        complete: queryCompleteHandler
      });
    if (!this.payload.isErrorMessageSubscriptionSetup()) {
      this.setupErrorMessageSubscription();
    }

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
    this.payload.currentState.set('Generating');
    const conversationMessage: ConversationMessage = {
      role: 'user',
      message: this.payload.lastChatGptText()
    }
    this.payload.conversationMessages.update(state => [...state, conversationMessage])
    // clean out any chunks and displayMessage props before sending to the server
    const sanitisedConversationMessages = this.payload.conversationMessages().map(message => {
      return {
        role: message.role,
        message: message.message
      }
    })
    this.queryService.textToQuery(sanitisedConversationMessages)
      .subscribe({
        next: results => {
          this.payload.conversationMessages.update(state => [...state, results])
          this.payload.currentState.set('Generated');
        },
        error: error => {
          console.log('Failed to parse ChatGPT query');
          console.log(error);
          this.payload.lastErrorMessage.set(`A problem occurred generating a query: ${error?.error?.message || error?.message || 'Unknown error'}`);
          this.payload.currentState.set('Error');
        }
      });
  }

  cancelQuery() {
    const previousState = this.payload.currentState();
    this.payload.currentState.set('Cancelling');
    this.payload.showMaxRecordCountWarning.set(false);
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

  compileQuery() {
    if (this.payload.query() === '') {
      this.payload.queryPlanData.set(of({steps: []} as QueryPlan))
      this.payload.isQuerySaveable.set(false)
    } else {
      this.queryService.compileQuery(this.payload.query())
        .subscribe({
          next: parsedQuery => {
            this.payload.isQuerySaveable.set(!parsedQuery.hasCompilationErrors)
            this.payload.queryPlanData.set(of(parsedQuery.queryPlan))
          },
          error: () => of({ steps: [] } as QueryPlan)
        })
    }
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
    this.payload.isQueryPaused.set($event)
    if (this.payload.showMaxRecordCountWarning()) {
      this.payload.showMaxRecordCountWarning.set(false)
    }
  }

  destroy() {
    this.destroySubject.next();
  }
}
