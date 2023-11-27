import {Inject, Injectable} from "@angular/core";
import {LANGUAGE_SERVER_WS_ADDRESS_TOKEN} from "./language-server.tokens";
import {initServices} from "monaco-languageclient";
import {defer, Observable} from "rxjs";
import {map, shareReplay} from "rxjs/operators";
import {Uri, languages} from "monaco-editor";
import getConfigurationServiceOverride from '@codingame/monaco-vscode-configuration-service-override';
import getKeybindingsServiceOverride from '@codingame/monaco-vscode-keybindings-service-override';
import getThemeServiceOverride from '@codingame/monaco-vscode-theme-service-override';
import getTextmateServiceOverride from '@codingame/monaco-vscode-textmate-service-override';
import {TAXI_LANGUAGE_ID, taxiLanguageConfiguration, taxiLanguageTokenProvider} from "../code-viewer/taxi-lang.monaco";
import {createWebsocketConnection, performInit, WsTransport} from "./language-server-commons";

@Injectable({
    providedIn: 'root',
})
export class MonacoLanguageServerService {

    readonly languageServicesInit$: Observable<void>

    //
    constructor(@Inject(LANGUAGE_SERVER_WS_ADDRESS_TOKEN) private languageServerWsAddress: string,) {

        this.languageServicesInit$ = defer(() => {
            // Copied from https://github.com/TypeFox/monaco-languageclient-ng-example/blob/main/src/app/app.component.ts
            console.info('Initializing Monaco language client')
            return performInit(true);
        }).pipe(
            shareReplay(1)
        );
    }

    async createLanguageServerWebsocketTransport(): Promise<[WebSocket,WsTransport]> {
        return createWebsocketConnection(this.languageServerWsAddress)
    }
}
