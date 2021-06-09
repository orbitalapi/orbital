import {Component, Input} from '@angular/core';
import {
  QueryProfileData,
  QueryResult,
  QueryService,
  RemoteCall,
  RemoteOperationPerformanceStats
} from '../../../services/query.service';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';

@Component({
  selector: 'app-call-explorer',
  template: `
    <div class="toolbar">
      <mat-button-toggle-group [(ngModel)]="displayMode">
        <mat-button-toggle value="sequence">
          <img class="icon" src="assets/img/sequence.svg">
        </mat-button-toggle>
        <mat-button-toggle value="stats" [disabled]="">
          <img class="icon" src="assets/img/table-view.svg">
        </mat-button-toggle>
      </mat-button-toggle-group>
    </div>
    <div class="sequence-diagram-container" *ngIf="displayMode === 'sequence'">
      <div class="operation-list">
        <div class="header">
          <div class="table-header">Calls</div>
          <div class="table-subheader">(Click to explore)</div>
        </div>
        <div class="operation" *ngFor="let operation of remoteCalls$ | async" (click)="selectOperation(operation)">
          <div class="verb">{{ operation.method }}</div>
          <div class="address" [matTooltip]="getPathOnly(operation.address)">{{ getPathOnly(operation.address) }}</div>
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
  `,
  styleUrls: ['./call-explorer.component.scss']
})
export class CallExplorerComponent {
  operationStats$: Observable<RemoteOperationPerformanceStats[]>;

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
  }


  selectedOperation: RemoteCall;
  selectedOperationResult$: Observable<string>;
  displayMode: CallExplorerDisplayMode = 'sequence';

  getPathOnly(address: string) {
    // Hack - there's proabably a better way
    const parts: string[] = address.split('/');
    return '/' + parts.slice(3).join('/');
  }

  selectOperation(operation: RemoteCall) {
    this.selectedOperation = operation;
    this.selectedOperationResult$ = this.queryService.getRemoteCallResponse(operation.remoteCallId);
  }
}

export type CallExplorerDisplayMode = 'sequence' | 'stats';
