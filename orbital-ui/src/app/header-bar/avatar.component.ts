import {Component, Inject, Input} from '@angular/core';
import {VyneUser} from '../services/user-info.service';
import {AuthService} from '../auth/auth.service';
import {MatLegacyDialog as MatDialog} from '@angular/material/legacy-dialog';
import {ConfirmationDialogComponent} from 'src/app/confirmation-dialog/confirmation-dialog.component';
import {TuiAlertService, TuiDialogService} from "@taiga-ui/core";
import {TUI_PROMPT, TuiPromptData} from "@taiga-ui/kit";

@Component({
  selector: 'app-avatar',
  styleUrls: ['./avatar.component.scss'],
  template: `
      <tui-hosted-dropdown [content]="userMenu">
          <tui-avatar [text]="user.username" [rounded]="true" [autoColor]="true" [avatarUrl]="user.profileUrl"
                      size="s"></tui-avatar>
      </tui-hosted-dropdown>
      <ng-template #userMenu>
          <tui-data-list>
              <tui-opt-group [label]="user.username">
                  <ng-container *ngIf="user?.isAuthenticated">
                      <a tuiOption type="button"  target="_blank" [href]="authService.securityConfig.accountManagementUrl"
                         *ngIf="authService.securityConfig.accountManagementUrl">Account settings</a>
                    <a tuiOption type="button" target="_blank" [href]="authService.securityConfig.orgManagementUrl"
                       *ngIf="authService.securityConfig.orgManagementUrl">Organisation settings</a>
                      <button tuiOption (click)="logout()" *ngIf="user?.isAuthenticated">
                          Logout
                      </button>
                  </ng-container>

              </tui-opt-group>
          </tui-data-list>
      </ng-template>
  `,

})
export class AvatarComponent {

  constructor(readonly authService: AuthService,
              @Inject(TuiDialogService) private readonly dialogs: TuiDialogService,
              @Inject(TuiAlertService) private readonly alerts: TuiAlertService,
  ) {
  }

  @Input()
  user: VyneUser;

  logout() {
    const data: TuiPromptData = {
      content: 'Are you sure you want to logout?',
      yes: 'Logout',
      no: 'Cancel'
    }
    this.dialogs.open<boolean>(TUI_PROMPT, {
      label: 'Logout',
      size: 's',
      data
    })
      .subscribe(result => {
        if (result) {
          this.authService.logout();
        }
      });
  }
}
