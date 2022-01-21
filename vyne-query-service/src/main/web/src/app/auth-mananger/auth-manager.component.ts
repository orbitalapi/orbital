import {Component, OnInit} from '@angular/core';
import {AuthManagerService, NoCredentialsAuthToken} from './auth-manager.service';
import {Observable} from 'rxjs/internal/Observable';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-auth-manager',
  template: `
    <app-header-bar title="Authentication Manager">
    </app-header-bar>
    <div class="page-content centered-page-block-container">
      <app-token-list
        class="centered-page-block"
        [tokens]="tokens" (newTokenSaved)="refreshTokenList()"
                      (deleteToken)="deleteToken($event)"></app-token-list>
    </div>
  `,
  styleUrls: ['./auth-manager.component.scss']
})
export class AuthManagerComponent {
  tokens: Observable<NoCredentialsAuthToken[]>;

  constructor(private service: AuthManagerService, private snackBar: MatSnackBar) {
    this.refreshTokenList();
  }

  refreshTokenList() {
    this.tokens = this.service.getAllTokens();
  }

  deleteToken($event: NoCredentialsAuthToken) {
    this.service.deleteToken($event.serviceName)
      .subscribe(result => {
          this.snackBar.open('Authentication token successfully deleted', 'Dismiss', {duration: 3000});
          this.refreshTokenList();
        },
        error => {
          console.log('Failed to delete auth token: ' + JSON.stringify(error));
          this.snackBar.open(`Failed to delete the token.  ${error.message}`, 'Dismiss');
        });
  }
}
