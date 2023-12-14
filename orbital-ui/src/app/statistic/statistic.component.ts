import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-statistic',
  template: `
    <div class="stat-label">
      <span>{{ label }}</span>
    </div>
    <div class="stat-value">
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
}
