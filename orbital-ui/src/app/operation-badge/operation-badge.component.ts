import {Component, Input} from '@angular/core';
import {QualifiedName, OperationName, splitOperationQualifiedName} from '../services/schema';

@Component({
  selector: 'app-operation-badge',
  template: `
    <div class="badges">
      <span class="mono-badge" [ngClass]="{'small' : size === 'small'}">
        <ng-container *ngIf="!schemaMemberNavigable">{{ operationName?.serviceDisplayName }}</ng-container>
        <a *ngIf="schemaMemberNavigable" [routerLink]="['/services',operationName?.serviceName]">
          {{ operationName?.serviceDisplayName }}
        </a>
      </span>
      <span class="separator-slash" *ngIf="operationName?.operationName">/</span>
      <span class="mono-badge" [ngClass]="{'small' : size === 'small'}" *ngIf="operationName?.operationName">
        <ng-container *ngIf="!schemaMemberNavigable">{{ operationName?.operationName }}</ng-container>
        <a *ngIf="schemaMemberNavigable" [routerLink]="['/services',operationName?.serviceName, operationName?.operationName]">
          {{ operationName?.operationName }}
        </a>
      </span>
    </div>
  `,
  styleUrls: ['./operation-badge.component.scss']
})
export class OperationBadgeComponent {

  @Input()
  size: 'normal' | 'small' = 'normal';

  private _qualifiedName: QualifiedName;
  @Input()
  get qualifiedName(): QualifiedName {
    return this._qualifiedName;
  }

  set qualifiedName(value: QualifiedName) {
    if (value === this.qualifiedName) {
      return;
    }
    this._qualifiedName = value;
    if (this.qualifiedName) {
      this.operationName = splitOperationQualifiedName(value.fullyQualifiedName);
    }
  }

  @Input()
  schemaMemberNavigable: boolean = true;

  operationName: OperationName;

}
