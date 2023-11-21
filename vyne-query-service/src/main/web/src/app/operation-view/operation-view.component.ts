import {Component, EventEmitter, Input, Output} from '@angular/core';
import {getDisplayName, InstanceLike, Operation, Parameter, QualifiedName, Schema, Type} from '../services/schema';
import {Fact} from '../services/query.service';
import {HttpErrorResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {BaseDeferredEditComponent} from '../type-viewer/base-deferred-edit.component';
import {MatLegacyDialog as MatDialog} from '@angular/material/legacy-dialog';
import {openTypeSearch} from '../type-viewer/model-attribute-tree-list/base-schema-member-display';
import {isNullOrUndefined} from 'util';
import {OperationSummary, toOperationSummary} from 'src/app/service-view/operation-summary';
import {methodClassFromName} from 'src/app/service-view/service-view-class-utils';

@Component({
  selector: 'app-operation-view',
  template: `
    <div class="documentation" *ngIf="operation">
      <div class="page-heading">
        <h1>{{operation.name}}</h1>
        <div class="badges">
            <span class="mono-badge">
              <a [routerLink]="['/services',operationSummary?.serviceName]">{{operationSummary?.serviceName}}</a>
            </span>
          <span class="separator-slash">/</span>
          <span class="mono-badge">{{operationSummary?.name}}</span>
        </div>

      </div>
      <section>
        <h4>Url</h4>
        <div class="http-box" [ngClass]="getMethodClass(operationSummary.method)" *ngIf="operationSummary.url">
          <span class="http-method"
                [ngClass]="getMethodClass(operationSummary.method)">{{ operationSummary.method }}</span>
          <span class="url">{{operationSummary.url}}</span>
        </div>
        <p class="subtle" *ngIf="!operationSummary.url">No url provided</p>
      </section>
      <section>
        <h4>Documentation</h4>
        <app-description-editor-container [type]="operation"
                                          *ngIf="operation?.typeDoc"></app-description-editor-container>
        <p class="subtle" *ngIf="!operation?.typeDoc">No documentation provided</p>
      </section>
      <section>
        <h4>Returns</h4>
        <button tuiLink [pseudo]="true" (click)="selectReturnType()"
                *ngIf="editable">{{ displayName(operation.returnTypeName, showFullTypeNames) }}</button>
        <span class="mono-badge" *ngIf="!editable"><a
          [routerLink]="['/catalog', navigationTargetForType(operation.returnTypeName)]">{{operation.returnTypeName.shortDisplayName}}</a></span>
      </section>


      <section *ngIf="operation">
        <h2>Parameters</h2>
        <div class="row">
          <span>Show full type names</span>
          <tui-toggle
            [(ngModel)]="showFullTypeNames"
            [showIcons]="true"
          ></tui-toggle>
        </div>
        <div>
          <table class="parameter-list" *ngIf="operation.parameters && operation.parameters.length > 0">
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
              <td><span class="mono-badge" *ngIf="!editable">
                  <a
                    [routerLink]="['/catalog',param.typeName.fullyQualifiedName]">{{ displayName(param.typeName, showFullTypeNames) }}</a>
                </span>
                <button *ngIf="editable" tuiLink [pseudo]="true" (click)="selectParameterType(param)"
                >{{ displayName(param.typeName, showFullTypeNames) }}</button>

              </td>
              <td><markdown [data]="param.typeDoc"></markdown></td>
              <td *ngIf="tryMode">
                <input (change)="updateModel(param, $event)">
              </td>
          </table>
          <p class="subtle" *ngIf="!operation?.parameters || operation?.parameters?.length === 0">No parameters
            required</p>
        </div>
        <div class="button-row" *ngIf="allowTryItOut">
          <button tuiButton size="m" appearance="outline" (click)="tryMode = true" *ngIf="!tryMode" [disabled]="!operationSummary.url">Try
            it out
          </button>
          <button tuiButton  size="m" appearance="outline"  (click)="onCancel()" *ngIf="tryMode">Cancel</button>
          <div class="spacer"></div>
          <button tuiButton  size="m" appearance="secondary"  *ngIf="tryMode" (click)="doSubmit()">Submit</button>
        </div>
      </section>

      <mat-spinner *ngIf="loading" [diameter]=40></mat-spinner>
      <app-operation-error [operationError]="operationError" *ngIf="operationError"></app-operation-error>
      <app-object-view-container *ngIf="operationResultType"
                                 [schema]="schema"
                                 [instances$]="instances$"
                                 [type]="operationResultType">
      </app-object-view-container>
    </div>
  `,
  styleUrls: ['./operation-view.component.scss']
})
export class OperationViewComponent extends BaseDeferredEditComponent<Operation> {


  constructor(private dialog: MatDialog) {
    super();
  }

  private _operation: Operation;
  @Input()
  get operation(): Operation {
    return this._operation;
  }

  get type(): Operation {
    return this.operation;
  }

  displayName(name: QualifiedName, showFullTypeNames: boolean): string {
    return getDisplayName(name, showFullTypeNames);
  }

  navigationTargetForType(name: QualifiedName): string {
    // Can't call directly, because function is not accessible via angular template
    return getCatalogType(name);
  }

  @Input()
  loading: boolean;

  @Input()
  schema: Schema;

  @Input()
  instances$: Observable<InstanceLike>;

  showFullTypeNames: boolean = false;

  @Input()
  operationResultType: Type;

  @Input()
  operationError: HttpErrorResponse;

  @Output()
  submit = new EventEmitter<{ [index: string]: Fact }>();

  @Output()
  cancel = new EventEmitter();

  @Input()
  editable: boolean = false;

  @Input()
  allowTryItOut = true;

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
    this.paramInputs[param.name] = new Fact(param.typeName.fullyQualifiedName, ($event.target as any).value);
  }

  doSubmit() {
    console.log(this.paramInputs);
    this.submit.emit(this.paramInputs);
  }

  onCancel() {
    this.tryMode = false;
    this.cancel.emit({});
  }

  selectReturnType() {
    const dialog = openTypeSearch(this.dialog);
    dialog.afterClosed().subscribe((event) => {
      if (!isNullOrUndefined(event)) {
        this.operation.returnTypeName = event.type.name;
        this.emitUpdateIfRequired();
        if (event.source === 'new') {
          this.newTypeCreated.next(event.type);
        }
      }
    })
  }

  selectParameterType(param: Parameter) {
    const dialog = openTypeSearch(this.dialog);
    dialog.afterClosed().subscribe((event) => {
      if (!isNullOrUndefined(event)) {
        param.typeName = event.type.name;
        this.emitUpdateIfRequired();
        if (event.source === 'new') {
          this.newTypeCreated.next(event.type);
        }
      }
    })
  }
}


/**
 * Unpacks array types to return the actual member value
 * @param typeName
 */
export function getCatalogType(typeName: QualifiedName): string {
  if (typeName.parameters && typeName.parameters.length === 1) {
    return getCatalogType(typeName.parameters[0]);
  } else {
    return typeName.fullyQualifiedName;
  }
}
