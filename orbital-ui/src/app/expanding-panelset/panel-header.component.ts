import { Component, HostBinding, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-panel-header',
  template: `
    <span class="caption-small" *ngIf="title">{{title}}</span>
    <ng-content></ng-content>
  `,
  styleUrls: ['./panel-header.component.scss']
})
export class PanelHeaderComponent {

  @Input()
  title: string;
  // Prevents tooltip displaying in browser
  @HostBinding('attr.title') get getTitle(): null {
    return null;
  }

  @Input()
  icon: string
}
