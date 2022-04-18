import {Component, Input} from '@angular/core';
import {ConnectorSummary, DbConnectionService} from '../db-connection-editor/db-importer.service';
import {Observable} from 'rxjs';
import {Router} from '@angular/router';

@Component({
  selector: 'app-connection-list',
  template: `
    <h2>Connections</h2>
    <div *ngIf="(connections | async)?.length > 0; else empty">
      <p>
        Create connections to manually register systems (databases, APIs and Message Buses) to Vyne.
        <br/>
        Alternatively, you can have systems publish their definitions directly to Vyne.
      </p>
      <div class="page-button-row">
        <button tuiButton size="m" (click)="createNewConnection()" appearance="secondary">Create new connection</button>
      </div>
      <table class="connection-list">
        <thead>
        <tr>
          <th>Name</th>
          <th>Type</th>
          <th>Address</th>
          <!--        <th></th>-->
        </tr>
        </thead>
        <tbody>
        <tr *ngFor="let connection of connections | async">
          <td>{{ connection.connectionName }}</td>
          <td>{{ connection.driverName | titlecase }}</td>
          <td>{{ connection.address }}</td>
          <!-- Don't support editing right now - see db-connection-editor.component.ts :: rebuildForm() for now -->
          <!--        <td><a tuiLink [routerLink]="[connection.type.toLowerCase(), connection.connectionName]">Manage</a>-->
          <!--        </td>-->
        </tr>
        </tbody>
      </table>
    </div>
    <ng-template #empty>
      <div class="empty-state-container">
        <img src="assets/img/illustrations/data-settings.svg">
        <p>
          Create connections to manually register systems (databases, APIs and Message Buses) to Vyne.
          <br/>
          Alternatively, you can have systems publish their definitions directly to Vyne.
        </p>
        <button tuiButton size="l" appearance="primary" (click)="createNewConnection()">Create new connection</button>
      </div>
    </ng-template>
  `,
  styleUrls: ['./connection-list.component.scss']
})
export class ConnectionListComponent {

  constructor(private dbService: DbConnectionService, private router: Router) {
    this.connections = dbService.getConnections();
  }

  @Input()
  connections: Observable<ConnectorSummary[]>;

  createNewConnection() {
    this.router.navigate(['connection-manager', 'new']);
  }

}
