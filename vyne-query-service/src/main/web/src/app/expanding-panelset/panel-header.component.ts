import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-panel-header',
  template: `
    <span class="caption-small">{{title}}</span>
    <div class="spacer" *ngIf="alignItems === 'right'"></div>
    <ng-content></ng-content>
  `,
  styleUrls: ['./panel-header.component.scss']
})
export class PanelHeaderComponent {

  @Input()
  title: string;

  @Input()
  alignItems: 'left' | 'right' = 'right';

}
