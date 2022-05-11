import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Observable} from 'rxjs/internal/Observable';
import {AuthTokenType, authTokenTypeDisplayName, NoCredentialsAuthToken} from './auth-manager.service';
import {MatDialog} from '@angular/material/dialog';
import {NewTokenPanelComponent} from './new-token-panel.component';
import {
  ConfirmationAction,
  ConfirmationDialogComponent,
  ConfirmationParams
} from '../confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-token-list',
  template: `
    <h2>Authentication Manager</h2>
    <div *ngIf="(tokens | async).length > 0; else empty">

      <p>
        These authentication tokens will be used when Vyne makes calls to services. The values of the tokens are not
        shown, but can be edited or deleted.
      </p>
      <div class="page-button-row">
        <button tuiButton size="m" appearance="secondary" (click)="showCreateTokenPopup()">Create new token</button>
      </div>
      <table class="token-list">
        <thead>
        <tr>
          <th>Service name</th>
          <th>Token type</th>
          <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr *ngFor="let token of tokens | async">
          <td>{{ token.serviceName }}</td>
          <td>{{ tokenTypeDisplayName(token.tokenType) }}</td>
          <td>
            <button tuiButton size="s" appearance="outline" (click)="editToken(token)">Edit</button>
            <button tuiButton size="s" appearance="outline" (click)="onDeleteTokenClicked(token)">Delete</button>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
    <ng-template #empty>
      <div class="empty-state-container">
        <img src="assets/img/illustrations/authentication.svg">
        <p>
          These authentication tokens will be used when Vyne makes calls to services. The values of the tokens are not
          shown, but can be edited or deleted.
        </p>
        <button tuiButton size="l" appearance="primary" (click)="showCreateTokenPopup()">Create new token</button>
      </div>
    </ng-template>
  `,
  styleUrls: ['./token-list.component.scss']
})
export class TokenListComponent {

  constructor(private dialogService: MatDialog) {
  }

  @Input()
  tokens: Observable<NoCredentialsAuthToken[]>;

  @Output()
  newTokenSaved = new EventEmitter<NoCredentialsAuthToken>();

  @Output()
  deleteToken = new EventEmitter<NoCredentialsAuthToken>();

  showCreateTokenPopup() {
    this.dialogService.open(NewTokenPanelComponent,
      {
        width: '1200px',
        maxWidth: '80vw',
      }
    )
      .afterClosed().subscribe(createdToken => {
      if (createdToken) {
        this.newTokenSaved.emit(createdToken);
      }
    });
  }

  onDeleteTokenClicked(token: NoCredentialsAuthToken) {
    this.dialogService.open(
      ConfirmationDialogComponent,
      {
        data: new ConfirmationParams(
          'Delete token?',
          `This will remove the token for service ${token.serviceName}.  This action cannot be undone.`
        )
      }
    ).afterClosed().subscribe((result: ConfirmationAction) => {
      if (result === 'OK') {
        this.deleteToken.emit(token);
      }
    });
  }

  editToken(token: NoCredentialsAuthToken) {
    this.dialogService.open(NewTokenPanelComponent, {
      data: token,
      width: '1200px',
      maxWidth: '80vw',
    })
      .afterClosed().subscribe(createdToken => {
      if (createdToken) {
        this.newTokenSaved.emit(createdToken);
      }
    });
  }

  tokenTypeDisplayName(tokenType: AuthTokenType): string {
    return authTokenTypeDisplayName(tokenType);
  }
}
