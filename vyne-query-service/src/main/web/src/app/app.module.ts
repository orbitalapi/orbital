import {BrowserModule} from '@angular/platform-browser';
import {ApplicationRef, DoBootstrap, NgModule, Optional} from '@angular/core';

import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {LayoutModule} from '@angular/cdk/layout';
import {CommonModule} from '@angular/common';
import {HttpClientModule} from '@angular/common/http';
import {TypesService} from './services/types.service';
import {QueryService} from './services/query.service';
import {SearchService} from './search/search.service';
import {VyneModule} from './vyne/vyne.module';
import {AuthModule} from './auth/auth.module';
import {AuthService} from './auth/auth.service';
import {
  TUI_SANITIZER,
  TuiAlertModule,
  TuiButtonModule,
  TuiDialogModule,
  TuiLinkModule,
  TuiRootModule,
  TuiScrollbarModule
} from '@taiga-ui/core';
import {LandingPageModule} from './landing-page/landing-page.module';
import {VYNE_ROUTES} from 'src/app/vyne.routes';
import {ConfirmationDialogModule} from 'src/app/confirmation-dialog/confirmation-dialog.module';
import {WebsocketService} from 'src/app/services/websocket.service';
import {environment} from 'src/environments/environment';
import {ENVIRONMENT} from 'src/app/services/environment';
import {TuiInputModule} from '@taiga-ui/kit';
import {PolymorpheusModule} from "@tinkoff/ng-polymorpheus";
import {TuiDialogHostModule, TuiFocusTrapModule, TuiOverscrollModule} from "@taiga-ui/cdk";
import {MatNativeDateModule} from "@angular/material/core";
import {NgDompurifySanitizer} from "@tinkoff/ng-dompurify";
import {LANGUAGE_SERVER_WS_ADDRESS_TOKEN} from "./code-editor/langServer.service";


const oauth2OidcModule = [AuthModule];


@NgModule({
    declarations: [
        AppComponent,
    ],
    imports: [
        VYNE_ROUTES,
        BrowserModule,
        BrowserAnimationsModule,
        CommonModule,
        LayoutModule,
        HttpClientModule,
        VyneModule,
        ...oauth2OidcModule,
        TuiRootModule,
        TuiAlertModule,
        TuiLinkModule,
        TuiDialogModule,
        TuiInputModule,
        PolymorpheusModule,
        TuiButtonModule,
        LandingPageModule,
        ConfirmationDialogModule,
        TuiFocusTrapModule,
        TuiScrollbarModule,
        TuiOverscrollModule,
        TuiDialogHostModule,
        MatNativeDateModule,
    ],
    providers: [
        TypesService,
        QueryService,
        {
            provide: TUI_SANITIZER,
            useClass: NgDompurifySanitizer,
        },
        SearchService,
        {
            provide: LANGUAGE_SERVER_WS_ADDRESS_TOKEN,
            useValue: WebsocketService.buildWsUrl(environment.serverUrl, '/api/language-server'),
        },
        {
            provide: ENVIRONMENT,
            useValue: environment,
        },
    ],
    exports: []
})
export class AppModule implements DoBootstrap {
  constructor(@Optional() private authService: AuthService) {
  }

  ngDoBootstrap(appRef: ApplicationRef): void {
    this.authService.bootstrapAuthService()
      .then(() => {
        console.log('bootstrapping the application');
        appRef.bootstrap(AppComponent);
      })
      .catch(error => {
        console.error(`[ngDoBootstrap] Problem while authService.bootstrapAuthService(): ${JSON.stringify(error)}`, error);
      });
  }
}
