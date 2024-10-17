import { AsyncPipe, CommonModule, TitleCasePipe } from '@angular/common';
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject} from '@angular/core';
import {TuiAlertService, TuiNotificationModule} from '@taiga-ui/core';
import { TuiBadgeModule } from '@taiga-ui/kit';
import {Observable, switchMap} from 'rxjs';
import { ConnectionStatusComponent } from '../data-source-manager/connection-status/connection-status.component';
import { HeaderComponentLayoutModule } from '../header-component-layout/header-component-layout.module';
import {SchemaNotificationService} from '../services/schema-notification.service';
import { SavedQuery, TypesService } from '../services/types.service';
import {ActivatedRoute, Router} from "@angular/router";
import {
  PipelineService, StreamServerStatusEvent,
} from "../pipelines/pipelines.service";
import {map, tap} from 'rxjs/operators';
import {TuiStatus} from "@taiga-ui/kit/types";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

@Component({
  selector: 'app-endpoint-list',
  standalone: true,
  template: `
    <app-header-component-layout title="Query Endpoints"
                                 description="Queries and streams defined in your schema">
      <ng-container ngProjectAs="header-components">
        <tui-notification *ngIf="websocketConnectionError && hasStreamingQueries"
                          status="error">{{ websocketConnectionError }}
        </tui-notification>
      </ng-container>
      <div *ngIf="queries$ | async as queries">
        <table class="query-list">
          <thead>
          <tr>
            <th>Name</th>
            <th>State</th>
            <th>Type</th>
            <th>URL</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let query of queries" (click)="navigateToQueryPage(query)">
            <td>{{ query.name.shortDisplayName }}</td>
            <td>
              <tui-badge [value]="queryState(query) | titlecase" [status]="queryStateBadgeType(queryState(query))"
                         size="s"></tui-badge>
            </td>
            <td>{{ query.queryKind }}</td>
            <td>
              <span *ngIf="query.httpEndpoint" class="url-parts">
                <span class="mono-badge method">{{ query.httpEndpoint.method }}</span>
                <span class="url">{{ query.httpEndpoint.url }}</span>
              </span>
              <span *ngIf="query.websocketOperation" class="url-parts">
                <span class="mono-badge method">WS</span>
                <span class="url">{{ query.websocketOperation.path }}</span>
              </span>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </app-header-component-layout>
  `,
  styleUrls: ['./endpoint-list.component.scss'],
  imports: [
    CommonModule,
    HeaderComponentLayoutModule,
    ConnectionStatusComponent,
    AsyncPipe,
    TuiNotificationModule,
    TitleCasePipe,
    TuiBadgeModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EndpointListComponent {

  queries$: Observable<SavedQuery[]>;

  hasStreamingQueries = false;
  private streamServerState: StreamServerStatusEvent = null;
  websocketConnectionError: string | null = null;

  constructor(
    private typeService: TypesService,
    private router: Router,
    private activeRoute: ActivatedRoute,
    private pipelineService: PipelineService,
    private schemaNotificationService: SchemaNotificationService,
    private changeDetector: ChangeDetectorRef,
    @Inject(TuiAlertService) private readonly alertService: TuiAlertService,
  ) {
    this.queries$ = this.schemaNotificationService.createSchemaNotificationsSubscription()
      .pipe(
        switchMap(() => this.typeService.getQueries()
          .pipe(
            map(savedQueries => savedQueries.filter(savedQuery =>
              savedQuery.httpEndpoint ||
              savedQuery.websocketOperation ||
              savedQuery.queryKind === 'Stream'
            )),
            tap(savedQueries => {
              this.hasStreamingQueries = savedQueries.some(query => query.queryKind === 'Stream');
              this.changeDetector.markForCheck();
            })
          )
        )
      );
    const streamServerStatusMessages = pipelineService.streamsStatus()
    streamServerStatusMessages
      .pipe(takeUntilDestroyed())
      .subscribe(
      {
        next: message => {
          this.streamServerState = message
          this.websocketConnectionError = null;
          changeDetector.markForCheck();
        },
        error: err => {
          console.log(err)
          this.websocketConnectionError = 'Unable to fetch stream statuses'
          this.alertService
            .open('Server disconnected, please refresh the browser to reconnect',
              {status: 'warning', autoClose: false, hasIcon: true, hasCloseButton: false }
            )
            .subscribe()
          changeDetector.markForCheck();
        }
      });
  }

  queryStateBadgeType(state: 'RUNNING' | 'PAUSED' | 'UNKNOWN'): TuiStatus {
    switch (state) {
      case "UNKNOWN":
        return "neutral";
      case "PAUSED":
        return "warning";
      case "RUNNING":
        return "success";
    }
  }

  queryState(query: SavedQuery) {
    if (query.queryKind === "Query") return "RUNNING"; // Can't suspend queries at the moment
    if (!this.streamServerState) return 'UNKNOWN';
    const streamStatus = this.streamServerState.streams.find(s => s.streamName === query.name.parameterizedName)
    if (!streamStatus) return 'UNKNOWN';
    return streamStatus.state;
  }

  navigateToQueryPage(query: SavedQuery) {
    this.router.navigate([query.name.parameterizedName], {relativeTo: this.activeRoute})
  }
}
