import {TuiBadge, TuiStatus} from '@taiga-ui/kit';
import {AsyncPipe, CommonModule, TitleCasePipe} from '@angular/common';
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject} from '@angular/core';
import {TuiAlertService, TuiAppearanceOptions, TuiNotification} from '@taiga-ui/core';
import {Observable, switchMap} from 'rxjs';
import {HeaderComponentLayoutComponent} from '../header-component-layout/header-component-layout.component';
import {SchemaNotificationService} from '../services/schema-notification.service';
import {SavedQuery, TypesService} from '../services/types.service';
import {ActivatedRoute, Router} from "@angular/router";
import {PipelineService, StreamRunningState, StreamStateWithJobStates} from "../pipelines/pipelines.service";
import {map, tap} from 'rxjs/operators';
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

@Component({
  selector: 'app-endpoint-list',
  standalone: true,
  template: `
    <app-header-component-layout title="Query Endpoints"
                                 description="Queries and streams defined in your schema">
      <ng-container ngProjectAs="header-components">
        <tui-notification size="m" *ngIf="websocketConnectionError && hasStreamingQueries"
                          appearance="error">{{ websocketConnectionError }}
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
            <td class="badges">
              <tui-badge tuiStatus [appearance]="queryStateBadgeType(queryState(query))"
                         size="m">{{ queryState(query) | titlecase }}
              </tui-badge>
              <tui-badge  *ngIf="query.queryKind === 'Stream'"  tuiStatus [appearance]="jobState(query).appearance"
                         size="m">{{ jobState(query).label | titlecase }}
              </tui-badge>
            </td>
            <td>{{ queryKindLabel(query) }}</td>
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
    HeaderComponentLayoutComponent,
    AsyncPipe,
    TuiNotification,
    TitleCasePipe,
    TuiBadge,
    TuiStatus,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EndpointListComponent {

  queries$: Observable<SavedQuery[]>;

  hasStreamingQueries = false;
  private streamServerState: { [index: string]: StreamStateWithJobStates } = {};
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
              savedQuery.publications && savedQuery.publications.length > 0
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
                {appearance: 'warning', autoClose: 0, closeable: false}
              )
              .subscribe()
            changeDetector.markForCheck();
          }
        });
  }

  queryStateBadgeType(state:StreamStateDisplayLabel): TuiAppearanceOptions["appearance"] {
    switch (state) {
      case "Enabled":
        return "positive";
      case "Disabled":
        return 'warning'
      default:
        return 'neutral'
    }
  }

  jobState(query: SavedQuery):{ label: string, appearance: TuiAppearanceOptions["appearance"] } {
    const unknown = {
      label: 'Unknown',
      appearance: 'neutral',
    };
    if (!this.streamServerState) return unknown;
    const streamStatus: StreamStateWithJobStates = this.streamServerState[query.name.parameterizedName]
    if (!streamStatus) return unknown;
    return jobStatusBadge(streamStatus)
  }


  queryKindLabel(query: SavedQuery): string {
    if (query.publications.some(p => p.kind === 'Scheduled')) {
      return 'Scheduled query'
    } else {
      return query.queryKind
    }
  }

  queryState(query: SavedQuery):StreamStateDisplayLabel {
    if (query.queryKind === "Query") return "Enabled"; // Can't suspend queries at the moment
    if (!this.streamServerState) return 'Unknown';
    const streamStatus: StreamStateWithJobStates = this.streamServerState[query.name.parameterizedName]
    if (!streamStatus) return 'Unknown';
    return streamStateToDisplayLabel(streamStatus.streamStatus.state)
  }

  navigateToQueryPage(query: SavedQuery) {
    this.router.navigate([query.name.parameterizedName], {relativeTo: this.activeRoute})
  }
}

export type StreamStateDisplayLabel = 'Enabled' | 'Disabled' | 'Unknown';

export function streamStateToDisplayLabel(streamState: StreamRunningState): StreamStateDisplayLabel {
  switch (streamState) {
    case "RUNNING":
      return 'Enabled';
    case "PAUSED":
      return 'Disabled';
    default:
      return 'Unknown';
  }
}




export function jobStatusBadge(streamAndJobState: StreamStateWithJobStates):{ label: string, appearance: TuiAppearanceOptions["appearance"] } {
  let appearance: TuiAppearanceOptions["appearance"];
  let label: string;
  const streamEnabled = streamAndJobState.streamStatus.state === 'RUNNING';
  switch (streamAndJobState?.jobState?.status) {
    case "RUNNING":
      label = 'Running';
      if (streamEnabled) {
        appearance = 'positive';
      } else {
        appearance = 'warning';
      }
      break;
    case 'SUSPENDED':
    case "FAILED":
      if (streamEnabled) {
        appearance = 'negative';
        label = 'Error'
      } else {
        appearance = 'positive';
        label = 'Not running'
      }
      break;
    default:
      label = streamAndJobState?.jobState?.status || 'UNKNOWN';
      appearance = 'warning';
  }
  return {
    label: label,
    appearance: appearance
  }
}
