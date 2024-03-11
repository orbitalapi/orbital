import {Component, ElementRef, EventEmitter, Input, OnDestroy, Output, ViewChild} from '@angular/core';
import {debounceTime} from "rxjs/operators";
import {editor, MarkerSeverity} from 'monaco-editor';
import {createLanguageClient, createTaxiEditor, createTaxiEditorModel} from "./language-server-commons";
import {ITextFileEditorModel} from "@codingame/monaco-vscode-api/monaco";
import {DidOpenTextDocumentNotification} from "vscode-languageclient";
import {MonacoLanguageClient} from "monaco-languageclient";
import {
  IStandaloneCodeEditor
} from "@codingame/monaco-vscode-api/vscode/vs/editor/standalone/browser/standaloneCodeEditor";
import {buildWorkerDefinition} from 'monaco-editor-workers';
import {MonacoLanguageServerService} from "./language-server.service";
import {CompilationMessage} from "../services/schema";
import {isNullOrUndefined} from "../utils/utils";
import IModelContentChangedEvent = editor.IModelContentChangedEvent;
import IMarkerData = editor.IMarkerData;

buildWorkerDefinition('./assets/monaco-editor-workers/workers', window.location.origin, false);

type WordWrapOptions = 'off' | 'on' | 'wordWrapColumn' | 'bounded';

@Component({
  selector: 'app-code-editor',
  styleUrls: ['./code-editor.component.scss'],
  template: `
    <div #codeEditorContainer class="code-editor"></div>
  `
})
export class CodeEditorComponent implements OnDestroy {

  private languageClient: MonacoLanguageClient;
  private monacoEditor: IStandaloneCodeEditor;
  private monacoModel: ITextFileEditorModel;
  private webSocket: WebSocket;

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
  languageServerEnabled: Boolean = true;

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

  constructor(
    private languageServerService: MonacoLanguageServerService
  ) {

    this.languageServerService.languageServicesInit$
      .subscribe(() => {
        // editor.defineTheme('vyne', this.editorTheme as any);
        // editor.setTheme('vyne');
      })
    this.modelChanged$.pipe(
      debounceTime(250),
    ).subscribe(e => {
      this.updateContent(this.monacoModel.textEditorModel.getValue());
      if (this.webSocket.readyState != this.webSocket.OPEN && this.languageServerEnabled) {
        console.log("Refresh websocket connection for language server");
        this.createWebsocketAndTransport();
      }
    })
  }

  async ngOnDestroy() {
    try {
      await this.languageClient.dispose();
    } catch (error) {
      console.error(error)
    }
  }

  private async createWebsocketAndTransport() {
    const [websocket, wsTransport] = await this.languageServerService.createLanguageServerWebsocketTransport()
    this.webSocket = websocket;
    return wsTransport;
  }


  private async createMonacoEditor() {
    if (this.monacoEditor) {
      this.monacoEditor.dispose();
    }

    // create the web socket
    if (this.languageServerEnabled) {
      const wsTransport = await this.createWebsocketAndTransport()
      this.languageClient = createLanguageClient(wsTransport);
    }

    const {modelRef, model} = await this.createNewMonacoModel();

    this.monacoEditor = await createTaxiEditor(this.codeEditorContainer.nativeElement, modelRef)
    this.monacoEditor.updateOptions({readOnly: this.readOnly});

    if (!isNullOrUndefined(this.compilationMessages)) {
      this.updateManualCompilationMessages();
    }

    if (this.languageServerEnabled) {
      await this.languageClient.sendNotification(DidOpenTextDocumentNotification.type, {
        textDocument: {
          uri: model.resource.toString(),
          languageId: 'taxi',
          version: 0,
          text: this.content,
        }
      })
    }

    this.updateActionsOnEditor()
    // this.createWebsocket();
  }

  private async createNewMonacoModel() {
    const modelRef = await createTaxiEditorModel(this.content);
    const model: ITextFileEditorModel = modelRef.object;
    model.onDidChangeContent((e: editor.IModelContentChangedEvent) => this.modelChanged$.next(e))
    this.monacoModel = model;
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

