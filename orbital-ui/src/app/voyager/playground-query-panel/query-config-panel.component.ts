import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {StubPanelComponent} from "./stub-panel.component";
import {TuiBadgeModule, TuiTabsModule} from "@taiga-ui/kit";
import {Schema} from "../../services/schema";
import {OperationStub} from "../../services/query.service";
import {ExpandingPanelSetModule} from "../../expanding-panelset/expanding-panel-set.module";
import {TuiButtonModule} from "@taiga-ui/core";
import {QueryParmsPanelComponent} from "./query-parms-panel.component";

@Component({
  selector: 'app-query-config-panel',
  standalone: true,
  imports: [CommonModule, StubPanelComponent, TuiTabsModule, ExpandingPanelSetModule, TuiButtonModule, QueryParmsPanelComponent, TuiBadgeModule],
  template: `
    <app-panel-header *ngIf="!expanded" title="Stubs and Parameters">
      <tui-badge *ngIf="stubsAndParamsCount > 0" status="info" [value]="stubsAndParamsCount"  size="xs" ></tui-badge>
      <span class="spacer"></span>
      <button tuiButton icon="tuiIconMaximize2" size="s" appearance="icon" (click)="toggleExpanded()"></button>
    </app-panel-header>
    <div class="config-panels" *ngIf="expanded">
      <div class="row">
        <tui-tabs [(activeItemIndex)]="activeTabIndex">
          <button tuiTab>STUBS <tui-badge size="xs" *ngIf="stubsCount > 0" status="info" [value]="stubsCount"></tui-badge></button>
          <button tuiTab>PARAMETERS <tui-badge size="xs" *ngIf="paramsCount > 0" status="info" [value]="paramsCount"></tui-badge></button>
        </tui-tabs>
        <span class="spacer"></span>
        <button tuiButton icon="tuiIconMinimize2" size="s" appearance="icon" (click)="toggleExpanded()"></button>
      </div>
      <app-stub-panel *ngIf="activeTabIndex === 0" [schema]="schema" [stubs]="stubs" (stubsChange)="stubsChange.emit($event)"></app-stub-panel>
      <app-query-params-panel *ngIf="activeTabIndex === 1" [(parameters)]="parameters"></app-query-params-panel>
    </div>
  `,
  styleUrls: ['./query-config-panel.component.scss'],
})
export class QueryConfigPanelComponent {
  activeTabIndex = 0;

  @Input()
  expanded: boolean = false;

  @Output()
  expandedChange = new EventEmitter<boolean>()

  @Input()
  schema: Schema

  @Input()
  stubs: OperationStub[];

  @Output()
  stubsChange = new EventEmitter<OperationStub[]>()

  @Input()
  parameters:  { [index: string]: any };

  get stubsAndParamsCount() {
    return this.stubsCount + this.paramsCount;
  }

  get stubsCount(): number {
    return this.stubs?.length || 0;
  }
  get paramsCount(): number {
    return  this.parameters ? Object.keys(this.parameters).length : 0;
  }


  @Output()
  parameterValuesChange = new EventEmitter< { [index: string]: any } >()


  toggleExpanded() {
    this.expanded = !this.expanded;
    this.expandedChange.emit(this.expanded);
  }
}
