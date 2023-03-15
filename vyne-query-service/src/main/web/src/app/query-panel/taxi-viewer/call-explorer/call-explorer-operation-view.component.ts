import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import * as HttpStatus from 'http-status-codes';
import { Observable } from 'rxjs';
import { HttpExchange, RemoteCallResponse } from 'src/app/services/query.service';
import { HttpErrorResponse } from '@angular/common/http';
import { statusTextClass } from './call-explorer.component';
import { OperationName, splitOperationQualifiedName } from 'src/app/services/schema';

@Component({
  selector: 'app-call-explorer-operation-view',
  template: `
    <div class="summary-container">
      <div class="header-bar">
        <h3>{{operation.displayName}}</h3>
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
        <span class="duration pill">Response size {{ responseSize | fileSize }}</span>
      </div>
    </div>
    <app-error-message *ngIf="errorMessage" [message]="errorMessage"></app-error-message>

    <as-split direction="vertical" unit="percent" class="request-response-container flex-split">
      <as-split-area *ngIf="operation.exchange.requestBody" size="50">
        <div class="panel">
          <app-json-viewer [json]="operation.exchange.requestBody" title="Request"></app-json-viewer>
        </div>
      </as-split-area>
      <as-split-area *ngIf="operationResponseContent" size="50">
        <div class="panel">

          <app-json-viewer [json]="operationResponseContent" title="Response"></app-json-viewer>
        </div>

      </as-split-area>
    </as-split>
  `,
  styleUrls: ['./call-explorer-operation-view.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CallExplorerOperationViewComponent {
  constructor(private changeRef: ChangeDetectorRef) {
  }
  operationName: OperationName;

  private _operation: RemoteCallResponse;

  @Input()
  get operation(): RemoteCallResponse {
    return this._operation;
  }

  set operation(value: RemoteCallResponse) {
    if (this.operation === value) {
      return;
    }
    this._operation = value;
    this.operationName = splitOperationQualifiedName(value.operation.fullyQualifiedName);
  }

  get responseSize(): number {
    return (this.operation?.exchange as HttpExchange)?.responseSize
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
        this.changeRef.markForCheck();
      },
      (errorResponse: HttpErrorResponse) => {
        this.errorMessage = errorResponse.error.message;
        this.changeRef.markForCheck();
      });
  }


  @Output()
  close = new EventEmitter<void>();


  errorMessage: string;

  get statusText(): string {
    const resultCode = parseInt(this.operation.resultCode)
    if (!isNaN(resultCode)) {
      return HttpStatus.getStatusText(resultCode);
    } else {
      return this.operation.resultCode;
    }

  }

  get statusTextClass(): string {
    return statusTextClass(this.operation.resultCode)
  }


  closeClicked() {
    this.close.emit();
  }
}
