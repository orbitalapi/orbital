import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { TuiButtonModule, TuiNotificationModule } from '@taiga-ui/core';
import { UiCustomisations } from '../../environments/ui-customisations';
import { HeaderComponentLayoutModule } from '../header-component-layout/header-component-layout.module';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, HeaderComponentLayoutModule, RouterOutlet, TuiButtonModule, RouterLink,
    TuiNotificationModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent {
  hideLiveReloadNotification: boolean;

  readonly uiConfig = UiCustomisations;
  private readonly HIDE_LIVE_RELOAD_NOTIFICATION_LOCAL_STORAGE_KEY = "hideLiveReloadNotification";

  constructor(private changeDetectorRef: ChangeDetectorRef) {
    this.hideLiveReloadNotification = localStorage.getItem(this.HIDE_LIVE_RELOAD_NOTIFICATION_LOCAL_STORAGE_KEY) === "true";
  }

  onHideNotification() {
    localStorage.setItem(this.HIDE_LIVE_RELOAD_NOTIFICATION_LOCAL_STORAGE_KEY, "true");
    this.hideLiveReloadNotification = true;
  }
}
