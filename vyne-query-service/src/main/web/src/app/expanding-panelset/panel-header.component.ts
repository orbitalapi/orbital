import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-panel-header',
  template: `
    <span class="caption-small">{{title}}</span>
    <ng-content></ng-content>
  `,
  styleUrls: ['./panel-header.component.scss']
})
export class PanelHeaderComponent {

  @Input()
  title: string;

}
