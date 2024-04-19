import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TuiButtonModule } from '@taiga-ui/core';
import { TuiProgressModule } from '@taiga-ui/kit';
import { BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { ConnectionStatusComponent } from '../../data-source-manager/connection-status/connection-status.component';
import { ConnectorSummary, DbConnectionService } from '../../db-connection-editor/db-importer.service';
import { CardComponent } from '../card/card.component';

@Component({
  selector: 'app-data-sources-card',
  standalone: true,
  imports: [CommonModule, CardComponent, TuiProgressModule, TuiButtonModule, RouterLink, ConnectionStatusComponent],
  templateUrl: './data-sources-card.component.html',
  styleUrls: ['./data-sources-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DataSourcesCardComponent {
  description$: BehaviorSubject<string> = new BehaviorSubject<string>("Loading...");
  unhealthyConnections$: BehaviorSubject<ConnectorSummary[]> = new BehaviorSubject([])
  percentHealthy$: BehaviorSubject<number> = new BehaviorSubject<number>(undefined);

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
          this.description$.next(`${healthyConnections.length} out of ${connLength} healthy`);
          this.unhealthyConnections$.next(unhealthyConnections);
          this.percentHealthy$.next((healthyConnections.length/connLength)*100);
        }),
        takeUntilDestroyed()
      ).subscribe();
  }
}
