import {Component, ContentChildren, EventEmitter, HostBinding, Input, OnInit, Output, QueryList} from '@angular/core';
import {PanelComponent} from "./panel.component";
import {active} from "d3-transition";

@Component({
  selector: 'app-panelset',
  template: `
    <div class="gutter" [ngClass]="{'gutter-border': !activePanel}">
      <div *ngFor="let panel of panels">
        <button class="panel-icon" (click)="setActivePanel(panel)" [ngClass]="{ active: panel === activePanel}">
          <img [src]="panel.icon">
        </button>
      </div>
    </div>
    <div class="body" *ngIf="activePanel">
      <app-panel-header [title]="activePanel.title"></app-panel-header>
      <ng-template [cdkPortalOutlet]="activePanel.contentPortal"></ng-template>
    </div>
  `,
  styleUrls: ['./panelset.component.scss']
})
export class PanelsetComponent {

  @HostBinding('class.reverse-gutter') get className() { return this.gutterSide === "right" }

  @ContentChildren(PanelComponent) panels: QueryList<PanelComponent>

  @Output()
  activePanelChange = new EventEmitter<PanelComponent>();

  @Input()
  activePanel: PanelComponent

  @Input()
  gutterSide: 'left' | 'right' = "left";

  setActivePanel(selectedPanel: PanelComponent) {
    let newActivePanel: PanelComponent | null;
    if (this.activePanel === selectedPanel) {
      newActivePanel = null;
    } else {
      newActivePanel = selectedPanel;
    }
    this.activePanel = newActivePanel;
    this.activePanelChange.emit(newActivePanel)
  }

  protected readonly active = active;
}

