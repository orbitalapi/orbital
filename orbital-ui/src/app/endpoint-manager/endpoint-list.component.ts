import {ChangeDetectionStrategy, ChangeDetectorRef, Component} from '@angular/core';
import {Observable} from 'rxjs';
import {TypesService} from "../services/types.service";
import {SavedQuery} from "../services/type-editor.service";
import {ActivatedRoute, Router} from "@angular/router";
import {
  PipelineService, StreamServerStatusEvent,
} from "../pipelines/pipelines.service";
import {filter, map, tap} from "rxjs/operators";
import {ConnectionStatus} from "../db-connection-editor/db-importer.service";
import {TuiStatus} from "@taiga-ui/kit/types";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

@Component({
  selector: 'app-endpoint-list',
  template: `
    <app-header-component-layout title="Query Endpoints"
                                 description="Queries, Streams and Pipelines defined in your schema">
      <ng-container ngProjectAs="header-components">
        <app-connection-status [status]="(streamServerConnectionStatus$ | async)"></app-connection-status>
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
              <div *ngIf="query.httpEndpoint" class="url-parts">
                <span class="method">{{ query.httpEndpoint.method }}</span>
                <span class="url">{{ query.httpEndpoint.url }}</span>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

    </app-header-component-layout>
  `,
  styleUrls: ['./endpoint-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EndpointListComponent {

  queries$: Observable<SavedQuery[]>;

  readonly streamServerConnectionStatus$: Observable<ConnectionStatus>
  private streamServerState: StreamServerStatusEvent = null;

  constructor(typeService: TypesService,
              private router: Router,
              private activeRoute: ActivatedRoute,
              private pipelineService: PipelineService,
              private changeDetector: ChangeDetectorRef) {
    this.queries$ = typeService.getQueries()
    const streamServerStatusMessages = pipelineService.streamsStatus()
      .pipe(
        takeUntilDestroyed()
      )
    this.streamServerConnectionStatus$ = streamServerStatusMessages.pipe(
      map(event => event.connectionStatus),
      map((event: ConnectionStatus) => {
        return {
          ...event,
          message: this.updateMessageText(event)
        } as ConnectionStatus
      })
    )

    streamServerStatusMessages.subscribe(next => {
      this.streamServerState = next.streamServerState;
      changeDetector.markForCheck();
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
    if (!this.streamServerState) return 'UNKNONW';
    const streamStatus = this.streamServerState.streams.find(s => s.streamName === query.name.parameterizedName)
    if (!streamStatus) return 'UNKNONW';
    return streamStatus.state;
  }

  private updateMessageText(event: ConnectionStatus): string {
    switch (event.status) {
      case "OK":
        return 'Stream server is connected and healthy'
      case "CONNECTING":
        return `Attempting to connect to stream server: ${event.message}`
      case "ERROR":
        return `Error with stream server: ${event.message}`
      case "UNKNOWN":
        return `Unknown state with stream server: ${event.message}`
    }
  }

  navigateToQueryPage(query: SavedQuery) {
    this.router.navigate([query.name.parameterizedName], {relativeTo: this.activeRoute})
  }
}
