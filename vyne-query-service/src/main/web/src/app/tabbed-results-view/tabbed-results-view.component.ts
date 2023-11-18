import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { BehaviorSubject, EMPTY, Observable, of, Subject } from 'rxjs';
import { DisplayMode, DownloadClickedEvent } from '../object-view/object-view-container.component';
import { InstanceLike, Type } from '../services/schema';
import { QueryProfileData } from '../services/query.service';
import { BaseQueryResultComponent } from '../query-panel/result-display/BaseQueryResultComponent';
import { TypesService } from '../services/types.service';
import { AppInfoService, AppConfig } from '../services/app-info.service';
import { ConfigDisabledFormComponent } from '../test-pack-module/config-disabled-form.component';
import {
  ConfigPersistResultsDisabledFormComponent
} from '../test-pack-module/config-persist-results-disabled-form.component';
import { MatLegacyDialog as MatDialog } from '@angular/material/legacy-dialog';
import { isNullOrUndefined } from 'util';
import { ExportFormat } from 'src/app/results-download/results-download.service';
import { map, scan, tap } from 'rxjs/operators';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-tabbed-results-view',
  template: `
    <!--    <app-error-panel *ngIf="lastQueryResultAsSuccess?.unmatchedNodes?.length > 0"-->
    <!--                     [queryResult]="lastQueryResultAsSuccess">-->
    <!--    </app-error-panel>-->
    <div class="alert" *ngIf="responseIsLarge$ | async">The response is really big. Some features have been disabled.
    </div>
    <app-panel-header title="Results" alignItems="left">
      <tui-tabs [(activeItemIndex)]="activeTabIndex" *ngIf="showResultsPanel"
                (activeItemIndexChange)="onTabIndexChanged()">
        <button tuiTab [disabled]="responseIsLarge$ | async">
          <img src="assets/img/tabler/table.svg" class="tab-icon">
          Table
        </button>
        <button tuiTab [disabled]="responseIsLarge$ | async">
          <img src="assets/img/tree-list.svg" class="tab-icon">
          Tree
        </button>
        <button tuiTab>
          <img src="assets/img/tabler/code-dots.svg" class="tab-icon">
          Raw
        </button>
        <button tuiTab *ngIf="profilerEnabled">
          <img src="assets/img/tabler/gauge.svg" class="tab-icon">
          Profiler
        </button>
      </tui-tabs>
      <div class="spacer"></div>
      <tui-hosted-dropdown
          *ngIf="showResultsPanel && downloadSupported"
        tuiDropdownAlign="left"
        [content]="dropdown"
        [(open)]="downloadMenuOpen"
      >
        <button tuiButton type="button" appearance="flat" [iconRight]="icon" size="m">
          Download
        </button>
      </tui-hosted-dropdown>

    </app-panel-header>
    <app-object-view-container
      *ngIf="activeTabIndex < 3 && showResultsPanel"
      [instances$]="_instances$"
      [schema]="schema"
      [displayMode]="displayMode"
      [selectable]="true"
      [downloadSupported]="downloadSupported"
      (downloadClicked)="this.downloadClicked.emit($event)"
      [type]="type"
      [anonymousTypes]="anonymousTypes"
      (instanceClicked)="instanceClicked($event,type.name)"></app-object-view-container>
    <app-call-explorer [queryProfileData$]="profileData$"
                       *ngIf="activeTabIndex === 3 && showResultsPanel"></app-call-explorer>

    <ng-template #icon>
      <tui-svg
        src="tuiIconChevronDown"
        class="icon"
        [class.icon_rotated]="downloadMenuOpen"
      ></tui-svg>
    </ng-template>
    <ng-template #dropdown>
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


  constructor(protected typeService: TypesService, protected appInfoService: AppInfoService, private dialogService: MatDialog, private changeDetector: ChangeDetectorRef) {
    super(typeService);
    appInfoService.getConfig()
      .subscribe(next => this.config = next);
  }

  LARGE_RESPONSE_LIMIT = 1_048_576; // 1MB
  activeTabIndex: number = 0;
  @Input()
  downloadSupported = true;

  @Input()
  profilerEnabled: boolean = true;

  @Output()
  loadProfileData = new EventEmitter();

  downloadMenuOpen = false;

  hasModelFormatSpecs: Subject<boolean> = new BehaviorSubject(true);
  jsonInstances$: Observable<string> = of();
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

  private _instances$: Observable<InstanceLike>;
  PROFILER_TAB_INDEX = 3;

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
      this.activeTabIndex = 0;
    }

    this.jsonInstances$ = this.instances$.pipe(
      map((result) => JSON.stringify(result.value))
    );

    this.responseIsLarge$ = this.jsonInstances$
      .pipe(
        scan((acc, curr) => curr.length + acc, 0),
        map(responseSize => {
          return responseSize > this.LARGE_RESPONSE_LIMIT;
        }),
        tap((isLargeResponse) => {
          if (isLargeResponse) {
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

  @Input()
  profileData$: Observable<QueryProfileData>;

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


  onTabIndexChanged() {
    if (this.activeTabIndex === this.PROFILER_TAB_INDEX) {
      this.loadProfileData.emit();
    }
  }
}
