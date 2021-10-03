import {Component, Input} from '@angular/core';
import {OperationName, splitOperationQualifiedName} from '../service-view/service-view.component';
import {QualifiedName} from '../services/schema';

@Component({
  selector: 'app-operation-badge',
  template: `
    <div class="badges">
            <span class="mono-badge" [ngClass]="{'small' : size === 'small'}">
              <a [routerLink]="['/services',operationName?.serviceName]">{{operationName?.serviceDisplayName}}</a>
            </span>
      <span class="separator-slash" *ngIf="operationName?.operationName">/</span>
      <span class="mono-badge" [ngClass]="{'small' : size === 'small'}"
            *ngIf="operationName?.operationName">
        <a
          [routerLink]="['/services',operationName?.serviceName, operationName?.operationName]">{{operationName?.operationName}}</a>
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

  operationName: OperationName;

}
