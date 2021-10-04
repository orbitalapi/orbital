import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Operation, Parameter, Schema, Type, TypedInstance} from '../services/schema';
import {methodClassFromName, OperationSummary, toOperationSummary} from '../service-view/service-view.component';
import {Fact} from '../services/query.service';
import {HttpErrorResponse} from '@angular/common/http';

@Component({
  selector: 'app-operation-view',
  template: `
    <div class="page-content">
      <div class="documentation" *ngIf="operation">
        <div class="page-heading">
          <h1>{{operation.name}}</h1>
          <div class="badges">
            <span class="mono-badge">
              <a [routerLink]="['/services',operationSummary?.serviceName]">{{operationSummary?.serviceName}}</a>
            </span>
            <span class="separator-slash">/</span>
            <span class="mono-badge">{{operation?.name}}</span>
          </div>

        </div>
        <section>
          <h4>Url</h4>
          <div class="http-box" [ngClass]="getMethodClass(operationSummary.method)">
          <span class="http-method"
                [ngClass]="getMethodClass(operationSummary.method)">{{ operationSummary.method }}</span>
            <span class="url">{{operationSummary.url}}</span>
          </div>
        </section>
        <section>
          <h4>Documentation</h4>
          <app-description-editor-container [type]="operation"></app-description-editor-container>
        </section>
        <section>
          <h4>Returns</h4>
          <span class="mono-badge"><a
            [routerLink]="['/types', operation.returnType.fullyQualifiedName]">{{operation.returnType.shortDisplayName}}</a></span>
        </section>


        <section *ngIf="operation">
          <h2>Parameters</h2>
          <div>
            <table class="parameter-list">
              <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th>Description</th>
                <th *ngIf="tryMode">Value</th>
              </tr>
              </thead>
              <tbody>
              <tr *ngFor="let param of operation.parameters">
                <td>{{ param.name }}</td>
                <td><span class="mono-badge">
                  <a [routerLink]="['/catalog',param.type.fullyQualifiedName]">{{ param.type.shortDisplayName}}</a>
                </span></td>
                <td>-</td>
                <td *ngIf="tryMode">
                  <input (change)="updateModel(param, $event)">
                </td>
            </table>
          </div>
          <div class="button-row">
            <button mat-stroked-button (click)="tryMode = true" *ngIf="!tryMode">Try it out</button>
            <button mat-stroked-button (click)="tryMode = false" *ngIf="tryMode">Cancel</button>
            <div class="spacer"></div>
            <button mat-raised-button color="primary" *ngIf="tryMode" (click)="doSubmit()">Submit</button>
          </div>
        </section>

        <mat-spinner *ngIf="loading" [diameter]=40></mat-spinner>
        <app-operation-error [operationError]="operationError" *ngIf="operationError"></app-operation-error>
        <app-object-view-container *ngIf="operationResult"
                                   [schema]="schema"
                                   [instance]="operationResult"
                                   [type]="operationResultType">
        </app-object-view-container>
      </div>
    </div>
  `,
  styleUrls: ['./operation-view.component.scss']
})
export class OperationViewComponent {
  private _operation: Operation;
  @Input()
  get operation(): Operation {
    return this._operation;
  }

  @Input()
  loading: boolean;

  @Input()
  schema: Schema;

  @Input()
  operationResult: TypedInstance;

  @Input()
  operationResultType: Type;

  @Input()
  operationError: HttpErrorResponse;

  @Output()
  submit = new EventEmitter<{ [index: string]: Fact }>();

  set operation(value: Operation) {
    if (this._operation === value) {
      return;
    }
    this._operation = value;
    this.operationSummary = toOperationSummary(this.operation);
  }

  operationSummary: OperationSummary;

  tryMode = false;

  paramInputs: { [index: string]: Fact } = {};

  getMethodClass(method: string) {
    return methodClassFromName(method);
  }

  updateModel(param: Parameter, $event: Event) {
    this.paramInputs[param.name] = new Fact(param.type.fullyQualifiedName, ($event.target as any).value);
  }

  doSubmit() {
    console.log(this.paramInputs);
    this.submit.emit(this.paramInputs);
  }
}
