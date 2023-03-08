import { Component, Input } from '@angular/core';
import { ConnectorSummary, DbConnectionService } from '../db-connection-editor/db-importer.service';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';

@Component({
  selector: 'app-connection-list',
  template: `
    <app-header-component-layout title="Connections"
                                 [description]="'Create connections to register databases and message brokers to Orbital. '">
      <ng-container ngProjectAs="buttons">
        <button tuiButton size="m" (click)="createNewConnection()"
                [appearance]="(connections | async)?.length > 0 ? 'outline' : 'primary'">Add connection
        </button>
      </ng-container>

      <div *ngIf="(connections | async)?.length > 0;" class='connection-list-container'>
        <table class="connection-list">
          <thead>
          <tr>
            <th>Name</th>
            <th>Type</th>
            <th>Address</th>
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
      </app-header-component-layout>

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
