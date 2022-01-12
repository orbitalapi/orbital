import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {MonacoEditorLoaderService} from '@materia-ui/ngx-monaco-editor';
import {filter, take} from 'rxjs/operators';
import {TAXI_LANGUAGE_ID, taxiLanguageConfiguration, taxiLanguageTokenProvider} from '../code-viewer/taxi-lang.monaco';

import {MessageConnection} from 'vscode-jsonrpc';
import {listen} from '@codingame/monaco-jsonrpc';
import {
  CloseAction,
  createConnection,
  ErrorAction,
  MonacoLanguageClient,
  MonacoServices
} from '@codingame/monaco-languageclient';
import {WebsocketService} from '../services/websocket.service';
import {iplastic_theme} from './themes/iplastic';
import ICodeEditor = editor.ICodeEditor;
import {editor} from 'monaco-editor-core';
import ITextModel = editor.ITextModel;
import {isNullOrUndefined} from 'util';

declare const monaco: any; // monaco

@Component({
  selector: 'app-code-editor',
  templateUrl: './code-editor.component.html',
  styleUrls: ['./code-editor.component.scss']
})
export class CodeEditorComponent implements OnInit {
  private editorTheme = iplastic_theme;

  @Input()
  content: string;

  @Output()
    // Deprecated, use content change, which matches for two-way binding
  contentChanged = new EventEmitter<string>();

  @Output()
  contentChange = new EventEmitter<string>();

  private monacoEditor: ICodeEditor;
  private monacoModel: ITextModel;

  constructor(private monacoLoaderService: MonacoEditorLoaderService,
              private websocketService: WebsocketService) {
  }

  ngOnInit(): void {
    this.createMonacoEditor();
  }

  private createMonacoEditor(): void {
    this.monacoLoaderService.isMonacoLoaded$.pipe(
      filter(isLoaded => isLoaded),
      take(1),
    ).subscribe(() => {
      monaco.editor.onDidCreateEditor(editorInstance => {
        console.log('on did create editor');
        editorInstance.updateOptions({readOnly: false, minimap: {enabled: false}});
        this.monacoEditor = editorInstance;
      });

      monaco.editor.onDidCreateModel(model => {
        console.log('on did create model');
        this.monacoModel = model;

        monaco.editor.defineTheme('vyne', this.editorTheme);
        monaco.editor.setTheme('vyne');
        monaco.editor.setModelLanguage(model, TAXI_LANGUAGE_ID);
        this.startLanguageService();
      });
      monaco.languages.register({id: TAXI_LANGUAGE_ID});

      monaco.languages.setLanguageConfiguration(TAXI_LANGUAGE_ID, taxiLanguageConfiguration);
      monaco.languages.setMonarchTokensProvider(TAXI_LANGUAGE_ID, taxiLanguageTokenProvider);
    });
  }

  createLanguageServerWebsocket(): WebSocket {
    /* Investigare ReconnectionWebSocket
  const socketOptions = {
      maxReconnectionDelay: 10000,
      minReconnectionDelay: 1000,
      reconnectionDelayGrowFactor: 1.3,
      connectionTimeout: 10000,
      maxRetries: Infinity,
      debug: false
  }; */
    const wsUrl = this.websocketService.getWsUrl('/api/language-server');
    return new WebSocket(wsUrl, []);
  }

  createLanguageClient(language: string, connection: MessageConnection): MonacoLanguageClient {
    return new MonacoLanguageClient({
      name: 'vyne-taxi-language-client',
      clientOptions: {
        // use a language id as a document selector
        documentSelector: [language],
        // disable the default error handler
        errorHandler: {
          error: () => ErrorAction.Continue,
          closed: () => CloseAction.DoNotRestart
        }
      },
      // create a language client connection from the JSON RPC connection on demand
      connectionProvider: {
        get: (errorHandler, closeHandler) => {
          return Promise.resolve(createConnection(connection, errorHandler, closeHandler));
        }
      }
    });
  }

  updateContent(content: string) {
    if (this.content !== content) {
      this.content = content;
      this.contentChanged.emit(content);
      this.contentChange.emit(content);
    }
  }

  private startLanguageService() {
    if (isNullOrUndefined(this.monacoEditor) || isNullOrUndefined(this.monacoModel)) {
      console.warn('Not creating language server client - need both an editor and a model.  Looks like monaco init hasn\'t finished correctly');
    }
    MonacoServices.install((<any>window).monaco);

    const webSocket = this.createLanguageServerWebsocket();
    listen({
      webSocket,
      onConnection: (connection: MessageConnection) => {
        // create and start the language client
        const languageClient = this.createLanguageClient(TAXI_LANGUAGE_ID, connection);
        const disposable = languageClient.start();
        connection.onClose(() => disposable.dispose());
      }
    });
  }
}

export interface EditorOptions {
  theme: string;
  language: string;
}


