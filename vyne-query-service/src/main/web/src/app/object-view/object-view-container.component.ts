import {AfterContentInit, Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {BaseTypedInstanceViewer} from './BaseTypedInstanceViewer';
import {Type, InstanceLikeOrCollection, InstanceLike} from '../services/schema';
import {Observable, Subscription} from 'rxjs';
import {ExportFormat} from '../services/export.file.service';
import {ObjectViewComponent} from './object-view.component';
import {ResultsTableComponent} from '../results-table/results-table.component';
import {tap} from 'rxjs/operators';
import {AppInfoService, QueryServiceConfig} from '../services/app-info.service';
import {MatDialog} from '@angular/material/dialog';
import {ConfigDisabledFormComponent} from '../test-pack-module/config-disabled-form.component';
import {ConfigPersistResultsDisabledFormComponent} from "../test-pack-module/config-persist-results-disabled-form.component";

@Component({
  selector: 'app-object-view-container',
  template: `
    <div class="container" *ngIf="ready">
      <div class="toolbar">
        <div class="type-name">{{ type?.name.shortDisplayName }}</div>
        <mat-button-toggle-group [(ngModel)]="displayMode" id="result-view-type-selection">
          <mat-button-toggle value="table" aria-label="Text align left" id="btn-table-view">
            <img class="icon" src="assets/img/table-view.svg">
          </mat-button-toggle>
          <mat-button-toggle value="tree" aria-label="Text align left" id="btn-tree-view">
            <img class="icon" src="assets/img/tree-view.svg">
          </mat-button-toggle>
        </mat-button-toggle-group>
        <div class="spacer"></div>
        <button mat-stroked-button [matMenuTriggerFor]="menu" *ngIf="downloadSupported"
                class="downloadFileButton">Download
        </button>
        <mat-menu #menu="matMenu">
          <button mat-menu-item (click)="onDownloadClicked(downloadFileType.JSON)"
                  [disabled]="!config?.history.persistResults">as JSON
            <a *ngIf="!config?.history.persistResults"
               href="#"
               (click)="showDisabledPersistResultsConfig($event)">Why is this disabled?</a>
          </button>
          <button mat-menu-item (click)="onDownloadClicked(downloadFileType.CSV)">as CSV</button>
          <button mat-menu-item (click)="onDownloadClicked(downloadFileType.TEST_CASE)"
                  [disabled]="!config?.history.persistRemoteCallResponses || !config?.history.persistResults">as Test Case
            <a *ngIf="!config?.history.persistRemoteCallResponses || !config?.history.persistResults"
               href="#"
               (click)="showDisabledTestCaseConfig($event)">Why is this disabled?</a>
          </button>
        </mat-menu>
      </div>
      <div class="display-wrapper">
        <app-results-table *ngIf="displayMode==='table'"
                           [instances$]="instances$"
                           [rowData]="instances"
                           [schema]="schema"
                           [selectable]="selectable"
                           [type]="type"
                           [anonymousTypes]="anonymousTypes"
                           (instanceClicked)="instanceClicked.emit($event)">
        </app-results-table>
        <app-object-view *ngIf="displayMode==='tree' && instances"
                         [instance]="instances"
                         [schema]="schema"
                         [selectable]="selectable"
                         [type]="type"
                         [anonymousTypes]="anonymousTypes"
                         (instanceClicked)="instanceClicked.emit($event)">
        </app-object-view>
      </div>
    </div>
  `,
  styleUrls: ['./object-view-container.component.scss']
})
export class ObjectViewContainerComponent extends BaseTypedInstanceViewer implements AfterContentInit {
  // workaround for lack of enum support in templates
  downloadFileType = ExportFormat;

  config: QueryServiceConfig;

  constructor(appInfoService: AppInfoService, private dialogService: MatDialog) {
    super();
    appInfoService.getConfig()
      .subscribe(next => this.config = next);
  }

  @ViewChild(ResultsTableComponent, {static: false})
  resultsTable: ResultsTableComponent;

  get displayMode(): DisplayMode {
    return this._displayMode;
  }

  set displayMode(value: DisplayMode) {
    this._displayMode = value;
  }

  instances: InstanceLike[];
  private _displayMode: DisplayMode = 'table';
  private _instances$: Observable<InstanceLike>;
  private _instanceSubscription: Subscription;
  @Input()
    // tslint:disable-next-line:no-inferrable-types
  selectable: boolean = false;

  @Input()
  anonymousTypes: Type[];

  get ready() {
    return this.instances$ && this.schema && this.type;
  }

  @Input()
  get instances$(): Observable<InstanceLike> {
    return this._instances$;
  }

  set instances$(value: Observable<InstanceLike>) {
    if (value === this._instances$) {
      return;
    }
    if (this._instanceSubscription) {
      this._instanceSubscription.unsubscribe();
    }
    this._instances$ = value;
    this.instances = [];
    this._instances$.subscribe(next => {
      this.instances.push(next);
    });

  }

  @Output()
  downloadClicked = new EventEmitter<DownloadClickedEvent>();

  @Input()
  downloadSupported = false;

  downloadRegressionPack: any;

  ngAfterContentInit(): void {
    console.log('object-view-container -> ngAfterContentInit');
    this.remeasureTable();
  }


  onDownloadClicked(format: ExportFormat) {
    if (this.config.history.persistResults) {
      this.downloadClicked.emit(new DownloadClickedEvent(format));
    } else {
      this.resultsTable.downloadAsCsvFromGrid();
    }
  }

  remeasureTable() {
    if (this.resultsTable) {
      console.log('Remeasuring resultsTable');
      this.resultsTable.remeasure();
    } else {
      console.warn('Called remeasureTable, but the resultsTable component isnt available yet');
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

export type DisplayMode = 'table' | 'tree';

export class DownloadClickedEvent {
  constructor(public readonly format: ExportFormat) {
  }
}
