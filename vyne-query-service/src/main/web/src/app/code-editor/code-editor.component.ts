import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {MonacoEditorLoaderService} from '@materia-ui/ngx-monaco-editor';
import {filter, take} from 'rxjs/operators';
import {editor} from 'monaco-editor';
import {TAXI_LANGUAGE_ID, taxiLanguageConfiguration, taxiLanguageTokenProvider} from '../code-viewer/taxi-lang.monaco';
// import {MessageConnection} from 'vscode-jsonrpc';
// import {listen} from '@codingame/monaco-jsonrpc';
import {listen, MessageConnection} from 'vscode-ws-jsonrpc';
import {CloseAction, createConnection, ErrorAction, MonacoLanguageClient, MonacoServices} from 'monaco-languageclient';
import ITextModel = editor.ITextModel;
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import IStandaloneThemeData = editor.IStandaloneThemeData;
import {WebsocketService} from '../services/websocket.service';

declare const monaco: any; // monaco

@Component({
  selector: 'app-code-editor',
  templateUrl: './code-editor.component.html',
  styleUrls: ['./code-editor.component.scss']
})
export class CodeEditorComponent implements OnInit {

  private editorTheme: IStandaloneThemeData = {
    base: 'vs-dark',
    inherit: true,
    rules: [
      {token: '', background: '#333f54'},
    ],
    colors: {
      ['editorBackground']: '#333f54',
    }
  };


  @Input()
  content: string;

  @Output()
  contentChanged = new EventEmitter<string>();

  private monacoEditor: IStandaloneCodeEditor;
  private monacoModel: ITextModel;

  constructor(private monacoLoaderService: MonacoEditorLoaderService,
              private websocketService: WebsocketService) {
  }

  ngOnInit(): void {
    this.createMonacoEditor();
  }

  private createMonacoEditor(): void {
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
        monaco.editor.defineTheme('vyne', this.editorTheme);
        monaco.editor.setTheme('vyne');
        monaco.editor.setModelLanguage(model, TAXI_LANGUAGE_ID);
      });
      monaco.languages.register({id: TAXI_LANGUAGE_ID});

      monaco.languages.setLanguageConfiguration(TAXI_LANGUAGE_ID, taxiLanguageConfiguration);
      monaco.languages.setMonarchTokensProvider(TAXI_LANGUAGE_ID, taxiLanguageTokenProvider);
      // here, we retrieve monaco-editor instance

      const monacoServices = MonacoServices.install(this.monacoEditor);
      const webSocket = this.createLanguageServerWebsocket();
      listen({
        webSocket,
        onConnection: connection => {
          // create and start the language client
          const languageClient = this.createLanguageClient(TAXI_LANGUAGE_ID, connection);
          const disposable = languageClient.start();
          connection.onClose(() => disposable.dispose());
        }
      });
    });
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
    }
  }
}

export interface EditorOptions {
  theme: string;
  language: string;
}


