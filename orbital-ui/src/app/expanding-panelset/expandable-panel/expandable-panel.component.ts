import { TuiButton } from "@taiga-ui/core";
import { TuiBadge } from "@taiga-ui/kit";
import {Component, EventEmitter, HostBinding, Input, Output} from '@angular/core';
import {ExpandingPanelSetModule} from "../expanding-panel-set.module";
import {NgIf} from "@angular/common";

@Component({
  selector: 'app-expandable-panel',
  standalone: true,
  imports: [
    ExpandingPanelSetModule,
    NgIf,
    TuiBadge,
    TuiButton
  ],
  template: `
    <app-panel-header [title]="title" [isSecondary]="isSecondary">
      <ng-content select="header-content"></ng-content>
      <span class="spacer"></span>
      <button tuiButton [iconStart]="expanded ? '@tui.minimize-2' : '@tui.maximize-2'" size="s" appearance="icon" (click)="toggleExpanded()"></button>
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
