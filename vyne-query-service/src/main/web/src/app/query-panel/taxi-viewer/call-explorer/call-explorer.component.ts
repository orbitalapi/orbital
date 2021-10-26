import {Component, Input} from '@angular/core';
import {
  QueryProfileData,
  QueryResult, QuerySankeyChartRow,
  QueryService,
  RemoteCall,
  RemoteOperationPerformanceStats
} from '../../../services/query.service';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {Operation} from '../../../services/schema';

@Component({
  selector: 'app-call-explorer',
  template: `
    <div class="toolbar">
      <mat-button-toggle-group [(ngModel)]="displayMode"  data-e2e-id="profiler-call-operation-selection">
        <mat-button-toggle value="lineage" data-e2e-id="call-select">
          <img class="icon" src="assets/img/lineage-nodes.svg">
        </mat-button-toggle>
        <mat-button-toggle value="sequence" data-e2e-id="call-select">
          <img class="icon" src="assets/img/sequence.svg">
        </mat-button-toggle>
        <mat-button-toggle value="stats" data-e2e-id="operation-select">
          <img class="icon" src="assets/img/table-view.svg">
        </mat-button-toggle>
      </mat-button-toggle-group>
    </div>
    <div class="sequence-diagram-container" *ngIf="displayMode === 'sequence'">
      <div class="operation-list-container">
        <div class="header">
          <div class="table-header">Calls</div>
          <div class="table-subheader">(Click to explore)</div>
        </div>
        <div class="operation-list">
          <div class="operation" *ngFor="let remoteCall of remoteCalls$ | async" (click)="selectOperation(remoteCall)">
            <div class="pill verb">{{ remoteCall.method }}</div>
            <div class="pill result"
                 [ngClass]="statusTextClassForRemoteCall(remoteCall)">{{ remoteCall.resultCode }}</div>
            <div class="pill duration">{{ remoteCall.durationMs }}ms</div>
            <div class="address"
                 [matTooltip]="getPathOnly(remoteCall.address)">{{ getOperationName(remoteCall) }}</div>
          </div>
        </div>
      </div>
      <div class="chart-container" *ngIf="!selectedOperation">
        <app-sequence-diagram [profileData$]="queryProfileData$"></app-sequence-diagram>
      </div>
      <app-call-explorer-operation-view [operation]="selectedOperation" *ngIf="selectedOperation"
                                        [operationResponse$]="selectedOperationResult$"
                                        (close)="selectedOperation = null"></app-call-explorer-operation-view>
    </div>
    <app-service-stats *ngIf="displayMode === 'stats'" [operationStats]="operationStats$ | async"></app-service-stats>
    <app-query-lineage *ngIf="displayMode === 'lineage'" [rows]="querySankeyChartRows$ | async"></app-query-lineage>
  `,
  styleUrls: ['./call-explorer.component.scss']
})
export class CallExplorerComponent {
  operationStats$: Observable<RemoteOperationPerformanceStats[]>;
  querySankeyChartRows$: Observable<QuerySankeyChartRow[]>;

  constructor(private queryService: QueryService) {
  }

  private _queryProfileData$: Observable<QueryProfileData>;

  remoteCalls$: Observable<RemoteCall[]>;

  @Input()
  get queryProfileData$(): Observable<QueryProfileData> {
    return this._queryProfileData$;
  }

  set queryProfileData$(value: Observable<QueryProfileData>) {
    if (this._queryProfileData$ === value) {
      return;
    }
    this._queryProfileData$ = value;
    this.remoteCalls$ = value.pipe(map(queryProfileData => queryProfileData.remoteCalls));
    this.operationStats$ = value.pipe(map(queryProfileData => queryProfileData.operationStats));
    this.querySankeyChartRows$ = value.pipe(map(queryProfileData => queryProfileData.queryLineageData));
  }


  selectedOperation: RemoteCall;
  selectedOperationResult$: Observable<string>;
  displayMode: CallExplorerDisplayMode = 'lineage';

  getOperationName(remoteCall: RemoteCall): string {
    const serviceName = remoteCall.service.split('.').pop();
    return `${serviceName}/${remoteCall.operation}`;
  }
  getPathOnly(address: string) {
    // Hack - there's proabably a better way
    const parts: string[] = address.split('/');
    return '/' + parts.slice(3).join('/');
  }

  selectOperation(operation: RemoteCall) {
    this.selectedOperation = operation;
    this.selectedOperationResult$ = this.queryService.getRemoteCallResponse(operation.remoteCallId);
  }

  statusTextClassForRemoteCall(remoteCall: RemoteCall): string {
    return statusTextClass(remoteCall.resultCode);
  }
}

export type CallExplorerDisplayMode = 'sequence' | 'stats' | 'lineage';

export function statusTextClass(resultCode: number): string {
  const codeStart = resultCode.toString().substr(0, 1);
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
