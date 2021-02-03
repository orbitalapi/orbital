import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {RunningQueryStatus} from '../../services/active-queries-notification-service';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-query-editor-bottom-bar',
  template: `
    <div class="footer-bar">
      <button mat-raised-button color="accent"
              *ngIf="currentState !== 'Running'"
              (click)="runQuery()">Run
      </button>
      <div class="running-timer" *ngIf="currentState === 'Running'">
        <img class="loading-spinner" src="assets/img/loading-spinner-yellow.svg">
        <span>Running...&nbsp;</span>
        <app-counter-timer *ngIf="queryStarted" [startDate]="queryStarted"></app-counter-timer>
        <div class="progress" *ngIf="queryStarted && percentComplete > 0">
          <mat-progress-bar mode="determinate" [value]="percentComplete"></mat-progress-bar>
          <span>{{ runningQueryStatus.completedProjections}} of {{ runningQueryStatus.estimatedProjectionCount}}
            records</span>
        </div>
        <button mat-stroked-button *ngIf="currentState === 'Running' && runningQueryStatus" color="accent"
            (click)="cancelQuery.emit()"
        >Cancel</button>

      </div>
      <div class="error-message" *ngIf="currentState === 'Error'">
        <span>{{ error }}</span>
      </div>
    </div>
  `,
  styleUrls: ['./bottom-bar.component.scss']
})
export class BottomBarComponent {

  @Input()
  currentState: QueryState;

  @Input()
  error: string;

  @Output()
  executeQuery = new EventEmitter<void>();

  @Output()
  cancelQuery = new EventEmitter();

  @Input()
  runningQueryStatus: RunningQueryStatus | null;

  get percentComplete(): number | null {
    if (!this.runningQueryStatus) {
      return null;
    }
    if (isNullOrUndefined(this.runningQueryStatus.estimatedProjectionCount)) {
      return null;
    }
    return (this.runningQueryStatus.completedProjections / this.runningQueryStatus.estimatedProjectionCount) * 100;
  }

  @Input()
  queryStarted: Date;

  runQuery() {
    this.currentState = 'Running';
    this.queryStarted = new Date();
    this.executeQuery.emit();
  }

}

export type QueryState = 'Editing' | 'Running' | 'Result' | 'Error';
