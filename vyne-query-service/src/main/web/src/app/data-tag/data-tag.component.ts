import {Component, Input, OnInit} from '@angular/core';
import {KeyValueMap} from '../services/schema';

@Component({
  selector: 'app-data-tag',
  styleUrls: ['./data-tag.component.scss'],
  template: `
    <div class="data-tag" [attr.matTooltip]="tooltip">
      <span class="key">{{ key }}</span>
      <div class="single-value" *ngIf="value">
        <span class="separator">:</span>
        <span class="value">{{ value }}</span>
      </div>
      <div class="multi-value" *ngIf="keyValuePairs">
        <div class="key-value-pair" *ngFor="let pair of keyValuePairs | keyvalue">
          <span class="key">{{ pair.key }}</span>
          <span class="separator">:</span>
          <span class="value">{{ pair.value }}</span>
        </div>
      </div>
    </div>
  `
})
export class DataTagComponent {
  @Input()
  key: string;

  @Input()
  value: string;

  @Input()
  keyValuePairs: KeyValueMap;

  @Input()
  tooltip: string;
}
