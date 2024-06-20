import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ExpandablePanelComponent} from "../../expanding-panelset/expandable-panel/expandable-panel.component";
import {QueryPlan} from "../../services/query.service";
import {LineageDisplayModule} from "../../lineage-display/lineage-display.module";

@Component({
  selector: 'app-query-plan-panel',
  standalone: true,
  imports: [
    ExpandablePanelComponent,
    LineageDisplayModule
  ],
  template: `
    <app-query-lineage [rows]="queryPlan?.steps"></app-query-lineage>
  `,
  styleUrl: './query-plan-panel.component.scss'
})
export class QueryPlanPanelComponent {

  @Input()
  expanded: boolean = false;

  @Output()
  expandedChange = new EventEmitter<boolean>()

  setExpanded(expanded: boolean) {
    this.expanded = expanded;
    this.expandedChange.emit(expanded)
  }

  @Input()
  queryPlan: QueryPlan
}
