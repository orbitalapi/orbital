import {ChangeDetectionStrategy, Component, signal} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import {TuiButtonModule, TuiNotificationModule} from '@taiga-ui/core';
import { TuiProgressModule } from '@taiga-ui/kit';
import { tap } from 'rxjs/operators';
import { ConnectionStatusComponent } from '../../data-source-manager/connection-status/connection-status.component';
import {ConnectorSummary, DbConnectionService, PackageWithError} from '../../db-connection-editor/db-importer.service';
import { CardComponent } from '../card/card.component';
import {StatisticModule} from "../../statistic/statistic.module";
import {BehaviorSubject} from "rxjs";

@Component({
  selector: 'app-data-sources-card',
  standalone: true,
  imports: [
    CommonModule, CardComponent, TuiProgressModule, TuiButtonModule, RouterLink, ConnectionStatusComponent,
    TuiNotificationModule,
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
  ) {
    this.dbService.getConnections(true)
      .pipe(
        tap(connections => {
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
        }),
        takeUntilDestroyed()
      ).subscribe();
  }
}
