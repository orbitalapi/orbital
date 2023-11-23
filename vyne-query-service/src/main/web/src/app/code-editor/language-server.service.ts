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
import {createWebSocketAndStartClient} from "./language-server-commons";
@Injectable({
    providedIn: 'root',
})
export class MonacoLanguageServerService {

    readonly languageServicesInit$: Observable<void>
    //
    constructor(@Inject(LANGUAGE_SERVER_WS_ADDRESS_TOKEN) private languageServerWsAddress: string,) {

        this.languageServicesInit$ = defer(() => {
            // Copied from https://github.com/TypeFox/monaco-languageclient-ng-example/blob/main/src/app/app.component.ts
            //

            console.info('Initializing Monaco language client')
            return this.initializeMonacoLanguageServices()
        }).pipe(
            shareReplay(1)
        );
    }

    createLanguageServerWebsocket(): Observable<WebSocket> {
        return this.languageServicesInit$
            .pipe(map(() => createWebSocketAndStartClient(this.languageServerWsAddress)
            ))
    }

    private async initializeMonacoLanguageServices()  {

        await initServices({
            userServices: {
                ...getThemeServiceOverride(),
                ...getTextmateServiceOverride(),
                ...getConfigurationServiceOverride(Uri.file('/workspace')),
                ...getKeybindingsServiceOverride()
            },
            debugLogging: true
        });

        languages.register({id: TAXI_LANGUAGE_ID});
        languages.setLanguageConfiguration(TAXI_LANGUAGE_ID, taxiLanguageConfiguration);
        languages.setMonarchTokensProvider(TAXI_LANGUAGE_ID, taxiLanguageTokenProvider);
    };

}
