import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { tuiIconPause, tuiIconPlay } from '@taiga-ui/icons';
import { BehaviorSubject, EMPTY, filter, Observable, of, Subject } from 'rxjs';
import { DisplayMode, DownloadClickedEvent } from '../object-view/object-view-container.component';
import { InstanceLike, Type } from '../services/schema';
import {QueryProfileData, StreamQueryErrorEvent} from '../services/query.service';
import { BaseQueryResultComponent } from '../query-panel/result-display/BaseQueryResultComponent';
import { TypesService } from '../services/types.service';
import { AppInfoService, AppConfig } from '../services/app-info.service';
import { ConfigDisabledFormComponent } from '../test-pack-module/config-disabled-form.component';
import {
  ConfigPersistResultsDisabledFormComponent
} from '../test-pack-module/config-persist-results-disabled-form.component';
import { MatDialog } from '@angular/material/dialog';
import { isNullOrUndefined } from 'src/app/utils/utils';
import { ExportFormat } from 'src/app/results-download/results-download.service';
import { map, scan, tap } from 'rxjs/operators';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-tabbed-results-view',
  template: `
    <!--    <app-error-panel *ngIf="lastQueryResultAsSuccess?.unmatchedNodes?.length > 0"-->
    <!--                     [queryResult]="lastQueryResultAsSuccess">-->
    <!--    </app-error-panel>-->
    <progress
      max="100"
      tuiProgressBar
      size='xs'
      new
      *ngIf='isQueryRunning'
    ></progress>
    <ng-container *ngIf="{obs: responseIsLarge$ | async} as responseIsLarge">
      <app-panel-header class="panel-header" title="Results" [isSecondary]="true">
        <tui-tabs-with-more *ngIf="showResultsPanel"
                            [(activeItemIndex)]="activeTabIndex"
                            (activeItemIndexChange)="onTabIndexChanged($event)"
                            [moreContent]='more'
        >
          <button *tuiItem tuiTab [disabled]="responseIsLarge.obs">
            <img src="assets/img/tabler/table.svg" class="tab-icon">
            Table
          </button>
          <button *tuiItem tuiTab [disabled]="responseIsLarge.obs">
            <img src="assets/img/tree-list.svg" class="tab-icon">
            Tree
          </button>
          <button *tuiItem tuiTab>
            <img src="assets/img/tabler/code-dots.svg" class="tab-icon">
            Raw
          </button>
          <ng-container *ngIf='profilerEnabled'>
            <button *tuiItem tuiTab>
              <img src="assets/img/tabler/gauge.svg" class="tab-icon">
              Profiler
            </button>
          </ng-container>
          <button *tuiItem tuiTab>
            <img src="assets/img/tabler/exclamation-circle.svg" class="tab-icon">
            Problems
            <tui-badge class="error-count-badge" *ngIf="errorCount > 0" [value]="errorCount" size="xs"></tui-badge>
          </button>
        </tui-tabs-with-more>
        <ng-template #more>
          <tui-svg src="tuiIconMoreHorizontalLarge"></tui-svg>
        </ng-template>
        <div class="rightside-controls-container">
          <button
            *ngIf="isStreamingQuery && isQueryRunning"
            tuiButton type="button" appearance="outline" size="s"
            [icon]="isQueryPaused ? tuiIconPlay : tuiIconPause"
            [tuiHint]="isQueryPaused ? 'Resume the stream on the UI' : 'Pause the stream on the UI'"
            tuiHintAppearance="onDark"
            (click)="pauseStreamToggled.emit(!isQueryPaused)"
            class="button-small menu-bar-button pause-stream-button"
            [class.is-query-paused]="isQueryPaused"
          >
            {{isQueryPaused ? 'Resume stream' : 'Pause stream'}}
          </button>
          <tui-hosted-dropdown
            *ngIf="showResultsPanel && downloadSupported"
            tuiDropdownAlign="left"
            [content]="downloadDropdown"
            [(open)]="downloadMenuOpen"
          >
            <button tuiButton type="button" appearance="outline" [iconRight]="downloadIcon" size="s"
                    class="button-small menu-bar-button">
              Download
            </button>
          </tui-hosted-dropdown>
        </div>
      </app-panel-header>
      <app-object-view-container
        *ngIf="activeTabIndex < 3 && showResultsPanel"
        [instances$]="_instances$"
        [schema]="schema"
        [displayMode]="displayMode"
        [selectable]="true"
        [downloadSupported]="downloadSupported"
        [type]="type"
        [anonymousTypes]="anonymousTypes"
        [isResponseLarge]="responseIsLarge.obs"
        [isStreamingQuery]="isStreamingQuery"
        (downloadClicked)="this.downloadClicked.emit($event)"
        (instanceClicked)="instanceClicked($event,type.name)"
      ></app-object-view-container>
    </ng-container>
    <app-call-explorer *ngIf="activeTabIndex === 3 && profileData$ && showResultsPanel && !isQueryRunning"
                       [queryProfileData$]="profileData$"
    ></app-call-explorer>
    <app-query-errors-list
      *ngIf="activeTabIndex == 4" [errorMessages$]="errorMessages$"></app-query-errors-list>
    <ng-template #downloadIcon>
      <tui-svg
        src="tuiIconChevronDown"
        class="icon"
        [class.icon_rotated]="downloadMenuOpen"
      ></tui-svg>
    </ng-template>
    <ng-template #downloadDropdown>
      <tui-data-list>
        <button tuiOption (click)="onDownloadClicked(downloadFileType.JSON)"
                [disabled]="!config?.analytics.persistResults">as JSON
          <a *ngIf="!config?.analytics.persistResults"
             href="#"
             (click)="showDisabledPersistResultsConfig($event)">Why is this disabled?</a>
        </button>
        <button tuiOption (click)="onDownloadClicked(downloadFileType.CSV)">as CSV</button>
        <button tuiOption (click)="onDownloadClicked(downloadFileType.TEST_CASE)"
                [disabled]="!config?.analytics.persistRemoteCallResponses || !config?.analytics.persistResults">as
          Test Case
          <a *ngIf="!config?.analytics.persistRemoteCallResponses || !config?.analytics.persistResults"
             href="#"
             (click)="showDisabledTestCaseConfig($event)">Why is this disabled?</a>
        </button>
        <button tuiOption
                (click)="onDownloadClicked(downloadFileType.CUSTOM_FORMAT)"
                [disabled]="!(hasModelFormatSpecs | async) || !config?.analytics.persistResults">Using the defined
          format
        </button>
      </tui-data-list>
    </ng-template>
  `,
  styleUrls: ['./tabbed-results-view.component.scss']
})
export class TabbedResultsViewComponent extends BaseQueryResultComponent {
  config: AppConfig;
// workaround for lack of enum support in templates
  downloadFileType = ExportFormat;

