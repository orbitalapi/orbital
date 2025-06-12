import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import {
  QueryPlan,
  QueryProfileData,
  QuerySankeyChartRow,
  QueryService,
  RemoteCallResponse,
  RemoteOperationPerformanceStats
} from '../../../services/query.service';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { isNullOrUndefined } from 'src/app/utils/utils';

@Component({
  selector: 'app-call-explorer',
  template: `
    <div class="toolbar" *ngIf="!onlyShowQueryPlan">
      <tui-segmented size="s">
        <label><input name="radio" type="radio" value="lineage" [(ngModel)]="displayMode"><app-svg-icon [width]="22" [height]="22" [strokeWidth]="1.5" tabler="binary-tree"></app-svg-icon>Lineage</label>
        <label><input name="radio" type="radio" value="sequence" [(ngModel)]="displayMode"><app-svg-icon [width]="22" [height]="22" [strokeWidth]="1.5" tabler="arrows-right-left"></app-svg-icon>Sequence</label>
        <label><input name="radio" type="radio" value="stats" [(ngModel)]="displayMode"><app-svg-icon [width]="22" [height]="22" [strokeWidth]="1.5" tabler="table"></app-svg-icon>Stats</label>
        <label><input name="radio" type="radio" value="trace" [(ngModel)]="displayMode"><app-svg-icon [width]="22" [height]="22" [strokeWidth]="1.5" tabler="arrows-right-left"></app-svg-icon>Trace</label>
      </tui-segmented>
<!--      <mat-button-toggle-group [(ngModel)]='displayMode' data-e2e-id='profiler-call-operation-selection'>-->
<!--        <mat-button-toggle value='lineage' data-e2e-id='call-select' title='Query Lineage'>-->
<!--          <img class='icon' src='assets/img/lineage-nodes.svg'>-->
<!--        </mat-button-toggle>-->
<!--        <mat-button-toggle value='sequence' data-e2e-id='call-select' title='Sequence Diagram'>-->
<!--          <img class='icon' src='assets/img/sequence.svg'>-->
<!--        </mat-button-toggle>-->
<!--        <mat-button-toggle value='stats' data-e2e-id='operation-select' title='Stats'>-->
<!--          <img class='icon' src='assets/img/table-view.svg'>-->
<!--        </mat-button-toggle>-->
<!--      </mat-button-toggle-group>-->
    </div>
    <app-query-lineage
      *ngIf="displayMode === 'lineage'"
      [rows]='querySankeyChartRows$ | async'
      [class.has-margin-top]="onlyShowQueryPlan"
    ></app-query-lineage>
    <app-waterfall *ngIf="displayMode === 'trace'" [traceSpans]="(queryProfileData$ | async).traceSpans"></app-waterfall>
    <div class='sequence-diagram-container' *ngIf="displayMode === 'sequence'">
      <as-split direction='horizontal' unit='pixel'>
        <as-split-area [size]='500'>
          <div class='operation-list-container'>
            <div class='header'>
              <div class='table-header'>Calls</div>
              <div class='table-subheader'>(Click to explore)</div>
            </div>
              <cdk-virtual-scroll-viewport class="cdk-viewport-list" itemSize="46.5">
                <div *cdkVirtualFor="let remoteCall of remoteCalls$ | async"
                     class="operation"
                     [ngClass]="{'active': remoteCall === selectedOperation}"
                     (click)='selectOperation(remoteCall)'
                >
                  <div class='pill verb'>{{ remoteCall.method }}</div>
                  <div class='pill result'
                       [ngClass]='statusTextClassForRemoteCall(remoteCall)'>{{ remoteCall.resultCode }}</div>
                  <div class='pill duration'>{{ remoteCall.durationMs }}ms</div>
                  <div class='address'
                       [matTooltip]='getPathOnly(remoteCall.address)'>{{ remoteCall.displayName }}</div>
                </div>
              </cdk-virtual-scroll-viewport>
          </div>
        </as-split-area>
        <as-split-area size='*'>
          <div class='chart-container' *ngIf='!selectedOperation'>
            <app-sequence-diagram [profileData$]='queryProfileData$'></app-sequence-diagram>
          </div>
          <app-call-explorer-operation-view [operation]='selectedOperation' *ngIf='selectedOperation'
                                            [operationResponse$]='selectedOperationResult$'
                                            (close)='selectedOperation = null'></app-call-explorer-operation-view>

        </as-split-area>
      </as-split>
    </div>
    <app-service-stats *ngIf="displayMode === 'stats'" [operationStats]='operationStats$ | async'></app-service-stats>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./call-explorer.component.scss']
})
export class CallExplorerComponent {
  operationStats$: Observable<RemoteOperationPerformanceStats[]>;
  querySankeyChartRows$: Observable<QuerySankeyChartRow[]>;

  constructor(private queryService: QueryService) {
  }

  remoteCalls$: Observable<RemoteCallResponse[]>;

  private _queryProfileData$: Observable<QueryProfileData>;
  @Input()
  get queryProfileData$(): Observable<QueryProfileData> {
    return this._queryProfileData$;
  }

  set queryProfileData$(value: Observable<QueryProfileData>) {
    if (this._queryProfileData$ === value || isNullOrUndefined(value)) {
      return;
    }
    this._queryProfileData$ = value;
    this.remoteCalls$ = value.pipe(map(queryProfileData => queryProfileData.remoteCalls));
    this.operationStats$ = value.pipe(map(queryProfileData => queryProfileData.operationStats));
    this.querySankeyChartRows$ = value.pipe(map(queryProfileData => queryProfileData.queryLineageData));
  }

  private _queryPlanData$: Observable<QueryPlan>;
  @Input()
  get queryPlanData$(): Observable<QueryPlan> {
    return this._queryPlanData$;
  }

  set queryPlanData$(value: Observable<QueryPlan>) {
    if (this._queryPlanData$ === value || isNullOrUndefined(value)) {
      return;
    }
    this._queryPlanData$ = value;
    this.querySankeyChartRows$ = value.pipe(map(queryPlan => queryPlan.steps));
  }

  @Input()
  onlyShowQueryPlan: boolean

  selectedOperation: RemoteCallResponse;
  selectedOperationResult$: Observable<string>;
  displayMode: CallExplorerDisplayMode = 'lineage';

  getPathOnly(address: string) {
    // Hack - there's proabably a better way
    const parts: string[] = address.split('/');
    return '/' + parts.slice(3).join('/');
  }

  selectOperation(operation: RemoteCallResponse) {
    this.selectedOperation = operation;
    this.selectedOperationResult$ = this.queryService.getRemoteCallResponse(operation.remoteCallId);
  }

  statusTextClassForRemoteCall(remoteCall: RemoteCallResponse): string {
    return statusTextClass(remoteCall.resultCode);
  }
}

export type CallExplorerDisplayMode = 'sequence' | 'stats' | 'lineage' | 'trace';

export function statusTextClass(resultCode: string): string {

  switch (resultCode) {
    case 'OK':
      return 'status-success';
    case 'ERROR' :
      return 'status-error';
  }
  const codeStart = resultCode.substr(0, 1);
  switch (codeStart) {
    case '2' :
      return 'status-success';
    case '3' :
      return 'status-success';
    case '4' :
      return 'status-error';
    case '5' :
      return 'status-error';
  }
}
