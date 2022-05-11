import {
  AfterContentInit,
  ChangeDetectionStrategy, ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import {BaseTypedInstanceViewer} from './BaseTypedInstanceViewer';
import {Type, InstanceLike} from '../services/schema';
import {Observable, Subscription, BehaviorSubject, Subject} from 'rxjs';
import {ExportFormat} from '../services/export.file.service';
import {ResultsTableComponent} from '../results-table/results-table.component';
import {AppInfoService, QueryServiceConfig} from '../services/app-info.service';
import {MatDialog} from '@angular/material/dialog';
import {ConfigDisabledFormComponent} from '../test-pack-module/config-disabled-form.component';
import {
  ConfigPersistResultsDisabledFormComponent
} from '../test-pack-module/config-persist-results-disabled-form.component';
import {TypesService} from '../services/types.service';
import {debounce, throttleTime} from "rxjs/operators";

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-object-view-container',
  template: `
    <div class="container" *ngIf="ready">
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
        <app-object-view *ngIf="displayMode==='tree'"
                         [instances$]="instances$"
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

  constructor(
    private typesService: TypesService,
    appInfoService: AppInfoService,
    private changeDetector: ChangeDetectorRef) {
    super();
    appInfoService.getConfig()
      .subscribe(next => this.config = next);

    this.instancesChanged$
      .pipe(throttleTime(500))
      .subscribe(() => changeDetector.markForCheck())
  }

  @ViewChild(ResultsTableComponent)
  resultsTable: ResultsTableComponent;

  @Input()
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
    // eslint-disable-next-line @typescript-eslint/no-inferrable-types
  selectable: boolean = false;

  @Input()
  anonymousTypes: Type[];

  private instancesChanged$: EventEmitter<void> = new EventEmitter<void>();

  get ready() {
    return this.instances$ && this.schema;
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
      this.instancesChanged$.emit();
    });

  }

  @Output()
  downloadClicked = new EventEmitter<DownloadClickedEvent>();

  @Input()
  downloadSupported = false;

  downloadRegressionPack: any;

  @Input()
  get type(): Type {
    return this._type;
  }

  set type(value: Type) {
    if (value === this._type) {
      return;
    }
    this._type = value;

  }


  ngAfterContentInit(): void {
    this.remeasureTable();
  }


  onDownloadClicked(format: ExportFormat) {
    if (this.config.analytics.persistResults) {
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


}

export type DisplayMode = 'table' | 'tree';

export class DownloadClickedEvent {
  constructor(public readonly format: ExportFormat) {
  }
}
