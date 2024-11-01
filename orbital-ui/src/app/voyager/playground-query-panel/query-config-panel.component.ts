import { TuiButton } from "@taiga-ui/core";
import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {StubPanelComponent} from "./stub-panel.component";
import { TuiBadge, TuiTabs } from "@taiga-ui/kit";
import {Schema} from "../../services/schema";
import {OperationStub} from "../../services/query.service";
import {ExpandingPanelSetModule} from "../../expanding-panelset/expanding-panel-set.module";
import {QueryParmsPanelComponent} from "./query-parms-panel.component";
import {ExpandablePanelComponent} from "../../expanding-panelset/expandable-panel/expandable-panel.component";

@Component({
  selector: 'app-query-config-panel',
  standalone: true,
  imports: [CommonModule, StubPanelComponent, TuiTabs, ExpandingPanelSetModule, TuiButton, QueryParmsPanelComponent, TuiBadge, ExpandablePanelComponent],
  template: `
    <app-stub-panel *ngIf="activeTabIndex === 0" [schema]="schema" [stubs]="stubs"
                    (stubsChange)="stubsChange.emit($event)"></app-stub-panel>
    <app-query-params-panel *ngIf="activeTabIndex === 1" [(parameters)]="parameters"></app-query-params-panel>
  `,
  styleUrls: ['./query-config-panel.component.scss'],
})
export class QueryConfigPanelComponent {
  activeTabIndex = 0;

  @Input()
  expanded: boolean = false;

  @Output()
  expandedChange = new EventEmitter<boolean>()

  setExpanded(expanded: boolean) {
    this.expanded = expanded;
    this.expandedChange.emit(expanded)
  }

  @Input()
  schema: Schema

  @Input()
  stubs: OperationStub[];

  @Output()
  stubsChange = new EventEmitter<OperationStub[]>()

  @Input()
  parameters: { [index: string]: any };

  get stubsAndParamsCount() {
    return this.stubsCount + this.paramsCount;
  }

  get stubsCount(): number {
    return this.stubs?.length || 0;
  }

  get paramsCount(): number {
    return this.parameters ? Object.keys(this.parameters).length : 0;
  }


  @Output()
  parameterValuesChange = new EventEmitter<{ [index: string]: any }>()


  toggleExpanded() {
    this.expanded = !this.expanded;
    this.expandedChange.emit(this.expanded);
  }
}
