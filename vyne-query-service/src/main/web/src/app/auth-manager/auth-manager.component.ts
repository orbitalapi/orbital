import { Component } from '@angular/core';
import {AuthManagerService, AuthScheme, AuthTokenMap, NoCredentialsAuthToken} from './auth-manager.service';
import { Observable } from 'rxjs/internal/Observable';
import { MatLegacySnackBar as MatSnackBar } from '@angular/material/legacy-snack-bar';

@Component({
  selector: 'app-auth-manager',
  template: `
      <app-token-list
              class="centered-page-block"
              [tokens$]="tokens" (newTokenSaved)="refreshTokenList()"
              (deleteToken)="deleteToken($event)"></app-token-list>
  `,
  styleUrls: ['./auth-manager.component.scss']
})
export class AuthManagerComponent {
  tokens: Observable<AuthTokenMap>;

  constructor(private service: AuthManagerService, private snackBar: MatSnackBar) {
    this.refreshTokenList();
  }

  refreshTokenList(): void {
    this.tokens = this.service.getAllTokens();
  }

  deleteToken($event: NoCredentialsAuthToken): void {
    this.service.deleteToken($event.serviceName)
      .subscribe(result => {
          this.snackBar.open('Authentication token successfully deleted', 'Dismiss', { duration: 3000 });
          this.refreshTokenList();
        },
        error => {
          console.log('Failed to delete auth token: ' + JSON.stringify(error));
          this.snackBar.open(`Failed to delete the token.  ${error.message}`, 'Dismiss');
        });
  }
}
