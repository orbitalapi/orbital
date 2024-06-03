import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  EventEmitter,
  Inject,
  Injector,
  Input,
  Output,
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {TuiAlertService, TuiDialogService, TuiNotification} from '@taiga-ui/core';
import {PolymorpheusComponent} from '@tinkoff/ng-polymorpheus';
import {editor, KeyCode, KeyMod} from 'monaco-editor';
import {QueryEditorPayload} from '../../services/query-editor.state';
import {QueryHistorySummary, QueryResult, QueryService} from '../../services/query.service';
import {QueryLanguage} from './query-editor-toolbar/query-editor-toolbar.component';
import {isQueryResult} from '../result-display/BaseQueryResultComponent';
import {QualifiedName, VersionedSource} from '../../services/schema';
import {ExportFormat, ResultsDownloadService} from 'src/app/results-download/results-download.service';
import {CopyQueryFormat} from 'src/app/query-panel/query-editor/QueryFormatter';
import {appendToQuery} from './query-code-generator';
import {SaveQueryDialogComponent, SaveQueryRequestProps} from './query-editor-toolbar/save-query-dialog.component';
import {SavedQuery} from '../../services/types.service';
import {
  EndpointType,
  PublishEndpointDialogComponent,
  PublishEndpointPanelProps
} from './query-editor-toolbar/publish-endpoint-dialog.component';
import {isNullOrUndefined} from "../../utils/utils";
import {
  CreateOrReplaceQuery,
  SavedQueryWithSource,
  SchemaEdit,
  SchemaImporterService
} from "../../project-import/schema-importer.service";

