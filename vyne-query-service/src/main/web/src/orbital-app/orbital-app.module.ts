import { ApplicationRef, DoBootstrap, NgModule, Optional } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { LANGUAGE_SERVER_WS_ADDRESS_TOKEN } from 'src/app/code-editor/code-editor.component';
import { WebsocketService } from 'src/app/services/websocket.service';
import { environment } from 'src/orbital-app/environments/environment';
import { TuiRootModule } from '@taiga-ui/core';

import Plausible from 'plausible-tracker'
import { PLAUSIBLE_ANALYTICS } from './plausible';
import { ORBITAL_ROUTES } from 'src/orbital-app/oribtal.routes';
import { OrbitalAppComponent } from 'src/orbital-app/orbital-app.component';
import { OrbitalShellModule } from 'src/app/orbital-shell/orbital-shell.module';
import { AuthModule } from 'src/app/auth/auth.module';
import { ENVIRONMENT } from 'src/app/services/environment';
import { AuthService } from 'src/app/auth/auth.service';
import { VyneServicesModule } from 'src/app/services/vyne-services.module';

@NgModule({
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    CommonModule,
    HttpClientModule,
    TuiRootModule,
    ORBITAL_ROUTES,
    AuthModule,
    VyneServicesModule,
    // Auth0:
    // AuthModule.forRoot({
    //   domain: 'orbitalhq.eu.auth0.com',
    //   clientId: 'ZaDGRQWEfgTFtlWVR9AXWg9vOiBxgVPv'
    // }),

    OrbitalShellModule
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
    },
    {
      provide: ENVIRONMENT,
      useValue: environment
    }
  ],
})
export class OrbitalAppModule implements DoBootstrap {
  constructor(@Optional() private authService: AuthService) {
  }

  ngDoBootstrap(appRef: ApplicationRef): void {
    this.authService.bootstrapAuthService()
      .then(() => {
        console.log('bootstrapping the application');
        appRef.bootstrap(OrbitalAppComponent);
      })
      .catch(error => {
        console.error(`[ngDoBootstrap] Problem while authService.bootstrapAuthService(): ${JSON.stringify(error)}`, error);
      });
  }
}
