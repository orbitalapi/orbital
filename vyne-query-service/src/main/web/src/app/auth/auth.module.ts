import {CommonModule} from '@angular/common';
import {HttpClient, HttpClientModule} from '@angular/common/http';
import {NgModule} from '@angular/core';
import {AuthService} from './auth.service';
import {OAuthModule} from 'angular-oauth2-oidc';
import {MatDialogModule} from '@angular/material/dialog';
import {CaskConfirmDialogComponent} from '../cask-viewer/cask-confirm-dialog.component';

@NgModule({
  imports: [
    MatDialogModule,
    CommonModule,
    HttpClientModule,
    OAuthModule.forRoot({
      resourceServer: {
        sendAccessToken: false
      }
    })
  ],
  declarations: [],
  providers: [AuthService],
  entryComponents: [CaskConfirmDialogComponent]
})
export class AuthModule {
}
