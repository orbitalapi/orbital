import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {RunningQueryStatus} from '../services/active-queries-notification-service';
import {Timespan} from '../query-panel/query-editor/counter-timer.component';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-active-query-card',
  template: `
    <div class="history-item" *ngIf="queryStatus">
      <app-vyneql-record [query]="queryStatus.vyneQlQuery"></app-vyneql-record>

      <div class="progress-container">
        <mat-progress-bar [mode]="progressMode" [value]="progress"></mat-progress-bar>
      </div>

      <div class="timestamp-row">
        <div class="record-stat">
          <mat-icon class="clock-icon">schedule</mat-icon>
          <span>{{ duration() }}</span>
        </div>
        <div class="record-stat" *ngIf="this.queryStatus.estimatedProjectionCount">
          <span>{{ queryStatus.completedProjections }} of {{queryStatus.estimatedProjectionCount}} records</span>
        </div>
        <span class="spacer"></span>
        <mat-icon class="clock-icon" (click)="cancel.emit()">close</mat-icon>
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
    if (isNullOrUndefined(this.queryStatus.estimatedProjectionCount)) {
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

  duration(): string {
    return Timespan.since(this.queryStatus.startTime).duration;
  }

}
