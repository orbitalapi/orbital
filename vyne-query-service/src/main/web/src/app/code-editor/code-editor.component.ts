import { Component, ElementRef, EventEmitter, Inject, Input, OnDestroy, Output, ViewChild } from '@angular/core';
import { debounceTime } from 'rxjs/operators';
import {
  TAXI_LANGUAGE_ID,
  taxiLanguageConfiguration,
  taxiLanguageTokenProvider
} from '../code-viewer/taxi-lang.monaco';
import { toSocket, WebSocketMessageReader, WebSocketMessageWriter } from 'vscode-ws-jsonrpc';
import {
  CloseAction,
  DidOpenTextDocumentNotification,
  ErrorAction,
  MonacoLanguageClient,
  MonacoServices
} from 'monaco-languageclient';
import { iplastic_theme } from './themes/iplastic';
import { editor } from 'monaco-editor';


// Import the core monaco editor
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';
import * as monadoEditorAll from 'monaco-editor/esm/vs/editor/editor.all.js';
import * as languageFeatureService from 'monaco-editor/esm/vs/editor/common/services/languageFeaturesService.js';

// Import features we care abut
import * as monacoFeature4
  from 'monaco-editor/esm/vs/editor/standalone/browser/quickAccess/standaloneHelpQuickAccess.js';
import * as monacoFeature5
  from 'monaco-editor/esm/vs/editor/standalone/browser/quickAccess/standaloneGotoLineQuickAccess.js';
import * as monacoFeature6
  from 'monaco-editor/esm/vs/editor/standalone/browser/quickAccess/standaloneGotoSymbolQuickAccess.js';
import * as monacoFeature7
  from 'monaco-editor/esm/vs/editor/standalone/browser/quickAccess/standaloneCommandsQuickAccess.js';
import * as monacoFeature8
  from 'monaco-editor/esm/vs/editor/standalone/browser/quickInput/standaloneQuickInputService.js';

import ITextModel = editor.ITextModel;
import IModelContentChangedEvent = editor.IModelContentChangedEvent;
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;


export const LANGUAGE_SERVER_WS_ADDRESS_TOKEN = 'LANGUAGE_SERVER_WS_ADDRESS_TOKEN';

type WordWrapOptions = 'off' | 'on' | 'wordWrapColumn' | 'bounded';

@Component({
  selector: 'app-code-editor',
  templateUrl: './code-editor.component.html',
  styleUrls: ['./code-editor.component.scss']
})
export class CodeEditorComponent implements OnDestroy {
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

  private editorTheme = iplastic_theme;

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
      this.monacoEditor.updateOptions({ readOnly: this.readOnly });
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
      this.monacoEditor.updateOptions({ wordWrap: this.wordWrap });
    }
  }

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
      this.monacoModel.setValue(value);
      this.contentChange.emit(value);
    }
  }

  private modelChanged$ = new EventEmitter<IModelContentChangedEvent>();

  @Output()
  contentChange = new EventEmitter<string>();

  private monacoEditor: IStandaloneCodeEditor;
  private monacoModel: ITextModel;

  private monacoLanguageClient: MonacoLanguageClient;


  constructor(@Inject(LANGUAGE_SERVER_WS_ADDRESS_TOKEN) private languageServerWsAddress: string) {
    // This does nothing, but prevents tree-shaking
    const features = [monadoEditorAll, monacoFeature4, monacoFeature5, monacoFeature6, monacoFeature7, monacoFeature8,languageFeatureService];

    this.monacoModel = monaco.editor.createModel(this.content, TAXI_LANGUAGE_ID, monaco.Uri.parse('inmemory://query.taxi'));
    monaco.languages.register({ id: TAXI_LANGUAGE_ID });

    monaco.languages.setLanguageConfiguration(TAXI_LANGUAGE_ID, taxiLanguageConfiguration);
    monaco.languages.setMonarchTokensProvider(TAXI_LANGUAGE_ID, taxiLanguageTokenProvider);

    monaco.editor.defineTheme('vyne', this.editorTheme as any);
    monaco.editor.setTheme('vyne');
    this.modelChanged$.pipe(
      debounceTime(500),
    ).subscribe(e => {
      this.updateContent(this.monacoModel.getValue());
    })
    this.monacoModel.onDidChangeContent(e => this.modelChanged$.next(e));
  }

  private createMonacoEditor(): void {
    if (this.monacoEditor) {
      this.monacoEditor.dispose();
    }

    this.monacoEditor = monaco.editor.create(this._codeEditorContainer.nativeElement, {
      model: this.monacoModel,
      glyphMargin: true,
      lightbulb: {
        enabled: true
      },
      automaticLayout: true,
      readOnly: this._readOnly,
      wordWrap: this._wordWrap
    });

    this.updateActionsOnEditor();
    MonacoServices.install();

    this.createLanguageClient();
  }

  private updateActionsOnEditor() {
    this.actions.forEach(action => {
      this.monacoEditor.addAction(action);
    })
  }


  createLanguageClient() {
    const webSocket = new WebSocket(this.languageServerWsAddress, []);

    // Implemented following the example here:
    // https://github.com/TypeFox/monaco-languageclient/blob/main/packages/examples/client/src/client.ts#L56
    webSocket.onopen = () => {
      const socket = toSocket(webSocket);
      const reader = new WebSocketMessageReader(socket);
      const writer = new WebSocketMessageWriter(socket);

      const transports = { reader, writer }

      this.monacoLanguageClient = new MonacoLanguageClient({
        name: 'vyne-taxi-language-client',
        clientOptions: {
          // use a language id as a document selector
          documentSelector: [TAXI_LANGUAGE_ID],
          // disable the default error handler
          errorHandler: {
            error: () => ({ action: ErrorAction.Continue }),
            closed: () => ({ action: CloseAction.DoNotRestart })
          }
        },
        // create a language client connection from the JSON RPC connection on demand
        connectionProvider: {
          get: () => {
            return Promise.resolve(transports);
          }
        }
      });
      this.monacoLanguageClient.start()
        .then(() => {
          this.monacoLanguageClient.sendNotification(DidOpenTextDocumentNotification.type, {
            textDocument: {
              uri: 'inmemory://query.taxi',
              languageId: TAXI_LANGUAGE_ID,
              version: 0,
              text: this.content
            }
          })
        });
    }

  }

  updateContent(content: string) {
    if (this._content !== content) {
      this._content = content;
      this.contentChange.emit(content);
    }
  }


  ngOnDestroy(): void {
    console.info('Closing Language Service');
    this.monacoLanguageClient.stop();
    this.monacoModel.dispose();
  }

}

