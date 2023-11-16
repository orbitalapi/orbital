import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {ConnectionStatus} from "../db-connection-editor/db-importer.service";

@Component({
  selector: 'app-connection-status',
  template: `
    <div *ngIf="status">
      <div class="row status">
        <span class="dot" [ngClass]="status.status"></span>
        <span>{{ statusMessage }}</span>
      </div>
      <div class="row timestamp-row">
        <span>{{ status.timestamp | amTimeAgo }}</span>
      </div>
    </div>

  `,
  styleUrls: ['./connection-status.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConnectionStatusComponent {

  @Input()
  status: ConnectionStatus

  get statusMessage(): string {
    if (!this.status) {
      return ''
    }
    switch (this.status.status) {
      case "OK":
        return 'Healthy';
      case "UNKNOWN":
        return 'Connection status unknown';
      case "ERROR":
        return this.status.errorMessage;
    }
  }

}
