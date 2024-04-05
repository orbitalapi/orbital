import { AsyncPipe, CommonModule, KeyValuePipe, TitleCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TuiNotificationModule } from '@taiga-ui/core';
import { Observable, } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { ConnectorConfigDetail, DbConnectionService } from '../db-connection-editor/db-importer.service';
import { SchemaMemberReference } from '../services/schema';
import { getRouterLink } from '../type-list/navigate-to-schema.member';
import { ConnectionStatusComponent } from './connection-status/connection-status.component';

@Component({
  selector: 'app-data-source-detail-view',
  template: `
    <ng-container *ngIf="(connectorSummary$ | async) as connectorSummary">
      <div class="header-text">
        <h2>{{connectorSummary.config.connectionName}}</h2>
        <app-connection-status [status]="connectorSummary.config?.connectionStatus"></app-connection-status>
      </div>
      <tui-notification class="error-notification" *ngIf="connectorSummary.config.connectionStatus.status === 'ERROR'" status="error">
        {{connectorSummary.config.connectionStatus.message}}
      </tui-notification>
      <table class="connection-properties">
        <tr *ngFor="let configParam of connectorSummary.config?.properties | keyvalue">
          <td class="label-col">{{ configParam.key }}</td>
          <td>{{ configParam.value }}</td>
        </tr>
      </table>
      <!--<h3>Usages</h3>
      <table class="connection-properties">
        <thead>
        <tr>
          <th>Name</th>
          <th>Type</th>
        </tr>
        </thead>
        <tbody>
        <tr *ngFor="let usage of connectorSummary.usages">
          <td><a [routerLink]="getRouterLink(usage)">{{ usage.qualifiedName.shortDisplayName }}</a></td>
          <td>{{ usage.kind | titlecase }}</td>
        </tr>
        </tbody>
      </table>-->
    </ng-container>
  `,
  styleUrls: ['./data-source-detail-view.component.scss'],
  standalone: true,
  imports: [
    AsyncPipe,
    ConnectionStatusComponent,
    KeyValuePipe,
    TitleCasePipe,
    RouterLink,
    CommonModule,
    TuiNotificationModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DataSourceDetailViewComponent {

  connectorSummary$: Observable<ConnectorConfigDetail>

  constructor(private dbService: DbConnectionService, private activeRoute: ActivatedRoute) {
    this.connectorSummary$ = activeRoute.paramMap.pipe(
      mergeMap(params => {
        const packageUri = params.get('packageUri');
        const connectionName = params.get('connectionName');

        return dbService.getConnectionByName(packageUri, connectionName)
      })
    )
  }

  getRouterLink(schemaMemberReference: SchemaMemberReference) {
    return getRouterLink(schemaMemberReference);
  }
}
