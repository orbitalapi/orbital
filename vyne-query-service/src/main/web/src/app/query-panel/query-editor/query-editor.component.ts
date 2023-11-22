import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Inject,
  Injector,
  Input,
  OnInit,
  Output
} from '@angular/core';
import {tap} from 'rxjs/operators';

import {editor, KeyCode, KeyMod} from 'monaco-editor';
import {
  ChatParseResult,
  QueryHistorySummary,
  QueryProfileData,
  QueryResult,
  QueryService,
  randomId,
  ResultMode
} from '../../services/query.service';
import {QueryLanguage, QueryState} from './query-editor-toolbar.component';
import {isQueryResult, QueryResultInstanceSelectedEvent} from '../result-display/BaseQueryResultComponent';
import {MatLegacyDialog as MatDialog} from '@angular/material/legacy-dialog';
import {findType, InstanceLike, QualifiedName, Schema, Type, VersionedSource} from '../../services/schema';
import {BehaviorSubject, Observable, ReplaySubject, Subject} from 'rxjs';
import { isNullOrUndefined } from 'src/app/utils/utils';
import {ActiveQueriesNotificationService, RunningQueryStatus} from '../../services/active-queries-notification-service';
import {TypesService} from '../../services/types.service';
import {
  FailedSearchResponse,
  isFailedSearchResponse,
  isValueWithTypeName,
  StreamingQueryMessage
} from '../../services/models';
import {Router} from '@angular/router';
import {ExportFormat, ResultsDownloadService} from 'src/app/results-download/results-download.service';
import {copyQueryAs, CopyQueryFormat} from 'src/app/query-panel/query-editor/QueryFormatter';
import {Clipboard} from '@angular/cdk/clipboard';
import {
  CodeGenRequest,
  QuerySnippetContainerComponent
} from 'src/app/query-snippet-panel/query-snippet-container.component';
import {TuiDialogService} from '@taiga-ui/core';
import {PolymorpheusComponent} from '@tinkoff/ng-polymorpheus';
import {appendToQuery} from "./query-code-generator";
import {SaveQueryPanelComponent, SaveQueryPanelProps} from "./save-query-panel.component";
import {SavedQuery, SaveQueryRequest, TypeEditorService} from "../../services/type-editor.service";
import {MatLegacySnackBar as MatSnackBar} from "@angular/material/legacy-snack-bar";
import {HttpEndpointPanelComponent} from "./http-endpoint-panel.component";
import ITextModel = editor.ITextModel;
import ICodeEditor = editor.ICodeEditor;

