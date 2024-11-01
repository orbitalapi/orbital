import { TuiProgress } from "@taiga-ui/kit";
import {ChangeDetectionStrategy, Component, signal} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TuiNotification, TuiButton } from '@taiga-ui/core';
import { ConnectionStatusComponent } from '../../data-source-manager/connection-status/connection-status.component';
import {ConnectorSummary, DbConnectionService, PackageWithError} from '../../db-connection-editor/db-importer.service';
import {SchemaNotificationService} from '../../services/schema-notification.service';
import { CardComponent } from '../card/card.component';
import {StatisticModule} from "../../statistic/statistic.module";

@Component({
  selector: 'app-data-sources-card',
  standalone: true,
  imports: [
    CommonModule, CardComponent, TuiProgress, TuiButton, RouterLink, ConnectionStatusComponent,
    TuiNotification,
    StatisticModule
  ],
  templateUrl: './data-sources-card.component.html',
  styleUrls: ['./data-sources-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DataSourcesCardComponent {
  connectionsLoading = signal<boolean>(true)
  description = signal<string>("Loading...");
  healthyConnections = signal<ConnectorSummary[]>([])
  unhealthyConnections = signal<ConnectorSummary[]>([])
  definitionsWithErrors = signal<PackageWithError[]>([])

  constructor(
    private dbService: DbConnectionService,
    private schemaNotificationService: SchemaNotificationService
  ) {
    this.schemaNotificationService.createSchemaNotificationsSubscription().pipe(
      takeUntilDestroyed()
    )
      .subscribe(() => {
        this.getConnections();
      });
  }

  private getConnections() {
    this.dbService.getConnections(true)
      .subscribe({
        next: (connections) => {
          const connLength = connections.connections.length
          const { healthyConnections, unhealthyConnections } = connections.connections.reduce((acc, connection) => {
            if (connection.connectionStatus.status === 'OK') {
              acc.healthyConnections.push(connection);
            } else {
              acc.unhealthyConnections.push(connection);
            }
            return acc;
          }, { healthyConnections: [], unhealthyConnections: [] });
          const healthDescription = `${healthyConnections.length} out of ${connLength} healthy`
          const definitionsWithErrorsDescription = connections.definitionsWithErrors.length ? `${connections.definitionsWithErrors.length} has definition error(s)` : null
          this.description.set(definitionsWithErrorsDescription || healthDescription)
          this.definitionsWithErrors.set(connections.definitionsWithErrors)
          this.unhealthyConnections.set(unhealthyConnections);
          this.healthyConnections.set(healthyConnections);
          this.connectionsLoading.set(false)
        },
    });
  }
}
