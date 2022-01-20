import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {MonacoEditorLoaderService} from '@materia-ui/ngx-monaco-editor';
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
import {Observable, Subject} from 'rxjs';
import {isNullOrUndefined} from 'util';
import {ActiveQueriesNotificationService, RunningQueryStatus} from '../../services/active-queries-notification-service';
import {TypesService} from '../../services/types.service';
import {ReplaySubject} from 'rxjs';
import {
  FailedSearchResponse,
  isFailedSearchResponse,
  isValueWithTypeName,
  StreamingQueryMessage
} from '../../services/models';
import ITextModel = editor.ITextModel;
import ICodeEditor = editor.ICodeEditor;

declare const monaco: any; // monaco

@Component({
  // eslint-disable-next-line @angular-eslint/component-selector
  selector: 'query-editor',
  templateUrl: './query-editor.component.html',
  styleUrls: ['./query-editor.component.scss']
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
  currentState: QueryState = 'Editing';

  @Output()
  queryResultUpdated = new EventEmitter<QueryResult | FailedSearchResponse>();
  @Output()
  loadingChanged = new EventEmitter<boolean>();

  @Output()
  instanceSelected = new EventEmitter<QueryResultInstanceSelectedEvent>();

  constructor(private monacoLoaderService: MonacoEditorLoaderService,
              private queryService: QueryService,
              private fileService: ExportFileService,
              private dialogService: MatDialog,
              private activeQueryNotificationService: ActiveQueriesNotificationService,
              private typeService: TypesService) {

    this.typeService.getTypes()
      .subscribe(schema => this.schema = schema);
    this.monacoLoaderService.isMonacoLoaded$.pipe(
      filter(isLoaded => isLoaded),
      take(1),
    ).subscribe(() => {
      monaco.editor.onDidCreateEditor(editorInstance => {
        editorInstance.updateOptions({readOnly: false, minimap: {enabled: false}});
        this.monacoEditor = editorInstance;
        this.remeasure();
      });
      monaco.editor.onDidCreateModel(model => {
        this.monacoModel = model;
        monaco.editor.defineTheme('vyne', {
          base: 'vs-dark',
          inherit: true,
          rules: [
            {token: '', background: '#333f54'},
          ],
          colors: {
            ['editorBackground']: '#333f54',
          }
        });
        monaco.editor.setTheme('vyne');
        monaco.editor.setModelLanguage(model, 'vyneQL');
      });
      monaco.languages.register({id: 'vyneQL'});
      monaco.languages.setLanguageConfiguration('vyneQL', vyneQueryLanguageConfiguration);
      monaco.languages.setMonarchTokensProvider('vyneQL', vyneQueryLanguageTokenProvider);
      // here, we retrieve monaco-editor instance

    });
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
    this.currentState = 'Running';
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
      this.currentState = 'Error';
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
    this.instanceSelected.emit($event);
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
    // If we're already in an error state, then don't change the state.
    if (this.currentState === 'Running' || this.currentState === 'Cancelling') {
      this.currentState = 'Result';
      if (!this.queryReturnedResults) {
        this.currentState = 'Error';
        this.lastErrorMessage = 'No results matched your query';
      }
    }
    this.queryProfileData$ = this.queryService.getQueryProfileFromClientId(this.queryClientId);

  }

  cancelQuery() {
    this.currentState = 'Cancelling';
    if (this.latestQueryStatus) {
      this.queryService.cancelQuery(this.latestQueryStatus.queryId)
        .subscribe();
    } else {
      this.queryService.cancelQueryByClientQueryId(this.queryClientId)
        .subscribe();
    }
  }

  onContentChanged(codeEditorContent: string) {
    this.query = codeEditorContent;
  }
}
