import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { TuiBadgeModule, TuiStatus } from '@taiga-ui/kit';
import { ConnectionStatus } from "../../db-connection-editor/db-importer.service";
import { CommonModule } from "@angular/common";
import { MomentModule } from "ngx-moment";

@Component({
  selector: 'app-connection-status',
  template: `
    <div *ngIf="status" class="status">
      <tui-badge [status]="getBadgeState()" size="s" [value]="getBadgeLabel()"></tui-badge>
      <span *ngIf="!hideTimestamp" class="timestamp">({{ status.timestamp | amTimeAgo }})</span>
    </div>
  `,
  styleUrls: ['./connection-status.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [
    CommonModule, MomentModule, TuiBadgeModule
  ]
})
export class ConnectionStatusComponent {

  @Input()
  status: ConnectionStatus

  @Input()
  hideTimestamp: boolean;

  getBadgeLabel(): string {
    switch(this.status.status) {
      case 'OK' :
        return 'Healthy'
      case 'UNKNOWN' :
        return 'Unknown'
      case 'CONNECTING' :
        return 'Connecting'
      case 'ERROR' :
        return 'Unhealthy'
    }
  }

  getBadgeState(): TuiStatus {
    switch(this.status.status) {
      case 'OK' :
        return 'success'
      case 'UNKNOWN' :
        return 'neutral'
      case 'CONNECTING' :
        return 'neutral'
      case 'ERROR' :
        return 'error'
    }
  }
}
