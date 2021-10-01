import {Component, Input, OnInit} from '@angular/core';
import {ConnectorSummary, DbConnectionService} from '../db-connection-editor/db-importer.service';
import {Observable} from 'rxjs/index';

@Component({
  selector: 'app-connection-list',
  template: `
    <div class="container">
      <h2>Connections</h2>
      <table class="connection-list">
        <thead>
        <tr>
          <th>Name</th>
          <th>Type</th>
          <th>Address</th>
          <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr *ngFor="let connection of connections | async">
          <td>{{ connection.connectionName }}</td>
          <td>{{ connection.driverName }}</td>
          <td>{{ connection.address }}</td>
          <td><a mat-stroked-button [routerLink]="[connection.type.toLowerCase(), connection.connectionName]">Manage</a> </td>
        </tr>
        </tbody>
      </table>
    </div>
  `,
  styleUrls: ['./connection-list.component.scss']
})
export class ConnectionListComponent {

  constructor(private dbService: DbConnectionService) {
    this.connections = dbService.getConnections();
  }

  @Input()
  connections: Observable<ConnectorSummary[]>;

}
