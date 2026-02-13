import {
  ChangeDetectionStrategy, ChangeDetectorRef,
  Component,
  DestroyRef, effect,
  ElementRef,
  EventEmitter, Inject, input,
  Input,
  OnDestroy, OnInit,
  Output,
  ViewChild
} from "@angular/core";
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {TuiAlertService} from '@taiga-ui/core';
import {IOutputData} from 'angular-split';
import {debounceTime, filter, skip} from 'rxjs/operators';
import {editor, IPosition, MarkerSeverity} from 'monaco-editor';
import {SchemaNotificationService} from '../services/schema-notification.service';
import {createTaxiEditor, createTaxiEditorModel} from "./language-server-commons";
import {ITextFileEditorModel} from "@codingame/monaco-vscode-api/monaco";
import {DiagnosticSeverity, DidOpenTextDocumentNotification} from "vscode-languageclient";
import {MonacoLanguageClient} from "monaco-languageclient";
import {
  IStandaloneCodeEditor
} from "@codingame/monaco-vscode-api/vscode/vs/editor/standalone/browser/standaloneCodeEditor";
import {buildWorkerDefinition} from 'monaco-editor-workers';
import {MonacoLanguageServerService} from "./language-server.service";
import {CompilationMessage, CompilationMessageSeverity} from "../services/schema";
import {isNullOrUndefined} from "../utils/utils";
import IModelContentChangedEvent = editor.IModelContentChangedEvent;
import IMarkerData = editor.IMarkerData;

buildWorkerDefinition('./assets/monaco-editor-workers/workers', window.location.origin, false);

type WordWrapOptions = 'off' | 'on' | 'wordWrapColumn' | 'bounded';

