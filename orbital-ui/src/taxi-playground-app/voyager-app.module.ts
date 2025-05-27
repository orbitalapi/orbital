import { NG_EVENT_PLUGINS } from "@taiga-ui/event-plugins";
import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {AsyncPipe, CommonModule} from '@angular/common';
import {AngularSplitModule} from 'angular-split';
import {CodeEditorModule} from 'src/app/code-editor/code-editor.module';
import {SchemaDiagramModule} from 'src/app/schema-diagram/schema-diagram.module';
import {HttpClientModule} from '@angular/common/http';
import {AuthModule} from '@auth0/auth0-angular';
import {WebsocketService} from 'src/app/services/websocket.service';
import {environment} from 'src/taxi-playground-app/environments/environment';
import { TuiRoot, TuiDialog } from '@taiga-ui/core';

import Plausible from 'plausible-tracker'
import {PLAUSIBLE_ANALYTICS} from './plausible';
import {RouterModule} from '@angular/router';
import {VoyagerContainerAppComponent} from 'src/taxi-playground-app/voyager-container-app.component';
import {VoyagerAppComponent} from "./voyager-app.component";
import {VoyagerModule} from "src/app/voyager/voyager.module";
import {LANGUAGE_SERVER_WS_ADDRESS_TOKEN} from "../app/code-editor/language-server.tokens";
import {PlaygroundQueryPanelComponent} from "../app/voyager/playground-query-panel/playground-query-panel.component";
import {VoyagerSidebarComponent} from "../app/voyager/voyager-sidebar/voyager-sidebar.component";
import {ExpandingPanelSetModule} from "../app/expanding-panelset/expanding-panel-set.module";
import {CompilationMessageListModule} from "../app/compilation-message-list/compilation-message-list.module";
import {ReadmePanelComponent} from "../app/voyager/readme-panel/readme-panel.component";
import {ResponseEditorDialogComponent} from '../app/voyager/playground-query-panel/response-editor-dialog.component';
import {StubDesignerComponent} from '../app/voyager/stub-designer/stub-designer.component';
import {ENVIRONMENT} from "../app/services/environment";
import {PlaygroundSchemaService} from "./playground-schema-service";
import {SCHEMA_PROVIDER_TOKEN, SchemaProvider} from "../app/services/types.service";

@NgModule({
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    CommonModule,
    AngularSplitModule,

    VoyagerModule,
    CodeEditorModule,
    SchemaDiagramModule,

    HttpClientModule,
    TuiRoot,
    TuiDialog,
    AuthModule.forRoot({
      domain: 'orbitalhq.eu.auth0.com',
      clientId: 'ZaDGRQWEfgTFtlWVR9AXWg9vOiBxgVPv'
    }),
    RouterModule.forRoot([
      {path: '', component: VoyagerAppComponent, children: [
          {path: 'examples/:exampleSlug', component: VoyagerAppComponent},
          {path: 's/:shareSlug', component: VoyagerAppComponent},
        ]},
      {path: '**', redirectTo: ''}
    ]),
    PlaygroundQueryPanelComponent,
    VoyagerSidebarComponent,
    ExpandingPanelSetModule,
    CompilationMessageListModule,
    AsyncPipe,
    ReadmePanelComponent,
    ResponseEditorDialogComponent,
    StubDesignerComponent,
  ],
  declarations: [VoyagerContainerAppComponent, VoyagerAppComponent],
  exports: [VoyagerContainerAppComponent, VoyagerAppComponent],
  providers: [
    {
      provide: LANGUAGE_SERVER_WS_ADDRESS_TOKEN,
      useValue: WebsocketService.buildWsUrl(environment.serverUrl, '/api/language-server'),
    },
    {
      provide: SCHEMA_PROVIDER_TOKEN,
      useExisting: PlaygroundSchemaService
    },
    {
      provide: PLAUSIBLE_ANALYTICS,
      useFactory: () => {
        const plausible = Plausible({
          domain: 'voyager.orbitalhq.com'
        })
        plausible.enableAutoOutboundTracking();
        return plausible;
      }
    },
    {
      provide: ENVIRONMENT,
      useValue: environment,
    },
    NG_EVENT_PLUGINS
],
  bootstrap: [VoyagerContainerAppComponent]
})
export class VoyagerAppModule {
}
