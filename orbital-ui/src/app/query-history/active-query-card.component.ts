import { Component, EventEmitter, Input, Output } from '@angular/core';
import { isNullOrUndefined } from 'util';
import { Timespan } from '../query-panel/query-editor/counter-timer.component';
import { RunningQueryStatus } from '../services/active-queries-notification-service';

@Component({
  selector: 'app-active-query-card',
  template: `
    <div class="history-item" *ngIf="queryStatus">
      <app-vyneql-record [taxiQlQuery]="queryStatus.taxiQlQuery"></app-vyneql-record>

      <div class="progress-container" *ngIf="progressMode === 'indeterminate'">
        <progress max="100" new size='xs' tuiProgressBar></progress>
      </div>

      <div class="progress-container" *ngIf="progressMode !== 'indeterminate'">
        <progress max="100" new size='xs' tuiProgressBar [value]="progress"></progress>
      </div>

      <div class="record-stats">
        <div class="record-stat">
          <img [src]="progressMode === 'indeterminate' ? 'assets/img/tabler/rss.svg' : 'assets/img/tabler/clock.svg'">
          <span>{{ duration }}</span>
        </div>

        <div class="record-stat" *ngIf="progressMode === 'indeterminate' || queryStatus.estimatedProjectionCount === 0">
          <span>{{ queryStatus.completedProjections | number }} records</span>
        </div>

        <div class="record-stat" *ngIf="progressMode !== 'indeterminate'">
          <span>{{ queryStatus.completedProjections | number }} of {{ queryStatus.estimatedProjectionCount | number }} records</span>
        </div>

        <span class="spacer"></span>
        <img class="icon-button" src="assets/img/tabler/x.svg" (click)="cancel.emit(); $event.stopImmediatePropagation()" title="Cancel query">
      </div>
    </div>
  `,
  styleUrls: ['./query-history-card.component.scss']
})
export class ActiveQueryCardComponent {

  @Input()
  queryStatus: RunningQueryStatus;

  @Output()
  cancel = new EventEmitter();

  get progressMode(): 'determinate' | 'indeterminate' {
    if (this.queryStatus.queryMode === 'STREAM' || isNullOrUndefined(this.queryStatus.estimatedProjectionCount)) {
      return 'indeterminate';
    } else {
      return 'determinate';
    }
  }

  get progress(): number {
    if (isNullOrUndefined(this.queryStatus.estimatedProjectionCount)) {
      return 0;
    } else {
      return (this.queryStatus.completedProjections / this.queryStatus.estimatedProjectionCount) * 100;
    }
  }

  get duration(): string {
    return Timespan.since(this.queryStatus.startTime).duration;
  }

}
