import { PolymorpheusTemplate, PolymorpheusOutlet } from "@taiga-ui/polymorpheus";
import { TUI_SANITIZER, TuiInputModule } from "@taiga-ui/legacy";
import { NG_EVENT_PLUGINS } from "@taiga-ui/event-plugins";
import { BrowserModule } from '@angular/platform-browser';
import { ApplicationRef, DoBootstrap, NgModule, Optional } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { LayoutModule } from '@angular/cdk/layout';
import { HttpClientModule } from '@angular/common/http';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DraftManagementBarModule } from './draft-management-bar/draft-management-bar.module';
import { HeaderBarModule } from './header-bar/header-bar.module';
import {SCHEMA_PROVIDER_TOKEN, TypesService} from './services/types.service';
import { QueryService } from './services/query.service';
import { SearchService } from './search/search.service';
import { SidenavComponent } from './sidenav/sidenav.component';
import {SystemAlertComponent} from './system-alert/system-alert.component';
import { AuthModule } from './auth/auth.module';
import { AuthService } from './auth/auth.service';
import { TUI_ALERT_POSITION, TuiRoot, TuiAlert, TuiScrollbar, TuiLink, TuiDialog, TuiButton } from '@taiga-ui/core';

import { APP_ROUTES } from 'src/app/app.routes';
import { ConfirmationDialogModule } from 'src/app/confirmation-dialog/confirmation-dialog.module';
import { WebsocketService } from 'src/app/services/websocket.service';
import { environment } from 'src/environments/environment';
import { ENVIRONMENT } from 'src/app/services/environment';
import {TuiProgress} from '@taiga-ui/kit';
import { MatNativeDateModule } from '@angular/material/core';
import { NgDompurifySanitizer } from '@taiga-ui/dompurify';
import { LANGUAGE_SERVER_WS_ADDRESS_TOKEN } from './code-editor/language-server.tokens';
import { CodeEditorModule } from './code-editor/code-editor.module';

const oauth2OidcModule = [AuthModule];


@NgModule({
  declarations: [
    AppComponent,
  ],
  imports: [
    APP_ROUTES,
    BrowserModule,
    BrowserAnimationsModule,
    CommonModule,
    LayoutModule,
    HttpClientModule,
    ...oauth2OidcModule,
    TuiRoot,
    TuiAlert,
    TuiLink,
    TuiDialog,
    TuiInputModule,
    PolymorpheusTemplate, PolymorpheusOutlet,
    TuiButton,
    ConfirmationDialogModule,
    TuiScrollbar,
    MatNativeDateModule,
    CodeEditorModule,
    DraftManagementBarModule,
    HeaderBarModule,
    SidenavComponent,
    SystemAlertComponent,
    ...TuiProgress,
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
    {
      provide: SCHEMA_PROVIDER_TOKEN,
      useExisting: TypesService
    },
    DatePipe,
    { provide: TUI_ALERT_POSITION, useValue: '2rem auto 0 auto' },
    NG_EVENT_PLUGINS
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
