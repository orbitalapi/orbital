import { Component, HostBinding, Input } from '@angular/core';

@Component({
  selector: 'app-panel-header',
  template: `
    <ng-content select="title-content"></ng-content>
    <h3 *ngIf="title && !isSecondary">{{title}}</h3>
    <h4 *ngIf="title && isSecondary">{{title}}</h4>
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

  @Input()
  @HostBinding('class.is-secondary')
  isSecondary: boolean
 }
