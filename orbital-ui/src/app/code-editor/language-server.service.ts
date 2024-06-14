import {Inject, Injectable} from "@angular/core";
import {LANGUAGE_SERVER_WS_ADDRESS_TOKEN} from "./language-server.tokens";
import {MonacoLanguageClient} from "monaco-languageclient";
import {defer, Observable, Subject} from 'rxjs';
import {shareReplay} from "rxjs/operators";
import {createLanguageClient, createWebsocketConnection, performInit, WsTransport} from "./language-server-commons";

@Injectable({
  providedIn: 'root',
})
export class MonacoLanguageServerService {
  readonly languageServicesInit$: Observable<void>
  readonly websocketClosed$: Subject<CloseEvent> = new Subject();

  private languageClient: MonacoLanguageClient;
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
    } catch(e) {
    }
    this.languageClient = null
  }

  private async createLanguageServerWebsocketTransport(): Promise<[WebSocket, WsTransport]> {
    // Re-use the connection. This is important as if there's multiple
    // editors in the page, they all need to be part of the same session.
    // In future, we may want to make this an observable that cleans up when
    // all subscribers have gone away, and handles reconnects.
    if (!this.connection) {
      this.connection = createWebsocketConnection(this.languageServerWsAddress);
    }
    return this.connection;
  }

  async getLanguageClient(): Promise<MonacoLanguageClient> {
    if (!this.languageClient) {
      console.log('Creating new language client')
      const [websocket, wsTransport] = await this.createLanguageServerWebsocketTransport()
      this.webSocket = websocket;
      this.webSocket.onclose = async (event) => {
        console.warn('language server web socket closed...', event)
        await this.reset();
        this.websocketClosed$.next(event)
      }
      this.languageClient = createLanguageClient(wsTransport);
    }
    return this.languageClient;
  }
}
