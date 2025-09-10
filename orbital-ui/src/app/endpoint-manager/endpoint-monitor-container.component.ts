import {CommonModule} from '@angular/common';
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Injector} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {
  TuiAppearanceOptions,
  TuiButton,
  tuiButtonOptionsProvider,
  TuiDialogService,
  TuiNotification,
} from '@taiga-ui/core';
import {TuiConfirm, TuiStatus, TuiBadge, TuiSwitch, TuiConfirmData, TuiLineClamp} from '@taiga-ui/kit';
import {PolymorpheusComponent} from '@taiga-ui/polymorpheus';
import {combineLatestWith, filter, Observable, of} from 'rxjs';
import {map, mergeMap, tap} from 'rxjs/operators';
import {HeaderComponentLayoutModule} from '../header-component-layout/header-component-layout.module';
import {
  PipelineService,
  StreamRunningState,
  StreamStateWithJobStates,
  StreamStatus
} from '../pipelines/pipelines.service';
import {
  PublishedEndpointInfoComponent
} from '../query-panel/query-editor/query-editor-toolbar/published-endpoint-info.component';
import { SavedQuery, ScheduledQueryConfigWithSchedule } from "../services/types.service";
import {TypesService} from '../services/types.service';
import {EndpointMonitorComponent} from './endpoint-monitor.component';
import {QueryParseMetadata, QueryService} from "../services/query.service";
import {LineageGraphModule} from "../type-viewer/lineage-graph/lineage-graph.module";
import {LineageDisplayModule} from "../lineage-display/lineage-display.module";
import {RequiresAuthorityDirective} from "../requires-authority.directive";
import {jobStatusBadge, streamStateToDisplayLabel} from "./endpoint-list.component";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {SvgIconComponent} from "../svg-icon/svg-icon.component";
import { MomentModule } from "ngx-moment";

