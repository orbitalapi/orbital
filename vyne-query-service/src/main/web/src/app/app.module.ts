import { BrowserModule } from '@angular/platform-browser';
import { ApplicationRef, DoBootstrap, NgModule, Optional } from '@angular/core';

import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { LayoutModule } from '@angular/cdk/layout';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { TypesService } from './services/types.service';
import { QueryService } from './services/query.service';
import { SearchService } from './search/search.service';
import { VyneModule } from './vyne/vyne.module';
import { AuthModule } from './auth/auth.module';
import { AuthService } from './auth/auth.service';
import { ConfirmationDialogComponent } from './confirmation-dialog/confirmation-dialog.component';
import { TuiLinkModule, TuiRootModule } from '@taiga-ui/core';
import { LandingPageModule } from './landing-page/landing-page.module';
import { ORBITAL_ROUTES } from 'src/app/orbital.routes';
import { ConfirmationDialogModule } from 'src/app/confirmation-dialog/confirmation-dialog.module';


const oauth2OidcModule = [AuthModule];

@NgModule({
  declarations: [
    AppComponent,
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
    LandingPageModule,
    ConfirmationDialogModule
  ],
  providers: [
    TypesService,
    QueryService,
    SearchService
  ],
  entryComponents: [AppComponent]
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
