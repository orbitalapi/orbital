import {Injectable} from "@angular/core";

export const LANGUAGE_SERVER_WS_ADDRESS_TOKEN = 'LANGUAGE_SERVER_WS_ADDRESS_TOKEN';

import {editor, languages} from 'monaco-editor';
import {createConfiguredEditor, createModelReference, IReference, ITextFileEditorModel} from 'vscode/monaco';
import '@codingame/monaco-vscode-theme-defaults-default-extension';
import '@codingame/monaco-vscode-json-default-extension';
import getConfigurationServiceOverride from '@codingame/monaco-vscode-configuration-service-override';
import getKeybindingsServiceOverride from '@codingame/monaco-vscode-keybindings-service-override';
import getThemeServiceOverride from '@codingame/monaco-vscode-theme-service-override';
import getTextmateServiceOverride from '@codingame/monaco-vscode-textmate-service-override';
import {initServices, MonacoLanguageClient} from 'monaco-languageclient';
import {CloseAction, ErrorAction, MessageTransports} from 'vscode-languageclient';
import {WebSocketMessageReader, WebSocketMessageWriter, toSocket} from 'vscode-ws-jsonrpc';
import {Uri} from 'vscode';
import {defer, Observable} from "rxjs";
import {shareReplay} from "rxjs/operators";

/**
 * This is a series of util functions taken from
 * https://github.com/TypeFox/monaco-languageclient/blob/main/packages/examples/src/common/client-commons.ts
 */

export const createLanguageClient = (transports: MessageTransports): MonacoLanguageClient => {
  return new MonacoLanguageClient({
    name: 'Orbital Taxi Language Client',
    clientOptions: {
      // use a language id as a document selector
      documentSelector: ['taxi'],
      // disable the default error handler
      errorHandler: {
        error: () => ({action: ErrorAction.Continue}),
        closed: () => ({action: CloseAction.DoNotRestart})
      }
    },
    // create a language client connection from the JSON RPC connection on demand
    connectionProvider: {
      get: () => {
        return Promise.resolve(transports);
      }
    }
  });
};

export const createUrl = (hostname: string, port: number, path: string, searchParams: Record<string, any> = {}, secure: boolean = location.protocol === 'https:'): string => {
  const protocol = secure ? 'wss' : 'ws';
  const url = new URL(`${protocol}://${hostname}:${port}${path}`);

  for (let [key, value] of Object.entries(searchParams)) {
    if (value instanceof Array) {
      value = value.join(',');
    }
    if (value) {
      url.searchParams.set(key, value);
    }
  }

  return url.toString();
};

export const createWebSocketAndStartClient = (url: string): WebSocket => {
  const webSocket = new WebSocket(url);
  webSocket.onopen = () => {
    const socket = toSocket(webSocket);
    const reader = new WebSocketMessageReader(socket);
    const writer = new WebSocketMessageWriter(socket);
    const languageClient = createLanguageClient({
      reader,
      writer
    });
    languageClient.start();
    reader.onClose(() => languageClient.stop());
  };
  return webSocket;
};

export const initializeMonacoLanguageServices = async (vscodeApiInit: boolean) => {
  if (vscodeApiInit === true) {
    await initServices({
      userServices: {
        ...getThemeServiceOverride(),
        ...getTextmateServiceOverride(),
        ...getConfigurationServiceOverride(Uri.file('/workspace')),
        ...getKeybindingsServiceOverride()
      },
      debugLogging: true
    });

    // register the JSON language with Monaco
    languages.register({
      id: 'json',
      extensions: ['.json', '.jsonc'],
      aliases: ['JSON', 'json'],
      mimetypes: ['application/json']
    });
  }
};


@Injectable({
  providedIn: 'root',
})
export class MonacoLanguageServerService {

  readonly languageServicesInit$: Observable<void>

  constructor() {
    this.languageServicesInit$ = defer(() => {
      console.info('Initializing Monaco language client')
      return initializeMonacoLanguageServices(true)
    }).pipe(
      shareReplay(1)
    );
  }
}
