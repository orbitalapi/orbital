import {Component, Inject, Injector, Input} from '@angular/core';
import {ConnectorSummary, DbConnectionService} from '../db-connection-editor/db-importer.service';
import {Observable} from 'rxjs';
import {Router} from '@angular/router';
import {TuiDialogService} from "@taiga-ui/core";

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
            <th>Parameters</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let connection of connections | async">
            <td>{{ connection.connectionName }}</td>
            <td>{{ connection.driverName | titlecase }}</td>
            <td>
              <table class="nested-table">
                <tr *ngFor="let configParam of connection.properties | keyvalue">
                  <td class="label-col">{{configParam.key}}</td>
                  <td>{{configParam.value}}</td>
                </tr>
              </table>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </app-header-component-layout>

  `,
  styleUrls: ['./connection-list.component.scss']
})
export class ConnectionListComponent {

  constructor(private dbService: DbConnectionService,
              private router: Router,
              @Inject(Injector) private readonly injector: Injector,
              @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
  ) {
    this.connections = dbService.getConnections();

  }

  @Input()
  connections: Observable<ConnectorSummary[]>;

  createNewConnection() {
    this.dialogService
      .open(
        `<div>
    <p>Connections are defined in your Taxi projects</p>
    <p>Click to learn more about how to <a href="https://orbitalhq.com/docs/describing-data-sources/configuring-connections">add connections</a>, or how <a href="https://orbitalhq.com/docs/deploying/managing-secrets"
                                                   target="_blank">secrets are managed</a> in our docs.</p>
</div>`,
        {label: 'Add a new connection'},
      )
      .subscribe();
    // this.router.navigate(['connection-manager', 'new']);
  }

}
