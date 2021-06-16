import {Component, Input, OnInit} from '@angular/core';
import {ConfiguredConnectionSummary, DbConnectionService} from '../db-connection-editor/db-importer.service';
import {Observable} from 'rxjs/index';

@Component({
  selector: 'app-connection-list',
  template: `
    <div class="container">
      <h2>Connections</h2>
      <table class="connection-list">
        <thead>
        <tr>
          <th>Connection name</th>
          <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr *ngFor="let connection of connections | async">
          <td>{{ connection.connectionName }}</td>
          <td></td>
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
  connections: Observable<ConfiguredConnectionSummary[]>;

}
