import {Inject, Injectable} from "@angular/core";
import {LANGUAGE_SERVER_WS_ADDRESS_TOKEN} from "./language-server.tokens";
import {buildWorkerDefinition} from "monaco-editor-workers";
import {initServices} from "monaco-languageclient";
import {defer, Observable} from "rxjs";
import {map, shareReplay} from "rxjs/operators";
import {createWebSocketAndStartClient} from "./language-server-utils";
import {Uri} from "monaco-editor";
import {createModelReference} from "@codingame/monaco-vscode-api/monaco";

@Injectable({
    providedIn: 'root',
})
export class MonacoLanguageServerService {

    readonly languageServicesInit$: Observable<void>
    //
    constructor(@Inject(LANGUAGE_SERVER_WS_ADDRESS_TOKEN) private languageServerWsAddress: string,) {
        buildWorkerDefinition('./assets/monaco-editor-workers/workers', window.location.origin, false);
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

    async createModelReference(uri: Uri, content: string) {
        return await createModelReference(uri, content)

    }

    private async initializeMonacoLanguageServices()  {

        await initServices({
            userServices: {
                // ...getThemeServiceOverride(),
                // ...getTextmateServiceOverride(),
                // ...getConfigurationServiceOverride(Uri.file('/workspace')),
                // ...getKeybindingsServiceOverride()
            },
            debugLogging: true
        });

        // languages.register({id: TAXI_LANGUAGE_ID});
        // languages.setLanguageConfiguration(TAXI_LANGUAGE_ID, taxiLanguageConfiguration);
        // languages.setMonarchTokensProvider(TAXI_LANGUAGE_ID, taxiLanguageTokenProvider);
    };

}
