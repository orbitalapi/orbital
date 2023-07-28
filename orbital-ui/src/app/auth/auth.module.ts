import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {AuthService} from './auth.service';
import {OAuthModule} from 'angular-oauth2-oidc';

@NgModule({
  imports: [
    CommonModule,
    OAuthModule.forRoot({
      resourceServer: {
        sendAccessToken: false
      }
    })
  ],
  providers: [AuthService],
})
export class AuthModule {
}
