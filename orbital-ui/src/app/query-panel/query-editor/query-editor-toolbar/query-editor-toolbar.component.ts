import {
  ChangeDetectionStrategy,
  Component,
  computed,
  EventEmitter,
  input,
  Input,
  Output,
  WritableSignal
} from '@angular/core';
import {RunningQueryStatus} from '../../../services/active-queries-notification-service';
import {CopyQueryFormat} from 'src/app/query-panel/query-editor/QueryFormatter';
import {AppInfoService, AppConfig} from 'src/app/services/app-info.service';
import {isNullOrUndefined} from 'src/app/utils/utils';
import {Type} from '../../../services/schema';
import {SavedQueryWithSource} from '../../../project-import/schema-importer.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-query-editor-toolbar',
  template: `
    <app-published-endpoint-info
      [savedQuery]="savedQueryWithSource()?.savedQuery"
      [showTitle]="true"
    ></app-published-endpoint-info>
    <div *ngIf="currentState() === 'Running'">
      <span class='running-timer has-separator'>
        <span class='loader'></span>
        <span>Running...&nbsp;</span>
        <app-counter-timer *ngIf='queryStarted' [startDate]='queryStarted'></app-counter-timer>
        <ng-container *ngIf="queryStarted && percentComplete > 0">
          <span
            *ngIf="runningQueryStatus.queryMode !== 'STREAM' && runningQueryStatus.estimatedProjectionCount !== 0"
            class="record-count"
          >
            ({{ runningQueryStatus.completedProjections | number }}
            of {{ runningQueryStatus.estimatedProjectionCount | number }} records)
          </span>
          <span
            *ngIf="runningQueryStatus.queryMode === 'STREAM' || runningQueryStatus.queryMode === 'FIND_ALL'"
            class="record-count"
          >
            ({{ runningQueryStatus.completedProjections | number }} records)
          </span>
        </ng-container>
      </span>
    </div>

    <button *ngIf="config.featureToggles.chatGptEnabled"
            tuiButton size="s" appearance="outline"
            class='button-small menu-bar-button toggleable'
            [class.is-toggled]="isCopilotOpen"
            (click)='showCopilotPanel.emit()'
    >
      <img src="assets/img/tabler/wand.svg">
      Copilot
    </button>

    <app-dropdown iconUrl="assets/img/tabler/clipboard.svg" hint="Copy query...">
      <tui-data-list>
        <button tuiOption (click)="copyQuery.emit('query')">Query only</button>
        <button tuiOption (click)="copyQuery.emit('curl')">As cURL statement</button>
        <button tuiOption (click)="copyQuery.emit('snippet')"
                tuiHint="Available once query has completed"
                tuiHintAppearance="onDark"
                [disabled]="!resultType"
        >
          As code...
        </button>
      </tui-data-list>
    </app-dropdown>

    <app-dropdown
      iconUrl="assets/img/tabler/broadcast.svg"
      hint="Publish endpoint..."
      [isDisabled]="!publishEndpointEnabled()"
      hintWhenDisabled="You need to save a query before being able to publish it"
    >
      <tui-data-list>
        @switch (savedQueryWithSource()?.savedQuery.queryKind) {
          @case ('Query') {
            <button tuiOption (click)="publishAsHttpEndpoint.emit()">
              {{ savedQueryWithSource().savedQuery.httpEndpoint ? 'Remove' : 'Publish as' }} HTTP Endpoint...
            </button>
          }
          @case ('Stream') {
            <button tuiOption (click)="publishAsHttpEndpoint.emit()">
              {{ savedQueryWithSource().savedQuery.httpEndpoint ? 'Remove' : 'Publish as' }} Server Sent Event...
            </button>
            <button tuiOption (click)="publishAsWebsocketpoint.emit()">
              {{ savedQueryWithSource().savedQuery.websocketOperation ? 'Remove' : 'Publish as' }} Websocket Endpoint...
            </button>
          }
        }
      </tui-data-list>
    </app-dropdown>

    <a
      tuiLink
      class="button-link"
      tuiHint="Save query to project"
      tuiHintAppearance="onDark"
      (click)="saveClicked.emit()"
    >
      <img src="assets/img/tabler/device-floppy.svg">
    </a>

    <button tuiButton size="s" appearance="primary"
            class='button-small menu-bar-button'
            *ngIf="currentState() !== 'Running' && currentState() !== 'Cancelling'"
            (click)='runQuery()'>
      <img src="assets/img/tabler/player-play.svg" class='filter-white'>
      Run
    </button>

    <div *ngIf="currentState() === 'Running'">
      <button tuiButton size="s" appearance="outline"
              class='button-small menu-bar-button'
              (click)='cancelQuery.emit()'
      >
        <img src="assets/img/tabler/player-stop.svg">
        Cancel
      </button>
    </div>

    <div *ngIf="currentState() === 'Cancelling'">
      <button tuiButton size="s" appearance="outline"
              class='button-small menu-bar-button'
              [disabled]='true'
      >
        <span class='running-timer'>
          <span class='loader'></span>
          <span>Cancelling...</span>
        </span>
      </button>
    </div>
  `,
  styleUrls: ['./query-editor-toolbar.component.scss']
})
export class QueryEditorToolbar {
  config: AppConfig;

  constructor(appInfo: AppInfoService) {
      appInfo.getConfig()
          .subscribe(config => this.config = config);
  }

  @Output()
  showCopilotPanel = new EventEmitter<void>();

  @Output()
  saveClicked = new EventEmitter();

  @Output()
  publishAsHttpEndpoint = new EventEmitter();

  @Output()
  publishAsWebsocketpoint = new EventEmitter();

  // NOTE: first usage of an input signal in the codebase!
  savedQueryWithSource = input<SavedQueryWithSource>();
  publishEndpointEnabled = computed(() => !!this.savedQueryWithSource())

  @Input()
  currentState: WritableSignal<QueryState>;

  @Output()
  copyQuery = new EventEmitter<CopyQueryFormat>();

  @Output()
  executeQuery = new EventEmitter<void>();

  @Input()
  queryStarted: Date;

  @Output()
  cancelQuery = new EventEmitter();

  @Input()
  runningQueryStatus: RunningQueryStatus | null;

  @Input()
  resultType: Type | null

  @Input()
  isCopilotOpen: boolean;

  get percentComplete(): number | null {
      if (!this.runningQueryStatus) {
          return null;
      }
      if (isNullOrUndefined(this.runningQueryStatus.estimatedProjectionCount)) {
          return null;
      }
      return (this.runningQueryStatus.completedProjections / this.runningQueryStatus.estimatedProjectionCount) * 100;
  }

  runQuery() {
      this.executeQuery.emit();
  }
}

export type QueryState = 'Editing' | 'Generating' | 'Generated' | 'Running' | 'Result' | 'Error' | 'Cancelling';
