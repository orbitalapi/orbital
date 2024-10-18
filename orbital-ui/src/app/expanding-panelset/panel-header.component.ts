import {Component, EventEmitter, HostBinding, HostListener, Input, Output} from '@angular/core';

@Component({
  selector: 'app-panel-header',
  template: `
    <button class="icon-button" *ngIf="collapsible">
      <app-svg-icon [class.expanded]="expanded" class="collapsible-icon" tabler="chevron-right" strokeWidth="2"
                    width="16" height="16"></app-svg-icon>
    </button>
    <app-svg-icon *ngIf="tablerIcon" [tabler]="tablerIcon" width="24" height="24" />

    <ng-content select="title-content"></ng-content>
    <h3 *ngIf="title && !isSecondary">{{ title }} <span *ngIf="helpText" class="help-text">{{ helpText }}</span></h3>
    <h4 *ngIf="title && isSecondary">{{ title }} <span *ngIf="helpText" class="help-text">{{ helpText }}</span></h4>
    <ng-content></ng-content>
  `,
  styleUrls: ['./panel-header.component.scss']
})
export class PanelHeaderComponent {

  @Input()
  @HostBinding('class.is-collapsible')
  collapsible: boolean = false;

  @Input()
  tablerIcon: string;

  private _expanded: boolean = false;
  @Input()
  get expanded(): boolean {
    return this._expanded;
  }

  set expanded(value: boolean) {
    this._expanded = value;
    this.expandedChange.emit(this.expanded);
  }

  @Output()
  expandedChange = new EventEmitter<boolean>();

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

  @HostListener('click', ['$event.target'])
  onClick() {
    this.expanded = !this.expanded
  }
 }
