import { BrowserModule } from '@angular/platform-browser';
import { ApplicationRef, DoBootstrap, NgModule, Optional } from '@angular/core';

import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { LayoutModule } from '@angular/cdk/layout';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { TypesService } from './services/types.service';
import { QueryService } from './services/query.service';
import { TypeAutocompleteModule } from './type-autocomplete/type-autocomplete.module';
import { PipelinesModule } from './pipelines/pipelines.module';
import { TypeViewerModule } from './type-viewer/type-viewer.module';
import { SearchModule } from './search/search.module';
import { CodeViewerModule } from './code-viewer/code-viewer.module';
import { SearchService } from './search/search.service';
import { QueryHistoryModule } from './query-history/query-history.module';
import { VyneModule } from './vyne/vyne.module';
import { CaskViewerModule } from './cask-viewer/cask-viewer.module';
import { AuthModule } from './auth/auth.module';
import { AuthService } from './auth/auth.service';
import { AuthManagerModule } from './auth-mananger/auth-manager.module';
import { ConfirmationDialogComponent } from './confirmation-dialog/confirmation-dialog.component';
import { ConnectionManagerModule } from './connection-manager/connection-manager.module';
import { DbConnectionEditorModule } from './db-connection-editor/db-connection-editor.module';
import { SchemaImporterModule } from './schema-importer/schema-importer.module';
import { TuiLinkModule, TuiRootModule } from '@taiga-ui/core';
import { DbConnectionEditorDialogComponent } from './db-connection-editor/db-connection-editor-dialog.component';
import { LandingPageModule } from './landing-page/landing-page.module';
import { ORBITAL_ROUTES } from 'src/app/orbital.routes';


const oauth2OidcModule = [AuthModule];


/*
if (!environment.secure) {
  oauth2OidcModule = [];
}
*/


@NgModule({
  declarations: [
    AppComponent,
    ConfirmationDialogComponent,
  ],
  imports: [
    ORBITAL_ROUTES,

    BrowserModule,
    BrowserAnimationsModule,
    CommonModule,
    LayoutModule,

    HttpClientModule,

    VyneModule,
    ...oauth2OidcModule,
    TuiRootModule,
    TuiLinkModule,
    LandingPageModule
  ],
  providers: [
    TypesService,
    QueryService,
    SearchService
  ],
  entryComponents: [AppComponent, ConfirmationDialogComponent]
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