@Component({
  selector: 'app-code-editor',
  styleUrls: ['./code-editor.component.scss'],
  template: `
    <as-split
      gutterSize="7"
      gutterStep="5"
      direction="vertical"
      unit="pixel"
      useTransition="true"
      (dragEnd)="onDragEnd($event)"
    >
      <as-split-area size="*">
        <div #codeEditorContainer class="code-editor"></div>
      </as-split-area>
      <as-split-area [size]="errorPanelSize" minSize="42" *ngIf="showCompilationProblemsPanel">
        <app-compilation-message-list [(expanded)]="compilationProblemsPanelExpanded" [compilationMessages]="compilationMessages"></app-compilation-message-list>
      </as-split-area>
    </as-split>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CodeEditorComponent implements OnInit, OnDestroy {

  private languageClient: MonacoLanguageClient;
  private monacoEditor: IStandaloneCodeEditor;
  private monacoModel: ITextFileEditorModel;

  private _codeEditorContainer: ElementRef;
  @ViewChild('codeEditorContainer')
  get codeEditorContainer(): ElementRef {
    return this._codeEditorContainer;
  }

  compilationProblemsPanelExpanded = false;

  set codeEditorContainer(value: ElementRef) {
    this._codeEditorContainer = value;
  }

  private _actions: editor.IActionDescriptor[] = [];
  @Input()
  get actions(): editor.IActionDescriptor[] {
    return this._actions;
  }

  set actions(value) {
    if (this._actions === value) {
      return;
    }
    this._actions = value;
    if (this.monacoEditor) {
      this.updateActionsOnEditor();
    }
  }

  @Input()
  showCompilationProblemsPanel: boolean = false;

  private _compilationMessages: CompilationMessage[];
  /**
   * Set only when not using a language server, and wish
   * to manually provide compiler messages
   */
  @Input()
  get compilationMessages(): CompilationMessage[] {
    return this._compilationMessages
  }

  set compilationMessages(value) {
    this._compilationMessages = value;
    this.updateManualCompilationMessages()
  }

  @Output()
  compilationMessagesUpdated = new EventEmitter<CompilationMessage[]>();

  // private editorTheme = iplastic_theme;
  //
  private _readOnly: boolean = false;

  @Input()
  get readOnly(): boolean {
    return this._readOnly;
  }

  set readOnly(value: boolean) {
    if (value === this._readOnly) {
      return;
    }
    this._readOnly = value;
    if (this.monacoEditor) {
      this.monacoEditor.updateOptions({readOnly: this.readOnly});
    }
  }

  private _wordWrap: WordWrapOptions = 'off';

  @Input()
  get wordWrap(): WordWrapOptions {
    return this._wordWrap;
  }

  set wordWrap(value: WordWrapOptions) {
    if (value === this._wordWrap) {
      return;
    }
    this._wordWrap = value;
    if (this.monacoEditor) {
      this.monacoEditor.updateOptions({wordWrap: this.wordWrap});
    }
  }

  //
  private _content: string = '';
  @Input()
  get content(): string {
    return this._content;
  }

  set content(value: string) {
    if (this._content === value) {
      return;
    }
    this._content = value;
    if (this.monacoModel) {
      this.monacoModel.textEditorModel.setValue(value);
      this.contentChange.emit(value);
    }
  }

  /**
   * Optional filename/path for the file being edited.
   * When provided, this will be used as the URI for the Monaco model,
   * allowing the language server to recognize it as an existing project file.
   * When not provided, a random sandbox filename will be generated.
   */
  @Input()
  filename?: string;

  private modelChanged$ = new EventEmitter<IModelContentChangedEvent>();

  @Output()
  contentChange = new EventEmitter<string>();

  cursorPosition = input<IPosition>()
  setFocus = input<boolean>(true)

  @Output()
  cursorPositionChanged = new EventEmitter<IPosition>();

  lastCompilationProblemsPanelHeight = 135;
  get errorPanelSize(): number {
    return this.compilationProblemsPanelExpanded ? this.lastCompilationProblemsPanelHeight : 42;
  }

  constructor(
    private languageServerService: MonacoLanguageServerService,
    private schemaNotificationService: SchemaNotificationService,
    private destroyRef: DestroyRef,
    private changeDetectorRef: ChangeDetectorRef,
    @Inject(TuiAlertService) private readonly alertService: TuiAlertService,
  ) {
    this.languageServerService.languageServicesInit$
      .subscribe(() => {
        // editor.defineTheme('vyne', this.editorTheme as any);
        // editor.setTheme('vyne');
      })
    this.modelChanged$.pipe(
      debounceTime(250),
      takeUntilDestroyed()
    ).subscribe(async(e) => {
      this.updateContent(this.monacoModel.textEditorModel.getValue());
    })

    this.schemaNotificationService.createSchemaNotificationsSubscription()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        skip(1),
        debounceTime(300)
      )
      .subscribe(async () => {
        await this.sendOpenNotification();
      });

    effect(() => {
      if (this.cursorPosition()) {
        this.monacoEditor?.setPosition(this.cursorPosition())
      }
      if (this.setFocus()) {
        this.monacoEditor?.focus()
      }
    });
  }

  async ngOnInit() {
    await this.createMonacoEditor()
  }

  async ngOnDestroy() {
    if (this.readOnly) return;
    console.info('Closing Language Service and disposing of model');
    try {
      this.monacoModel.dispose()
      this.monacoEditor.dispose();
      // await this.languageClient.dispose();
    } catch (error) {
      // Best as I can tell, the error that's occurring here is innocuous
      // and the languageClient has finished shutting down correctly
      //console.error(error)
    }
  }

  private async createMonacoEditor() {
    if (this.monacoEditor) {
      this.monacoEditor.dispose();
    }

    this.languageClient = await this.languageServerService.getLanguageClient();

    const {modelRef, model} = await this.createNewMonacoModel();
    this.monacoModel = model;
    const diagnosticsEvents = await this.languageServerService.getDiagnostics$();
    const modelUri = model.textEditorModel.uri.toString();
    diagnosticsEvents.pipe(
      takeUntilDestroyed(this.destroyRef),
      filter(event => {
        // For read-only files, match against the real filename (not the random sandbox URI)
        // For editable files, match against the model URI
        if (this.readOnly && this.filename) {
          // Strip /web/sandbox prefix from filename if present for comparison
          const realFilename = this.filename.replace(/^\/web\/sandbox/, '');
          const eventUri = event.uri.replace(/^file:\/\//, '');
          return eventUri === realFilename || event.uri === this.filename;
        }
        // For editable files, use model URI
        return event.uri === modelUri;
      }),
      debounceTime(100)
    ).subscribe(next => {
      this.compilationMessages = next.diagnostics.map(message => {
        let severity:CompilationMessageSeverity
        if (message.severity == DiagnosticSeverity.Error) {
          severity = "ERROR"
        } else if (message.severity == DiagnosticSeverity.Warning) {
          severity = "WARNING"
        } else {
          severity = "INFO"
        }
        return {
          char: message.range.start.character,
          line: message.range.start.line + 1,
          sourceName: message.source,
          severity: severity,
          detailMessage: message.message
        } as CompilationMessage;
      })
      this.compilationMessagesUpdated.emit(this.compilationMessages)
      this.changeDetectorRef.markForCheck();
    })

    this.monacoEditor = await createTaxiEditor(this.codeEditorContainer.nativeElement, modelRef)
    this.monacoEditor.updateOptions({readOnly: this.readOnly});
    if (this.cursorPosition()) {
      this.monacoEditor.setPosition(this.cursorPosition())
    }
    this.monacoEditor.onDidChangeCursorPosition((event) => {
      if (event.source !== 'model') {
        this.cursorPositionChanged.emit(event.position)
      }
    })
    if (this.setFocus() && !this.readOnly) {
      this.monacoEditor.focus()
    }

    if (!isNullOrUndefined(this.compilationMessages)) {
      this.updateManualCompilationMessages();
    }

    await this.sendOpenNotification();

    this.languageServerService.websocketClosed$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(async (closeEvent) => this.reconnect(closeEvent))

    this.languageServerService.websocketTerminallyClosed$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(async (closeEvent) => {
        this.alertService
          .open('Server disconnected, please refresh the browser to reconnect',
            {appearance: 'warning', autoClose: 0, closeable: false }
          )
          .subscribe()
      })

    this.updateActionsOnEditor()
  }

  private async sendOpenNotification() {
    const modelUri = this.monacoModel.resource.toString();
    // Strip /web/sandbox prefix for the language server if present
    // Monaco uses /web/sandbox/ to avoid file system writes, but the language server
    // knows files by their real paths
    const languageServerUri = modelUri.replace(/^file:\/\/\/web\/sandbox/, 'file://');

    this.languageClient.sendNotification(DidOpenTextDocumentNotification.type, {
      textDocument: {
        uri: languageServerUri,
        languageId: 'taxi',
        version: 0,
        text: this.content,
      }
    })
  }

  private async reconnect(closeEvent: CloseEvent) {
    if (closeEvent.code === 1006 || closeEvent.code === 1005) {
      console.log("Refreshing websocket connection for language server");
      this.languageClient = await this.languageServerService.getLanguageClient();
      await this.sendOpenNotification();
    }
  }

  private async createNewMonacoModel() {
    const modelRef = await createTaxiEditorModel(this.content, this.filename, this.readOnly);
    const model: ITextFileEditorModel = modelRef.object;
    model.onDidChangeContent(() => this.modelChanged$.emit())
    return {modelRef, model};
  }

  private updateActionsOnEditor() {
    this.actions.forEach(action => {
      this.monacoEditor.addAction(action);
    })
  }

  updateContent(content: string) {
    if (this._content !== content) {
      this._content = content;
      this.contentChange.emit(content);
    }
  }

  private updateManualCompilationMessages() {
    if (isNullOrUndefined(this.compilationMessages))
      return;
    if (!this.monacoEditor)
      return;


    const model = this.monacoEditor.getModel();
    const markers = this.compilationMessages.map(message => {
      let severity: MarkerSeverity;
      switch (message.severity) {
        case "INFO":
          severity = MarkerSeverity.Info
          break;
        case "WARNING":
          severity = MarkerSeverity.Warning;
          break;
        case "ERROR":
          severity = MarkerSeverity.Error
          break
      }


      const word = model.getWordAtPosition({
        column: message.char,
        lineNumber: message.line
      })
      return {
        message: message.detailMessage,
        severity,
        startLineNumber: message.line,
        startColumn: message.char + 1,
        endLineNumber: message.line,
        endColumn: word ? word.endColumn : 1000
      } as IMarkerData
    })

    editor.setModelMarkers(model, 'owner', markers)
  }

  onDragEnd(e: IOutputData) {
    console.log("onDragEnd", e.sizes[1]);
    const height = e.sizes[1] as number
    this.compilationProblemsPanelExpanded = height > 42
    this.lastCompilationProblemsPanelHeight = height === 42 ? 135 : height;
  }
}

