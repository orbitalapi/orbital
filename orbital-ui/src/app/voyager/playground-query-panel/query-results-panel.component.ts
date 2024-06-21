import {Component, EventEmitter, Input, Output} from '@angular/core';
import {JsonViewerModule} from "../../json-viewer/json-viewer.module";
import {NgIf} from "@angular/common";
import {ExpandablePanelComponent} from "../../expanding-panelset/expandable-panel/expandable-panel.component";

@Component({
  selector: 'app-query-results-panel',
  standalone: true,
  imports: [
    JsonViewerModule,
    NgIf,
    ExpandablePanelComponent
  ],
  template: `
    <app-expandable-panel title="Query results" [isSecondary]="true" [expanded]="expanded"
                          (expandedChange)="setExpanded($event)">
      <app-json-viewer [readOnly]="true" [json]="queryResult" [showHeader]="false"
                       *ngIf="queryResult"></app-json-viewer>
      <div *ngIf="!queryResult" class="empty-results">
        No results to show
      </div>
    </app-expandable-panel>
  `,
  styleUrl: './query-results-panel.component.scss'
})
export class QueryResultsPanelComponent {

  @Input()
  expanded: boolean = false;

  @Output()
  expandedChange = new EventEmitter<boolean>()

  setExpanded(expanded: boolean) {
    this.expanded = expanded;
    this.expandedChange.emit(expanded)
  }

  @Input()
  queryResult: string
}
