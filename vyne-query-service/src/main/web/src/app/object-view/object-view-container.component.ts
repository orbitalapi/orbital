import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {BaseTypedInstanceViewer} from './BaseTypedInstanceViewer';
import {DownloadFileType} from '../query-panel/result-display/result-container.component';
import {Type, InstanceLikeOrCollection, InstanceLike} from '../services/schema';
import {Observable, Subscription} from 'rxjs';

@Component({
  selector: 'app-object-view-container',
  template: `
    <div class="container">
      <div class="toolbar">
        <div class="type-name">{{ type?.name.shortDisplayName }}</div>
        <mat-button-toggle-group [(ngModel)]="displayMode">
          <mat-button-toggle value="table" aria-label="Text align left">
            <img class="icon" src="assets/img/table-view.svg">
          </mat-button-toggle>
          <mat-button-toggle value="tree" aria-label="Text align left">
            <img class="icon" src="assets/img/tree-view.svg">
          </mat-button-toggle>
        </mat-button-toggle-group>
        <div class="spacer"></div>
        <button mat-stroked-button [matMenuTriggerFor]="menu" *ngIf="downloadSupported"
                class="downloadFileButton">Download
        </button>
        <mat-menu #menu="matMenu">
          <button mat-menu-item (click)="onDownloadClicked(downloadFileType.JSON)">as JSON</button>
          <button mat-menu-item (click)="onDownloadClicked(downloadFileType.CSV)">as CSV</button>
          <button mat-menu-item (click)="onDownloadClicked(downloadFileType.TEST_CASE)">as Test Case</button>
        </mat-menu>
      </div>
      <div *ngIf="ready" class="display-wrapper">
        <app-results-table *ngIf="displayMode==='table'"
                           [instances$]="instances$"
                           [rowData]="instances"
                           [schema]="schema"
                           [selectable]="selectable"
                           [type]="type"
                           (instanceClicked)="instanceClicked.emit($event)">
        </app-results-table>
        <app-object-view *ngIf="displayMode==='tree' && instances"
                         [instance]="instances"
                         [schema]="schema"
                         [selectable]="selectable"
                         [type]="type"
                         (instanceClicked)="instanceClicked.emit($event)">
        </app-object-view>
      </div>
    </div>
  `,
  styleUrls: ['./object-view-container.component.scss']
})
export class ObjectViewContainerComponent extends BaseTypedInstanceViewer {
  // workaroun for lack of enum support in templates
  downloadFileType = DownloadFileType;

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

  onDownloadClicked(format: DownloadFileType) {
    this.downloadClicked.emit(new DownloadClickedEvent(format));
  }
}

export type DisplayMode = 'table' | 'tree';

export class DownloadClickedEvent {
  constructor(public readonly format: DownloadFileType) {
  }
}
