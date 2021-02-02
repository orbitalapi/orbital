import {Component, Input} from '@angular/core';
import {VyneUser} from '../services/user-info.service';
import {AuthService} from '../auth/auth.service';
import {MatDialog} from '@angular/material/dialog';
import {CaskConfirmDialogComponent} from '../cask-viewer/cask-confirm-dialog.component';

@Component({
  selector: 'app-avatar',
  template: `
    <button mat-icon-button [matMenuTriggerFor]="menu">
      <mat-icon *ngIf="!user?.profileUrl" class="avatar">account_circle</mat-icon>
      <img class="avatar" *ngIf="user.profileUrl" [src]="user.profileUrl" alt="User profile">
    </button>
    <mat-menu #menu="matMenu">
      <div class="menu-header">
        <span>
        {{user.username}}
      </span>
        <span>
        {{user.email}}
      </span>
      </div>

      <button mat-menu-item (click)="logout()">
        Logout
      </button>
    </mat-menu>

  `,
  styleUrls: ['./avatar.component.scss']
})
export class AvatarComponent {

  constructor(private authService: AuthService, private dialog: MatDialog) {
  }

  @Input()
  user: VyneUser;

  logout() {
    const dialogRef = this.dialog.open(CaskConfirmDialogComponent, {
      data: {title: 'Logout', message: 'Are you sure?'}
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true) {
        this.authService.logout();
      }
    });
  }

}
