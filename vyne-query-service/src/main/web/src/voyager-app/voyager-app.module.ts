import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {CommonModule} from '@angular/common';
import {AngularSplitModule} from 'angular-split';
import {CodeEditorModule} from 'src/app/code-editor/code-editor.module';
import {SchemaDiagramModule} from 'src/app/schema-diagram/schema-diagram.module';
import {HttpClientModule} from '@angular/common/http';
import {AuthModule} from '@auth0/auth0-angular';
import {WebsocketService} from 'src/app/services/websocket.service';
import {environment} from 'src/voyager-app/environments/environment';
import {TuiDialogModule, TuiRootModule} from '@taiga-ui/core';

import Plausible from 'plausible-tracker'
import {PLAUSIBLE_ANALYTICS} from './plausible';
import {RouterModule} from '@angular/router';
import {VoyagerContainerAppComponent} from 'src/voyager-app/voyager-container-app.component';
import {VoyagerAppComponent} from "./voyager-app.component";
import {VoyagerModule} from "src/app/voyager/voyager.module";
import {LANGUAGE_SERVER_WS_ADDRESS_TOKEN} from "../app/code-editor/langServer.service";

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
    TuiRootModule,
    TuiDialogModule,
    AuthModule.forRoot({
      domain: 'orbitalhq.eu.auth0.com',
      clientId: 'ZaDGRQWEfgTFtlWVR9AXWg9vOiBxgVPv'
    }),
    RouterModule.forRoot([
      {path: 's/:shareSlug', component: VoyagerAppComponent},
      {path: '', component: VoyagerAppComponent},
      {path: '**', redirectTo: ''}
    ])
  ],
  declarations: [VoyagerContainerAppComponent, VoyagerAppComponent],
  exports: [VoyagerContainerAppComponent, VoyagerAppComponent],
  providers: [
    {
      provide: LANGUAGE_SERVER_WS_ADDRESS_TOKEN,
      useValue: WebsocketService.buildWsUrl(environment.serverUrl, '/api/language-server'),
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
    }
  ],
  bootstrap: [VoyagerContainerAppComponent]
})
export class VoyagerAppModule {
}
