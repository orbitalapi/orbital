import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, Input, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TuiHintModule, TuiNotificationModule } from '@taiga-ui/core';
import { TuiTreeModule } from '@taiga-ui/kit';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import {
  ConnectionsListResponse,
  ConnectorSummary,
  DbConnectionService
} from '../../db-connection-editor/db-importer.service';
import {
  collectAllServiceOperations,
  OperationKind,
  Schema,
  Service,
  ServiceKind
} from '../../services/schema';
import { ConnectionStatusComponent } from '../connection-status/connection-status.component';

interface TreeEntry {
  connectorSummary: ConnectorSummary
  services: Service[]
}

@Component({
  selector: 'app-data-source-tree',
  standalone: true,
  imports: [
    CommonModule, TuiTreeModule, TuiHintModule, RouterLink, RouterLinkActive, ConnectionStatusComponent,
    TuiNotificationModule
  ],
  template: `
    <ng-container [tuiTreeController]="true">
      <tui-tree-item class="root-tree-item" *ngFor="let entry of (treeData$ | async) | keyvalue">
        <img class="tree-icon" src="assets/img/tabler/folder-code.svg">
        {{entry.key}}
        <tui-tree-item *ngFor="let connection of entry.value" class="show-tree-decoration">
          <!--// NOTE: these spans are here so we can use routerLink and to prevent the mouse event bubbling up through the tree-->
          <span
            class="is-navigable"
            [routerLink]="connection.connectorSummary.packageIdentifier.uriSafeId+'/'+connection.connectorSummary.connectionName"
            routerLinkActive="active"
          >
            <img class="tree-icon" src="assets/img/tabler/plug.svg">
            <span class="connection-name">{{connection.connectorSummary.connectionName}}</span>
            <app-connection-status [status]="connection.connectorSummary.connectionStatus" [hideTimestamp]="true"></app-connection-status>
          </span>
          <tui-tree-item *ngFor="let service of connection.services" class="show-tree-decoration">
            <span
              class="is-navigable"
              [routerLink]="'services/'+service.memberQualifiedName.fullyQualifiedName"
              [routerLinkActiveOptions]="{exact: true}"
              routerLinkActive="active"
            >
              <img class="tree-icon" [src]=serviceIcon(service.serviceKind)>{{ service.memberQualifiedName.shortDisplayName }}
            </span>
            <tui-tree-item *ngFor="let operation of collectAllServiceOperations(service)" class="show-tree-decoration">
              <span
                class="is-navigable"
                [routerLink]="'services/'+service.memberQualifiedName.fullyQualifiedName+'/'+operation.memberQualifiedName.shortDisplayName"
                routerLinkActive="active"
              >
                <img class="tree-icon" [src]="operationIcon(operation.operationKind)">{{operation.memberQualifiedName.shortDisplayName}}
              </span>
            </tui-tree-item>
          </tui-tree-item>
        </tui-tree-item>
      </tui-tree-item>
    </ng-container>
  `,
  styleUrls: ['./data-source-tree.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})

export class DataSourceTreeComponent implements OnInit {

  @Input()
  connections$: Observable<ConnectionsListResponse>

  @Input()
  schema$: Observable<Schema>

  treeData$: BehaviorSubject<Map<string, TreeEntry[]>> = new BehaviorSubject<Map<string, TreeEntry[]>>(new Map());

  constructor(private dbService: DbConnectionService, private destroyRef: DestroyRef) {

  }

  ngOnInit(): void {
    // TODO: what should we do with the definitionsWithErrors from this subscription
    combineLatest([this.schema$, this.connections$])
      .pipe(
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(([schema, {connections, definitionsWithErrors}]) => {
        // make a map of the connections grouping them by their packageIdentifier.uriSafeId
        const map = connections.reduce((acc, item) => {
          const {packageIdentifier: {uriSafeId}} = item
          return acc.set(uriSafeId, [...acc.get(uriSafeId) || [], {
            connectorSummary: item,
            services: schema.services.filter(service => {
              return item.usages.find(usage => {
                return usage.qualifiedName.fullyQualifiedName === service.memberQualifiedName.fullyQualifiedName
              })
            })
          }])
        }, new Map<string, TreeEntry[]>());

        this.treeData$.next(map)
      })
  }

  serviceIcon(serviceKind: ServiceKind) {
    switch (serviceKind) {
      case 'Database' :
        return 'assets/img/chart-icons/database-icon.svg'
      case 'Kafka' :
        return 'assets/img/data-source-icons/kafka-icon.svg'
      case 'API' :
        return 'assets/img/chart-icons/api-icon.svg'
      default :
        return 'assets/img/chart-icons/api-icon.svg'
    }
  }

  operationIcon(operationKind: OperationKind) {
    switch (operationKind) {
      case 'Table' :
        return 'assets/img/tabler/table.svg'
      // TODO: need icons for all of these
      case 'Stream' :
        //return 'assets/img/tabler/chart-arrows.svg'
        return 'assets/img/tabler/mail.svg'
      case 'ApiCall' :
      case 'Query' :
      default :
        return 'assets/img/tabler/file-description.svg'
    }
  }

  protected readonly collectAllServiceOperations = collectAllServiceOperations;
}
