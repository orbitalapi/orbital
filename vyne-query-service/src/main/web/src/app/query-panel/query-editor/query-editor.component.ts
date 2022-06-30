import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output
} from '@angular/core';
import {filter, take, tap} from 'rxjs/operators';

import {editor} from 'monaco-editor';
import {
  QueryHistorySummary,
  QueryProfileData,
  QueryResult,
  QueryService,
  randomId,
  ResultMode,
} from '../../services/query.service';
import {vyneQueryLanguageConfiguration, vyneQueryLanguageTokenProvider} from './vyne-query-language.monaco';
import {QueryState} from './bottom-bar.component';
import {isQueryResult, QueryResultInstanceSelectedEvent} from '../result-display/BaseQueryResultComponent';
import {ExportFileService, ExportFormat} from '../../services/export.file.service';
import {MatDialog} from '@angular/material/dialog';
import {findType, InstanceLike, Schema, Type} from '../../services/schema';
import {BehaviorSubject, Observable, ReplaySubject, Subject} from 'rxjs';
import {isNullOrUndefined} from 'util';
import {ActiveQueriesNotificationService, RunningQueryStatus} from '../../services/active-queries-notification-service';
import {TypesService} from '../../services/types.service';
import {
  FailedSearchResponse,
  isFailedSearchResponse,
  isValueWithTypeName,
  StreamingQueryMessage
} from '../../services/models';
import {Router} from '@angular/router';
import ITextModel = editor.ITextModel;
import ICodeEditor = editor.ICodeEditor;

declare const monaco: any; // monaco

@Component({
  // eslint-disable-next-line @angular-eslint/component-selector
  selector: 'query-editor',
  templateUrl: './query-editor.component.html',
  styleUrls: ['./query-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QueryEditorComponent implements OnInit {

  @Input()
  initialQuery: QueryHistorySummary;

  monacoEditor: ICodeEditor;
  monacoModel: ITextModel;
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

  @Output()
  queryResultUpdated = new EventEmitter<QueryResult | FailedSearchResponse>();
  @Output()
  loadingChanged = new EventEmitter<boolean>();

  // Use a replay subject, as sometimes the UI hasn't rendered at the time
  // when the event is emitted, but will subscribe shortly after
  @Output()
  instanceSelected$ = new ReplaySubject<QueryResultInstanceSelectedEvent>(1);

  constructor(private queryService: QueryService,
              private fileService: ExportFileService,
              private dialogService: MatDialog,
              private activeQueryNotificationService: ActiveQueriesNotificationService,
              private typeService: TypesService,
              private router: Router,
              private changeDetector: ChangeDetectorRef
  ) {

    this.initialQuery = this.router.getCurrentNavigation()?.extras?.state?.query;
    this.typeService.getTypes()
      .subscribe(schema => this.schema = schema);
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
    this.currentState$.next('Running');
    this.lastQueryResult = null;
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

    this.queryService.submitVyneQlQueryStreaming(this.query, this.queryClientId, ResultMode.SIMPLE).subscribe(
      queryMessageHandler,
      queryErrorHandler,
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
    const currentState = this.currentState$.getValue()
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
      cancelOperation$ = this.queryService.cancelQuery(this.latestQueryStatus.queryId)
    } else {
      cancelOperation$ = this.queryService.cancelQueryByClientQueryId(this.queryClientId)
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
    })
  }


  loadProfileData() {
    const currentState = this.currentState$.getValue();
    const isFinished = (currentState === "Result" || currentState === 'Error')
    if (isFinished && !isNullOrUndefined(this.queryProfileData$)) {
      // We've alreaded loaded the query profile data.  It won't be different, as
      // the query is finished, so no point in loading it again.
      return;
    }

    this.queryProfileData$ = this.queryService.getQueryProfileFromClientId(this.queryClientId);
    this.changeDetector.detectChanges();
  }
}
