import { Component, Input, OnInit } from '@angular/core';
import { QualifiedName } from '../services/schema';
import { ParameterDiff } from './changelog.service';

@Component({
  selector: 'app-input-params-change',
  template: `
    <div class="col">
      <h4>Old Values</h4>
      <div  *ngFor="let param of oldValue">
        <span class="param-name" *ngIf="param.name"> {{ param.name }} : </span>
        <span class="mono-badge">{{ param.type.shortDisplayName }}</span>
      </div>
      <div class="mono-badge" *ngIf="oldValue?.length === 0">No inputs</div>
    </div>
    <div class="col">
      <h4>New Values</h4>
      <div  *ngFor="let param of newValue">
        <span class="param-name" *ngIf="param.name"> {{ param.name }} : </span>
        <span class="mono-badge">{{ param.type.shortDisplayName }}</span>
      </div>
      <div class="mono-badge" *ngIf="newValue?.length === 0">No inputs</div>
    </div>

  `,
  styleUrls: ['./input-params-change.component.scss']
})
export class InputParamsChangeComponent {

  @Input()
  oldValue: ParameterDiff[];

  @Input()
  newValue: ParameterDiff[];
}
