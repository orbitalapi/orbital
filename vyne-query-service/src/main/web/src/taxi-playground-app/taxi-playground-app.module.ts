import { NgModule } from '@angular/core';
import { TaxiPlaygroundAppComponent } from './taxi-playground-app.component';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CommonModule } from '@angular/common';
import { TaxiPlaygroundModule } from 'src/app/taxi-playground/taxi-playground.module';
import { AngularSplitModule } from 'angular-split';
import { CodeEditorModule } from 'src/app/code-editor/code-editor.module';
import { SchemaDiagramModule } from 'src/app/schema-diagram/schema-diagram.module';
import { HttpClientModule } from '@angular/common/http';
import { AuthModule } from '@auth0/auth0-angular';
import { LANGUAGE_SERVER_WS_ADDRESS_TOKEN } from 'src/app/code-editor/code-editor.component';
import { WebsocketService } from 'src/app/services/websocket.service';
import { environment } from 'src/taxi-playground-app/environments/environment';
import { TuiDialogModule, TuiRootModule } from '@taiga-ui/core';

import Plausible from 'plausible-tracker'
import { PLAUSIBLE_ANALYTICS } from './plausible';
import { RouterModule } from '@angular/router';
import { TaxiPlaygroundContainerAppComponent } from 'src/taxi-playground-app/taxi-playground-container-app.component';

@NgModule({
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    CommonModule,
    AngularSplitModule,

    TaxiPlaygroundModule,
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
      { path: 's/:shareSlug', component: TaxiPlaygroundAppComponent },
      { path: '', component: TaxiPlaygroundAppComponent },
      { path: '**', redirectTo: '' }
    ])
  ],
  declarations: [TaxiPlaygroundContainerAppComponent, TaxiPlaygroundAppComponent],
  exports: [TaxiPlaygroundContainerAppComponent, TaxiPlaygroundAppComponent],
  providers: [
    {
      provide: LANGUAGE_SERVER_WS_ADDRESS_TOKEN,
      useValue: WebsocketService.buildWsUrl(environment.serverUrl, '/api/language-server'),
    },
    {
      provide: PLAUSIBLE_ANALYTICS,
      useFactory: () => {
        const plausible = Plausible({
          domain: 'voyager.vyne.co'
        })
        plausible.enableAutoOutboundTracking();
        return plausible;
      }
    }
  ],
  bootstrap: [TaxiPlaygroundContainerAppComponent]
})
export class TaxiPlaygroundAppModule {
}
