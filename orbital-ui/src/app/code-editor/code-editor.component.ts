import {
  ChangeDetectionStrategy, ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  ViewChild
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {debounceTime, filter} from "rxjs/operators";
import {editor, MarkerSeverity} from 'monaco-editor';
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
      gutterDblClickDuration="250"
      (gutterDblClick)="errorPanelSize < 60 ? errorPanelSize = 250 : errorPanelSize = 50"
    >
      <as-split-area size="*">
        <div #codeEditorContainer class="code-editor"></div>
      </as-split-area>
      <as-split-area [size]="errorPanelSize" minSize="50" maxSize="250" *ngIf="showCompilationErrors">
        <app-compilation-message-list [compilationMessages]="compilationMessages"></app-compilation-message-list>
      </as-split-area>
    </as-split>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CodeEditorComponent implements OnDestroy {

  private languageClient: MonacoLanguageClient;
  private monacoEditor: IStandaloneCodeEditor;
  private monacoModel: ITextFileEditorModel;

  private _codeEditorContainer: ElementRef;
  @ViewChild('codeEditorContainer')
  get codeEditorContainer(): ElementRef {
    return this._codeEditorContainer;
  }

  set codeEditorContainer(value: ElementRef) {
    this._codeEditorContainer = value;
    this.createMonacoEditor()
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
  showCompilationErrors: boolean = true;

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

  private modelChanged$ = new EventEmitter<IModelContentChangedEvent>();

  @Output()
  contentChange = new EventEmitter<string>();

  errorPanelSize: number = 135;

  constructor(
    private languageServerService: MonacoLanguageServerService,
    private destroyRef: DestroyRef,
    private changeDetectorRef: ChangeDetectorRef
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
    diagnosticsEvents.pipe(
      takeUntilDestroyed(this.destroyRef),
      filter(event => event.uri === model.textEditorModel.uri.toString()),
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
          char: message.range.start.character + 1,
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

    if (!isNullOrUndefined(this.compilationMessages)) {
      this.updateManualCompilationMessages();
    }

    await this.sendOpenNotifcation();

    this.languageServerService.websocketClosed$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(async (closeEvent) => this.reconnect(closeEvent))

    this.updateActionsOnEditor()
  }

  private async sendOpenNotifcation() {
    await this.languageClient.sendNotification(DidOpenTextDocumentNotification.type, {
      textDocument: {
        uri: this.monacoModel.resource.toString(),
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
      await this.sendOpenNotifcation();
    }
  }

  private async createNewMonacoModel() {
    const modelRef = await createTaxiEditorModel(this.content);
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
}

