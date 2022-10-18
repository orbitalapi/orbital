import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { AuthModule } from '@auth0/auth0-angular';
import { LANGUAGE_SERVER_WS_ADDRESS_TOKEN } from 'src/app/code-editor/code-editor.component';
import { WebsocketService } from 'src/app/services/websocket.service';
import { environment } from 'src/orbital-app/environments/environment';
import { TuiRootModule } from '@taiga-ui/core';

import Plausible from 'plausible-tracker'
import { PLAUSIBLE_ANALYTICS } from './plausible';
import { ORBITAL_ROUTES } from 'src/orbital-app/oribtal.routes';
import { OrbitalAppComponent } from 'src/orbital-app/orbital-app.component';

@NgModule({
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    CommonModule,
    HttpClientModule,
    TuiRootModule,
    ORBITAL_ROUTES,
    AuthModule.forRoot({
      domain: 'orbitalhq.eu.auth0.com',
      clientId: 'ZaDGRQWEfgTFtlWVR9AXWg9vOiBxgVPv'
    }),
  ],
  declarations: [OrbitalAppComponent],
  exports: [OrbitalAppComponent],
  providers: [
    {
      provide: LANGUAGE_SERVER_WS_ADDRESS_TOKEN,
      useValue: WebsocketService.buildWsUrl(environment.serverUrl, '/api/language-server'),
    },
    {
      provide: PLAUSIBLE_ANALYTICS,
      useFactory: () => {
        const plausible = Plausible({
          domain: 'app.orbitalhq.com'
        })
        plausible.enableAutoOutboundTracking();
        return plausible;
      }
    }
  ],
  bootstrap: [OrbitalAppComponent]
})
export class OrbitalAppModule {
}
