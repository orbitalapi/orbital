import {Component, Input, OnInit} from '@angular/core';
import {ConnectorSummary, DbConnectionService} from '../db-connection-editor/db-importer.service';
import {Observable} from 'rxjs/index';
import {Router} from '@angular/router';

@Component({
  selector: 'app-connection-list',
  template: `
    <h2>Connections</h2>
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
