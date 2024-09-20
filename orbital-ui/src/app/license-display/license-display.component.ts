import { Component, EventEmitter, Input, Output } from "@angular/core";
import { TuiButtonModule, TuiHintModule, TuiLinkModule } from "@taiga-ui/core";
import { ShortNumberPipe } from "../utils/short-number.pipe";
import {LicenseService, LicenseWithUsage} from "./license.service";
import { CommonModule, NgIf } from "@angular/common";

@Component({
  selector: 'app-license-display',
  standalone: true,
  imports: [
    CommonModule,
    TuiButtonModule,
    ShortNumberPipe,
    TuiLinkModule,
    TuiHintModule
  ],
  template: `
    @if (!license) {
      <div class="plan-header">
        <h3>Plan</h3>
      </div>
      <strong>No license found</strong>
      <div class="">
        Please contact <a href="mailto:support@orbitalhq.com" target="_blank" class="link">support&#64;orbitalhq.com</a>
        <br />or reach out on <a
        href="https://join.slack.com/t/orbitalapi/shared_invite/zt-697laanr-DHGXXak5slqsY9DqwrkzHg" target="_blank" class="link">Slack</a>
        for assistance.
      </div>
    } @else {
      <div class="plan-header">
        <h3>Plan</h3>
        <div class="right-content">
          <span class="plan-badge">{{ license.licenseSummary.plan }}</span>
          <a
            tuiLink
            tuiHint="Refresh license"
            tuiHintAppearance="onDark"
            class="button-link"
            (click)="refreshLicense.emit()"
          >
            <img src="assets/img/tabler/refresh.svg">
          </a>
        </div>
      </div>
      <div class="limits">
        <ng-container *ngFor="let usage of license.usage">
          <span>{{ usage.quota.capability }}s:</span>
          <span class="quota">
            <span [class]="usage.health.toLowerCase()" class="usage">{{ usage.usage | shortNumber:1 }}</span> /
            <span [class.inifity]="usage.quota.limit === -1" class="limit">{{ usage.quota.limit === -1 ? '&infin;' : usage.quota.limit | shortNumber:1 }}</span>
          </span>
        </ng-container>
      </div>
      <div>Want more?<br />Upgrade your plan or <a href="mailto:support@orbitalhq.com" target="_blank" class="link">contact
        support</a><br />for personalised options
      </div>
      <button
        tuiButton
        type="button"
        appearance="outline"
        size="m"
        class="manage-button"
        (click)="gotoLicenseManagement()"
      >
        Manage
      </button>
    }
  `,
  styleUrl: './license-display.component.scss'
})
export class LicenseDisplayComponent {
  @Input()
  license: LicenseWithUsage;

  @Input()
  licenseServerEndpoint: string;

  @Output()
  refreshLicense: EventEmitter<string> = new EventEmitter();

  gotoLicenseManagement() {
    window.open(this.licenseServerEndpoint, '_blank');
  }

}
