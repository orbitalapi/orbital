import {Component, EventEmitter, Input, Output} from '@angular/core';
import { Router } from '@angular/router';
import {
  getDisplayName,
  InstanceLike,
  NamedAndDocumented,
  Operation,
  Parameter,
  QualifiedName,
  Schema,
  Type
} from '../services/schema';
import {Fact} from '../services/query.service';
import {HttpErrorResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {BaseDeferredEditComponent} from '../type-viewer/base-deferred-edit.component';
import {MatLegacyDialog as MatDialog} from '@angular/material/legacy-dialog';
import {openTypeSearch} from '../type-viewer/model-attribute-tree-list/base-schema-member-display';
import {isNullOrUndefined} from '../utils/utils';
import {OperationSummary, toOperationSummary} from 'src/app/service-view/operation-summary';
import {methodClassFromName} from 'src/app/service-view/service-view-class-utils';
import {
  ChangeOperationParameterTypeEvent, ChangeOperationReturnTypeEvent,
  EditMemberDescriptionEvent,
  SchemaEditOperation
} from '../project-import/schema-importer.service';

@Component({
  selector: 'app-operation-view',
  template: `
    <div class="documentation" *ngIf="operation">
      <div class="page-heading">
        <h1>{{ operation.name }}<span class="badge service">{{ operation.operationKind }}</span></h1>
        <div class="badges">
          <span class="mono-badge">
            <ng-container *ngIf="!schemaMemberNavigable">{{ operationSummary?.serviceName }}</ng-container>
            <a *ngIf="schemaMemberNavigable" [routerLink]="['/services',operationSummary?.serviceName]">{{ operationSummary?.serviceName }}</a>
          </span>
          <span class="separator-slash">/</span>
          <span class="mono-badge">{{ operationSummary?.name }}</span>
        </div>
      </div>
      <section>
        <h4>Url</h4>
        <div class="http-box" [ngClass]="getMethodClass(operationSummary.method)" *ngIf="operationSummary.url">
          <span class="http-method"
                [ngClass]="getMethodClass(operationSummary.method)">{{ operationSummary.method }}</span>
          <span class="url">{{ operationSummary.url }}</span>
        </div>
        <p class="subtle actionable" *ngIf="!operationSummary.url">No url provided</p>
      </section>
      <section>
        <app-description-editor-container [type]="operation"
                                          commitMode="explicit"
                                          [editable]="editable"
                                          (updateDeferred)="onDescriptionChanged($event)"
        ></app-description-editor-container>
      </section>
      <section>
        <h4>Returns</h4>
        <div class="returns-container">
          <span
            [class.type-name-container]="editable"
            (click)="selectReturnType()"
            [attr.title]="editable ? 'Click to edit return type' : null"
          >
          <span class="mono-badge">
            <ng-container *ngIf="editable">{{ displayName(operation.returnTypeName, showFullTypeNames) }}</ng-container>
            <a *ngIf="!editable">{{ displayName(operation.returnTypeName, showFullTypeNames) }}</a>
            <!--// TODO: ask Marty whether we should be showing the same info as we do in the Models view?-->
            <!--<span class="scalar-base-type" *ngIf="treeNode.type.isScalar">
              {{ '(' + (treeNode.type.basePrimitiveTypeName?.shortDisplayName || displayName(treeNode.type.aliasForType, showFullTypeNames)) + ')' }}
            </span>-->
          </span>
          <img *ngIf="editable" src='assets/img/tabler/pencil.svg'>
          </span>
        </div>
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
              <td class="param-name">{{ param.name }}</td>
              <td>
                <div class="returns-container">
                  <span
                    [class.type-name-container]="editable"
                    (click)="selectParameterType(param)"
                    [attr.title]="editable ? 'Click to edit ' + param.name + ' return type' : null"
                  >
                  <span class="mono-badge">
                    <ng-container *ngIf="editable">{{ displayName(param.typeName, showFullTypeNames) }}</ng-container>
                    <ng-container *ngIf="!editable && !schemaMemberNavigable">{{ displayName(param.typeName, showFullTypeNames) }}</ng-container>
                    <a *ngIf="!editable && schemaMemberNavigable">{{ displayName(param.typeName, showFullTypeNames) }}</a>
                    <!--// TODO: ask Marty whether we should be showing the same info as we do in the Models view?-->
                    <!--<span class="scalar-base-type" *ngIf="treeNode.type.isScalar">
                      {{ '(' + (treeNode.type.basePrimitiveTypeName?.shortDisplayName || displayName(treeNode.type.aliasForType, showFullTypeNames)) + ')' }}
                    </span>-->
                  </span>
                  <img *ngIf="editable" src='assets/img/tabler/pencil.svg'>
                  </span>
                </div>
              </td>
              <td>
                <app-description-editor-container
                  [type]="param"
                  [editable]="editable"
                  [showHeader]="false"
                  commitMode="explicit"
                  (updateDeferred)="onDescriptionChanged($event, param.name)"
                ></app-description-editor-container>
              </td>
              <td *ngIf="tryMode">
                <input (change)="updateModel(param, $event)">
              </td>
          </table>
          <p class="subtle" *ngIf="!operation?.parameters || operation?.parameters?.length === 0">No parameters
            required</p>
        </div>
        <div class="button-row" *ngIf="allowTryItOut">
          <button tuiButton size="m" appearance="outline" (click)="tryMode = true" *ngIf="!tryMode"
                  [disabled]="!operationSummary.url">Try it out
          </button>
          <button tuiButton size="m" appearance="outline" (click)="onCancel()" *ngIf="tryMode">Cancel</button>
          <div class="spacer"></div>
          <button tuiButton size="m" appearance="secondary" *ngIf="tryMode" (click)="doSubmit()">Submit</button>
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


  constructor(private dialog: MatDialog, private router: Router) {
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

  @Output()
  operationUpdated: EventEmitter<{schemaEditOperation: SchemaEditOperation, member: Operation}> = new EventEmitter();

  @Input()
  editable: boolean = false;

  @Input()
  schemaMemberNavigable: boolean;

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
    if (!this.editable) {
      this.router.navigate(['/catalog', this.navigationTargetForType(this.operation.returnTypeName)])
    } else {
      const dialog = openTypeSearch(this.dialog);
      dialog.afterClosed().subscribe((event) => {
        if (!isNullOrUndefined(event)) {
          const changeEvent: ChangeOperationReturnTypeEvent = {
            editKind: 'ChangeOperationReturnType',
            symbol: this.operation.qualifiedName,
            newReturnType: event.type.name
          }
          this.operation.returnTypeName = event.type.name;
          this.emitUpdateIfRequired(changeEvent);
          if (event.source === 'new') {
            this.newTypeCreated.next(event.type);
          }
        }
      })
    }
  }

  selectParameterType(param: Parameter) {
    if (!this.editable) {
      this.router.navigate(['/catalog', param.typeName.fullyQualifiedName])
    } else {
    const dialog = openTypeSearch(this.dialog);
    dialog.afterClosed().subscribe((event) => {
      if (!isNullOrUndefined(event)) {
        const changeEvent: ChangeOperationParameterTypeEvent = {
          editKind: 'ChangeOperationParameterType',
          symbol: this.operation.qualifiedName,
          parameterName: param.name,
          newType: event.type.name
        }
        param.typeName = event.type.name;
        this.emitUpdateIfRequired(changeEvent);
        if (event.source === 'new') {
          this.newTypeCreated.next(event.type);
        }
      }
    })
      }
  }

  onDescriptionChanged($event: NamedAndDocumented, memberName?: string) {
    const event: EditMemberDescriptionEvent = {
      editKind: 'EditMemberDescription',
      symbol: this.type.qualifiedName,
      memberKind: 'OPERATION',
      memberName,
      typeDoc: $event.typeDoc,
    }
    this.updateDeferred.emit({schemaEditOperation: event, member: this.type})
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
