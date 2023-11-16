import {ChangeDetectionStrategy, Component, Inject, Injector} from '@angular/core';
import {ConnectorConfigDetail, DbConnectionService} from "../db-connection-editor/db-importer.service";
import {ActivatedRoute, Router} from "@angular/router";
import {TuiDialogService} from "@taiga-ui/core";
import {map, mergeMap} from "rxjs/operators";
import {Observable, of} from "rxjs";
import {SchemaMemberReference} from "../services/schema";
import {getRouterLink} from "../type-list/navigate-to-schema.member";

@Component({
  selector: 'app-connection-detail-view',
  template: `
    <app-header-component-layout [title]="(connectorSummary$ | async)?.config.connectionName"
                                 subtitle="Connections" [showBack]="false">
      <ng-container ngProjectAs="header-components">
        <app-connection-status [status]="(connectorSummary$ | async)?.config?.connectionStatus"></app-connection-status>
      </ng-container>
      <div>
        <h3>Connection Details</h3>
        <table class="connection-properties">
          <tr *ngFor="let configParam of (connectorSummary$ | async)?.config?.properties | keyvalue">
            <td class="label-col">{{configParam.key}}</td>
            <td>{{configParam.value}}</td>
          </tr>
        </table>

        <h3>Usages</h3>
        <table class="connection-properties">
          <thead>
          <tr>
            <th>Name</th>
            <th>Type</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let usage of (connectorSummary$ | async)?.usages">
            <td><a [routerLink]="getRouterLink(usage)">{{ usage.qualifiedName.shortDisplayName }}</a></td>
            <td>{{ usage.kind | titlecase}}</td>
          </tr>
          </tbody>
        </table>
      </div>

    </app-header-component-layout>

  `,
  styleUrls: ['./connection-detail-view.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConnectionDetailViewComponent {

  connectorSummary$: Observable<ConnectorConfigDetail>

  constructor(private dbService: DbConnectionService,
              private router: Router,
              private activeRoute: ActivatedRoute,
              @Inject(Injector) private readonly injector: Injector,
              @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
  ) {
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