  private _isQueryRunning: boolean

  get isQueryRunning(): boolean {
    return this._isQueryRunning;
  }

  @Input()
  set isQueryRunning(value: boolean) {
    this._isQueryRunning = value;
    this.isQueryPaused = false;
    if (value && this.activeTabIndex === 3) this.activeTabIndex = 0;
  }

  constructor(protected typeService: TypesService,
              protected appInfoService: AppInfoService,
              private dialogService: MatDialog,
              private changeDetector: ChangeDetectorRef) {
    super(typeService);
    appInfoService.getConfig()
      .subscribe(next => this.config = next);
  }

  LARGE_RESPONSE_LIMIT = 1_048_576; // 1MB
  @Input()
  activeTabIndex: number = 0;

  @Input()
  downloadSupported = true;

  @Input()
  profilerEnabled: boolean = true;

  @Input()
  errorCount = 0

  @Output()
  loadProfileData = new EventEmitter();

  downloadMenuOpen = false;

  hasModelFormatSpecs: Subject<boolean> = new BehaviorSubject(true);
  private jsonInstances$: Observable<string> = of();
  responseIsLarge$: Observable<boolean> = of(false);

  get displayMode(): DisplayMode {
    switch (this.activeTabIndex) {
      case 0:
        return 'table';
      case 1:
        return 'tree';
      case 2:
        return 'json';
    }
  }

  protected _instances$: Observable<InstanceLike>;
  PROFILER_TAB_INDEX = 3;

  @Input()
  errorMessages$: Observable<StreamQueryErrorEvent>

  @Input()
  get instances$(): Observable<InstanceLike> {
    return this._instances$;
  }

  set instances$(value: Observable<InstanceLike>) {
    if (isNullOrUndefined(value)) {
      this._instances$ = EMPTY;
    } else {
      this._instances$ = value;
    }

    // If we're currently on the Profiler tab, switch back, as the
    // profile data is now stale.
    if (this.activeTabIndex === this.PROFILER_TAB_INDEX) {
      //this.activeTabIndex = 0;
    }

    this.jsonInstances$ = this.instances$.pipe(
      filter(result => this.activeTabIndex === 2),
      map((result) => JSON.stringify(result.value))
    );

    this.responseIsLarge$ = this.jsonInstances$
      .pipe(
        scan((acc, curr) => curr.length + acc, 0),
        map(responseSize => {
          return responseSize > this.LARGE_RESPONSE_LIMIT;
        }),
        tap((isLargeResponse) => {
          if (this.activeTabIndex < 2 && isLargeResponse) {
            // Only show JSON in large responses.
            this.activeTabIndex = 2;
          }
        })
      );

    this.changeDetector.markForCheck();
  }

  protected _type: Type;

  get showResultsPanel(): boolean {
    return !isNullOrUndefined(this.type) || !isNullOrUndefined(this._instances$);
  }

  @Input()
  get type(): Type {
    return this._type;
  }

  set type(value: Type) {
    if (value === this._type) {
      return;
    }
    this._type = value;

    if (value) {
      this.typeService
        .getModelFormatSpecsForType(this.type)
        .subscribe(data => this.hasModelFormatSpecs.next(data.length > 0));
    }
    this.changeDetector.detectChanges();
  }

  @Input()
  anonymousTypes: Type[] = [];

  @Output()
  downloadClicked = new EventEmitter<DownloadClickedEvent>();

  @Output()
  pauseStreamToggled = new EventEmitter<boolean>();

  @Input()
  profileData$: Observable<QueryProfileData>;

  @Input()
  isStreamingQuery: boolean;

  @Input()
  isQueryPaused: boolean;

  protected updateDataSources() {
  }
  onDownloadClicked(format: ExportFormat) {
    if (this.config.analytics.persistResults) {
      this.downloadClicked.emit(new DownloadClickedEvent(format));
    } else {
      // this.resultsTable.downloadAsCsvFromGrid();
    }
  }

  showDisabledTestCaseConfig($event) {
    $event.preventDefault();
    $event.stopPropagation();
    this.dialogService.open(ConfigDisabledFormComponent);
  }

  showDisabledPersistResultsConfig($event) {
    $event.preventDefault();
    $event.stopPropagation();
    this.dialogService.open(ConfigPersistResultsDisabledFormComponent);
  }


  onTabIndexChanged($event: number) {
    this.activeTabIndex = $event
    if (this.activeTabIndex === this.PROFILER_TAB_INDEX) {
      this.loadProfileData.emit();
    }
    this.changeDetector.detectChanges();
  }

  protected readonly tuiIconPlay = tuiIconPlay;
  protected readonly tuiIconPause = tuiIconPause;
}
