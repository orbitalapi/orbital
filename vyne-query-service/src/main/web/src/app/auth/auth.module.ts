import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { AuthService } from './auth.service';
import { OAuthModule } from 'angular-oauth2-oidc';
import { MatLegacyDialogModule as MatDialogModule } from '@angular/material/legacy-dialog';

@NgModule({
    imports: [
        MatDialogModule,
        CommonModule,
        OAuthModule.forRoot({
            resourceServer: {
                sendAccessToken: false
            }
        })
    ],
    declarations: [],
    providers: [AuthService]
})
export class AuthModule {
}
