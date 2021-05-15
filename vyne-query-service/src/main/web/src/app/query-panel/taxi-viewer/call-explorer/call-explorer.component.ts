import {Component, Input} from '@angular/core';
import {QueryProfileData, QueryResult, QueryService, RemoteCall} from '../../../services/query.service';
import {QueryFailure} from '../../query-wizard/query-wizard.component';
import {Observable} from 'rxjs/index';
import {map} from 'rxjs/operators';

@Component({
  selector: 'app-call-explorer',
  template: `
    <div class="container">
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
  `,
  styleUrls: ['./call-explorer.component.scss']
})
export class CallExplorerComponent {

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
  }


  selectedOperation: RemoteCall;
  selectedOperationResult$: Observable<string>;

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
