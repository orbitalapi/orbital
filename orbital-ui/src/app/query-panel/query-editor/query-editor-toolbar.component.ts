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
import { isNullOrUndefined } from 'src/app/utils/utils';
import { Type } from '../../services/schema';
import { SavedQueryWithSource } from '../../project-import/schema-importer.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-query-editor-toolbar',
  template: `
    <tui-select
      *ngIf='config?.featureToggles?.chatGptEnabled'
      tuiTextfieldSize='s'
      [(ngModel)]='queryLanguage'
      (ngModelChange)='queryLanguageChange.emit($event)'
    >
      Query language
      <input
        tuiTextfield
        placeholder='Query lanaguage'
      />
      <tui-data-list-wrapper
        *tuiDataList
        [items]='queryLanguages'
      ></tui-data-list-wrapper>
    </tui-select>
    <tui-hosted-dropdown
      tuiDropdownAlign="left"
      [content]="copyMenuDropdown"
      [(open)]="copyMenuOpen">
      <button
        appearance="outline"
        iconRight="tuiIconChevronDown"
        class="button-small menu-bar-button"
        size="s"
        tuiButton
        type="button"
        [pseudoActive]="copyMenuOpen || null"> Copy
      </button>
    </tui-hosted-dropdown>
    <ng-template
      #copyMenuDropdown
      let-close="close"
    >
      <tui-data-list>
        <button tuiOption (click)="copyQuery.emit('query')">Query only</button>
        <button tuiOption (click)="copyQuery.emit('curl')">As cURL statement</button>
        <button tuiOption (click)="copyQuery.emit('snippet')" tuiHint='Available once query has completed'
                [disabled]="!resultType">
          As code
        </button>
      </tui-data-list>
    </ng-template>
    <tui-hosted-dropdown
      tuiDropdownAlign="left"
      [content]="publishMenuDropdown"
      [(open)]="publishMenuOpen"
    >
      <button
        appearance="outline"
        iconRight="tuiIconChevronDown"
        class="button-small menu-bar-button"
        size="s"
        tuiButton
        type="button"
        [pseudoActive]="publishMenuOpen || null"
        [disabled]="!publishAsHttpEndpointEnabled()"
        [tuiHint]="!publishAsHttpEndpointEnabled() ? 'You need to save a query before being able to publishing it' : null"
      >
        Publish
      </button>
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
    <button tuiButton size="s" appearance="outline" class="button-small  menu-bar-button"
            (click)="saveClicked.emit()">Save
    </button>
    <button tuiButton size="s" appearance="primary"
            class='button-small menu-bar-button'
            *ngIf="currentState() !== 'Running' && currentState() !== 'Cancelling'"
            (click)='runQuery()'>
      <img src='assets/img/tabler/player-play.svg' class='filter-white'>
      Run
    </button>

    <div *ngIf="currentState() === 'Running'">
      <button tuiButton size="s" appearance="outline"
              class='button-small menu-bar-button'
              (click)='cancelQuery.emit()'
      >
        <span class='running-timer'>
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
        Cancel
      </button>
    </div>

    <div *ngIf="currentState() === 'Generating'">
      <button tuiButton size="s" appearance="outline"
              class='button-small menu-bar-button'
              [disabled]='true'
      >
        <span class='running-timer no-separator'>
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

  publishMenuOpen = false;
  copyMenuOpen = false;

  onActiveZone(active: boolean): void {
      this.publishMenuOpen = active && this.publishMenuOpen;
  }

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
