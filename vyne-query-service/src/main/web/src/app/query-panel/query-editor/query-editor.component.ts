import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {MonacoEditorLoaderService} from '@materia-ui/ngx-monaco-editor';
import {filter, take} from 'rxjs/operators';

import {editor} from 'monaco-editor';
import {
  QueryHistoryRecord,
  QueryResult,
  QueryService, randomId,
  ResponseStatus,
  ResultMode, ValueWithTypeName,
  VyneQlQueryHistoryRecord
} from 'src/app/services/query.service';
import {QueryFailure} from '../query-wizard/query-wizard.component';
import {HttpErrorResponse} from '@angular/common/http';
import {vyneQueryLanguageConfiguration, vyneQueryLanguageTokenProvider} from './vyne-query-language.monaco';
import {DownloadFileType} from '../result-display/result-container.component';
import {QueryState} from './bottom-bar.component';
import {
  isQueryFailure,
  isQueryResult,
  QueryResultInstanceSelectedEvent
} from '../result-display/BaseQueryResultComponent';
import {ExportFileService} from '../../services/export.file.service';
import ITextModel = editor.ITextModel;
import ICodeEditor = editor.ICodeEditor;
import {TestSpecFormComponent} from '../../test-pack-module/test-spec-form.component';
import {MatDialog} from '@angular/material/dialog';
import {findType, InstanceLike, Schema, Type} from '../../services/schema';
import {Subject} from 'rxjs/index';
import {Subscription} from 'rxjs';
import {isNullOrUndefined} from 'util';
import {RunningQueryStatus} from '../../services/active-queries-notification-service';
import {TypesService} from '../../services/types.service';

declare const monaco: any; // monaco

@Component({
  selector: 'query-editor',
  templateUrl: './query-editor.component.html',
  styleUrls: ['./query-editor.component.scss']
})
export class QueryEditorComponent implements OnInit {

  @Input()
  initialQuery: QueryHistoryRecord;

  editorOptions: { theme: 'vs-dark', language: 'vyneQL' };
  monacoEditor: ICodeEditor;
  monacoModel: ITextModel;
  query: string;
  queryClientId: string | null = null;
  lastQueryResult: QueryResult | QueryFailure;

  queryResults: InstanceLike[];

  partialResultType: Type | null = null;
  queryStatus: RunningQueryStatus | null = null;
  partialResults$: Subject<InstanceLike>;
  queryStatusSubscription: Subscription | null = null;
  queryResultsSubscription: Subscription | null = null;

  get showStreamingResults(): boolean {
    return !isNullOrUndefined(this.partialResultType);
  }

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
  queryResultUpdated = new EventEmitter<QueryResult | QueryFailure>();
  @Output()
  loadingChanged = new EventEmitter<boolean>();

  @Output()
  instanceSelected = new EventEmitter<QueryResultInstanceSelectedEvent>();

  constructor(private monacoLoaderService: MonacoEditorLoaderService,
              private queryService: QueryService,
              private fileService: ExportFileService,
              private dialogService: MatDialog,
              private typeService: TypesService) {

    this.typeService.getTypes()
      .subscribe(schema => this.schema = schema);
    this.monacoLoaderService.isMonacoLoaded.pipe(
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
    this.query = this.initialQuery ? (this.initialQuery as VyneQlQueryHistoryRecord).query : '';
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
    this.loading = true;
    this.loadingChanged.emit(true);
    this.queryClientId = randomId();
    this.partialResultType = null;
    this.partialResults$ = new Subject();
    this.queryStatus = null;

    this.queryResults = [];

    const queryResultHandler = (result: ValueWithTypeName) => {
      if (!isNullOrUndefined(result.typeName)) {
        this.partialResultType = findType(this.schema, result.typeName.parameterizedName);
      }
      this.partialResults$.next(result.value);
    };

    const queryErrorHandler = (error) => {
      this.loading = false;
      const errorResponse = error as HttpErrorResponse;
      if (errorResponse.error && (errorResponse.error as any).hasOwnProperty('profilerOperation')) {
        this.lastQueryResult = new QueryFailure(
          errorResponse.error.message,
          errorResponse.error.profilerOperation,
          errorResponse.error.remoteCalls);
      } else {
        // There was an unhandled error...
        console.error('An unhandled error occurred:');
        console.error(JSON.stringify(error));
        const errorMessage = 'Something went wrong - this looks like a bug in Vyne, not your query: '
          + errorResponse.message + '\n' + errorResponse.error.message;
        this.lastQueryResult = new QueryFailure(
          errorMessage,
          null, []);
        this.queryResultUpdated.emit(this.lastQueryResult);
      }
      this.queryResultUpdated.emit(this.lastQueryResult);
      this.loadingChanged.emit(false);
      this.currentState = 'Error';
      this.lastErrorMessage = this.lastQueryResult.message;
    };

    const queryCompleteHandler = () => {
      this.handleQueryFinished(null);
    };

    // Hard coded to test UI
    this.partialResultType = findType(this.schema, 'bgc.orders.Order');
    this.queryService.submitVyneQlQueryStreaming(this.query, this.queryClientId, ResultMode.SIMPLE).subscribe(
      queryResultHandler,
      queryErrorHandler,
      queryCompleteHandler);

  }

  onInstanceSelected($event: QueryResultInstanceSelectedEvent) {
    this.instanceSelected.emit($event);
  }

  public downloadQueryHistory(fileType: DownloadFileType) {
    const queryResponseId = (<QueryResult>this.lastQueryResult).queryResponseId;
    if (fileType === DownloadFileType.TEST_CASE) {
      const dialogRef = this.dialogService.open(TestSpecFormComponent, {
        width: '550px'
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result !== null) {
          // noinspection UnnecessaryLocalVariableJS
          const specName = result;
          this.fileService.downloadRegressionPackZipFile(queryResponseId, specName);
        }
      });
    } else {
      this.fileService.downloadQueryHistory(queryResponseId, fileType);
    }
  }

  private handleQueryFinished(queryStatus: RunningQueryStatus) {
    this.loading = false;
    this.loadingChanged.emit(false);
    this.currentState = 'Result';
      
    // this.queryService.getHistoryRecord(queryStatus.queryId)
    //   .subscribe(historyRecord => {
    //     this.lastQueryResult = historyRecord.response;
    //     this.queryResultUpdated.emit(this.lastQueryResult);
    //     this.loading = false;
    //     this.loadingChanged.emit(false);
    //     if (this.lastQueryResult.responseStatus === ResponseStatus.COMPLETED) {
    //       this.currentState = 'Result';
    //     } else if (this.lastQueryResult.responseStatus === ResponseStatus.INCOMPLETE) {
    //       this.currentState = 'Result';
    //     } else {
    //       this.currentState = 'Error';
    //       if (isQueryFailure(this.lastQueryResult)) {
    //         this.lastErrorMessage = this.lastQueryResult.message;
    //       }
    //     }
    //   });
  }

  cancelQuery() {
    this.queryService.cancelQuery(this.queryStatus.queryId)
      .subscribe();
  }

}
