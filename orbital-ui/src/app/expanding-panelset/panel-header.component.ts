import { Component, HostBinding, Input } from '@angular/core';

@Component({
  selector: 'app-panel-header',
  template: `
    <ng-content select="title-content"></ng-content>
    <h3 *ngIf="title && !isSecondary">{{title}} <span *ngIf="helpText" class="help-text">{{helpText}}</span></h3>
    <h4 *ngIf="title && isSecondary">{{title}} <span *ngIf="helpText" class="help-text">{{helpText}}</span></h4>
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
  helpText: string

  @Input()
  icon: string

  @Input()
  @HostBinding('class.is-secondary')
  isSecondary: boolean
 }
