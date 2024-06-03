import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TuiDialogService, } from '@taiga-ui/core';
import { TUI_PROMPT, TuiBadgeModule, TuiPromptData, TuiStatus, TuiToggleModule } from '@taiga-ui/kit';
import { combineLatestWith, filter, Observable, of } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import { HeaderComponentLayoutModule } from '../header-component-layout/header-component-layout.module';
import { PipelineService, StreamRunningState, StreamStatus } from '../pipelines/pipelines.service';
import {
  PublishedEndpointInfoComponent
} from '../query-panel/query-editor/query-editor-toolbar/published-endpoint-info.component';
import { SavedQuery } from '../services/types.service';
import { TypesService } from '../services/types.service';
import { EndpointMonitorComponent } from './endpoint-monitor.component';

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
        <div *ngIf="query.queryKind === 'Stream'" class="row stream-status-and-toggle">
          <tui-toggle [ngModel]="streamIsRunning" (click)="handleToggleClick($event)" size="l"></tui-toggle>
          <tui-badge size="l" [value]="streamStatusBadge.label | titlecase"
                     [status]="streamStatusBadge.status"></tui-badge>
        </div>
      </ng-container>
      <app-endpoint-monitor
        [endpointName$]="endpointName$"
        [query$]="query$"
        [streamLoadingError]="streamLoadingError"
      ></app-endpoint-monitor>
    </app-header-component-layout>
  `,
  styleUrls: ['./endpoint-monitor-container.component.scss'],
  imports: [
    CommonModule,
    HeaderComponentLayoutModule,
    TuiToggleModule,
    TuiBadgeModule,
    EndpointMonitorComponent,
    FormsModule,
    PublishedEndpointInfoComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EndpointMonitorContainerComponent {

  query$: Observable<SavedQuery>
  endpointName$: Observable<string>
  streamLoadingError: string

  private streamStatus: StreamStatus

  get streamStatusBadge() {
    return {
      label: this.streamStatus?.state || 'Unknown',
      status: this.streamIsRunning ? 'success' : this.streamStatus?.state ? 'warning' : null as TuiStatus
    }
  }

  get streamIsRunning(): boolean {
    return this.streamStatus?.state === 'RUNNING';
  }

  constructor(
    private activatedRoute: ActivatedRoute,
    private typeService: TypesService,
    private changeDetector: ChangeDetectorRef,
    private pipelineService: PipelineService,
    @Inject(TuiDialogService) private readonly dialogs: TuiDialogService,
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
      })
    )

    this.endpointName$.pipe(
      combineLatestWith(this.query$),
      filter(([streamName, query]) => query.queryKind === 'Stream'),
      mergeMap(([streamName, query]) => pipelineService.getStreamStatus(streamName))
    ).subscribe({
      next: value => {
        this.streamStatus = value;
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
    const desiredState: StreamRunningState = (this.streamIsRunning) ? 'PAUSED' : 'RUNNING';
    this.updateStreamRunningState(desiredState);
    $event.preventDefault()
    $event.stopImmediatePropagation();
    $event.stopPropagation();
  }

  updateStreamRunningState(targetState: StreamRunningState) {
    let promptData: TuiPromptData;
    this.query$.pipe(
      mergeMap(savedQuery => {
        let dialogLabel: string;
        if (targetState === 'RUNNING') {
          promptData = {
            content: 'This will enable the data stream, allowing it to start processing any queued data.',
            yes: `Enable ${savedQuery.name.shortDisplayName}`,
            no: 'Cancel'
          }
          dialogLabel = `Enable ${savedQuery.name.shortDisplayName}?`
        } else {
          promptData = {
            content: 'This will disable the data stream, stopping all processing.<br /><br />Depending on how your data sources are configured, messages may be lost.',
            yes: `Disable ${savedQuery.name.shortDisplayName}`,
            no: 'Cancel'
          }
          dialogLabel = `Disable ${savedQuery.name.shortDisplayName}?`
        }
        return this.dialogs.open<boolean>(TUI_PROMPT, {
          label: dialogLabel,
          size: 's',
          data: promptData,
          closeable: false,
          dismissible: false,
        }).pipe(map(confirmed => {
          return { savedQuery, confirmed }
        }))
      }),
      mergeMap(({ savedQuery, confirmed }) => {
        if (confirmed) {
          return this.pipelineService.updateStreamStatus(savedQuery.name.parameterizedName, targetState)
        } else {
          return of(this.streamStatus)
        }
      })
    ).subscribe(next => {
      this.streamStatus = next;
      this.changeDetector.markForCheck();
    })
  }
}
