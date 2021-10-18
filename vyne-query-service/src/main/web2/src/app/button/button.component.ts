import {Component, Input, OnInit} from '@angular/core';
import {IconName, icons} from '../icons/icons';

@Component({
  selector: 'app-button',
  template: `
    <button
      [ngClass]="{'disabled' : !enabled, 'primary': type === 'primary', 'default': type === 'default', 'tertiary' : type === 'tertiary', 'small' : size === 'small' }">
      <img *ngIf="icon" [attr.src]="iconMap[icon]">
      <ng-content></ng-content>
    </button>`,
  styleUrls: ['./button.component.scss']
})
export class ButtonComponent {
  @Input()
  icon: IconName | null = null;

  // To get around binding issues
  iconMap = icons;

  @Input()
  enabled = true;

  @Input()
  size: 'small' | 'default' = 'default';

  @Input()
  type: 'primary' | 'default' | 'tertiary' = 'default';
}
