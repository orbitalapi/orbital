import {Clipboard} from '@angular/cdk/clipboard';
import {computed, Inject, Injectable, Injector, Signal, signal, WritableSignal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {TuiAlertService, TuiDialogService, TuiNotification} from '@taiga-ui/core';
import {PolymorpheusComponent} from '@tinkoff/ng-polymorpheus';
import {ReplaySubject} from 'rxjs';
import {copyQueryAs, CopyQueryFormat} from '../query-panel/query-editor/QueryFormatter';
import {QueryResultInstanceSelectedEvent} from '../query-panel/result-display/BaseQueryResultComponent';
import {
  CodeGenRequest,
  QuerySnippetContainerComponent
} from '../query-snippet-panel/query-snippet-container.component';
import {AppConfig, AppInfoService} from './app-info.service';
import {QueryEditorState} from './query-editor.state';
import {LocalStorageQuery} from './query-panel-store.service';
import {QueryService} from './query.service';
import {Schema} from './schema';
import {TypesService} from './types.service';

@Injectable({
  providedIn: 'root'
})
export class QueryEditorStoreService {
  readonly queryEditorStates: WritableSignal<QueryEditorState[]> = signal([])
  readonly activeQueryEditorState: WritableSignal<QueryEditorState> = signal(null);
  readonly existingQueryEndpoints: Signal<string[]> = computed(() => {
    return this.queryEditorStates().reduce((accum, query) => {
      const url = query.payload.savedQueryWithSource()?.savedQuery.httpEndpoint?.url;
      const path = query.payload.savedQueryWithSource()?.savedQuery.websocketOperation?.path;
      if (url) accum.push(url)
      if (path) accum.push(path)
      return accum
    }, [])
  })
  readonly existingSavedQueryNames: Signal<string[]> = computed(() => {
    return this.queryEditorStates().reduce((accum, query) => {
      const queryName = query.payload.savedQueryWithSource()?.savedQuery.name.shortDisplayName;
      if (queryName) accum.push(queryName)
      return accum
    }, [])
  })

  schema: Schema

  private config: AppConfig;

  constructor(
    private appInfoService: AppInfoService,
    private typeService: TypesService,
    private queryService: QueryService,
    private clipboard: Clipboard,
    @Inject(TuiDialogService) private readonly tuiDialogService: TuiDialogService,
    @Inject(Injector) private readonly injector: Injector,
    @Inject(TuiAlertService) private readonly alerts: TuiAlertService,
  ) {
    appInfoService.getConfig()
      .pipe(takeUntilDestroyed())
      .subscribe(next => this.config = next);

    typeService.getTypes()
      .pipe(takeUntilDestroyed())
      .subscribe(schema => {
        this.schema = schema;
        this.compileQuery();
      });
  }

  addQueryEditorState(localStorageQuery: LocalStorageQuery) {
    this.queryEditorStates.update(tabs => {
      return [
        ...tabs,
        new QueryEditorState({
          query: signal(localStorageQuery.query),
          lastChatGptText: signal(''),
          conversationMessages: signal(localStorageQuery.conversationMessages),
          savedQueryWithSource: signal(localStorageQuery.savedQueryWithSource),
          currentState: signal('Editing'),
          queryClientId: signal(null),
          lastQueryResult: signal(null),
          queryReturnedResults: signal(null),
          queryStartTime: signal(null),
          resultType: signal(null),
          anonymousTypes: signal([]),
          latestQueryStatus: signal(null),
          lastErrorMessage: signal(null),
          valuePanelVisible: signal(false),
          errorCount: signal(0),
          isErrorMessageSubscriptionSetup: signal(false),
          isQueryPaused: signal(false),
          isQuerySaveable: signal(false),
          // Observables/Subjects
          results: signal(null),
          potentiallyPausedResults: signal(null),
          errors: signal(null),
          queryProfileData: signal(null),
          isProfileDataLoading: signal(null),
          queryPlanData: signal(null),
          queryMetadata: signal(null),
          instanceSelected: signal(new ReplaySubject<QueryResultInstanceSelectedEvent>(1)),
        })
      ]
    })
  }

  removeQueryEditorState(index: number) {
    this.queryEditorStates()[index].destroy()
    const clonedQueries = this.queryEditorStates().slice();
    clonedQueries.splice(index, 1)
    this.queryEditorStates.set(clonedQueries)
  }

  updateQuery(query: string) {
    this.activeQueryEditorState().payload.query.set(query);
    this.compileQuery();
  }

  updateLastChatGptText(lastChatGptText: string) {
    this.activeQueryEditorState().payload.lastChatGptText.set(lastChatGptText);
  }

  resetConversationMessages() {
    this.activeQueryEditorState().payload.conversationMessages.set([]);
  }

  updateActiveQueryEditorStateIndex(index: number) {
    this.activeQueryEditorState.set(this.queryEditorStates()[index])
  }

  submitQuery() {
    this.activeQueryEditorState().submitQuery('TaxiQL', this.schema)
  }

  submitTextToChatGpt() {
    this.activeQueryEditorState().submitQuery('Text', this.schema)
    this.activeQueryEditorState().payload.lastChatGptText.set('');
  }

  cancelQuery() {
    this.activeQueryEditorState().cancelQuery()
    this.pauseQuery(false)
  }

  pauseQuery(value: boolean) {
    this.activeQueryEditorState().payload.isQueryPaused.set(value)
    this.activeQueryEditorState().toggleStreamPauseState(value)
  }

  loadProfileData() {
    this.activeQueryEditorState().loadProfileData()
  }

  compileQuery() {
    this.activeQueryEditorState().compileQuery()
  }

  copyQuery($event: CopyQueryFormat) {
    if ($event === 'snippet') {
      this.tuiDialogService.open(
        new PolymorpheusComponent(QuerySnippetContainerComponent, this.injector),
        {
          size: 'l',
          data: {
            query: this.activeQueryEditorState().payload.query(),
            returnType: this.activeQueryEditorState().payload.resultType(),
            schema: this.schema,
            anonymousTypes: this.activeQueryEditorState().payload.anonymousTypes()
          } as CodeGenRequest,
          dismissible: true
        }
      ).subscribe();
    } else {
      copyQueryAs(this.activeQueryEditorState().payload.query(), this.queryService.queryEndpoint, $event, this.clipboard);
      this.alerts.open('Copied to clipboard', {status: TuiNotification.Success})
        .subscribe()
    }
  }
}
