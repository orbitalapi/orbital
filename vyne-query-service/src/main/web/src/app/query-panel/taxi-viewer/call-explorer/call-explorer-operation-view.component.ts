import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import * as HttpStatus from 'http-status-codes';
import {Observable} from 'rxjs/index';
import {RemoteCall} from '../../../services/query.service';
import {HttpErrorResponse} from '@angular/common/http';
import {OperationName, splitOperationQualifiedName} from '../../../service-view/service-view.component';
import {statusTextClass} from './call-explorer.component';

@Component({
  selector: 'app-call-explorer-operation-view',
  template: `
    <div class="operation-view">
      <div class="summary-container">
        <div class="header-bar">
          <h3>{{operationName?.operationName}}</h3>
          <button class="close-button" mat-icon-button (click)="closeClicked()">
            <mat-icon>close</mat-icon>
          </button>
        </div>

        <div class="badges row">
            <span class="mono-badge">
              <a [routerLink]="['/services',operationName?.serviceName]">{{operationName?.serviceName}}</a>
            </span>
          <span class="separator-slash">/</span>
          <span class="mono-badge">
              <a
                [routerLink]="['/services',operationName?.serviceName, operationName?.operationName]">{{operationName?.operationName}}</a>
            </span>
        </div>

        <div class="address  row">
          <span class="verb pill">{{ operation.method }}</span>
          <span class="address">{{ operation.address }}</span>
        </div>
        <div class="response-code-line row">
          <span class="result pill" [ngClass]="statusTextClass">{{statusText}} {{ operation.resultCode }}</span>
          <span class="duration pill">Time {{ operation.durationMs }}ms</span>
        </div>
      </div>
      <app-error-message *ngIf="errorMessage" [message]="errorMessage"></app-error-message>
      <app-simple-code-viewer lang="json" *ngIf="operation.requestBody" [content]="operation.requestBody"
                              name="Request"></app-simple-code-viewer>
      <app-simple-code-viewer lang="json" *ngIf="operationResponseContent" [content]="operationResponseContent"
                              name="Response"></app-simple-code-viewer>
    </div>
  `,
  styleUrls: ['./call-explorer-operation-view.component.scss']
})
export class CallExplorerOperationViewComponent {
  private _operation: RemoteCall;
  operationName: OperationName;

  @Input()
  get operation(): RemoteCall {
    return this._operation;
  }

  set operation(value: RemoteCall) {
    if (this.operation === value) {
      return;
    }
    this._operation = value;
    this.operationName = splitOperationQualifiedName(value.operationQualifiedName);
  }

  private _operationResponse$: Observable<string>;

  operationResponseContent: string;

  @Input()
  get operationResponse$(): Observable<string> {
    return this._operationResponse$;
  }

  set operationResponse$(value: Observable<string>) {
    if (this._operationResponse$ === value) {
      return;
    }
    this._operationResponse$ = value;
    this.operationResponse$.subscribe(next => {
        this.operationResponseContent = next;
      },
      (errorResponse: HttpErrorResponse) => {
        this.errorMessage = errorResponse.error.message;
      });
  }


  @Output()
  close = new EventEmitter<void>();


  errorMessage: string;

  get statusText(): string {
    return HttpStatus.getStatusText(this._operation.resultCode);
  }

  get statusTextClass(): string {
    return statusTextClass(this.operation.resultCode)
  }


  closeClicked() {
    this.close.emit();
  }
}
