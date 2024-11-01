import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import {TuiAppearanceOptions} from '@taiga-ui/core';
import { TuiBadge } from '@taiga-ui/kit';
import { ConnectionStatus } from "../../db-connection-editor/db-importer.service";
import { CommonModule } from "@angular/common";
import { MomentModule } from "ngx-moment";

@Component({
  selector: 'app-connection-status',
  template: `
    <div *ngIf="status" class="status">
      <tui-badge [appearance]="getBadgeState()" size="m">{{ getBadgeLabel() }}</tui-badge>
      <span *ngIf="!hideTimestamp" class="timestamp">({{ status.timestamp | amTimeAgo }})</span>
    </div>
  `,
  styleUrls: ['./connection-status.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [
    CommonModule, MomentModule, TuiBadge
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

  getBadgeState(): TuiAppearanceOptions["appearance"] {
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
