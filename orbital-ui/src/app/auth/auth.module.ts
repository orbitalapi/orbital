import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {AuthService} from './auth.service';
import {OAuthModule} from 'angular-oauth2-oidc';
import {environment} from '../../environments/environment';

@NgModule({
  imports: [
    CommonModule,
    OAuthModule.forRoot({
      resourceServer: {
        sendAccessToken: true,
        allowedUrls: [environment.serverUrl]
      }
    })
  ],
  declarations: [],
  providers: [AuthService]
})
export class AuthModule {
}
