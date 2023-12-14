import {Component, Input, OnInit, Output} from '@angular/core';

@Component({
  selector: 'app-system-alert',
  template: `
    <div *ngIf="alert" [ngClass]="alert.severity.toLowerCase()" class="container">
      <span>{{ alert.message }}</span>
      <div class="action-container" *ngIf="alert.actionLabel">
        <button mat-stroked-button (click)="alert.handler()">
          {{ alert.actionLabel }}
        </button>
      </div>
    </div>
  `,
  styleUrls: ['./system-alert.component.scss']
})
export class SystemAlertComponent {

  @Input()
  alert: SystemAlert;
}

export type AlertSeverity = 'Info' | 'Warning' | 'Error';

export interface SystemAlert {
  id: string | null;
  message: string;
  actionLabel: string | null;
  handler: () => void | null;
  severity: AlertSeverity;
}
