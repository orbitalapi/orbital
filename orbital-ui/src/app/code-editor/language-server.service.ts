import {Inject, Injectable} from "@angular/core";
import {LANGUAGE_SERVER_WS_ADDRESS_TOKEN} from "./language-server.tokens";
import {MonacoLanguageClient} from "monaco-languageclient";
import {defer, Observable, Subject} from 'rxjs';
import {shareReplay} from "rxjs/operators";
import {
  createLanguageClient,
  createWebsocketConnection,
  DiagnosticsEvent,
  performInit,
  WsTransport
} from "./language-server-commons";
import {Uri} from "monaco-editor";
import {Diagnostic} from "vscode-languageclient";

@Injectable({
  providedIn: 'root',
})
export class MonacoLanguageServerService {
  readonly languageServicesInit$: Observable<void>
  readonly websocketClosed$: Subject<CloseEvent> = new Subject();
  readonly websocketTerminallyClosed$: Subject<void> = new Subject();

  private languageClient: MonacoLanguageClient;
  private languageClientCreationPromise: Promise<MonacoLanguageClient> | null = null;
  private diagnosticsSubject: Subject<DiagnosticsEvent> = new Subject<{ uri: Uri, diagnostics: Diagnostic[] }>();
  private webSocket: WebSocket;
  private connection: Promise<[WebSocket, WsTransport]> | null = null;

  constructor(@Inject(LANGUAGE_SERVER_WS_ADDRESS_TOKEN) private languageServerWsAddress: string,) {
    this.languageServicesInit$ = defer(() => {
      // Copied from https://github.com/TypeFox/monaco-languageclient-ng-example/blob/main/src/app/app.component.ts
      console.info('Initializing Monaco language client')
      return performInit(true);
    }).pipe(
      shareReplay(1)
    );

    // For testing websocket reconnection
    // @ts-ignore
    window.killWebsocket = () => {
      this.webSocket.close()
    }
  }

  private async reset() {
    this.connection = null;
    this.webSocket.onclose = null;
    try {
      await this.languageClient?.dispose()
    } catch (e) {
    }
    this.languageClient = null
  }

  private createLanguageServerWebsocketTransport(): Promise<[WebSocket, WsTransport]> {
    // Re-use the connection. This is important as if there's multiple
    // editors in the page, they all need to be part of the same session.
    // In future, we may want to make this an observable that cleans up when
    // all subscribers have gone away, and handles reconnects.
    if (!this.connection) {
      this.connection = createWebsocketConnection(this.languageServerWsAddress);
    }
    return this.connection;
  }

  async getDiagnostics$(): Promise<Observable<DiagnosticsEvent>> {
    if (!this.languageClient) {
      await this.getLanguageClient();
    }
    return this.diagnosticsSubject;
  }

  async getLanguageClient(): Promise<MonacoLanguageClient> {
    if (this.languageClient) {
      // If the language client already exists, return it
      return this.languageClient;
    }

    // If the creation process has already started, return the promise
    if (this.languageClientCreationPromise) {
      return this.languageClientCreationPromise;
    }

    // Otherwise, start the creation process and store the promise
    this.languageClientCreationPromise = new Promise(async (resolve, reject) => {
      try {
        console.log('Creating new language client');
        let websocket: WebSocket, wsTransport: WsTransport;
        try {
          [websocket, wsTransport] = await this.createLanguageServerWebsocketTransport();
        } catch (error) {
          console.error('Failed to establish WebSocket connection:', error);
          this.websocketTerminallyClosed$.next();
          reject(error);
          return;
        }

        this.webSocket = websocket;
        this.webSocket.onclose = async (event) => {
          console.warn('language server web socket closed...', event);
          await this.reset();
          this.websocketClosed$.next(event);
        };
        this.languageClient = createLanguageClient(wsTransport);
        this.languageClient.onNotification('textDocument/publishDiagnostics', (params) => {
          this.diagnosticsSubject.next(params);
        });

        resolve(this.languageClient);
      } catch (error) {
        reject(error);
      } finally {
        // Clear the languageClientCreationPromise when done,
        // so subsequent requests can re-trigger the process if needed
        this.languageClientCreationPromise = null;
      }
    });

    return this.languageClientCreationPromise;
  }

}
