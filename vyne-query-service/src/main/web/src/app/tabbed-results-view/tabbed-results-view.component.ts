import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output} from '@angular/core';
import {BehaviorSubject, Observable, Subject} from 'rxjs';
import {DownloadClickedEvent} from '../object-view/object-view-container.component';
import {InstanceLike, Type} from '../services/schema';
import {QueryProfileData} from '../services/query.service';
import {BaseQueryResultComponent} from '../query-panel/result-display/BaseQueryResultComponent';
import {TypesService} from '../services/types.service';
import {AppInfoService, QueryServiceConfig} from '../services/app-info.service';
import {ConfigDisabledFormComponent} from '../test-pack-module/config-disabled-form.component';
import {ConfigPersistResultsDisabledFormComponent} from '../test-pack-module/config-persist-results-disabled-form.component';
import {MatDialog} from '@angular/material/dialog';
import {ExportFormat} from '../services/export.file.service';
import {isNullOrUndefined} from 'util';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-tabbed-results-view',
  template: `
    <!--    <app-error-panel *ngIf="lastQueryResultAsSuccess?.unmatchedNodes?.length > 0"-->
    <!--                     [queryResult]="lastQueryResultAsSuccess">-->
    <!--    </app-error-panel>-->
    <app-panel-header title="Results" alignItems="left">
      <tui-tabs [(activeItemIndex)]="activeTabIndex" *ngIf="showResultsPanel">
        <button tuiTab>
          <img src="assets/img/tabler/table.svg" class="tab-icon">
          Table
        </button>
        <button tuiTab>
          <img src="assets/img/tree-list.svg" class="tab-icon">
          Tree
        </button>
        <button tuiTab>
          <img src="assets/img/tabler/gauge.svg" class="tab-icon">
          Profiler
        </button>
      </tui-tabs>
      <div class="spacer"></div>
      <tui-hosted-dropdown
        *ngIf="showResultsPanel"
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
      *ngIf="activeTabIndex < 2 && showResultsPanel"
      [instances$]="instances$"
      [schema]="schema"
      [displayMode]="activeTabIndex === 0 ? 'table' : 'tree'"
      [selectable]="true"
      [downloadSupported]="downloadSupported"
      (downloadClicked)="this.downloadClicked.emit($event)"
      [type]="type"
      [anonymousTypes]="anonymousTypes"
      (instanceClicked)="instanceClicked($event,type.name)"></app-object-view-container>
    <app-call-explorer [queryProfileData$]="profileData$"
                       *ngIf="activeTabIndex === 2 && showResultsPanel"></app-call-explorer>

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
  config: QueryServiceConfig;
// workaround for lack of enum support in templates
  downloadFileType = ExportFormat;

  constructor(protected typeService: TypesService, protected appInfoService: AppInfoService, private dialogService: MatDialog, private changeDetector: ChangeDetectorRef) {
    super(typeService);
    appInfoService.getConfig()
      .subscribe(next => this.config = next);
  }

  activeTabIndex: number = 0;
  @Input()
  downloadSupported = true;

  downloadMenuOpen = false;

  hasModelFormatSpecs: Subject<boolean> = new BehaviorSubject(true);


  @Input()
  instances$: Observable<InstanceLike>;

  protected _type: Type;

  get showResultsPanel(): boolean {
    return !isNullOrUndefined(this.type) || !isNullOrUndefined(this.instances$);
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


}
