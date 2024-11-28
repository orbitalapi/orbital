import {HttpErrorResponse} from '@angular/common/http';
import {Component, Inject, Input} from '@angular/core';
import {PolymorpheusContent} from '@taiga-ui/polymorpheus';
import {LicenseService} from "../license-display/license.service";
import {AppInfoService} from "../services/app-info.service";
import {VyneUser} from '../services/user-info.service';
import {AuthService} from '../auth/auth.service';
import {TuiAlertService, TuiDialogContext, TuiDialogService} from '@taiga-ui/core';

@Component({
  selector: 'app-avatar',
  styleUrls: ['./avatar.component.scss'],
  template: `
    <div tuiDropdownOpen [tuiDropdown]="userMenu" tuiDropdownMaxHeight="550">
      <div class="user-container">
        <tui-avatar [round]="true"
                    size="s"
                    [src]="(user.name || user.username).slice(0, 1).toUpperCase()"
        />
        <div class="username">{{ user.name || user.username }}</div>
      </div>
    </div>
    <ng-template #userMenu>
      <tui-data-list>
        <tui-opt-group [label]="user.username">
          <ng-container *ngIf="user?.isAuthenticated">
            <a tuiOption type="button" target="_blank" [href]="authService.securityConfig.accountManagementUrl"
               *ngIf="authService.securityConfig.accountManagementUrl">Account settings</a>
            <a tuiOption type="button" target="_blank" [href]="authService.securityConfig.orgManagementUrl"
               *ngIf="authService.securityConfig.orgManagementUrl">Organisation settings</a>
            <app-license-display
              *ngIf="(appInfoService.getConfig() | async).featureToggles.enableLicenseEnforcement"
              [license]="licenseService.license | async"
              [licenseServerEndpoint]="(appInfoService.getConfig() | async).licenseServerEndpoint"
              (refreshLicense)="licenseService.getLicenseDetails(true)"
            ></app-license-display>
            <app-cluster-members-display *appRequiresAuthority="['ViewClusterInfo']">
            </app-cluster-members-display>
            <button tuiOption (click)="showDialog(template)" *ngIf="user?.isAuthenticated" class="logout-button">
              <img class="logout-icon filter-black-ish" src="assets/img/tabler/logout.svg">
              Logout
            </button>
          </ng-container>
        </tui-opt-group>
      </tui-data-list>
    </ng-template>
    <ng-template
      #template
      let-observer
    >
      <p>Are you sure you want to logout?</p>
      <div class="logout-buttons">
        <button
          size="m"
          tuiButton
          type="button"
          appearance="secondary"
          (click)="observer.complete()"
        >
          Cancel
        </button>
        <button
          size="m"
          tuiButton
          type="button"
          class="tui-space_right-3"
          [loading]="isLoggingOut"
          (click)="logout()"
        >
          Logout
        </button>
      </div>
      <tui-notification size="m" *ngIf="errorMessage" appearance="error">{{ errorMessage }}</tui-notification>
    </ng-template>
  `,
})
export class AvatarComponent {
  constructor(
    readonly authService: AuthService,
    @Inject(TuiDialogService) private readonly dialogs: TuiDialogService,
    @Inject(TuiAlertService) private readonly alerts: TuiAlertService,
    public licenseService: LicenseService,
    public appInfoService: AppInfoService
  ) {
  }

  @Input()
  user: VyneUser;

  isLoggingOut: boolean = false;
  errorMessage: string;

  showDialog(content: PolymorpheusContent<TuiDialogContext>): void {
    this.errorMessage = null;
    this.dialogs.open(content, {size: 's', label: 'Logout'}).subscribe();
  }

  async logout() {
    this.isLoggingOut = true;
    this.errorMessage = null;
    if (this.user.authenticationType === 'Oidc')  {
      console.log('Performing OIDC logout');
      const logoutConfirmation = await this.authService.logoutOidc();
      if (logoutConfirmation instanceof HttpErrorResponse) {
        this.errorMessage = logoutConfirmation.error?.['error_description'] || logoutConfirmation.message
      }
      this.isLoggingOut = false;
    } else if (this.user.authenticationType === 'Saml') {
      console.log('Performing Saml Logout');
      await this.authService.samlLogout();
      this.isLoggingOut = false;
    }
  }
}