@Component({
  selector: 'app-endpoint-monitor-container',
  standalone: true,
  template: `
    <app-header-component-layout *ngIf="query$ | async as query"
                                 [title]="query?.name.name"
                                 [subtitle]="query.queryKind"
                                 backLink="/endpoints"
    >
      <ng-container ngProjectAs="header-components">
        <app-published-endpoint-info [savedQuery]="query" [showTitle]="false"></app-published-endpoint-info>
        <div *ngIf="query.queryKind === 'Stream'" class="stream-status-and-toggle">
          <input
            tuiSwitch
            type="checkbox" *appRequiresAuthority="['EditPipelines']" [ngModel]="streamIsEnabled"
            (click)="handleToggleClick($event)" size="m" />
          <tui-badge tuiStatus
                     [appearance]="streamStatusBadge.appearance">{{ streamStatusBadge.label | titlecase }}
          </tui-badge>
          <ng-container *ngIf="streamStatusBadge.label === 'Enabled'">
            <tui-badge tuiStatus
                       [appearance]="jobStatusBadge.appearance">{{ jobStatusBadge.label | titlecase }}
            </tui-badge>
            <button *ngIf="streamAndJobState?.jobState?.status !== 'RUNNING'" size="s" tuiButton type="button"
                    appearance="outline" (click)="restartStream()">
              <app-svg-icon tabler="refresh"></app-svg-icon>
              Restart
            </button>
          </ng-container>

        </div>
        <div *ngIf="query.schedule" class="row">
          <ng-container *ngFor="let trigger of (schedule$ | async)?.triggers">
            <div class="schedule">
              <app-svg-icon tabler="clock" [width]="24" [strokeWidth]="1.5"></app-svg-icon>
              <span class="label">Last trigger:</span>
              <span>{{ trigger.lastTriggerDate |  amCalendar:calendarFormats }}</span>
            </div>
            <div class="schedule">
              <app-svg-icon tabler="clock" [width]="24" [strokeWidth]="1.5"></app-svg-icon>
              <span class="label">Next trigger:</span>
              <span>{{ trigger.nextTriggerDate |  amCalendar:calendarFormats }}</span>
            </div>
          </ng-container>
        </div>
        <tui-notification
          *ngIf="streamAndJobState?.streamStatus?.state == 'RUNNING' && streamAndJobState?.jobState?.status !== 'RUNNING'"
          appearance="negative">
          <tui-line-clamp [lineHeight]="20" [linesLimit]="8" [content]="streamAndJobState?.jobState?.description">
          </tui-line-clamp>
        </tui-notification>

      </ng-container>
      <app-endpoint-monitor
        [endpointName$]="endpointName$"
        [queryPlan]="(parsedQuery$ | async)?.queryPlan"
        [query$]="query$"
        [streamLoadingError]="streamLoadingError"
      ></app-endpoint-monitor>
    </app-header-component-layout>
  `,
  styleUrls: ['./endpoint-monitor-container.component.scss'],
  imports: [
    CommonModule,
    HeaderComponentLayoutModule,
    TuiSwitch,
    TuiBadge,
    EndpointMonitorComponent,
    FormsModule,
    PublishedEndpointInfoComponent,
    LineageGraphModule,
    LineageDisplayModule,
    RequiresAuthorityDirective,
    TuiStatus,
    TuiNotification,
    TuiLineClamp,
    TuiButton,
    SvgIconComponent,
    MomentModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EndpointMonitorContainerComponent {
  calendarFormats = {
    sameDay: '[Today at] HH:mm:ss',
    lastDay: '[Yesterday at] HH:mm:ss',
    lastWeek: '[Last] dddd [at] HH:mm:ss',
    nextDay: '[Tomorrow at] HH:mm:ss',
    nextWeek: 'dddd [at] HH:mm:ss',
    sameElse: 'DD/MM/YYYY HH:mm:ss'
  };
  query$: Observable<SavedQuery>
  endpointName$: Observable<string>
  streamLoadingError: string
  parsedQuery$: Observable<QueryParseMetadata>

  streamAndJobState: StreamStateWithJobStates
  schedule$: Observable<ScheduledQueryConfigWithSchedule>;

  get streamStatusBadge(): { label: string, appearance: TuiAppearanceOptions["appearance"] } {
    let appearance: string
    switch (true) {
      case this.streamIsEnabled:
        appearance = 'positive';
        break;
      default:
        appearance = 'warning';
        break;
    }
    return {
      label: streamStateToDisplayLabel(this.streamAndJobState?.streamStatus?.state),
      appearance: appearance
    }
  }

  get jobStatusBadge(): { label: string, appearance: TuiAppearanceOptions["appearance"] } {
    return jobStatusBadge(this.streamAndJobState)

  }

  get streamIsEnabled(): boolean {
    return this.streamAndJobState?.streamStatus?.state === 'RUNNING';
  }

  constructor(
    private activatedRoute: ActivatedRoute,
    private typeService: TypesService,
    private changeDetector: ChangeDetectorRef,
    private pipelineService: PipelineService,
    private queryService: QueryService,
    @Inject(TuiDialogService) private readonly dialogs: TuiDialogService,
    @Inject(Injector) private readonly injector: Injector,
  ) {
    this.endpointName$ = activatedRoute.paramMap.pipe(
      map(paramMap => {
        const endpoint = paramMap.get('endpointName');
        return endpoint

      }),
    )
    this.query$ = this.endpointName$.pipe(
      mergeMap(endpoint => {
        return typeService.getQuery(endpoint)
          .pipe(
            tap(savedQuery => {
              this.parsedQuery$ = queryService.compileQuery(savedQuery.sources[0].content)
              if (savedQuery.schedule) {
                this.schedule$ = typeService.getQuerySchedule(endpoint)
              }
            })
          )
      })
    )

    this.endpointName$.pipe(
      takeUntilDestroyed(),
      combineLatestWith(this.query$),
      filter(([streamName, query]) => query.queryKind === 'Stream'),
      mergeMap(([streamName, query]) => {
        return pipelineService.streamsStatus()
          .pipe(
            map(event => {
                return event[streamName]
              }
            ))
      })
    ).subscribe({
      next: value => {
        console.log('Updating stream status: ', value)
        this.streamAndJobState = value;
        this.changeDetector.markForCheck();
      },
      error: err => {
        console.log(err)
        this.streamLoadingError = `Failed to load data stream details: ${err.error.message}`;
        this.changeDetector.markForCheck();
      }
    })
  }

  getIconUrl(queryKind: 'Stream' | 'Query') {
    switch (queryKind) {
      case 'Query':
        return 'assets/img/tabler/arrows-right-left.svg';
      case 'Stream':
        return 'assets/img/tabler/arrows-right.svg';
    }
  }

  handleToggleClick($event: Event) {
    const desiredState: StreamRunningState = (this.streamIsEnabled) ? 'PAUSED' : 'RUNNING';
    this.updateStreamRunningState(desiredState);
    $event.preventDefault()
    $event.stopImmediatePropagation();
    $event.stopPropagation();
  }

  updateStreamRunningState(targetState: StreamRunningState) {
    let promptData: TuiConfirmData;
    this.query$.pipe(
      mergeMap(savedQuery => {
        let dialogLabel: string;
        if (targetState === 'RUNNING') {
          promptData = {
            content: 'This will enable the data stream, allowing it to start processing any queued data.',
            yes: `Enable stream`,
            no: 'Cancel'
          }
          dialogLabel = `Enable ${savedQuery.name.shortDisplayName}`
        } else {
          promptData = {
            content: 'This will disable the data stream, stopping all processing.<br /><br />Depending on how your data sources are configured, messages may be lost.',
            yes: `Disable stream`,
            no: 'Cancel'
          }
          dialogLabel = `Disable ${savedQuery.name.shortDisplayName}`
        }
        return this.dialogs.open<boolean>(new PolymorpheusComponent(
          TuiConfirm,
          Injector.create({
            providers: [tuiButtonOptionsProvider({appearance: targetState === 'RUNNING' ? 'primary' : 'destructive'})],
            parent: this.injector,
          })
        ), {
          label: dialogLabel,
          size: 's',
          data: promptData,
          closeable: true,
          dismissible: true,
        }).pipe(map(confirmed => {
          return {savedQuery, confirmed}
        }))
      }),
      mergeMap(({savedQuery, confirmed}) => {
        if (confirmed) {
          console.log('Updating stream status: ', this.streamAndJobState)
          this.changeDetector.markForCheck();
          return this.pipelineService.updateStreamStatus(savedQuery.name.parameterizedName, targetState)
        } else {
          return of(this.streamAndJobState)
        }
      })
    ).subscribe(next => {
      // Don't update the stream status.
      // We'll get an event on the websocket
      // Otherwise, a race condition exists if the stream has already failed by the time the
      // HTTP call returns.
      // this.streamStatus = next;
      // this.changeDetector.markForCheck();
    })
  }

  restartStream() {
    this.pipelineService.restartStream(this.streamAndJobState.streamStatus.streamName)
      .subscribe(next => {
          console.log('Restart of pipeline sent successfully')
      })
  }
}

