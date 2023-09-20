import { Component, Input } from '@angular/core';
import { VyneUser } from '../services/user-info.service';
import { AuthService } from '../auth/auth.service';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from 'src/app/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-avatar',
  template: `
    <button mat-icon-button [matMenuTriggerFor]="menu">
      <tui-avatar [text]="user.username" [rounded]="true" [avatarUrl]="user.profileUrl" size="s"></tui-avatar>
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
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: { title: 'Logout', message: 'Are you sure?' }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true) {
        this.authService.logout();
      }
    });
  }

}
