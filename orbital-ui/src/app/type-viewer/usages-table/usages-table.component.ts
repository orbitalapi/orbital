import {Component, Input, OnInit} from '@angular/core';
import {OperationQueryResult, OperationQueryResultItem} from '../../services/types.service';

@Component({
  selector: 'app-usages-table',
  template: `
    <div class="column">
      <h4>Consumed by</h4>
      <div *ngFor="let input of typeAsInput">
        <app-operation-badge *ngIf="input.operationName" [qualifiedName]="input.operationName"></app-operation-badge>
        <span *ngIf="!input.operationName" class="mono-badge">
          <a [routerLink]="['/services',input.serviceName.fullyQualifiedName]">
          {{input.serviceName.shortDisplayName }}
            </a>
        </span>
      </div>

      <span class="subtle" *ngIf="!typeAsInput || typeAsInput.length === 0">Nothing</span>
    </div>
    <div class="column">
      <h4>Published by</h4>
      <app-operation-badge *ngFor="let output of typeAsOutput"
                           [qualifiedName]="output.operationName"></app-operation-badge>
      <span class="subtle" *ngIf="!typeAsOutput || typeAsOutput.length === 0">Nothing</span>
    </div>
  `,
  styleUrls: ['./usages-table.component.scss']
})
export class UsagesTableComponent {
  private _typeUsages: OperationQueryResult;

  typeAsInput: OperationQueryResultItem[];
  typeAsOutput: OperationQueryResultItem[];

  @Input()
  get typeUsages(): OperationQueryResult {
    return this._typeUsages;
  }

  set typeUsages(value: OperationQueryResult) {
    if (this._typeUsages === value) {
      return;
    }
    this._typeUsages = value;
    if (this.typeUsages) {
      this.typeAsInput = this.typeUsages.results.filter(r => r.role === 'Input');
      this.typeAsOutput = this.typeUsages.results.filter(r => r.role === 'Output');
    }
  }


}
