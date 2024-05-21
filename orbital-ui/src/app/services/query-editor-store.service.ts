import { Clipboard } from '@angular/cdk/clipboard';
import { Inject, Injectable, Injector, signal, WritableSignal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TuiAlertService, TuiDialogService, TuiNotification } from '@taiga-ui/core';
import { PolymorpheusComponent } from '@tinkoff/ng-polymorpheus';
import { ReplaySubject } from 'rxjs';
import { QueryLanguage } from '../query-panel/query-editor/query-editor-toolbar.component';
import { copyQueryAs, CopyQueryFormat } from '../query-panel/query-editor/QueryFormatter';
import { QueryResultInstanceSelectedEvent } from '../query-panel/result-display/BaseQueryResultComponent';
import {
  CodeGenRequest,
  QuerySnippetContainerComponent
} from '../query-snippet-panel/query-snippet-container.component';
import { AppConfig, AppInfoService } from './app-info.service';
import { QueryEditorState } from './query-editor.state';
import { LocalStorageQuery } from './query-panel-store.service';
import { QueryService } from './query.service';
import { Schema } from './schema';
import { TypesService } from './types.service';

@Injectable()
export class QueryEditorStoreService {
  readonly queryEditorStates: WritableSignal<QueryEditorState[]> = signal([])
  readonly activeQueryEditorState: WritableSignal<QueryEditorState> = signal(null);

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
      .subscribe(schema => this.schema = schema);
  }


  addQueryEditorState(localStorageQuery: LocalStorageQuery) {
    this.queryEditorStates.update(tabs => {
      return [
        ...tabs,
        new QueryEditorState({
          queryLanguage: signal(localStorageQuery.queryLanguage),
          query: signal(localStorageQuery.query),
          chatQuery: signal(localStorageQuery.chatQuery),
          savedQuery: signal(localStorageQuery.savedQuery),
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
          // Observables/Subjects
          results: signal(null),
          potentiallyPausedResults: signal(null),
          errors: signal(null),
          queryProfileData: signal(null),
          isProfileDataLoading: signal(null),
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

  updateQueryLanguage($event: QueryLanguage) {
    this.activeQueryEditorState().payload.queryLanguage.set($event)
  }

  updateQuery(query: string, chatQuery: string) {
    this.activeQueryEditorState().payload.query.set(query);
    this.activeQueryEditorState().payload.chatQuery.set(chatQuery);
  }

  submitQuery() {
    this.activeQueryEditorState().submitQuery(this.schema, this.config)
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
