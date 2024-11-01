import {Component, HostBinding, Input} from '@angular/core';
import {TuiAppearanceOptions, TuiSizeL} from '@taiga-ui/core';

@Component({
  selector: 'app-statistic',
  template: `
    <div class="stat-label">
      <span>{{ label }}</span>
    </div>
    <div class="stat-value">
      <tui-badge size="s" *ngIf="status" [appearance]="status"></tui-badge>
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
  status:  TuiAppearanceOptions["appearance"] | null = null;

  @Input()
  size: TuiSizeL = 'm'

  @HostBinding('class')
  get sizeClass() {
    return `size-${this.size}`;
  }

}