declare const monaco: any; // monaco
@Component({
  // eslint-disable-next-line @angular-eslint/component-selector
  selector: 'query-editor',
  templateUrl: './query-editor.component.html',
  styleUrls: ['./query-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QueryEditorComponent implements OnInit {

  @Input()
  initialQuery: QueryHistorySummary;

  queryLanguage: QueryLanguage = 'TaxiQL';

  codeEditorTabIndex: number = 0;

  monacoEditor: ICodeEditor;
  monacoModel: ITextModel;
  chatQuery: string;
  query: string;
  queryClientId: string | null = null;
  lastQueryResult: QueryResult | FailedSearchResponse;
  queryReturnedResults: boolean | null = null;

  // queryResults: InstanceLike[];

  resultType: Type | null = null;
  anonymousTypes: Type[] = [];
  private latestQueryStatus: RunningQueryStatus | null = null;
  results$: Subject<InstanceLike>;
  queryProfileData$: Observable<QueryProfileData>;
  queryMetadata$: Observable<RunningQueryStatus>;

  customActions: editor.IActionDescriptor[];


  get lastQueryResultAsSuccess(): QueryResult | null {
    if (isQueryResult(this.lastQueryResult)) {
      return this.lastQueryResult;
    } else {
      return null;
    }
  }

  lastErrorMessage: string | null;

  loading = false;

  schema: Schema;
  currentState$: BehaviorSubject<QueryState> = new BehaviorSubject<QueryState>('Editing');

  valuePanelVisible: boolean = false;

  queryParseResult: ChatParseResult;

  @Output()
  queryResultUpdated = new EventEmitter<QueryResult | FailedSearchResponse>();
  @Output()
  loadingChanged = new EventEmitter<boolean>();

  // Use a replay subject, as sometimes the UI hasn't rendered at the time
  // when the event is emitted, but will subscribe shortly after
  @Output()
  instanceSelected$ = new ReplaySubject<QueryResultInstanceSelectedEvent>(1);

  savedQuery: SavedQuery = null;

  constructor(private queryService: QueryService,
              private fileService: ResultsDownloadService,
              private dialogService: MatDialog,
              private activeQueryNotificationService: ActiveQueriesNotificationService,
              private typeService: TypesService,
              private router: Router,
              private changeDetector: ChangeDetectorRef,
              private clipboard: Clipboard,
              @Inject(TuiDialogService) private readonly tuiDialogService: TuiDialogService,
              @Inject(Injector) private readonly injector: Injector,
              private editorService: TypeEditorService,
              private snackbarService: MatSnackBar
  ) {

    this.initialQuery = this.router.getCurrentNavigation()?.extras?.state?.query;
    this.typeService.getTypes()
      .subscribe(schema => this.schema = schema);
    this.customActions = [
      {
        id: 'run-query',
        run: () => this.submitQuery(),
        label: 'Execute query',
        keybindings: [
          KeyMod.CtrlCmd | KeyCode.Enter
        ],
        contextMenuGroupId: 'navigation',
        contextMenuOrder: 1.5
      }
    ];
  }

  ngOnInit(): void {
    this.query = this.initialQuery ? this.initialQuery.taxiQl : '';
  }

  remeasure() {
    setTimeout(() => {
      if (!this.monacoEditor) {
        return;
      }
      const editorDomNode = this.monacoEditor.getDomNode();
      const offsetHeightFixer = 20;
      if (editorDomNode) {
        const codeContainer = this.monacoEditor.getDomNode().getElementsByClassName('view-lines')[0] as HTMLElement;
        const calculatedHeight = codeContainer.offsetHeight + offsetHeightFixer + 'px';
        editorDomNode.style.height = calculatedHeight;
        const firstParent = editorDomNode.parentElement;
        firstParent.style.height = calculatedHeight;
        const secondParent = firstParent.parentElement;
        secondParent.style.height = calculatedHeight;
        console.log('Resizing Monaco editor to ' + calculatedHeight);
        this.monacoEditor.layout();
      }
    }, 10);
  }

  submitQuery() {
    switch (this.queryLanguage) {
      case 'Text':
        this.submitTextQuery();
        break;
      case 'TaxiQL':
        this.submitTaxiQlQuery();
        break;
    }

  }

  saveQuery() {
    if (this.savedQuery == null) {
      this.saveNewQuery();
    } else {
      this.saveExistingQuery();
    }
  }

  private prepareToSubmitQuery() {
    this.currentState$.next('Running');
    this.lastQueryResult = null;
    this.lastErrorMessage = null;
    this.queryReturnedResults = false;
    this.loading = true;
    this.loadingChanged.emit(true);
    this.queryClientId = randomId();
    this.resultType = null;
    // Use a replay subject here, so that when people switch
    // between Query Results and Profiler tabs, the results are still made available
    this.results$ = new ReplaySubject(5000);
    this.latestQueryStatus = null;
    this.queryMetadata$ = null;
    this.queryProfileData$ = null;

    this.changeDetector.markForCheck();
  }

  private submitTaxiQlQuery() {

    this.prepareToSubmitQuery();

    const queryErrorHandler = (error: FailedSearchResponse) => {
      this.loading = false;
      this.lastQueryResult = error;
      console.error('Search failed: ' + JSON.stringify(error));
      this.queryResultUpdated.emit(this.lastQueryResult);
      this.loadingChanged.emit(false);
      this.currentState$.next('Error');
      this.lastErrorMessage = this.lastQueryResult.message;
    };

    const queryMessageHandler = (message: StreamingQueryMessage) => {
      if (isFailedSearchResponse(message)) {
        queryErrorHandler(message);
      } else if (isValueWithTypeName(message)) {
        this.queryReturnedResults = true;
        if (!isNullOrUndefined(message.typeName)) {
          this.anonymousTypes = message.anonymousTypes;
          this.resultType = findType(this.schema, message.typeName, message.anonymousTypes);
        }
        this.results$.next(message);
        if (this.queryMetadata$ === null) {
          this.subscribeForQueryStatusUpdates(message.queryId);
        }
      } else {
        console.error('Received an unexpected type of message from a query event stream: ' + JSON.stringify(message));
      }

    };


    const queryCompleteHandler = () => {
      this.handleQueryFinished();
    };

    this.queryService.websocketQuery(this.query, this.queryClientId, ResultMode.SIMPLE).subscribe(
      queryMessageHandler,
      queryCompleteHandler,
      queryCompleteHandler);
  }

  private subscribeForQueryStatusUpdates(queryId: string) {
    this.queryMetadata$ = this.activeQueryNotificationService.getQueryStatusStreamForQueryId(
      queryId
    ).pipe(
      tap(message => {
        if (isNullOrUndefined(this.latestQueryStatus)) {
          this.latestQueryStatus = message;
        } else if (this.latestQueryStatus.completedProjections < message.completedProjections || !message.running) {
          // We can receive messages out-of-order, because of how everything
          // executes in parallel.  Therefore, only update if this update moves us forward.
          this.latestQueryStatus = message;
        }
      })
    );
  }

  onInstanceSelected($event: QueryResultInstanceSelectedEvent) {
    this.instanceSelected$.next($event);
  }

  public downloadQueryHistory(fileType: ExportFormat) {
    if (fileType === ExportFormat.TEST_CASE) {
      this.queryService.getHistorySummaryFromClientId(this.queryClientId)
        .subscribe(result => {
          this.fileService.promptToDownloadTestCase(result.queryId);
        });
    } else {
      this.fileService.downloadQueryHistoryFromClientQueryId(this.queryClientId, fileType);
    }
  }

  private handleQueryFinished() {
    this.loading = false;
    this.loadingChanged.emit(false);
    const currentState = this.currentState$.getValue();
    // If we're already in an error state, then don't change the state.
    if (currentState === 'Running' || currentState === 'Cancelling') {
      this.currentState$.next('Result');
      if (!this.queryReturnedResults) {
        this.currentState$.next('Error');
        this.lastErrorMessage = 'No results matched your query';
      }
    }
    this.queryProfileData$ = null;
    this.loadProfileData();

  }

  cancelQuery() {
    const previousState = this.currentState$.getValue();
    this.currentState$.next('Cancelling');
    let cancelOperation$: Observable<void>;

    if (this.latestQueryStatus) {
      cancelOperation$ = this.queryService.cancelQuery(this.latestQueryStatus.queryId);
    } else {
      cancelOperation$ = this.queryService.cancelQueryByClientQueryId(this.queryClientId);
    }

    cancelOperation$.subscribe(next => {
      if (previousState === 'Running') {
        this.currentState$.next('Editing');
      } else {
        this.currentState$.next('Result');
      }
    }, error => {
      console.log('Error occurred trying to cancel query: ' + JSON.stringify(error));
      this.currentState$.next('Editing');
    });
  }


  loadProfileData() {
    const currentState = this.currentState$.getValue();
    const isFinished = (currentState === 'Result' || currentState === 'Error');
    if (isFinished && !isNullOrUndefined(this.queryProfileData$)) {
      // We've alreaded loaded the query profile data.  It won't be different, as
      // the query is finished, so no point in loading it again.
      return;
    }

    this.queryProfileData$ = this.queryService.getQueryProfileFromClientId(this.queryClientId);
    this.changeDetector.markForCheck();
  }

  copyQuery($event: CopyQueryFormat) {

    if ($event === 'snippet') {
      this.tuiDialogService.open(
        new PolymorpheusComponent(QuerySnippetContainerComponent, this.injector),
        {
          size: 'l',
          data: {
            query: this.query,
            returnType: this.resultType,
            schema: this.schema,
            anonymousTypes: this.anonymousTypes
          } as CodeGenRequest,
          dismissible: true
        }
      ).subscribe();
    } else {
      copyQueryAs(this.query, this.queryService.queryEndpoint, $event, this.clipboard);
    }
  }

  onAddToQueryClicked($event: QualifiedName) {
    this.query = appendToQuery(this.query, $event);
  }

  createHttpEndpoint() {
    this.tuiDialogService.open<string>(new PolymorpheusComponent(HttpEndpointPanelComponent, this.injector),
      {
        size: 'l',
        data: this.query,
        dismissible: true
      }
    ).subscribe(result => {
      if (result !== null) {
        this.query = result;
      }
      this.changeDetector.markForCheck();
    });
  }

  private submitTextQuery() {
    this.prepareToSubmitQuery();
    this.queryParseResult = null;
    this.currentState$.next('Generating');
    this.queryService.textToQuery(this.chatQuery)
      .subscribe(result => {
        this.query = result.taxi;
        this.queryParseResult = result;
        // Submit the taxiQL query.  Make sure parsingQuery = true, so we don't come
        // through this branch again,
        this.submitTaxiQlQuery();
      }, error => {
        console.log('Failed to parse ChatGPT query');
        console.log(error);
        this.lastErrorMessage = 'A problem occurred parsing the text to a query';
        this.currentState$.next('Error');
      });
  }

  private saveExistingQuery() {
    const updatedSource: VersionedSource = {
      ...this.savedQuery.sources[0],
      content: this.query,
    }
    const request: SaveQueryRequest = {
      source: updatedSource,
      changesetName: ''
    }
    this.editorService.saveQuery(request)
      .subscribe(
        result => {
          this.snackbarService.open('Query saved successfully');
          this.savedQuery = result;
        },
        error => {
          console.error(error);
          this.snackbarService.open('An error occurred saving the query')
        }
      )
  }

  queryHistoryElementClicked($event: QueryHistorySummary) {
    this.query = $event.taxiQl;
  }

  private saveNewQuery() {
    this.tuiDialogService.open<SavedQuery>(new PolymorpheusComponent(SaveQueryPanelComponent, this.injector),
      {
        size: 'l',
        data: {
          query: this.query,
          previousVersion: this.savedQuery
        } as SaveQueryPanelProps,
        dismissible: true
      }
    ).subscribe(result => {
      this.onSavedQuerySelected(result);
      // this.savedQuery = result;
      // this.query = result.sources[0].content;
      // this.changeDetector.markForCheck();
    });
  }

  onSavedQuerySelected(selectedQuery: SavedQuery) {
    this.savedQuery = selectedQuery;
    this.query = selectedQuery.sources[0].content;
    this.changeDetector.markForCheck();
  }
}
