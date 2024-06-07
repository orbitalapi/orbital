import { BrowserModule } from '@angular/platform-browser';
import { ApplicationRef, DoBootstrap, NgModule, Optional } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { LayoutModule } from '@angular/cdk/layout';
import { HttpClientModule } from '@angular/common/http';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DraftManagementBarModule } from './draft-management-bar/draft-management-bar.module';
import { HeaderBarModule } from './header-bar/header-bar.module';
import { TypesService } from './services/types.service';
import { QueryService } from './services/query.service';
import { SearchService } from './search/search.service';
import { SidenavComponent } from './sidenav/sidenav.component';
import { SystemAlertModule } from './system-alert/system-alert.module';
import { AuthModule } from './auth/auth.module';
import { AuthService } from './auth/auth.service';
import {
  TUI_ALERT_POSITION,
  TUI_SANITIZER,
  TuiAlertModule,
  TuiButtonModule,
  TuiDialogModule,
  TuiLinkModule,
  TuiRootModule,
  TuiScrollbarModule
} from '@taiga-ui/core';

import { APP_ROUTES } from 'src/app/app.routes';
import { ConfirmationDialogModule } from 'src/app/confirmation-dialog/confirmation-dialog.module';
import { WebsocketService } from 'src/app/services/websocket.service';
import { environment } from 'src/environments/environment';
import { ENVIRONMENT } from 'src/app/services/environment';
import { TuiInputModule, TuiProgressModule } from '@taiga-ui/kit';
import { PolymorpheusModule } from '@tinkoff/ng-polymorpheus';
import { TuiDialogHostModule, TuiFocusTrapModule, TuiOverscrollModule } from '@taiga-ui/cdk';
import { MatNativeDateModule } from '@angular/material/core';
import { NgDompurifySanitizer } from '@tinkoff/ng-dompurify';
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
    TuiRootModule,
    TuiAlertModule,
    TuiLinkModule,
    TuiDialogModule,
    TuiInputModule,
    PolymorpheusModule,
    TuiButtonModule,
    ConfirmationDialogModule,
    TuiFocusTrapModule,
    TuiScrollbarModule,
    TuiOverscrollModule,
    TuiDialogHostModule,
    MatNativeDateModule,
    CodeEditorModule,
    DraftManagementBarModule,
    HeaderBarModule,
    SidenavComponent,
    SystemAlertModule,
    TuiProgressModule
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
    DatePipe,
    { provide: TUI_ALERT_POSITION, useValue: '2rem auto 0 auto' },
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
