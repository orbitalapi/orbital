import {Component, OnInit, Input} from '@angular/core';
import {MonacoEditorLoaderService} from '@materia-ui/ngx-monaco-editor';
import {QueryService} from '../services/query.service';
import {filter, take} from 'rxjs/operators';
import {
  vyneQueryLanguageConfiguration,
  vyneQueryLanguageTokenProvider
} from '../query-panel/query-editor/vyne-query-language.monaco';
import {editor} from 'monaco-editor';
import ITextModel = editor.ITextModel;
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import {taxiLanguageConfiguration, taxiLanguageTokenProvider} from '../code-viewer/taxi-lang.monaco';

declare const monaco: any; // monaco
import {listen, MessageConnection} from 'vscode-ws-jsonrpc';
import {MonacoLanguageClient, CloseAction, ErrorAction, MonacoServices, createConnection} from 'monaco-languageclient';

@Component({
  selector: 'app-code-editor',
  templateUrl: './code-editor.component.html',
  styleUrls: ['./code-editor.component.scss']
})
export class CodeEditorComponent implements OnInit {

  content: string;

  @Input()
  editorOptions: EditorOptions;

  monacoEditor: IStandaloneCodeEditor;
  monacoModel: ITextModel;

  constructor(private monacoLoaderService: MonacoEditorLoaderService) {

  }

  ngOnInit(): void {
    this.monacoLoaderService.isMonacoLoaded.pipe(
      filter(isLoaded => isLoaded),
      take(1),
    ).subscribe(() => {
      monaco.editor.onDidCreateEditor(editorInstance => {
        editorInstance.updateOptions({readOnly: false, minimap: {enabled: false}});
        this.monacoEditor = editorInstance;
        this.remeasure();
      });

      const language = this.editorOptions.language;
      monaco.editor.onDidCreateModel(model => {
        this.monacoModel = model;
        monaco.editor.defineTheme('vyne', this.getTheme(language));
        monaco.editor.setTheme('vyne');
        monaco.editor.setModelLanguage(model, language);
      });
      monaco.languages.register({id: language});

      const conf = this.getConfiguration(language);
      monaco.languages.setLanguageConfiguration(language, conf.configuration);
      monaco.languages.setMonarchTokensProvider(language, conf.tokenProvider);
      // here, we retrieve monaco-editor instance

      MonacoServices.install(this.monacoEditor);
      const webSocket = this.createWebSocket(`ws://localhost:9022/language-server`);
      listen({
        webSocket,
        onConnection: connection => {
          // create and start the language client
          const languageClient = this.createLanguageClient(language, connection);
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

  getTheme(language: string) {
    return {
      base: 'vs-dark',
      inherit: true,
      rules: [
        {token: '', background: '#333f54'},
      ],
      colors: {
        ['editorBackground']: '#333f54',
      }
    };
  }

  getConfiguration(language: string) {
    switch (language) {
      case 'taxi':
        return {configuration: taxiLanguageConfiguration, tokenProvider: taxiLanguageTokenProvider};
      case 'vyneQL':
        return {configuration: vyneQueryLanguageConfiguration, tokenProvider: vyneQueryLanguageTokenProvider};
    }

  }

  createWebSocket(url: string): WebSocket {
    /* Investigare ReconnectionWebSocket
  const socketOptions = {
      maxReconnectionDelay: 10000,
      minReconnectionDelay: 1000,
      reconnectionDelayGrowFactor: 1.3,
      connectionTimeout: 10000,
      maxRetries: Infinity,
      debug: false
  }; */
    return new WebSocket(url, []);
  }

  createLanguageClient(language: string, connection: MessageConnection): MonacoLanguageClient {
    return new MonacoLanguageClient({
      name: 'Sample Language Client',
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

}

export interface EditorOptions {
  theme: string;
  language: string;
}


