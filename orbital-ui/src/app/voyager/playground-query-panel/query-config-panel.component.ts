import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {StubPanelComponent} from "./stub-panel.component";
import {TuiBadgeModule, TuiTabsModule} from "@taiga-ui/kit";
import {Schema} from "../../services/schema";
import {OperationStub} from "../../services/query.service";
import {ExpandingPanelSetModule} from "../../expanding-panelset/expanding-panel-set.module";
import {TuiButtonModule} from "@taiga-ui/core";
import {QueryParmsPanelComponent} from "./query-parms-panel.component";
import {ExpandablePanelComponent} from "../../expanding-panelset/expandable-panel/expandable-panel.component";

@Component({
  selector: 'app-query-config-panel',
  standalone: true,
  imports: [CommonModule, StubPanelComponent, TuiTabsModule, ExpandingPanelSetModule, TuiButtonModule, QueryParmsPanelComponent, TuiBadgeModule, ExpandablePanelComponent],
  template: `
<!--    <app-expandable-panel title="Stubs and parameters" [isSecondary]="true" [expanded]="expanded" (expandedChange)="setExpanded($event)">-->
<!--      <ng-container ngProjectAs="header-content">-->
<!--        <tui-badge *ngIf="stubsAndParamsCount > 0" status="info" [value]="stubsAndParamsCount" size="xs"></tui-badge>-->
<!--      </ng-container>-->
<!--      <div class="config-panels">-->
<!--        <div class="row">-->
<!--          <tui-tabs [(activeItemIndex)]="activeTabIndex">-->
<!--            <button tuiTab>STUBS-->
<!--              <tui-badge size="xs" *ngIf="stubsCount > 0" status="info" [value]="stubsCount"></tui-badge>-->
<!--            </button>-->
<!--            <button tuiTab>PARAMETERS-->
<!--              <tui-badge size="xs" *ngIf="paramsCount > 0" status="info" [value]="paramsCount"></tui-badge>-->
<!--            </button>-->
<!--          </tui-tabs>-->
<!--        </div>-->
        <app-stub-panel *ngIf="activeTabIndex === 0" [schema]="schema" [stubs]="stubs"
                        (stubsChange)="stubsChange.emit($event)"></app-stub-panel>
        <app-query-params-panel *ngIf="activeTabIndex === 1" [(parameters)]="parameters"></app-query-params-panel>
<!--      </div>-->
<!--    </app-expandable-panel>-->
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
