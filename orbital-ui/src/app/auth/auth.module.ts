import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {AuthService} from './auth.service';
import {OAuthModule} from 'angular-oauth2-oidc';
import {environment} from '../../environments/environment';
import {authenticationInterceptor} from "./auth.interceptor";

@NgModule({
  imports: [
    CommonModule,
    OAuthModule.forRoot(
      {
        resourceServer: {
          // We manually configure sending access tokens
          sendAccessToken: false,
          allowedUrls: [environment.serverUrl]
        }
      }
    )
  ],
  declarations: [],
  providers: [AuthService, authenticationInterceptor]
})
export class AuthModule {
}
