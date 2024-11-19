import {TuiPulse, TuiSegmented} from '@taiga-ui/kit';
import {TuiExpand, TuiNotification} from '@taiga-ui/core';
import {Component, EventEmitter, Input, Output} from '@angular/core';
import {JsonViewerModule} from "../../json-viewer/json-viewer.module";
import {NgIf} from "@angular/common";
import {ExpandablePanelComponent} from "../../expanding-panelset/expandable-panel/expandable-panel.component";
import {QueryProfileData} from "../../services/query.service";
import {ExpandingPanelSetModule} from "../../expanding-panelset/expanding-panel-set.module";
import {CallExplorerModule} from "../../query-panel/taxi-viewer/call-explorer/call-explorer.module";
import {SequenceDiagramModule} from "../../query-panel/taxi-viewer/sequence-diagram/sequence-diagram.module";
import {BehaviorSubject} from "rxjs";
import {LineageDisplayModule} from "../../lineage-display/lineage-display.module";

@Component({
  selector: 'app-query-results-panel',
  standalone: true,
  imports: [
    JsonViewerModule,
    NgIf,
    ExpandablePanelComponent,
    ExpandingPanelSetModule,
    TuiExpand,
    TuiSegmented,
    CallExplorerModule,
    SequenceDiagramModule,
    LineageDisplayModule,
    TuiPulse,
    TuiNotification,
  ],
  template: `
    <app-panel-header [collapsible]="true" [(expanded)]="expanded"
                      [isSecondary]="true"
                      title="Results">
      <div class="spacer"></div>
      <tui-segmented size="s" [activeItemIndex]="viewModeActiveIndex"
                     (activeItemIndexChange)="viewModeActiveIndexChanged($event)" class="dark view-mode-segments"
                     *ngIf="queryResult"
                     (click)="$event.stopImmediatePropagation()">
        <button [class.active]="viewModeActiveIndex === 0">Results</button>
        <button [class.active]="viewModeActiveIndex === 1">
          Lineage Diagram
          <tui-pulse *ngIf="!hasLineageDiagramViewModeBeenActivated"/>
        </button>
        <button [class.active]="viewModeActiveIndex === 2">
          Requests
          <tui-pulse *ngIf="!hasRequestViewModeBeenActivated"/>
        </button>
      </tui-segmented>
    </app-panel-header>
    <tui-expand [expanded]="expanded" class="flex-expand">
      <ng-template tuiExpandContent>
        <div class="result-panel">
          <tui-notification *ngIf="usesStubs && viewModeActiveIndex > 0" size="s">Service calls are stubbed. All requests appear as HTTP GETs.</tui-notification>
          <app-json-viewer [readOnly]="true" [json]="queryResult" [showHeader]="false"
                           *ngIf="queryResult && viewModeActiveIndex == 0"></app-json-viewer>
          <app-query-lineage class="inline" [(fullscreen)]="queryPlanFullscreen"
                             *ngIf="!queryPlanFullscreen && profileData && viewModeActiveIndex == 1"
                             [rows]="profileData?.queryLineageData"></app-query-lineage>

          <app-sequence-diagram [profileData$]="profileData$"
                                *ngIf="profileData && viewModeActiveIndex == 2"></app-sequence-diagram>
          <div *ngIf="!queryResult" class="empty-results">
            No results to show
          </div>
        </div>
      </ng-template>
    </tui-expand>
    <!-- This is a hacky workaround.
 See comments on QueryLineageComponent fullscreen as to why we have have two app-query-lineage
 components (one inside the accordion, and one outside)
 -->
    <app-query-lineage *ngIf="profileData && queryPlanFullscreen" [(fullscreen)]="queryPlanFullscreen"
                       [rows]="profileData?.queryLineageData"></app-query-lineage>
  `,
  styleUrl: './query-results-panel.component.scss'
})
export class QueryResultsPanelComponent {
  profileDataSink = new BehaviorSubject<QueryProfileData>(null);
  profileData$ = this.profileDataSink.asObservable();

  queryPlanFullscreen: boolean = false;
  private _profileData: QueryProfileData
  @Input()
  get profileData(): QueryProfileData {
    return this._profileData;
  }

  set profileData(value: QueryProfileData) {
    this._profileData = value;
    this.profileDataSink.next(value);
  }

  viewModeActiveIndex = 0

  @Input()
  usesStubs: boolean = false;

  @Input()
  expanded: boolean = false;

  @Output()
  expandedChange = new EventEmitter<boolean>()

  setExpanded(expanded: boolean) {
    this.expanded = expanded;
    this.expandedChange.emit(expanded)
  }

  @Input()
  queryResult: string

  viewModeActiveIndexChanged(index: number) {
    this.viewModeActiveIndex = index
    if (this.viewModeActiveIndex === 1) {
      this.hasLineageDiagramViewModeBeenActivated = true
    } else if (this.viewModeActiveIndex === 2) {
      this.hasRequestViewModeBeenActivated = true
    }
  }

  hasLineageDiagramViewModeBeenActivated = false
  hasRequestViewModeBeenActivated = false

}
