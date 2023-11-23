/* --------------------------------------------------------------------------------------------
 * Copyright (c) 2018-2022 TypeFox GmbH (http://www.typefox.io). All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 * ------------------------------------------------------------------------------------------ */

import { editor, languages } from 'monaco-editor';
import { createConfiguredEditor, createModelReference, IReference, ITextFileEditorModel } from 'vscode/monaco';
import '@codingame/monaco-vscode-theme-defaults-default-extension';
import '@codingame/monaco-vscode-json-default-extension';
import getConfigurationServiceOverride from '@codingame/monaco-vscode-configuration-service-override';
import getKeybindingsServiceOverride from '@codingame/monaco-vscode-keybindings-service-override';
import getThemeServiceOverride from '@codingame/monaco-vscode-theme-service-override';
import getTextmateServiceOverride from '@codingame/monaco-vscode-textmate-service-override';
import { initServices, MonacoLanguageClient } from 'monaco-languageclient';
import { CloseAction, ErrorAction, MessageTransports } from 'vscode-languageclient';
import { WebSocketMessageReader, WebSocketMessageWriter, toSocket } from 'vscode-ws-jsonrpc';
import { Uri } from 'vscode';
import {
    IStandaloneCodeEditor
} from "@codingame/monaco-vscode-api/vscode/vs/editor/standalone/browser/standaloneCodeEditor";
import {TAXI_LANGUAGE_ID, taxiLanguageConfiguration, taxiLanguageTokenProvider} from "../code-viewer/taxi-lang.monaco";
import {iplastic_theme} from "./themes/iplastic";

export const createLanguageClient = (transports: MessageTransports): MonacoLanguageClient => {
    return new MonacoLanguageClient({
        name: 'Sample Language Client',
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

export type WsTransport = {
    reader: WebSocketMessageReader,
    writer: WebSocketMessageWriter
}
export const createWebsocketConnection = (url: string): Promise<[WebSocket,WsTransport]>  => {
    const webSocket = new WebSocket(url, []);

    return  new Promise<[WebSocket,WsTransport]>((resolve, reject) => {
        webSocket.onopen = () => {
            const socket = toSocket(webSocket);
            const reader = new WebSocketMessageReader(socket);
            const writer = new WebSocketMessageWriter(socket);
            resolve([webSocket, {reader, writer}]);
        };

        webSocket.onerror = (err) => {
            reject(err);
        };
    });
}


export const performInit = async (vscodeApiInit: boolean) => {
    if (vscodeApiInit === true) {
        await initServices({
            // enableTextmateService: true,
            // enableThemeService: true,
            userServices: {
                // Enabling theme and textMate services stops the
                // Monarch service working, which is what provides taxi highlighting
                // ...getThemeServiceOverride(),
                // ...getTextmateServiceOverride(),
                ...getConfigurationServiceOverride(Uri.file('/web/sandbox')),
                ...getKeybindingsServiceOverride()
            },
            debugLogging: true
        });
        // register the Taxi language with Monaco
        languages.register({
            id: TAXI_LANGUAGE_ID,
            extensions: ['.taxi'],
            aliases: ['TAXI', 'taxi'],
            mimetypes: ['application/taxi']
        });

        editor.defineTheme('orbital', iplastic_theme as any);
        editor.setTheme('orbital');

        languages.setLanguageConfiguration(TAXI_LANGUAGE_ID, taxiLanguageConfiguration);
        languages.setMonarchTokensProvider(TAXI_LANGUAGE_ID, taxiLanguageTokenProvider);
    }
};

export const createTaxiEditorModel = async (content: string):Promise<IReference<ITextFileEditorModel>> => {
    const uri = Uri.parse('/web/sandbox/query-123.taxi');
    const modelRef: IReference<ITextFileEditorModel> = await createModelReference(uri, content);

    modelRef.object.setLanguageId(TAXI_LANGUAGE_ID);
    return modelRef
}

export const createTaxiEditor = async (htmlElement: HTMLElement,    modelRef: IReference<ITextFileEditorModel>
) => {
    // create monaco editor
    const editor: IStandaloneCodeEditor = createConfiguredEditor(htmlElement, {
        model: modelRef.object.textEditorModel,
        glyphMargin: true,
        lightbulb: {
            enabled: true
        },
        automaticLayout: true,
        wordBasedSuggestions: false
    });

    return Promise.resolve(editor);
};
