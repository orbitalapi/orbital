import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {BaseTypedInstanceViewer} from './BaseTypedInstanceViewer';
import {DownloadFileType} from '../query-panel/result-display/result-container.component';
import {Type, InstanceLikeOrCollection} from '../services/schema';

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
        </mat-menu>
      </div>
      <div *ngIf="ready" class="display-wrapper">
        <app-results-table *ngIf="displayMode==='table'"
                           [instance]="instance"
                           [schema]="schema"
                           [selectable]="selectable"
                           [type]="type"
                           (instanceClicked)="instanceClicked.emit($event)">
        </app-results-table>
        <app-object-view *ngIf="displayMode==='tree' && instance"
                         [instance]="instance"
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

  private _displayMode: DisplayMode = 'table';
  @Input()
    // tslint:disable-next-line:no-inferrable-types
  selectable: boolean = false;

  get ready() {
    return this.instance && this.schema && this.type;
  }

  @Output()
  downloadClicked = new EventEmitter<DownloadClickedEvent>();

  @Input()
  get instance(): InstanceLikeOrCollection {
    return this._instance;
  }

  @Input()
  downloadSupported = false;

  private _localInstance: InstanceLikeOrCollection;
  private _localInstanceCopy: InstanceLikeOrCollection;

  set instance(value: InstanceLikeOrCollection) {
    this._instance = value;
    this._localInstance = value;
    this._localInstanceCopy = JSON.parse(JSON.stringify(value));
    // When the instance changes, any assumptions we've made about
    // types based on the old instance are invalid, so null out to recompute
    this._derivedType = null;
    this._collectionMemberType = null;
  }

  onDownloadClicked(format: DownloadFileType) {
    this.downloadClicked.emit(new DownloadClickedEvent(format));
  }
}

export type DisplayMode = 'table' | 'tree';

export class DownloadClickedEvent {
  constructor(public readonly format: DownloadFileType) {
  }
}
