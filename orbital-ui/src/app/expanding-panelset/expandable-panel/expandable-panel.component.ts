import {Component, EventEmitter, HostBinding, Input, Output} from '@angular/core';
import {ExpandingPanelSetModule} from "../expanding-panel-set.module";
import {NgIf} from "@angular/common";
import {TuiBadgeModule} from "@taiga-ui/kit";
import {TuiButtonModule} from "@taiga-ui/core";

@Component({
  selector: 'app-expandable-panel',
  standalone: true,
  imports: [
    ExpandingPanelSetModule,
    NgIf,
    TuiBadgeModule,
    TuiButtonModule
  ],
  template: `
    <app-panel-header [title]="title" [isSecondary]="isSecondary">
      <ng-content select="header-content"></ng-content>
      <span class="spacer"></span>
      <button tuiButton [icon]="expanded ? 'tuiIconMinimize2' : 'tuiIconMaximize2'" size="s" appearance="icon" (click)="toggleExpanded()"></button>
    </app-panel-header>
    <ng-content *ngIf="expanded"></ng-content>
  `,
  styleUrl: './expandable-panel.component.scss'
})
export class ExpandablePanelComponent {

  @Input()
  expanded: boolean = false;

  @Output()
  expandedChange = new EventEmitter<boolean>()

  @Input()
  title: string;

  @Input()
  icon: string

  @Input()
  isSecondary: boolean

  toggleExpanded() {
    this.expanded = !this.expanded;
    this.expandedChange.emit(this.expanded);
  }
}
