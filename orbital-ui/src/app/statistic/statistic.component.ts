import {Component, HostBinding, Input, OnInit} from '@angular/core';
import {TuiStatus} from "@taiga-ui/kit/types";
import {TuiSizeL} from "@taiga-ui/core";

@Component({
  selector: 'app-statistic',
  template: `
    <div class="stat-label">
      <span>{{ label }}</span>
    </div>
    <div class="stat-value">
      <tui-badge size="xs" *ngIf="status" [status]="status" [value]="" ></tui-badge>
      <span>{{ value }}</span>
    </div>
  `,
  styleUrls: ['./statistic.component.scss']
})
export class StatisticComponent {

  @Input()
  label: string;

  @Input()
  value: string;

  @Input()
  status: TuiStatus | null = null;

  @Input()
  size: TuiSizeL = 'm'

  @HostBinding('class')
  get sizeClass() {
    return `size-${this.size}`;
  }

}
