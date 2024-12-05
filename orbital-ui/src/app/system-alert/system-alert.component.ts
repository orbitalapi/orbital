import {CommonModule} from '@angular/common';
import {Component, Input} from '@angular/core';
import {TuiButton} from '@taiga-ui/core';

@Component({
  selector: 'app-system-alert',
  standalone: true,
  imports: [
    CommonModule,
    TuiButton,
  ],
  template: `
    <div *ngIf="alert" [ngClass]="alert.severity.toLowerCase()" class="container">
      <span class="h3">{{ alert.message }}</span>
      <div class="action-container" *ngIf="alert.actionLabel">
        <button tuiButton appearance="neutral" size="s" (click)="alert.handler()">
          {{ alert.actionLabel }}
        </button>
      </div>
    </div>
  `,
  styleUrls: ['./system-alert.component.scss'],
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
