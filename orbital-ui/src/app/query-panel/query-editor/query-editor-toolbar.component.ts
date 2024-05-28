import {
  ChangeDetectionStrategy,
  Component,
  computed,
  EventEmitter, input,
  Input,
  Output,
  WritableSignal
} from '@angular/core';
import {RunningQueryStatus} from '../../services/active-queries-notification-service';
import {CopyQueryFormat} from 'src/app/query-panel/query-editor/QueryFormatter';
import {AppInfoService, AppConfig} from 'src/app/services/app-info.service';
import {isNullOrUndefined} from 'src/app/utils/utils';
import {Type} from '../../services/schema';
import {SavedQueryWithSource} from '../../project-import/schema-importer.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-query-editor-toolbar',
  template: `
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

    <tui-hosted-dropdown
      tuiDropdownAlign="left"
      [content]="queryLanguageDropdown"
      [(open)]="queryLanguageDropdownOpen"
    >
      <a
        tuiLink
        class="dropdown-link"
        [class.is-open]="queryLanguageDropdownOpen"
        tuiHint="Query language"
        tuiHintAppearance="onDark"
        tuiHintDirection="top"
      >
        {{queryLanguage}}
        <tui-svg
          src="tuiIconChevronDown"
          class="dropdown-arrow"
          [class.dropdown-arrow_open]="queryLanguageDropdownOpen"
        ></tui-svg>
      </a>
    </tui-hosted-dropdown>
    <ng-template
      #queryLanguageDropdown
      let-close="close"
    >
      <tui-data-list class="query-language-dropdown">
        @for (ql of queryLanguages; track ql) {
          <button tuiOption (click)="queryLanguageChange.emit(ql); close()">
            {{ql}}
            <tui-svg *ngIf="ql === queryLanguage" src="tuiIconCheck"></tui-svg>
          </button>
        }
      </tui-data-list>
    </ng-template>

    <tui-hosted-dropdown
      tuiDropdownAlign="left"
      [content]="copyMenuDropdown"
      [(open)]="copyMenuOpen"
    >
      <a
        tuiLink
        class="dropdown-link"
        [class.is-open]="copyMenuOpen"
        tuiHint="Copy..."
        tuiHintAppearance="onDark"
        tuiHintDirection="top"
      >
        <tui-svg src="tuiIconClipboard"></tui-svg>
        <tui-svg
          src="tuiIconChevronDown"
          class="dropdown-arrow"
          [class.dropdown-arrow_open]="copyMenuOpen"
        ></tui-svg>
      </a>
    </tui-hosted-dropdown>
    <ng-template
      #copyMenuDropdown
      let-close="close"
    >
      <tui-data-list>
        <button tuiOption (click)="copyQuery.emit('query')">Query only</button>
        <button tuiOption (click)="copyQuery.emit('curl')">As cURL statement</button>
        <button tuiOption (click)="copyQuery.emit('snippet')"
                tuiHint="Available once query has completed"
                tuiHintAppearance="onDark"
                [disabled]="!resultType"
        >
          As code
        </button>
      </tui-data-list>
    </ng-template>

    <tui-hosted-dropdown
      tuiDropdownAlign="left"
      [content]="publishMenuDropdown"
      [(open)]="publishMenuOpen"
      [canOpen]="publishAsHttpEndpointEnabled()"
      [tuiHint]="!publishAsHttpEndpointEnabled() ? 'You need to save a query before being able to publishing it' : null"
      tuiHintAppearance="onDark"
    >
      <a
        tuiLink
        class="dropdown-link"
        [class.is-open]="publishMenuOpen"
        [class.is-disabled]="!publishAsHttpEndpointEnabled()"
        tuiHint="Publish endpoint..."
        tuiHintAppearance="onDark"
        tuiHintDirection="top"
      >
        <img src="assets/img/tabler/broadcast.svg">
        <tui-svg
          src="tuiIconChevronDown"
          class="dropdown-arrow"
          [class.dropdown-arrow_open]="publishMenuOpen"
        ></tui-svg>
      </a>
    </tui-hosted-dropdown>
    <ng-template
      #publishMenuDropdown
      let-close="close"
    >
      <tui-data-list>
        @switch (savedQuery().savedQuery.queryKind) {
          @case ('Query') {
            <button tuiOption (click)="publishAsHttpEndpoint.emit()">As HTTP Endpoint</button>
          }
          @case ('Stream') {
            <button tuiOption (click)="publishAsHttpEndpoint.emit()">As Server Sent Events</button>
            <button tuiOption (click)="publishAsWebsocketpoint.emit()">As Websocket Endpoint</button>
          }
        }
      </tui-data-list>
    </ng-template>
    <a
      tuiLink
      class="button-link"
      tuiHint="Save query"
      tuiHintAppearance="onDark"
      tuiHintDirection="top"
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

    <div *ngIf="currentState() === 'Generating'">
      <button tuiButton size="s" appearance="outline"
              class='button-small menu-bar-button'
              [disabled]='true'
      >
        <span class='running-timer'>
          <span class='loader'></span>
          <span>Thinking...</span>
        </span>
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
        Cancel
      </button>
    </div>
  `,
  styleUrls: ['./query-editor-toolbar.component.scss']
})
export class QueryEditorToolbar {
  config: AppConfig;

  queryLanguageDropdownOpen = false;
  publishMenuOpen = false;
  copyMenuOpen = false;

  constructor(appInfo: AppInfoService) {
      appInfo.getConfig()
          .subscribe(config => this.config = config);
  }


  queryLanguages: QueryLanguage[] = ['TaxiQL', 'Text'];

  @Input()
  queryLanguage: QueryLanguage = 'TaxiQL';

  @Output()
  queryLanguageChange = new EventEmitter<QueryLanguage>();

  @Output()
  saveClicked = new EventEmitter();

  @Output()
  publishAsHttpEndpoint = new EventEmitter();

  @Output()
  publishAsWebsocketpoint = new EventEmitter();

  // NOTE: first usage of an input signal in the codebase!
  savedQuery = input<SavedQueryWithSource>();
  publishAsHttpEndpointEnabled = computed(() => !!this.savedQuery())

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

export type QueryLanguage = 'TaxiQL' | 'Text';
export type QueryState = 'Editing' | 'Generating' | 'Running' | 'Result' | 'Error' | 'Cancelling';
