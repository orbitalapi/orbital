import {Component, Input, OnInit} from '@angular/core';
import {QualifiedName} from '../../services/schema';
import {OperationName, OperationSummary, splitOperationQualifiedName} from '../../service-view/service-view.component';

@Component({
  selector: 'app-operation-badge',
  template: `
    <div class="badges">
            <span class="mono-badge small">
              <a [routerLink]="['/services',operationName?.serviceName]">{{operationName?.serviceDisplayName}}</a>
            </span>
      <span class="separator-slash" *ngIf="operationName?.operationName">/</span>
      <span class="mono-badge small" *ngIf="operationName?.operationName">{{ operationName.operationName}}</span>
    </div>
  `,
  styleUrls: ['./operation-badge.component.scss']
})
export class OperationBadgeComponent {
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
