import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {ConnectionStatus} from "../db-connection-editor/db-importer.service";
import {CommonModule} from "@angular/common";
import {MomentModule} from "ngx-moment";

@Component({
  selector: 'app-connection-status',
  template: `
    <div *ngIf="status">
      <div class="row status">
        <span class="dot" [ngClass]="status.status"></span>
        <span>{{ status.message }}</span>
      </div>
      <div class="row timestamp-row">
        <span>{{ status.timestamp | amTimeAgo }}</span>
      </div>
    </div>

  `,
  styleUrls: ['./connection-status.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [
    CommonModule, MomentModule
  ]
})
export class ConnectionStatusComponent {

  @Input()
  status: ConnectionStatus

}