@Component({
  // eslint-disable-next-line @angular-eslint/component-selector
  selector: 'app-query-editor',
  templateUrl: './query-editor.component.html',
  styleUrls: ['./query-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QueryEditorComponent {
  // TODO: still think sending the actual store in here is better, despite it allowing access to
  //       addQueryEditorState and removeQueryEditorState. Would remove the need for the Outputs
  //       and wouldn't need to pass the schema in separately either. Essentially the signature of
  //       the component would be reliant completely on the store...
  //       Will also facilitate computed values far more easily, they can sit at the Store level
  //       instead of on the QueryEditorState where they're hard to access
  @Input()
  state: QueryEditorPayload;

  // KLUDGE: this is a computed prop from the QueryEditorStoreService which we need to pass down
  @Input()
  existingEndpointPaths: string[];


  @Output()
  queryChanged = new EventEmitter<{ query: string, chatQuery: string }>();

  @Output()
  queryLanguageChanged = new EventEmitter<QueryLanguage>();

  @Output()
  submitQuery = new EventEmitter<void>();

  @Output()
  cancelQuery = new EventEmitter<void>();

  @Output()
  pauseQuery = new EventEmitter<boolean>();

  @Output()
  loadProfileData = new EventEmitter<void>();

  @Output()
  onQuerySaved = new EventEmitter<SavedQueryWithSource>();

  @Output()
  onSavedQuerySelected = new EventEmitter<SavedQuery>();

  @Output()
  onCopyQuery = new EventEmitter<CopyQueryFormat>();

  readonly customActions: editor.IActionDescriptor[] = [
    {
      id: 'run-query',
      run: () => this.submitQuery.emit(),
      label: 'Execute query',
      keybindings: [
        KeyMod.CtrlCmd | KeyCode.Enter
      ],
      contextMenuGroupId: 'navigation',
      contextMenuOrder: 1.5
    }
  ];

  constructor(private queryService: QueryService,
              private fileService: ResultsDownloadService,
              @Inject(TuiDialogService) private readonly tuiDialogService: TuiDialogService,
              @Inject(Injector) private readonly injector: Injector,
              @Inject(TuiAlertService) private readonly alerts: TuiAlertService,
              private destroyRef: DestroyRef,
              private schemaImporterService: SchemaImporterService,
  ) {
  }

  public downloadQueryHistory(fileType: ExportFormat) {
    if (fileType === ExportFormat.TEST_CASE) {
      this.queryService.getHistorySummaryFromClientId(this.state.queryClientId())
        .subscribe(result => {
          this.fileService.promptToDownloadTestCase(result.queryId);
        });
    } else {
      this.fileService.downloadQueryHistoryFromClientQueryId(this.state.queryClientId(), fileType);
    }
  }

  // TODO Would like to make this computed, but this isn't easily
  //      achieved with the state prop, so wait to see how things pan
  //      out with moving the props/output fully into the store.
  get lastQueryResultAsSuccess(): QueryResult | null {
    if (isQueryResult(this.state.lastQueryResult())) {
      return this.state.lastQueryResult() as QueryResult;
    } else {
      return null;
    }
  }

  // TODO Would like to make this computed, but this isn't easily
  //      achieved with the state prop, so wait to see how things pan
  //      out with moving the props/output fully into the store.
  get isStreamingQuery(): boolean {
    return this.state.query()?.includes('stream {')
  }

  onAddToQueryClicked($event: QualifiedName) {
    this.state.query.set(appendToQuery(this.state.query(), $event));
  }

  createEndpoint(endpointType: EndpointType) {
    const existingEndpointPaths =
    this.tuiDialogService.open<SavedQueryWithSource>(new PolymorpheusComponent(PublishEndpointDialogComponent, this.injector),
      {
        size: 'l',
        data: {
          query: this.state.query(),
          previousVersion: this.state.savedQueryWithSource(),
          endpointType,
          queryKind: this.state.savedQueryWithSource().savedQuery.queryKind,
          existingEndpointPaths: this.existingEndpointPaths
        } as PublishEndpointPanelProps,
        dismissible: true
      }
    ).subscribe(result => {
      if (result) {
        this.onQuerySaved.emit(result)
        this.state.savedQueryWithSource.set(result)
        this.state.query.set(result.sourceFile.content);
      }
    });
  }

  saveQuery() {
    if (isNullOrUndefined(this.state.savedQueryWithSource())) {
      this.saveNewQuery();
    } else {
      this.saveExistingQuery();
    }
  }

  private saveNewQuery() {
    this.tuiDialogService.open<SavedQueryWithSource>(new PolymorpheusComponent(SaveQueryDialogComponent, this.injector),
      {
        size: 'l',
        data: {
          query: this.state.query(),
          previousVersion: this.state.savedQueryWithSource()
        } as SaveQueryRequestProps,
        dismissible: true
      }
    ).subscribe(result => {
      if (result) {
        this.onQuerySaved.emit(result)
        this.state.savedQueryWithSource.set(result)
        this.state.query.set(result.sourceFile.content);
      }
    });
  }

  private saveExistingQuery() {
    const versionedSource: VersionedSource = this.state.savedQueryWithSource().sourceFile
    const updatedSource: VersionedSource = {
      ...versionedSource,
      content: this.state.query()
    }
    const schemaEdit: SchemaEdit = {
      packageIdentifier: versionedSource.packageIdentifier,
      edits: [
        {
          editKind: 'CreateOrReplaceQuery',
          sources: [updatedSource]
        } as CreateOrReplaceQuery
      ],
      dryRun: false
    }
    this.schemaImporterService.submitSchemaEditOperation(schemaEdit)
      .subscribe({
        next: (result) => {
          this.alerts.open('Query saved successfully', {status: TuiNotification.Success})
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe()
          const updatedState = this.schemaImporterService.getQueryStateFromEditResult(result, versionedSource.name)
          this.state.query.set(updatedState.sourceFile.content)
          this.state.savedQueryWithSource.set(updatedState)
        },
        error: (error) => {
          console.error(error);
          this.alerts.open('An error occurred saving the query', {status: TuiNotification.Error})
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe()
        }
      })
  }

  queryHistoryElementClicked($event: QueryHistorySummary) {
    this.state.query.set($event.taxiQl);
    this.state.savedQueryWithSource.set(null);
  }

  onQueryChanged(query: string) {
    this.queryChanged.emit({query, chatQuery: this.state.chatQuery()});
  }

  onChatQueryChanged(chatQuery: string) {
    this.queryChanged.emit({query: this.state.query(), chatQuery});
  }
}
